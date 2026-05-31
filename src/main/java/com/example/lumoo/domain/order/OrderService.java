package com.example.lumoo.domain.order;
import com.example.lumoo.domain.product.Product;
import com.example.lumoo.domain.product.ProductService;
import com.example.lumoo.domain.product.Review;
import com.example.lumoo.domain.order.Order;
import com.example.lumoo.domain.order.OrderItem;
import com.example.lumoo.domain.order.CartItem;
import com.example.lumoo.domain.payment.PayoutService;
import com.example.lumoo.domain.payment.WebhookEvent;
import com.example.lumoo.infrastructure.email.EmailService;
import com.example.lumoo.infrastructure.email.EmailTemplates;
import com.example.lumoo.domain.user.User;
import com.example.lumoo.domain.user.Role;
import com.example.lumoo.domain.user.Notification;
import com.example.lumoo.domain.vendor.VendorApplication;
import com.example.lumoo.domain.blog.BlogPost;
import com.example.lumoo.domain.admin.SiteSettings;
import com.example.lumoo.domain.inquiry.Inquiry;
import com.example.lumoo.domain.subscriber.Subscriber;
import com.example.lumoo.domain.pdpp.AuditLog;
import com.example.lumoo.domain.pdpp.ErasureRequest;
import com.example.lumoo.domain.pdpp.DataAccessRequest;
import com.example.lumoo.domain.pdpp.BreachIncident;
import com.example.lumoo.domain.product.ProductRepository;
import com.example.lumoo.domain.product.ReviewRepository;
import com.example.lumoo.domain.order.OrderRepository;
import com.example.lumoo.domain.order.OrderItemRepository;
import com.example.lumoo.domain.order.CartRepository;
import com.example.lumoo.domain.payment.WebhookEventRepository;
import com.example.lumoo.domain.user.UserRepository;
import com.example.lumoo.domain.user.NotificationRepository;
import com.example.lumoo.domain.vendor.VendorApplicationRepository;
import com.example.lumoo.domain.blog.BlogPostRepository;
import com.example.lumoo.domain.admin.SiteSettingsRepository;
import com.example.lumoo.domain.inquiry.InquiryRepository;
import com.example.lumoo.domain.subscriber.SubscriberRepository;
import com.example.lumoo.domain.pdpp.AuditLogRepository;
import com.example.lumoo.domain.pdpp.ErasureRequestRepository;
import com.example.lumoo.domain.pdpp.DataAccessRequestRepository;
import com.example.lumoo.domain.pdpp.BreachIncidentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    @Autowired private OrderRepository orderRepository;
    @Autowired private com.example.lumoo.domain.shipping.ShippingRateRepository shippingRateRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private CartRepository cartRepository;
    @Autowired private PayoutService payoutService;
    @Autowired private ProductService productService;
    @Autowired private EmailService emailService;
    @Autowired private com.example.lumoo.domain.user.UserService userService;
    @org.springframework.beans.factory.annotation.Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;
    public List<Order> getUserOrders(User user) {
        return orderRepository.findByUserWithItems(user);
    }
    public List<Order> getVendorOrders(Long vendorId) {
        return orderRepository.findVendorOrdersWithItems(vendorId);
    }
    public List<Order> getAll() {
        return orderRepository.findAll();
    }
    public long countAll() { return orderRepository.count(); }
    public org.springframework.data.domain.Page<Order> getPage(int page, int size) {
        return orderRepository.findAll(
            org.springframework.data.domain.PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by("createdAt").descending()));
    }
    public double sumTotalRevenue() { return orderRepository.sumTotalRevenue(); }
    public double sumTotalCommission() { return orderRepository.sumTotalCommission(); }
    public double sumVendorRevenue(Long vendorId) { return orderRepository.sumVendorRevenue(vendorId); }
    public double sumVendorMonthlySales(Long vendorId) { return orderRepository.sumVendorMonthlySales(vendorId); }
    public long countVendorOrders(Long vendorId) { return orderRepository.countVendorOrders(vendorId); }
    public org.springframework.data.domain.Page<Order> getVendorOrdersPage(Long vendorId, int page, int size) {
        return orderRepository.findVendorOrdersPaged(vendorId,
            org.springframework.data.domain.PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by("createdAt").descending()));
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
                privacyAccepted, termsAccepted, marketingConsent, null, null);
    }

    @Transactional
    public List<Order> placeOrders(User user, String address, String paymentMethod,
                                   List<CartItem> cartItems,
                                   boolean privacyAccepted, boolean termsAccepted, boolean marketingConsent,
                                   String courierName, Double shippingCost) {
        return placeOrders(user, address, paymentMethod, cartItems,
                privacyAccepted, termsAccepted, marketingConsent, courierName, shippingCost, null);
    }

    @Transactional
    public List<Order> placeOrders(User user, String address, String paymentMethod,
                                   List<CartItem> cartItems,
                                   boolean privacyAccepted, boolean termsAccepted, boolean marketingConsent,
                                   String courierName, Double shippingCost, String deliveryArea) {
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
        List<Order> orders = new ArrayList<>();
        for (List<CartItem> vendorItems : byVendor.values()) {
            orders.add(createOrder(user, address, paymentMethod, vendorItems,
                    privacyAccepted, termsAccepted, marketingConsent, courierName, shippingCost, deliveryArea));
        }
        cartRepository.deleteAll(cartItems);
        for (Order o : orders) {
            String buyerEmail = o.getUser().getEmail();
            String buyerName  = o.getUser().getUsername();
            emailService.sendEmail(buyerEmail, "Order #LMO-" + o.getId() + " received",
                    EmailTemplates.orderPlaced(buyerName, String.valueOf(o.getId()), o.getTotalAmount(), o.getAddress()));
            userService.notifyAdmins("🛒 New order #LMO-" + o.getId() + " placed by " + buyerName + " — GMD " + String.format("%.2f", o.getTotalAmount()));
        }
        return orders;
    }
    private Order createOrder(User user, String address, String paymentMethod,
                              List<CartItem> items,
                              boolean privacyAccepted, boolean termsAccepted, boolean marketingConsent,
                              String courierName, Double shippingCost, String deliveryArea) {
        double itemsTotal = items.stream().mapToDouble(i -> i.getPrice() * i.getQuantity()).sum();
        double shipping   = shippingCost != null ? shippingCost : 0.0;
        double total      = itemsTotal + shipping;
        double commission = itemsTotal * 0.10;
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
        if (courierName != null && !courierName.isBlank()) order.setCourierName(courierName);
        if (shippingCost != null && shippingCost > 0) order.setShippingCost(shippingCost);
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
    public enum CancelResult { CANCELLED, NOT_FOUND, UNAUTHORIZED, CANNOT_CANCEL }
    @Transactional
    public CancelResult cancelOrder(Long id, User user) {
        Order order = orderRepository.findByIdWithItems(id).orElse(null);
        if (order == null) return CancelResult.NOT_FOUND;
        if (!order.getUser().getId().equals(user.getId())) return CancelResult.UNAUTHORIZED;
        String st = order.getStatus();
        if (!st.equals("PENDING") && !st.equals("AWAITING_PROOF") && !st.equals("AWAITING_PAYMENT")) return CancelResult.CANNOT_CANCEL;
        returnStockForOrder(order);
        String buyerEmail = order.getUser().getEmail();
        String orderId    = String.valueOf(order.getId());
        orderRepository.delete(order);
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
    public DeliverResult markDelivered(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) return DeliverResult.NOT_FOUND;
        if (!order.getUser().getId().equals(userId)) return DeliverResult.UNAUTHORIZED;
        if (!order.getStatus().equals("SHIPPED")) return DeliverResult.INVALID_STATUS;
        order.setStatus("DELIVERED");
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
        userService.notifyAdmins("↩ Return requested for order #LMO-" + orderId + " by " + order.getUser().getUsername() + ".");
        return ReturnResult.REQUESTED;
    }
    public void resolveReturn(Long orderId) {
        orderRepository.findById(orderId).ifPresent(o -> {
            o.setStatus("RETURNED");
            orderRepository.save(o);
        });
    }
    public void submitProof(Long orderId, String proofUrl) {
        orderRepository.findById(orderId).ifPresent(o -> {
            o.setPaymentProofUrl(proofUrl);
            o.setStatus("PROOF_UPLOADED");
            orderRepository.save(o);
            userService.notifyAdmins("📎 Payment proof uploaded for order #LMO-" + orderId + " — awaiting verification.");
        });
    }
    public void verifyPayment(Long orderId) {
        orderRepository.findById(orderId).ifPresent(o -> {
            o.setStatus("PAID");
            orderRepository.save(o);
            emailService.sendEmail(o.getUser().getEmail(),
                    "Payment confirmed for order #LMO-" + o.getId(),
                    EmailTemplates.orderPaid(o.getUser().getUsername(),
                            String.valueOf(o.getId()), o.getTotalAmount()));
        });
    }
    @Transactional
    public void updateStatus(Long orderId, String status) {
        updateStatus(orderId, status, null, null);
    }

    public void updateStatus(Long orderId, String status, String trackingNumber, java.time.LocalDate estimatedDelivery) {
        orderRepository.findByIdWithItems(orderId).ifPresent(o -> {
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
                    payoutService.tryPayoutVendor(o);
                }
                case "CANCELLED" -> emailService.sendEmail(email, "Order #LMO-" + id + " cancelled",
                                        EmailTemplates.orderCancelled(name, id, null));
            }
        });
    }
    public void delete(Long id) {
        orderRepository.deleteById(id);
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
