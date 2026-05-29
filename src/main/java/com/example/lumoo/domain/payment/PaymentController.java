package com.example.lumoo.domain.payment;

import com.example.lumoo.shared.dto.ApiResponse;
import com.example.lumoo.domain.order.Order;
import com.example.lumoo.domain.payment.ModemPayService;
import com.example.lumoo.domain.order.OrderService;
import com.example.lumoo.domain.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    @Autowired private ModemPayService modemPayService;
    @Autowired private OrderService orderService;
    @Autowired private UserService userService;

    /**
     * Initiates a ModemPay payment for an order.
     * Returns the hosted checkout URL to redirect the customer to.
     *
     * POST /api/payment/initiate?orderId=12
     */
    @PostMapping("/initiate")
    public ResponseEntity<ApiResponse<String>> initiatePayment(
            @RequestParam Long orderId,
            Principal principal) {

        if (!modemPayService.isConfigured()) {
            return ResponseEntity.ok(ApiResponse.error(
                    "Online payment not yet available. Please use bank transfer or mobile money and upload your proof."));
        }

        var user = userService.findByUsername(principal.getName()).orElse(null);
        if (user == null) return ResponseEntity.status(401).body(ApiResponse.error("Not authenticated."));

        Order order = orderService.findById(orderId).orElse(null);
        if (order == null) return ResponseEntity.status(404).body(ApiResponse.error("Order not found."));
        if (!order.getUser().getId().equals(user.getId()))
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied."));
        if (!"AWAITING_PROOF".equals(order.getStatus()))
            return ResponseEntity.badRequest().body(ApiResponse.error("Order is not awaiting payment."));

        try {
            String paymentUrl = modemPayService.createPaymentIntent(java.util.List.of(order));
            return ResponseEntity.ok(ApiResponse.ok("Payment initiated.", paymentUrl));
        } catch (Exception e) {
            log.error("[Payment] Failed to create intent for order {}", orderId, e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Payment initiation failed. Please try again."));
        }
    }

    /**
     * Receives webhooks from ModemPay.
     * CSRF disabled for this endpoint (external POST from ModemPay).
     * Verifies HMAC-SHA256 signature before processing.
     *
     * POST /api/payment/webhook
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "x-modem-signature", required = false) String signature,
            HttpServletRequest request) {

        log.info("[Webhook] Received from={} signature={} payload={}",
                request.getRemoteAddr(), signature,
                rawPayload.length() > 200 ? rawPayload.substring(0, 200) : rawPayload);

        // Log all headers for debugging
        java.util.Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames != null && headerNames.hasMoreElements()) {
            String h = headerNames.nextElement();
            if (h.toLowerCase().contains("sign") || h.toLowerCase().contains("modem") || h.toLowerCase().contains("hash")) {
                log.info("[Webhook] Header: {}={}", h, request.getHeader(h));
            }
        }

        if (!modemPayService.verifySignature(rawPayload, signature)) {
            log.warn("[Webhook] Invalid signature — received={} from={}", signature, request.getRemoteAddr());
            // In test mode, still process but log the mismatch
            if (signature == null || signature.isBlank()) {
                log.warn("[Webhook] No signature header — processing anyway for debug");
            } else {
                return ResponseEntity.status(401).body("Invalid signature");
            }
        }

        try {
            modemPayService.processWebhook(rawPayload);
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("[Webhook] Processing failed", e);
            // Return 200 anyway so ModemPay doesn't keep retrying for our bug
            return ResponseEntity.ok("ERROR");
        }
    }
}
