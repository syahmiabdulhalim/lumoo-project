package com.example.lumoo.domain.career;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "career_applications")
@Getter
@Setter
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
}
