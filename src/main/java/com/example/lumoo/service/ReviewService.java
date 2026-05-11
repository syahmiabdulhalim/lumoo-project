package com.example.lumoo.service;

import com.example.lumoo.model.Product;
import com.example.lumoo.model.Review;
import com.example.lumoo.model.User;
import com.example.lumoo.repository.OrderItemRepository;
import com.example.lumoo.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReviewService {

    @Autowired private ReviewRepository reviewRepository;
    @Autowired private OrderItemRepository orderItemRepository;

    public List<Review> getByProduct(Product product) {
        return reviewRepository.findByProduct(product);
    }

    public double getAverageRating(List<Review> reviews) {
        return reviews.isEmpty() ? 0 : reviews.stream().mapToInt(Review::getRating).average().orElse(0);
    }

    public boolean canReview(User user, Product product) {
        return orderItemRepository.existsByOrderUserAndOrderStatusAndProduct(user, "DELIVERED", product);
    }

    public boolean hasAlreadyReviewed(User user, Product product) {
        return reviewRepository.existsByUserAndProduct(user, product);
    }

    public void addReview(Product product, User user, int rating, String comment) {
        Review review = new Review();
        review.setUser(user);
        review.setReviewerName(user.getFullName() != null && !user.getFullName().isBlank()
                ? user.getFullName() : user.getEmail());
        review.setRating(rating);
        review.setComment(comment.trim());
        review.setProduct(product);
        review.setCreatedAt(LocalDateTime.now());
        reviewRepository.save(review);
    }
}
