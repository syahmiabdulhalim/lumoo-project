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
    @Autowired private CartRepository cartRepository;

    @GetMapping("/checkout")
    public String checkoutPage(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";

        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        List<CartItem> cartItems = cartRepository.findByUser(user);

        if (cartItems.isEmpty()) return "redirect:/cart";

        double total = cartItems.stream()
                .mapToDouble(i -> i.getPrice() * i.getQuantity()).sum();

        model.addAttribute("cart", cartItems);
        model.addAttribute("total", total);
        return "checkout";
    }

    @Transactional
    @PostMapping("/order/place")
    public String placeOrder(@RequestParam String address,
                             @RequestParam String paymentMethod,
                             Principal principal) {

        // 1. Pastikan user login
        if (principal == null) return "redirect:/login";

        // 2. Load user
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();

        // 3. Load cart dari DB
        List<CartItem> cartItems = cartRepository.findByUser(user);
        if (cartItems.isEmpty()) return "redirect:/cart?error=empty";

        // 4. Kira total
        double total = cartItems.stream()
                .mapToDouble(i -> i.getPrice() * i.getQuantity()).sum();

        // 5. Kira commission & vendor earnings
        double commission = total * 0.10;
        double vendorEarnings = total - commission;

        // 6. Bina Order
        Order order = new Order();
        order.setUser(user);
        order.setAddress(address);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(paymentMethod.equals("COD") ? "PENDING" : "PAID");
        order.setTotalAmount(total);
        order.setAdminCommission(commission);
        order.setVendorEarnings(vendorEarnings);

        Order savedOrder = orderRepository.save(order);

        // 7. Bina OrderItems
        for (CartItem ci : cartItems) {
            OrderItem oi = new OrderItem();
            oi.setOrder(savedOrder);
            oi.setProductName(ci.getName());
            oi.setPrice(ci.getPrice());
            oi.setQuantity(ci.getQuantity());
            orderItemRepository.save(oi);
        }

        // 8. Kosongkan cart
        cartRepository.deleteAll(cartItems);

        return "redirect:/buyer/order/" + savedOrder.getId() + "?success";
    }
}