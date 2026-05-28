package com.example.lumoo.domain.subscriber;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscribers")
public class Subscriber {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    private LocalDateTime subscribedAt;
    private boolean active = true;

    @PrePersist
    protected void onCreate() { this.subscribedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public LocalDateTime getSubscribedAt() { return subscribedAt; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
