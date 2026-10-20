package com.example.lumoo.web;
import com.example.lumoo.domain.admin.SiteSettingsService;
import com.example.lumoo.domain.pdpp.CustomerRightsController;
import com.example.lumoo.domain.pdpp.CustomerRightsService;
import com.example.lumoo.domain.user.User;
import com.example.lumoo.domain.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(value = CustomerRightsController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class CustomerRightsControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean CustomerRightsService customerRightsService;
    @MockitoBean SiteSettingsService siteSettingsService;
    @MockitoBean UserService userService;
    @MockitoBean PasswordEncoder passwordEncoder;
    private static UsernamePasswordAuthenticationToken auth(String email) {
        return new UsernamePasswordAuthenticationToken(email, null, List.of());
    }
    private User user(String email) {
        User u = new User();
        u.setId(1L);
        u.setEmail(email);
        u.setPassword("hashed");
        when(userService.findByEmail(email)).thenReturn(Optional.of(u));
        return u;
    }
    @Test
    void requestErasure_returns401_whenNotLoggedIn() throws Exception {
        mvc.perform(post("/api/customer-rights/erasure-request")
                        .param("password", "secret"))
                .andExpect(status().isUnauthorized());
        verify(customerRightsService, never()).processErasureRequest(anyString(), any());
    }
    @Test
    void requestErasure_returns403_whenPasswordWrong() throws Exception {
        user("user@test.com");
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);
        mvc.perform(post("/api/customer-rights/erasure-request")
                        .principal(auth("user@test.com"))
                        .param("password", "wrong"))
                .andExpect(status().isForbidden());
        verify(customerRightsService, never()).processErasureRequest(anyString(), any());
    }
    @Test
    void requestErasure_erasesOwnAccount_ignoringEmailParam() throws Exception {
        user("user@test.com");
        when(passwordEncoder.matches("secret", "hashed")).thenReturn(true);
        when(customerRightsService.processErasureRequest(eq("user@test.com"), any()))
                .thenReturn(Map.of("status", "anonymised"));
        mvc.perform(post("/api/customer-rights/erasure-request")
                        .principal(auth("user@test.com"))
                        .param("password", "secret")
                        .param("email", "victim@test.com"))
                .andExpect(status().isOk());
        verify(customerRightsService).processErasureRequest(eq("user@test.com"), any());
        verify(customerRightsService, never()).processErasureRequest(eq("victim@test.com"), any());
    }
    @Test
    void requestDataAccess_returns401_whenNotLoggedIn() throws Exception {
        mvc.perform(post("/api/customer-rights/data-access-request")
                        .param("email", "user@test.com"))
                .andExpect(status().isUnauthorized());
        verify(customerRightsService, never()).processDataAccessRequest(anyString(), any());
    }
    @Test
    void requestDataAccess_returnsOwnData_ignoringEmailParam() throws Exception {
        when(customerRightsService.processDataAccessRequest(eq("user@test.com"), any()))
                .thenReturn(Map.of("orders", 3));
        mvc.perform(post("/api/customer-rights/data-access-request")
                        .principal(auth("user@test.com"))
                        .param("email", "victim@test.com"))
                .andExpect(status().isOk());
        verify(customerRightsService).processDataAccessRequest(eq("user@test.com"), any());
        verify(customerRightsService, never()).processDataAccessRequest(eq("victim@test.com"), any());
    }
}
