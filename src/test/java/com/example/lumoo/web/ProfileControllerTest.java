package com.example.lumoo.web;
import com.example.lumoo.domain.admin.SiteSettingsService;
import com.example.lumoo.domain.user.ProfileController;
import com.example.lumoo.domain.user.Role;
import com.example.lumoo.domain.user.User;
import com.example.lumoo.domain.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(value = ProfileController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class ProfileControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean UserService userService;
    @MockitoBean SiteSettingsService siteSettingsService;
    private UsernamePasswordAuthenticationToken principal(String email) {
        return new UsernamePasswordAuthenticationToken(email, null, List.of());
    }
    private User user(String email) {
        User u = new User();
        u.setId(1L);
        u.setEmail(email);
        u.setRole(Role.USER);
        return u;
    }
    @Test
    void showProfile_returns200_whenAuthenticated() throws Exception {
        when(userService.findByEmail("buyer@test.com")).thenReturn(Optional.of(user("buyer@test.com")));
        mvc.perform(get("/profile").principal(principal("buyer@test.com")))
                .andExpect(status().isOk());
    }
    @Test
    void updateProfile_redirectsToProfile_whenNoAvatar() throws Exception {
        User u = user("buyer@test.com");
        when(userService.findByEmail("buyer@test.com")).thenReturn(Optional.of(u));
        mvc.perform(post("/profile/update")
                        .principal(principal("buyer@test.com"))
                        .param("fullName", "Syahmi Uzair")
                        .param("phone", "+220111111")
                        .param("address", "Banjul"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/profile"));
        verify(userService).updateProfile(eq(u), eq("Syahmi Uzair"), eq("+220111111"), eq("Banjul"));
    }
    @Test
    void updateProfile_redirectsToProfile_whenAvatarUploaded() throws Exception {
        User u = user("buyer@test.com");
        when(userService.findByEmail("buyer@test.com")).thenReturn(Optional.of(u));
        when(userService.saveAvatar(any())).thenReturn("/uploads/avatar.jpg");
        MockMultipartFile avatar = new MockMultipartFile("avatar", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});
        mvc.perform(multipart("/profile/update")
                        .file(avatar)
                        .principal(principal("buyer@test.com"))
                        .param("fullName", "Syahmi")
                        .param("phone", "+220111111")
                        .param("address", "Banjul"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/profile"));
        verify(userService).updateProfile(eq(u), eq("Syahmi"), eq("+220111111"), eq("Banjul"));
    }
    @Test
    void updateProfile_redirectsWithError_whenAvatarUploadFails() throws Exception {
        User u = user("buyer@test.com");
        when(userService.findByEmail("buyer@test.com")).thenReturn(Optional.of(u));
        when(userService.saveAvatar(any())).thenThrow(new RuntimeException("disk full"));
        MockMultipartFile avatar = new MockMultipartFile("avatar", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});
        mvc.perform(multipart("/profile/update")
                        .file(avatar)
                        .principal(principal("buyer@test.com"))
                        .param("fullName", "Syahmi")
                        .param("phone", "+220111111")
                        .param("address", "Banjul"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/profile"));
        verify(userService, never()).updateProfile(any(), any(), any(), any());
    }
    @Test
    void changePassword_success_redirectsToProfile() throws Exception {
        User u = user("buyer@test.com");
        when(userService.findByEmail("buyer@test.com")).thenReturn(Optional.of(u));
        when(userService.changePassword(u, "old", "new12345", "new12345"))
                .thenReturn(UserService.PasswordChangeResult.SUCCESS);
        mvc.perform(post("/profile/change-password")
                        .principal(principal("buyer@test.com"))
                        .param("currentPassword", "old")
                        .param("newPassword", "new12345")
                        .param("confirmPassword", "new12345"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/profile"));
    }
    @Test
    void changePassword_wrongCurrent_redirectsToProfile() throws Exception {
        User u = user("buyer@test.com");
        when(userService.findByEmail("buyer@test.com")).thenReturn(Optional.of(u));
        when(userService.changePassword(u, "wrong", "new12345", "new12345"))
                .thenReturn(UserService.PasswordChangeResult.WRONG_CURRENT);
        mvc.perform(post("/profile/change-password")
                        .principal(principal("buyer@test.com"))
                        .param("currentPassword", "wrong")
                        .param("newPassword", "new12345")
                        .param("confirmPassword", "new12345"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/profile"));
    }
    @Test
    void changePassword_mismatch_redirectsToProfile() throws Exception {
        User u = user("buyer@test.com");
        when(userService.findByEmail("buyer@test.com")).thenReturn(Optional.of(u));
        when(userService.changePassword(u, "old", "new12345", "different"))
                .thenReturn(UserService.PasswordChangeResult.MISMATCH);
        mvc.perform(post("/profile/change-password")
                        .principal(principal("buyer@test.com"))
                        .param("currentPassword", "old")
                        .param("newPassword", "new12345")
                        .param("confirmPassword", "different"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/profile"));
    }
    @Test
    void changePassword_tooShort_redirectsToProfile() throws Exception {
        User u = user("buyer@test.com");
        when(userService.findByEmail("buyer@test.com")).thenReturn(Optional.of(u));
        when(userService.changePassword(u, "old", "short", "short"))
                .thenReturn(UserService.PasswordChangeResult.TOO_SHORT);
        mvc.perform(post("/profile/change-password")
                        .principal(principal("buyer@test.com"))
                        .param("currentPassword", "old")
                        .param("newPassword", "short")
                        .param("confirmPassword", "short"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/profile"));
    }
}
