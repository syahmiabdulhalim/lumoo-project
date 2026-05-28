package com.example.lumoo.domain.pdpp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "breach_incidents", indexes = {
    @Index(name = "idx_breach_detected", columnList = "detected_at")
})
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

    @Enumerated(EnumType.STRING)
    private Status status = Status.DETECTED;

    @PrePersist
    protected void onCreate() { this.detectedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public LocalDateTime getDetectedAt() { return detectedAt; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAffectedData() { return affectedData; }
    public void setAffectedData(String affectedData) { this.affectedData = affectedData; }
    public int getAffectedUsers() { return affectedUsers; }
    public void setAffectedUsers(int affectedUsers) { this.affectedUsers = affectedUsers; }
    public LocalDateTime getDpaNotifiedAt() { return dpaNotifiedAt; }
    public void setDpaNotifiedAt(LocalDateTime dpaNotifiedAt) { this.dpaNotifiedAt = dpaNotifiedAt; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}
