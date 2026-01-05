package com.talentica.paymentgateway.util;

import com.talentica.paymentgateway.entity.SubscriptionStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA converter for SubscriptionStatus enum to handle database enum types.
 * Converts between Java enum and PostgreSQL enum/varchar types.
 */
@Converter(autoApply = false)
public class SubscriptionStatusConverter implements AttributeConverter<SubscriptionStatus, String> {

    @Override
    public String convertToDatabaseColumn(SubscriptionStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name();
    }

    @Override
    public SubscriptionStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        
        try {
            return SubscriptionStatus.valueOf(dbData.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            // Log the error and return default value
            return SubscriptionStatus.PENDING;
        }
    }
}
