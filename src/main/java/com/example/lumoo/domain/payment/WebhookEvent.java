package com.example.lumoo.domain.payment;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "webhook_events")
@Getter
@Setter
public class WebhookEvent {
    @Id
    @Column(nullable = false, length = 100)
    private String eventId;
    @Column(nullable = false, length = 100)
    private String eventType;
    private Long orderId;
    @Column(nullable = false)
    private LocalDateTime processedAt;
    @PrePersist
    protected void onCreate() { this.processedAt = LocalDateTime.now(); }
}
