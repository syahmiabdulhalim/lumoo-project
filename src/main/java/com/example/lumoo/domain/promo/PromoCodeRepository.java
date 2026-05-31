package com.example.lumoo.domain.promo;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface PromoCodeRepository extends JpaRepository<PromoCode, Long> {
    Optional<PromoCode> findByCodeIgnoreCase(String code);
    List<PromoCode> findAllByOrderByCreatedAtDesc();
}
