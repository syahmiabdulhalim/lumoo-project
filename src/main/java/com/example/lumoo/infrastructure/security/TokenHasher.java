package com.example.lumoo.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Reset and email-verification tokens are emailed in raw form but stored only as SHA-256,
 * so a database leak does not hand out working links.
 */
public final class TokenHasher {

    private TokenHasher() {}

    public static String newToken() {
        return UUID.randomUUID().toString();
    }

    public static String hash(String rawToken) {
        if (rawToken == null) return null;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
