package com.example.lumoo.domain.pdpp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "erasure_requests", indexes = {
    @Index(name = "idx_erasure_email", columnList = "email"),
    @Index(name = "idx_erasure_status", columnList = "status")
})
public class ErasureRequest {

    public enum Status { PENDING, PROCESSING, COMPLETED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;
    private int ordersAffected;

    @Column(unique = true, nullable = false, length = 36)
    private String referenceId;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    @PrePersist
    protected void onCreate() { this.requestedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    public int getOrdersAffected() { return ordersAffected; }
    public void setOrdersAffected(int ordersAffected) { this.ordersAffected = ordersAffected; }
    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}
