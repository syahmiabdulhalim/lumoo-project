package com.example.lumoo.repository;

import com.example.lumoo.model.Product;
import com.example.lumoo.model.Review;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
   List<Review> findByProduct(Product product);
}