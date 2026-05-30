package com.example.lumoo.domain.vendor;
import com.example.lumoo.domain.user.Notification;
import com.example.lumoo.domain.user.Role;
import com.example.lumoo.domain.user.User;
import com.example.lumoo.domain.vendor.VendorApplication;
import com.example.lumoo.domain.user.NotificationRepository;
import com.example.lumoo.domain.user.UserRepository;
import com.example.lumoo.domain.vendor.VendorApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
@Service
public class VendorApplicationService {
    @Autowired private VendorApplicationRepository applicationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private NotificationRepository notificationRepository;
    public java.util.Optional<VendorApplication> findById(Long id) {
        return applicationRepository.findById(id);
    }
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
    public void apply(User user, VendorApplication app) {
        app.setUser(user);
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
            msg += " You may re-apply after 12 hours. Contact info@lumoo.my for help.";
            notificationRepository.save(new Notification(msg, app.getUser()));
        });
    }
}
