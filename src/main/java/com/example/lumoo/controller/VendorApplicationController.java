package com.example.lumoo.controller;

import com.example.lumoo.model.*;
import com.example.lumoo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Controller
public class VendorApplicationController {

    @Autowired private VendorApplicationRepository applicationRepository;
    @Autowired private UserRepository userRepository;

    // Show the application form
    @GetMapping("/vendor/apply")
    public String applyPage(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";

        User user = userRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null) return "redirect:/login";

        // Only USER role can apply
        if (user.getRole() != Role.USER) return "redirect:/?error=not_eligible";

        // Check if already has a pending application
        boolean alreadyApplied = applicationRepository.existsByUserAndStatus(user, "PENDING");
        if (alreadyApplied) return "redirect:/vendor/apply?already_applied";

        model.addAttribute("user", user);
        return "vendor-apply";
    }

    // Submit the application
    @PostMapping("/vendor/apply")
    public String submitApplication(
            @RequestParam String businessName,
            @RequestParam String businessType,
            @RequestParam String phone,
            @RequestParam String reason,
            Principal principal) {

        if (principal == null) return "redirect:/login";

        // Validation
        if (businessName == null || businessName.trim().isEmpty())
            return "redirect:/vendor/apply?error=missing_fields";
        if (phone == null || phone.trim().isEmpty())
            return "redirect:/vendor/apply?error=missing_fields";
        if (reason == null || reason.trim().isEmpty())
            return "redirect:/vendor/apply?error=missing_fields";

        User user = userRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null) return "redirect:/login";
        if (user.getRole() != Role.USER) return "redirect:/?error=not_eligible";

        // Prevent duplicate pending application
        if (applicationRepository.existsByUserAndStatus(user, "PENDING"))
            return "redirect:/vendor/apply?already_applied";

        VendorApplication application = new VendorApplication();
        application.setUser(user);
        application.setBusinessName(businessName.trim());
        application.setBusinessType(businessType.trim());
        application.setPhone(phone.trim());
        application.setReason(reason.trim());

        applicationRepository.save(application);

        return "redirect:/vendor/apply?submitted";
    }
}