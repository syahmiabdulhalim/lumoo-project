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
        // Empty key → ephemeral AES-256 key generated in init()
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
        // Should return same value — idempotent on already-encrypted input
        assertEquals(encrypted, encryptedAgain);
    }

    @Test
    void decrypt_plainText_returnsAsIs() {
        // Rows in DB before encryption was enabled are plain text (no ENC: prefix)
        String plainText = "old plain text phone number";
        assertEquals(plainText, encryptor.decrypt(plainText));
    }

    @Test
    void encrypt_producesDifferentCiphertextsForSameInput() {
        // Random IV means same plaintext → different ciphertext each time
        String original = "+220 987 6543";
        String first = encryptor.encrypt(original);
        String second = encryptor.encrypt(original);
        assertNotEquals(first, second);
        // But both decrypt to same value
        assertEquals(encryptor.decrypt(first), encryptor.decrypt(second));
    }

    @Test
    void encrypted_valueStartsWithPrefix() {
        String encrypted = encryptor.encrypt("test");
        assertTrue(encrypted.startsWith("ENC:"));
    }
}
