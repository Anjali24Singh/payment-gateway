package com.talentica.paymentgateway.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * JPA converter for Map<String, Object> to JSONB column type.
 * Handles conversion between Java Map objects and PostgreSQL JSONB.
 */
@Slf4j
@Converter
public class MapToJsonConverter implements AttributeConverter<Map<String, Object>, String> {
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "{}";
        }
        
        try {
            String json = objectMapper.writeValueAsString(attribute);
            log.debug("Converting map to JSON: {}", json);
            return json;
        } catch (JsonProcessingException e) {
            log.error("Error converting map to JSON", e);
            return "{}";
        }
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return new HashMap<>();
        }
        
        try {
            Map<String, Object> map = objectMapper.readValue(dbData, new TypeReference<Map<String, Object>>() {});
            log.debug("Converting JSON to map: {}", map);
            return map != null ? map : new HashMap<>();
        } catch (IOException e) {
            log.error("Error converting JSON to map: {}", dbData, e);
            return new HashMap<>();
        }
    }
}
