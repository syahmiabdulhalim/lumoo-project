package com.example.lumoo.infrastructure.security;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.session.HttpSessionEventPublisher;
@Configuration
@EnableWebSecurity
public class SecurityConfig {
@Autowired
private CustomSuccessHandler customSuccessHandler;
@Autowired
private LoginFailureHandler loginFailureHandler;
   @Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf
            .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
            .ignoringRequestMatchers("/api/payment/webhook", "/subscribe", "/api/notifications/read-all")
        )
        // CSP and the other security headers are set once, in SecurityHeadersFilter.
        .headers(headers -> headers
            .frameOptions(frame -> frame.deny())
            .contentTypeOptions(opt -> {})
        )
        .authorizeHttpRequests(auth -> auth
    .requestMatchers("/", "/login", "/register", "/css/**", "/js/**", "/images/**", "/error").permitAll()
    .requestMatchers("/uploads/products/**", "/uploads/avatars/**").permitAll()
    .requestMatchers("/uploads/proofs/**").hasRole("ADMIN")
    .requestMatchers("/uploads/kyc/**").hasRole("ADMIN")
    .requestMatchers("/stores", "/store/**", "/product/**", "/category/**", "/blog", "/blog/**", "/sitemap.xml", "/privacy-policy", "/terms", "/cookie-policy", "/returns", "/subscribe").permitAll()
    .requestMatchers("/careers", "/careers/**", "/partnerships", "/partnerships/**", "/about").permitAll()
    .requestMatchers("/track", "/track/**").permitAll()
    .requestMatchers("/shipping", "/api/shipping/**").permitAll()
    .requestMatchers("/.well-known/**").permitAll()
    .requestMatchers("/forgot-password/**", "/reset-password/**").permitAll()
    .requestMatchers("/verify-email", "/resend-verification").permitAll()
    .requestMatchers("/api/customer-rights/**").authenticated()
    .requestMatchers("/api/payment/webhook").permitAll()
    .requestMatchers("/api/payment/initiate").authenticated()
    .requestMatchers("/api/notifications", "/api/notifications/**").authenticated()
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
            .failureHandler(loginFailureHandler)
            .permitAll()
        )
        .sessionManagement(session -> session
            .sessionFixation().changeSessionId()
            .maximumSessions(5).expiredUrl("/login?expired")
            .sessionRegistry(sessionRegistry())
        )
        .logout(logout -> logout
            .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
            .logoutSuccessUrl("/")
            .invalidateHttpSession(true)
            .deleteCookies("JSESSIONID")
            .permitAll()
        );
    return http.build();
}
    /** Tracks live sessions so SessionRevoker can end them after a password reset or suspension. */
    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
