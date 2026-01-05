package com.talentica.paymentgateway.mapper;

import com.talentica.paymentgateway.dto.payment.PaymentMethodResponse;
import com.talentica.paymentgateway.dto.payment.PaymentResponse;
import com.talentica.paymentgateway.entity.PaymentMethod;
import com.talentica.paymentgateway.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * MapStruct mapper for Payment and Transaction entity to DTO conversions.
 * Handles mapping between domain entities and data transfer objects for payment operations.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Mapper(componentModel = "spring")
public interface PaymentMapper {

    PaymentMapper INSTANCE = Mappers.getMapper(PaymentMapper.class);

    /**
     * Map Transaction entity to PaymentResponse DTO.
     */
    @Mapping(source = "transactionId", target = "transactionId")
    @Mapping(source = "authnetTransactionId", target = "authnetTransactionId")
    @Mapping(source = "amount", target = "amount")
    @Mapping(source = "currency", target = "currency")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "transactionType", target = "transactionType")
    @Mapping(source = "authnetAuthCode", target = "authorizationCode")
    @Mapping(source = "authnetAvsResult", target = "avsResult")
    @Mapping(source = "authnetCvvResult", target = "cvvResult")
    @Mapping(source = "authnetResponseCode", target = "responseCode")
    @Mapping(source = "authnetResponseReason", target = "responseReasonText")
    @Mapping(source = "customer.customerReference", target = "customerId")
    @Mapping(source = "correlationId", target = "correlationId")
    @Mapping(source = "createdAt", target = "createdAt", qualifiedByName = "formatDateTime")
    @Mapping(target = "success", ignore = true)
    PaymentResponse toPaymentResponse(Transaction transaction);

    /**
     * Map PaymentMethod entity to PaymentMethodResponse DTO.
     */
    @Mapping(source = "paymentType", target = "type")
    @Mapping(source = "cardLastFour", target = "maskedCardNumber")
    @Mapping(source = "cardBrand", target = "cardBrand")
    @Mapping(source = "expiryMonth", target = "expiryMonth")
    @Mapping(source = "expiryYear", target = "expiryYear")
    @Mapping(source = "cardholderName", target = "cardholderName")
    @Mapping(target = "maskedAccountNumber", ignore = true)
    @Mapping(target = "routingNumber", ignore = true)
    @Mapping(source = "paymentToken", target = "token")
    PaymentMethodResponse toPaymentMethodResponse(PaymentMethod paymentMethod);

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
