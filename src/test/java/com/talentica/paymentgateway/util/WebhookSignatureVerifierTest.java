package com.talentica.paymentgateway.util;

import com.talentica.paymentgateway.config.properties.WebhookProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookSignatureVerifierTest {

    private WebhookProperties webhookProperties;
    private WebhookSignatureVerifier verifier;

    @BeforeEach
    void setUp() {
        webhookProperties = new WebhookProperties();
        // Initialize with default values
        WebhookProperties.Signature signature = new WebhookProperties.Signature();
        signature.setSecret("test-secret");
        signature.setAlgorithm("HMAC_SHA256");
        signature.setEnabled(true);
        webhookProperties.setSignature(signature);
        
        verifier = new WebhookSignatureVerifier(webhookProperties);
    }

    @Test
    @DisplayName("Disabled verification returns true")
    void disabledVerification() {
        // Given - Disable signature verification
        webhookProperties.getSignature().setEnabled(false);
        verifier = new WebhookSignatureVerifier(webhookProperties);
        
        Map<String, String> headers = new HashMap<>();
        
        // When
        boolean ok = verifier.verifySignature(headers, "payload");
        
        // Then
        assertThat(ok).isTrue();
    }

    @Test
    @DisplayName("Missing secret allows request")
    void missingSecret() {
        // Given - Empty secret
        webhookProperties.getSignature().setEnabled(true);
        webhookProperties.getSignature().setSecret("");
        verifier = new WebhookSignatureVerifier(webhookProperties);
        
        // When
        boolean ok = verifier.verifySignature(new HashMap<>(), "payload");
        
        // Then
        assertThat(ok).isTrue();
    }

    @Test
    @DisplayName("HMAC_SHA256 valid signature")
    void hmacSha256Valid() {
        // Given - Configure HMAC_SHA256 with secret
        webhookProperties.getSignature().setSecret("secret");
        webhookProperties.getSignature().setAlgorithm("HMAC_SHA256");
        verifier = new WebhookSignatureVerifier(webhookProperties);
        
        // When
        String sig = verifier.generateSignatureWithSecret("payload", "secret");
        Map<String, String> headers = Map.of("X-ANET-Signature", sig);
        assertThat(verifier.verifySignature(headers, "payload")).isTrue();
    }

    @Test
    @DisplayName("SHA256 valid signature with alt header")
    void sha256Valid() {
        // Given - Configure SHA256 with secret
        webhookProperties.getSignature().setSecret("secret");
        webhookProperties.getSignature().setAlgorithm("SHA256");
        verifier = new WebhookSignatureVerifier(webhookProperties);
        
        // When
        String sig = verifier.generateSignatureWithSecret("payload", "secret");
        Map<String, String> headers = Map.of("X-Signature", sig);
        
        // Then
        assertThat(verifier.verifySignature(headers, "payload")).isTrue();
    }

    @Test
    @DisplayName("Stripe/GitHub header formats handled")
    void providerHeaders() {
        // Given - Configure HMAC_SHA256 with secret
        webhookProperties.getSignature().setSecret("secret");
        webhookProperties.getSignature().setAlgorithm("HMAC_SHA256");
        verifier = new WebhookSignatureVerifier(webhookProperties);
        
        // When
        String sig = verifier.generateSignatureWithSecret("payload", "secret");
        Map<String, String> gh = Map.of("x-hub-signature-256", "sha256=" + sig);
        Map<String, String> stripe = Map.of("stripe-signature", "t=1,v1=" + sig);
        
        // Then
        assertThat(verifier.verifySignature(gh, "payload")).isTrue();
        assertThat(verifier.verifySignature(stripe, "payload")).isTrue();
    }

    @Test
    @DisplayName("Invalid signature returns false")
    void invalidSignature() {
        // Given - Configure HMAC_SHA256 with secret
        webhookProperties.getSignature().setSecret("secret");
        webhookProperties.getSignature().setAlgorithm("HMAC_SHA256");
        verifier = new WebhookSignatureVerifier(webhookProperties);
        
        // When
        Map<String, String> headers = Map.of("X-ANET-Signature", "deadbeef");
        
        // Then - Use method with explicit secret to avoid any env interference
        assertThat(verifier.verifySignatureWithSecret(headers, "payload", "secret")).isFalse();
    }

    @Test
    @DisplayName("Timestamp validation and config info")
    void timestampAndInfo() {
        // Given - Configure with secret
        webhookProperties.getSignature().setSecret("secret");
        verifier = new WebhookSignatureVerifier(webhookProperties);
        
        // When & Then
        long now = System.currentTimeMillis() / 1000;
        assertThat(verifier.validateTimestamp(now, 5)).isTrue();
        assertThat(verifier.validateTimestamp(now - 6000, 5)).isFalse();
        String info = verifier.getConfigurationInfo();
        assertThat(info).contains("algorithm");
    }
}
