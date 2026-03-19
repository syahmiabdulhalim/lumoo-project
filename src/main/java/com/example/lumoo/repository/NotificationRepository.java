package com.example.lumoo.repository;

import com.example.lumoo.model.Notification;
import com.example.lumoo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserOrderByCreatedAtDesc(User user);
    long countByUserAndIsReadFalse(User user);
    List<Notification> findByUser(User user);
}