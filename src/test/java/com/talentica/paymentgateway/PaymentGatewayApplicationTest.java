package com.talentica.paymentgateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentGatewayApplicationTest {

    @Test
    @DisplayName("Should have main method for application startup")
    void mainMethodExists() throws NoSuchMethodException {
        // Verify that the main method exists and has correct signature
        var mainMethod = PaymentGatewayApplication.class.getMethod("main", String[].class);
        
        assertThat(mainMethod).isNotNull();
        assertThat(mainMethod.getReturnType()).isEqualTo(void.class);
        assertThat(mainMethod.getParameterTypes()).hasSize(1);
        assertThat(mainMethod.getParameterTypes()[0]).isEqualTo(String[].class);
    }

    @Test
    @DisplayName("Should be annotated with required Spring Boot annotations")
    void hasRequiredAnnotations() {
        var clazz = PaymentGatewayApplication.class;
        
        // Verify Spring Boot annotations
        assertThat(clazz.isAnnotationPresent(org.springframework.boot.autoconfigure.SpringBootApplication.class))
            .isTrue();
        assertThat(clazz.isAnnotationPresent(org.springframework.cache.annotation.EnableCaching.class))
            .isTrue();
        assertThat(clazz.isAnnotationPresent(org.springframework.scheduling.annotation.EnableAsync.class))
            .isTrue();
        assertThat(clazz.isAnnotationPresent(org.springframework.transaction.annotation.EnableTransactionManagement.class))
            .isTrue();
    }

    @Test
    @DisplayName("Should have correct class structure")
    void classStructure() {
        var clazz = PaymentGatewayApplication.class;
        
        assertThat(clazz.getPackage().getName()).isEqualTo("com.talentica.paymentgateway");
        assertThat(clazz.getSimpleName()).isEqualTo("PaymentGatewayApplication");
        assertThat(clazz.getSuperclass()).isEqualTo(Object.class);
    }
}
