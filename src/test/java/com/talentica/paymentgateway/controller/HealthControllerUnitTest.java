package com.talentica.paymentgateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for HealthController.
 * Tests all health check endpoints without Spring context.
 */
@ExtendWith(MockitoExtension.class)
class HealthControllerUnitTest {

    @InjectMocks
    private HealthController healthController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(healthController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testGetHealthStatus_ReturnsHealthInformation() throws Exception {
        // When & Then
        mockMvc.perform(get("/health/status"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.application").value("Payment Gateway Platform"))
                .andExpect(jsonPath("$.version").value("1.0.0"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testGetDetailedHealth_ReturnsDetailedHealthInformation() throws Exception {
        // When & Then
        mockMvc.perform(get("/health/detailed"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.application").value("Payment Gateway Platform"))
                .andExpect(jsonPath("$.version").value("1.0.0"))
                .andExpect(jsonPath("$.java_version").exists())
                .andExpect(jsonPath("$.memory_info").exists())
                .andExpect(jsonPath("$.memory_info.max_memory_mb").exists())
                .andExpect(jsonPath("$.memory_info.total_memory_mb").exists())
                .andExpect(jsonPath("$.memory_info.free_memory_mb").exists())
                .andExpect(jsonPath("$.memory_info.used_memory_mb").exists())
                .andExpect(jsonPath("$.memory_info.memory_usage_percent").exists())
                .andExpect(jsonPath("$.components").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testGetReadiness_ReturnsReadinessStatus() throws Exception {
        // When & Then
        mockMvc.perform(get("/health/ready"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testGetLiveness_ReturnsLivenessStatus() throws Exception {
        // When & Then
        mockMvc.perform(get("/health/live"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("ALIVE"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testMemoryInfoCalculation_ReturnsValidMemoryData() throws Exception {
        // When & Then
        mockMvc.perform(get("/health/detailed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memory_info.memory_usage_percent").isNumber())
                .andExpect(jsonPath("$.memory_info.max_memory_mb").isNumber())
                .andExpect(jsonPath("$.memory_info.total_memory_mb").isNumber())
                .andExpect(jsonPath("$.memory_info.free_memory_mb").isNumber())
                .andExpect(jsonPath("$.memory_info.used_memory_mb").isNumber());
    }
}
