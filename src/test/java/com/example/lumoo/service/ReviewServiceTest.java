package com.example.lumoo.service;

import com.example.lumoo.domain.order.OrderItemRepository;
import com.example.lumoo.domain.product.Product;
import com.example.lumoo.domain.product.Review;
import com.example.lumoo.domain.product.ReviewRepository;
import com.example.lumoo.domain.product.ReviewService;
import com.example.lumoo.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @InjectMocks private ReviewService reviewService;

    private User user;
    private Product product;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("buyer@test.com");

        product = new Product();
        product.setId(10L);
    }

    // ── getAverageRating ──────────────────────────────────────────────────────

    @Test
    void getAverageRating_returnsZero_whenEmpty() {
        assertEquals(0.0, reviewService.getAverageRating(List.of()), 0.001);
    }

    @Test
    void getAverageRating_correctAverage() {
        assertEquals(3.5, reviewService.getAverageRating(List.of(review(4), review(3))), 0.001);
    }

    @Test
    void getAverageRating_singleReview() {
        assertEquals(5.0, reviewService.getAverageRating(List.of(review(5))), 0.001);
    }

    // ── canReview ─────────────────────────────────────────────────────────────

    @Test
    void canReview_returnsTrue_whenDeliveredOrderExists() {
        when(orderItemRepository.existsByOrderUserAndOrderStatusAndProduct(user, "DELIVERED", product))
                .thenReturn(true);

        assertTrue(reviewService.canReview(user, product));
    }

    @Test
    void canReview_returnsFalse_whenNoDeliveredOrder() {
        when(orderItemRepository.existsByOrderUserAndOrderStatusAndProduct(user, "DELIVERED", product))
                .thenReturn(false);

        assertFalse(reviewService.canReview(user, product));
    }

    // ── hasAlreadyReviewed ────────────────────────────────────────────────────

    @Test
    void hasAlreadyReviewed_returnsTrue_whenReviewExists() {
        when(reviewRepository.existsByUserAndProduct(user, product)).thenReturn(true);

        assertTrue(reviewService.hasAlreadyReviewed(user, product));
    }

    @Test
    void hasAlreadyReviewed_returnsFalse_whenNoReview() {
        when(reviewRepository.existsByUserAndProduct(user, product)).thenReturn(false);

        assertFalse(reviewService.hasAlreadyReviewed(user, product));
    }

    // ── addReview ─────────────────────────────────────────────────────────────

    @Test
    void addReview_savesReview_withRatingAndComment() {
        reviewService.addReview(product, user, 4, "  Great product  ");

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        Review saved = captor.getValue();
        assertEquals(4, saved.getRating());
        assertEquals("Great product", saved.getComment());
        assertEquals(product, saved.getProduct());
        assertEquals(user, saved.getUser());
    }

    @Test
    void addReview_usesFullName_whenAvailable() {
        user.setFullName("John Buyer");

        reviewService.addReview(product, user, 5, "Excellent");

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        assertEquals("John Buyer", captor.getValue().getReviewerName());
    }

    @Test
    void addReview_usesEmail_whenFullNameBlank() {
        user.setFullName("   ");

        reviewService.addReview(product, user, 3, "OK");

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        assertEquals("buyer@test.com", captor.getValue().getReviewerName());
    }

    @Test
    void addReview_usesEmail_whenFullNameNull() {
        user.setFullName(null);

        reviewService.addReview(product, user, 3, "OK");

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        assertEquals("buyer@test.com", captor.getValue().getReviewerName());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Review review(int rating) {
        Review r = new Review();
        r.setRating(rating);
        return r;
    }
}
