package com.example.lumoo.domain.pdpp;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface BreachIncidentRepository extends JpaRepository<BreachIncident, Long> {
    List<BreachIncident> findAllByOrderByDetectedAtDesc();
}
