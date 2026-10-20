package com.example.lumoo.infrastructure;
import com.example.lumoo.domain.user.User;
import com.example.lumoo.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class StartupValidatorTest {
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private StartupValidator validator;
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(validator, "encryptionKey", "dGVzdC1rZXktbm90LWZvci1wcm9kdWN0aW9uLXVzZTE=");
        ReflectionTestUtils.setField(validator, "resendKey", "re_live");
        ReflectionTestUtils.setField(validator, "baseUrl", "https://lumoo.my");
        ReflectionTestUtils.setField(validator, "adminPassword", "a-strong-password");
        lenient().when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
    }
    @Test
    void passes_withSafeConfig() {
        assertDoesNotThrow(() -> validator.run(null));
    }
    @Test
    void fails_whenEncryptionKeyIsNot32Bytes() {
        ReflectionTestUtils.setField(validator, "encryptionKey", "c2hvcnQ=");
        assertThrows(IllegalStateException.class, () -> validator.run(null));
    }
    @Test
    void fails_whenEncryptionKeyIsNotBase64() {
        ReflectionTestUtils.setField(validator, "encryptionKey", "not base64!!");
        assertThrows(IllegalStateException.class, () -> validator.run(null));
    }
    @Test
    void fails_whenAdminPasswordIsAdmin123() {
        ReflectionTestUtils.setField(validator, "adminPassword", "admin123");
        assertThrows(IllegalStateException.class, () -> validator.run(null));
    }
    @Test
    void existingAccountWithDefaultPassword_isReported_butDoesNotBlockStartup() {
        User vendor = new User();
        vendor.setPassword("hash");
        when(userRepository.findByEmail("hassan@lumoo.my")).thenReturn(Optional.of(vendor));
        when(passwordEncoder.matches("vendor123", "hash")).thenReturn(true);
        assertDoesNotThrow(() -> validator.run(null));
        verify(passwordEncoder).matches("vendor123", "hash");
    }
}
