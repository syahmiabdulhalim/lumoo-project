package com.example.lumoo.domain.pdpp;

import com.example.lumoo.infrastructure.email.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class DataBreachService {

    private static final Logger log = LoggerFactory.getLogger(DataBreachService.class);

    @Autowired private BreachIncidentRepository breachIncidentRepository;
    @Autowired private AuditService auditService;
    @Autowired private EmailService emailService;

    @Value("${notification.dpa-email:informationcommission@gambia.gov.gm}")
    private String dpaEmail;

    @Value("${notification.admin-email:admin@lumoo.my}")
    private String adminEmail;

    /**
     * Call immediately when a potential breach is detected.
     * PDPP 2025 s.38 — notify DPA within 72 hours.
     */
    public BreachIncident reportBreach(String description, String affectedData,
                                       int estimatedAffectedUsers, HttpServletRequest request) {
        BreachIncident incident = new BreachIncident();
        incident.setDescription(description);
        incident.setAffectedData(affectedData);
        incident.setAffectedUsers(estimatedAffectedUsers);
        incident = breachIncidentRepository.save(incident);

        auditService.log("DATA_BREACH_DETECTED", "System", incident.getId().toString(),
                null, Map.of(
                        "description", description,
                        "affectedData", affectedData,
                        "estimatedAffectedUsers", estimatedAffectedUsers,
                        "detectedAt", LocalDateTime.now().toString()
                ), request);

        // Alert admin immediately
        emailService.sendBreachAlert(adminEmail,
                "[URGENT] Data Breach Detected — lumoo.my",
                buildAdminEmail(incident));

        // Notify Gambia Information Commission (PDPP s.38 — within 72 hours)
        boolean dpaNotified = emailService.sendBreachAlert(dpaEmail,
                "Data Breach Notification — lumoo.my (PDPP 2025)",
                buildDpaEmail(incident));

        if (dpaNotified) {
            incident.setDpaNotifiedAt(LocalDateTime.now());
            incident = breachIncidentRepository.save(incident);
        }

        log.error("DATA BREACH REPORTED [id={}]: {} — {} users affected",
                incident.getId(), description, estimatedAffectedUsers);

        return incident;
    }

    public BreachIncident updateStatus(Long id, BreachIncident.Status status) {
        BreachIncident incident = breachIncidentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Breach incident not found: " + id));
        incident.setStatus(status);
        return breachIncidentRepository.save(incident);
    }

    private String buildAdminEmail(BreachIncident i) {
        return "<h2 style='color:red'>⚠ DATA BREACH DETECTED</h2>" +
               "<p><b>Detected at:</b> " + i.getDetectedAt() + "</p>" +
               "<p><b>Description:</b> " + i.getDescription() + "</p>" +
               "<p><b>Affected data:</b> " + i.getAffectedData() + "</p>" +
               "<p><b>Estimated affected users:</b> " + i.getAffectedUsers() + "</p>" +
               "<p><b>Incident ID:</b> " + i.getId() + "</p>" +
               "<p style='color:red'><b>ACTION REQUIRED:</b> Notify the Gambia Information Commission " +
               "within 72 hours per PDPP 2025 s.38 if not already done automatically.</p>";
    }

    private String buildDpaEmail(BreachIncident i) {
        return "<h2>Data Breach Notification</h2>" +
               "<p>Under Personal Data Protection and Privacy Act 2025, Section 38</p>" +
               "<hr>" +
               "<p><b>Data Controller:</b> Lumoo E-Commerce (lumoo.my)</p>" +
               "<p><b>Contact:</b> privacy@lumoo.my</p>" +
               "<hr>" +
               "<h3>Breach Details</h3>" +
               "<p><b>Detected at:</b> " + i.getDetectedAt() + "</p>" +
               "<p><b>Description:</b> " + i.getDescription() + "</p>" +
               "<p><b>Data affected:</b> " + i.getAffectedData() + "</p>" +
               "<p><b>Estimated affected users:</b> " + i.getAffectedUsers() + "</p>" +
               "<h3>Immediate Actions Taken</h3>" +
               "<ul><li>Breach isolated and contained</li>" +
               "<li>Internal investigation commenced</li>" +
               "<li>This notification sent within 72-hour requirement</li></ul>" +
               "<p>Full incident report to follow within 30 days.</p>";
    }
}
