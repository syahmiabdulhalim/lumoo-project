package com.example.lumoo.domain.pdpp;

import com.example.lumoo.shared.dto.ApiResponse;
import com.example.lumoo.domain.pdpp.CustomerRightsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/customer-rights")
@Validated
public class CustomerRightsController {

    @Autowired private CustomerRightsService customerRightsService;

    /**
     * PDPP 2025 s.24 — Right to Erasure.
     * Anonymises all personal data for the given email address.
     */
    @PostMapping("/erasure-request")
    public ResponseEntity<ApiResponse<Map<String, Object>>> requestErasure(
            @RequestParam @NotBlank @Email String email,
            HttpServletRequest request) {
        Map<String, Object> result = customerRightsService.processErasureRequest(email, request);
        return ResponseEntity.ok(ApiResponse.ok("Erasure request processed.", result));
    }

    /**
     * PDPP 2025 s.23 — Right of Access.
     * Returns all personal data held for the given email address.
     */
    @PostMapping("/data-access-request")
    public ResponseEntity<ApiResponse<Map<String, Object>>> requestDataAccess(
            @RequestParam @NotBlank @Email String email,
            HttpServletRequest request) {
        Map<String, Object> result = customerRightsService.processDataAccessRequest(email, request);
        return ResponseEntity.ok(ApiResponse.ok("Data access request fulfilled.", result));
    }
}
