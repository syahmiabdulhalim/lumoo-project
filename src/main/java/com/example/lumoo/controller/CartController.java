package com.example.lumoo.controller;

import com.example.lumoo.model.*;
import com.example.lumoo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@Controller
public class CartController {

    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;

    @PostMapping("/cart/add/{id}")
    public String addToCart(@PathVariable Long id, Principal principal) {
        // Jika user tak login, Spring Security akan redirect ke /login
        // Tapi kita buat check manual juga untuk keselamatan
        if (principal == null) return "redirect:/login";

        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        Product product = productRepository.findById(id).orElse(null);

        if (user != null && product != null) {
            // LOGIK CART ANDA DI SINI
            // Contoh: Simpan ke database CartItem
            return "redirect:/?added=success";
        }
        return "redirect:/";
    }
}