package com.example.lumoo.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class StartupValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupValidator.class);

    @Value("${encryption.key:}")
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
        }

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
}
