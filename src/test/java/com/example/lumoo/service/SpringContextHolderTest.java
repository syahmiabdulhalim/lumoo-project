package com.example.lumoo.service;

import com.example.lumoo.infrastructure.security.SpringContextHolder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertNull;

class SpringContextHolderTest {

    @Test
    void getBean_returnsNull_whenContextNotSet() throws Exception {
        Field f = SpringContextHolder.class.getDeclaredField("context");
        f.setAccessible(true);
        Object original = f.get(null);
        try {
            f.set(null, null);
            assertNull(SpringContextHolder.getBean(String.class));
        } finally {
            f.set(null, original);
        }
    }
}
