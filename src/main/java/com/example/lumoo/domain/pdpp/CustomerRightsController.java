package com.example.lumoo.domain.pdpp;
import com.example.lumoo.shared.dto.ApiResponse;
import com.example.lumoo.domain.pdpp.CustomerRightsService;
import com.example.lumoo.domain.user.User;
import com.example.lumoo.domain.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.Map;
/**
 * PDPP data-subject requests. Always acts on the logged-in user's own account;
 * erasure additionally requires the current password.
 */
@RestController
@RequestMapping("/api/customer-rights")
@Validated
public class CustomerRightsController {
    @Autowired private CustomerRightsService customerRightsService;
    @Autowired private UserService userService;
    @Autowired private PasswordEncoder passwordEncoder;
    @PostMapping("/erasure-request")
    public ResponseEntity<ApiResponse<Map<String, Object>>> requestErasure(
            @RequestParam String password,
            Principal principal,
            HttpServletRequest request) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Login required."));
        User user = userService.findByEmail(principal.getName()).orElse(null);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Login required."));
        if (!passwordEncoder.matches(password, user.getPassword()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Incorrect password."));
        Map<String, Object> result = customerRightsService.processErasureRequest(user.getEmail(), request);
        // The account's email has been anonymised, so the current session can no longer be valid.
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(ApiResponse.ok("Erasure request processed.", result));
    }
    @PostMapping("/data-access-request")
    public ResponseEntity<ApiResponse<Map<String, Object>>> requestDataAccess(
            Principal principal,
            HttpServletRequest request) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Login required."));
        Map<String, Object> result = customerRightsService.processDataAccessRequest(principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.ok("Data access request fulfilled.", result));
    }
}
