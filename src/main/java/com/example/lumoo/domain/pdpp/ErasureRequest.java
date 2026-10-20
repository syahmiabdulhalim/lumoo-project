package com.example.lumoo.domain.pdpp;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "erasure_requests", indexes = {
    @Index(name = "idx_erasure_email", columnList = "email"),
    @Index(name = "idx_erasure_status", columnList = "status")
})
@Getter
@Setter
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
}
