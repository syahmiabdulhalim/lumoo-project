package com.example.lumoo.domain.promo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;

public interface PromoCodeRepository extends JpaRepository<PromoCode, Long> {
    /** Atomic: two checkouts can't both take the last remaining use. Returns 1 if a use was taken. */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE PromoCode p SET p.usedCount = p.usedCount + 1 WHERE p.id = :id AND (p.usageLimit IS NULL OR p.usedCount < p.usageLimit)")
    int incrementUsage(@Param("id") Long id);
    Optional<PromoCode> findByCodeIgnoreCase(String code);
    List<PromoCode> findAllByOrderByCreatedAtDesc();
}
