package com.example.lumoo.infrastructure.security;
import java.nio.charset.StandardCharsets;

/** Server-side password rule shared by register, reset and change-password. */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    /** BCrypt only uses the first 72 bytes; refuse longer input instead of silently truncating it. */
    public static final int MAX_BYTES = 72;
    public static final String REQUIREMENT = "Password must be 8–72 characters.";

    private PasswordPolicy() {}

    public static boolean isAcceptable(String password) {
        return password != null
                && !password.isBlank()
                && password.length() >= MIN_LENGTH
                && password.getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES;
    }
}
