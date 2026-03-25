package com.example.lumoo.controller;

import com.example.lumoo.model.*;
import com.example.lumoo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
public class OrderController {

    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private UserRepository userRepository;
    // Anggap anda ada CartRepository atau Service
    @Autowired private CartRepository cartRepository; 

    @GetMapping("/checkout")
public String checkoutPage(Model model, Principal principal) {
    if (principal == null) return "redirect:/login";
    
    User user = userRepository.findByUsername(principal.getName()).orElseThrow();
    List<CartItem> cartItems = cartRepository.findByUser(user);
    
    if (cartItems.isEmpty()) return "redirect:/cart";

    double total = cartItems.stream().mapToDouble(i -> i.getPrice() * i.getQuantity()).sum();
    
    model.addAttribute("cart", cartItems);
    model.addAttribute("total", total);
    return "checkout";
}

    @Transactional
    @PostMapping("/order/place")
    public String placeOrder(@RequestParam String address, 
                             @RequestParam String paymentMethod, 
                             Principal principal) {
        
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        List<CartItem> cartItems = cartRepository.findByUser(user);

        if (cartItems.isEmpty()) return "redirect:/?error=empty_cart";

        // 1. Cipta Order
        Order order = new Order();
        order.setUser(user);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus("PAID"); // Jika COD mungkin "PENDING"
        order.setAddress(address);
        
        double total = cartItems.stream().mapToDouble(i -> i.getPrice() * i.getQuantity()).sum();
        order.setTotalAmount(total);
        Order savedOrder = orderRepository.save(order);

        // 2. Pindahkan ke OrderItems
        for (CartItem ci : cartItems) {
            OrderItem oi = new OrderItem();
            oi.setOrder(savedOrder);
            oi.setProductName(ci.getName());
            oi.setPrice(ci.getPrice());
            oi.setQuantity(ci.getQuantity());
            orderItemRepository.save(oi);
        }

        // 3. Clear Cart
        cartRepository.deleteAll(cartItems);

        return "redirect:/buyer/order/" + savedOrder.getId() + "?success";
    }
}