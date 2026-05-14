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
        .headers(headers -> headers
            .frameOptions(frame -> frame.deny())
            .contentTypeOptions(opt -> {})
            .contentSecurityPolicy(csp -> csp.policyDirectives(
                "default-src 'self'; " +
                "script-src 'self' 'unsafe-inline' https://cdn.tailwindcss.com https://cdn.jsdelivr.net; " +
                "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                "font-src 'self' https://fonts.gstatic.com; " +
                "img-src 'self' data: https: blob:; " +
                "frame-ancestors 'none';"
            ))
        )
        .authorizeHttpRequests(auth -> auth
    .requestMatchers("/", "/login", "/register", "/css/**", "/js/**", "/images/**").permitAll()
    .requestMatchers("/uploads/products/**", "/uploads/avatars/**").permitAll()
    .requestMatchers("/uploads/kyc/**").hasRole("ADMIN")
    .requestMatchers("/stores", "/store/**", "/product/**", "/category/**", "/blog", "/blog/**", "/sitemap.xml").permitAll()
    .requestMatchers("/.well-known/**").permitAll()
    .requestMatchers("/forgot-password/**", "/reset-password/**").permitAll()
    .requestMatchers("/admin/**").hasRole("ADMIN")
    .requestMatchers("/vendor/**").hasRole("VENDOR")

    .requestMatchers("/buyer/vendorapply").authenticated()

    .requestMatchers("/buyer/**").authenticated()
    .requestMatchers("/cart/**").authenticated()
    .anyRequest().authenticated()
)
        .formLogin(form -> form
            .loginPage("/login")
            .usernameParameter("username")
            .successHandler(customSuccessHandler)
            .permitAll()
        )
        .sessionManagement(session -> session
            .sessionFixation().changeSessionId()
            .maximumSessions(5).expiredUrl("/login?expired")
        )
        .logout(logout -> logout
            .logoutSuccessUrl("/")
            .invalidateHttpSession(true)
            .deleteCookies("JSESSIONID")
            .permitAll()
        );

    return http.build();
}

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}