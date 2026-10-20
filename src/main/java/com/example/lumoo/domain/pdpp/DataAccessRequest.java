package com.example.lumoo.domain.pdpp;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "data_access_requests", indexes = {
    @Index(name = "idx_dar_email", columnList = "email")
})
@Getter
@Setter
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
}
