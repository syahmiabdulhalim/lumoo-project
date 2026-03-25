package com.example.lumoo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity

public class SecurityConfig {
@Autowired
private CustomSuccessHandler customSuccessHandler;
   @Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
    .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/", "/login", "/register", "/css/**", "/js/**", "/images/**").permitAll()
            .requestMatchers("/cart/**").authenticated()
            .requestMatchers("/admin/**").hasRole("ADMIN")
            .requestMatchers("/vendor/**").hasRole("VENDOR")
            .requestMatchers("/buyer/**", "/cart/**").hasRole("USER")
            .requestMatchers("/.well-known/**").permitAll()
            .requestMatchers("/forgot-password/**", "/reset-password/**").permitAll()
            .anyRequest().authenticated()
        )
        .formLogin(form -> form
            .loginPage("/login")
            // TAMBAH LOGIK DI BAWAH INI:
            .successHandler(customSuccessHandler)
            .permitAll()
        )
        .logout(logout -> logout.logoutSuccessUrl("/").permitAll());

    return http.build();
}

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}