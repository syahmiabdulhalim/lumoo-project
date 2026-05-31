package com.example.lumoo.domain.admin;

import com.example.lumoo.domain.order.OrderRepository;
import com.example.lumoo.domain.subscriber.SubscriberRepository;
import com.example.lumoo.domain.vendor.VendorApplicationRepository;
import com.example.lumoo.infrastructure.email.EmailService;
import com.example.lumoo.infrastructure.email.EmailTemplates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class DailyDigestJob {

    private static final Logger log = LoggerFactory.getLogger(DailyDigestJob.class);

    @Autowired private SiteSettingsService siteSettingsService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private SubscriberRepository subscriberRepository;
    @Autowired private VendorApplicationRepository vendorApplicationRepository;
    @Autowired private EmailService emailService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Scheduled(cron = "0 0 8 * * *")
    public void sendDailyDigest() {
        String adminEmail = siteSettingsService.get().getBusinessEmail();
        if (adminEmail == null || adminEmail.isBlank()) {
            log.warn("[DailyDigest] No admin email configured — skipping digest");
            return;
        }

        LocalDateTime since = LocalDateTime.now().minusDays(1);
        String date         = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy"));

        long newOrders        = orderRepository.countOrdersSince(since);
        double revenue        = orderRepository.sumRevenueSince(since);
        long pendingProof     = orderRepository.countByStatus("PROOF_UPLOADED");
        long returnRequests   = orderRepository.countByStatus("RETURN_REQUESTED");
        long pendingVendorApps = vendorApplicationRepository.countByStatus("PENDING");
        long newSubscribers   = subscriberRepository.countNewSince(since);

        emailService.sendEmail(adminEmail,
                "LUMOO Daily Digest — " + date,
                EmailTemplates.adminDailyDigest(date, newOrders, revenue,
                        pendingProof, returnRequests, pendingVendorApps,
                        newSubscribers, baseUrl + "/admin/dashboard"));

        log.info("[DailyDigest] Sent to {} — orders={} revenue={}GMD", adminEmail, newOrders, revenue);
    }
}
