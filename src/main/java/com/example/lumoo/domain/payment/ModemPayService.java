package com.example.lumoo.domain.payment;

import com.example.lumoo.domain.order.Order;
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
import java.util.HexFormat;
import java.util.Map;

@Service
public class ModemPayService {

    private static final Logger log = LoggerFactory.getLogger(ModemPayService.class);

    @Value("${modempay.api-key:}")
    private String apiKey;

    @Value("${modempay.webhook-secret:}")
    private String webhookSecret;

    @Value("${modempay.merchant-id:}")
    private String merchantId;

    @Value("${modempay.api-url:https://api.modempay.com/v1}")
    private String apiUrl;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Autowired private OrderRepository orderRepository;
    @Autowired private WebhookEventRepository webhookEventRepository;
    @Autowired private ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    // ── Configuration check ──────────────────────────────────────────────────

    public boolean isConfigured() {
        return !apiKey.isBlank() && !webhookSecret.isBlank() && !merchantId.isBlank();
    }

    // ── Payment Intent ───────────────────────────────────────────────────────

    /**
     * Creates a ModemPay payment intent and returns the hosted checkout URL.
     * Order status must be AWAITING_PROOF before calling this.
     *
     * @return checkout URL to redirect the customer to
     */
    public String createPaymentIntent(Order order) throws Exception {
        if (!isConfigured()) {
            throw new IllegalStateException("ModemPay credentials not configured. Set MODEMPAY_API_KEY, MODEMPAY_MERCHANT_ID, and MODEMPAY_WEBHOOK_SECRET.");
        }

        Map<String, Object> body = Map.of(
                "merchantId",   merchantId,
                "orderId",      order.getId().toString(),
                "amount",       order.getTotalAmount(),
                "currency",     "GMD",
                "description",  "LUMOO Order #" + order.getId(),
                "callbackUrl",  baseUrl + "/api/payment/webhook",
                "returnUrl",    baseUrl + "/buyer/order/" + order.getId() + "?paid"
        );

        String requestBody = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "/payment-intents"))
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
        String paymentId  = json.path("paymentId").asText();
        String paymentUrl = json.path("paymentUrl").asText();

        // Persist the ModemPay payment ID against the order for reconciliation
        order.setModempayPaymentId(paymentId);
        orderRepository.save(order);

        log.info("[ModemPay] Payment intent created — orderId={} paymentId={}", order.getId(), paymentId);
        return paymentUrl;
    }

    // ── Webhook signature verification ───────────────────────────────────────

    /**
     * Verifies the HMAC-SHA256 signature from ModemPay.
     * Header: X-ModemPay-Signature: <hex digest>
     */
    public boolean verifySignature(String rawPayload, String receivedSignature) {
        if (webhookSecret.isBlank() || receivedSignature == null) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(), "HmacSHA256"));
            byte[] computed = mac.doFinal(rawPayload.getBytes());
            String computedHex = HexFormat.of().formatHex(computed);
            return computedHex.equalsIgnoreCase(receivedSignature.trim());
        } catch (Exception e) {
            log.error("[ModemPay] Signature verification failed", e);
            return false;
        }
    }

    // ── Webhook processing ───────────────────────────────────────────────────

    /**
     * Processes an incoming ModemPay webhook idempotently.
     * Duplicate eventIds are silently ignored.
     */
    @Transactional
    public void processWebhook(String rawPayload) throws Exception {
        JsonNode json = objectMapper.readTree(rawPayload);

        String eventId   = json.path("eventId").asText();
        String eventType = json.path("eventType").asText();
        Long   orderId   = json.path("orderId").asLong();

        // Idempotency check — ignore duplicate deliveries
        if (webhookEventRepository.existsByEventId(eventId)) {
            log.info("[ModemPay] Duplicate webhook ignored — eventId={}", eventId);
            return;
        }

        // Record first so a crash after save doesn't cause double processing
        WebhookEvent event = new WebhookEvent();
        event.setEventId(eventId);
        event.setEventType(eventType);
        event.setOrderId(orderId);
        webhookEventRepository.save(event);

        // Apply business logic
        switch (eventType) {
            case "PAYMENT_SUCCESS" -> handlePaymentSuccess(orderId);
            case "PAYMENT_FAILED"  -> handlePaymentFailed(orderId);
            case "PAYMENT_EXPIRED" -> handlePaymentExpired(orderId);
            default -> log.warn("[ModemPay] Unknown event type: {}", eventType);
        }

        log.info("[ModemPay] Webhook processed — eventId={} eventType={} orderId={}", eventId, eventType, orderId);
    }

    private void handlePaymentSuccess(Long orderId) {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.setStatus("PAID");
            orderRepository.save(order);
            log.info("[ModemPay] Order #{} marked PAID", orderId);
        });
    }

    private void handlePaymentFailed(Long orderId) {
        orderRepository.findById(orderId).ifPresent(order -> {
            if ("AWAITING_PROOF".equals(order.getStatus())) {
                order.setStatus("PAYMENT_FAILED");
                orderRepository.save(order);
                log.info("[ModemPay] Order #{} marked PAYMENT_FAILED", orderId);
            }
        });
    }

    private void handlePaymentExpired(Long orderId) {
        orderRepository.findById(orderId).ifPresent(order -> {
            if ("AWAITING_PROOF".equals(order.getStatus())) {
                order.setStatus("PAYMENT_EXPIRED");
                orderRepository.save(order);
                log.info("[ModemPay] Order #{} marked PAYMENT_EXPIRED", orderId);
            }
        });
    }
}
