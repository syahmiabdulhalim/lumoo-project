package com.example.lumoo.domain.promo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public void markUsed(PromoCode promo) {
        promo.setUsedCount(promo.getUsedCount() + 1);
        repo.save(promo);
        log.info("[Promo] Code '{}' used — total uses: {}", promo.getCode(), promo.getUsedCount());
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
