package com.example.lumoo.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    @Value("${rate.limit.general.max:100}")
    private int generalMax;

    @Value("${rate.limit.general.window-ms:60000}")
    private long generalWindowMs;

    @Value("${rate.limit.sensitive.max:10}")
    private int sensitiveMax;

    @Value("${rate.limit.sensitive.window-ms:60000}")
    private long sensitiveWindowMs;

    // Sensitive paths get a lower rate limit
    private static final Map<String, Integer> SENSITIVE_PREFIXES = Map.of(
            "/api/customer-rights/", 5,
            "/forgot-password", 5,
            "/reset-password", 5,
            "/login", 10,
            "/register", 10,
            "/subscribe", 20
    );

    // Per-IP request timestamps
    private final ConcurrentHashMap<String, Deque<Long>> requestLog = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ip = getClientIp(request);
        String uri = request.getRequestURI();

        // Determine limit for this path
        int limit = SENSITIVE_PREFIXES.entrySet().stream()
                .filter(e -> uri.startsWith(e.getKey()))
                .mapToInt(Map.Entry::getValue)
                .findFirst()
                .orElse(generalMax);

        long windowMs = (limit <= sensitiveMax) ? sensitiveWindowMs : generalWindowMs;

        String key = ip + ":" + (limit <= sensitiveMax ? uri : "general");
        Deque<Long> timestamps = requestLog.computeIfAbsent(key, k -> new ArrayDeque<>());
        long now = System.currentTimeMillis();

        synchronized (timestamps) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > windowMs) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= limit) {
                log.warn("[RateLimit] {} blocked on {} ({}req/{}ms)", ip, uri, limit, windowMs);
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"success\":false,\"message\":\"Too many requests. Please slow down.\"}");
                return false;
            }
            timestamps.addLast(now);
        }
        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
