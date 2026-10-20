package com.example.lumoo.domain.order;
import com.example.lumoo.domain.promo.PromoService;
import com.example.lumoo.domain.shipping.ShippingRateRepository;
import com.example.lumoo.domain.user.UserService;
import java.time.LocalDate;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.example.lumoo.domain.product.ProductService;
import com.example.lumoo.domain.payment.PayoutService;
import com.example.lumoo.domain.shipping.ShippingService;
import com.example.lumoo.infrastructure.email.EmailService;
import com.example.lumoo.infrastructure.email.EmailTemplates;
import com.example.lumoo.domain.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    @Autowired private OrderRepository orderRepository;
    @Autowired private ShippingRateRepository shippingRateRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private CartRepository cartRepository;
    @Autowired private PayoutService payoutService;
    @Autowired private ProductService productService;
    @Autowired private PromoService promoService;
    @Autowired private EmailService emailService;
    @Autowired private UserService userService;
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;
    public List<Order> getUserOrders(User user) {
        return orderRepository.findByUserWithItems(user);
    }
    public Page<Order> getUserOrdersPage(User user, int page, int size) {
        return orderRepository.findByUserPaged(user,
            PageRequest.of(page, size,
                Sort.by("orderDate").descending()));
    }
    public List<Order> getVendorOrders(Long vendorId) {
        return orderRepository.findVendorOrdersWithItems(vendorId);
    }
    public List<Order> getAll() {
        return orderRepository.findAll();
    }
    public long countAll() { return orderRepository.count(); }
    public Page<Order> getPage(int page, int size) {
        return orderRepository.findAll(
            PageRequest.of(page, size,
                Sort.by("createdAt").descending()));
    }
    public double sumTotalRevenue() { return orderRepository.sumTotalRevenue(); }
    public double sumTotalCommission() { return orderRepository.sumTotalCommission(); }
    public double sumVendorRevenue(Long vendorId) { return orderRepository.sumVendorRevenue(vendorId); }
    public double sumVendorMonthlySales(Long vendorId) { return orderRepository.sumVendorMonthlySales(vendorId); }
    public long countVendorOrders(Long vendorId) { return orderRepository.countVendorOrders(vendorId); }
    public Page<Order> getVendorOrdersPage(Long vendorId, int page, int size) {
        return orderRepository.findVendorOrdersPaged(vendorId,
            PageRequest.of(page, size,
                Sort.by("createdAt").descending()));
    }
    public Page<Order> getVendorOrdersPageFiltered(Long vendorId, String status, int page, int size) {
        var pageable = PageRequest.of(page, size,
                Sort.by("createdAt").descending());
        if (status == null || status.isBlank() || "ALL".equals(status))
            return orderRepository.findVendorOrdersPaged(vendorId, pageable);
        return orderRepository.findVendorOrdersPagedByStatus(vendorId, status, pageable);
    }
    public long countVendorOrdersByStatus(Long vendorId, String status) {
        return orderRepository.countVendorOrdersByStatus(vendorId, status);
    }
    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }
    public Optional<Order> findByIdWithItems(Long id) {
        return orderRepository.findByIdWithItems(id);
    }
    @Transactional
    public List<Order> placeOrders(User user, String address, String paymentMethod,
                                   List<CartItem> cartItems,
                                   boolean privacyAccepted, boolean termsAccepted, boolean marketingConsent) {
        return placeOrders(user, address, paymentMethod, cartItems,
                privacyAccepted, termsAccepted, marketingConsent, null, 0.0, null, null);
    }

    /** Thrown when the promo code is invalid, expired or used up at the moment of ordering. */
    public static class PromoUnavailableException extends RuntimeException {
        public PromoUnavailableException(String code) { super("Promo code unavailable: " + code); }
    }

    /**
     * @param shippingTotal shipping for the whole cart, computed server-side by ShippingService.
     *                      It is split across the per-vendor orders by weight.
     * @param promoCode     optional; redeemed here (inside this transaction) and its discount is
     *                      split across the vendor orders by item value.
     */
    @Transactional
    public List<Order> placeOrders(User user, String address, String paymentMethod,
                                   List<CartItem> cartItems,
                                   boolean privacyAccepted, boolean termsAccepted, boolean marketingConsent,
                                   String courierName, double shippingTotal, String deliveryArea, String promoCode) {
        double discountTotal = 0;
        if (promoCode != null && !promoCode.isBlank()) {
            double allItems = cartItems.stream().mapToDouble(i -> i.getPrice() * i.getQuantity()).sum();
            discountTotal = promoService.redeem(promoCode, allItems)
                    .orElseThrow(() -> new PromoUnavailableException(promoCode));
        }
        Map<Long, List<CartItem>> byVendor = cartItems.stream()
                .collect(Collectors.groupingBy(ci ->
                        ci.getProduct() != null && ci.getProduct().getVendor() != null
                                ? ci.getProduct().getVendor().getId() : 0L));
        for (CartItem ci : cartItems) {
            if (ci.getProduct() != null) {
                boolean ok = productService.decrementStock(ci.getProduct().getId(), ci.getQuantity());
                if (!ok) throw new IllegalStateException(
                        "Insufficient stock for: " + ci.getName() + ". Please update your cart.");
            }
        }
        List<List<CartItem>> groups = new ArrayList<>(byVendor.values());
        double[] shippingShares = split(Math.max(0.0, shippingTotal), groups.stream()
                .mapToDouble(g -> g.stream().mapToDouble(i -> ShippingService.unitWeightKg(i) * i.getQuantity()).sum())
                .toArray());
        double[] discountShares = split(discountTotal, groups.stream()
                .mapToDouble(g -> g.stream().mapToDouble(i -> i.getPrice() * i.getQuantity()).sum())
                .toArray());
        String appliedCode = discountTotal > 0 ? promoCode.trim().toUpperCase() : null;
        List<Order> orders = new ArrayList<>();
        for (int i = 0; i < groups.size(); i++) {
            Order o = createOrder(user, address, paymentMethod, groups.get(i),
                    privacyAccepted, termsAccepted, marketingConsent, courierName, shippingShares[i], deliveryArea,
                    discountShares[i], appliedCode);
            orders.add(o);
        }
        cartRepository.deleteAll(cartItems);
        for (Order o : orders) {
            String buyerEmail = o.getUser().getEmail();
            String buyerName  = o.getUser().getUsername();
            emailService.sendEmail(buyerEmail, "Order #LMO-" + o.getId() + " received",
                    EmailTemplates.orderPlaced(buyerName, String.valueOf(o.getId()), o.getTotalAmount(), o.getAddress()));
            userService.notifyAdmins("🛒 New order #LMO-" + o.getId() + " — " + o.getUser().getDisplayName() + " — GMD " + String.format("%.2f", o.getTotalAmount()));
        }
        return orders;
    }
    /**
     * Splits one amount (shipping by weight, discount by item value) across vendor orders in
     * proportion to the weights; the last order absorbs rounding so the shares add up exactly.
     */
    static double[] split(double amount, double[] weights) {
        double[] shares = new double[weights.length];
        double totalWeight = Arrays.stream(weights).sum();
        if (amount <= 0 || weights.length == 0 || totalWeight <= 0) return shares;
        double allocated = 0;
        for (int i = 0; i < shares.length - 1; i++) {
            shares[i] = Math.round(amount * weights[i] / totalWeight * 100.0) / 100.0;
            allocated += shares[i];
        }
        shares[shares.length - 1] = Math.round((amount - allocated) * 100.0) / 100.0;
        return shares;
    }
    private Order createOrder(User user, String address, String paymentMethod,
                              List<CartItem> items,
                              boolean privacyAccepted, boolean termsAccepted, boolean marketingConsent,
                              String courierName, double shippingCost, String deliveryArea,
                              double discount, String promoCode) {
        double itemsTotal = items.stream().mapToDouble(i -> i.getPrice() * i.getQuantity()).sum();
        double shipping   = shippingCost;
        double total      = itemsTotal + shipping - discount;
        // Lumoo funds promo discounts: they come out of the commission, the vendor is paid in full.
        double commission = itemsTotal * 0.10 - discount;
        Order order = new Order();
        order.setUser(user);
        order.setAddress(address.trim());
        order.setOrderDate(LocalDateTime.now());
        order.setPaymentMethod(paymentMethod);
        order.setStatus(switch (paymentMethod) {
            case "COD"      -> "PENDING";
            case "MODEMPAY" -> "AWAITING_PAYMENT";
            default         -> "AWAITING_PROOF";
        });
        order.setTotalAmount(total);
        order.setAdminCommission(commission);
        order.setVendorEarnings(total - commission);
        order.setPrivacyAccepted(privacyAccepted);
        order.setTermsAccepted(termsAccepted);
        order.setMarketingConsent(marketingConsent);
        if (courierName != null && !courierName.isBlank() && !courierName.equalsIgnoreCase("none")) order.setCourierName(courierName);
        if (shippingCost > 0) order.setShippingCost(shippingCost);
        if (discount > 0) {
            order.setDiscountAmount(discount);
            order.setPromoCode(promoCode);
        }
        if (deliveryArea != null && !deliveryArea.isBlank()) order.setDeliveryArea(deliveryArea);
        Order saved = orderRepository.save(order);
        for (CartItem ci : items) {
            OrderItem oi = new OrderItem();
            oi.setOrder(saved);
            oi.setProduct(ci.getProduct());
            oi.setProductName(ci.getName());
            oi.setPrice(ci.getPrice());
            oi.setQuantity(ci.getQuantity());
            orderItemRepository.save(oi);
        }
        return saved;
    }
    /** Statuses in which the order's items are still taken out of stock (not shipped yet). */
    static final Set<String> STOCK_RESERVED =
            Set.of("PENDING", "AWAITING_PROOF", "PROOF_UPLOADED", "AWAITING_PAYMENT", "PAID");
    /** Orders past this point are part of the financial record and can no longer be cancelled. */
    private static final Set<String> NOT_CANCELLABLE =
            Set.of("CANCELLED", "DELIVERED", "RETURN_REQUESTED", "RETURNED");

    /**
     * Orders are never deleted: they are part of the financial/audit trail and a late ModemPay
     * payment must still find its order. Cancelling releases reserved stock exactly once.
     */
    public void cancelAndRelease(Order order) {
        if (STOCK_RESERVED.contains(order.getStatus())) returnStockForOrder(order);
        order.setStatus("CANCELLED");
    }
    public enum CancelResult { CANCELLED, NOT_FOUND, UNAUTHORIZED, CANNOT_CANCEL }
    @Transactional
    public CancelResult cancelOrder(Long id, User user) {
        Order order = orderRepository.findByIdWithItems(id).orElse(null);
        if (order == null) return CancelResult.NOT_FOUND;
        if (!order.getUser().getId().equals(user.getId())) return CancelResult.UNAUTHORIZED;
        String st = order.getStatus();
        if (!st.equals("PENDING") && !st.equals("AWAITING_PROOF") && !st.equals("AWAITING_PAYMENT")) return CancelResult.CANNOT_CANCEL;
        cancelAndRelease(order);
        orderRepository.save(order);
        String buyerEmail = order.getUser().getEmail();
        String orderId    = String.valueOf(order.getId());
        emailService.sendEmail(buyerEmail, "Order #LMO-" + orderId + " cancelled",
                EmailTemplates.orderCancelled(order.getUser().getUsername(), orderId, null));
        return CancelResult.CANCELLED;
    }
    public enum ShipResult { SHIPPED, NOT_FOUND, UNAUTHORIZED, INVALID_STATUS }
    @Transactional
    public ShipResult markShipped(Long orderId, Long vendorId, String trackingNumber) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) return ShipResult.NOT_FOUND;
        boolean isVendorOrder = order.getItems() != null && order.getItems().stream()
                .anyMatch(i -> i.getProduct() != null
                        && i.getProduct().getVendor() != null
                        && i.getProduct().getVendor().getId().equals(vendorId));
        if (!isVendorOrder) return ShipResult.UNAUTHORIZED;
        String s = order.getStatus();
        if (!s.equals("PAID")) return ShipResult.INVALID_STATUS;
        order.setStatus("SHIPPED");
        order.setShippedAt(LocalDateTime.now());
        if (trackingNumber != null && !trackingNumber.isBlank()) {
            order.setTrackingNumber(trackingNumber.trim());
            setTrackingUrl(order, trackingNumber.trim());
        }
        orderRepository.save(order);
        emailService.sendEmail(order.getUser().getEmail(),
                "Your order #LMO-" + orderId + " has shipped",
                EmailTemplates.orderShipped(order.getUser().getUsername(),
                        String.valueOf(orderId), order.getTrackingNumber(), order.getShippingTrackingUrl()));
        return ShipResult.SHIPPED;
    }
    public enum DeliverResult { DELIVERED, NOT_FOUND, UNAUTHORIZED, INVALID_STATUS }
    @Transactional
    public DeliverResult markDelivered(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) return DeliverResult.NOT_FOUND;
        if (!order.getUser().getId().equals(userId)) return DeliverResult.UNAUTHORIZED;
        if (!order.getStatus().equals("SHIPPED")) return DeliverResult.INVALID_STATUS;
        applyDelivered(order);
        orderRepository.save(order);
        String name = order.getUser().getUsername();
        String id   = String.valueOf(orderId);
        emailService.sendEmail(order.getUser().getEmail(),
                "Order #LMO-" + id + " delivered",
                EmailTemplates.orderDelivered(name, id));
        sendReviewRequest(order, name, id);
        return DeliverResult.DELIVERED;
    }
    public enum ReturnResult { REQUESTED, NOT_FOUND, UNAUTHORIZED, INVALID_STATUS }
    public ReturnResult requestReturn(Long orderId, Long userId, String reason) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) return ReturnResult.NOT_FOUND;
        if (!order.getUser().getId().equals(userId)) return ReturnResult.UNAUTHORIZED;
        if (!order.getStatus().equals("DELIVERED")) return ReturnResult.INVALID_STATUS;
        order.setStatus("RETURN_REQUESTED");
        if (reason != null && !reason.isBlank()) order.setReturnReason(reason.trim());
        orderRepository.save(order);
        userService.notifyAdmins("↩ Return requested — #LMO-" + orderId + " by " + order.getUser().getDisplayName());
        return ReturnResult.REQUESTED;
    }
    /** Completes a requested return. A payout still on hold is cancelled — the vendor isn't paid for returned goods. */
    @Transactional
    public boolean resolveReturn(Long orderId) {
        Order o = orderRepository.findById(orderId).orElse(null);
        if (o == null || !"RETURN_REQUESTED".equals(o.getStatus())) return false;
        o.setStatus("RETURNED");
        if ("HELD".equals(o.getPayoutStatus())) o.setPayoutStatus("CANCELLED");
        orderRepository.save(o);
        return true;
    }
    public void submitProof(Long orderId, String proofUrl) {
        orderRepository.findById(orderId).ifPresent(o -> {
            o.setPaymentProofUrl(proofUrl);
            o.setStatus("PROOF_UPLOADED");
            orderRepository.save(o);
            userService.notifyAdmins("📎 Payment proof uploaded for order #LMO-" + orderId + " — awaiting verification.");
        });
    }
    /** Admin confirms a bank-transfer / mobile-money proof. Only valid while a proof is pending. */
    @Transactional
    public boolean verifyPayment(Long orderId) {
        Order o = orderRepository.findById(orderId).orElse(null);
        if (o == null || !Set.of("AWAITING_PROOF", "PROOF_UPLOADED").contains(o.getStatus())) return false;
        markPaid(o);
        return true;
    }
    /** The one PAID transition (manual proof, webhook and polling all use it). */
    public void markPaid(Order o) {
        o.setStatus("PAID");
        orderRepository.save(o);
        emailService.sendEmail(o.getUser().getEmail(),
                "Payment confirmed for order #LMO-" + o.getId(),
                EmailTemplates.orderPaid(o.getUser().getUsername(),
                        String.valueOf(o.getId()), o.getTotalAmount()));
    }
    public enum AdminCancelResult { CANCELLED, NOT_FOUND, NOT_ALLOWED }
    /** Replaces the old hard delete: the row stays, stock is released, status becomes CANCELLED. */
    @Transactional
    public AdminCancelResult adminCancel(Long orderId) {
        Order o = orderRepository.findByIdWithItems(orderId).orElse(null);
        if (o == null) return AdminCancelResult.NOT_FOUND;
        if (NOT_CANCELLABLE.contains(o.getStatus())) return AdminCancelResult.NOT_ALLOWED;
        if ("PAID".equals(o.getStatus()))
            log.warn("[Order] Paid order #{} cancelled by admin — buyer needs a refund", o.getId());
        cancelAndRelease(o);
        orderRepository.save(o);
        return AdminCancelResult.CANCELLED;
    }
    @Transactional
    public void updateStatus(Long orderId, String status) {
        updateStatus(orderId, status, null, null);
    }

    @Transactional
    public void updateStatus(Long orderId, String status, String trackingNumber, LocalDate estimatedDelivery) {
        orderRepository.findByIdWithItems(orderId).ifPresent(o -> {
            if ("CANCELLED".equals(status) && STOCK_RESERVED.contains(o.getStatus())) returnStockForOrder(o);
            o.setStatus(status);
            if ("SHIPPED".equals(status)) {
                o.setShippedAt(LocalDateTime.now());
                if (trackingNumber != null && !trackingNumber.isBlank()) {
                    o.setTrackingNumber(trackingNumber.trim());
                    setTrackingUrl(o, trackingNumber.trim());
                }
                if (estimatedDelivery != null)
                    o.setEstimatedDeliveryDate(estimatedDelivery);
            }
            if ("DELIVERED".equals(status)) applyDelivered(o);
            orderRepository.save(o);
            String email = o.getUser().getEmail();
            String name  = o.getUser().getUsername();
            String id    = String.valueOf(o.getId());
            switch (status) {
                case "PAID"      -> emailService.sendEmail(email, "Payment confirmed — #LMO-" + id,
                                        EmailTemplates.orderPaid(name, id, o.getTotalAmount()));
                case "SHIPPED"   -> emailService.sendEmail(email, "Order #LMO-" + id + " shipped",
                                        EmailTemplates.orderShipped(name, id, o.getTrackingNumber()));
                case "DELIVERED" -> {
                    emailService.sendEmail(email, "Order #LMO-" + id + " delivered",
                            EmailTemplates.orderDelivered(name, id));
                    sendReviewRequest(o, name, id);
                }
                case "CANCELLED" -> emailService.sendEmail(email, "Order #LMO-" + id + " cancelled",
                                        EmailTemplates.orderCancelled(name, id, null));
            }
        });
    }
    @Transactional
    public void returnStockForOrder(Order order) {
        if (order.getItems() == null) return;
        for (OrderItem item : order.getItems()) {
            if (item.getProduct() != null) {
                productService.returnStock(item.getProduct().getId(), item.getQuantity());
            }
        }
    }

    /**
     * Single place where an order becomes DELIVERED. Starts the payout hold that
     * PayoutReleaseJob releases after payoutHoldDays. COD orders are skipped: the
     * vendor/courier already collected the cash, so a ModemPay payout would pay twice.
     */
    public void applyDelivered(Order order) {
        order.setStatus("DELIVERED");
        if (order.getDeliveredAt() == null) order.setDeliveredAt(LocalDateTime.now());
        if (order.getPayoutStatus() == null || order.getPayoutStatus().isBlank())
            order.setPayoutStatus("COD".equals(order.getPaymentMethod()) ? "SKIPPED" : "HELD");
    }

    private void sendReviewRequest(Order order, String name, String orderId) {
        if (order.getItems() == null || order.getItems().isEmpty()) return;
        OrderItem first = order.getItems().get(0);
        if (first.getProduct() == null) return;
        String productName = first.getProductName();
        String reviewUrl   = baseUrl + "/product/" + first.getProduct().getId();
        emailService.sendEmail(order.getUser().getEmail(),
                "How was your order? Leave a review",
                EmailTemplates.reviewRequest(name, orderId, productName, reviewUrl));
    }

    private void setTrackingUrl(Order order, String trackingNumber) {
        if (order.getCourierName() == null) return;
        shippingRateRepository.findAll().stream()
                .filter(r -> r.getCourierName().equalsIgnoreCase(order.getCourierName()))
                .findFirst()
                .map(r -> r.buildTrackingUrl(trackingNumber))
                .ifPresent(order::setShippingTrackingUrl);
    }
}
