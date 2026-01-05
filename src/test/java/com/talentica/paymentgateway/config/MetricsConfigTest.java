package com.talentica.paymentgateway.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.info.BuildProperties;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class MetricsConfigTest {

    @Test
    @DisplayName("Applies common tags and filters to meter registry")
    void appliesCustomizer() {
        MeterRegistry registry = new SimpleMeterRegistry();
        Properties props = new Properties();
        props.put("version", "9.9.9");
        BuildProperties buildProperties = new BuildProperties(props);

        MetricsConfig config = new MetricsConfig();
        config.metricsCommonTags(buildProperties).customize(registry);

        // Create a counter and ensure tags applied
        registry.counter("test.metric").increment();
        assertThat(registry.get("test.metric").tags("application", "payment-gateway").counter().count()).isGreaterThanOrEqualTo(1.0);

        // Sanity check: creating a meter with a 'uri' tag should not throw and common tags are present
        registry.counter("test.metric2", "uri", "/foo").increment();
        assertThat(registry.get("test.metric2").tags("application", "payment-gateway").counter().count()).isGreaterThanOrEqualTo(1.0);
    }
}
