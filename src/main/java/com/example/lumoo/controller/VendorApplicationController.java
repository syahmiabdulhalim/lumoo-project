package com.example.lumoo.controller;

import com.example.lumoo.model.Role;
import com.example.lumoo.model.User;
import com.example.lumoo.model.VendorApplication;
import com.example.lumoo.service.UserService;
import com.example.lumoo.service.VendorApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
public class VendorApplicationController {

    @Autowired private VendorApplicationService vendorApplicationService;
    @Autowired private UserService userService;

    @GetMapping("/buyer/vendorapply")
    public String applyPage(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        User user = userService.findByEmail(principal.getName()).orElse(null);
        if (user == null) return "redirect:/login";
        if (user.getRole() != Role.USER) return "redirect:/?error=not_eligible";

        List<VendorApplication> applications = vendorApplicationService.getByUser(user);
        VendorApplication latest = applications.isEmpty() ? null : applications.get(0);

        String state = "FORM";
        LocalDateTime canReapplyAt = null;

        if (latest != null) {
            if ("PENDING".equals(latest.getStatus())) {
                state = "PENDING";
            } else if ("REJECTED".equals(latest.getStatus())) {
                if (latest.getReviewedAt() != null &&
                        latest.getReviewedAt().plusHours(12).isAfter(LocalDateTime.now())) {
                    state = "REJECTED_COOLDOWN";
                    canReapplyAt = latest.getReviewedAt().plusHours(12);
                }
                // else: reviewedAt + 12h passed → state stays FORM, allow re-apply
            }
        }

        model.addAttribute("applications", applications);
        model.addAttribute("state", state);
        model.addAttribute("latest", latest);
        model.addAttribute("canReapplyAt", canReapplyAt);
        return "vendor-apply";
    }

    @PostMapping("/buyer/vendorapply")
    public String submitApplication(@RequestParam String businessName,
                                    @RequestParam String businessType,
                                    @RequestParam String phone,
                                    @RequestParam String reason,
                                    Principal principal) {
        if (principal == null) return "redirect:/login";
        if (businessName == null || businessName.trim().isEmpty()) return "redirect:/buyer/vendorapply?error";
        if (phone == null || phone.trim().isEmpty()) return "redirect:/buyer/vendorapply?error";
        if (reason == null || reason.trim().isEmpty()) return "redirect:/buyer/vendorapply?error";

        User user = userService.findByEmail(principal.getName()).orElse(null);
        if (user == null) return "redirect:/login";
        if (user.getRole() != Role.USER) return "redirect:/?error=not_eligible";
        if (!vendorApplicationService.canReapply(user)) return "redirect:/buyer/vendorapply";

        vendorApplicationService.apply(user, businessName, businessType, phone, reason);
        return "redirect:/buyer/vendorapply?submitted";
    }
}
