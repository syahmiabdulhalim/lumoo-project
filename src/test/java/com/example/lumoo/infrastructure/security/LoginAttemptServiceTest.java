package com.example.lumoo.infrastructure.security;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import static org.junit.jupiter.api.Assertions.*;
class LoginAttemptServiceTest {
    /** Mutable clock so the lock window can be fast-forwarded. */
    static class TestClock extends Clock {
        Instant now = Instant.parse("2026-10-21T00:00:00Z");
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
    private final TestClock clock = new TestClock();
    private final LoginAttemptService service = new LoginAttemptService(clock);
    @Test
    void locksAfterFiveFailures_caseInsensitive() {
        for (int i = 0; i < 4; i++) service.loginFailed("User@Test.com");
        assertFalse(service.isLocked("user@test.com"));
        service.loginFailed("user@test.com");
        assertTrue(service.isLocked("USER@test.com"));
    }
    @Test
    void unlocksAfterLockWindow() {
        for (int i = 0; i < 5; i++) service.loginFailed("a@test.com");
        clock.now = clock.now.plus(LoginAttemptService.LOCK_DURATION).plusSeconds(1);
        assertFalse(service.isLocked("a@test.com"));
        service.loginFailed("a@test.com");   // counter starts again
        assertFalse(service.isLocked("a@test.com"));
    }
    @Test
    void successResetsCounter() {
        for (int i = 0; i < 4; i++) service.loginFailed("a@test.com");
        service.loginSucceeded("a@test.com");
        service.loginFailed("a@test.com");
        assertFalse(service.isLocked("a@test.com"));
    }
    @Test
    void oldFailuresExpire() {
        for (int i = 0; i < 4; i++) service.loginFailed("a@test.com");
        clock.now = clock.now.plus(Duration.ofHours(1));
        service.loginFailed("a@test.com");
        assertFalse(service.isLocked("a@test.com"));
    }
}
