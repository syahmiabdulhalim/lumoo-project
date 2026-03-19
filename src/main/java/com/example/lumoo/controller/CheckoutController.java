package com.example.lumoo.controller;

import com.example.lumoo.model.CartItem;
import com.example.lumoo.model.Order;
import com.example.lumoo.model.OrderItem;
import com.example.lumoo.repository.OrderRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Controller
@RequestMapping("/checkout")
public class CheckoutController {
    @Autowired private OrderRepository orderRepository;

    @PostMapping("/process")
    @SuppressWarnings("unchecked")
    public String processCheckout(HttpSession session) {
        List<CartItem> cart = (List<CartItem>) session.getAttribute("cart");
        if (cart == null || cart.isEmpty()) return "redirect:/cart?error=empty";

        Order order = new Order();
        order.setOrderDate(java.time.LocalDateTime.now());
        order.setStatus("PENDING");
        
        List<OrderItem> orderItems = cart.stream().map(item -> {
            OrderItem oi = new OrderItem();
            oi.setProductName(item.getName());
            oi.setQuantity(item.getQuantity());
            oi.setPrice(item.getPrice());
            return oi;
        }).toList();

        order.setItems(orderItems);
        order.setTotalAmount(cart.stream().mapToDouble(i -> i.getPrice() * i.getQuantity()).sum());
        
        orderRepository.save(order);
        session.removeAttribute("cart");
        
        return "redirect:/checkout/success?id=" + order.getId();
    }

    @GetMapping("/success")
    public String successPage(@RequestParam Long id, Model model) {
        model.addAttribute("orderId", id);
        return "checkout-success";
    }
}