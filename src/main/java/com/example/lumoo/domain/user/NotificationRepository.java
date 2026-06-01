package com.example.lumoo.domain.user;
import com.example.lumoo.domain.user.Notification;
import com.example.lumoo.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserOrderByCreatedAtDesc(User user);
    List<Notification> findByUserAndIdLessThanOrderByCreatedAtDesc(User user, Long beforeId, org.springframework.data.domain.Pageable pageable);
    List<Notification> findByUserOrderByCreatedAtDesc(User user, org.springframework.data.domain.Pageable pageable);
    long countByUserAndIsReadFalse(User user);
    List<Notification> findByUser(User user);
    List<Notification> findByUserAndIsReadFalse(User user);
    @Modifying
    @Transactional
    @Query("DELETE FROM Notification n WHERE n.isRead = true AND n.createdAt < :cutoff")
    int deleteOldRead(@Param("cutoff") LocalDateTime cutoff);
}
