package com.example.lumoo.infrastructure;

import com.example.lumoo.domain.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Profile("prod")
public class StartupValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupValidator.class);

    /** Passwords that older builds seeded automatically. Accounts still using them must be changed. */
    static final Map<String, String> SEEDED_DEFAULT_PASSWORDS = Map.of(
            "admin@lumoo.my",  "admin123",
            "hassan@lumoo.my", "vendor123",
            "fatou@lumoo.my",  "vendor123",
            "omar@lumoo.my",   "vendor123");

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Value("${ADMIN_PASSWORD:}")
    private String adminPassword;

    @Value("${app.encryption.key:${encryption.key:}}")
    private String encryptionKey;

    @Value("${resend.api.key:}")
    private String resendKey;

    @Value("${app.base-url:}")
    private String baseUrl;

    @Override
    public void run(ApplicationArguments args) {
        boolean fail = false;

        if (encryptionKey.isBlank()) {
            log.error("STARTUP FAILURE: ENCRYPTION_KEY is not set. All encrypted data will be unreadable. Set ENCRYPTION_KEY in .env");
            fail = true;
        } else if (!isValidAesKey(encryptionKey)) {
            log.error("STARTUP FAILURE: ENCRYPTION_KEY must be base64 of exactly 32 bytes. Generate one with: openssl rand -base64 32");
            fail = true;
        }

        if (SEEDED_DEFAULT_PASSWORDS.containsValue(adminPassword)) {
            log.error("STARTUP FAILURE: ADMIN_PASSWORD is set to a known default. Choose a strong password in .env");
            fail = true;
        }

        SEEDED_DEFAULT_PASSWORDS.forEach((email, defaultPassword) ->
                userRepository.findByEmail(email)
                        .filter(u -> u.getPassword() != null && passwordEncoder.matches(defaultPassword, u.getPassword()))
                        .ifPresent(u -> log.error("STARTUP SECURITY: account {} still uses its seeded default password. Change it or suspend the account now.", email)));

        if (resendKey.isBlank() || resendKey.startsWith("dummy")) {
            log.warn("STARTUP WARNING: RESEND_API_KEY is not set. Emails will not be sent.");
        }

        if (baseUrl.isBlank() || baseUrl.contains("localhost")) {
            log.warn("STARTUP WARNING: APP_BASE_URL is set to '{}'. Payment return URLs will be wrong in production.", baseUrl);
        }

        if (fail) {
            throw new IllegalStateException("Critical environment variables missing — refusing to start. Check logs above.");
        }

        log.info("[Startup] Environment validation passed.");
    }

    static boolean isValidAesKey(String base64) {
        try {
            return java.util.Base64.getDecoder().decode(base64.trim()).length == 32;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
