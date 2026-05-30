package com.example.lumoo.domain.partnership;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PartnershipApplicationRepository extends JpaRepository<PartnershipApplication, Long> {
    List<PartnershipApplication> findAllByOrderBySubmittedAtDesc();
    long countByStatus(PartnershipApplication.Status status);
}
