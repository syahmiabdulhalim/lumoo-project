package com.example.lumoo.controller;

import com.example.lumoo.model.*;
import com.example.lumoo.dto.RegisterRequest;
import com.example.lumoo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String loginPage() { return "login"; }

    @PostMapping("/login")
    public String processLogin(@RequestParam String username, @RequestParam String password) {
        var userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent() && passwordEncoder.matches(password, userOpt.get().getPassword())) {
            if (userOpt.get().getRole() == Role.VENDOR) return "redirect:/vendor/add-product";
            return "redirect:/";
        }
        return "redirect:/login?error";
    }

    @GetMapping("/register")
    public String registerPage() { return "register"; }

    @PostMapping("/register")
public String processRegister(@ModelAttribute RegisterRequest req) {
    try {
        User user = new User();
        user.setUsername(req.username());
        user.setEmail(req.email());
        user.setPassword(passwordEncoder.encode(req.password()));
        // Guna toUpperCase() untuk elakkan ralat enum
        user.setRole(Role.valueOf(req.role().toUpperCase()));
        userRepository.save(user);
        return "redirect:/login?success";
    } catch (Exception e) {
        System.err.println("REGISTER ERROR: " + e.getMessage());
        return "redirect:/register?error";
    }
}

    
}