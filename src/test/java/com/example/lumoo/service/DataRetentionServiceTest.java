package com.example.lumoo.service;

import com.example.lumoo.domain.order.Order;
import com.example.lumoo.domain.order.OrderRepository;
import com.example.lumoo.domain.pdpp.AuditLogRepository;
import com.example.lumoo.domain.pdpp.DataRetentionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataRetentionServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @InjectMocks private DataRetentionService service;

    // ── anonymiseOldOrders ────────────────────────────────────────────────────

    @Test
    void anonymiseOldOrders_anonymisesOrdersAndReturnsCount() {
        Order order = new Order();
        order.setCustomerName("John");
        order.setAddress("123 Street");
        when(orderRepository.findByCreatedAtBeforeAndCustomerNameNot(any(LocalDateTime.class), anyString()))
                .thenReturn(List.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int count = service.anonymiseOldOrders();

        assertEquals(1, count);
        assertEquals("ANONYMISED", order.getCustomerName());
        assertNull(order.getAddress());
    }

    @Test
    void anonymiseOldOrders_whenNoOrders_returnsZero() {
        when(orderRepository.findByCreatedAtBeforeAndCustomerNameNot(any(LocalDateTime.class), anyString()))
                .thenReturn(List.of());

        int count = service.anonymiseOldOrders();

        assertEquals(0, count);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void anonymiseOldOrders_anonymisesMultipleOrders() {
        Order o1 = new Order(); o1.setCustomerName("Alice");
        Order o2 = new Order(); o2.setCustomerName("Bob");
        when(orderRepository.findByCreatedAtBeforeAndCustomerNameNot(any(LocalDateTime.class), anyString()))
                .thenReturn(List.of(o1, o2));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int count = service.anonymiseOldOrders();

        assertEquals(2, count);
    }

    // ── purgeOldAuditLogs ─────────────────────────────────────────────────────

    @Test
    void purgeOldAuditLogs_callsDeleteByCreatedAtBefore() {
        service.purgeOldAuditLogs();

        verify(auditLogRepository).deleteByCreatedAtBefore(any(LocalDateTime.class));
    }
}
