package com.example.lumoo.domain.order;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import com.example.lumoo.domain.user.User;
import com.example.lumoo.infrastructure.security.EncryptedStringConverter;
import jakarta.persistence.*;
import java.util.List;
import java.time.LocalDateTime;
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_order_user", columnList = "user_id"),
    @Index(name = "idx_order_status", columnList = "status"),
    @Index(name = "idx_order_payout_status", columnList = "payout_status"),
    @Index(name = "idx_order_modempay_payment", columnList = "modempayPaymentId"),
    @Index(name = "idx_order_payout_transfer", columnList = "payoutTransferId"),
    @Index(name = "idx_order_created_at", columnList = "createdAt")
})
@Getter
@Setter
public class Order {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String customerName;
    @Convert(converter = EncryptedStringConverter.class)
    private String address;
    private String paymentMethod;
    private String trackingNumber;
    private LocalDateTime shippedAt;
    private LocalDate estimatedDeliveryDate;
    private String courierName;
    private Double shippingCost;
    @Column(length = 50)
    private String promoCode;
    /** This order's share of the promo discount (items only, never shipping). Funded by Lumoo's commission. */
    @Column(nullable = false)
    private double discountAmount;
    /** Sum of all refunds approved for this order (totalAmount itself never changes). */
    @Column(nullable = false)
    private double refundedAmount;
    @Column(nullable = false)
    private boolean shippingRefunded;
    private String shippingTrackingUrl;
    private Long riderId;
    private String riderName;
    private String riderPhone;
    private String deliveryArea;
    private String returnReason;
    private String paymentProofUrl;
    private String modempayPaymentId;
    private String payoutStatus;
    private String payoutTransferId;
    /** Kept back from this order's payout to cover the vendor's earlier refund debt. */
    @Column(nullable = false)
    private double payoutDeduction;
    private LocalDateTime deliveredAt;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
private LocalDateTime createdAt;
    /** Optimistic lock: webhook, payment sync and admin edits can no longer overwrite each other. */
    @Version
    private Long version;
    private LocalDateTime orderDate;
    private String status;
    private double totalAmount;
    private double adminCommission;
    private double vendorEarnings;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> items;
    private boolean privacyAccepted;
    private boolean termsAccepted;
    private boolean marketingConsent;
    private LocalDateTime consentTimestamp;
    @Column(length = 45)
    private String consentIp;
@PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
