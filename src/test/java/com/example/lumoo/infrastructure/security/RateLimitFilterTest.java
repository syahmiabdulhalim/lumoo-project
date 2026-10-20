package com.example.lumoo.infrastructure.security;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
class RateLimitFilterTest {
    private RateLimitFilter filter;
    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter();
        ReflectionTestUtils.setField(filter, "generalMax", 100);
        ReflectionTestUtils.setField(filter, "generalWindowMs", 60_000L);
        ReflectionTestUtils.setField(filter, "sensitiveMax", 10);
        ReflectionTestUtils.setField(filter, "sensitiveWindowMs", 60_000L);
    }
    private int post(String ip, String uri, String forwardedFor) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", uri);
        req.setRemoteAddr(ip);
        if (forwardedFor != null) req.addHeader("X-Forwarded-For", forwardedFor);
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain());
        return res.getStatus();
    }
    @Test
    void loginPost_isLimitedTo10PerMinute() throws Exception {
        for (int i = 0; i < 10; i++) assertEquals(200, post("1.2.3.4", "/login", null));
        assertEquals(429, post("1.2.3.4", "/login", null));
        assertEquals(200, post("5.6.7.8", "/login", null)); // other clients unaffected
    }
    @Test
    void spoofedForwardedFor_doesNotResetTheLimit() throws Exception {
        for (int i = 0; i < 10; i++) post("1.2.3.4", "/login", "10.0.0." + i);
        assertEquals(429, post("1.2.3.4", "/login", "9.9.9.9"));
    }
    @Test
    void resendVerification_isLimited() throws Exception {
        for (int i = 0; i < 3; i++) assertEquals(200, post("1.2.3.4", "/resend-verification", null));
        assertEquals(429, post("1.2.3.4", "/resend-verification", null));
    }
    @Test
    void idleKeysAreEvicted() throws Exception {
        post("1.2.3.4", "/login", null);
        ReflectionTestUtils.setField(filter, "generalWindowMs", -1L);
        ReflectionTestUtils.setField(filter, "sensitiveWindowMs", -1L);
        filter.evictIdle();
        assertEquals(0, filter.trackedKeys());
    }
}
