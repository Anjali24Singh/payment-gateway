package com.talentica.paymentgateway.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MapToJsonConverterTest {

    private MapToJsonConverter converter;

    @BeforeEach
    void setUp() {
        converter = new MapToJsonConverter();
    }

    @Test
    @DisplayName("Should convert map to JSON string successfully")
    void convertToDatabaseColumn_WithValidMap_ShouldReturnJsonString() {
        // Given
        Map<String, Object> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", 123);
        map.put("key3", true);

        // When
        String result = converter.convertToDatabaseColumn(map);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).contains("\"key1\":\"value1\"");
        assertThat(result).contains("\"key2\":123");
        assertThat(result).contains("\"key3\":true");
    }

    @Test
    @DisplayName("Should return empty JSON object for null map")
    void convertToDatabaseColumn_WithNullMap_ShouldReturnEmptyJson() {
        // When
        String result = converter.convertToDatabaseColumn(null);

        // Then
        assertThat(result).isEqualTo("{}");
    }

    @Test
    @DisplayName("Should return empty JSON object for empty map")
    void convertToDatabaseColumn_WithEmptyMap_ShouldReturnEmptyJson() {
        // Given
        Map<String, Object> emptyMap = new HashMap<>();

        // When
        String result = converter.convertToDatabaseColumn(emptyMap);

        // Then
        assertThat(result).isEqualTo("{}");
    }

    @Test
    @DisplayName("Should handle complex objects in map")
    void convertToDatabaseColumn_WithComplexObjects_ShouldSerializeCorrectly() {
        // Given
        Map<String, Object> map = new HashMap<>();
        map.put("timestamp", LocalDateTime.now());
        map.put("nested", Map.of("inner", "value"));

        // When
        String result = converter.convertToDatabaseColumn(map);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).contains("\"timestamp\":");
        assertThat(result).contains("\"nested\":");
    }

    @Test
    @DisplayName("Should convert JSON string to map successfully")
    void convertToEntityAttribute_WithValidJson_ShouldReturnMap() {
        // Given
        String json = "{\"key1\":\"value1\",\"key2\":123,\"key3\":true}";

        // When
        Map<String, Object> result = converter.convertToEntityAttribute(json);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result.get("key1")).isEqualTo("value1");
        assertThat(result.get("key2")).isEqualTo(123);
        assertThat(result.get("key3")).isEqualTo(true);
    }

    @Test
    @DisplayName("Should return empty map for null JSON")
    void convertToEntityAttribute_WithNullJson_ShouldReturnEmptyMap() {
        // When
        Map<String, Object> result = converter.convertToEntityAttribute(null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty map for empty JSON string")
    void convertToEntityAttribute_WithEmptyJson_ShouldReturnEmptyMap() {
        // When
        Map<String, Object> result = converter.convertToEntityAttribute("");

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty map for whitespace JSON string")
    void convertToEntityAttribute_WithWhitespaceJson_ShouldReturnEmptyMap() {
        // When
        Map<String, Object> result = converter.convertToEntityAttribute("   ");

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should handle invalid JSON gracefully")
    void convertToEntityAttribute_WithInvalidJson_ShouldReturnEmptyMap() {
        // Given
        String invalidJson = "{invalid-json}";

        // When
        Map<String, Object> result = converter.convertToEntityAttribute(invalidJson);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should handle malformed JSON gracefully")
    void convertToEntityAttribute_WithMalformedJson_ShouldReturnEmptyMap() {
        // Given
        String malformedJson = "{\"key\":}";

        // When
        Map<String, Object> result = converter.convertToEntityAttribute(malformedJson);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should handle round trip conversion correctly")
    void roundTripConversion_ShouldPreserveData() {
        // Given
        Map<String, Object> originalMap = new HashMap<>();
        originalMap.put("string", "test");
        originalMap.put("number", 42);
        originalMap.put("boolean", false);

        // When
        String json = converter.convertToDatabaseColumn(originalMap);
        Map<String, Object> convertedBack = converter.convertToEntityAttribute(json);

        // Then
        assertThat(convertedBack).isNotNull();
        assertThat(convertedBack.get("string")).isEqualTo("test");
        assertThat(convertedBack.get("number")).isEqualTo(42);
        assertThat(convertedBack.get("boolean")).isEqualTo(false);
    }
}
