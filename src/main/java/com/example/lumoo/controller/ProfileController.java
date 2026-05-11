package com.example.lumoo.controller;

import com.example.lumoo.model.User;
import com.example.lumoo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProfileController {

    @Autowired private UserService userService;

    @GetMapping("/profile")
    public String showProfile(Authentication auth, Model model) {
        User user = userService.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", user);
        return "profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam String fullName,
                                @RequestParam String phone,
                                @RequestParam String address,
                                Authentication auth,
                                RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        userService.updateProfile(user, fullName, phone, address);
        redirectAttributes.addFlashAttribute("success", "Profile updated successfully.");
        return "redirect:/profile";
    }

    @PostMapping("/profile/change-password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Authentication auth,
                                 RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserService.PasswordChangeResult result =
                userService.changePassword(user, currentPassword, newPassword, confirmPassword);

        return switch (result) {
            case SUCCESS -> {
                redirectAttributes.addFlashAttribute("passwordSuccess", "Password changed successfully.");
                yield "redirect:/profile";
            }
            case WRONG_CURRENT -> {
                redirectAttributes.addFlashAttribute("passwordError", "Current password is incorrect.");
                yield "redirect:/profile";
            }
            case MISMATCH -> {
                redirectAttributes.addFlashAttribute("passwordError", "New passwords do not match.");
                yield "redirect:/profile";
            }
            case TOO_SHORT -> {
                redirectAttributes.addFlashAttribute("passwordError", "New password must be at least 6 characters.");
                yield "redirect:/profile";
            }
        };
    }
}
