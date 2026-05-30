package com.example.lumoo.domain.payment;
import com.example.lumoo.domain.payment.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, String> {
    boolean existsByEventId(String eventId);
}
