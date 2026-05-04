package com.example.lumoo.controller;

import com.example.lumoo.model.*;
import com.example.lumoo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private OrderRepository orderRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private InquiryRepository inquiryRepository;
    @Autowired private VendorApplicationRepository applicationRepository;

    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        List<Order> orders = orderRepository.findAll();

        model.addAttribute("totalOrders", orders.size());
        model.addAttribute("totalRevenue", orders.stream().mapToDouble(Order::getTotalAmount).sum());
        model.addAttribute("totalCommission", orders.stream().mapToDouble(Order::getAdminCommission).sum());
        model.addAttribute("products", productRepository.findAll());
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("orders", orders);
        model.addAttribute("inquiries", inquiryRepository.findAllByOrderByCreatedAtDesc());
        model.addAttribute("pendingProducts", productRepository.findByApproved(false));

        // ✓ This line is now INSIDE the method
        model.addAttribute("vendorApplications",
                applicationRepository.findByStatusOrderByAppliedAtDesc("PENDING"));

        return "admin/dashboard";
    }

    // Approve product
    @GetMapping("/approve-product/{id}")
    public String approveProduct(@PathVariable Long id) {
        Product product = productRepository.findById(id).orElseThrow();
        product.setApproved(true);
        productRepository.save(product);
        return "redirect:/admin/dashboard?approved#moderation";
    }

    // Reject & delete product
    @GetMapping("/reject-product/{id}")
    public String rejectProduct(@PathVariable Long id) {
        productRepository.deleteById(id);
        return "redirect:/admin/dashboard?rejected#moderation";
    }

    // Delete product
    @GetMapping("/delete-product/{id}")
    public String adminDeleteProduct(@PathVariable Long id) {
        productRepository.deleteById(id);
        return "redirect:/admin/dashboard?deleted_product#inventory";
    }

    // Delete user
    @GetMapping("/delete-user/{id}")
    public String adminDeleteUser(@PathVariable Long id) {
        userRepository.deleteById(id);
        return "redirect:/admin/dashboard?deleted_user#users";
    }

    // Update order status
    @PostMapping("/update-order-status")
    public String updateOrderStatus(@RequestParam Long orderId, @RequestParam String status) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(status);
        orderRepository.save(order);
        return "redirect:/admin/dashboard#orders";
    }

    // Delete order
    @GetMapping("/delete-order/{id}")
    public String deleteOrder(@PathVariable Long id) {
        orderRepository.deleteById(id);
        return "redirect:/admin/dashboard#orders";
    }

    // Delete inquiry
    @GetMapping("/delete-inquiry/{id}")
    public String deleteInquiry(@PathVariable Long id) {
        inquiryRepository.deleteById(id);
        return "redirect:/admin/dashboard?deleted_inquiry#inquiries";
    }

    // Approve vendor application
    @GetMapping("/approve-vendor/{id}")
    public String approveVendor(@PathVariable Long id) {
        applicationRepository.findById(id).ifPresent(app -> {
            app.setStatus("APPROVED");
            applicationRepository.save(app);
            User user = app.getUser();
            user.setRole(Role.VENDOR);
            userRepository.save(user);
        });
        return "redirect:/admin/dashboard?vendor_approved";
    }

    // Reject vendor application
    @GetMapping("/reject-vendor/{id}")
    public String rejectVendor(@PathVariable Long id) {
        applicationRepository.findById(id).ifPresent(app -> {
            app.setStatus("REJECTED");
            applicationRepository.save(app);
        });
        return "redirect:/admin/dashboard?vendor_rejected";
    }
}