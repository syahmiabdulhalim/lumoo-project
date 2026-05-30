package com.example.lumoo.domain.career;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "career_applications")
public class CareerApplication {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "posting_id")
    private CareerPosting posting;
    private String applicantName;
    private String email;
    private String phone;
    @Column(columnDefinition = "TEXT")
    private String coverLetter;
    @Enumerated(EnumType.STRING)
    private Status status = Status.NEW;
    private LocalDateTime appliedAt;
    @PrePersist
    protected void onCreate() { this.appliedAt = LocalDateTime.now(); }
    public enum Status { NEW, REVIEWED, SHORTLISTED, REJECTED }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public CareerPosting getPosting() { return posting; }
    public void setPosting(CareerPosting posting) { this.posting = posting; }
    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getCoverLetter() { return coverLetter; }
    public void setCoverLetter(String coverLetter) { this.coverLetter = coverLetter; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }
}
