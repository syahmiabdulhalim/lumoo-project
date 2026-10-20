package com.example.lumoo.domain.career;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "career_postings")
@Getter
@Setter
public class CareerPosting {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String department;
    private String location;
    @Enumerated(EnumType.STRING)
    private JobType type = JobType.FULL_TIME;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(columnDefinition = "TEXT")
    private String requirements;
    private boolean active = true;
    private LocalDateTime createdAt;
    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }
    public enum JobType {
        FULL_TIME, PART_TIME, CONTRACT, INTERNSHIP;
        public String label() {
            return switch (this) {
                case FULL_TIME  -> "Full-time";
                case PART_TIME  -> "Part-time";
                case CONTRACT   -> "Contract";
                case INTERNSHIP -> "Internship";
            };
        }
    }
}
