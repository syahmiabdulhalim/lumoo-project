package com.example.lumoo.web;
import com.example.lumoo.domain.admin.SiteSettingsService;
import com.example.lumoo.domain.user.ForgotPasswordController;
import com.example.lumoo.domain.user.User;
import com.example.lumoo.domain.user.UserRepository;
import com.example.lumoo.infrastructure.email.EmailService;
import com.example.lumoo.infrastructure.security.RateLimitInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(value = ForgotPasswordController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class ForgotPasswordControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean UserRepository userRepository;
    @MockitoBean PasswordEncoder passwordEncoder;
    @MockitoBean EmailService emailService;
    @MockitoBean SiteSettingsService siteSettingsService;
    @MockitoBean RateLimitInterceptor rateLimitInterceptor;
    @BeforeEach
    void allowRequests() throws Exception {
        when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }
    @Test
    void showForgotPage_returns200() throws Exception {
        mvc.perform(get("/forgot-password"))
                .andExpect(status().isOk());
    }
    @Test
    void processForgot_userExists_emailSent_returns200() throws Exception {
        User user = new User();
        user.setEmail("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(emailService.sendResetEmail(any(), any())).thenReturn(true);
        mvc.perform(post("/forgot-password").param("email", "user@test.com"))
                .andExpect(status().isOk());
        verify(userRepository).save(user);
    }
    @Test
    void processForgot_userExists_emailFailed_returns200() throws Exception {
        User user = new User();
        user.setEmail("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(emailService.sendResetEmail(any(), any())).thenReturn(false);
        mvc.perform(post("/forgot-password").param("email", "user@test.com"))
                .andExpect(status().isOk());
    }
    @Test
    void processForgot_userNotFound_returns200() throws Exception {
        when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());
        mvc.perform(post("/forgot-password").param("email", "nobody@test.com"))
                .andExpect(status().isOk());
    }
    @Test
    void showResetPage_validToken_returns200() throws Exception {
        User user = new User();
        user.setTokenExpiry(LocalDateTime.now().plusHours(1));
        when(userRepository.findByResetToken("valid-token")).thenReturn(Optional.of(user));
        mvc.perform(get("/reset-password").param("token", "valid-token"))
                .andExpect(status().isOk());
    }
    @Test
    void showResetPage_tokenNotFound_redirects() throws Exception {
        when(userRepository.findByResetToken("bad-token")).thenReturn(Optional.empty());
        mvc.perform(get("/reset-password").param("token", "bad-token"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/login?error=invalid_token"));
    }
    @Test
    void showResetPage_expiredToken_redirects() throws Exception {
        User user = new User();
        user.setTokenExpiry(LocalDateTime.now().minusHours(1));
        when(userRepository.findByResetToken("expired-token")).thenReturn(Optional.of(user));
        mvc.perform(get("/reset-password").param("token", "expired-token"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/login?error=invalid_token"));
    }
    @Test
    void showResetPage_nullTokenExpiry_redirects() throws Exception {
        User user = new User();
        user.setTokenExpiry(null);
        when(userRepository.findByResetToken("null-expiry")).thenReturn(Optional.of(user));
        mvc.perform(get("/reset-password").param("token", "null-expiry"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/login?error=invalid_token"));
    }
    @Test
    void handleReset_validToken_redirectsToLoginSuccess() throws Exception {
        User user = new User();
        user.setTokenExpiry(LocalDateTime.now().plusHours(1));
        when(userRepository.findByResetToken("valid-token")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPass123")).thenReturn("encoded");
        mvc.perform(post("/reset-password")
                        .param("token", "valid-token")
                        .param("password", "newPass123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/login?reset_success"));
        verify(userRepository).save(user);
    }
    @Test
    void handleReset_invalidToken_redirectsWithError() throws Exception {
        when(userRepository.findByResetToken("bad-token")).thenReturn(Optional.empty());
        mvc.perform(post("/reset-password")
                        .param("token", "bad-token")
                        .param("password", "newPass123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/login?error=invalid_token"));
    }
    @Test
    void handleReset_expiredToken_redirectsWithError() throws Exception {
        User user = new User();
        user.setTokenExpiry(LocalDateTime.now().minusMinutes(5));
        when(userRepository.findByResetToken("expired-token")).thenReturn(Optional.of(user));
        mvc.perform(post("/reset-password")
                        .param("token", "expired-token")
                        .param("password", "newPass123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/login?error=invalid_token"));
    }
}
