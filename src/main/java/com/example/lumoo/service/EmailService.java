package com.example.lumoo.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Value("${resend.api.key}")
    private String apiKey;

    public void sendResetEmail(String toEmail, String token) {
        Resend resend = new Resend(apiKey);

        String resetLink = "http://localhost:8080/reset-password?token=" + token;

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from("LUMOO <onboarding@resend.dev>") // Gunakan domain resend.dev untuk testing localhost
                .to(toEmail)
                .subject("Reset Your LUMOO Password")
                .html("<strong>Build your Future.</strong><br><br>" +
                      "Click the link below to reset your password:<br>" +
                      "<a href=\"" + resetLink + "\">Reset Password</a>")
                .build();

        try {
            resend.emails().send(params);
            System.out.println("Email sent successfully to " + toEmail);
        } catch (ResendException e) {
            e.printStackTrace();
        }
    }
}