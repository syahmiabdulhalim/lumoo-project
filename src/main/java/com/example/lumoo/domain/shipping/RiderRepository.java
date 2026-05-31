package com.example.lumoo.domain.shipping;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RiderRepository extends JpaRepository<Rider, Long> {
    List<Rider> findByActiveTrueOrderByNameAsc();
}
