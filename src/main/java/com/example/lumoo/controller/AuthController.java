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

    private static final org.slf4j.Logger log = 
    org.slf4j.LoggerFactory.getLogger(AuthController.class);

    @GetMapping("/login")
    public String loginPage() { return "login"; }

    @GetMapping("/register")
    public String registerPage() { return "register"; }

@PostMapping("/register")
public String processRegister(@ModelAttribute RegisterRequest req) {
    try {
        if (userRepository.findByEmail(req.email()).isPresent()) {
            return "redirect:/register?email_taken";
        }

        User user = new User();
        user.setUsername(req.email());
        user.setEmail(req.email());
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setRole(Role.USER);
        userRepository.save(user);
        return "redirect:/login?success";
    } catch (Exception e) {
    log.error("Registration failed for {}: {}", req.email(), e.getMessage());
    return "redirect:/register?error";
}
}
    
}