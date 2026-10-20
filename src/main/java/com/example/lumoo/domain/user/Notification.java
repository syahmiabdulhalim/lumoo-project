package com.example.lumoo.domain.user;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity 
@Table(name = "notifications")
@Getter
@Setter
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
    public Notification() {}
    public Notification(String message, User user) {
        this.message = message;
        this.user = user;
        this.createdAt = LocalDateTime.now();
        this.isRead = false;
    }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
}
