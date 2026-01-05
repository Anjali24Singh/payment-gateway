package com.talentica.paymentgateway.dto.customer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for customer profile operations in Authorize.Net CIM
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerProfileResponse {

    private String customerProfileId;
    private String merchantCustomerId;
    private String email;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Payment profiles associated with this customer
    private List<CustomerPaymentProfileResponse> paymentProfiles;
    
    // Shipping addresses (if any)
    private List<CustomerAddressResponse> shippingAddresses;
}
