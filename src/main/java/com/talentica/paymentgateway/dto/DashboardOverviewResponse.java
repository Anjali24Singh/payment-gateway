package com.talentica.paymentgateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Basic dashboard overview response DTO.
 * Provides quick overview metrics for the dashboard.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Basic dashboard overview with key metrics")
public class DashboardOverviewResponse {
    
    @Schema(description = "Dashboard message", example = "Dashboard overview")
    private String message;
    
    @Schema(description = "Total number of transactions", example = "0")
    private long totalTransactions;
    
    @Schema(description = "Total revenue amount", example = "0.00")
    private BigDecimal totalRevenue;
    
    @Schema(description = "Number of active subscriptions", example = "0")
    private long activeSubscriptions;
}
