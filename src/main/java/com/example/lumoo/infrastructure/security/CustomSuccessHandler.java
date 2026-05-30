package com.example.lumoo.infrastructure.security;
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
        SavedRequest savedRequest = new HttpSessionRequestCache().getRequest(request, response);
        if (savedRequest != null) {
            String targetUrl = savedRequest.getRedirectUrl();
            if (!targetUrl.contains("/error") && !targetUrl.contains("/login")) {
                response.sendRedirect(targetUrl);
                return;
            }
        }
        var authorities = authentication.getAuthorities();
        String redirectUrl = "/"; 
        for (var authority : authorities) {
            String role = authority.getAuthority();
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
        response.sendRedirect(redirectUrl);
    }
}
