package com.example.lumoo.infrastructure.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Locks an email for {@link #LOCK_DURATION} after {@link #MAX_FAILURES} consecutive bad passwords.
 * In-memory: the app runs as a single instance (see docker-compose.yml).
 */
@Service
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);
    static final int MAX_FAILURES = 5;
    static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private record Attempts(int failures, Instant lastFailure, Instant lockedUntil) {}

    private final ConcurrentHashMap<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginAttemptService() { this(Clock.systemUTC()); }

    LoginAttemptService(Clock clock) { this.clock = clock; }

    @EventListener
    public void onFailure(AuthenticationFailureBadCredentialsEvent event) {
        loginFailed(String.valueOf(event.getAuthentication().getPrincipal()));
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        loginSucceeded(event.getAuthentication().getName());
    }

    public void loginFailed(String email) {
        String key = key(email);
        Attempts updated = attempts.compute(key, (k, a) -> {
            Instant now = clock.instant();
            int failures = (a == null || isStale(a) ? 0 : a.failures()) + 1;
            Instant lockedUntil = failures >= MAX_FAILURES ? now.plus(LOCK_DURATION) : null;
            return new Attempts(failures, now, lockedUntil);
        });
        if (updated.lockedUntil() != null && updated.failures() == MAX_FAILURES)
            log.warn("[Login] {} locked for {} min after {} failed attempts", key, LOCK_DURATION.toMinutes(), MAX_FAILURES);
    }

    public void loginSucceeded(String email) {
        attempts.remove(key(email));
    }

    public boolean isLocked(String email) {
        Attempts a = attempts.get(key(email));
        return a != null && a.lockedUntil() != null && clock.instant().isBefore(a.lockedUntil());
    }

    /** A lock that has ended, or failures older than the lock window, no longer count. */
    private boolean isStale(Attempts a) {
        Instant now = clock.instant();
        if (a.lockedUntil() != null) return !now.isBefore(a.lockedUntil());
        return a.lastFailure().plus(LOCK_DURATION).isBefore(now);
    }

    @Scheduled(fixedDelay = 10 * 60 * 1000)
    public void evictStale() {
        attempts.values().removeIf(this::isStale);
    }

    private static String key(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
