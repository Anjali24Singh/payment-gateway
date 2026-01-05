package com.talentica.paymentgateway.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CorrelationIdUtil.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
class CorrelationIdUtilUnitTest {

    @BeforeEach
    void setUp() {
        MDC.clear();
    }

    @Test
    void generate_ShouldReturnUUIDString() {
        // When
        String correlationId = CorrelationIdUtil.generate();

        // Then
        assertNotNull(correlationId);
        assertTrue(correlationId.length() > 0);
        // UUID format: 8-4-4-4-12 characters with hyphens
        assertTrue(correlationId.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"));
    }

    @Test
    void generate_MultipleCallsShouldReturnDifferentIds() {
        // When
        String id1 = CorrelationIdUtil.generate();
        String id2 = CorrelationIdUtil.generate();

        // Then
        assertNotNull(id1);
        assertNotNull(id2);
        assertNotEquals(id1, id2);
    }

    @Test
    void get_WithNoMDCValue_ShouldReturnNull() {
        // When
        String correlationId = CorrelationIdUtil.get();

        // Then
        assertNull(correlationId);
    }

    @Test
    void get_WithMDCValue_ShouldReturnValue() {
        // Given
        String expectedId = "test-correlation-id";
        MDC.put(CorrelationIdUtil.CORRELATION_ID_KEY, expectedId);

        // When
        String correlationId = CorrelationIdUtil.get();

        // Then
        assertEquals(expectedId, correlationId);
    }

    @Test
    void set_WithValidId_ShouldSetMDC() {
        // Given
        String correlationId = "test-correlation-id";

        // When
        CorrelationIdUtil.set(correlationId);

        // Then
        assertEquals(correlationId, MDC.get(CorrelationIdUtil.CORRELATION_ID_KEY));
    }

    @Test
    void set_WithNullId_ShouldNotSetMDC() {
        // When
        CorrelationIdUtil.set(null);

        // Then
        assertNull(MDC.get(CorrelationIdUtil.CORRELATION_ID_KEY));
    }

    @Test
    void set_WithEmptyId_ShouldNotSetMDC() {
        // When
        CorrelationIdUtil.set("");

        // Then
        assertNull(MDC.get(CorrelationIdUtil.CORRELATION_ID_KEY));
    }

    @Test
    void set_WithWhitespaceId_ShouldNotSetMDC() {
        // When
        CorrelationIdUtil.set("   ");

        // Then
        assertNull(MDC.get(CorrelationIdUtil.CORRELATION_ID_KEY));
    }

    @Test
    void clear_ShouldRemoveCorrelationIdFromMDC() {
        // Given
        String correlationId = "test-correlation-id";
        MDC.put(CorrelationIdUtil.CORRELATION_ID_KEY, correlationId);
        assertEquals(correlationId, MDC.get(CorrelationIdUtil.CORRELATION_ID_KEY));

        // When
        CorrelationIdUtil.clear();

        // Then
        assertNull(MDC.get(CorrelationIdUtil.CORRELATION_ID_KEY));
    }

    @Test
    void clear_WithOtherMDCValues_ShouldOnlyRemoveCorrelationId() {
        // Given
        String correlationId = "test-correlation-id";
        String otherKey = "otherKey";
        String otherValue = "otherValue";
        
        MDC.put(CorrelationIdUtil.CORRELATION_ID_KEY, correlationId);
        MDC.put(otherKey, otherValue);

        // When
        CorrelationIdUtil.clear();

        // Then
        assertNull(MDC.get(CorrelationIdUtil.CORRELATION_ID_KEY));
        assertEquals(otherValue, MDC.get(otherKey));
    }

    @Test
    void clearAll_ShouldRemoveAllMDCValues() {
        // Given
        String correlationId = "test-correlation-id";
        String otherKey = "otherKey";
        String otherValue = "otherValue";
        
        MDC.put(CorrelationIdUtil.CORRELATION_ID_KEY, correlationId);
        MDC.put(otherKey, otherValue);

        // When
        CorrelationIdUtil.clearAll();

        // Then
        assertNull(MDC.get(CorrelationIdUtil.CORRELATION_ID_KEY));
        assertNull(MDC.get(otherKey));
    }

    @Test
    void getOrGenerate_WithExistingId_ShouldReturnExisting() {
        // Given
        String existingId = "existing-correlation-id";
        MDC.put(CorrelationIdUtil.CORRELATION_ID_KEY, existingId);

        // When
        String correlationId = CorrelationIdUtil.getOrGenerate();

        // Then
        assertEquals(existingId, correlationId);
        assertEquals(existingId, MDC.get(CorrelationIdUtil.CORRELATION_ID_KEY));
    }

    @Test
    void getOrGenerate_WithoutExistingId_ShouldGenerateAndSet() {
        // When
        String correlationId = CorrelationIdUtil.getOrGenerate();

        // Then
        assertNotNull(correlationId);
        assertTrue(correlationId.length() > 0);
        assertEquals(correlationId, MDC.get(CorrelationIdUtil.CORRELATION_ID_KEY));
        // Should be a valid UUID format
        assertTrue(correlationId.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"));
    }

    @Test
    void getOrGenerate_WithEmptyExistingId_ShouldGenerateNew() {
        // Given
        MDC.put(CorrelationIdUtil.CORRELATION_ID_KEY, "");

        // When
        String correlationId = CorrelationIdUtil.getOrGenerate();

        // Then
        assertNotNull(correlationId);
        assertTrue(correlationId.length() > 0);
        assertNotEquals("", correlationId);
        assertEquals(correlationId, MDC.get(CorrelationIdUtil.CORRELATION_ID_KEY));
    }

    @Test
    void getOrGenerate_WithWhitespaceExistingId_ShouldGenerateNew() {
        // Given
        MDC.put(CorrelationIdUtil.CORRELATION_ID_KEY, "   ");

        // When
        String correlationId = CorrelationIdUtil.getOrGenerate();

        // Then
        assertNotNull(correlationId);
        assertTrue(correlationId.length() > 0);
        assertNotEquals("   ", correlationId);
        assertEquals(correlationId, MDC.get(CorrelationIdUtil.CORRELATION_ID_KEY));
    }

    @Test
    void constants_ShouldHaveExpectedValues() {
        // Then
        assertEquals("correlationId", CorrelationIdUtil.CORRELATION_ID_KEY);
        assertEquals("X-Correlation-ID", CorrelationIdUtil.CORRELATION_ID_HEADER);
    }

    @Test
    void set_WithValidIdAfterClear_ShouldSetMDC() {
        // Given
        String firstId = "first-id";
        String secondId = "second-id";
        
        CorrelationIdUtil.set(firstId);
        assertEquals(firstId, MDC.get(CorrelationIdUtil.CORRELATION_ID_KEY));
        
        CorrelationIdUtil.clear();
        assertNull(MDC.get(CorrelationIdUtil.CORRELATION_ID_KEY));

        // When
        CorrelationIdUtil.set(secondId);

        // Then
        assertEquals(secondId, MDC.get(CorrelationIdUtil.CORRELATION_ID_KEY));
    }

    @Test
    void getOrGenerate_MultipleCallsWithSameExisting_ShouldReturnSame() {
        // Given
        String existingId = "existing-id";
        MDC.put(CorrelationIdUtil.CORRELATION_ID_KEY, existingId);

        // When
        String id1 = CorrelationIdUtil.getOrGenerate();
        String id2 = CorrelationIdUtil.getOrGenerate();

        // Then
        assertEquals(existingId, id1);
        assertEquals(existingId, id2);
        assertEquals(id1, id2);
    }

    @Test
    void getOrGenerate_MultipleCallsWithoutExisting_ShouldReturnSameGenerated() {
        // When
        String id1 = CorrelationIdUtil.getOrGenerate();
        String id2 = CorrelationIdUtil.getOrGenerate();

        // Then
        assertNotNull(id1);
        assertNotNull(id2);
        assertEquals(id1, id2); // Should return the same generated ID from MDC
    }

    @Test
    void set_WithSpecialCharacters_ShouldSetMDC() {
        // Given
        String correlationId = "test-id_123-abc";

        // When
        CorrelationIdUtil.set(correlationId);

        // Then
        assertEquals(correlationId, MDC.get(CorrelationIdUtil.CORRELATION_ID_KEY));
    }

    @Test
    void set_WithLongId_ShouldSetMDC() {
        // Given
        String longId = "a".repeat(200);

        // When
        CorrelationIdUtil.set(longId);

        // Then
        assertEquals(longId, MDC.get(CorrelationIdUtil.CORRELATION_ID_KEY));
    }

    @Test
    void clear_WhenNoCorrelationIdSet_ShouldNotThrow() {
        // When & Then - should not throw any exception
        assertDoesNotThrow(() -> {
            CorrelationIdUtil.clear();
        });
        
        assertNull(MDC.get(CorrelationIdUtil.CORRELATION_ID_KEY));
    }

    @Test
    void clearAll_WhenNoMDCSet_ShouldNotThrow() {
        // When & Then - should not throw any exception
        assertDoesNotThrow(() -> {
            CorrelationIdUtil.clearAll();
        });
    }

    @Test
    void threadSafety_MultipleThreadsGenerating_ShouldReturnDifferentIds() throws InterruptedException {
        // Given
        final String[] ids = new String[2];
        Thread thread1 = new Thread(() -> ids[0] = CorrelationIdUtil.generate());
        Thread thread2 = new Thread(() -> ids[1] = CorrelationIdUtil.generate());

        // When
        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        // Then
        assertNotNull(ids[0]);
        assertNotNull(ids[1]);
        assertNotEquals(ids[0], ids[1]);
    }
}
