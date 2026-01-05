package com.talentica.paymentgateway.util;

import com.talentica.paymentgateway.dto.payment.*;
import com.talentica.paymentgateway.entity.*;
import net.authorize.api.contract.v1.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Utility class for mapping between payment DTOs and Authorize.Net SDK objects.
 * Handles the conversion between our internal models and Authorize.Net API structures.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Component
public class AuthorizeNetMapper {

    /**
     * Maps a PaymentRequest to Authorize.Net CreateTransactionRequest for purchase.
     */
    public CreateTransactionRequest mapToPurchaseTransaction(PaymentRequest request, 
                                                           MerchantAuthenticationType merchant) {
        CreateTransactionRequest apiRequest = new CreateTransactionRequest();
        apiRequest.setMerchantAuthentication(merchant);

        TransactionRequestType transactionRequest = new TransactionRequestType();
        transactionRequest.setTransactionType(TransactionTypeEnum.AUTH_CAPTURE_TRANSACTION.value());
        transactionRequest.setAmount(request.getAmount());

        // Set payment method
        PaymentType payment = mapToPaymentType(request.getPaymentMethod());
        transactionRequest.setPayment(payment);

        // Set order information
        if (request.getOrderNumber() != null || request.getDescription() != null) {
            OrderType order = new OrderType();
            order.setInvoiceNumber(request.getInvoiceNumber());
            order.setDescription(request.getDescription());
            transactionRequest.setOrder(order);
        }

        // Set billing address
        if (request.getBillingAddress() != null) {
            CustomerAddressType billTo = mapToCustomerAddress(request.getBillingAddress());
            transactionRequest.setBillTo(billTo);
        }

        // Set shipping address
        if (request.getShippingAddress() != null) {
            CustomerAddressType shipTo = mapToCustomerAddress(request.getShippingAddress());
            transactionRequest.setShipTo(shipTo);
        }

        // Set customer information
        if (request.getCustomerId() != null) {
            CustomerDataType customer = new CustomerDataType();
            customer.setId(request.getCustomerId());
            transactionRequest.setCustomer(customer);
        }

        // Set solution ID for tracking
        SolutionType solution = new SolutionType();
        solution.setId("A1000010"); // Authorize.Net assigned solution ID
        transactionRequest.setSolution(solution);

        apiRequest.setTransactionRequest(transactionRequest);
        return apiRequest;
    }

    /**
     * Maps a PaymentRequest to Authorize.Net CreateTransactionRequest for authorization only.
     */
    public CreateTransactionRequest mapToAuthorizeTransaction(AuthorizeRequest request, 
                                                            MerchantAuthenticationType merchant) {
        CreateTransactionRequest apiRequest = new CreateTransactionRequest();
        apiRequest.setMerchantAuthentication(merchant);

        TransactionRequestType transactionRequest = new TransactionRequestType();
        transactionRequest.setTransactionType(TransactionTypeEnum.AUTH_ONLY_TRANSACTION.value());
        transactionRequest.setAmount(request.getAmount());

        // Set payment method
        PaymentType payment = mapToPaymentType(request.getPaymentMethod());
        transactionRequest.setPayment(payment);

        // Set order information
        if (request.getOrderNumber() != null || request.getDescription() != null) {
            OrderType order = new OrderType();
            order.setInvoiceNumber(request.getInvoiceNumber());
            order.setDescription(request.getDescription());
            transactionRequest.setOrder(order);
        }

        // Set billing address
        if (request.getBillingAddress() != null) {
            CustomerAddressType billTo = mapToCustomerAddress(request.getBillingAddress());
            transactionRequest.setBillTo(billTo);
        }

        // Set shipping address
        if (request.getShippingAddress() != null) {
            CustomerAddressType shipTo = mapToCustomerAddress(request.getShippingAddress());
            transactionRequest.setShipTo(shipTo);
        }

        // Set customer information
        if (request.getCustomerId() != null) {
            CustomerDataType customer = new CustomerDataType();
            customer.setId(request.getCustomerId());
            transactionRequest.setCustomer(customer);
        }

        apiRequest.setTransactionRequest(transactionRequest);
        return apiRequest;
    }

    /**
     * Maps a CaptureRequest to Authorize.Net CreateTransactionRequest for prior auth capture.
     */
    public CreateTransactionRequest mapToCaptureTransaction(CaptureRequest request,
                                                          String authnetTransactionId,
                                                          MerchantAuthenticationType merchant) {
        CreateTransactionRequest apiRequest = new CreateTransactionRequest();
        apiRequest.setMerchantAuthentication(merchant);

        TransactionRequestType transactionRequest = new TransactionRequestType();
        transactionRequest.setTransactionType(TransactionTypeEnum.PRIOR_AUTH_CAPTURE_TRANSACTION.value());
        
        if (request.getAmount() != null) {
            transactionRequest.setAmount(request.getAmount());
        }

        // Set reference to original authorization
        transactionRequest.setRefTransId(authnetTransactionId);

        // Set order information
        if (request.getInvoiceNumber() != null || request.getDescription() != null) {
            OrderType order = new OrderType();
            order.setInvoiceNumber(request.getInvoiceNumber());
            order.setDescription(request.getDescription());
            transactionRequest.setOrder(order);
        }

        apiRequest.setTransactionRequest(transactionRequest);
        return apiRequest;
    }

    /**
     * Maps a RefundRequest to Authorize.Net CreateTransactionRequest for refund.
     */
    public CreateTransactionRequest mapToRefundTransaction(RefundRequest request,
                                                         String authnetTransactionId,
                                                         PaymentMethod paymentMethod,
                                                         MerchantAuthenticationType merchant) {
        CreateTransactionRequest apiRequest = new CreateTransactionRequest();
        apiRequest.setMerchantAuthentication(merchant);

        TransactionRequestType transactionRequest = new TransactionRequestType();
        transactionRequest.setTransactionType(TransactionTypeEnum.REFUND_TRANSACTION.value());
        
        if (request.getAmount() != null) {
            transactionRequest.setAmount(request.getAmount());
        }

        // Set reference to original transaction
        transactionRequest.setRefTransId(authnetTransactionId);

        // Set payment method for refund (required for refunds)
        if (paymentMethod != null) {
            PaymentType payment = new PaymentType();
            CreditCardType creditCard = new CreditCardType();
            // For sandbox testing, use the full card number if available, otherwise use last 4
            String cardNumber = paymentMethod.getCardNumber() != null ? 
                paymentMethod.getCardNumber() : 
                (paymentMethod.getCardLastFour() != null ? paymentMethod.getCardLastFour() : "XXXX");
            creditCard.setCardNumber(cardNumber);
            creditCard.setExpirationDate("XXXX"); // Masked expiration for refunds
            payment.setCreditCard(creditCard);
            transactionRequest.setPayment(payment);
        }

        // Set order information
        if (request.getReferenceNumber() != null || request.getDescription() != null) {
            OrderType order = new OrderType();
            order.setInvoiceNumber(request.getReferenceNumber());
            order.setDescription(request.getDescription());
            transactionRequest.setOrder(order);
        }

        apiRequest.setTransactionRequest(transactionRequest);
        return apiRequest;
    }

    /**
     * Maps a VoidRequest to Authorize.Net CreateTransactionRequest for void.
     */
    public CreateTransactionRequest mapToVoidTransaction(VoidRequest request,
                                                       String authnetTransactionId,
                                                       MerchantAuthenticationType merchant) {
        CreateTransactionRequest apiRequest = new CreateTransactionRequest();
        apiRequest.setMerchantAuthentication(merchant);

        TransactionRequestType transactionRequest = new TransactionRequestType();
        transactionRequest.setTransactionType(TransactionTypeEnum.VOID_TRANSACTION.value());

        // Set reference to original transaction
        transactionRequest.setRefTransId(authnetTransactionId);

        // Set order information
        if (request.getReferenceNumber() != null || request.getDescription() != null) {
            OrderType order = new OrderType();
            order.setInvoiceNumber(request.getReferenceNumber());
            order.setDescription(request.getDescription());
            transactionRequest.setOrder(order);
        }

        apiRequest.setTransactionRequest(transactionRequest);
        return apiRequest;
    }

    /**
     * Maps PaymentMethodRequest to Authorize.Net PaymentType.
     */
    private PaymentType mapToPaymentType(PaymentMethodRequest paymentMethod) {
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
                // For tokenized payments, we would typically use customer profile references
                // This is a simplified implementation
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
     * Maps AddressRequest to Authorize.Net CustomerAddressType.
     */
    private CustomerAddressType mapToCustomerAddress(AddressRequest address) {
        CustomerAddressType customerAddress = new CustomerAddressType();
        customerAddress.setFirstName(address.getFirstName());
        customerAddress.setLastName(address.getLastName());
        customerAddress.setCompany(address.getCompany());
        customerAddress.setAddress(address.getAddress1());
        customerAddress.setCity(address.getCity());
        customerAddress.setState(address.getState());
        customerAddress.setZip(address.getZipCode());
        customerAddress.setCountry(address.getCountry());
        customerAddress.setPhoneNumber(address.getPhoneNumber());
        return customerAddress;
    }

    /**
     * Maps Authorize.Net CreateTransactionResponse to PaymentResponse.
     */
    public PaymentResponse mapToPaymentResponse(CreateTransactionResponse authNetResponse, 
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
            // Successful transaction
            TransactionResponse transactionResponse = authNetResponse.getTransactionResponse();
            
            response.setSuccess(true);
            response.setAuthnetTransactionId(transactionResponse.getTransId());
            response.setAuthorizationCode(transactionResponse.getAuthCode());
            response.setResponseCode(transactionResponse.getResponseCode());
            response.setResponseReasonCode(transactionResponse.getMessages().getMessage().get(0).getCode());
            response.setResponseReasonText(transactionResponse.getMessages().getMessage().get(0).getDescription());
            
            // Set status based on transaction type
            response.setStatus(mapTransactionStatus(transactionType, transactionResponse));
            
            // Set amounts - we'll use the amount from the original request as TransactionResponse doesn't expose amount directly
            // The amount can be retrieved from the original request context
            response.setAmount(null); // Will be set by the calling service with the original amount
            response.setCurrency("USD"); // Authorize.Net primarily handles USD
            
            // Set AVS and CVV results
            response.setAvsResult(transactionResponse.getAvsResultCode());
            response.setCvvResult(transactionResponse.getCvvResultCode());
            
            // Set test mode
            response.setTestMode(transactionResponse.getTestRequest() != null && 
                               "1".equals(transactionResponse.getTestRequest()));
            
            // Map payment method for response
            response.setPaymentMethod(mapToPaymentMethodResponse(paymentMethod, transactionResponse));
            
        } else {
            // Failed transaction
            response.setSuccess(false);
            response.setStatus("FAILED");
            
            PaymentErrorResponse error = mapToPaymentError(authNetResponse, correlationId);
            response.setError(error);
        }

        return response;
    }

    /**
     * Maps transaction type and response to appropriate status.
     */
    private String mapTransactionStatus(String transactionType, TransactionResponse transactionResponse) {
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
     * Maps PaymentMethodRequest to PaymentMethodResponse for response.
     */
    private PaymentMethodResponse mapToPaymentMethodResponse(PaymentMethodRequest request, 
                                                           TransactionResponse transactionResponse) {
        if (request == null) {
            return new PaymentMethodResponse(); // Return empty response for null requests (e.g., void transactions)
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
     * Maps Authorize.Net error response to PaymentErrorResponse.
     */
    private PaymentErrorResponse mapToPaymentError(CreateTransactionResponse authNetResponse, 
                                                 String correlationId) {
        PaymentErrorResponse error;
        
        if (authNetResponse.getTransactionResponse() != null && 
            authNetResponse.getTransactionResponse().getErrors() != null &&
            !authNetResponse.getTransactionResponse().getErrors().getError().isEmpty()) {
            
            // Transaction-level error
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
            
            // API-level error
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
            // Unknown error
            error = PaymentErrorResponse.processingError("Unknown payment processing error", correlationId);
        }
        
        return error;
    }

    /**
     * Maps Authorize.Net error codes to our standard error codes.
     */
    private String mapErrorCode(String authNetErrorCode) {
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
     * Detects card brand from card number.
     */
    private String detectCardBrand(String cardNumber) {
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
     * Formats expiration date for Authorize.Net (MMYY format).
     */
    private String formatExpirationDate(String month, String year) {
        if (month == null || year == null) {
            return null;
        }
        
        String formattedMonth = month.length() == 1 ? "0" + month : month;
        String formattedYear = year.length() == 4 ? year.substring(2) : year;
        
        return formattedMonth + formattedYear;
    }

    /**
     * Generates a unique transaction ID.
     */
    public String generateTransactionId() {
        return "txn_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
