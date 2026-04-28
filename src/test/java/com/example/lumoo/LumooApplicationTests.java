package com.example.lumoo;

import com.example.lumoo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class LumooApplicationTests {

    @Autowired 
    private UserRepository userRepository;

    @Test
    void testAdminExists() {
        // Ini akan menguji jika akaun admin yang kita buat dalam 
        // DataInitializer.java benar-benar wujud dalam database.
        assertNotNull(userRepository.findByEmail("admin").orElse(null), 
            "Admin account should be initialized on startup");
    }
}