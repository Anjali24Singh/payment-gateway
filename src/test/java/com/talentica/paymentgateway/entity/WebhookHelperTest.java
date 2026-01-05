package com.talentica.paymentgateway.entity;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookHelperTest {

    @Test
    void webhook_lifecycle_retry_and_headers() {
        Webhook w = new Webhook("WH-1", "PAYMENT", "EV-1", "https://example.com", new HashMap<>());
        assertThat(w.isPending()).isTrue();
        w.markAsProcessing();
        w.markAsFailed("err");
        assertThat(w.isRetrying() || w.isFailed()).isTrue();
        w.markAsFailedWithResponse(500, new HashMap<>(), "resp", "e");
        w.markAsDelivered(200, new HashMap<>(), "ok");
        assertThat(w.isDelivered()).isTrue();
        assertThat(w.isSuccessfulResponse()).isTrue();
        w.setCorrelationId("corr");
        w.setStandardHeaders();
        w.addRequestHeader("X-Test","1");
        assertThat(w.getRequestHeaders().get("X-Test")).isEqualTo("1");
        assertThat(w.getNextAttemptDescription()).isNotNull();
    }
}
