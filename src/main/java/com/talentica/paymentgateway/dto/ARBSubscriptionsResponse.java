package com.talentica.paymentgateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for ARB subscriptions list.
 * Provides a placeholder for future ARB subscription implementation.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "ARB subscriptions list response")
public class ARBSubscriptionsResponse {
    
    @Schema(description = "Response message", example = "ARB subscriptions list endpoint - implementation pending")
    private String message;
    
    @Schema(description = "List of ARB subscriptions")
    @Builder.Default
    private List<Object> subscriptions = new ArrayList<>();
}
