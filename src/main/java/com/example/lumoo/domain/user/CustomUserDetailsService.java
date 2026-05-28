package com.example.lumoo.domain.user;

import com.example.lumoo.domain.user.User;
import com.example.lumoo.domain.user.UserRepository;
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
    // username parameter ni actually akan receive email sekarang
    User user = userRepository.findByEmail(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    return new org.springframework.security.core.userdetails.User(
            user.getEmail(), // ← guna email, bukan username
            user.getPassword(),
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
    );
}
}