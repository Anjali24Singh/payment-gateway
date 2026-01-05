package com.talentica.paymentgateway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SandboxConfigTest {

    private SandboxConfig sandboxConfig;

    @BeforeEach
    void setUp() {
        sandboxConfig = new SandboxConfig();
    }

    @Test
    @DisplayName("Should have default values set correctly")
    void defaultValues() {
        assertThat(sandboxConfig.isEnabled()).isTrue();
        assertThat(sandboxConfig.isMockPayments()).isTrue();
        assertThat(sandboxConfig.isGenerateTestData()).isTrue();
        assertThat(sandboxConfig.getDefaultApiKey()).isEqualTo("sandbox_test_key_123456789");
        assertThat(sandboxConfig.getTestCards()).isNotNull();
        assertThat(sandboxConfig.getMockResponses()).isNotNull();
    }

    @Test
    @DisplayName("Should allow setting and getting enabled flag")
    void enabledProperty() {
        sandboxConfig.setEnabled(false);
        assertThat(sandboxConfig.isEnabled()).isFalse();
        
        sandboxConfig.setEnabled(true);
        assertThat(sandboxConfig.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should allow setting and getting mock payments flag")
    void mockPaymentsProperty() {
        sandboxConfig.setMockPayments(false);
        assertThat(sandboxConfig.isMockPayments()).isFalse();
        
        sandboxConfig.setMockPayments(true);
        assertThat(sandboxConfig.isMockPayments()).isTrue();
    }

    @Test
    @DisplayName("Should allow setting and getting generate test data flag")
    void generateTestDataProperty() {
        sandboxConfig.setGenerateTestData(false);
        assertThat(sandboxConfig.isGenerateTestData()).isFalse();
        
        sandboxConfig.setGenerateTestData(true);
        assertThat(sandboxConfig.isGenerateTestData()).isTrue();
    }

    @Test
    @DisplayName("Should allow setting and getting default API key")
    void defaultApiKeyProperty() {
        String newApiKey = "new_test_key_987654321";
        sandboxConfig.setDefaultApiKey(newApiKey);
        assertThat(sandboxConfig.getDefaultApiKey()).isEqualTo(newApiKey);
    }

    @Test
    @DisplayName("Should allow setting and getting test cards")
    void testCardsProperty() {
        SandboxConfig.TestCards newTestCards = new SandboxConfig.TestCards();
        sandboxConfig.setTestCards(newTestCards);
        assertThat(sandboxConfig.getTestCards()).isEqualTo(newTestCards);
    }

    @Test
    @DisplayName("Should allow setting and getting mock responses")
    void mockResponsesProperty() {
        SandboxConfig.MockResponses newMockResponses = new SandboxConfig.MockResponses();
        sandboxConfig.setMockResponses(newMockResponses);
        assertThat(sandboxConfig.getMockResponses()).isEqualTo(newMockResponses);
    }

    @Test
    @DisplayName("TestCards should have default values set correctly")
    void testCardsDefaultValues() {
        SandboxConfig.TestCards testCards = new SandboxConfig.TestCards();
        
        assertThat(testCards.getSuccessCard()).isEqualTo("4111111111111111");
        assertThat(testCards.getDeclineCard()).isEqualTo("4000000000000002");
        assertThat(testCards.getErrorCard()).isEqualTo("4000000000000119");
        assertThat(testCards.getExpiredCard()).isEqualTo("4000000000000069");
        assertThat(testCards.getCvvFailCard()).isEqualTo("4000000000000127");
    }

    @Test
    @DisplayName("TestCards should allow setting and getting all card properties")
    void testCardsProperties() {
        SandboxConfig.TestCards testCards = new SandboxConfig.TestCards();
        
        testCards.setSuccessCard("1111111111111111");
        assertThat(testCards.getSuccessCard()).isEqualTo("1111111111111111");
        
        testCards.setDeclineCard("2222222222222222");
        assertThat(testCards.getDeclineCard()).isEqualTo("2222222222222222");
        
        testCards.setErrorCard("3333333333333333");
        assertThat(testCards.getErrorCard()).isEqualTo("3333333333333333");
        
        testCards.setExpiredCard("4444444444444444");
        assertThat(testCards.getExpiredCard()).isEqualTo("4444444444444444");
        
        testCards.setCvvFailCard("5555555555555555");
        assertThat(testCards.getCvvFailCard()).isEqualTo("5555555555555555");
    }

    @Test
    @DisplayName("MockResponses should have default values set correctly")
    void mockResponsesDefaultValues() {
        SandboxConfig.MockResponses mockResponses = new SandboxConfig.MockResponses();
        
        assertThat(mockResponses.getDelayMs()).isEqualTo(1000);
        assertThat(mockResponses.getFailureRate()).isEqualTo(0.1);
        assertThat(mockResponses.isRandomFailures()).isFalse();
    }

    @Test
    @DisplayName("MockResponses should allow setting and getting all properties")
    void mockResponsesProperties() {
        SandboxConfig.MockResponses mockResponses = new SandboxConfig.MockResponses();
        
        mockResponses.setDelayMs(2000);
        assertThat(mockResponses.getDelayMs()).isEqualTo(2000);
        
        mockResponses.setFailureRate(0.2);
        assertThat(mockResponses.getFailureRate()).isEqualTo(0.2);
        
        mockResponses.setRandomFailures(true);
        assertThat(mockResponses.isRandomFailures()).isTrue();
    }
}
