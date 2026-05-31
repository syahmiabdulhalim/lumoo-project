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
        if (!"AWAITING_PAYMENT".equals(order.getStatus()) && !"AWAITING_PROOF".equals(order.getStatus()))
            return ResponseEntity.badRequest().body(ApiResponse.error("Order is not awaiting payment."));
        try {
            String paymentUrl = modemPayService.createPaymentIntent(java.util.List.of(order));
            return ResponseEntity.ok(ApiResponse.ok("Payment initiated.", paymentUrl));
        } catch (Exception e) {
            log.error("[Payment] Failed to create intent for order {}", orderId, e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Payment initiation failed. Please try again."));
        }
    }
    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "x-modem-signature", required = false) String signature,
            HttpServletRequest request) {
if (!modemPayService.verifySignature(rawPayload, signature)) {
            log.warn("[Webhook] Invalid signature from={}", request.getRemoteAddr());
            return ResponseEntity.status(401).body("Invalid signature");
        }
        try {
            modemPayService.processWebhook(rawPayload);
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("[Webhook] Processing failed", e);
            return ResponseEntity.ok("ERROR");
        }
    }
}
