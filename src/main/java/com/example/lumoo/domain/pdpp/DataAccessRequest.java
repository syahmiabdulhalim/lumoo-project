package com.example.lumoo.domain.pdpp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "data_access_requests", indexes = {
    @Index(name = "idx_dar_email", columnList = "email")
})
public class DataAccessRequest {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    private LocalDateTime requestedAt;
    private LocalDateTime fulfilledAt;

    @Column(unique = true, nullable = false, length = 36)
    private String referenceId;

    @PrePersist
    protected void onCreate() { this.requestedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public LocalDateTime getFulfilledAt() { return fulfilledAt; }
    public void setFulfilledAt(LocalDateTime fulfilledAt) { this.fulfilledAt = fulfilledAt; }
    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
}
