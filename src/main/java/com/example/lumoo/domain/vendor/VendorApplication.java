package com.example.lumoo.domain.vendor;
import lombok.Getter;
import lombok.Setter;
import com.example.lumoo.domain.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "vendor_applications")
@Getter
@Setter
public class VendorApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    private String businessName;
    private String businessType;
    private String businessRegNumber;
    private String tinNumber;
    private String yearsInBusiness;
    private String estimatedMonthlyTurnover;
    @Column(columnDefinition = "TEXT")
    private String businessAddress;
    private String businessRegion;
    private String phone;
    private String ownerFullName;
    private String ownerIdType;
    private String ownerIdNumber;
    @Column(columnDefinition = "TEXT")
    private String ownerAddress;
    private String bankName;
    private String bankAccountNumber;
    private String bankAccountName;
    private String mobileMoneyNumber;
    @Column(columnDefinition = "TEXT")
    private String productsToSell;
    @Column(columnDefinition = "TEXT")
    private String reason;
    private String nationalIdDocUrl;
    private String businessRegDocUrl;
    private String status; 
    private LocalDateTime appliedAt;
    private LocalDateTime reviewedAt;
    @Column(columnDefinition = "TEXT")
    private String rejectionNote;
    @PrePersist
    protected void onCreate() {
        this.appliedAt = LocalDateTime.now();
        this.status = "PENDING";
    }
}
