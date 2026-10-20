package com.example.lumoo.security;
import com.example.lumoo.domain.user.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
/** Vendors are cached in Redis as JSON inside Product; secrets must never be serialised. */
class UserJsonTest {
    @Test
    void secretsAreNotSerialised() throws Exception {
        User u = new User();
        u.setEmail("vendor@test.com");
        u.setPassword("$2a$10$HASH");
        u.setResetToken("reset-hash");
        u.setEmailVerificationToken("verify-hash");
        u.setPayoutPhone("+2207770000");
        String plainJson = new ObjectMapper().findAndRegisterModules().writeValueAsString(u);
        String redisJson = new String(new GenericJackson2JsonRedisSerializer().serialize(u), StandardCharsets.UTF_8);
        for (String json : new String[]{plainJson, redisJson}) {
            assertTrue(json.contains("vendor@test.com"));
            assertFalse(json.contains("$2a$10$HASH"));
            assertFalse(json.contains("reset-hash"));
            assertFalse(json.contains("verify-hash"));
            assertFalse(json.contains("+2207770000"));
        }
    }
}
