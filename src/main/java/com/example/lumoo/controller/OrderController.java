package com.example.lumoo.controller;

import com.example.lumoo.model.CartItem;
import com.example.lumoo.model.Order;
import com.example.lumoo.model.Role;
import com.example.lumoo.model.User;
import com.example.lumoo.service.CartService;
import com.example.lumoo.service.OrderService;
import com.example.lumoo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Set;

@Controller
public class OrderController {

    @Autowired private OrderService orderService;
    @Autowired private CartService cartService;
    @Autowired private UserService userService;

    private static final Set<String> VALID_PAYMENT_METHODS = Set.of("COD", "TRANSFER", "AFRIMONEY", "QMONEY");

    @GetMapping("/checkout")
    public String checkoutPage(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        User user = userService.findByEmail(principal.getName()).orElse(null);
        if (user == null) return "redirect:/login";
        if (user.getRole() != Role.USER) return "redirect:/?error=not_a_buyer";
        List<CartItem> items = cartService.getItems(user);
        if (items.isEmpty()) return "redirect:/cart";
        model.addAttribute("cart", items);
        model.addAttribute("total", cartService.getTotal(items));
        return "checkout";
    }

    @PostMapping("/order/place")
    public String placeOrder(@RequestParam String address,
                             @RequestParam String paymentMethod,
                             Principal principal) {
        if (principal == null) return "redirect:/login";
        if (address == null || address.trim().isEmpty()) return "redirect:/checkout?error=address_required";
        if (!VALID_PAYMENT_METHODS.contains(paymentMethod)) return "redirect:/checkout?error=invalid_payment";

        User user = userService.findByEmail(principal.getName()).orElse(null);
        if (user == null) return "redirect:/login";
        if (user.getRole() != Role.USER) return "redirect:/?error=not_a_buyer";

        List<CartItem> items = cartService.getItems(user);
        if (items.isEmpty()) return "redirect:/cart?error=empty";

        Order saved = orderService.placeOrder(user, address, paymentMethod, items);
        return "redirect:/buyer/order/" + saved.getId() + "?success";
    }
}
