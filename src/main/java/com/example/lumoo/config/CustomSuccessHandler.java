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
        
        // 1. Check jika ada request yang tersangkut (macam klik Add to Cart sebelum login)
        SavedRequest savedRequest = new HttpSessionRequestCache().getRequest(request, response);
        if (savedRequest != null) {
            response.sendRedirect(savedRequest.getRedirectUrl());
            return;
        }

        var authorities = authentication.getAuthorities();
        String redirectUrl = "/"; 

        for (var authority : authorities) {
            if (authority.getAuthority().equals("ROLE_ADMIN")) {
                redirectUrl = "/admin/dashboard";
                break;
            } else if (authority.getAuthority().equals("ROLE_VENDOR")) {
                redirectUrl = "/vendor/dashboard"; // Biasanya vendor dashboard lebih baik
                break;
            } else if (authority.getAuthority().equals("ROLE_USER")) { 
                // TAMBAH INI: Sebab dalam DB anda tulis 'USER'
                redirectUrl = "/"; 
                break;
            }
        }
        response.sendRedirect(redirectUrl);
    }
}