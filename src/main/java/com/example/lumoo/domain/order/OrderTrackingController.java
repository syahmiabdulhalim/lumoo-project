package com.example.lumoo.domain.order;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/track")
public class OrderTrackingController {

    @Autowired private OrderRepository orderRepository;

    @GetMapping({"", "/"})
    public String form(@RequestParam(required = false) String error, Model model) {
        if (error != null) model.addAttribute("error", error);
        return "track";
    }

    @PostMapping
    public String lookup(@RequestParam String orderId,
                         @RequestParam String email,
                         Model model) {
        Long id;
        try {
            String cleaned = orderId.trim().toUpperCase().replace("LMO-", "").replace("#", "");
            id = Long.parseLong(cleaned);
        } catch (NumberFormatException e) {
            model.addAttribute("error", "Invalid order ID. Use the format LMO-123.");
            return "track";
        }

        Order order = orderRepository.findById(id).orElse(null);
        if (order == null
                || order.getUser() == null
                || !order.getUser().getEmail().equalsIgnoreCase(email.trim())) {
            model.addAttribute("error", "No order found. Please check your order ID and email.");
            return "track";
        }

        model.addAttribute("order", order);
        model.addAttribute("items", order.getItems());
        return "track";
    }
}
