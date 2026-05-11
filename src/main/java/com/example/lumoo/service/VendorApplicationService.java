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

import java.util.List;

@Service
public class VendorApplicationService {

    @Autowired private VendorApplicationRepository applicationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private NotificationRepository notificationRepository;

    public boolean hasAlreadyApplied(User user) {
        return applicationRepository.existsByUserAndStatus(user, "PENDING");
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

    public void reject(Long id) {
        applicationRepository.findById(id).ifPresent(app -> {
            app.setStatus("REJECTED");
            applicationRepository.save(app);
            notificationRepository.save(new Notification(
                "Your vendor application has been reviewed. Unfortunately it was not approved at this time. Contact us at info@lumoo.gm for more information.",
                app.getUser()
            ));
        });
    }
}
