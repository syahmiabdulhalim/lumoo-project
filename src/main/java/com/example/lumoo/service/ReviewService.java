package com.example.lumoo.service;

import com.example.lumoo.model.Product;
import com.example.lumoo.model.Review;
import com.example.lumoo.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReviewService {

    @Autowired private ReviewRepository reviewRepository;

    public List<Review> getByProduct(Product product) {
        return reviewRepository.findByProduct(product);
    }

    public double getAverageRating(List<Review> reviews) {
        return reviews.isEmpty() ? 0 : reviews.stream().mapToInt(Review::getRating).average().orElse(0);
    }

    public void addReview(Product product, String reviewerName, int rating, String comment) {
        Review review = new Review();
        review.setReviewerName(reviewerName);
        review.setRating(rating);
        review.setComment(comment.trim());
        review.setProduct(product);
        review.setCreatedAt(LocalDateTime.now());
        reviewRepository.save(review);
    }
}
