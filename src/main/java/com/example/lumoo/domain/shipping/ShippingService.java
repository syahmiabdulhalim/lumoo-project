package com.example.lumoo.domain.shipping;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShippingService {

    @Autowired private ShippingRateRepository repo;

    public List<ShippingQuote> getQuotes(double weightKg, ShippingRate.Coverage coverage) {
        List<ShippingRate.Coverage> filter = coverage == ShippingRate.Coverage.NATIONAL
                ? List.of(ShippingRate.Coverage.NATIONAL, ShippingRate.Coverage.BOTH)
                : List.of(ShippingRate.Coverage.INTERNATIONAL, ShippingRate.Coverage.BOTH);

        return repo.findByActiveTrueAndCoverageInOrderByDisplayOrderAsc(filter).stream()
                .filter(r -> weightKg >= r.getMinWeightKg() && weightKg <= r.getMaxWeightKg())
                .map(r -> new ShippingQuote(
                        r.getId(),
                        r.getCourierName(),
                        r.getCourierEmoji(),
                        r.calculateRate(weightKg),
                        r.getEstimatedDaysMin(),
                        r.getEstimatedDaysMax(),
                        r.getDescription(),
                        r.getTrackingUrlTemplate() != null && !r.getTrackingUrlTemplate().isBlank()
                ))
                .sorted(java.util.Comparator.comparingDouble(ShippingQuote::rateGmd))
                .collect(Collectors.toList());
    }

    public List<ShippingRate> getAll() {
        return repo.findAll(org.springframework.data.domain.Sort.by("displayOrder", "courierName"));
    }

    public ShippingRate save(ShippingRate rate) {
        return repo.save(rate);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }

    public void toggle(Long id) {
        repo.findById(id).ifPresent(r -> { r.setActive(!r.isActive()); repo.save(r); });
    }

    public java.util.Optional<ShippingRate> findById(Long id) {
        return repo.findById(id);
    }

    public record ShippingQuote(
        Long rateId,
        String courierName,
        String emoji,
        double rateGmd,
        int daysMin,
        int daysMax,
        String description,
        boolean hasTracking
    ) {}
}
