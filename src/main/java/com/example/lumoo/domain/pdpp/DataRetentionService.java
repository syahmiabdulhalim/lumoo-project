package com.example.lumoo.domain.pdpp;

import com.example.lumoo.domain.order.Order;
import com.example.lumoo.domain.pdpp.AuditLogRepository;
import com.example.lumoo.domain.order.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DataRetentionService {

    private static final Logger log = LoggerFactory.getLogger(DataRetentionService.class);

    @Autowired private OrderRepository orderRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    @Value("${data.retention.orders-anonymise-years:2}")
    private int ordersAnonymiseYears;

    @Value("${data.retention.audit-keep-years:5}")
    private int auditKeepYears;

    // Run on the 1st of every month at 2am
    @Scheduled(cron = "0 0 2 1 * *")
    @Transactional
    public void enforceRetentionPolicy() {
        anonymiseOldOrders();
        purgeOldAuditLogs();
    }

    @Transactional
    public int anonymiseOldOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusYears(ordersAnonymiseYears);
        List<Order> oldOrders = orderRepository.findByCreatedAtBeforeAndCustomerNameNot(cutoff, "ANONYMISED");

        for (Order order : oldOrders) {
            order.setCustomerName("ANONYMISED");
            order.setAddress(null);
            orderRepository.save(order);
        }

        if (!oldOrders.isEmpty()) {
            log.info("[DataRetention] Anonymised {} orders older than {} years", oldOrders.size(), ordersAnonymiseYears);
        }
        return oldOrders.size();
    }

    @Transactional
    public void purgeOldAuditLogs() {
        LocalDateTime cutoff = LocalDateTime.now().minusYears(auditKeepYears);
        auditLogRepository.deleteByCreatedAtBefore(cutoff);
        log.info("[DataRetention] Purged audit logs older than {} years", auditKeepYears);
    }
}
