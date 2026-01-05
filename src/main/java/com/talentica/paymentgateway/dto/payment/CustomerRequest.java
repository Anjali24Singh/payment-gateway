package com.talentica.paymentgateway.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Customer request DTO for payment processing.
 * Contains customer information required for transactions.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer information for payment processing")
public class CustomerRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    @Schema(description = "Customer email address", example = "john@example.com", required = true)
    @JsonProperty("email")
    private String email;

    @Size(max = 100, message = "First name must not exceed 100 characters")
    @Schema(description = "Customer first name", example = "John")
    @JsonProperty("firstName")
    private String firstName;

    @Size(max = 100, message = "Last name must not exceed 100 characters")
    @Schema(description = "Customer last name", example = "Doe")
    @JsonProperty("lastName")
    private String lastName;

    @Size(max = 20, message = "Phone must not exceed 20 characters")
    @Schema(description = "Customer phone number", example = "+1-555-123-4567")
    @JsonProperty("phone")
    private String phone;

    @Size(max = 50, message = "Customer reference must not exceed 50 characters")
    @Schema(description = "External customer reference", example = "CUST-001")
    @JsonProperty("customerReference")
    private String customerReference;

    @Schema(description = "Customer billing address")
    @JsonProperty("billingAddress")
    private AddressRequest billingAddress;
}

