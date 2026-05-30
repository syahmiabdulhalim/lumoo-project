package com.example.lumoo.service;
import com.example.lumoo.domain.order.Order;
import com.example.lumoo.domain.order.OrderRepository;
import com.example.lumoo.domain.pdpp.*;
import com.example.lumoo.domain.user.User;
import com.example.lumoo.domain.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class CustomerRightsServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private ErasureRequestRepository erasureRequestRepository;
    @Mock private DataAccessRequestRepository dataAccessRequestRepository;
    @Mock private AuditService auditService;
    @InjectMocks private CustomerRightsService service;
    private HttpServletRequest mockRequest() {
        return mock(HttpServletRequest.class);
    }
    @Test
    void processErasureRequest_whenUserNotFound_completesWithZeroOrders() {
        when(erasureRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());
        Map<String, Object> result = service.processErasureRequest("ghost@test.com", mockRequest());
        assertNotNull(result.get("referenceId"));
        assertEquals(0, result.get("ordersAnonymised"));
        verify(erasureRequestRepository, times(2)).save(any(ErasureRequest.class));
        verify(userRepository, never()).save(any());
    }
    @Test
    void processErasureRequest_whenUserExists_anonymisesUserAndOrders() {
        User user = new User();
        user.setId(1L);
        user.setEmail("ali@test.com");
        user.setFullName("Ali");
        user.setUsername("ali");
        Order order1 = new Order();
        order1.setCustomerName("Ali");
        Order alreadyAnon = new Order();
        alreadyAnon.setCustomerName("ANONYMISED");
        when(erasureRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findByEmail("ali@test.com")).thenReturn(Optional.of(user));
        when(orderRepository.findByUserOrderByOrderDateDesc(user)).thenReturn(List.of(order1, alreadyAnon));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Map<String, Object> result = service.processErasureRequest("ali@test.com", mockRequest());
        assertEquals(1, result.get("ordersAnonymised"));
        assertEquals("ANONYMISED", order1.getCustomerName());
        assertEquals("ANONYMISED", user.getFullName());
        assertTrue(user.getEmail().contains("anonymised"));
        assertNull(user.getPhone());
        assertNull(user.getAddress());
    }
    @Test
    void processErasureRequest_setsStatusCompletedAndProcessedAt() {
        when(erasureRequestRepository.save(any())).thenAnswer(inv -> {
            ErasureRequest er = inv.getArgument(0);
            if (er.getStatus() == ErasureRequest.Status.COMPLETED) {
                assertNotNull(er.getProcessedAt());
            }
            return er;
        });
        when(userRepository.findByEmail("x@test.com")).thenReturn(Optional.empty());
        service.processErasureRequest("x@test.com", mockRequest());
        verify(erasureRequestRepository, times(2)).save(any());
    }
    @Test
    void processDataAccessRequest_whenUserNotFound_returnsEmptyOrders() {
        when(dataAccessRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());
        Map<String, Object> result = service.processDataAccessRequest("nobody@test.com", mockRequest());
        assertEquals(0, result.get("ordersCount"));
        assertNotNull(result.get("referenceId"));
    }
    @Test
    void processDataAccessRequest_whenUserExists_returnsOrderSummary() {
        User user = new User();
        user.setId(2L);
        Order order = new Order();
        order.setId(10L);
        order.setStatus("DELIVERED");
        order.setTotalAmount(99.0);
        order.setPaymentMethod("COD");
        when(dataAccessRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(user));
        when(orderRepository.findByUserOrderByOrderDateDesc(user)).thenReturn(List.of(order));
        Map<String, Object> result = service.processDataAccessRequest("buyer@test.com", mockRequest());
        assertEquals(1, result.get("ordersCount"));
        assertEquals("buyer@test.com", result.get("email"));
        verify(dataAccessRequestRepository, times(2)).save(any(DataAccessRequest.class));
    }
}
