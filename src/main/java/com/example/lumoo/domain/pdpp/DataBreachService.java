package com.example.lumoo.domain.pdpp;
import com.example.lumoo.infrastructure.email.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
/**
 * PDPP breach register. Recording a breach alerts the internal admin only; the regulator is
 * notified solely by an explicit admin action, with the facts the admin entered — never automatically.
 */
@Service
public class DataBreachService {
    private static final Logger log = LoggerFactory.getLogger(DataBreachService.class);
    @Autowired private BreachIncidentRepository breachIncidentRepository;
    @Autowired private AuditService auditService;
    @Autowired private EmailService emailService;
    @Value("${notification.dpa-email:informationcommission@gambia.gov.gm}")
    private String dpaEmail;
    /** Off until the regulator's official address is confirmed; the UI then shows the email for manual sending. */
    @Value("${notification.dpa-enabled:false}")
    private boolean dpaEnabled;
    @Value("${notification.admin-email:admin@lumoo.my}")
    private String adminEmail;

    public enum NotifyResult { SENT, ALREADY_SENT, DISABLED, SEND_FAILED, NOT_FOUND }

    public List<BreachIncident> getAll() {
        return breachIncidentRepository.findAllByOrderByDetectedAtDesc();
    }
    public java.util.Optional<BreachIncident> findById(Long id) {
        return breachIncidentRepository.findById(id);
    }
    public boolean isRegulatorSendingEnabled() { return dpaEnabled; }
    public String getRegulatorEmail() { return dpaEmail; }

    public BreachIncident reportBreach(String description, String affectedData, int estimatedAffectedUsers,
                                       LocalDateTime discoveredAt, String containmentActions, String reportedBy,
                                       HttpServletRequest request) {
        BreachIncident incident = new BreachIncident();
        incident.setDescription(description);
        incident.setAffectedData(affectedData);
        incident.setAffectedUsers(estimatedAffectedUsers);
        incident.setDiscoveredAt(discoveredAt);
        incident.setContainmentActions(containmentActions);
        incident.setReportedBy(reportedBy);
        incident = breachIncidentRepository.save(incident);
        auditService.log("DATA_BREACH_RECORDED", "BreachIncident", incident.getId().toString(),
                null, Map.of(
                        "affectedData", String.valueOf(affectedData),
                        "estimatedAffectedUsers", estimatedAffectedUsers,
                        "reportedBy", String.valueOf(reportedBy)
                ), request);
        emailService.sendBreachAlert(adminEmail, "[URGENT] Data breach recorded — lumoo.my", buildAdminEmail(incident));
        log.error("DATA BREACH RECORDED [id={}] — {} users affected", incident.getId(), estimatedAffectedUsers);
        return incident;
    }

    /** Sends the PDPP s.38 notification for one incident, once, on an admin's explicit request. */
    public NotifyResult notifyRegulator(Long id, String actor, HttpServletRequest request) {
        BreachIncident incident = breachIncidentRepository.findById(id).orElse(null);
        if (incident == null) return NotifyResult.NOT_FOUND;
        if (incident.getDpaNotifiedAt() != null) return NotifyResult.ALREADY_SENT;
        if (!dpaEnabled) return NotifyResult.DISABLED;
        if (!emailService.sendBreachAlert(dpaEmail, "Data Breach Notification — lumoo.my (PDPP 2025)", buildRegulatorEmail(incident)))
            return NotifyResult.SEND_FAILED;
        incident.setDpaNotifiedAt(LocalDateTime.now());
        breachIncidentRepository.save(incident);
        auditService.log("DATA_BREACH_REGULATOR_NOTIFIED", "BreachIncident", id.toString(),
                null, Map.of("to", dpaEmail, "by", String.valueOf(actor)), request);
        return NotifyResult.SENT;
    }

    public BreachIncident updateStatus(Long id, BreachIncident.Status status) {
        BreachIncident incident = breachIncidentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Breach incident not found: " + id));
        incident.setStatus(status);
        return breachIncidentRepository.save(incident);
    }

    private static String esc(Object v) {
        return v == null ? "—" : HtmlUtils.htmlEscape(String.valueOf(v));
    }
    private String buildAdminEmail(BreachIncident i) {
        return "<h2 style='color:red'>⚠ DATA BREACH RECORDED</h2>" +
               "<p><b>Discovered at:</b> " + esc(i.getDiscoveredAt()) + "</p>" +
               "<p><b>Description:</b> " + esc(i.getDescription()) + "</p>" +
               "<p><b>Affected data:</b> " + esc(i.getAffectedData()) + "</p>" +
               "<p><b>Estimated affected users:</b> " + i.getAffectedUsers() + "</p>" +
               "<p><b>Incident ID:</b> " + i.getId() + "</p>" +
               "<p style='color:red'><b>ACTION REQUIRED:</b> Review the incident in Admin → Data Breaches and notify " +
               "the Information Commission before " + esc(i.getRegulatorDeadline()) + " (PDPP 2025 s.38).</p>";
    }
    /** Only facts the admin entered — no pre-written claims about containment or investigation. */
    public String buildRegulatorEmail(BreachIncident i) {
        return "<h2>Data Breach Notification</h2>" +
               "<p>Under the Personal Data Protection and Privacy Act 2025, Section 38</p><hr>" +
               "<p><b>Data Controller:</b> Lumoo E-Commerce (lumoo.my)</p>" +
               "<p><b>Contact:</b> privacy@lumoo.my</p><hr>" +
               "<h3>Breach Details</h3>" +
               "<p><b>Discovered at:</b> " + esc(i.getDiscoveredAt()) + "</p>" +
               "<p><b>Description:</b> " + esc(i.getDescription()) + "</p>" +
               "<p><b>Data affected:</b> " + esc(i.getAffectedData()) + "</p>" +
               "<p><b>Estimated affected users:</b> " + i.getAffectedUsers() + "</p>" +
               "<h3>Measures taken</h3><p>" + esc(i.getContainmentActions()) + "</p>" +
               "<p><b>Reference:</b> Incident #" + i.getId() + "</p>";
    }
}
