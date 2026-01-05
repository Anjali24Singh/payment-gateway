package com.talentica.paymentgateway.dto.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PurchaseRequestTest {

    private PurchaseRequest purchaseRequest;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        purchaseRequest = new PurchaseRequest();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Should create PurchaseRequest with default constructor")
    void defaultConstructor() {
        assertThat(purchaseRequest).isNotNull();
        assertThat(purchaseRequest).isInstanceOf(PaymentRequest.class);
    }

    @Test
    @DisplayName("Should generate correct toString representation")
    void toStringMethod() {
        String result = purchaseRequest.toString();
        
        assertThat(result).contains("PurchaseRequest("); // Lombok format
        assertThat(result).contains(")");
    }

    @Test
    @DisplayName("Should serialize to JSON correctly")
    void jsonSerialization() throws Exception {
        String json = objectMapper.writeValueAsString(purchaseRequest);
        
        assertThat(json).isNotNull();
        assertThat(json).contains("{");
        assertThat(json).contains("}");
    }

    @Test
    @DisplayName("Should deserialize from JSON correctly")
    void jsonDeserialization() throws Exception {
        String json = "{}";
        
        PurchaseRequest result = objectMapper.readValue(json, PurchaseRequest.class);
        
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(PurchaseRequest.class);
    }

    @Test
    @DisplayName("Should inherit from PaymentRequest")
    void inheritance() {
        assertThat(purchaseRequest).isInstanceOf(PaymentRequest.class);
        
        // Verify class hierarchy
        Class<?> superClass = PurchaseRequest.class.getSuperclass();
        assertThat(superClass).isEqualTo(PaymentRequest.class);
    }

    @Test
    @DisplayName("Should have correct class annotations")
    void classAnnotations() {
        var clazz = PurchaseRequest.class;
        
        // Verify Schema annotation
        assertThat(clazz.isAnnotationPresent(io.swagger.v3.oas.annotations.media.Schema.class))
            .isTrue();
    }
}
