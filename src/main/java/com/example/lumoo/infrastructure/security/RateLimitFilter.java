package com.example.lumoo.infrastructure.security;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/**
 * Per-IP sliding-window rate limit. Runs as a servlet filter ahead of Spring Security so it
 * also covers the login POST, which Security handles before any MVC interceptor would see it.
 * The client IP is request.getRemoteAddr(): behind Caddy, Tomcat's RemoteIpValve
 * (server.forward-headers-strategy=native) sets it from X-Forwarded-For only for trusted proxies,
 * so clients cannot spoof it.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    @Value("${rate.limit.enabled:true}")
    private boolean enabled = true;
    @Value("${rate.limit.general.max:100}")
    private int generalMax;
    @Value("${rate.limit.general.window-ms:60000}")
    private long generalWindowMs;
    @Value("${rate.limit.sensitive.max:10}")
    private int sensitiveMax;
    @Value("${rate.limit.sensitive.window-ms:60000}")
    private long sensitiveWindowMs;
    private static final Map<String, Integer> SENSITIVE_PREFIXES = Map.of(
            "/api/customer-rights/", 5,
            "/forgot-password", 5,
            "/reset-password", 5,
            "/resend-verification", 3,
            "/login", 10,
            "/register", 10,
            "/subscribe", 20
    );
    /** Hard cap so a flood of distinct IPs cannot exhaust memory between cleanups. */
    static final int MAX_TRACKED_KEYS = 50_000;
    private final ConcurrentHashMap<String, Deque<Long>> requestLog = new ConcurrentHashMap<>();
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (enabled && !allow(request.getRemoteAddr(), request.getRequestURI())) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Too many requests. Please slow down.\"}");
            return;
        }
        chain.doFilter(request, response);
    }
    boolean allow(String ip, String uri) {
        int limit = SENSITIVE_PREFIXES.entrySet().stream()
                .filter(e -> uri.startsWith(e.getKey()))
                .mapToInt(Map.Entry::getValue)
                .findFirst()
                .orElse(generalMax);
        boolean sensitive = limit <= sensitiveMax;
        long windowMs = sensitive ? sensitiveWindowMs : generalWindowMs;
        String key = ip + ":" + (sensitive ? uri : "general");
        if (requestLog.size() >= MAX_TRACKED_KEYS && !requestLog.containsKey(key)) evictIdle();
        Deque<Long> timestamps = requestLog.computeIfAbsent(key, k -> new ArrayDeque<>());
        long now = System.currentTimeMillis();
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > windowMs) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= limit) {
                log.warn("[RateLimit] {} blocked on {} ({}req/{}ms)", ip, uri, limit, windowMs);
                return false;
            }
            timestamps.addLast(now);
        }
        return true;
    }
    /** Drops keys with no request inside the longest window. */
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void evictIdle() {
        long cutoff = System.currentTimeMillis() - Math.max(generalWindowMs, sensitiveWindowMs);
        requestLog.values().removeIf(d -> {
            synchronized (d) { return d.isEmpty() || d.peekLast() < cutoff; }
        });
    }
    int trackedKeys() { return requestLog.size(); }
}
