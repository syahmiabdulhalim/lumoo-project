package com.example.lumoo.domain.pdpp;

import com.example.lumoo.domain.pdpp.BreachIncident;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BreachIncidentRepository extends JpaRepository<BreachIncident, Long> {
    List<BreachIncident> findByStatusOrderByDetectedAtDesc(BreachIncident.Status status);
    List<BreachIncident> findAllByOrderByDetectedAtDesc();
}
