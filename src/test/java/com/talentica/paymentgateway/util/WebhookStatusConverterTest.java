package com.talentica.paymentgateway.util;

import com.talentica.paymentgateway.entity.WebhookStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebhookStatusConverterTest {

    private final WebhookStatusConverter converter = new WebhookStatusConverter();

    @Test
    @DisplayName("Converts enum to DB string and back")
    void convertsBothWays() {
        assertThat(converter.convertToDatabaseColumn(WebhookStatus.DELIVERED)).isEqualTo("DELIVERED");
        assertThat(converter.convertToEntityAttribute("delivered")).isEqualTo(WebhookStatus.DELIVERED);
        assertThat(converter.convertToEntityAttribute(null)).isNull();
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    @DisplayName("Unknown status throws")
    void unknownThrows() {
        assertThatThrownBy(() -> converter.convertToEntityAttribute("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown webhook status");
    }
}
