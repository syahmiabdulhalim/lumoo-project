package com.example.lumoo.controller;

import com.example.lumoo.model.Order;
import com.example.lumoo.model.Product;
import com.example.lumoo.model.User;
import com.example.lumoo.repository.OrderRepository;
import com.example.lumoo.repository.ProductRepository;
import com.example.lumoo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/vendor") // <-- INI DAH ADA VENDOR
public class VendorController {

    @Autowired private ProductRepository productRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private UserRepository userRepository;

    @GetMapping("/dashboard")
    public String vendorDashboard(Model model, @AuthenticationPrincipal UserDetails currentUser) {
        User vendor = userRepository.findByUsername(currentUser.getUsername()).orElseThrow();
        List<Product> products = productRepository.findByVendor(vendor);
        List<Order> vendorOrders = orderRepository.findOrdersByVendorId(vendor.getId());

        double totalRevenue = vendorOrders.stream().mapToDouble(Order::getTotalAmount).sum();
        double averageOrder = (vendorOrders.size() > 0) ? totalRevenue / vendorOrders.size() : 0.0;
        double monthlySales = vendorOrders.stream()
                .filter(o -> o.getCreatedAt().getMonth() == LocalDate.now().getMonth())
                .mapToDouble(Order::getTotalAmount).sum();

        model.addAttribute("products", products);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("averageOrder", averageOrder);
        model.addAttribute("monthlySales", monthlySales);
        model.addAttribute("totalSalesCount", vendorOrders.size());
        model.addAttribute("vendorName", vendor.getUsername());

        return "vendor/dashboard";
    }

    // CUCI: Buang perkataan '/vendor' dalam path bawah ni
    @GetMapping("/add-product")
    public String addProductPage(Model model) {
        model.addAttribute("product", new Product());
        return "vendor/add-product";
    }

    @PostMapping("/add-product")
    public String saveProduct(@ModelAttribute Product product, Principal principal) {
        if (principal == null) return "redirect:/login";
        User currentVendor = userRepository.findByUsername(principal.getName()).orElse(null);
        if (currentVendor != null) {
            product.setVendor(currentVendor); 
            productRepository.save(product);
        }
        return "redirect:/vendor/dashboard?success_add";
    }

    @GetMapping("/edit-product/{id}")
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

    @PostMapping("/edit-product/{id}")
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

    @GetMapping("/delete-product/{id}")
    public String deleteProduct(@PathVariable Long id, Principal principal) {
        if (principal == null) return "redirect:/login";
        User currentUser = userRepository.findByUsername(principal.getName()).orElse(null);
        Product existing = productRepository.findById(id).orElse(null);

        if (existing != null && existing.getVendor().getId().equals(currentUser.getId())) {
            productRepository.deleteById(id);
        }
        return "redirect:/vendor/dashboard?success_delete";
    }
}