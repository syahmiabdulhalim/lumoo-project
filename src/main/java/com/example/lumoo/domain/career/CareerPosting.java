package com.example.lumoo.domain.career;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "career_postings")
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
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public JobType getType() { return type; }
    public void setType(JobType type) { this.type = type; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRequirements() { return requirements; }
    public void setRequirements(String requirements) { this.requirements = requirements; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
