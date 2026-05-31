package com.example.lumoo.domain.payment;

import com.example.lumoo.domain.order.Order;
import com.example.lumoo.domain.order.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class PaymentSyncJob {

    private static final Logger log = LoggerFactory.getLogger(PaymentSyncJob.class);

    @Autowired private OrderRepository orderRepository;
    @Autowired private ModemPayService modemPayService;

    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void syncStaleOrders() {
        if (!modemPayService.isConfigured()) return;

        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(5);
        List<Order> stale = orderRepository.findStaleAwaitingPayment(cutoff);

        if (stale.isEmpty()) return;

        log.info("[PaymentSync] Checking {} stale AWAITING_PAYMENT order(s)", stale.size());

        stale.stream()
                .map(Order::getModempayPaymentId)
                .distinct()
                .forEach(intentSecret -> {
                    try {
                        modemPayService.verifyAndSync(intentSecret);
                    } catch (Exception e) {
                        log.warn("[PaymentSync] Failed to sync intentSecret={}: {}", intentSecret, e.getMessage());
                    }
                });
    }
}
