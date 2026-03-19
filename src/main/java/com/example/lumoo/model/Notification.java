package com.example.lumoo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity 
@Table(name = "notifications")
public class Notification {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String message;
    
    @Column(name = "is_read")
    private boolean isRead = false;

    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // --- CONSTRUCTORS ---
    public Notification() {}

    public Notification(String message, User user) {
        this.message = message;
        this.user = user;
        this.createdAt = LocalDateTime.now();
        this.isRead = false;
    }

    // --- GETTERS ---
    public Long getId() { return id; }
    public String getMessage() { return message; }
    public boolean isRead() { return isRead; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public User getUser() { return user; }

    // --- SETTERS ---
    public void setId(Long id) { this.id = id; }
    public void setMessage(String message) { this.message = message; }
    public void setRead(boolean read) { isRead = read; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUser(User user) { this.user = user; }
}