package com.example.lumoo.domain.product;

import com.example.lumoo.domain.product.Product;
import com.example.lumoo.domain.product.Review;
import com.example.lumoo.domain.user.User;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProduct(Product product);
    boolean existsByUserAndProduct(User user, Product product);
}