package com.example.lumoo.service;
import com.example.lumoo.domain.order.Order;
import com.example.lumoo.domain.order.OrderMaintenanceJob;
import com.example.lumoo.domain.order.OrderRepository;
import com.example.lumoo.domain.order.OrderService;
import com.example.lumoo.domain.payment.PayoutService;
import com.example.lumoo.domain.user.User;
import com.example.lumoo.infrastructure.email.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class OrderMaintenanceJobTest {
    @Mock private OrderRepository orderRepository;
    @Mock private OrderService orderService;
    @Mock private PayoutService payoutService;
    @Mock private EmailService emailService;
    @InjectMocks private OrderMaintenanceJob job;
    @Test
    void autoConfirmShippedOrders_usesPayoutHold_notImmediatePayout() {
        User buyer = new User();
        buyer.setId(1L);
        buyer.setEmail("buyer@test.com");
        Order order = new Order();
        order.setId(5L);
        order.setUser(buyer);
        order.setStatus("SHIPPED");
        when(orderRepository.findStaleShipped(any())).thenReturn(List.of(order));
        job.autoConfirmShippedOrders();
        verify(orderService).applyDelivered(order);
        verify(orderRepository).save(order);
        verify(payoutService, never()).tryPayoutVendor(any());
    }
}
