package com.example.lumoo.controller;

import com.example.lumoo.model.Order;
import com.example.lumoo.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private OrderService orderService;
    @Autowired private ProductService productService;
    @Autowired private UserService userService;
    @Autowired private InquiryService inquiryService;
    @Autowired private VendorApplicationService vendorApplicationService;

    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        List<Order> orders = orderService.getAll();
        model.addAttribute("totalOrders", orders.size());
        model.addAttribute("totalRevenue", orders.stream().mapToDouble(Order::getTotalAmount).sum());
        model.addAttribute("totalCommission", orders.stream().mapToDouble(Order::getAdminCommission).sum());
        model.addAttribute("products", productService.getAll());
        model.addAttribute("users", userService.getAll());
        model.addAttribute("orders", orders);
        model.addAttribute("inquiries", inquiryService.getAll());
        model.addAttribute("pendingProducts", productService.getPendingApproval());
        model.addAttribute("vendorApplications", vendorApplicationService.getPending());
        return "admin/dashboard";
    }

    @GetMapping("/approve-product/{id}")
    public String approveProduct(@PathVariable Long id) {
        productService.approve(id);
        return "redirect:/admin/dashboard?approved#moderation";
    }

    @GetMapping("/reject-product/{id}")
    public String rejectProduct(@PathVariable Long id) {
        productService.delete(id);
        return "redirect:/admin/dashboard?rejected#moderation";
    }

    @GetMapping("/delete-product/{id}")
    public String adminDeleteProduct(@PathVariable Long id) {
        productService.delete(id);
        return "redirect:/admin/dashboard?deleted_product#inventory";
    }

    @GetMapping("/delete-user/{id}")
    public String adminDeleteUser(@PathVariable Long id) {
        userService.delete(id);
        return "redirect:/admin/dashboard?deleted_user#users";
    }

    @PostMapping("/update-order-status")
    public String updateOrderStatus(@RequestParam Long orderId, @RequestParam String status) {
        orderService.updateStatus(orderId, status);
        return "redirect:/admin/dashboard#orders";
    }

    @GetMapping("/delete-order/{id}")
    public String deleteOrder(@PathVariable Long id) {
        orderService.delete(id);
        return "redirect:/admin/dashboard#orders";
    }

    @GetMapping("/delete-inquiry/{id}")
    public String deleteInquiry(@PathVariable Long id) {
        inquiryService.delete(id);
        return "redirect:/admin/dashboard?deleted_inquiry#inquiries";
    }

    @GetMapping("/approve-vendor/{id}")
    public String approveVendor(@PathVariable Long id) {
        vendorApplicationService.approve(id);
        return "redirect:/admin/dashboard?vendor_approved";
    }

    @GetMapping("/reject-vendor/{id}")
    public String rejectVendor(@PathVariable Long id) {
        vendorApplicationService.reject(id);
        return "redirect:/admin/dashboard?vendor_rejected";
    }
}
