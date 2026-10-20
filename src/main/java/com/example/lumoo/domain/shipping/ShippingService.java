package com.example.lumoo.domain.shipping;
import java.util.Comparator;
import org.springframework.data.domain.Sort;

import com.example.lumoo.domain.order.CartItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
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
                .sorted(Comparator.comparingDouble(ShippingQuote::rateGmd))
                .collect(Collectors.toList());
    }

    /** Weight of one unit; products without a weight count as 1 kg (same rule as checkout.html). */
    public static double unitWeightKg(CartItem item) {
        Double kg = item.getProduct() != null ? item.getProduct().getWeightKg() : null;
        return kg != null && kg > 0 ? kg : 1.0;
    }

    public static double cartWeightKg(List<CartItem> items) {
        if (items == null || items.isEmpty()) return 1;
        double w = items.stream().mapToDouble(i -> unitWeightKg(i) * i.getQuantity()).sum();
        return Math.round(w * 10) / 10.0;
    }

    /**
     * Server-side price for the courier the buyer picked. Blank or "none" means self collect (free).
     * Empty when the courier is unknown, inactive, or doesn't carry this weight.
     */
    public Optional<Double> quoteFor(String courierName, double weightKg) {
        if (courierName == null || courierName.isBlank() || courierName.equalsIgnoreCase("none")) return Optional.of(0.0);
        return repo.findByActiveTrueOrderByDisplayOrderAscCourierNameAsc().stream()
                .filter(r -> r.getCourierName().equalsIgnoreCase(courierName.trim()))
                .filter(r -> weightKg >= r.getMinWeightKg() && weightKg <= r.getMaxWeightKg())
                .map(r -> r.calculateRate(weightKg))
                .findFirst();
    }

    public List<ShippingRate> getAll() {
        return repo.findAll(Sort.by("displayOrder", "courierName"));
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

    public Optional<ShippingRate> findById(Long id) {
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
