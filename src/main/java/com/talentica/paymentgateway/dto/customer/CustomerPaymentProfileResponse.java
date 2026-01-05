package com.talentica.paymentgateway.dto.customer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for customer payment profile information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerPaymentProfileResponse {

    private String customerPaymentProfileId;
    private String customerProfileId;
    private String cardNumber; // Masked card number (e.g., XXXX1234)
    private String expirationDate; // YYYY-MM format
    private String cardType; // Visa, MasterCard, etc.
    private CustomerAddressResponse billingAddress;
    private String defaultPaymentProfile; // Y or N
}
