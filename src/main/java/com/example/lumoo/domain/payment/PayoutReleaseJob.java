package com.example.lumoo.domain.payment;

import com.example.lumoo.domain.admin.SiteSettingsService;
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
public class PayoutReleaseJob {

    private static final Logger log = LoggerFactory.getLogger(PayoutReleaseJob.class);

    @Autowired private OrderRepository orderRepository;
    @Autowired private PayoutService payoutService;
    @Autowired private SiteSettingsService siteSettingsService;

    @Scheduled(cron = "0 0 6 * * *")
    public void releaseHeldPayouts() {
        int holdDays = siteSettingsService.get().getPayoutHoldDays();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(holdDays);
        List<Order> ready = orderRepository.findHeldPayoutsReadyForRelease(cutoff);
        if (ready.isEmpty()) return;
        log.info("[PayoutRelease] {} orders eligible for payout (hold={}d)", ready.size(), holdDays);
        for (Order order : ready) {
            try {
                payoutService.tryPayoutVendor(order);
                log.info("[PayoutRelease] Payout initiated for order #{}", order.getId());
            } catch (Exception e) {
                log.error("[PayoutRelease] Failed for order #{}", order.getId(), e);
            }
        }
    }
}
