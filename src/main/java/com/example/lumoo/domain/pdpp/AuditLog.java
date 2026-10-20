package com.example.lumoo.domain.pdpp;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_action", columnList = "action"),
    @Index(name = "idx_audit_actor", columnList = "actor_id"),
    @Index(name = "idx_audit_created", columnList = "created_at")
})
@Getter
@Setter
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String action;
    @Column(length = 100)
    private String entityType;
    private String entityId;
    private String actorId;
    @Column(columnDefinition = "TEXT")
    private String beforeState;
    @Column(columnDefinition = "TEXT")
    private String afterState;
    @Column(length = 45)
    private String ipAddress;
    @Column(length = 500)
    private String userAgent;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }
}
