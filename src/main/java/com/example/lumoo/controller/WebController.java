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

    // --- VENDOR: DASHBOARD & MANAGEMENT ---
    @GetMapping("/vendor/dashboard")
    public String vendorDashboard(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        User currentVendor = userRepository.findByUsername(principal.getName()).orElse(null);
        if (currentVendor != null) {
            model.addAttribute("products", productRepository.findByVendor(currentVendor));
        }
        return "vendor/dashboard";
    }

    @GetMapping("/vendor/add-product")
    public String addProductPage(Model model) {
        model.addAttribute("product", new Product());
        return "vendor/add-product";
    }

    @PostMapping("/vendor/add-product")
    public String saveProduct(@ModelAttribute Product product, Principal principal) {
        if (principal == null) return "redirect:/login";
        User currentVendor = userRepository.findByUsername(principal.getName()).orElse(null);
        if (currentVendor != null) {
            product.setVendor(currentVendor); 
            productRepository.save(product);
        }
        return "redirect:/vendor/dashboard?success_add";
    }

    @GetMapping("/vendor/edit-product/{id}")
    public String editProductPage(@PathVariable Long id, Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        User currentUser = userRepository.findByUsername(principal.getName()).orElse(null);
        Product product = productRepository.findById(id).orElse(null);

        if (product == null || !product.getVendor().getId().equals(currentUser.getId())) {
            return "redirect:/vendor/dashboard?error=unauthorized";
        }

        model.addAttribute("product", product);
        return "vendor/edit-product"; 
    }

    @PostMapping("/vendor/edit-product/{id}")
    public String updateProduct(@PathVariable Long id, @ModelAttribute("product") Product product, Principal principal) {
        if (principal == null) return "redirect:/login";
        User currentUser = userRepository.findByUsername(principal.getName()).orElse(null);
        Product existing = productRepository.findById(id).orElse(null);

        if (existing == null || !existing.getVendor().getId().equals(currentUser.getId())) {
            return "redirect:/vendor/dashboard?error=unauthorized";
        }

        product.setId(id);
        product.setVendor(currentUser); 
        productRepository.save(product);
        return "redirect:/vendor/dashboard?success_update";
    }

    @GetMapping("/vendor/delete-product/{id}")
    public String deleteProduct(@PathVariable Long id, Principal principal) {
        if (principal == null) return "redirect:/login";
        User currentUser = userRepository.findByUsername(principal.getName()).orElse(null);
        Product existing = productRepository.findById(id).orElse(null);

        if (existing != null && existing.getVendor().getId().equals(currentUser.getId())) {
            productRepository.deleteById(id);
        }
        return "redirect:/vendor/dashboard?success_delete";
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
        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        if (user != null) {
            List<Order> myOrders = orderRepository.findByUserOrderByOrderDateDesc(user);
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
        
        User currentUser = userRepository.findByUsername(principal.getName()).orElse(null);
        Order order = orderRepository.findById(id).orElse(null);

        // PEMBETULAN: Security check tanpa ralat casting
        if (order == null || !((User) order.getUser()).getId().equals(currentUser.getId())) {
    return "redirect:/buyer/dashboard?error=unauthorized";
}

        model.addAttribute("order", order);
        return "buyer/order-details";
    }

    @PostMapping("/buyer/order/delete/{id}")
    public String deleteOrder(@PathVariable Long id, Principal principal) {
        if (principal == null) return "redirect:/login";
        
        Order order = orderRepository.findById(id).orElse(null);
        User currentUser = userRepository.findByUsername(principal.getName()).orElse(null);

        // PEMBETULAN: Security check tanpa ralat casting
        if (order != null && ((User) order.getUser()).getId().equals(currentUser.getId())) {
    orderRepository.delete(order);
}
        return "redirect:/buyer/dashboard?order_cancelled";
    }
}