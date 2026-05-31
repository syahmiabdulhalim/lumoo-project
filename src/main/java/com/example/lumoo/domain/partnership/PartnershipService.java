package com.example.lumoo.domain.partnership;
import com.example.lumoo.domain.admin.SiteSettingsService;
import com.example.lumoo.infrastructure.email.EmailService;
import com.example.lumoo.infrastructure.email.EmailTemplates;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
@Service
public class PartnershipService {
    private static final Logger log = LoggerFactory.getLogger(PartnershipService.class);

    @Autowired private PartnershipApplicationRepository repo;
    @Autowired private EmailService emailService;
    @Autowired private SiteSettingsService siteSettingsService;
    public List<PartnershipApplication> getAll() {
        return repo.findAllByOrderBySubmittedAtDesc();
    }
    public Optional<PartnershipApplication> findById(Long id) {
        return repo.findById(id);
    }
    public void submit(String companyName, String contactName, String email,
                       String phone, PartnershipApplication.PartnerType type, String message) {
        PartnershipApplication app = new PartnershipApplication();
        app.setCompanyName(companyName);
        app.setContactName(contactName);
        app.setEmail(email);
        app.setPhone(phone);
        app.setPartnerType(type);
        app.setMessage(message);
        repo.save(app);
        String adminEmail = siteSettingsService.get().getBusinessEmail();
        if (adminEmail != null && !adminEmail.isBlank()) {
            emailService.sendEmail(adminEmail,
                    "New partnership application — " + companyName,
                    EmailTemplates.newPartnershipApplication(companyName, contactName, email, type.label()));
        }
    }
    public void updateStatus(Long id, PartnershipApplication.Status status) {
        repo.findById(id).ifPresent(app -> {
            app.setStatus(status);
            repo.save(app);
        });
    }
    public long countNew() {
        return repo.countByStatus(PartnershipApplication.Status.NEW);
    }
}
