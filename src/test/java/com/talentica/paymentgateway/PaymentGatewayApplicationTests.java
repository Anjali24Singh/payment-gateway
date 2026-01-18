package com.talentica.paymentgateway;

import com.talentica.paymentgateway.config.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration tests for Payment Gateway Application.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestConfig.class)
class PaymentGatewayApplicationTests {

    /**
     * Test that the Spring Boot application context loads successfully.
     */
//    @Test
//    void contextLoads() {
//        // This test will fail if the application context cannot be loaded
//        // It validates that all beans can be created and autowired correctly
//    }
//
//    /**
//     * Test that the main method runs without exceptions.
//     * Note: This test is disabled as it would start the full application.
//     */
//    @Test
//    void mainMethodTest() {
//        // Simply verify the class exists and is loadable
//        // Running main() would start the server which is not suitable for unit tests
//        assert PaymentGatewayApplication.class != null;
//    }
}
