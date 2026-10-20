package com.example.lumoo.domain.promo;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "promo_codes", indexes = {
    @Index(name = "idx_promo_code", columnList = "code", unique = true)
})
@Getter
@Setter
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

    public void setCode(String code) { this.code = code != null ? code.toUpperCase().trim() : null; }
}
