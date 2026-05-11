package com.example.lumoo.controller;

import com.example.lumoo.model.Order;
import com.example.lumoo.model.Product;
import com.example.lumoo.model.User;
import com.example.lumoo.service.OrderService;
import com.example.lumoo.service.ProductService;
import com.example.lumoo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/vendor")
public class VendorController {

    @Autowired private ProductService productService;
    @Autowired private OrderService orderService;
    @Autowired private UserService userService;

    @GetMapping("/dashboard")
    public String vendorDashboard(Model model, @AuthenticationPrincipal UserDetails currentUser) {
        User vendor = userService.findByEmail(currentUser.getUsername()).orElseThrow();
        List<Product> products = productService.getByVendor(vendor);
        List<Order> vendorOrders = orderService.getVendorOrders(vendor.getId());

        double totalRevenue = vendorOrders.stream().mapToDouble(Order::getTotalAmount).sum();
        double averageOrder = vendorOrders.isEmpty() ? 0.0 : totalRevenue / vendorOrders.size();
        double monthlySales = vendorOrders.stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().getMonth() == LocalDate.now().getMonth())
                .mapToDouble(Order::getTotalAmount).sum();

        model.addAttribute("products", products);
        model.addAttribute("vendorOrders", vendorOrders);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("averageOrder", averageOrder);
        model.addAttribute("monthlySales", monthlySales);
        model.addAttribute("totalSalesCount", vendorOrders.size());
        model.addAttribute("vendorName", vendor.getUsername());
        return "vendor/dashboard";
    }

    @GetMapping("/add-product")
    public String addProductPage(Model model) {
        model.addAttribute("product", new Product());
        return "vendor/add-product";
    }

    @PostMapping("/add-product")
    public String saveProduct(@ModelAttribute Product product,
                              @RequestParam(required = false) MultipartFile image,
                              Principal principal) {
        if (principal == null) return "redirect:/login";
        User vendor = userService.findByEmail(principal.getName()).orElse(null);
        if (vendor == null) return "redirect:/login";
        try {
            if (image != null && !image.isEmpty()) {
                product.setImageUrl(productService.saveImage(image));
            }
        } catch (IOException e) {
            return "redirect:/vendor/add-product?error=upload_failed";
        }
        productService.addProduct(product, vendor);
        return "redirect:/vendor/dashboard?success_add";
    }

    @GetMapping("/edit-product/{id}")
    public String editProductPage(@PathVariable Long id, Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        User vendor = userService.findByEmail(principal.getName()).orElse(null);
        Product product = productService.findById(id).orElse(null);
        if (product == null || vendor == null || !product.getVendor().getId().equals(vendor.getId())) {
            return "redirect:/vendor/dashboard?error=unauthorized";
        }
        model.addAttribute("product", product);
        return "vendor/edit-product";
    }

    @PostMapping("/edit-product/{id}")
    public String updateProduct(@PathVariable Long id,
                                @ModelAttribute Product product,
                                @RequestParam(required = false) MultipartFile image,
                                Principal principal) {
        if (principal == null) return "redirect:/login";
        User vendor = userService.findByEmail(principal.getName()).orElse(null);
        if (vendor == null) return "redirect:/login";
        try {
            boolean updated = productService.updateProduct(id, product, vendor, image);
            return updated ? "redirect:/vendor/dashboard?success_update" : "redirect:/vendor/dashboard?error=unauthorized";
        } catch (IOException e) {
            return "redirect:/vendor/edit-product/" + id + "?error=upload_failed";
        }
    }

    @PostMapping("/order/{id}/ship")
    public String shipOrder(@PathVariable Long id,
                            @RequestParam(required = false) String trackingNumber,
                            @AuthenticationPrincipal UserDetails currentUser) {
        User vendor = userService.findByEmail(currentUser.getUsername()).orElseThrow();
        OrderService.ShipResult result = orderService.markShipped(id, vendor.getId(), trackingNumber);
        return switch (result) {
            case SHIPPED -> "redirect:/vendor/dashboard?shipped";
            case NOT_FOUND -> "redirect:/vendor/dashboard?error=not_found";
            case UNAUTHORIZED -> "redirect:/vendor/dashboard?error=unauthorized";
            case INVALID_STATUS -> "redirect:/vendor/dashboard?error=invalid_status";
        };
    }

    @GetMapping("/delete-product/{id}")
    public String deleteProduct(@PathVariable Long id, Principal principal) {
        if (principal == null) return "redirect:/login";
        User vendor = userService.findByEmail(principal.getName()).orElse(null);
        if (vendor == null) return "redirect:/login";
        productService.deleteByVendor(id, vendor);
        return "redirect:/vendor/dashboard?success_delete";
    }
}
