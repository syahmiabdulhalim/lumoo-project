package com.example.lumoo.config;

import com.example.lumoo.model.User;
import com.example.lumoo.model.Role;
import com.example.lumoo.repository.UserRepository;
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
    admin.setPassword(passwordEncoder.encode(System.getenv("ADMIN_PASSWORD")));
    admin.setRole(Role.ADMIN);
    userRepository.save(admin);
    // System.out.println(">>> Admin created: admin@lumoo.my / " + System.getenv("ADMIN_PASSWORD"));
}
        };
    }
}