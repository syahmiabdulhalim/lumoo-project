package com.example.lumoo.controller;

import com.example.lumoo.model.Role;
import com.example.lumoo.model.User;
import com.example.lumoo.service.UserService;
import com.example.lumoo.service.VendorApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

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
        if (vendorApplicationService.hasAlreadyApplied(user)) return "redirect:/buyer/vendorapply?already_applied";
        model.addAttribute("user", user);
        return "vendor-apply";
    }

    @PostMapping("/buyer/vendorapply")
    public String submitApplication(@RequestParam String businessName,
                                    @RequestParam String businessType,
                                    @RequestParam String phone,
                                    @RequestParam String reason,
                                    Principal principal) {
        if (principal == null) return "redirect:/login";
        if (businessName == null || businessName.trim().isEmpty()) return "redirect:/buyer/vendorapply?error=missing_fields";
        if (phone == null || phone.trim().isEmpty()) return "redirect:/buyer/vendorapply?error=missing_fields";
        if (reason == null || reason.trim().isEmpty()) return "redirect:/buyer/vendorapply?error=missing_fields";

        User user = userService.findByEmail(principal.getName()).orElse(null);
        if (user == null) return "redirect:/login";
        if (user.getRole() != Role.USER) return "redirect:/?error=not_eligible";
        if (vendorApplicationService.hasAlreadyApplied(user)) return "redirect:/buyer/vendorapply?already_applied";

        vendorApplicationService.apply(user, businessName, businessType, phone, reason);
        return "redirect:/buyer/vendorapply?submitted";
    }
}
