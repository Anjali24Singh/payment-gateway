package com.talentica.paymentgateway.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Authorization request DTO for authorize-only transactions.
 * Used when funds need to be held but not immediately captured.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Schema(description = "Authorization request for holding funds without immediate capture")
public class AuthorizeRequest extends PaymentRequest {

    @Min(value = 1, message = "Authorization hold period must be at least 1 day")
    @Max(value = 30, message = "Authorization hold period cannot exceed 30 days")
    @Schema(description = "Number of days to hold the authorization", example = "7", minimum = "1", maximum = "30")
    @JsonProperty("holdPeriodDays")
    private Integer holdPeriodDays;

    @Schema(description = "Auto-capture after hold period expires", example = "false")
    @JsonProperty("autoCapture")
    private Boolean autoCapture = false;
}
