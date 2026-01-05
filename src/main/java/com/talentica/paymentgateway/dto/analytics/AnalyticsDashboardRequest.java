package com.talentica.paymentgateway.dto.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.ZonedDateTime;

/**
 * DTO for analytics dashboard requests with time period configuration.
 * Supports real-time metrics and configurable time ranges.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Schema(description = "Analytics dashboard request for real-time metrics")
public class AnalyticsDashboardRequest {
    
    @Schema(description = "Start date for the analytics period")
    @NotNull(message = "Start date is required")
    private ZonedDateTime startDate;
    
    @Schema(description = "End date for the analytics period")
    @NotNull(message = "End date is required")
    private ZonedDateTime endDate;
    
    @Schema(description = "Time zone for date calculations")
    private String timeZone = "UTC";
    
    @Schema(description = "Include real-time metrics")
    private Boolean includeRealTime = true;
    
    @Schema(description = "Include historical comparisons")
    private Boolean includeComparisons = false;
    
    @Schema(description = "Include detailed breakdowns")
    private Boolean includeBreakdowns = true;
    
    @Schema(description = "Refresh interval in seconds for real-time data")
    private Integer refreshInterval = 30;
    
    @Schema(description = "Metrics resolution (HOUR, DAY, WEEK, MONTH)")
    private String resolution = "DAY";
    
    // Constructors
    public AnalyticsDashboardRequest() {}
    
    public AnalyticsDashboardRequest(ZonedDateTime startDate, ZonedDateTime endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }
    
    // Getters and Setters
    public ZonedDateTime getStartDate() {
        return startDate;
    }
    
    public void setStartDate(ZonedDateTime startDate) {
        this.startDate = startDate;
    }
    
    public ZonedDateTime getEndDate() {
        return endDate;
    }
    
    public void setEndDate(ZonedDateTime endDate) {
        this.endDate = endDate;
    }
    
    public String getTimeZone() {
        return timeZone;
    }
    
    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }
    
    public Boolean getIncludeRealTime() {
        return includeRealTime;
    }
    
    public void setIncludeRealTime(Boolean includeRealTime) {
        this.includeRealTime = includeRealTime;
    }
    
    public Boolean getIncludeComparisons() {
        return includeComparisons;
    }
    
    public void setIncludeComparisons(Boolean includeComparisons) {
        this.includeComparisons = includeComparisons;
    }
    
    public Boolean getIncludeBreakdowns() {
        return includeBreakdowns;
    }
    
    public void setIncludeBreakdowns(Boolean includeBreakdowns) {
        this.includeBreakdowns = includeBreakdowns;
    }
    
    public Integer getRefreshInterval() {
        return refreshInterval;
    }
    
    public void setRefreshInterval(Integer refreshInterval) {
        this.refreshInterval = refreshInterval;
    }
    
    public String getResolution() {
        return resolution;
    }
    
    public void setResolution(String resolution) {
        this.resolution = resolution;
    }
}
