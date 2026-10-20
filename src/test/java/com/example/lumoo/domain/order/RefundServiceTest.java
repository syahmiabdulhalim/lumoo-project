package com.example.lumoo.domain.order;
import com.example.lumoo.domain.product.Product;
import com.example.lumoo.domain.user.User;
import com.example.lumoo.domain.user.UserService;
import com.example.lumoo.infrastructure.email.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class RefundServiceTest {
    @Mock private RefundRepository refundRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private EmailService emailService;
    @Mock private UserService userService;
    @InjectMocks private RefundService refundService;

    private static OrderItem item(long id, double price, int qty) {
        OrderItem i = new OrderItem();
        i.setId(id);
        i.setProductName("P" + id);
        i.setPrice(price);
        i.setQuantity(qty);
        Product p = new Product();
        User vendor = new User();
        vendor.setId(100L);
        p.setVendor(vendor);
        i.setProduct(p);
        return i;
    }
    /** 2×100 + 1×50, shipping 20, promo 25 → buyer paid 245; vendor earns 225 + 20 = 245; Lumoo 25 − 25 = 0. */
    private static Order order(String status, String payoutStatus, double discount) {
        Order o = new Order();
        o.setId(1L);
        o.setStatus(status);
        o.setPaymentMethod("MODEMPAY");
        o.setPayoutStatus(payoutStatus);
        o.setItems(new ArrayList<>(List.of(item(1, 100, 2), item(2, 50, 1))));
        o.setShippingCost(20.0);
        o.setDiscountAmount(discount);
        o.setTotalAmount(250 + 20 - discount);
        o.setAdminCommission(25 - discount);
        o.setVendorEarnings(o.getTotalAmount() - o.getAdminCommission());
        User buyer = new User();
        buyer.setEmail("buyer@test.com");
        buyer.setUsername("buyer");
        o.setUser(buyer);
        return o;
    }
    private void stub(Order o) {
        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(o));
    }

    @Test
    void partialRefund_noPromo_vendorGivesBackEarnings_lumooGivesBackCommission() {
        RefundService.Breakdown b = RefundService.compute(order("DELIVERED", "HELD", 0), Map.of(1L, 1), false);
        assertEquals(100, b.amount(), 0.001);
        assertEquals(90, b.vendorShare(), 0.001);
        assertEquals(10, b.lumooShare(), 0.001);
    }
    @Test
    void partialRefund_withPromo_buyerGetsWhatTheyPaid_lumooRecoversItsDiscount() {
        RefundService.Breakdown b = RefundService.compute(order("DELIVERED", "HELD", 25), Map.of(1L, 1), false);
        assertEquals(10, b.discountShare(), 0.001);
        assertEquals(90, b.amount(), 0.001);
        assertEquals(90, b.vendorShare(), 0.001);
        assertEquals(0, b.lumooShare(), 0.001);
    }
    @Test
    void shippingIsRefundedOnlyOnce() {
        Order o = order("DELIVERED", "HELD", 0);
        assertEquals(20, RefundService.compute(o, Map.of(), true).amount(), 0.001);
        o.setShippingRefunded(true);
        assertTrue(RefundService.compute(o, Map.of(), true).isEmpty());
    }
    @Test
    void cannotRefundMoreThanWasBought() {
        Order o = order("DELIVERED", "HELD", 0);
        o.getItems().get(0).setRefundedQuantity(2);
        assertThrows(RefundService.RefundException.class, () -> RefundService.compute(o, Map.of(1L, 1), false));
    }
    @Test
    void partialRefundsAddUpExactlyToWhatWasPaid() {
        Order o = new Order();
        o.setId(1L);
        o.setStatus("DELIVERED");
        o.setItems(new ArrayList<>(List.of(item(1, 10, 3))));
        o.setDiscountAmount(1);
        o.setTotalAmount(29);
        double sum = 0;
        for (int n = 0; n < 3; n++) {
            RefundService.Breakdown b = RefundService.compute(o, Map.of(1L, 1), false);
            sum += b.amount();
            o.setRefundedAmount(o.getRefundedAmount() + b.amount());
            o.getItems().get(0).setRefundedQuantity(o.getItems().get(0).getRefundedQuantity() + 1);
        }
        assertEquals(29, sum, 0.001);
    }
    @Test
    void create_heldPayout_reducesThisOrdersPayout() {
        Order o = order("DELIVERED", "HELD", 0);
        stub(o);
        Refund r = refundService.create(1L, Map.of(1L, 1), false, "Damaged", "admin");
        assertEquals(100, r.getAmount(), 0.001);
        assertEquals(90, r.getVendorDeduction(), 0.001);
        assertEquals(0, r.getVendorDebt(), 0.001);
        assertEquals(245 - 90, o.getVendorEarnings(), 0.001);
        assertEquals(15, o.getAdminCommission(), 0.001);
        assertEquals(100, o.getRefundedAmount(), 0.001);
        assertEquals(1, o.getItems().get(0).getRefundedQuantity());
        assertEquals("HELD", o.getPayoutStatus());
        assertEquals(Refund.Status.PENDING, r.getStatus());
        assertNotEquals(DayOfWeek.SATURDAY, r.getDueBy().getDayOfWeek());
        assertNotEquals(DayOfWeek.SUNDAY, r.getDueBy().getDayOfWeek());
        verify(refundRepository).save(r);
        verify(emailService).sendEmail(eq("buyer@test.com"), contains("Refund approved"), anyString());
    }
    @Test
    void create_paidOutAlready_becomesVendorDebt() {
        Order o = order("DELIVERED", "PAID", 0);
        stub(o);
        Refund r = refundService.create(1L, Map.of(2L, 1), true, "Wrong item", "admin");
        assertEquals(70, r.getAmount(), 0.001);
        assertEquals(65, r.getVendorDebt(), 0.001);
        assertEquals(245, o.getVendorEarnings(), 0.001);
        assertEquals(100L, r.getVendor().getId());
        assertTrue(o.isShippingRefunded());
    }
    @Test
    void createFull_onHeldPayout_cancelsPayout() {
        Order o = order("DELIVERED", "HELD", 25);
        Refund r = refundService.createFull(o, "Cancelled", "admin").orElseThrow();
        assertEquals(245, r.getAmount(), 0.001);
        assertEquals("CANCELLED", o.getPayoutStatus());
        assertEquals(0, o.getVendorEarnings(), 0.001);
        assertTrue(refundService.createFull(o, "again", "admin").isEmpty());
    }
    @Test
    void cancelledPayout_vendorNotCharged() {
        Order o = order("RETURNED", "CANCELLED", 0);
        stub(o);
        Refund r = refundService.create(1L, Map.of(1L, 2, 2L, 1), true, "Returned", "admin");
        assertEquals(270, r.getAmount(), 0.001);
        assertEquals(0, r.getVendorDeduction(), 0.001);
        assertEquals(0, r.getVendorDebt(), 0.001);
    }
    @Test
    void unpaidOrders_cannotBeRefunded() {
        Order o = order("AWAITING_PAYMENT", null, 0);
        stub(o);
        assertThrows(RefundService.RefundException.class,
                () -> refundService.create(1L, Map.of(1L, 1), false, "x", "admin"));
        Order cod = order("SHIPPED", null, 0);
        cod.setPaymentMethod("COD");
        assertFalse(refundService.isRefundable(cod));
        cod.setDeliveredAt(LocalDateTime.now());
        cod.setStatus("DELIVERED");
        assertTrue(refundService.isRefundable(cod));
    }
    @Test
    void complete_requiresReference_thenEmailsBuyer() {
        Refund r = new Refund();
        r.setOrder(order("DELIVERED", "HELD", 0));
        r.setAmount(100);
        when(refundRepository.findById(5L)).thenReturn(Optional.of(r));
        assertEquals(RefundService.CompleteResult.REFERENCE_REQUIRED, refundService.complete(5L, "Wave", " ", "admin"));
        assertEquals(RefundService.CompleteResult.COMPLETED, refundService.complete(5L, "Wave", "TX-123", "admin"));
        assertEquals(Refund.Status.COMPLETED, r.getStatus());
        assertEquals("TX-123", r.getReference());
        assertEquals(RefundService.CompleteResult.ALREADY_COMPLETED, refundService.complete(5L, "Wave", "TX-9", "admin"));
        verify(emailService).sendEmail(eq("buyer@test.com"), contains("has been sent"), contains("TX-123"));
    }
    @Test
    void outstandingDebt_isDebtMinusWhatPayoutsKeptBack() {
        when(refundRepository.sumVendorDebt(100L)).thenReturn(65.0);
        when(orderRepository.sumPayoutDeductions(100L)).thenReturn(40.0);
        assertEquals(25, refundService.outstandingVendorDebt(100L), 0.001);
    }
    @Test
    void addBusinessDays_skipsWeekends() {
        LocalDate friday = LocalDate.of(2026, 10, 23);
        assertEquals(LocalDate.of(2026, 10, 26), RefundService.addBusinessDays(friday, 1));
        assertEquals(LocalDate.of(2026, 11, 12), RefundService.addBusinessDays(friday, 14));
    }
    @Test
    void underpaidCharge_refundsWhatWasPaid_splitByOrderTotal_vendorUntouched() {
        Order a = order("PAYMENT_FAILED", null, 0);  // total 270
        Order b = order("PAYMENT_FAILED", null, 0);
        b.setId(2L);
        b.setTotalAmount(130);
        double earningsBefore = a.getVendorEarnings(), commissionBefore = a.getAdminCommission();
        List<Refund> made = refundService.refundChargedPayment(List.of(a, b), 200, java.util.Set.of(1L, 2L), "short");
        assertEquals(2, made.size());
        assertEquals(135, made.get(0).getAmount(), 0.001);
        assertEquals(65, made.get(1).getAmount(), 0.001);
        assertEquals(0, made.get(0).getVendorDeduction(), 0.001);
        assertEquals(earningsBefore, a.getVendorEarnings(), 0.001);
        assertEquals(commissionBefore, a.getAdminCommission(), 0.001);
        assertEquals(135, a.getRefundedAmount(), 0.001);
    }
    @Test
    void sameChargeReportedTwice_refundsOnce() {
        Order a = order("PAYMENT_FAILED", null, 0);
        when(refundRepository.findByOrderId(1L)).thenReturn(List.of(new Refund()));
        assertTrue(refundService.refundChargedPayment(List.of(a), 100, java.util.Set.of(1L), "short").isEmpty());
        verify(refundRepository, never()).save(any());
    }
    @Test
    void unknownPaidAmount_refundsOrderTotal_onlyForListedOrders() {
        Order a = order("CANCELLED", null, 0);
        Order b = order("PAID", null, 0);
        b.setId(2L);
        List<Refund> made = refundService.refundChargedPayment(List.of(a, b), -1, java.util.Set.of(1L), "late");
        assertEquals(1, made.size());
        assertEquals(270, made.get(0).getAmount(), 0.001);
        assertEquals(0, b.getRefundedAmount(), 0.001);
    }
}
