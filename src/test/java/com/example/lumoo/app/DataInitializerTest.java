package com.example.lumoo.app;
import com.example.lumoo.domain.user.Role;
import com.example.lumoo.domain.user.User;
import com.example.lumoo.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class DataInitializerTest {
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private Environment environment;
    @InjectMocks private DataInitializer initializer;
    @Test
    void prod_withoutAdminPassword_refusesToStart() {
        when(userRepository.findByEmail(DataInitializer.ADMIN_EMAIL)).thenReturn(Optional.empty());
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> initializer.createAdminIfMissing(null));
        verify(userRepository, never()).save(any());
    }
    @Test
    void dev_withoutAdminPassword_usesRandomPassword_notAdmin123() {
        when(userRepository.findByEmail(DataInitializer.ADMIN_EMAIL)).thenReturn(Optional.empty());
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenAnswer(inv -> "enc:" + inv.getArgument(0));
        initializer.createAdminIfMissing("");
        ArgumentCaptor<String> raw = ArgumentCaptor.forClass(String.class);
        verify(passwordEncoder).encode(raw.capture());
        assertNotEquals("admin123", raw.getValue());
        assertTrue(raw.getValue().length() >= 16);
        verify(userRepository).save(argThat(u -> u.getRole() == Role.ADMIN));
    }
    @Test
    void usesAdminPasswordWhenProvided() {
        when(userRepository.findByEmail(DataInitializer.ADMIN_EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode("S3cure-pass!")).thenReturn("hashed");
        initializer.createAdminIfMissing("S3cure-pass!");
        verify(userRepository).save(argThat(u -> "hashed".equals(u.getPassword())));
    }
    @Test
    void existingAdmin_isLeftUntouched() {
        when(userRepository.findByEmail(DataInitializer.ADMIN_EMAIL)).thenReturn(Optional.of(new User()));
        initializer.createAdminIfMissing(null);
        verify(userRepository, never()).save(any());
    }
}
