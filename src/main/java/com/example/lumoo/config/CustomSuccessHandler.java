package com.example.lumoo.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class CustomSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        
        // System.out.println("DEBUG LOGIN: Berjaya Authenticate! Menentukan hala tuju untuk: " + authentication.getName());

        // 1. Check jika ada request yang tersangkut (macam klik Add to Cart sebelum login)
        SavedRequest savedRequest = new HttpSessionRequestCache().getRequest(request, response);
        if (savedRequest != null) {
            String targetUrl = savedRequest.getRedirectUrl();
            if (!targetUrl.contains("/error") && !targetUrl.contains("/login")) {
                // System.out.println("DEBUG LOGIN: Menghantar user ke URL asal: " + targetUrl);
                response.sendRedirect(targetUrl);
                return;
            }
        }

        // 2. Tentukan Redirect URL berdasarkan Role
        var authorities = authentication.getAuthorities();
        String redirectUrl = "/"; 

        for (var authority : authorities) {
            String role = authority.getAuthority();
            // System.out.println("DEBUG LOGIN: Menyemak Authority -> " + role);

            if (role.equals("ROLE_ADMIN")) {
                redirectUrl = "/admin/dashboard";
                break;
            } else if (role.equals("ROLE_VENDOR")) {
                redirectUrl = "/vendor/dashboard";
                break;
            } else if (role.equals("ROLE_USER")) { 
                redirectUrl = "/"; 
                break;
            }
        }

        // System.out.println("DEBUG LOGIN: Redirecting ke -> " + redirectUrl);
        response.sendRedirect(redirectUrl);
    }
}