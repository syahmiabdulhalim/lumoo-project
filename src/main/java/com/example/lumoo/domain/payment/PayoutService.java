package com.example.lumoo.domain.payment;

import com.example.lumoo.domain.order.Order;
import com.example.lumoo.domain.order.OrderItem;
import com.example.lumoo.domain.order.OrderRepository;
import com.example.lumoo.domain.user.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Set;

@Service
public class PayoutService {

    private static final Logger log = LoggerFactory.getLogger(PayoutService.class);
    private static final Set<String> SUPPORTED_NETWORKS = Set.of("afrimoney", "wave");

    @Value("${modempay.api-key:}")
    private String apiKey;

    @Value("${modempay.api-url:https://api.modempay.com/v1}")
    private String apiUrl;

    @Autowired private OrderRepository orderRepository;
    @Autowired private ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Initiates a ModemPay transfer to the vendor when an order is marked DELIVERED.
     * Idempotent — uses "payout-order-{id}" as idempotency key.
     * Silently skips if vendor has no payout details configured.
     */
    @Transactional
    public void tryPayoutVendor(Order order) {
        if (!isConfigured()) {
            log.info("[Payout] Skipped — ModemPay not configured (order #{})", order.getId());
            return;
        }

        User vendor = resolveVendor(order);
        if (vendor == null) {
            log.warn("[Payout] Skipped — cannot resolve vendor for order #{}", order.getId());
            order.setPayoutStatus("SKIPPED");
            orderRepository.save(order);
            return;
        }

        String payoutPhone   = vendor.getPayoutPhone();
        String payoutNetwork = vendor.getPayoutNetwork();

        if (payoutPhone == null || payoutPhone.isBlank() ||
            payoutNetwork == null || !SUPPORTED_NETWORKS.contains(payoutNetwork)) {
            log.info("[Payout] Skipped — vendor {} has no payout details configured", vendor.getId());
            order.setPayoutStatus("SKIPPED");
            orderRepository.save(order);
            return;
        }

        long amountGmd = Math.round(order.getVendorEarnings());
        if (amountGmd < 1) {
            log.warn("[Payout] Skipped — vendor earnings too small ({}GMD) for order #{}", amountGmd, order.getId());
            order.setPayoutStatus("SKIPPED");
            orderRepository.save(order);
            return;
        }

        try {
            Map<String, Object> body = Map.of(
                    "amount",           amountGmd,
                    "currency",         "GMD",
                    "network",          payoutNetwork,
                    "account_number",   payoutPhone,
                    "beneficiary_name", vendor.getFullName() != null ? vendor.getFullName() : vendor.getUsername(),
                    "narration",        "LUMOO vendor payout — Order #" + order.getId(),
                    "metadata",         Map.of("order_id", order.getId().toString(), "vendor_id", vendor.getId().toString())
            );

            String idempotencyKey = "payout-order-" + order.getId();
            String requestBody = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "/transfers"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Idempotency-Key", idempotencyKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                JsonNode json = objectMapper.readTree(response.body());
                String transferId = json.path("id").asText();
                order.setPayoutStatus("PENDING");
                order.setPayoutTransferId(transferId);
                log.info("[Payout] Transfer initiated — orderId={} transferId={} vendor={} amount={}GMD",
                        order.getId(), transferId, vendor.getId(), amountGmd);
            } else {
                log.error("[Payout] Transfer failed — status={} body={}", response.statusCode(), response.body());
                order.setPayoutStatus("FAILED");
            }

        } catch (Exception e) {
            log.error("[Payout] Transfer exception for order #{}", order.getId(), e);
            order.setPayoutStatus("FAILED");
        }

        orderRepository.save(order);
    }

    /** Updates payout status when ModemPay sends transfer.succeeded / transfer.failed webhooks. */
    @Transactional
    public void handleTransferWebhook(String transferId, String event) {
        orderRepository.findByPayoutTransferId(transferId).ifPresent(order -> {
            switch (event) {
                case "transfer.succeeded" -> order.setPayoutStatus("PAID");
                case "transfer.failed", "transfer.reversed" -> order.setPayoutStatus("FAILED");
                default -> { return; }
            }
            orderRepository.save(order);
            log.info("[Payout] Status updated — orderId={} transferId={} event={}", order.getId(), transferId, event);
        });
    }

    private User resolveVendor(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) return null;
        for (OrderItem item : order.getItems()) {
            if (item.getProduct() != null && item.getProduct().getVendor() != null) {
                return item.getProduct().getVendor();
            }
        }
        return null;
    }
}
