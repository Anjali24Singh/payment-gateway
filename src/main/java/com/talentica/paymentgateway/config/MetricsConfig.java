package com.talentica.paymentgateway.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.info.BuildProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metrics Configuration for Payment Gateway Application.
 * 
 * Configures Micrometer metrics with proper tagging, filtering, and custom registrations.
 * Provides comprehensive metrics for payment operations, system health, and performance monitoring.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Configuration
public class MetricsConfig {

    /**
     * Customize meter registry with common tags and filters.
     * 
     * @param buildProperties Build properties for version information (optional)
     * @return Meter registry customizer
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags(@Autowired(required = false) BuildProperties buildProperties) {
        return registry -> {
            // Get version from build properties or default
            String version = buildProperties != null ? buildProperties.getVersion() : "1.0.0";
            
            // Add common tags to all metrics
            registry.config()
                .commonTags(
                    "application", "payment-gateway",
                    "version", version,
                    "environment", System.getProperty("spring.profiles.active", "unknown")
                )
                // Add meter filters
                .meterFilter(MeterFilter.ignoreTags("uri")) // Remove high cardinality URI tags
                .meterFilter(MeterFilter.deny(id -> {
                    String name = id.getName();
                    // Filter out noisy metrics
                    return name.startsWith("jvm.gc.pause") ||
                           name.startsWith("hikaricp.connections.idle") ||
                           name.startsWith("logback.events");
                }));
        };
    }

}
