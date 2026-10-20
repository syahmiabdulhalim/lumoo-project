package com.example.lumoo.domain.partnership;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "partnership_applications")
@Getter
@Setter
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
}
