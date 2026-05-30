package com.example.lumoo.service;
import com.example.lumoo.domain.order.OrderService;
import com.example.lumoo.domain.product.Product;
import com.example.lumoo.domain.product.Review;
import com.example.lumoo.domain.order.Order;
import com.example.lumoo.domain.order.OrderItem;
import com.example.lumoo.domain.order.CartItem;
import com.example.lumoo.domain.payment.WebhookEvent;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private CartRepository cartRepository;
    @Mock private com.example.lumoo.domain.payment.PayoutService payoutService;
    @Mock private com.example.lumoo.domain.product.ProductService productService;
    @InjectMocks private OrderService orderService;
    private User buyer;
    private User vendor;
    @BeforeEach
    void setUp() {
        buyer = new User();
        buyer.setId(1L);
        vendor = new User();
        vendor.setId(2L);
    }
    @Test
    void placeOrder_COD_setsPendingStatus() {
        CartItem item = cartItem(100.0, 2);
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Order result = orderService.placeOrders(buyer, "123 Street", "COD", List.of(item), true, true, false).get(0);
        assertEquals("PENDING", result.getStatus());
    }
    @Test
    void placeOrder_bankTransfer_setsAwaitingProofStatus() {
        CartItem item = cartItem(100.0, 1);
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Order result = orderService.placeOrders(buyer, "123 Street", "BANK", List.of(item), true, true, false).get(0);
        assertEquals("AWAITING_PROOF", result.getStatus());
    }
    @Test
    void placeOrder_calculatesCorrectTotal() {
        CartItem a = cartItem(50.0, 2);   
        CartItem b = cartItem(25.0, 4);   
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Order result = orderService.placeOrders(buyer, "123 Street", "COD", List.of(a, b), true, true, false).get(0);
        assertEquals(200.0, result.getTotalAmount(), 0.001);
    }
    @Test
    void placeOrder_charges10PercentCommission() {
        CartItem item = cartItem(200.0, 1);
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Order result = orderService.placeOrders(buyer, "123 Street", "COD", List.of(item), true, true, false).get(0);
        assertEquals(20.0, result.getAdminCommission(), 0.001);
        assertEquals(180.0, result.getVendorEarnings(), 0.001);
    }
    @Test
    void placeOrder_clearsCart() {
        CartItem item = cartItem(50.0, 1);
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        orderService.placeOrders(buyer, "123 Street", "COD", List.of(item), true, true, false);
        verify(cartRepository).deleteAll(List.of(item));
    }
    @Test
    void placeOrder_savesOneOrderItemPerCartItem() {
        CartItem a = cartItem(10.0, 1);
        CartItem b = cartItem(20.0, 2);
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        orderService.placeOrders(buyer, "123 Street", "COD", List.of(a, b), true, true, false);
        verify(orderItemRepository, times(2)).save(any(OrderItem.class));
    }
    @Test
    void placeOrder_throwsWhenInsufficientStock() {
        Product p = new Product();
        p.setId(10L);
        p.setVendor(vendor);
        CartItem item = cartItem(100.0, 3);
        item.setProduct(p);
        when(productService.decrementStock(10L, 3)).thenReturn(false);
        assertThrows(IllegalStateException.class, () ->
            orderService.placeOrders(buyer, "123 Street", "COD", List.of(item), true, true, false));
    }
    @Test
    void placeOrder_splitsOrdersByVendor() {
        User vendor2 = new User();
        vendor2.setId(3L);
        Product p1 = new Product(); p1.setId(1L); p1.setVendor(vendor);
        Product p2 = new Product(); p2.setId(2L); p2.setVendor(vendor2);
        CartItem a = cartItem(50.0, 1); a.setProduct(p1);
        CartItem b = cartItem(80.0, 1); b.setProduct(p2);
        when(productService.decrementStock(anyLong(), anyInt())).thenReturn(true);
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        List<Order> orders = orderService.placeOrders(buyer, "123 Street", "COD",
                List.of(a, b), true, true, false);
        assertEquals(2, orders.size());
    }
    @Test
    void cancelOrder_notFound() {
        when(orderRepository.findByIdWithItems(99L)).thenReturn(Optional.empty());
        assertEquals(OrderService.CancelResult.NOT_FOUND, orderService.cancelOrder(99L, buyer));
    }
    @Test
    void cancelOrder_unauthorized_whenDifferentUser() {
        Order order = orderWithStatus("PENDING", buyer);
        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));
        User attacker = new User();
        attacker.setId(99L);
        assertEquals(OrderService.CancelResult.UNAUTHORIZED, orderService.cancelOrder(1L, attacker));
    }
    @Test
    void cancelOrder_cannotCancel_whenShipped() {
        Order order = orderWithStatus("SHIPPED", buyer);
        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));
        assertEquals(OrderService.CancelResult.CANNOT_CANCEL, orderService.cancelOrder(1L, buyer));
    }
    @Test
    void cancelOrder_success_whenPending() {
        Order order = orderWithStatus("PENDING", buyer);
        order.setItems(List.of());
        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));
        assertEquals(OrderService.CancelResult.CANCELLED, orderService.cancelOrder(1L, buyer));
        verify(orderRepository).delete(order);
    }
    @Test
    void cancelOrder_success_whenAwaitingProof() {
        Order order = orderWithStatus("AWAITING_PROOF", buyer);
        order.setItems(List.of());
        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));
        assertEquals(OrderService.CancelResult.CANCELLED, orderService.cancelOrder(1L, buyer));
    }
    @Test
    void cancelOrder_success_whenAwaitingPayment() {
        Order order = orderWithStatus("AWAITING_PAYMENT", buyer);
        order.setItems(List.of());
        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));
        assertEquals(OrderService.CancelResult.CANCELLED, orderService.cancelOrder(1L, buyer));
    }
    @Test
    void markShipped_notFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());
        assertEquals(OrderService.ShipResult.NOT_FOUND, orderService.markShipped(99L, 2L, null));
    }
    @Test
    void markShipped_unauthorized_whenNotVendorOrder() {
        Order order = orderWithStatus("PAID", buyer);
        order.setItems(List.of(itemForVendor(99L)));   
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        assertEquals(OrderService.ShipResult.UNAUTHORIZED, orderService.markShipped(1L, 2L, null));
    }
    @Test
    void markShipped_invalidStatus_whenNotPaid() {
        Order order = orderWithStatus("PENDING", buyer);
        order.setItems(List.of(itemForVendor(2L)));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        assertEquals(OrderService.ShipResult.INVALID_STATUS, orderService.markShipped(1L, 2L, null));
    }
    @Test
    void markShipped_success_setsStatusAndTracking() {
        Order order = orderWithStatus("PAID", buyer);
        order.setItems(List.of(itemForVendor(2L)));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        OrderService.ShipResult result = orderService.markShipped(1L, 2L, "TRK-001");
        assertEquals(OrderService.ShipResult.SHIPPED, result);
        assertEquals("SHIPPED", order.getStatus());
        assertEquals("TRK-001", order.getTrackingNumber());
    }
    @Test
    void markDelivered_unauthorized_whenWrongUser() {
        Order order = orderWithStatus("SHIPPED", buyer);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        assertEquals(OrderService.DeliverResult.UNAUTHORIZED, orderService.markDelivered(1L, 99L));
    }
    @Test
    void markDelivered_invalidStatus_whenNotShipped() {
        Order order = orderWithStatus("PAID", buyer);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        assertEquals(OrderService.DeliverResult.INVALID_STATUS, orderService.markDelivered(1L, 1L));
    }
    @Test
    void markDelivered_success() {
        Order order = orderWithStatus("SHIPPED", buyer);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        assertEquals(OrderService.DeliverResult.DELIVERED, orderService.markDelivered(1L, 1L));
        assertEquals("DELIVERED", order.getStatus());
    }
    @Test
    void requestReturn_invalidStatus_whenNotDelivered() {
        Order order = orderWithStatus("SHIPPED", buyer);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        assertEquals(OrderService.ReturnResult.INVALID_STATUS, orderService.requestReturn(1L, 1L, "damaged"));
    }
    @Test
    void requestReturn_success_setsReasonAndStatus() {
        Order order = orderWithStatus("DELIVERED", buyer);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        OrderService.ReturnResult result = orderService.requestReturn(1L, 1L, "product damaged");
        assertEquals(OrderService.ReturnResult.REQUESTED, result);
        assertEquals("RETURN_REQUESTED", order.getStatus());
        assertEquals("product damaged", order.getReturnReason());
    }
    @Test
    void countAll_delegatesToRepository() {
        when(orderRepository.count()).thenReturn(42L);
        assertEquals(42L, orderService.countAll());
    }
    @Test
    void sumVendorRevenue_delegatesToRepository() {
        when(orderRepository.sumVendorRevenue(2L)).thenReturn(1500.0);
        assertEquals(1500.0, orderService.sumVendorRevenue(2L), 0.001);
    }
    @Test
    void countVendorOrders_delegatesToRepository() {
        when(orderRepository.countVendorOrders(2L)).thenReturn(7L);
        assertEquals(7L, orderService.countVendorOrders(2L));
    }
    private CartItem cartItem(double price, int qty) {
        CartItem ci = new CartItem();
        ci.setPrice(price);
        ci.setQuantity(qty);
        ci.setName("Test Product");
        return ci;
    }
    private Order orderWithStatus(String status, User user) {
        Order o = new Order();
        o.setId(1L);
        o.setUser(user);
        o.setStatus(status);
        return o;
    }
    private OrderItem itemForVendor(Long vendorId) {
        User v = new User();
        v.setId(vendorId);
        Product p = new Product();
        p.setVendor(v);
        OrderItem oi = new OrderItem();
        oi.setProduct(p);
        return oi;
    }
}
