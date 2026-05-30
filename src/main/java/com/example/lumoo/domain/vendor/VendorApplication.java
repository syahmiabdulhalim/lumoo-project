package com.example.lumoo.domain.vendor;
import com.example.lumoo.domain.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "vendor_applications")
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
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }
    public String getBusinessRegNumber() { return businessRegNumber; }
    public void setBusinessRegNumber(String businessRegNumber) { this.businessRegNumber = businessRegNumber; }
    public String getTinNumber() { return tinNumber; }
    public void setTinNumber(String tinNumber) { this.tinNumber = tinNumber; }
    public String getYearsInBusiness() { return yearsInBusiness; }
    public void setYearsInBusiness(String yearsInBusiness) { this.yearsInBusiness = yearsInBusiness; }
    public String getEstimatedMonthlyTurnover() { return estimatedMonthlyTurnover; }
    public void setEstimatedMonthlyTurnover(String estimatedMonthlyTurnover) { this.estimatedMonthlyTurnover = estimatedMonthlyTurnover; }
    public String getBusinessAddress() { return businessAddress; }
    public void setBusinessAddress(String businessAddress) { this.businessAddress = businessAddress; }
    public String getBusinessRegion() { return businessRegion; }
    public void setBusinessRegion(String businessRegion) { this.businessRegion = businessRegion; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getOwnerFullName() { return ownerFullName; }
    public void setOwnerFullName(String ownerFullName) { this.ownerFullName = ownerFullName; }
    public String getOwnerIdType() { return ownerIdType; }
    public void setOwnerIdType(String ownerIdType) { this.ownerIdType = ownerIdType; }
    public String getOwnerIdNumber() { return ownerIdNumber; }
    public void setOwnerIdNumber(String ownerIdNumber) { this.ownerIdNumber = ownerIdNumber; }
    public String getOwnerAddress() { return ownerAddress; }
    public void setOwnerAddress(String ownerAddress) { this.ownerAddress = ownerAddress; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getBankAccountNumber() { return bankAccountNumber; }
    public void setBankAccountNumber(String bankAccountNumber) { this.bankAccountNumber = bankAccountNumber; }
    public String getBankAccountName() { return bankAccountName; }
    public void setBankAccountName(String bankAccountName) { this.bankAccountName = bankAccountName; }
    public String getMobileMoneyNumber() { return mobileMoneyNumber; }
    public void setMobileMoneyNumber(String mobileMoneyNumber) { this.mobileMoneyNumber = mobileMoneyNumber; }
    public String getProductsToSell() { return productsToSell; }
    public void setProductsToSell(String productsToSell) { this.productsToSell = productsToSell; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getNationalIdDocUrl() { return nationalIdDocUrl; }
    public void setNationalIdDocUrl(String nationalIdDocUrl) { this.nationalIdDocUrl = nationalIdDocUrl; }
    public String getBusinessRegDocUrl() { return businessRegDocUrl; }
    public void setBusinessRegDocUrl(String businessRegDocUrl) { this.businessRegDocUrl = businessRegDocUrl; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public String getRejectionNote() { return rejectionNote; }
    public void setRejectionNote(String rejectionNote) { this.rejectionNote = rejectionNote; }
}
