package com.example.lumoo.config;

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

   @Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
    .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/", "/login", "/register", "/css/**", "/js/**", "/images/**").permitAll()
            .requestMatchers("/cart/**").authenticated()
            .requestMatchers("/admin/**").hasRole("ADMIN")
            .requestMatchers("/vendor/**").hasRole("VENDOR")
            .requestMatchers("/buyer/**").hasRole("BUYER")
            .anyRequest().authenticated()
        )
        .formLogin(form -> form
            .loginPage("/login")
            // TAMBAH LOGIK DI BAWAH INI:
            .successHandler((request, response, authentication) -> {
                var authorities = authentication.getAuthorities();
                
                // Periksa jika ada request yang tersimpan (seperti klik Add to Cart tadi)
                var session = request.getSession(false);
                var savedRequest = (session != null) ? (org.springframework.security.web.savedrequest.SavedRequest) 
                                session.getAttribute("SPRING_SECURITY_SAVED_REQUEST") : null;

                if (savedRequest != null) {
                    response.sendRedirect(savedRequest.getRedirectUrl());
                    return;
                }

                // Jika tiada saved request, guna logik asal anda
                String redirectUrl = "/";
                for (var authority : authorities) {
                    // TAMBAH 'ROLE_' DI DEPAN NAMA ROLE
                    if (authority.getAuthority().equals("ROLE_ADMIN")) {
                        redirectUrl = "/admin/dashboard"; 
                        break;
                    } else if (authority.getAuthority().equals("ROLE_VENDOR")) {
                        redirectUrl = "/vendor/dashboard"; 
                        break;
                    } else if (authority.getAuthority().equals("ROLE_BUYER")) {
                        redirectUrl = "/"; 
                        break;
                    }
                }
                response.sendRedirect(redirectUrl);
            })
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