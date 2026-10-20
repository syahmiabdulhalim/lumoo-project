package com.example.lumoo.domain.shipping;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;

@Entity
@Table(name = "shipping_rates")
@Getter
@Setter
public class ShippingRate {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String courierName;

    private String courierEmoji = "📦";

    @Enumerated(EnumType.STRING)
    private Coverage coverage = Coverage.NATIONAL;

    private double minWeightKg = 0;
    private double maxWeightKg = 30;
    private double baseRateGmd  = 0;
    private double perKgRateGmd = 0;
    private int estimatedDaysMin = 1;
    private int estimatedDaysMax = 3;

    @Column(length = 500)
    private String trackingUrlTemplate;

    private String description;
    private boolean active = true;
    private int displayOrder = 0;

    public enum Coverage { NATIONAL, INTERNATIONAL, BOTH }

    public double calculateRate(double weightKg) {
        double w = Math.max(weightKg, minWeightKg);
        return Math.round((baseRateGmd + (w * perKgRateGmd)) * 100.0) / 100.0;
    }

    public String buildTrackingUrl(String trackingNumber) {
        if (trackingUrlTemplate == null || trackingUrlTemplate.isBlank()
                || trackingNumber == null || trackingNumber.isBlank()) return null;
        return trackingUrlTemplate.replace("{}", trackingNumber.trim());
    }

}
