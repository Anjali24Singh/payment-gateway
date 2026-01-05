package com.talentica.paymentgateway.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdUtilTest {

    @AfterEach
    void cleanup() {
        CorrelationIdUtil.clearAll();
    }

    @Test
    @DisplayName("generate, set/get, getOrGenerate, clear")
    void basics() {
        String id1 = CorrelationIdUtil.generate();
        assertThat(id1).isNotBlank();

        CorrelationIdUtil.set(id1);
        assertThat(CorrelationIdUtil.get()).isEqualTo(id1);

        CorrelationIdUtil.clear();
        assertThat(CorrelationIdUtil.get()).isNull();

        String id2 = CorrelationIdUtil.getOrGenerate();
        assertThat(id2).isNotBlank();
        assertThat(CorrelationIdUtil.get()).isEqualTo(id2);
    }
}
