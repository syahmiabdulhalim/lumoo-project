package com.example.lumoo.domain.career;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface CareerApplicationRepository extends JpaRepository<CareerApplication, Long> {
    List<CareerApplication> findByPostingOrderByAppliedAtDesc(CareerPosting posting);
    long countByPosting(CareerPosting posting);
    long countByStatus(CareerApplication.Status status);
}
