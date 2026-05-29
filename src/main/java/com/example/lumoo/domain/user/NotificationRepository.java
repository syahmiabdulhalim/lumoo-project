package com.example.lumoo.domain.user;

import com.example.lumoo.domain.user.Notification;
import com.example.lumoo.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserOrderByCreatedAtDesc(User user);
    long countByUserAndIsReadFalse(User user);
    List<Notification> findByUser(User user);
    List<Notification> findByUserAndIsReadFalse(User user);
}