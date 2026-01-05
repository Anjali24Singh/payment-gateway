package com.talentica.paymentgateway.util;

import com.talentica.paymentgateway.entity.WebhookStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA converter for WebhookStatus enum to PostgreSQL webhook_status enum type.
 * Handles conversion between Java enum and PostgreSQL enum.
 */
@Converter(autoApply = true)
public class WebhookStatusConverter implements AttributeConverter<WebhookStatus, String> {

    @Override
    public String convertToDatabaseColumn(WebhookStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name();
    }

    @Override
    public WebhookStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        
        try {
            return WebhookStatus.valueOf(dbData.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown webhook status: " + dbData, e);
        }
    }
}
