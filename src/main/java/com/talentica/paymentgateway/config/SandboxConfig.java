package com.talentica.paymentgateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration for sandbox environment settings.
 * Provides test data, mock responses, and development-specific configurations.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "app.sandbox")
@Profile({"dev", "sandbox", "test"})
public class SandboxConfig {

    private boolean enabled = true;
    private boolean mockPayments = true;
    private boolean generateTestData = true;
    private String defaultApiKey = "sandbox_test_key_123456789";
    private TestCards testCards = new TestCards();
    private MockResponses mockResponses = new MockResponses();

    // Getters and setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isMockPayments() { return mockPayments; }
    public void setMockPayments(boolean mockPayments) { this.mockPayments = mockPayments; }
    public boolean isGenerateTestData() { return generateTestData; }
    public void setGenerateTestData(boolean generateTestData) { this.generateTestData = generateTestData; }
    public String getDefaultApiKey() { return defaultApiKey; }
    public void setDefaultApiKey(String defaultApiKey) { this.defaultApiKey = defaultApiKey; }
    public TestCards getTestCards() { return testCards; }
    public void setTestCards(TestCards testCards) { this.testCards = testCards; }
    public MockResponses getMockResponses() { return mockResponses; }
    public void setMockResponses(MockResponses mockResponses) { this.mockResponses = mockResponses; }

    /**
     * Test card configurations for sandbox testing.
     */
    public static class TestCards {
        private String successCard = "4111111111111111";
        private String declineCard = "4000000000000002";
        private String errorCard = "4000000000000119";
        private String expiredCard = "4000000000000069";
        private String cvvFailCard = "4000000000000127";

        // Getters and setters
        public String getSuccessCard() { return successCard; }
        public void setSuccessCard(String successCard) { this.successCard = successCard; }
        public String getDeclineCard() { return declineCard; }
        public void setDeclineCard(String declineCard) { this.declineCard = declineCard; }
        public String getErrorCard() { return errorCard; }
        public void setErrorCard(String errorCard) { this.errorCard = errorCard; }
        public String getExpiredCard() { return expiredCard; }
        public void setExpiredCard(String expiredCard) { this.expiredCard = expiredCard; }
        public String getCvvFailCard() { return cvvFailCard; }
        public void setCvvFailCard(String cvvFailCard) { this.cvvFailCard = cvvFailCard; }
    }

    /**
     * Mock response configurations for testing different scenarios.
     */
    public static class MockResponses {
        private int delayMs = 1000;
        private double failureRate = 0.1; // 10% failure rate
        private boolean randomFailures = false;

        // Getters and setters
        public int getDelayMs() { return delayMs; }
        public void setDelayMs(int delayMs) { this.delayMs = delayMs; }
        public double getFailureRate() { return failureRate; }
        public void setFailureRate(double failureRate) { this.failureRate = failureRate; }
        public boolean isRandomFailures() { return randomFailures; }
        public void setRandomFailures(boolean randomFailures) { this.randomFailures = randomFailures; }
    }
}
