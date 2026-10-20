package com.example.lumoo.domain.payment;
import com.example.lumoo.domain.order.Order;
import com.example.lumoo.domain.order.OrderRepository;
import com.example.lumoo.domain.order.OrderService;
import com.example.lumoo.domain.user.User;
import com.example.lumoo.domain.user.UserService;
import com.example.lumoo.infrastructure.email.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class ModemPayServiceTest {
    @Mock private OrderRepository orderRepository;
    @Mock private WebhookEventRepository webhookEventRepository;
    @Mock private PayoutService payoutService;
    @Mock private OrderService orderService;
    @Mock private EmailService emailService;
    @Mock private UserService userService;
    @Mock private com.example.lumoo.domain.order.RefundService refundService;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks private ModemPayService service;
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "webhookSecret", "whsec_test");
        ReflectionTestUtils.setField(service, "apiKey", "sk_test");
    }
    private Order order(long id, String status, double total) {
        Order o = new Order();
        o.setId(id);
        o.setStatus(status);
        o.setTotalAmount(total);
        User u = new User();
        u.setEmail("buyer@test.com");
        o.setUser(u);
        return o;
    }
    @Test
    void multiVendorCharge_fullAmount_marksAllPaid() {
        Order a = order(1, "AWAITING_PAYMENT", 300), b = order(2, "AWAITING_PAYMENT", 200);
        service.applySuccessfulCharge(List.of(a, b), 500, "webhook");
        verify(orderService).markPaid(a);
        verify(orderService).markPaid(b);
    }
    @Test
    void multiVendorCharge_underpaid_marksNonePaid() {
        // Old check compared 450 with each order separately (300 and 200) and passed both.
        Order a = order(1, "AWAITING_PAYMENT", 300), b = order(2, "AWAITING_PAYMENT", 200);
        service.applySuccessfulCharge(List.of(a, b), 450, "webhook");
        verify(orderService, never()).markPaid(any());
        assertEquals("PAYMENT_FAILED", a.getStatus());
        assertEquals("PAYMENT_FAILED", b.getStatus());
        verify(orderService).returnStockForOrder(a);
        verify(orderService).returnStockForOrder(b);
    }
    @Test
    void latePaymentOnCancelledOrder_createsRefund_alertsAdmins_andDoesNotMarkPaid() {
        Order cancelled = order(5, "CANCELLED", 100);
        service.applySuccessfulCharge(List.of(cancelled), 100, "webhook");
        verify(orderService, never()).markPaid(any());
        verify(refundService).refundLatePayment(eq(5L), contains("CANCELLED"));
        verify(userService).notifyAdmins(contains("#LMO-5"));
        assertEquals("CANCELLED", cancelled.getStatus());
    }
    @Test
    void alreadyPaid_isLeftAlone() {
        Order paid = order(6, "PAID", 100);
        service.applySuccessfulCharge(List.of(paid), 100, "polling");
        verify(orderService, never()).markPaid(any());
        verifyNoInteractions(userService);
    }
    @Test
    void dedupKey_distinguishesEventTypesOnSameObject() {
        assertNotEquals(ModemPayService.dedupKey("transfer.succeeded", "tr_1"),
                        ModemPayService.dedupKey("transfer.reversed", "tr_1"));
        assertTrue(ModemPayService.dedupKey("x", "y".repeat(200)).length() <= 100);
    }
    @Test
    void transferReversedAfterSucceeded_isProcessed() throws Exception {
        when(webhookEventRepository.existsByEventId("transfer.succeeded:tr_1")).thenReturn(true);
        when(webhookEventRepository.existsByEventId("transfer.reversed:tr_1")).thenReturn(false);
        service.processWebhook("{\"event\":\"transfer.succeeded\",\"payload\":{\"id\":\"tr_1\",\"metadata\":{}}}");
        service.processWebhook("{\"event\":\"transfer.reversed\",\"payload\":{\"id\":\"tr_1\",\"metadata\":{}}}");
        verify(payoutService, never()).handleTransferWebhook("tr_1", "transfer.succeeded");
        verify(payoutService).handleTransferWebhook("tr_1", "transfer.reversed");
    }
    @Test
    void signature_validAndInvalid() throws Exception {
        String body = "{\"event\":\"charge.succeeded\"}";
        Mac mac = Mac.getInstance("HmacSHA512");
        mac.init(new SecretKeySpec("whsec_test".getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
        String sig = HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
        assertTrue(service.verifySignature(body, sig));
        assertTrue(service.verifySignature(body, sig.toUpperCase()));
        assertFalse(service.verifySignature(body, sig.substring(0, sig.length() - 1) + "0"));
        assertFalse(service.verifySignature(body + " ", sig));
        assertFalse(service.verifySignature(body, null));
    }
}
