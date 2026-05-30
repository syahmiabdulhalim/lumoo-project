package com.example.lumoo.domain.payment;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "webhook_events")
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
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public LocalDateTime getProcessedAt() { return processedAt; }
}
