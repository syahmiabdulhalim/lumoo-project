package com.example.lumoo.domain.order;

import com.example.lumoo.domain.inquiry.InquiryRepository;
import com.example.lumoo.domain.payment.PayoutService;
import com.example.lumoo.domain.payment.WebhookEventRepository;
import com.example.lumoo.domain.user.NotificationRepository;
import com.example.lumoo.infrastructure.email.EmailService;
import com.example.lumoo.infrastructure.email.EmailTemplates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OrderMaintenanceJob {

    private static final Logger log = LoggerFactory.getLogger(OrderMaintenanceJob.class);

    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderService orderService;
    @Autowired private PayoutService payoutService;
    @Autowired private EmailService emailService;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private WebhookEventRepository webhookEventRepository;
    @Autowired private InquiryRepository inquiryRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cancelStaleProofOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        List<Order> stale = orderRepository.findStaleAwaitingProof(cutoff);
        if (stale.isEmpty()) return;
        log.info("[OrderMaintenance] Auto-cancelling {} stale AWAITING_PROOF order(s)", stale.size());
        for (Order order : stale) {
            orderService.returnStockForOrder(order);
            String buyerEmail = order.getUser().getEmail();
            String orderId    = String.valueOf(order.getId());
            String buyerName  = order.getUser().getUsername();
            orderRepository.delete(order);
            emailService.sendEmail(buyerEmail,
                    "Order #LMO-" + orderId + " expired",
                    EmailTemplates.staleCancelled(buyerName, orderId));
            log.info("[OrderMaintenance] Order #{} auto-cancelled (no proof in 7 days)", orderId);
        }
    }

    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void retryFailedPayouts() {
        List<Order> failed = orderRepository.findDeliveredWithFailedPayout();
        if (failed.isEmpty()) return;
        log.info("[OrderMaintenance] Retrying payout for {} FAILED order(s)", failed.size());
        for (Order order : failed) {
            log.info("[OrderMaintenance] Retrying payout for order #{}", order.getId());
            payoutService.tryPayoutVendor(order);
        }
    }

    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void autoConfirmShippedOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(14);
        List<Order> stale = orderRepository.findStaleShipped(cutoff);
        if (stale.isEmpty()) return;
        log.info("[OrderMaintenance] Auto-confirming {} SHIPPED order(s) older than 14 days", stale.size());
        for (Order order : stale) {
            order.setStatus("DELIVERED");
            orderRepository.save(order);
            payoutService.tryPayoutVendor(order);
            emailService.sendEmail(order.getUser().getEmail(),
                    "Order #LMO-" + order.getId() + " marked as delivered",
                    EmailTemplates.orderDelivered(order.getUser().getUsername(), String.valueOf(order.getId())));
            log.info("[OrderMaintenance] Order #{} auto-confirmed as DELIVERED", order.getId());
        }
    }

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupOldNotifications() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        int deleted = notificationRepository.deleteOldRead(cutoff);
        if (deleted > 0) {
            log.info("[OrderMaintenance] Deleted {} read notification(s) older than 30 days", deleted);
        }
    }

    @Scheduled(cron = "0 30 2 * * *")
    @Transactional
    public void cleanupOldWebhookEvents() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
        int deleted = webhookEventRepository.deleteOldEvents(cutoff);
        if (deleted > 0) {
            log.info("[OrderMaintenance] Deleted {} webhook event(s) older than 90 days", deleted);
        }
    }

    @Scheduled(cron = "0 0 2 1 * *")
    @Transactional
    public void cleanupOldInquiries() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(365);
        int deleted = inquiryRepository.deleteOldInquiries(cutoff);
        if (deleted > 0) {
            log.info("[OrderMaintenance] Deleted {} inquiry/inquiries older than 1 year", deleted);
        }
    }
}
