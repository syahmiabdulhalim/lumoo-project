package com.example.lumoo.app;
import com.example.lumoo.domain.user.User;
import com.example.lumoo.domain.user.Role;
import com.example.lumoo.domain.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.security.SecureRandom;
import java.util.Base64;
@Configuration
public class DataInitializer {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    static final String ADMIN_EMAIL = "admin@lumoo.my";
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private Environment environment;
    @Bean
    public CommandLineRunner initData() {
        return args -> createAdminIfMissing(System.getenv("ADMIN_PASSWORD"));
    }
    /**
     * Creates the first admin account. There is no default password: prod refuses to start
     * without ADMIN_PASSWORD, other profiles get a random one that is logged once.
     */
    void createAdminIfMissing(String adminPassword) {
        if (userRepository.findByEmail(ADMIN_EMAIL).isPresent()) return;
        if (adminPassword == null || adminPassword.isBlank()) {
            if (environment.acceptsProfiles(Profiles.of("prod")))
                throw new IllegalStateException("ADMIN_PASSWORD must be set to create the admin account in prod.");
            byte[] bytes = new byte[12];
            new SecureRandom().nextBytes(bytes);
            adminPassword = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            log.warn("[DataInitializer] ADMIN_PASSWORD not set — created {} with generated password: {}", ADMIN_EMAIL, adminPassword);
        }
        User admin = new User();
        admin.setUsername(ADMIN_EMAIL);
        admin.setEmail(ADMIN_EMAIL);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
    }
}
