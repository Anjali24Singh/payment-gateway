package com.talentica.paymentgateway.util;

import com.talentica.paymentgateway.entity.SubscriptionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionStatusConverterTest {

    private final SubscriptionStatusConverter converter = new SubscriptionStatusConverter();

    @Test
    @DisplayName("Converts enum to DB string and back with default on bad value")
    void convertsBothWays() {
        assertThat(converter.convertToDatabaseColumn(SubscriptionStatus.ACTIVE)).isEqualTo("ACTIVE");
        assertThat(converter.convertToEntityAttribute("paused")).isEqualTo(SubscriptionStatus.PAUSED);
        assertThat(converter.convertToEntityAttribute("bad")).isEqualTo(SubscriptionStatus.PENDING);
        assertThat(converter.convertToEntityAttribute(null)).isNull();
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }
}
