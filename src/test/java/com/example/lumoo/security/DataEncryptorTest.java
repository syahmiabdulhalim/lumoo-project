package com.example.lumoo.security;
import com.example.lumoo.infrastructure.security.DataEncryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
class DataEncryptorTest {
    private DataEncryptor encryptor;
    @BeforeEach
    void setUp() throws Exception {
        encryptor = new DataEncryptor();
        ReflectionTestUtils.setField(encryptor, "base64Key", "");
        encryptor.init();
    }
    @Test
    void encryptThenDecrypt_returnsOriginalValue() {
        String original = "+220 123 4567";
        String decrypted = encryptor.decrypt(encryptor.encrypt(original));
        assertEquals(original, decrypted);
    }
    @Test
    void encrypt_null_returnsNull() {
        assertNull(encryptor.encrypt(null));
    }
    @Test
    void decrypt_null_returnsNull() {
        assertNull(encryptor.decrypt(null));
    }
    @Test
    void encrypt_alreadyEncrypted_doesNotDoubleEncrypt() {
        String original = "123 Main Street";
        String encrypted = encryptor.encrypt(original);
        String encryptedAgain = encryptor.encrypt(encrypted);
        assertEquals(encrypted, encryptedAgain);
    }
    @Test
    void decrypt_plainText_returnsAsIs() {
        String plainText = "old plain text phone number";
        assertEquals(plainText, encryptor.decrypt(plainText));
    }
    @Test
    void encrypt_producesDifferentCiphertextsForSameInput() {
        String original = "+220 987 6543";
        String first = encryptor.encrypt(original);
        String second = encryptor.encrypt(original);
        assertNotEquals(first, second);
        assertEquals(encryptor.decrypt(first), encryptor.decrypt(second));
    }
    @Test
    void encrypted_valueStartsWithPrefix() {
        String encrypted = encryptor.encrypt("test");
        assertTrue(encrypted.startsWith("ENC:"));
    }
    @Test
    void configuredKey_decryptsAcrossInstances() throws Exception {
        DataEncryptor a = new DataEncryptor();
        ReflectionTestUtils.setField(a, "base64Key", "dGVzdC1rZXktbm90LWZvci1wcm9kdWN0aW9uLXVzZTE=");
        a.init();
        DataEncryptor b = new DataEncryptor();
        ReflectionTestUtils.setField(b, "base64Key", "dGVzdC1rZXktbm90LWZvci1wcm9kdWN0aW9uLXVzZTE=");
        b.init();
        assertEquals("+220 777 0000", b.decrypt(a.encrypt("+220 777 0000")));
    }
    @Test
    void wrongLengthKey_isRejected() {
        DataEncryptor e = new DataEncryptor();
        ReflectionTestUtils.setField(e, "base64Key", "c2hvcnQ=");
        assertThrows(IllegalStateException.class, e::init);
    }
}
