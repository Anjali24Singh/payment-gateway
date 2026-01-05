package com.talentica.paymentgateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main Spring Boot application class for Payment Gateway Integration Platform.
 * 
 * This application provides a comprehensive payment processing solution with:
 * - Authorize.Net integration for payment processing
 * - JWT-based authentication and authorization
 * - Comprehensive observability and monitoring
 * - Async webhook processing
 * - Subscription and recurring billing support
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableTransactionManagement
@EnableJpaAuditing
public class PaymentGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentGatewayApplication.class, args);
    }
}
