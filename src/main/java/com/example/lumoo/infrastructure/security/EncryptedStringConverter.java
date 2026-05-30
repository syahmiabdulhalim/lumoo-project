package com.example.lumoo.infrastructure.security;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        DataEncryptor enc = SpringContextHolder.getBean(DataEncryptor.class);
        if (enc == null) return attribute; 
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
