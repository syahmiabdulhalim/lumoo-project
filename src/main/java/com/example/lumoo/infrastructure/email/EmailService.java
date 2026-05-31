package com.example.lumoo.infrastructure.email;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Value("${resend.api.key}")
    private String apiKey;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.email.from:onboarding@resend.dev}")
    private String fromAddress;

    @Async
    public void sendEmail(String toEmail, String subject, String htmlBody) {
        if (apiKey.startsWith("dummy") || apiKey.isBlank()) {
            log.debug("[Email] RESEND_API_KEY not configured — skipping email to {}", toEmail);
            return;
        }
        Resend resend = new Resend(apiKey);
        CreateEmailOptions params = CreateEmailOptions.builder()
                .from("LUMOO <" + fromAddress + ">")
                .to(toEmail)
                .subject(subject)
                .html(htmlBody)
                .build();
        int attempts = 0;
        while (attempts < 3) {
            try {
                resend.emails().send(params);
                log.info("[Email] Sent '{}' to {}", subject, toEmail);
                return;
            } catch (ResendException e) {
                attempts++;
                if (attempts == 3) {
                    log.error("[Email] Permanently failed '{}' to {} after 3 attempts: {}", subject, toEmail, e.getMessage());
                } else {
                    log.warn("[Email] Attempt {}/3 failed for '{}' to {}: {} — retrying in {}s",
                            attempts, subject, toEmail, e.getMessage(), attempts * 5);
                    try { Thread.sleep(attempts * 5000L); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); return; }
                }
            }
        }
    }

    public boolean sendBreachAlert(String toEmail, String subject, String htmlBody) {
        if (apiKey.startsWith("dummy") || apiKey.isBlank()) return false;
        Resend resend = new Resend(apiKey);
        CreateEmailOptions params = CreateEmailOptions.builder()
                .from("LUMOO <" + fromAddress + ">")
                .to(toEmail).subject(subject).html(htmlBody)
                .build();
        try {
            resend.emails().send(params);
            log.info("[Email] Breach alert sent to {}", toEmail);
            return true;
        } catch (ResendException e) {
            log.error("[Email] Breach alert failed to {}: {}", toEmail, e.getMessage());
            return false;
        }
    }

    public boolean sendResetEmail(String toEmail, String token) {
        if (apiKey.startsWith("dummy") || apiKey.isBlank()) {
            log.debug("[Email] RESEND_API_KEY not configured — skipping reset email to {}", toEmail);
            return false;
        }
        String resetLink = baseUrl + "/reset-password?token=" + token;
        Resend resend = new Resend(apiKey);
        CreateEmailOptions params = CreateEmailOptions.builder()
                .from("LUMOO <" + fromAddress + ">")
                .to(toEmail)
                .subject("Reset Your LUMOO Password")
                .html("<strong>Build your Future.</strong><br><br>" +
                      "Click the link below to reset your password:<br><br>" +
                      "<a href=\"" + resetLink + "\" style=\"background:#2563eb;color:white;padding:12px 24px;text-decoration:none;font-weight:bold;display:inline-block\">Reset Password</a>" +
                      "<br><br><small>This link expires in 1 hour. If you did not request this, ignore this email.</small>")
                .build();
        try {
            resend.emails().send(params);
            log.info("[Email] Reset link sent to {}", toEmail);
            return true;
        } catch (ResendException e) {
            log.error("[Email] Reset email failed to {}: {}", toEmail, e.getMessage());
            return false;
        }
    }
}
