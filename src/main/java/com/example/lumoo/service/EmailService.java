package com.example.lumoo.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Value("${resend.api.key}") 
    private String apiKey;

    @Value("${app.base-url:http://localhost:8080}")
private String baseUrl;

    public boolean sendResetEmail(String toEmail, String token) {
        Resend resend = new Resend(apiKey);

        String resetLink = baseUrl + "/reset-password?token=" + token;
        CreateEmailOptions params = CreateEmailOptions.builder()
                .from("LUMOO <onboarding@resend.dev>")
                .to(toEmail)
                .subject("Reset Your LUMOO Password")
                .html("<strong>Build your Future.</strong><br><br>" +
                      "Click the link below to reset your password:<br><br>" +
                      "<a href=\"" + resetLink + "\" style=\"background:#2563eb;color:white;padding:12px 24px;text-decoration:none;font-weight:bold;display:inline-block\">Reset Password</a>" +
                      "<br><br><small>This link expires in 1 hour. If you did not request this, ignore this email.</small>")
                .build();

        try {
            resend.emails().send(params);
            return true;
        } catch (ResendException e) {
            System.err.println("Resend error for " + toEmail + ": " + e.getMessage());
            return false;
        }
    }
}
