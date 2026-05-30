package com.example.lumoo.domain.partnership;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
@Service
public class PartnershipService {
    @Autowired private PartnershipApplicationRepository repo;
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
