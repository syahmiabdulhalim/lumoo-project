package com.example.lumoo.infrastructure.web;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RequestLoggingFilter implements Filter {

    private static final Logger ACCESS = LoggerFactory.getLogger("ACCESS");

    private static final String[] SKIP_EXTENSIONS = {
        ".css", ".js", ".png", ".jpg", ".ico", ".woff", ".woff2", ".svg", ".map"
    };

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  request  = (HttpServletRequest)  req;
        HttpServletResponse response = (HttpServletResponse) res;

        String uri = request.getRequestURI();

        for (String ext : SKIP_EXTENSIONS) {
            if (uri.endsWith(ext)) {
                chain.doFilter(req, res);
                return;
            }
        }

        long start = System.currentTimeMillis();
        chain.doFilter(req, res);
        long ms = System.currentTimeMillis() - start;

        String user = request.getUserPrincipal() != null
                ? request.getUserPrincipal().getName()
                : "anonymous";

        ACCESS.info("{} {} {} {} {}ms [{}]",
                request.getMethod(),
                uri,
                response.getStatus(),
                request.getRemoteAddr(),
                ms,
                user);
    }
}
