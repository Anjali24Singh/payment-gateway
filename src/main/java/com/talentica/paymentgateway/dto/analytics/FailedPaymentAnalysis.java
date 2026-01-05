package com.talentica.paymentgateway.dto.analytics;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for failed payment analysis and fraud detection reporting.
 * Provides insights into payment failures, patterns, and risk indicators.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Schema(description = "Failed payment analysis with fraud detection insights")
public class FailedPaymentAnalysis {
    
    @Schema(description = "Total number of failed payments")
    private long totalFailedPayments;
    
    @Schema(description = "Failed payment rate as percentage")
    private double failureRate;
    
    @Schema(description = "Total amount of failed payments")
    private BigDecimal totalFailedAmount;
    
    @Schema(description = "Analysis period start")
    private ZonedDateTime periodStart;
    
    @Schema(description = "Analysis period end")
    private ZonedDateTime periodEnd;
    
    @Schema(description = "Breakdown of failures by error code")
    private Map<String, FailureCodeAnalysis> errorCodeBreakdown;
    
    @Schema(description = "Breakdown of failures by payment method")
    private Map<String, Long> paymentMethodBreakdown;
    
    @Schema(description = "Geographic failure patterns")
    private List<GeographicFailurePattern> geographicPatterns;
    
    @Schema(description = "Temporal failure patterns")
    private List<TemporalFailurePattern> temporalPatterns;
    
    @Schema(description = "Risk indicators and fraud signals")
    private FraudRiskIndicators riskIndicators;
    
    @Schema(description = "Recommended actions")
    private List<String> recommendations;
    
    // Constructors
    public FailedPaymentAnalysis() {}
    
    // Getters and Setters
    public long getTotalFailedPayments() {
        return totalFailedPayments;
    }
    
    public void setTotalFailedPayments(long totalFailedPayments) {
        this.totalFailedPayments = totalFailedPayments;
    }
    
    public double getFailureRate() {
        return failureRate;
    }
    
    public void setFailureRate(double failureRate) {
        this.failureRate = failureRate;
    }
    
    public BigDecimal getTotalFailedAmount() {
        return totalFailedAmount;
    }
    
    public void setTotalFailedAmount(BigDecimal totalFailedAmount) {
        this.totalFailedAmount = totalFailedAmount;
    }
    
    public ZonedDateTime getPeriodStart() {
        return periodStart;
    }
    
    public void setPeriodStart(ZonedDateTime periodStart) {
        this.periodStart = periodStart;
    }
    
    public ZonedDateTime getPeriodEnd() {
        return periodEnd;
    }
    
    public void setPeriodEnd(ZonedDateTime periodEnd) {
        this.periodEnd = periodEnd;
    }
    
    public Map<String, FailureCodeAnalysis> getErrorCodeBreakdown() {
        return errorCodeBreakdown;
    }
    
    public void setErrorCodeBreakdown(Map<String, FailureCodeAnalysis> errorCodeBreakdown) {
        this.errorCodeBreakdown = errorCodeBreakdown;
    }
    
    public Map<String, Long> getPaymentMethodBreakdown() {
        return paymentMethodBreakdown;
    }
    
    public void setPaymentMethodBreakdown(Map<String, Long> paymentMethodBreakdown) {
        this.paymentMethodBreakdown = paymentMethodBreakdown;
    }
    
    public List<GeographicFailurePattern> getGeographicPatterns() {
        return geographicPatterns;
    }
    
    public void setGeographicPatterns(List<GeographicFailurePattern> geographicPatterns) {
        this.geographicPatterns = geographicPatterns;
    }
    
    public List<TemporalFailurePattern> getTemporalPatterns() {
        return temporalPatterns;
    }
    
    public void setTemporalPatterns(List<TemporalFailurePattern> temporalPatterns) {
        this.temporalPatterns = temporalPatterns;
    }
    
    public FraudRiskIndicators getRiskIndicators() {
        return riskIndicators;
    }
    
    public void setRiskIndicators(FraudRiskIndicators riskIndicators) {
        this.riskIndicators = riskIndicators;
    }
    
    public List<String> getRecommendations() {
        return recommendations;
    }
    
    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }
    
    /**
     * Analysis of specific failure codes.
     */
    @Schema(description = "Analysis of specific failure error codes")
    public static class FailureCodeAnalysis {
        private String errorCode;
        private String description;
        private long count;
        private double percentage;
        private BigDecimal totalAmount;
        private boolean isRetryable;
        private String recommendedAction;
        
        // Constructors
        public FailureCodeAnalysis() {}
        
        public FailureCodeAnalysis(String errorCode, String description, long count, double percentage) {
            this.errorCode = errorCode;
            this.description = description;
            this.count = count;
            this.percentage = percentage;
        }
        
        // Getters and Setters
        public String getErrorCode() {
            return errorCode;
        }
        
        public void setErrorCode(String errorCode) {
            this.errorCode = errorCode;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public long getCount() {
            return count;
        }
        
        public void setCount(long count) {
            this.count = count;
        }
        
        public double getPercentage() {
            return percentage;
        }
        
        public void setPercentage(double percentage) {
            this.percentage = percentage;
        }
        
        public BigDecimal getTotalAmount() {
            return totalAmount;
        }
        
        public void setTotalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
        }
        
        public boolean isRetryable() {
            return isRetryable;
        }
        
        public void setRetryable(boolean retryable) {
            isRetryable = retryable;
        }
        
        public String getRecommendedAction() {
            return recommendedAction;
        }
        
        public void setRecommendedAction(String recommendedAction) {
            this.recommendedAction = recommendedAction;
        }
    }
    
    /**
     * Geographic failure patterns by region or country.
     */
    @Schema(description = "Geographic failure patterns and hotspots")
    public static class GeographicFailurePattern {
        private String region;
        private String countryCode;
        private long failureCount;
        private double failureRate;
        private String riskLevel;
        
        // Constructors
        public GeographicFailurePattern() {}
        
        // Getters and Setters
        public String getRegion() {
            return region;
        }
        
        public void setRegion(String region) {
            this.region = region;
        }
        
        public String getCountryCode() {
            return countryCode;
        }
        
        public void setCountryCode(String countryCode) {
            this.countryCode = countryCode;
        }
        
        public long getFailureCount() {
            return failureCount;
        }
        
        public void setFailureCount(long failureCount) {
            this.failureCount = failureCount;
        }
        
        public double getFailureRate() {
            return failureRate;
        }
        
        public void setFailureRate(double failureRate) {
            this.failureRate = failureRate;
        }
        
        public String getRiskLevel() {
            return riskLevel;
        }
        
        public void setRiskLevel(String riskLevel) {
            this.riskLevel = riskLevel;
        }
    }
    
    /**
     * Temporal failure patterns by time periods.
     */
    @Schema(description = "Temporal failure patterns and trends")
    public static class TemporalFailurePattern {
        private String timePeriod;
        private ZonedDateTime periodStart;
        private ZonedDateTime periodEnd;
        private long failureCount;
        private double failureRate;
        private String trend;
        
        // Constructors
        public TemporalFailurePattern() {}
        
        // Getters and Setters
        public String getTimePeriod() {
            return timePeriod;
        }
        
        public void setTimePeriod(String timePeriod) {
            this.timePeriod = timePeriod;
        }
        
        public ZonedDateTime getPeriodStart() {
            return periodStart;
        }
        
        public void setPeriodStart(ZonedDateTime periodStart) {
            this.periodStart = periodStart;
        }
        
        public ZonedDateTime getPeriodEnd() {
            return periodEnd;
        }
        
        public void setPeriodEnd(ZonedDateTime periodEnd) {
            this.periodEnd = periodEnd;
        }
        
        public long getFailureCount() {
            return failureCount;
        }
        
        public void setFailureCount(long failureCount) {
            this.failureCount = failureCount;
        }
        
        public double getFailureRate() {
            return failureRate;
        }
        
        public void setFailureRate(double failureRate) {
            this.failureRate = failureRate;
        }
        
        public String getTrend() {
            return trend;
        }
        
        public void setTrend(String trend) {
            this.trend = trend;
        }
    }
    
    /**
     * Fraud risk indicators and security metrics.
     */
    @Schema(description = "Fraud risk indicators and security analytics")
    public static class FraudRiskIndicators {
        private long suspiciousTransactions;
        private double fraudScore;
        private long velocityViolations;
        private long duplicateAttempts;
        private long unusualPatterns;
        private List<String> riskFactors;
        private String overallRiskLevel;
        
        // Constructors
        public FraudRiskIndicators() {}
        
        // Getters and Setters
        public long getSuspiciousTransactions() {
            return suspiciousTransactions;
        }
        
        public void setSuspiciousTransactions(long suspiciousTransactions) {
            this.suspiciousTransactions = suspiciousTransactions;
        }
        
        public double getFraudScore() {
            return fraudScore;
        }
        
        public void setFraudScore(double fraudScore) {
            this.fraudScore = fraudScore;
        }
        
        public long getVelocityViolations() {
            return velocityViolations;
        }
        
        public void setVelocityViolations(long velocityViolations) {
            this.velocityViolations = velocityViolations;
        }
        
        public long getDuplicateAttempts() {
            return duplicateAttempts;
        }
        
        public void setDuplicateAttempts(long duplicateAttempts) {
            this.duplicateAttempts = duplicateAttempts;
        }
        
        public long getUnusualPatterns() {
            return unusualPatterns;
        }
        
        public void setUnusualPatterns(long unusualPatterns) {
            this.unusualPatterns = unusualPatterns;
        }
        
        public List<String> getRiskFactors() {
            return riskFactors;
        }
        
        public void setRiskFactors(List<String> riskFactors) {
            this.riskFactors = riskFactors;
        }
        
        public String getOverallRiskLevel() {
            return overallRiskLevel;
        }
        
        public void setOverallRiskLevel(String overallRiskLevel) {
            this.overallRiskLevel = overallRiskLevel;
        }
    }
}
