package com.example.lumoo.app;

import com.example.lumoo.domain.user.User;
import com.example.lumoo.domain.user.Role;
import com.example.lumoo.domain.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            if (userRepository.findByEmail("admin@lumoo.my").isEmpty()) {
    User admin = new User();
    admin.setUsername("admin@lumoo.my");
    admin.setEmail("admin@lumoo.my");
    String adminPassword = System.getenv("ADMIN_PASSWORD");
if (adminPassword == null) adminPassword = "admin123"; // fallback

admin.setPassword(passwordEncoder.encode(adminPassword));
    admin.setRole(Role.ADMIN);
    userRepository.save(admin);
    // System.out.println(">>> Admin created: admin@lumoo.my / " + System.getenv("ADMIN_PASSWORD"));
}
        };
    }
}