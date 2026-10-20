package com.example.lumoo.domain.user;
import com.example.lumoo.domain.user.User;
import com.example.lumoo.domain.user.UserRepository;
import com.example.lumoo.infrastructure.email.EmailService;
import com.example.lumoo.infrastructure.security.PasswordPolicy;
import com.example.lumoo.infrastructure.security.SessionRevoker;
import com.example.lumoo.infrastructure.security.TokenHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
@Controller
public class ForgotPasswordController {
    private static final Logger log = LoggerFactory.getLogger(ForgotPasswordController.class);
    /** Same text whether or not the account exists, so the form can't be used to discover emails. */
    static final String GENERIC_MESSAGE = "If an account exists for that email, a reset link has been sent. Please check your inbox (and spam folder).";
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EmailService emailService;
    @Autowired private SessionRevoker sessionRevoker;
    @GetMapping("/forgot-password")
    public String showForgotPage() {
        return "forgot-password";
    }
    @PostMapping("/forgot-password")
    public String processForgot(@RequestParam String email, Model model) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String token = TokenHasher.newToken();
            user.setResetToken(TokenHasher.hash(token));
            user.setTokenExpiry(LocalDateTime.now().plusHours(1));
            userRepository.save(user);
            if (!emailService.sendResetEmail(user.getEmail(), token))
                log.error("[ForgotPassword] Reset email could not be sent for user #{}", user.getId());
        });
        model.addAttribute("message", GENERIC_MESSAGE);
        return "forgot-password";
    }
    @GetMapping("/reset-password")
    public String showResetPage(@RequestParam String token, Model model) {
        var userOpt = userRepository.findByResetToken(TokenHasher.hash(token));
        if (userOpt.isEmpty() || isTokenExpired(userOpt.get())) {
            return "redirect:/login?error=invalid_token";
        }
        model.addAttribute("token", token);
        return "reset-password";
    }
    @PostMapping("/reset-password")
    public String handleReset(@RequestParam String token, @RequestParam String password, Model model) {
        var userOpt = userRepository.findByResetToken(TokenHasher.hash(token));
        if (userOpt.isEmpty() || isTokenExpired(userOpt.get())) {
            return "redirect:/login?error=invalid_token";
        }
        if (!PasswordPolicy.isAcceptable(password)) {
            model.addAttribute("token", token);
            model.addAttribute("error", PasswordPolicy.REQUIREMENT);
            return "reset-password";
        }
        User user = userOpt.get();
        user.setPassword(passwordEncoder.encode(password));
        user.setResetToken(null);
        user.setTokenExpiry(null);
        userRepository.save(user);
        // Whoever might have been using the old password is logged out everywhere.
        sessionRevoker.expireAllSessions(user.getEmail());
        return "redirect:/login?reset_success";
    }
    private boolean isTokenExpired(User user) {
        return user.getTokenExpiry() == null || user.getTokenExpiry().isBefore(LocalDateTime.now());
    }
}
