package com.example.lumoo.controller;

import com.example.lumoo.model.User;
import com.example.lumoo.repository.UserRepository;
import com.example.lumoo.service.EmailService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Controller
public class ForgotPasswordController {

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EmailService emailService;

    @GetMapping("/forgot-password")
    public String showForgotPage() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
public String processForgot(@RequestParam String email, Model model) {
    var userOpt = userRepository.findByEmail(email);
    if (userOpt.isPresent()) {
        User user = userOpt.get();
        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setTokenExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        boolean sent = emailService.sendResetEmail(user.getEmail(), token);
        if (sent) {
            model.addAttribute("message", "A reset link has been sent to your email. Please check your inbox (and spam folder).");
        } else {
            model.addAttribute("error", "We could not send the email right now. Please try again later or contact support at info@lumoo.my.");
        }
    } else {
        model.addAttribute("error", "No account found with that email address.");
    }
    return "forgot-password";
}

    @GetMapping("/reset-password")
    public String showResetPage(@RequestParam String token, Model model) {
        var userOpt = userRepository.findByResetToken(token); // Tambah findByResetToken di Repository
        if (userOpt.isEmpty() || userOpt.get().getTokenExpiry().isBefore(LocalDateTime.now())) {
            return "redirect:/login?error=invalid_token";
        }
        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String handleReset(@RequestParam String token, @RequestParam String password) {
        User user = userRepository.findByResetToken(token).orElseThrow();
        user.setPassword(passwordEncoder.encode(password));
        user.setResetToken(null); // Clear token lepas guna
        user.setTokenExpiry(null);
        userRepository.save(user);
        return "redirect:/login?reset_success";
    }


}