package com.example.lumoo.infrastructure.security;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;
@Component
public class DataEncryptor {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;
    private static final String ENC_PREFIX = "ENC:";
    @Value("${app.encryption.key:}")
    private String base64Key;
    private SecretKey secretKey;
    @PostConstruct
    public void init() throws Exception {
        if (base64Key == null || base64Key.isBlank()) {
            KeyGenerator kg = KeyGenerator.getInstance("AES");
            kg.init(256);
            secretKey = kg.generateKey();
            System.err.println("[DataEncryptor] WARNING: ENCRYPTION_KEY not set — using ephemeral key. Set ENCRYPTION_KEY env var (openssl rand -base64 32).");
        } else {
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);
            secretKey = new SecretKeySpec(keyBytes, "AES");
        }
    }
    public String encrypt(String plaintext) {
        if (plaintext == null) return null;
        if (plaintext.startsWith(ENC_PREFIX)) return plaintext; 
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes());
            byte[] result = new byte[IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, result, 0, IV_LENGTH);
            System.arraycopy(encrypted, 0, result, IV_LENGTH, encrypted.length);
            return ENC_PREFIX + Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }
    public String decrypt(String ciphertext) {
        if (ciphertext == null) return null;
        if (!ciphertext.startsWith(ENC_PREFIX)) return ciphertext;
        try {
            byte[] data = Base64.getDecoder().decode(ciphertext.substring(ENC_PREFIX.length()));
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(data, 0, iv, 0, IV_LENGTH);
            byte[] encrypted = new byte[data.length - IV_LENGTH];
            System.arraycopy(data, IV_LENGTH, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH, iv));
            return new String(cipher.doFinal(encrypted));
        } catch (Exception e) {
            return ciphertext;
        }
    }
}
