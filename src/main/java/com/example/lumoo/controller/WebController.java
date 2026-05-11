package com.example.lumoo.controller;

import com.example.lumoo.model.*;
import com.example.lumoo.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Objects;

@Controller
public class WebController {

    @Autowired private ProductService productService;
    @Autowired private UserService userService;
    @Autowired private OrderService orderService;
    @Autowired private ReviewService reviewService;
    @Autowired private VendorApplicationService vendorApplicationService;

    @GetMapping("/")
    public String home(Model model,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String category) {
        List<Product> products;
        if (keyword != null && !keyword.isEmpty()) {
            products = productService.searchByName(keyword);
        } else if (category != null && !category.isEmpty()) {
            products = productService.getByCategory(category);
        } else {
            products = productService.getAll();
        }
        model.addAttribute("products", products);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategory", category);
        return "index";
    }

    @GetMapping("/product/{id}")
    public String productDetail(@PathVariable Long id, Model model, Principal principal) {
        Product product = productService.findById(id).orElse(null);
        if (product == null) return "redirect:/?error=not_found";
        List<Review> reviews = reviewService.getByProduct(product);
        model.addAttribute("product", product);
        model.addAttribute("reviews", reviews);
        model.addAttribute("avgRating", reviewService.getAverageRating(reviews));
        if (principal != null) {
            userService.findByEmail(principal.getName()).ifPresent(user -> {
                model.addAttribute("canReview", reviewService.canReview(user, product));
                model.addAttribute("alreadyReviewed", reviewService.hasAlreadyReviewed(user, product));
            });
        }
        return "product-detail";
    }

    @PostMapping("/product/{id}/review")
    public String addReview(@PathVariable Long id,
                            @RequestParam int rating,
                            @RequestParam String comment,
                            Principal principal) {
        if (principal == null) return "redirect:/login";
        if (rating < 1 || rating > 5) return "redirect:/product/" + id + "?error=invalid_rating";
        if (comment == null || comment.trim().isEmpty()) return "redirect:/product/" + id + "?error=empty_comment";
        User currentUser = userService.findByEmail(principal.getName()).orElse(null);
        if (currentUser == null) return "redirect:/login";
        Product product = productService.findById(id).orElse(null);
        if (product == null) return "redirect:/?error=not_found";
        if (!reviewService.canReview(currentUser, product)) return "redirect:/product/" + id + "?error=not_purchased";
        if (reviewService.hasAlreadyReviewed(currentUser, product)) return "redirect:/product/" + id + "?error=already_reviewed";
        reviewService.addReview(product, currentUser, rating, comment);
        return "redirect:/product/" + id + "?review_success";
    }

    @PostMapping("/buyer/order/{id}/received")
    public String markReceived(@PathVariable Long id, Principal principal) {
        if (principal == null) return "redirect:/login";
        User currentUser = userService.findByEmail(principal.getName()).orElse(null);
        if (currentUser == null) return "redirect:/login";
        OrderService.DeliverResult result = orderService.markDelivered(id, currentUser.getId());
        return switch (result) {
            case DELIVERED -> "redirect:/buyer/order/" + id + "?received";
            case NOT_FOUND -> "redirect:/buyer/dashboard?error=not_found";
            case UNAUTHORIZED -> "redirect:/buyer/dashboard?error=unauthorized";
            case INVALID_STATUS -> "redirect:/buyer/order/" + id + "?error=invalid_status";
        };
    }

    @PostMapping("/buyer/order/{id}/return")
    public String requestReturn(@PathVariable Long id,
                                @RequestParam(required = false) String returnReason,
                                Principal principal) {
        if (principal == null) return "redirect:/login";
        User currentUser = userService.findByEmail(principal.getName()).orElse(null);
        if (currentUser == null) return "redirect:/login";
        OrderService.ReturnResult result = orderService.requestReturn(id, currentUser.getId(), returnReason);
        return switch (result) {
            case REQUESTED -> "redirect:/buyer/order/" + id + "?return_requested";
            case NOT_FOUND -> "redirect:/buyer/dashboard?error=not_found";
            case UNAUTHORIZED -> "redirect:/buyer/dashboard?error=unauthorized";
            case INVALID_STATUS -> "redirect:/buyer/order/" + id + "?error=invalid_status";
        };
    }

    @GetMapping("/buyer/dashboard")
    public String buyerDashboard(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        User user = userService.findByEmail(principal.getName()).orElse(null);
        if (user != null) {
            List<Order> orders = orderService.getUserOrders(user);
            if (orders != null) orders.removeIf(Objects::isNull);
            model.addAttribute("orders", orders);
            model.addAttribute("alreadyApplied", vendorApplicationService.hasAlreadyApplied(user));
            List<Notification> notes = userService.getAndMarkNotificationsRead(user);
            model.addAttribute("notifications", notes);
        } else {
            model.addAttribute("alreadyApplied", false);
        }
        return "buyer/dashboard";
    }

    @GetMapping("/buyer/order/{id}")
    public String orderDetails(@PathVariable Long id, Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        User currentUser = userService.findByEmail(principal.getName()).orElse(null);
        Order order = orderService.findById(id).orElse(null);
        if (order == null || currentUser == null || !order.getUser().getId().equals(currentUser.getId())) {
            return "redirect:/buyer/dashboard?error=unauthorized";
        }
        model.addAttribute("order", order);
        return "buyer/order-details";
    }

    @PostMapping("/buyer/order/delete/{id}")
    public String cancelOrder(@PathVariable Long id, Principal principal) {
        if (principal == null) return "redirect:/login";
        User currentUser = userService.findByEmail(principal.getName()).orElse(null);
        if (currentUser == null) return "redirect:/login";
        OrderService.CancelResult result = orderService.cancelOrder(id, currentUser);
        return switch (result) {
            case CANCELLED -> "redirect:/buyer/dashboard?order_cancelled";
            case NOT_FOUND -> "redirect:/buyer/dashboard?error=not_found";
            case UNAUTHORIZED -> "redirect:/buyer/dashboard?error=unauthorized";
            case CANNOT_CANCEL -> "redirect:/buyer/dashboard?error=cannot_cancel";
        };
    }
}
