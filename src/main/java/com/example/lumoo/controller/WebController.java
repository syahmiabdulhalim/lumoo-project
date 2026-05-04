package com.example.lumoo.controller;

import java.util.List;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Objects;

import com.example.lumoo.model.*;
import com.example.lumoo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class WebController {

    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private VendorApplicationRepository vendorApplicationRepository;

    // --- HOME PAGE & SEARCH ---
    @GetMapping("/")
    public String home(Model model, 
                       @RequestParam(value = "keyword", required = false) String keyword,
                       @RequestParam(value = "category", required = false) String category) {
        List<Product> products;
        if (keyword != null && !keyword.isEmpty()) {
            products = productRepository.findByNameContainingIgnoreCase(keyword);
        } else if (category != null && !category.isEmpty()) {
            products = productRepository.findByCategory(category);
        } else {
            products = productRepository.findAll();
        }
        
        model.addAttribute("products", products);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategory", category);
        return "index";
    }


    // --- REVIEW SYSTEM ---
    @PostMapping("/review/add/{productId}")
    public String addReview(@PathVariable Long productId, @RequestParam String reviewerName,
                           @RequestParam int rating, @RequestParam String comment) {
        productRepository.findById(productId).ifPresent(product -> {
            Review review = new Review();
            review.setReviewerName(reviewerName);
            review.setRating(rating);
            review.setComment(comment);
            review.setProduct(product);
            review.setCreatedAt(LocalDateTime.now());
            reviewRepository.save(review);
        });
        return "redirect:/?review_success";
    }

    // --- BUYER: DASHBOARD & DETAILS ---
    @GetMapping("/buyer/dashboard")
    public String buyerDashboard(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        User user = userRepository.findByEmail(principal.getName()).orElse(null);
        if (user != null) {
            List<Order> myOrders = orderRepository.findByUserOrderByOrderDateDesc(user);
            boolean alreadyApplied = vendorApplicationRepository.existsByUserAndStatus(user, "PENDING");
            model.addAttribute("alreadyApplied", alreadyApplied);
            if (myOrders != null) myOrders.removeIf(Objects::isNull);
            model.addAttribute("orders", myOrders);
            
            List<Notification> notes = notificationRepository.findByUser(user);
            if (notes != null) {
                notes.forEach(n -> n.setRead(true));
                notificationRepository.saveAll(notes);
                model.addAttribute("notifications", notes);
            }
        }
        return "buyer/dashboard"; 
    }

    @GetMapping("/buyer/order/{id}")
    public String orderDetails(@PathVariable Long id, Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        
        User currentUser = userRepository.findByEmail(principal.getName()).orElse(null);
        Order order = orderRepository.findById(id).orElse(null);

        if (order == null || currentUser == null || 
    !order.getUser().getId().equals(currentUser.getId())) {
    return "redirect:/buyer/dashboard?error=unauthorized";

}

        model.addAttribute("order", order);
        return "buyer/order-details";
    }

   @PostMapping("/buyer/order/delete/{id}")
public String deleteOrder(@PathVariable Long id, Principal principal) {
    if (principal == null) return "redirect:/login";

    User currentUser = userRepository.findByEmail(principal.getName()).orElse(null);
    if (currentUser == null) return "redirect:/login";

    Order order = orderRepository.findById(id).orElse(null);
    if (order == null) return "redirect:/buyer/dashboard?error=not_found";

    if (!order.getUser().getId().equals(currentUser.getId()))
        return "redirect:/buyer/dashboard?error=unauthorized";

    // Can only cancel PENDING orders
    if (!order.getStatus().equals("PENDING"))
        return "redirect:/buyer/dashboard?error=cannot_cancel";

    orderRepository.delete(order);
    return "redirect:/buyer/dashboard?order_cancelled";
}
@GetMapping("/product/{id}")
public String productDetail(@PathVariable Long id, Model model) {
    Product product = productRepository.findById(id).orElse(null);
    if (product == null) return "redirect:/?error=not_found";

    List<Review> reviews = reviewRepository.findByProduct(product);

    double avgRating = reviews.isEmpty() ? 0 :
        reviews.stream().mapToInt(Review::getRating).average().orElse(0);

    model.addAttribute("product", product);
    model.addAttribute("reviews", reviews);
    model.addAttribute("avgRating", avgRating);
    return "product-detail";
}

@PostMapping("/product/{id}/review")
public String addReview(@PathVariable Long id,
                        @RequestParam String reviewerName,
                        @RequestParam int rating,
                        @RequestParam String comment,
                        Principal principal) {
    if (principal == null) return "redirect:/login";
    if (rating < 1 || rating > 5) return "redirect:/product/" + id + "?error=invalid_rating";
    if (comment == null || comment.trim().isEmpty()) return "redirect:/product/" + id + "?error=empty_comment";

    productRepository.findById(id).ifPresent(product -> {
        Review review = new Review();
        review.setReviewerName(reviewerName);
        review.setRating(rating);
        review.setComment(comment.trim());
        review.setProduct(product);
        review.setCreatedAt(LocalDateTime.now());
        reviewRepository.save(review);
    });
    return "redirect:/product/" + id + "?review_success";
}
}