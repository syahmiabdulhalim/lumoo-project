package com.example.lumoo.controller;

import com.example.lumoo.model.*;
import com.example.lumoo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@Controller
public class CartController {

    @Autowired private CartRepository cartRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;

    // --- PAPAR CART ---
    @GetMapping("/cart")
    public String viewCart(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        List<CartItem> cartItems = cartRepository.findByUser(user);
        
        double total = cartItems.stream().mapToDouble(i -> i.getPrice() * i.getQuantity()).sum();
        
        model.addAttribute("cart", cartItems);
        model.addAttribute("total", total);
        return "cart";
    }

    // --- TAMBAH KE CART (INI LOGIK YANG PENTING) ---
    @PostMapping("/cart/add/{id}")
    public String addToCart(@PathVariable Long id, Principal principal) {
        if (principal == null) return "redirect:/login";

        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        Product product = productRepository.findById(id).orElseThrow();

        // Check jika produk dah ada dalam cart user
        CartItem cartItem = cartRepository.findByUserAndProduct(user, product).orElse(null);

        if (cartItem != null) {
            // Jika dah ada, cuma tambah quantity
            cartItem.setQuantity(cartItem.getQuantity() + 1);
        } else {
            cartItem = new CartItem();
            cartItem.setUser(user);
            cartItem.setProduct(product);
            cartItem.setQuantity(1);
            cartItem.setName(product.getName()); 
            cartItem.setPrice(product.getPrice()); 
        }

        cartRepository.save(cartItem);
        return "redirect:/cart?success_add";
    }
}