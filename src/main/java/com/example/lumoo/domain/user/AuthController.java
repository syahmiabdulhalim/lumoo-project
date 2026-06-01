package com.example.lumoo.domain.user;
import com.example.lumoo.domain.user.RegisterRequest;
import com.example.lumoo.domain.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
@Controller
public class AuthController {
    @Autowired private UserService userService;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthController.class);
    @GetMapping("/login")
    public String loginPage() { return "login"; }
    @GetMapping("/register")
    public String registerPage() { return "register"; }
    @PostMapping("/register")
    public String processRegister(@ModelAttribute RegisterRequest req) {
        try {
            boolean registered = userService.register(req.email(), req.password());
            return registered ? "redirect:/login?registered" : "redirect:/register?email_taken";
        } catch (Exception e) {
            log.error("Registration failed for {}: {}", req.email(), e.getMessage());
            return "redirect:/register?error";
        }
    }
    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam String token) {
        boolean ok = userService.verifyEmail(token);
        return ok ? "redirect:/login?email_verified" : "redirect:/login?verify_failed";
    }
    @PostMapping("/resend-verification")
    public String resendVerification(@RequestParam String email) {
        userService.resendVerification(email);
        return "redirect:/login?verification_sent";
    }
}
