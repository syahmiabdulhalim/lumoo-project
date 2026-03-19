package com.example.lumoo.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class CustomSuccessHandler implements AuthenticationSuccessHandler {
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        var authorities = authentication.getAuthorities();
        String redirectUrl = "/"; 

        for (var authority : authorities) {
            if (authority.getAuthority().equals("ROLE_VENDOR")) {
                redirectUrl = "/vendor/add-product";
                break;
            } else if (authority.getAuthority().equals("ROLE_ADMIN")) {
                redirectUrl = "/admin/dashboard";
                break;
            }
        }
        response.sendRedirect(redirectUrl);
    }
}