package com.talentica.paymentgateway.config.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Type-safe configuration properties for application metadata.
 * Replaces @Value annotations in OpenApiConfig with validated configuration beans.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "app")
public class ApplicationProperties {

    /**
     * Application version.
     */
    @NotBlank
    private String version = "1.0.0";

    /**
     * Application name.
     */
    @NotBlank
    private String name = "Payment Gateway API";

    /**
     * Application description.
     */
    private String description = "Authorize.Net Payment Gateway Integration Platform";

    /**
     * Server configuration.
     */
    private Server server = new Server();

    /**
     * Server configuration nested class.
     */
    @Data
    public static class Server {
        /**
         * Server port.
         */
        private int port = 8080;

        /**
         * Servlet context path.
         */
        private String contextPath = "/api/v1";
    }
}
