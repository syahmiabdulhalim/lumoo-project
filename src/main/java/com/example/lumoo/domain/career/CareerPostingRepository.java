package com.example.lumoo.domain.career;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface CareerPostingRepository extends JpaRepository<CareerPosting, Long> {
    List<CareerPosting> findByActiveTrueOrderByCreatedAtDesc();
    List<CareerPosting> findAllByOrderByCreatedAtDesc();
}
