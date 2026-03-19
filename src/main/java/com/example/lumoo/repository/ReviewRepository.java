package com.example.lumoo.repository;

import com.example.lumoo.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    // Repository ini membolehkan fungsi CRUD untuk entiti Inquiry
}