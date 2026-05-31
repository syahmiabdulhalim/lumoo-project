package com.example.lumoo.domain.career;
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
public class CareerService {
    private static final Logger log = LoggerFactory.getLogger(CareerService.class);

    @Autowired private CareerPostingRepository postingRepo;
    @Autowired private CareerApplicationRepository applicationRepo;
    @Autowired private EmailService emailService;
    @Autowired private SiteSettingsService siteSettingsService;
    public List<CareerPosting> getAllPostings() {
        return postingRepo.findAllByOrderByCreatedAtDesc();
    }
    public List<CareerPosting> getActivePostings() {
        return postingRepo.findByActiveTrueOrderByCreatedAtDesc();
    }
    public Optional<CareerPosting> findPostingById(Long id) {
        return postingRepo.findById(id);
    }
    public CareerPosting savePosting(CareerPosting posting) {
        return postingRepo.save(posting);
    }
    public void deletePosting(Long id) {
        postingRepo.deleteById(id);
    }
    public void submitApplication(Long postingId, String name, String email, String phone, String coverLetter) {
        CareerPosting posting = postingRepo.findById(postingId).orElse(null);
        if (posting == null || !posting.isActive()) return;
        CareerApplication app = new CareerApplication();
        app.setPosting(posting);
        app.setApplicantName(name);
        app.setEmail(email);
        app.setPhone(phone);
        app.setCoverLetter(coverLetter);
        applicationRepo.save(app);
        String adminEmail = siteSettingsService.get().getBusinessEmail();
        if (adminEmail != null && !adminEmail.isBlank()) {
            emailService.sendEmail(adminEmail,
                    "New career application — " + posting.getTitle(),
                    EmailTemplates.newCareerApplication(adminEmail, posting.getTitle(), name, email));
        }
    }
    public List<CareerApplication> getApplicationsForPosting(CareerPosting posting) {
        return applicationRepo.findByPostingOrderByAppliedAtDesc(posting);
    }
    public Optional<CareerApplication> findApplicationById(Long id) {
        return applicationRepo.findById(id);
    }
    public void updateApplicationStatus(Long id, CareerApplication.Status status) {
        applicationRepo.findById(id).ifPresent(app -> {
            app.setStatus(status);
            applicationRepo.save(app);
        });
    }
    public long countApplicationsForPosting(CareerPosting posting) {
        return applicationRepo.countByPosting(posting);
    }
    public long countNewApplications() {
        return applicationRepo.countByStatus(CareerApplication.Status.NEW);
    }
}
