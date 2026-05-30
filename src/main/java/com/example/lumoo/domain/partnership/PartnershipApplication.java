package com.example.lumoo.domain.partnership;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "partnership_applications")
public class PartnershipApplication {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String companyName;
    private String contactName;
    private String email;
    private String phone;
    @Enumerated(EnumType.STRING)
    private PartnerType partnerType = PartnerType.OTHER;
    @Column(columnDefinition = "TEXT")
    private String message;
    @Enumerated(EnumType.STRING)
    private Status status = Status.NEW;
    private LocalDateTime submittedAt;
    @PrePersist
    protected void onCreate() { this.submittedAt = LocalDateTime.now(); }
    public enum PartnerType {
        DISTRIBUTOR, RESELLER, SUPPLIER, LOGISTICS, OTHER;
        public String label() {
            return switch (this) {
                case DISTRIBUTOR -> "Distributor";
                case RESELLER    -> "Reseller";
                case SUPPLIER    -> "Supplier";
                case LOGISTICS   -> "Logistics";
                case OTHER       -> "Other";
            };
        }
    }
    public enum Status { NEW, REVIEWED, APPROVED, REJECTED }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public PartnerType getPartnerType() { return partnerType; }
    public void setPartnerType(PartnerType partnerType) { this.partnerType = partnerType; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
}
