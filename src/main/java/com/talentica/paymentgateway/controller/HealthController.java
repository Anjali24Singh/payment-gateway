package com.talentica.paymentgateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check controller for monitoring application status and dependencies.
 * Provides custom health endpoints beyond Spring Boot Actuator.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/health")
@Tag(name = "Health", description = "Health check and monitoring endpoints")
public class HealthController {

    /**
     * Basic health check endpoint.
     * 
     * @return Health status with timestamp
     */
    @GetMapping("/status")
    @Operation(summary = "Get application health status", 
               description = "Returns basic health status of the application")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Application is healthy"),
        @ApiResponse(responseCode = "503", description = "Application is unhealthy")
    })
    public ResponseEntity<Map<String, Object>> getHealthStatus() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("application", "Payment Gateway Platform");
        health.put("version", "1.0.0");
        
        return ResponseEntity.ok(health);
    }

    /**
     * Detailed health check with system information.
     * 
     * @return Detailed health information
     */
    @GetMapping("/detailed")
    @Operation(summary = "Get detailed health information", 
               description = "Returns detailed health status with system information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Detailed health information retrieved")
    })
    public ResponseEntity<Map<String, Object>> getDetailedHealth() {
        Map<String, Object> health = new HashMap<>();
        Map<String, Object> components = new HashMap<>();
        
        // Application info
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("application", "Payment Gateway Platform");
        health.put("version", "1.0.0");
        health.put("java_version", System.getProperty("java.version"));
        health.put("memory_info", getMemoryInfo());
        health.put("components", components);
        
        return ResponseEntity.ok(health);
    }

    /**
     * Readiness probe for Kubernetes deployments.
     * 
     * @return Readiness status
     */
    @GetMapping("/ready")
    @Operation(summary = "Readiness probe", 
               description = "Indicates if the application is ready to serve traffic")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Application is ready")
    })
    public ResponseEntity<Map<String, Object>> getReadiness() {
        Map<String, Object> readiness = new HashMap<>();
        
        readiness.put("status", "READY");
        readiness.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(readiness);
    }

    /**
     * Liveness probe for Kubernetes deployments.
     * 
     * @return Liveness status
     */
    @GetMapping("/live")
    @Operation(summary = "Liveness probe", 
               description = "Indicates if the application is alive and should not be restarted")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Application is alive"),
        @ApiResponse(responseCode = "503", description = "Application should be restarted")
    })
    public ResponseEntity<Map<String, Object>> getLiveness() {
        Map<String, Object> liveness = new HashMap<>();
        liveness.put("status", "ALIVE");
        liveness.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(liveness);
    }

    /**
     * Get memory information.
     * 
     * @return Memory usage details
     */
    private Map<String, Object> getMemoryInfo() {
        Map<String, Object> memoryInfo = new HashMap<>();
        Runtime runtime = Runtime.getRuntime();
        
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        memoryInfo.put("max_memory_mb", maxMemory / (1024 * 1024));
        memoryInfo.put("total_memory_mb", totalMemory / (1024 * 1024));
        memoryInfo.put("free_memory_mb", freeMemory / (1024 * 1024));
        memoryInfo.put("used_memory_mb", usedMemory / (1024 * 1024));
        memoryInfo.put("memory_usage_percent", Math.round((double) usedMemory / totalMemory * 100));
        
        return memoryInfo;
    }
}
