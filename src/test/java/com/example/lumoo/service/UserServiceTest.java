package com.example.lumoo.service;

import com.example.lumoo.domain.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private NotificationRepository notificationRepository;

    @InjectMocks private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("buyer@test.com");
        user.setPassword("encoded-old-password");
        user.setRole(Role.USER);
        user.setVerified(false);
    }

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    void register_success_whenEmailNotTaken() {
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");

        boolean result = userService.register("new@test.com", "password123");

        assertTrue(result);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_fails_whenEmailAlreadyExists() {
        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(user));

        boolean result = userService.register("buyer@test.com", "password123");

        assertFalse(result);
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_newUser_hasRoleUser() {
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        userService.register("new@test.com", "pass");
        verify(userRepository).save(captor.capture());

        assertEquals(Role.USER, captor.getValue().getRole());
    }

    // ── changePassword ────────────────────────────────────────────────────────

    @Test
    void changePassword_success() {
        when(passwordEncoder.matches("correct", user.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("newPass1")).thenReturn("hashed-new");

        UserService.PasswordChangeResult result =
                userService.changePassword(user, "correct", "newPass1", "newPass1");

        assertEquals(UserService.PasswordChangeResult.SUCCESS, result);
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_wrongCurrent() {
        when(passwordEncoder.matches("wrong", user.getPassword())).thenReturn(false);

        assertEquals(UserService.PasswordChangeResult.WRONG_CURRENT,
                userService.changePassword(user, "wrong", "newPass1", "newPass1"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_mismatch() {
        when(passwordEncoder.matches("correct", user.getPassword())).thenReturn(true);

        assertEquals(UserService.PasswordChangeResult.MISMATCH,
                userService.changePassword(user, "correct", "newPass1", "different"));
    }

    @Test
    void changePassword_tooShort() {
        when(passwordEncoder.matches("correct", user.getPassword())).thenReturn(true);

        assertEquals(UserService.PasswordChangeResult.TOO_SHORT,
                userService.changePassword(user, "correct", "short", "short"));
    }

    // ── verifyUser ────────────────────────────────────────────────────────────

    @Test
    void verifyUser_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        boolean result = userService.verifyUser(1L);

        assertTrue(result);
        assertTrue(user.isVerified());
        verify(userRepository).save(user);
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void verifyUser_returnsFalse_whenAlreadyVerified() {
        user.setVerified(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertFalse(userService.verifyUser(1L));
        verify(userRepository, never()).save(any());
    }

    @Test
    void verifyUser_returnsFalse_whenNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertFalse(userService.verifyUser(99L));
    }

    // ── upgradeToVendor ───────────────────────────────────────────────────────

    @Test
    void upgradeToVendor_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        boolean result = userService.upgradeToVendor(1L);

        assertTrue(result);
        assertEquals(Role.VENDOR, user.getRole());
        assertTrue(user.isVerified());
        verify(userRepository).save(user);
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void upgradeToVendor_returnsFalse_whenAlreadyVendor() {
        user.setRole(Role.VENDOR);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertFalse(userService.upgradeToVendor(1L));
        verify(userRepository, never()).save(any());
    }

    @Test
    void upgradeToVendor_returnsFalse_whenAdmin() {
        user.setRole(Role.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertFalse(userService.upgradeToVendor(1L));
    }

    @Test
    void upgradeToVendor_returnsFalse_whenNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertFalse(userService.upgradeToVendor(99L));
    }

    // ── getAndMarkNotificationsRead ───────────────────────────────────────────

    @Test
    void getAndMarkNotificationsRead_marksAllRead_andSaves() {
        Notification n1 = new Notification("msg1", user);
        Notification n2 = new Notification("msg2", user);
        when(notificationRepository.findByUser(user)).thenReturn(List.of(n1, n2));

        userService.getAndMarkNotificationsRead(user);

        assertTrue(n1.isRead());
        assertTrue(n2.isRead());
        verify(notificationRepository).saveAll(List.of(n1, n2));
    }

    @Test
    void getAndMarkNotificationsRead_returnsEmptyList_whenNoNotifications() {
        when(notificationRepository.findByUser(user)).thenReturn(List.of());

        List<Notification> result = userService.getAndMarkNotificationsRead(user);

        assertTrue(result.isEmpty());
        verify(notificationRepository, never()).saveAll(any());
    }

    // ── updateProfile ─────────────────────────────────────────────────────────

    @Test
    void updateProfile_updatesFieldsAndSaves() {
        userService.updateProfile(user, "John Doe", "+220123456", "Banjul");

        assertEquals("John Doe", user.getFullName());
        assertEquals("+220123456", user.getPhone());
        assertEquals("Banjul", user.getAddress());
        verify(userRepository).save(user);
    }
}
