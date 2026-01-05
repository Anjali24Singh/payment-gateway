package com.talentica.paymentgateway.dto.customer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for customer address information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAddressResponse {

    private String firstName;
    private String lastName;
    private String company;
    private String address;
    private String city;
    private String state;
    private String zip;
    private String country;
    private String phoneNumber;
    private String faxNumber;
}
