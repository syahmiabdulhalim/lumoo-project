package com.example.lumoo.domain.subscriber;
import com.example.lumoo.domain.subscriber.Subscriber;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {
    Optional<Subscriber> findByEmail(String email);
    List<Subscriber> findByActiveTrue();
}
