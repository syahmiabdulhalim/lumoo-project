package com.example.lumoo.domain.pdpp;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "breach_incidents", indexes = {
    @Index(name = "idx_breach_detected", columnList = "detected_at")
})
@Getter
@Setter
public class BreachIncident {
    public enum Status { DETECTED, CONTAINED, RESOLVED }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime detectedAt;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;
    @Column(columnDefinition = "TEXT")
    private String affectedData;
    private int affectedUsers;
    private LocalDateTime dpaNotifiedAt;
    /** When Lumoo became aware of the breach — the PDPP 72-hour notification clock starts here. */
    private LocalDateTime discoveredAt;
    @Column(columnDefinition = "TEXT")
    private String containmentActions;
    private String reportedBy;
    @Enumerated(EnumType.STRING)
    private Status status = Status.DETECTED;
    @PrePersist
    protected void onCreate() {
        this.detectedAt = LocalDateTime.now();
        if (this.discoveredAt == null) this.discoveredAt = this.detectedAt;
    }
    /** PDPP: the regulator must be told within 72 hours of discovery. */
    public LocalDateTime getRegulatorDeadline() {
        LocalDateTime start = discoveredAt != null ? discoveredAt : detectedAt;
        return start == null ? null : start.plusHours(72);
    }
    public boolean isRegulatorOverdue() {
        LocalDateTime d = getRegulatorDeadline();
        return dpaNotifiedAt == null && d != null && d.isBefore(LocalDateTime.now());
    }
}
