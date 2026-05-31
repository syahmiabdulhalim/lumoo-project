package com.example.lumoo.domain.shipping;

import jakarta.persistence.*;

@Entity
@Table(name = "shipping_rates")
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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCourierName() { return courierName; }
    public void setCourierName(String courierName) { this.courierName = courierName; }
    public String getCourierEmoji() { return courierEmoji; }
    public void setCourierEmoji(String courierEmoji) { this.courierEmoji = courierEmoji; }
    public Coverage getCoverage() { return coverage; }
    public void setCoverage(Coverage coverage) { this.coverage = coverage; }
    public double getMinWeightKg() { return minWeightKg; }
    public void setMinWeightKg(double minWeightKg) { this.minWeightKg = minWeightKg; }
    public double getMaxWeightKg() { return maxWeightKg; }
    public void setMaxWeightKg(double maxWeightKg) { this.maxWeightKg = maxWeightKg; }
    public double getBaseRateGmd() { return baseRateGmd; }
    public void setBaseRateGmd(double baseRateGmd) { this.baseRateGmd = baseRateGmd; }
    public double getPerKgRateGmd() { return perKgRateGmd; }
    public void setPerKgRateGmd(double perKgRateGmd) { this.perKgRateGmd = perKgRateGmd; }
    public int getEstimatedDaysMin() { return estimatedDaysMin; }
    public void setEstimatedDaysMin(int estimatedDaysMin) { this.estimatedDaysMin = estimatedDaysMin; }
    public int getEstimatedDaysMax() { return estimatedDaysMax; }
    public void setEstimatedDaysMax(int estimatedDaysMax) { this.estimatedDaysMax = estimatedDaysMax; }
    public String getTrackingUrlTemplate() { return trackingUrlTemplate; }
    public void setTrackingUrlTemplate(String trackingUrlTemplate) { this.trackingUrlTemplate = trackingUrlTemplate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
}
