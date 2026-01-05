package com.talentica.paymentgateway.mapper;

import com.talentica.paymentgateway.dto.subscription.CreateSubscriptionRequest;
import com.talentica.paymentgateway.dto.subscription.PlanResponse;
import com.talentica.paymentgateway.dto.subscription.SubscriptionResponse;
import com.talentica.paymentgateway.entity.Subscription;
import com.talentica.paymentgateway.entity.SubscriptionPlan;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * MapStruct mapper for Subscription and SubscriptionPlan entity to DTO conversions.
 * Handles mapping between subscription domain entities and data transfer objects.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Mapper(componentModel = "spring")
public interface SubscriptionMapper {

    SubscriptionMapper INSTANCE = Mappers.getMapper(SubscriptionMapper.class);

    /**
     * Map Subscription entity to SubscriptionResponse DTO.
     */
    @Mapping(source = "subscriptionId", target = "subscriptionId")
    @Mapping(source = "customer.customerReference", target = "customerId")
    @Mapping(source = "plan.planCode", target = "planCode")
    @Mapping(source = "plan.name", target = "planName")
    @Mapping(source = "plan.amount", target = "planAmount")
    @Mapping(source = "plan.currency", target = "currency")
    @Mapping(source = "status", target = "status")
    @Mapping(target = "customerName", ignore = true)
    @Mapping(target = "customerEmail", ignore = true)
    SubscriptionResponse toSubscriptionResponse(Subscription subscription);

    /**
     * Map SubscriptionPlan entity to PlanResponse DTO.
     */
    @Mapping(source = "planCode", target = "planCode")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "description", target = "description")
    @Mapping(source = "amount", target = "amount")
    @Mapping(source = "currency", target = "currency")
    @Mapping(source = "intervalUnit", target = "intervalUnit")
    @Mapping(source = "intervalCount", target = "intervalCount")
    @Mapping(source = "trialPeriodDays", target = "trialPeriodDays")
    @Mapping(target = "setupFee", ignore = true)
    PlanResponse toPlanResponse(SubscriptionPlan plan);

    /**
     * Map CreateSubscriptionRequest DTO to Subscription entity.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "subscriptionId", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "plan", ignore = true)
    @Mapping(target = "paymentMethod", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "currentPeriodStart", ignore = true)
    @Mapping(target = "currentPeriodEnd", ignore = true)
    @Mapping(target = "trialStart", ignore = true)
    @Mapping(target = "trialEnd", ignore = true)
    @Mapping(target = "nextBillingDate", ignore = true)
    @Mapping(target = "billingCycleAnchor", ignore = true)
    @Mapping(target = "cancelledAt", ignore = true)
    @Mapping(target = "cancellationReason", ignore = true)
    @Mapping(target = "metadata", ignore = true)
    @Mapping(target = "invoices", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Subscription toSubscription(CreateSubscriptionRequest request);

    /**
     * Format LocalDateTime to ISO 8601 string.
     * Converts LocalDateTime to ZonedDateTime using system default timezone.
     */
    @Named("formatDateTime")
    default String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        ZonedDateTime zonedDateTime = dateTime.atZone(ZoneId.systemDefault());
        return zonedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    /**
     * Convert LocalDateTime to ZonedDateTime using system default timezone.
     * This method is used automatically by MapStruct for LocalDateTime to ZonedDateTime mappings.
     */
    default ZonedDateTime map(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atZone(ZoneId.systemDefault());
    }

    /**
     * Convert ZonedDateTime to LocalDateTime.
     * This method is used automatically by MapStruct for ZonedDateTime to LocalDateTime mappings.
     */
    default LocalDateTime map(ZonedDateTime zonedDateTime) {
        if (zonedDateTime == null) {
            return null;
        }
        return zonedDateTime.toLocalDateTime();
    }
}
