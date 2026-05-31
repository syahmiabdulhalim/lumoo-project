package com.example.lumoo.domain.subscriber;
import com.example.lumoo.domain.subscriber.Subscriber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {
    Optional<Subscriber> findByEmail(String email);
    List<Subscriber> findByActiveTrue();
    @Query("SELECT COUNT(s) FROM Subscriber s WHERE s.active = true AND s.subscribedAt >= :since")
    long countNewSince(@Param("since") LocalDateTime since);
}
