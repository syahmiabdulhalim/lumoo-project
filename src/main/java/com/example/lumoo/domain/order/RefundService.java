package com.example.lumoo.domain.order;
import com.example.lumoo.domain.user.User;
import com.example.lumoo.domain.user.UserService;
import com.example.lumoo.infrastructure.email.EmailService;
import com.example.lumoo.infrastructure.email.EmailTemplates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
/**
 * Lumoo refunds the buyer and recovers the vendor's share: from the order's payout while it is still
 * held, otherwise from the vendor's next payouts (see PayoutService). Lumoo gives back its own commission
 * on the refunded items and, because Lumoo funds promo discounts, gets that discount share back.
 */
@Service
public class RefundService {
    private static final Logger log = LoggerFactory.getLogger(RefundService.class);
    /** Refund policy (terms + returns page): refunds are paid within 14 business days. */
    static final int REFUND_BUSINESS_DAYS = 14;
    /** Statuses in which the buyer has paid. COD orders additionally need to be delivered (cash collected). */
    private static final Set<String> REFUNDABLE =
            Set.of("PAID", "SHIPPED", "DELIVERED", "RETURN_REQUESTED", "RETURNED");
    /** Payout not sent yet: the vendor's share is simply taken off this order's payout. */
    private static final Set<String> PAYOUT_NOT_SENT = Set.of("HELD", "FAILED");
    /** Payout already sent (or settled outside ModemPay): the vendor's share becomes a debt. */
    private static final Set<String> PAYOUT_SENT = Set.of("PENDING", "PAID", "SKIPPED", "OFFSET");

    @Autowired private RefundRepository refundRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private EmailService emailService;
    @Autowired private UserService userService;

    /** How a refund splits between buyer, vendor and Lumoo. Computed server-side only. */
    public record Breakdown(double itemsGross, double discountShare, double shipping, double amount,
                            double vendorShare, double lumooShare, String itemsSummary) {
        public boolean isEmpty() { return amount <= 0; }
    }

    public static class RefundException extends RuntimeException {
        public RefundException(String message) { super(message); }
    }

    public List<Refund> getPending() { return refundRepository.findByStatusWithOrder(Refund.Status.PENDING); }
    public List<Refund> getCompleted() { return refundRepository.findByStatusWithOrder(Refund.Status.COMPLETED); }
    public List<Refund> getForOrder(Long orderId) { return refundRepository.findByOrderId(orderId); }
    public long countPending() { return refundRepository.countByStatus(Refund.Status.PENDING); }

    public boolean isRefundable(Order o) {
        if (!REFUNDABLE.contains(o.getStatus())) return false;
        if ("COD".equals(o.getPaymentMethod()) && o.getDeliveredAt() == null) return false;
        return round2(o.getTotalAmount() - o.getRefundedAmount()) > 0;
    }

    /** Refund preview for the admin form. */
    public Breakdown quote(Long orderId, Map<Long, Integer> quantities, boolean includeShipping) {
        Order o = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new RefundException("Order not found."));
        return compute(o, quantities, includeShipping);
    }

    /**
     * @param quantities order item id → quantity to refund (missing = 0)
     */
    @Transactional
    public Refund create(Long orderId, Map<Long, Integer> quantities, boolean includeShipping,
                         String reason, String actor) {
        if (reason == null || reason.isBlank()) throw new RefundException("A reason is required.");
        Order o = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new RefundException("Order not found."));
        if (!isRefundable(o)) throw new RefundException("This order can't be refunded in its current status.");
        return apply(o, quantities, includeShipping, reason.trim(), actor);
    }

    /**
     * Refunds everything not refunded yet. Used when a paid order is cancelled and when ModemPay
     * money arrives for an order that is already closed — both skip the status check.
     */
    @Transactional
    public Optional<Refund> createFull(Order o, String reason, String actor) {
        if (round2(o.getTotalAmount() - o.getRefundedAmount()) <= 0) return Optional.empty();
        Map<Long, Integer> all = new HashMap<>();
        if (o.getItems() != null)
            for (OrderItem i : o.getItems()) all.put(i.getId(), remaining(i));
        return Optional.of(apply(o, all, true, reason, actor));
    }

    /** ModemPay money for an order that is already closed: refund all of it (no-op if already refunded). */
    @Transactional
    public Optional<Refund> refundLatePayment(Long orderId, String reason) {
        return orderRepository.findByIdWithItems(orderId).flatMap(o -> createFull(o, reason, "system"));
    }

    private Refund apply(Order o, Map<Long, Integer> quantities, boolean includeShipping, String reason, String actor) {
        Breakdown b = compute(o, quantities, includeShipping);
        if (b.isEmpty()) throw new RefundException("Nothing to refund — pick at least one item or the shipping.");

        Refund r = new Refund();
        r.setOrder(o);
        r.setVendor(resolveVendor(o));
        r.setAmount(b.amount());
        r.setShippingAmount(b.shipping());
        r.setItemsSummary(b.itemsSummary());
        r.setReason(reason.length() > 255 ? reason.substring(0, 255) : reason);
        r.setCreatedBy(actor);
        r.setCreatedAt(LocalDateTime.now());
        r.setDueBy(addBusinessDays(LocalDate.now(), REFUND_BUSINESS_DAYS));
        applyVendorShare(o, r, b.vendorShare());

        if (o.getItems() != null)
            for (OrderItem i : o.getItems()) {
                int q = requested(quantities, i);
                if (q > 0) i.setRefundedQuantity(i.getRefundedQuantity() + q);
            }
        if (b.shipping() > 0) o.setShippingRefunded(true);
        o.setRefundedAmount(round2(o.getRefundedAmount() + b.amount()));
        o.setAdminCommission(round2(o.getAdminCommission() - b.lumooShare()));
        orderRepository.save(o);
        refundRepository.save(r);

        log.info("[Refund] #{} approved for order #{} — buyer GMD {} (vendor share {}, debt {}) by {}",
                r.getId(), o.getId(), r.getAmount(), r.getVendorDeduction(), r.getVendorDebt(), actor);
        if (o.getUser() != null)
            emailService.sendEmail(o.getUser().getEmail(), "Refund approved for order #LMO-" + o.getId(),
                    EmailTemplates.refundApproved(o.getUser().getUsername(), String.valueOf(o.getId()),
                            r.getAmount(), r.getDueBy().toString()));
        userService.notifyAdmins("💸 Refund of GMD " + String.format("%.2f", r.getAmount()) + " owed for order #LMO-"
                + o.getId() + " — pay it by " + r.getDueBy() + " and record the reference.");
        return r;
    }

    /**
     * The vendor's share comes off the order's payout while it hasn't been sent; once it has, it is
     * recorded as debt and PayoutService keeps it back from the vendor's next payouts.
     */
    private void applyVendorShare(Order o, Refund r, double vendorShare) {
        String ps = o.getPayoutStatus();
        if ("CANCELLED".equals(ps)) return; // vendor is not being paid for this order at all
        r.setVendorDeduction(vendorShare);
        if (ps != null && PAYOUT_SENT.contains(ps)) {
            r.setVendorDebt(vendorShare);
            return;
        }
        o.setVendorEarnings(Math.max(0, round2(o.getVendorEarnings() - vendorShare)));
        if (PAYOUT_NOT_SENT.contains(ps) && Math.round(o.getVendorEarnings()) < 1) o.setPayoutStatus("CANCELLED");
    }

    public enum CompleteResult { COMPLETED, NOT_FOUND, ALREADY_COMPLETED, REFERENCE_REQUIRED }

    /** Admin has sent the money: record how and the transfer reference. */
    @Transactional
    public CompleteResult complete(Long refundId, String method, String reference, String actor) {
        Refund r = refundRepository.findById(refundId).orElse(null);
        if (r == null) return CompleteResult.NOT_FOUND;
        if (r.getStatus() == Refund.Status.COMPLETED) return CompleteResult.ALREADY_COMPLETED;
        if (reference == null || reference.isBlank()) return CompleteResult.REFERENCE_REQUIRED;
        r.setStatus(Refund.Status.COMPLETED);
        r.setMethod(method != null && !method.isBlank() ? truncate(method.trim(), 50) : null);
        r.setReference(truncate(reference.trim(), 100));
        r.setCompletedBy(actor);
        r.setCompletedAt(LocalDateTime.now());
        refundRepository.save(r);
        Order o = r.getOrder();
        if (o.getUser() != null)
            emailService.sendEmail(o.getUser().getEmail(), "Your refund for order #LMO-" + o.getId() + " has been sent",
                    EmailTemplates.refundSent(o.getUser().getUsername(), String.valueOf(o.getId()),
                            r.getAmount(), r.getMethod(), r.getReference()));
        log.info("[Refund] #{} completed (ref {}) by {}", r.getId(), r.getReference(), actor);
        return CompleteResult.COMPLETED;
    }

    /** What the vendor still owes from refunds on orders they had already been paid for. */
    public double outstandingVendorDebt(Long vendorId) {
        if (vendorId == null) return 0;
        double owed = refundRepository.sumVendorDebt(vendorId) - orderRepository.sumPayoutDeductions(vendorId);
        return Math.max(0, round2(owed));
    }

    static Breakdown compute(Order o, Map<Long, Integer> quantities, boolean includeShipping) {
        List<OrderItem> items = o.getItems() != null ? o.getItems() : List.of();
        double itemsTotal = items.stream().mapToDouble(i -> price(i) * qty(i)).sum();
        double gross = 0;
        StringBuilder summary = new StringBuilder();
        for (OrderItem i : items) {
            int q = requested(quantities, i);
            if (q <= 0) continue;
            if (q > remaining(i))
                throw new RefundException("Only " + remaining(i) + " × " + i.getProductName() + " can still be refunded.");
            gross += price(i) * q;
            if (!summary.isEmpty()) summary.append(", ");
            summary.append(q).append("× ").append(i.getProductName());
        }
        gross = round2(gross);
        double discountShare = itemsTotal > 0 ? round2(o.getDiscountAmount() * gross / itemsTotal) : 0;
        double shipping = includeShipping && !o.isShippingRefunded() && o.getShippingCost() != null
                ? round2(o.getShippingCost()) : 0;
        double remainingTotal = round2(o.getTotalAmount() - o.getRefundedAmount());
        double amount = round2(gross - discountShare + shipping);
        boolean refundsEverything = (shipping > 0 || o.isShippingRefunded() || o.getShippingCost() == null
                || o.getShippingCost() <= 0)
                && items.stream().allMatch(i -> requested(quantities, i) >= remaining(i));
        // The last refund absorbs rounding so the refunds add up to exactly what the buyer paid.
        if (refundsEverything || amount > remainingTotal) amount = remainingTotal;
        if (amount <= 0) return new Breakdown(gross, discountShare, shipping, 0, 0, 0, summary.toString());
        // The vendor gives back exactly what they earned on these items (+ the shipping they were paid).
        double vendorShare = round2(gross * (1 - OrderService.COMMISSION_RATE) + shipping);
        double lumooShare = round2(amount - vendorShare);
        if (shipping > 0) summary.append(summary.isEmpty() ? "" : ", ").append("shipping");
        return new Breakdown(gross, discountShare, shipping, amount, vendorShare, lumooShare,
                truncate(summary.toString(), 500));
    }

    static LocalDate addBusinessDays(LocalDate from, int days) {
        LocalDate d = from;
        int added = 0;
        while (added < days) {
            d = d.plusDays(1);
            if (d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY) added++;
        }
        return d;
    }

    /** Parses the admin form's qty_&lt;itemId&gt; fields. */
    public static Map<Long, Integer> parseQuantities(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(e -> e.getKey().startsWith("qty_") && e.getValue() != null && !e.getValue().isBlank())
                .collect(Collectors.toMap(
                        e -> Long.valueOf(e.getKey().substring(4)),
                        e -> Math.max(0, Integer.parseInt(e.getValue().trim()))));
    }

    static int remaining(OrderItem i) { return Math.max(0, qty(i) - i.getRefundedQuantity()); }
    private static int requested(Map<Long, Integer> quantities, OrderItem i) {
        Integer q = quantities != null ? quantities.get(i.getId()) : null;
        return q != null ? q : 0;
    }
    private static int qty(OrderItem i) { return i.getQuantity() != null ? i.getQuantity() : 0; }
    private static double price(OrderItem i) { return i.getPrice() != null ? i.getPrice() : 0; }
    private static double round2(double v) { return Math.round(v * 100.0) / 100.0; }
    private static String truncate(String s, int max) { return s.length() > max ? s.substring(0, max) : s; }

    private static User resolveVendor(Order o) {
        if (o.getItems() == null) return null;
        for (OrderItem i : o.getItems())
            if (i.getProduct() != null && i.getProduct().getVendor() != null) return i.getProduct().getVendor();
        return null;
    }
}
