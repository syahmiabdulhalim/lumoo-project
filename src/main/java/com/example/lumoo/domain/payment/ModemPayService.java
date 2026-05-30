package com.example.lumoo.domain.payment;
import com.example.lumoo.domain.order.Order;
import com.example.lumoo.domain.order.OrderService;
import com.example.lumoo.domain.payment.WebhookEvent;
import com.example.lumoo.domain.order.OrderRepository;
import com.example.lumoo.domain.payment.WebhookEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Service
public class ModemPayService {
    private static final Logger log = LoggerFactory.getLogger(ModemPayService.class);
    @Value("${modempay.api-key:}")
    private String apiKey;
    @Value("${modempay.webhook-secret:}")
    private String webhookSecret;
    @Value("${modempay.api-url:https://api.modempay.com/v1}")
    private String apiUrl;
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;
    @Autowired private OrderRepository orderRepository;
    @Autowired private WebhookEventRepository webhookEventRepository;
    @Autowired private PayoutService payoutService;
    @Autowired private OrderService orderService;
    @Autowired private ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    public boolean isConfigured() {
        return !apiKey.isBlank() && !webhookSecret.isBlank();
    }
    public String createPaymentIntent(List<Order> orders) throws Exception {
        if (!isConfigured()) {
            throw new IllegalStateException("ModemPay credentials not configured.");
        }
        if (orders == null || orders.isEmpty()) throw new IllegalArgumentException("No orders");
        Order first   = orders.get(0);
        long  total   = orders.stream().mapToLong(o -> Math.round(o.getTotalAmount())).sum();
        String orderIds = orders.stream()
                .map(o -> o.getId().toString())
                .collect(Collectors.joining(","));
        Map<String, Object> payload = Map.of(
                "amount",         total,
                "currency",       "GMD",
                "customer_name",  first.getUser().getUsername(),
                "customer_email", first.getUser().getEmail(),
                "return_url",     baseUrl + "/buyer/dashboard?paid",
                "cancel_url",     baseUrl + "/buyer/dashboard?payment_cancelled",
                "callback_url",   baseUrl + "/api/payment/webhook",
                "metadata",       Map.of("order_ids", orderIds),
                "from_sdk",       false
        );
        String requestBody = objectMapper.writeValueAsString(Map.of("data", payload));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "/payments"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 && response.statusCode() != 201) {
            log.error("[ModemPay] Payment intent failed — status={} body={}", response.statusCode(), response.body());
            throw new RuntimeException("ModemPay API error: " + response.statusCode());
        }
        JsonNode json = objectMapper.readTree(response.body());
        JsonNode data  = json.path("data");
        String intentSecret = data.path("intent_secret").asText();
        String paymentLink  = data.path("payment_link").asText();
        for (Order order : orders) {
            order.setModempayPaymentId(intentSecret);
            orderRepository.save(order);
        }
        log.info("[ModemPay] Payment intent created — orderIds=[{}] intentSecret={}", orderIds, intentSecret);
        return paymentLink;
    }
    @Transactional
    public void verifyAndSync(String intentSecret) {
        if (!isConfigured() || intentSecret == null || intentSecret.isBlank()) return;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "/payments/verify?intent_secret=" + intentSecret))
                    .header("Authorization", "Bearer " + apiKey)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return;
            JsonNode json   = objectMapper.readTree(response.body());
            String  status  = json.path("data").path("status").asText();
            List<Order> orders = orderRepository.findByModempayPaymentId(intentSecret);
            for (Order order : orders) {
                if (!"AWAITING_PAYMENT".equals(order.getStatus())) continue;
                if ("successful".equals(status)) {
                    order.setStatus("PAID");
                    orderRepository.save(order);
                    log.info("[ModemPay] Order #{} verified PAID via polling (webhook was missed)", order.getId());
                } else if ("failed".equals(status) || "cancelled".equals(status)) {
                    order.setStatus("PAYMENT_FAILED");
                    orderRepository.save(order);
                    orderService.returnStockForOrder(order);
                    log.info("[ModemPay] Order #{} verified PAYMENT_FAILED via polling", order.getId());
                }
            }
        } catch (Exception e) {
            log.warn("[ModemPay] Verification polling failed for secret={}: {}", intentSecret, e.getMessage());
        }
    }
    public boolean verifySignature(String rawPayload, String receivedSignature) {
        if (webhookSecret.isBlank() || receivedSignature == null) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] computed = mac.doFinal(rawPayload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String computedHex = HexFormat.of().formatHex(computed);
            return computedHex.equalsIgnoreCase(receivedSignature.trim());
        } catch (Exception e) {
            log.error("[ModemPay] Signature verification failed", e);
            return false;
        }
    }
    @Transactional
    public void processWebhook(String rawPayload) throws Exception {
        JsonNode json = objectMapper.readTree(rawPayload);
        String eventType = json.path("event").asText();
        JsonNode payload  = json.path("payload");
        String eventId    = payload.path("id").asText();   
        JsonNode metadata = payload.path("metadata");
        List<Long> orderIds = new ArrayList<>();
        if (metadata.has("order_ids")) {
            for (String id : metadata.path("order_ids").asText().split(",")) {
                try { orderIds.add(Long.parseLong(id.trim())); } catch (NumberFormatException ignored) {}
            }
        } else if (metadata.has("order_id")) {
            try { orderIds.add(Long.parseLong(metadata.path("order_id").asText())); } catch (NumberFormatException ignored) {}
        }
        Long firstOrderId = orderIds.isEmpty() ? null : orderIds.get(0);
        if (webhookEventRepository.existsByEventId(eventId)) {
            log.info("[ModemPay] Duplicate webhook ignored — eventId={}", eventId);
            return;
        }
        WebhookEvent event = new WebhookEvent();
        event.setEventId(eventId);
        event.setEventType(eventType);
        event.setOrderId(firstOrderId);
        webhookEventRepository.save(event);
        switch (eventType) {
            case "charge.succeeded"     -> orderIds.forEach(this::handlePaymentSuccess);
            case "charge.failed",
                 "charge.cancelled"    -> orderIds.forEach(this::handlePaymentFailed);
            case "charge.expired"      -> orderIds.forEach(this::handlePaymentExpired);
            case "transfer.succeeded",
                 "transfer.failed",
                 "transfer.reversed"   -> payoutService.handleTransferWebhook(eventId, eventType);
            default -> log.warn("[ModemPay] Unknown event type: {}", eventType);
        }
        log.info("[ModemPay] Webhook processed — eventId={} eventType={} orderIds={}", eventId, eventType, orderIds);
    }
    private void handlePaymentSuccess(Long orderId) {
        orderRepository.findById(orderId).ifPresent(order -> {
            if ("AWAITING_PAYMENT".equals(order.getStatus())) {
                order.setStatus("PAID");
                orderRepository.save(order);
                log.info("[ModemPay] Order #{} marked PAID", orderId);
            } else {
                log.info("[ModemPay] Order #{} already in status {} — skipping PAID transition", orderId, order.getStatus());
            }
        });
    }
    private void handlePaymentFailed(Long orderId) {
        orderRepository.findById(orderId).ifPresent(order -> {
            if ("AWAITING_PAYMENT".equals(order.getStatus())) {
                order.setStatus("PAYMENT_FAILED");
                orderRepository.save(order);
                orderService.returnStockForOrder(order);
                log.info("[ModemPay] Order #{} marked PAYMENT_FAILED — stock returned", orderId);
            }
        });
    }
    private void handlePaymentExpired(Long orderId) {
        orderRepository.findById(orderId).ifPresent(order -> {
            if ("AWAITING_PAYMENT".equals(order.getStatus())) {
                order.setStatus("PAYMENT_EXPIRED");
                orderRepository.save(order);
                orderService.returnStockForOrder(order);
                log.info("[ModemPay] Order #{} marked PAYMENT_EXPIRED — stock returned", orderId);
            }
        });
    }
}
