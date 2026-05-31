package com.example.lumoo.domain.payment;
import com.example.lumoo.domain.payment.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, String> {
    boolean existsByEventId(String eventId);
    @Modifying @Transactional
    @Query("DELETE FROM WebhookEvent w WHERE w.processedAt < :cutoff")
    int deleteOldEvents(@Param("cutoff") LocalDateTime cutoff);
}
