package com.example.lumoo.infrastructure.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter that transparently encrypts on write and decrypts on read.
 * Uses ENC: prefix so existing plain-text rows are returned as-is (backwards compatible).
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        DataEncryptor enc = SpringContextHolder.getBean(DataEncryptor.class);
        if (enc == null) return attribute; // Spring not yet ready (e.g. schema init)
        return enc.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        DataEncryptor enc = SpringContextHolder.getBean(DataEncryptor.class);
        if (enc == null) return dbData;
        return enc.decrypt(dbData);
    }
}
