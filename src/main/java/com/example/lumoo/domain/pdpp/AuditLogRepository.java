package com.example.lumoo.domain.pdpp;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    void deleteByCreatedAtBefore(LocalDateTime cutoff);
    Page<AuditLog> findAllByOrderByCreatedAtDesc(
            Pageable pageable);
}
