package com.example.lumoo.infrastructure.security;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PasswordPolicyTest {
    @Test
    void rules() {
        assertFalse(PasswordPolicy.isAcceptable(null));
        assertFalse(PasswordPolicy.isAcceptable(""));
        assertFalse(PasswordPolicy.isAcceptable("        "));
        assertFalse(PasswordPolicy.isAcceptable("1234567"));
        assertTrue(PasswordPolicy.isAcceptable("12345678"));
        assertTrue(PasswordPolicy.isAcceptable("a".repeat(72)));
        assertFalse(PasswordPolicy.isAcceptable("a".repeat(73)));
    }
}
