package com.example.lumoo;

import com.example.lumoo.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class LumooApplicationTests {

    @Autowired
    private UserRepository userRepository;

    @Test
    void contextLoads_userRepositoryIsWired() {
        // Smoke test: Spring context starts up and JPA repositories are wired correctly.
        // Admin account existence is environment-specific (seeded by DataInitializer on server).
        assertNotNull(userRepository, "UserRepository should be a Spring-managed bean");
    }
}