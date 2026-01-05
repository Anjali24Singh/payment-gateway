package com.talentica.paymentgateway.dto.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Purchase request DTO for direct purchase transactions (auth + capture in one step).
 * Extends the base PaymentRequest with purchase-specific functionality.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Schema(description = "Purchase request for direct payment transactions")
public class PurchaseRequest extends PaymentRequest {

    // Purchase requests use all fields from PaymentRequest
    // This class exists for type safety and potential future purchase-specific fields
}
