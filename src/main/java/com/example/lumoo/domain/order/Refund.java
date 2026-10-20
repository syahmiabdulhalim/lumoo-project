package com.example.lumoo.domain.order;
import com.example.lumoo.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;
/**
 * A refund Lumoo owes (PENDING) or has paid (COMPLETED) to a buyer. The money moves outside the
 * system (ModemPay dashboard, wallet or bank transfer); the reference of that transfer is recorded here.
 */
@Entity
@Table(name = "refunds", indexes = {
    @Index(name = "idx_refund_status", columnList = "status"),
    @Index(name = "idx_refund_order", columnList = "order_id"),
    @Index(name = "idx_refund_vendor", columnList = "vendor_id")
})
@Getter
@Setter
public class Refund {
    public enum Status { PENDING, COMPLETED }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private User vendor;
    /** What the buyer gets back. */
    @Column(nullable = false)
    private double amount;
    @Column(nullable = false)
    private double shippingAmount;
    /** The vendor's share of the refund (their earnings on the refunded items + shipping). */
    @Column(nullable = false)
    private double vendorDeduction;
    /** Part of vendorDeduction the vendor had already been paid; recovered from their next payouts. */
    @Column(nullable = false)
    private double vendorDebt;
    @Column(length = 500)
    private String itemsSummary;
    @Column(nullable = false)
    private String reason;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;
    @Column(length = 50)
    private String method;
    @Column(length = 100)
    private String reference;
    private String createdBy;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    /** Refund policy: paid within 14 business days. */
    private LocalDate dueBy;
    private String completedBy;
    private LocalDateTime completedAt;
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
    public boolean isOverdue() {
        return status == Status.PENDING && dueBy != null && LocalDate.now().isAfter(dueBy);
    }
}
