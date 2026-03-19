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
            // Semak jika Admin sudah wujud
            if (userRepository.findByUsername("admin").isEmpty()) {
                User admin = new User();
                admin.setUsername("admin");
                
                // Kata laluan akan di-encrypt: "admin123"
                admin.setPassword(passwordEncoder.encode("admin123"));
                
                // PENTING: Role mesti bermula dengan "ROLE_" untuk Spring Security
                admin.setRole(Role.ADMIN); 
                
                userRepository.save(admin);
                System.out.println(">>> [LUMOO] Admin account created: admin / admin123");
            }
        };
    }
}