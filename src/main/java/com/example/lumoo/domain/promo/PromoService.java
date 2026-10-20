package com.example.lumoo.domain.promo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PromoService {

    private static final Logger log = LoggerFactory.getLogger(PromoService.class);

    @Autowired private PromoCodeRepository repo;

    public Optional<PromoCode> validate(String code, double orderTotal) {
        return repo.findByCodeIgnoreCase(code)
                .filter(p -> p.isValid(orderTotal));
    }

    /** Discount the code would give on this items total, without using it (checkout preview). */
    public Optional<Double> quote(String code, double itemsTotal) {
        if (code == null || code.isBlank()) return Optional.empty();
        return validate(code.trim(), itemsTotal).map(p -> cappedDiscount(p, itemsTotal));
    }

    /**
     * Validates on the server and takes one use atomically. Runs inside the order-placement
     * transaction, so a failed checkout gives the use back. Empty if invalid or used up.
     */
    @Transactional
    public Optional<Double> redeem(String code, double itemsTotal) {
        if (code == null || code.isBlank()) return Optional.empty();
        return validate(code.trim(), itemsTotal)
                .filter(p -> repo.incrementUsage(p.getId()) == 1)
                .map(p -> {
                    log.info("[Promo] Code '{}' redeemed", p.getCode());
                    return cappedDiscount(p, itemsTotal);
                });
    }

    /** Never more than the items total (e.g. a 150% or oversized fixed discount). */
    private static double cappedDiscount(PromoCode p, double itemsTotal) {
        return Math.min(itemsTotal, Math.max(0, p.discountAmount(itemsTotal)));
    }

    public List<PromoCode> getAll() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    public Optional<PromoCode> findById(Long id) {
        return repo.findById(id);
    }

    public PromoCode save(PromoCode promo) {
        return repo.save(promo);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }

    public void toggle(Long id) {
        repo.findById(id).ifPresent(p -> {
            p.setActive(!p.isActive());
            repo.save(p);
        });
    }
}
