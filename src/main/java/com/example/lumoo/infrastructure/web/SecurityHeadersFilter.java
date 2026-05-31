package com.example.lumoo.infrastructure.web;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(1)
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;

        response.setHeader("X-Content-Type-Options",    "nosniff");
        response.setHeader("X-Frame-Options",           "SAMEORIGIN");
        response.setHeader("X-XSS-Protection",          "1; mode=block");
        response.setHeader("Referrer-Policy",           "strict-origin-when-cross-origin");
        response.setHeader("Permissions-Policy",        "camera=(), microphone=(), geolocation=()");
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        response.setHeader("Content-Security-Policy",
            "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline' https://cdn.tailwindcss.com https://cdn.jsdelivr.net https://www.googletagmanager.com https://www.google-analytics.com; " +
            "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://cdn.tailwindcss.com https://cdn.jsdelivr.net; " +
            "font-src 'self' https://fonts.gstatic.com; " +
            "img-src 'self' data: https: blob:; " +
            "connect-src 'self' https://www.google-analytics.com; " +
            "frame-ancestors 'self';"
        );

        chain.doFilter(req, res);
    }
}
