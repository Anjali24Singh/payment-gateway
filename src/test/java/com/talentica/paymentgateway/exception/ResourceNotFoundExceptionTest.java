package com.talentica.paymentgateway.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceNotFoundExceptionTest {

    @Test
    void constructors() {
        ResourceNotFoundException ex1 = new ResourceNotFoundException("not found");
        assertThat(ex1.getMessage()).isEqualTo("not found");
        ResourceNotFoundException ex2 = new ResourceNotFoundException("nf", new RuntimeException("c"));
        assertThat(ex2.getCause()).isNotNull();
    }
}
