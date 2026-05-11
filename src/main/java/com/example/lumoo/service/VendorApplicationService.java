package com.example.lumoo.service;

import com.example.lumoo.model.Notification;
import com.example.lumoo.model.Role;
import com.example.lumoo.model.User;
import com.example.lumoo.model.VendorApplication;
import com.example.lumoo.repository.NotificationRepository;
import com.example.lumoo.repository.UserRepository;
import com.example.lumoo.repository.VendorApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class VendorApplicationService {

    @Autowired private VendorApplicationRepository applicationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private NotificationRepository notificationRepository;

    public List<VendorApplication> getByUser(User user) {
        return applicationRepository.findByUserOrderByAppliedAtDesc(user);
    }

    public boolean hasAlreadyApplied(User user) {
        return applicationRepository.existsByUserAndStatus(user, "PENDING");
    }

    public boolean canReapply(User user) {
        List<VendorApplication> apps = getByUser(user);
        if (apps.isEmpty()) return true;
        VendorApplication latest = apps.get(0);
        if ("PENDING".equals(latest.getStatus())) return false;
        if ("REJECTED".equals(latest.getStatus())) {
            if (latest.getReviewedAt() == null) return true;
            return latest.getReviewedAt().plusHours(12).isBefore(LocalDateTime.now());
        }
        return false;
    }

    public void apply(User user, String businessName, String businessType, String phone, String reason) {
        VendorApplication app = new VendorApplication();
        app.setUser(user);
        app.setBusinessName(businessName.trim());
        app.setBusinessType(businessType.trim());
        app.setPhone(phone.trim());
        app.setReason(reason.trim());
        applicationRepository.save(app);
    }

    public List<VendorApplication> getPending() {
        return applicationRepository.findByStatusOrderByAppliedAtDesc("PENDING");
    }

    public void approve(Long id) {
        applicationRepository.findById(id).ifPresent(app -> {
            app.setStatus("APPROVED");
            app.setReviewedAt(LocalDateTime.now());
            applicationRepository.save(app);
            User user = app.getUser();
            user.setRole(Role.VENDOR);
            userRepository.save(user);
            notificationRepository.save(new Notification(
                "🎉 Congratulations! Your vendor application has been approved. Please log out and log back in to access your Vendor Hub.",
                user
            ));
        });
    }

    public void reject(Long id, String note) {
        applicationRepository.findById(id).ifPresent(app -> {
            app.setStatus("REJECTED");
            app.setReviewedAt(LocalDateTime.now());
            if (note != null && !note.isBlank()) app.setRejectionNote(note.trim());
            applicationRepository.save(app);
            String msg = "Your vendor application was not approved.";
            if (note != null && !note.isBlank()) msg += " Reason: " + note.trim();
            msg += " You may re-apply after 12 hours. Contact info@lumoo.gm for help.";
            notificationRepository.save(new Notification(msg, app.getUser()));
        });
    }
}
