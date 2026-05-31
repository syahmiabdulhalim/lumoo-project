package com.example.lumoo.infrastructure.security;
import com.example.lumoo.domain.pdpp.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;
import java.io.IOException;
@Component
public class CustomSuccessHandler implements AuthenticationSuccessHandler {
    private static final Logger log = LoggerFactory.getLogger(CustomSuccessHandler.class);
    @Autowired private AuditService auditService;
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String user = authentication.getName();
        log.info("[Auth] Login success — user={} ip={}", user,
                request.getHeader("X-Forwarded-For") != null
                        ? request.getHeader("X-Forwarded-For").split(",")[0].trim()
                        : request.getRemoteAddr());
        auditService.log("USER_LOGIN", "User", user, user);
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
