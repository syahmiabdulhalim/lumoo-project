package com.example.lumoo.security;
import com.example.lumoo.domain.user.Role;
import com.example.lumoo.domain.user.User;
import com.example.lumoo.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
/** Real SecurityConfig + H2: lockout and the email-verification rule at login. */
@SpringBootTest
@AutoConfigureMockMvc
class LoginIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    private void user(String email, boolean verificationRequired, boolean verified) {
        if (userRepository.findByEmail(email).isPresent()) return;
        User u = new User();
        u.setEmail(email);
        u.setUsername(email);
        u.setPassword(passwordEncoder.encode("correct-password"));
        u.setRole(Role.USER);
        u.setEmailVerificationRequired(verificationRequired);
        u.setEmailVerified(verified);
        userRepository.save(u);
    }
    private org.springframework.test.web.servlet.ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/login").with(csrf()).param("username", email).param("password", password));
    }
    @BeforeEach
    void users() {
        user("legacy@test.com", false, false);
        user("new-unverified@test.com", true, false);
        user("new-verified@test.com", true, true);
        user("lockme@test.com", false, true);
    }
    @Test
    void existingUnverifiedAccount_canStillLogIn() throws Exception {
        login("legacy@test.com", "correct-password").andExpect(redirectedUrl("/"));
    }
    @Test
    void newUnverifiedAccount_isSentToVerify() throws Exception {
        login("new-unverified@test.com", "correct-password").andExpect(redirectedUrl("/login?unverified"));
    }
    @Test
    void newVerifiedAccount_logsIn() throws Exception {
        login("new-verified@test.com", "correct-password").andExpect(redirectedUrl("/"));
    }
    @Test
    void fiveBadPasswords_lockEvenTheCorrectOne() throws Exception {
        for (int i = 0; i < 5; i++) login("lockme@test.com", "wrong").andExpect(redirectedUrl("/login?error"));
        login("lockme@test.com", "correct-password").andExpect(redirectedUrl("/login?locked"));
    }
}
