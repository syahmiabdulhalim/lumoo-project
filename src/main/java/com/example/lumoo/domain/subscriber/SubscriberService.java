package com.example.lumoo.domain.subscriber;

import com.example.lumoo.domain.subscriber.Subscriber;
import com.example.lumoo.domain.subscriber.SubscriberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubscriberService {

    @Autowired private SubscriberRepository repo;

    public enum Result { SUBSCRIBED, ALREADY_SUBSCRIBED, INVALID }

    public Result subscribe(String email) {
        if (email == null || !email.contains("@") || email.length() > 254) return Result.INVALID;
        email = email.trim().toLowerCase();
        var existing = repo.findByEmail(email);
        if (existing.isPresent()) {
            if (!existing.get().isActive()) {
                existing.get().setActive(true);
                repo.save(existing.get());
                return Result.SUBSCRIBED;
            }
            return Result.ALREADY_SUBSCRIBED;
        }
        Subscriber s = new Subscriber();
        s.setEmail(email);
        repo.save(s);
        return Result.SUBSCRIBED;
    }

    public List<Subscriber> getAll() { return repo.findAll(); }
    public List<Subscriber> getActive() { return repo.findByActiveTrue(); }

    public void unsubscribe(Long id) {
        repo.findById(id).ifPresent(s -> { s.setActive(false); repo.save(s); });
    }

    public void delete(Long id) { repo.deleteById(id); }
}
