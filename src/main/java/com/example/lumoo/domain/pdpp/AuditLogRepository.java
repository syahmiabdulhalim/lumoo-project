package com.example.lumoo.domain.pdpp;
import com.example.lumoo.domain.pdpp.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByActorIdOrderByCreatedAtDesc(String actorId);
    List<AuditLog> findByActionOrderByCreatedAtDesc(String action);
    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, String entityId);
    void deleteByCreatedAtBefore(LocalDateTime cutoff);
    org.springframework.data.domain.Page<AuditLog> findAllByOrderByCreatedAtDesc(
            org.springframework.data.domain.Pageable pageable);
}
