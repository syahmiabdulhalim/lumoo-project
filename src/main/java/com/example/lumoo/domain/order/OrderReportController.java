package com.example.lumoo.domain.order;

import com.example.lumoo.domain.user.User;
import com.example.lumoo.domain.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
public class OrderReportController {

    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderReportRepository reportRepository;
    @Autowired private UserService userService;

    @PostMapping("/buyer/order/{id}/report")
    public String reportOrder(@PathVariable Long id,
                              @RequestParam String reason,
                              @RequestParam(required = false) String details,
                              Principal principal, RedirectAttributes ra) {
        if (principal == null) return "redirect:/login";
        User user = userService.findByEmail(principal.getName()).orElse(null);
        if (user == null) return "redirect:/login";

        Order order = orderRepository.findById(id).orElse(null);
        if (order == null || !order.getUser().getId().equals(user.getId())) {
            ra.addFlashAttribute("flashMsg", "Order not found.");
            ra.addFlashAttribute("flashType", "red");
            return "redirect:/buyer/dashboard";
        }

        if (reportRepository.existsByOrderIdAndResolvedFalse(id)) {
            ra.addFlashAttribute("flashMsg", "You already have an open report for this order. Our team will contact you.");
            ra.addFlashAttribute("flashType", "blue");
            return "redirect:/buyer/dashboard";
        }

        OrderReport report = new OrderReport();
        report.setOrder(order);
        report.setReporter(user);
        report.setReason(reason);
        report.setDetails(details != null ? details.trim() : null);
        reportRepository.save(report);

        userService.notifyAdmins("🚨 Order #LMO-" + id + " reported by " + user.getDisplayName() + " — Reason: " + reason);

        ra.addFlashAttribute("flashMsg", "Report submitted. Our team will review it within 24 hours.");
        ra.addFlashAttribute("flashType", "green");
        return "redirect:/buyer/dashboard";
    }

    @PostMapping("/admin/report/{id}/resolve")
    public String resolveReport(@PathVariable Long id,
                                @RequestParam(required = false) String adminNote,
                                RedirectAttributes ra) {
        reportRepository.findById(id).ifPresent(r -> {
            r.setResolved(true);
            r.setAdminNote(adminNote != null ? adminNote.trim() : null);
            reportRepository.save(r);
        });
        ra.addFlashAttribute("flashMsg", "Report marked as resolved.");
        ra.addFlashAttribute("flashType", "green");
        return "redirect:/admin/dashboard#orders";
    }
}
