package com.talentica.paymentgateway.mapper;

import com.talentica.paymentgateway.dto.payment.*;
import com.talentica.paymentgateway.entity.PaymentMethod;
import net.authorize.api.contract.v1.*;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * MapStruct mapper for Authorize.Net API conversions.
 * Handles mapping between payment DTOs and Authorize.Net SDK objects.
 * 
 * Note: This interface uses Spring component model for dependency injection.
 * Complex mappings for Authorize.Net SDK objects use custom mapping methods.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Mapper(componentModel = "spring")
public interface AuthorizeNetApiMapper {

    /**
     * Map PaymentMethodRequest to Authorize.Net PaymentType.
     * This method handles credit card, bank account, and token payment types.
     */
    @Named("toPaymentType")
    default PaymentType toPaymentType(PaymentMethodRequest paymentMethod) {
        if (paymentMethod == null) {
            return null;
        }

        PaymentType payment = new PaymentType();

        switch (paymentMethod.getType()) {
            case "CREDIT_CARD":
                CreditCardType creditCard = new CreditCardType();
                creditCard.setCardNumber(paymentMethod.getCardNumber());
                creditCard.setExpirationDate(formatExpirationDate(paymentMethod.getExpiryMonth(), 
                                                                 paymentMethod.getExpiryYear()));
                creditCard.setCardCode(paymentMethod.getCvv());
                payment.setCreditCard(creditCard);
                break;

            case "TOKEN":
                throw new UnsupportedOperationException("Token payments require customer profiles implementation");

            case "BANK_ACCOUNT":
                BankAccountType bankAccount = new BankAccountType();
                bankAccount.setAccountNumber(paymentMethod.getAccountNumber());
                bankAccount.setRoutingNumber(paymentMethod.getRoutingNumber());
                bankAccount.setAccountType(BankAccountTypeEnum.fromValue(paymentMethod.getAccountType().toLowerCase()));
                bankAccount.setNameOnAccount(paymentMethod.getAccountHolderName());
                bankAccount.setBankName(paymentMethod.getBankName());
                payment.setBankAccount(bankAccount);
                break;

            default:
                throw new IllegalArgumentException("Unsupported payment method type: " + paymentMethod.getType());
        }

        return payment;
    }

    /**
     * Map AddressRequest to Authorize.Net CustomerAddressType.
     */
    @Named("toCustomerAddress")
    @Mapping(source = "firstName", target = "firstName")
    @Mapping(source = "lastName", target = "lastName")
    @Mapping(source = "company", target = "company")
    @Mapping(source = "address1", target = "address")
    @Mapping(source = "city", target = "city")
    @Mapping(source = "state", target = "state")
    @Mapping(source = "zipCode", target = "zip")
    @Mapping(source = "country", target = "country")
    @Mapping(source = "phoneNumber", target = "phoneNumber")
    CustomerAddressType toCustomerAddress(AddressRequest address);

    /**
     * Map PaymentMethodRequest to PaymentMethodResponse.
     */
    @Named("toPaymentMethodResponse")
    default PaymentMethodResponse toPaymentMethodResponse(PaymentMethodRequest request, 
                                                         TransactionResponse transactionResponse) {
        if (request == null) {
            return new PaymentMethodResponse();
        }
        
        switch (request.getType()) {
            case "CREDIT_CARD":
                return PaymentMethodResponse.fromCreditCard(
                    request.getCardNumber(),
                    detectCardBrand(request.getCardNumber()),
                    request.getExpiryMonth(),
                    request.getExpiryYear(),
                    request.getCardholderName()
                );
            case "TOKEN":
                return PaymentMethodResponse.fromToken(request.getToken());
            case "BANK_ACCOUNT":
                return PaymentMethodResponse.fromBankAccount(
                    request.getAccountNumber(),
                    request.getRoutingNumber(),
                    request.getAccountType(),
                    request.getBankName(),
                    request.getAccountHolderName()
                );
            default:
                return new PaymentMethodResponse();
        }
    }

    /**
     * Map Authorize.Net CreateTransactionResponse to PaymentResponse.
     */
    @Named("toPaymentResponse")
    default PaymentResponse toPaymentResponse(CreateTransactionResponse authNetResponse, 
                                            String internalTransactionId,
                                            String transactionType,
                                            PaymentMethodRequest paymentMethod,
                                            String correlationId) {
        PaymentResponse response = new PaymentResponse();
        response.setTransactionId(internalTransactionId);
        response.setTransactionType(transactionType);
        response.setCorrelationId(correlationId);
        response.setCreatedAt(ZonedDateTime.now());

        if (authNetResponse.getMessages().getResultCode() == MessageTypeEnum.OK) {
            TransactionResponse transactionResponse = authNetResponse.getTransactionResponse();
            
            response.setSuccess(true);
            response.setAuthnetTransactionId(transactionResponse.getTransId());
            response.setAuthorizationCode(transactionResponse.getAuthCode());
            response.setResponseCode(transactionResponse.getResponseCode());
            response.setResponseReasonCode(transactionResponse.getMessages().getMessage().get(0).getCode());
            response.setResponseReasonText(transactionResponse.getMessages().getMessage().get(0).getDescription());
            response.setStatus(mapTransactionStatus(transactionType, transactionResponse));
            response.setAmount(null); // Will be set by calling service
            response.setCurrency("USD");
            response.setAvsResult(transactionResponse.getAvsResultCode());
            response.setCvvResult(transactionResponse.getCvvResultCode());
            response.setTestMode(transactionResponse.getTestRequest() != null && 
                               "1".equals(transactionResponse.getTestRequest()));
            response.setPaymentMethod(toPaymentMethodResponse(paymentMethod, transactionResponse));
        } else {
            response.setSuccess(false);
            response.setStatus("FAILED");
            response.setError(toPaymentError(authNetResponse, correlationId));
        }

        return response;
    }

    /**
     * Map Authorize.Net error response to PaymentErrorResponse.
     */
    @Named("toPaymentError")
    default PaymentErrorResponse toPaymentError(CreateTransactionResponse authNetResponse, 
                                              String correlationId) {
        PaymentErrorResponse error;
        
        if (authNetResponse.getTransactionResponse() != null && 
            authNetResponse.getTransactionResponse().getErrors() != null &&
            !authNetResponse.getTransactionResponse().getErrors().getError().isEmpty()) {
            
            TransactionResponse.Errors.Error transactionError = 
                authNetResponse.getTransactionResponse().getErrors().getError().get(0);
            
            error = new PaymentErrorResponse(
                mapErrorCode(transactionError.getErrorCode()),
                transactionError.getErrorText(),
                transactionError.getErrorText(),
                "TRANSACTION_ERROR",
                correlationId
            );
            
            error.setGatewayCode(authNetResponse.getTransactionResponse().getResponseCode());
            error.setGatewayReasonCode(transactionError.getErrorCode());
            error.setGatewayReasonText(transactionError.getErrorText());
            
        } else if (authNetResponse.getMessages() != null && 
                   !authNetResponse.getMessages().getMessage().isEmpty()) {
            
            MessagesType.Message message = authNetResponse.getMessages().getMessage().get(0);
            
            error = new PaymentErrorResponse(
                mapErrorCode(message.getCode()),
                message.getText(),
                message.getText(),
                "API_ERROR",
                correlationId
            );
            
            error.setGatewayReasonCode(message.getCode());
            error.setGatewayReasonText(message.getText());
        } else {
            error = PaymentErrorResponse.processingError("Unknown payment processing error", correlationId);
        }
        
        return error;
    }

    /**
     * Map transaction type to status.
     */
    @Named("mapTransactionStatus")
    default String mapTransactionStatus(String transactionType, TransactionResponse transactionResponse) {
        switch (transactionType.toUpperCase()) {
            case "PURCHASE":
                return "CAPTURED";
            case "AUTHORIZE":
                return "AUTHORIZED";
            case "CAPTURE":
                return "CAPTURED";
            case "REFUND":
                return "REFUNDED";
            case "VOID":
                return "VOIDED";
            default:
                return "PENDING";
        }
    }

    /**
     * Map Authorize.Net error codes to standard error codes.
     */
    @Named("mapErrorCode")
    default String mapErrorCode(String authNetErrorCode) {
        switch (authNetErrorCode) {
            case "2":
            case "3":
                return "CARD_DECLINED";
            case "4":
                return "PROCESSING_ERROR";
            case "5":
                return "INVALID_AMOUNT";
            case "6":
                return "INVALID_CARD";
            case "7":
                return "INVALID_EXPIRY";
            case "8":
                return "INVALID_AUTH_CODE";
            case "27":
                return "AVS_MISMATCH";
            case "28":
                return "MERCHANT_NOT_CONFIGURED";
            case "44":
                return "CARD_TYPE_NOT_ACCEPTED";
            case "45":
                return "INSUFFICIENT_FUNDS";
            default:
                return "PROCESSING_ERROR";
        }
    }

    /**
     * Detect card brand from card number.
     */
    @Named("detectCardBrand")
    default String detectCardBrand(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "UNKNOWN";
        }
        
        String firstFour = cardNumber.substring(0, Math.min(4, cardNumber.length()));
        
        if (firstFour.startsWith("4")) {
            return "VISA";
        } else if (firstFour.startsWith("5") || firstFour.startsWith("2")) {
            return "MASTERCARD";
        } else if (firstFour.startsWith("3")) {
            return "AMEX";
        } else if (firstFour.startsWith("6")) {
            return "DISCOVER";
        } else {
            return "UNKNOWN";
        }
    }

    /**
     * Format expiration date for Authorize.Net (MMYY format).
     */
    @Named("formatExpirationDate")
    default String formatExpirationDate(String month, String year) {
        if (month == null || year == null) {
            return null;
        }
        
        String formattedMonth = month.length() == 1 ? "0" + month : month;
        String formattedYear = year.length() == 4 ? year.substring(2) : year;
        
        return formattedMonth + formattedYear;
    }

    /**
     * Generate unique transaction ID.
     */
    @Named("generateTransactionId")
    default String generateTransactionId() {
        return "txn_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
