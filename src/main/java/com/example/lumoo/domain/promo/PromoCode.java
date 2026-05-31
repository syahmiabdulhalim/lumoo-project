package com.example.lumoo.domain.promo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "promo_codes", indexes = {
    @Index(name = "idx_promo_code", columnList = "code", unique = true)
})
public class PromoCode {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    private DiscountType discountType = DiscountType.PERCENTAGE;

    private double discountValue;
    private double minimumOrder = 0;
    private Integer usageLimit;
    private int usedCount = 0;
    private boolean active = true;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }

    public enum DiscountType { PERCENTAGE, FIXED }

    public boolean isValid(double orderTotal) {
        if (!active) return false;
        if (expiresAt != null && expiresAt.isBefore(LocalDateTime.now())) return false;
        if (usageLimit != null && usedCount >= usageLimit) return false;
        if (orderTotal < minimumOrder) return false;
        return true;
    }

    public double apply(double orderTotal) {
        if (discountType == DiscountType.PERCENTAGE) {
            return Math.round(orderTotal * (1 - discountValue / 100) * 100.0) / 100.0;
        }
        return Math.max(0, orderTotal - discountValue);
    }

    public double discountAmount(double orderTotal) {
        return Math.round((orderTotal - apply(orderTotal)) * 100.0) / 100.0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code != null ? code.toUpperCase().trim() : null; }
    public DiscountType getDiscountType() { return discountType; }
    public void setDiscountType(DiscountType discountType) { this.discountType = discountType; }
    public double getDiscountValue() { return discountValue; }
    public void setDiscountValue(double discountValue) { this.discountValue = discountValue; }
    public double getMinimumOrder() { return minimumOrder; }
    public void setMinimumOrder(double minimumOrder) { this.minimumOrder = minimumOrder; }
    public Integer getUsageLimit() { return usageLimit; }
    public void setUsageLimit(Integer usageLimit) { this.usageLimit = usageLimit; }
    public int getUsedCount() { return usedCount; }
    public void setUsedCount(int usedCount) { this.usedCount = usedCount; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
