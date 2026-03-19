package com.example.lumoo.service;

import com.example.lumoo.model.User;
import com.example.lumoo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("DEBUG LOGIN: Mencuba login -> " + username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    System.out.println("DEBUG LOGIN: User '" + username + "' TAK JUMPA!");
                    return new UsernameNotFoundException("User not found");
                });

        System.out.println("DEBUG LOGIN: User JUMPA! Role: " + user.getRole());

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}