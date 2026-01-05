package com.talentica.paymentgateway.dto.subscription;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

/**
 * DTO for proration calculation results.
 * Contains detailed breakdown of proration amounts and reasoning.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Schema(description = "Proration calculation details for subscription changes")
public class ProrationCalculation {

    @Schema(description = "Original plan amount", example = "29.99")
    @JsonProperty("originalAmount")
    private BigDecimal originalAmount;

    @Schema(description = "New plan amount", example = "49.99")
    @JsonProperty("newAmount")
    private BigDecimal newAmount;

    @Schema(description = "Current period start date", example = "2024-01-01T00:00:00Z")
    @JsonProperty("periodStart")
    private ZonedDateTime periodStart;

    @Schema(description = "Current period end date", example = "2024-02-01T00:00:00Z")
    @JsonProperty("periodEnd")
    private ZonedDateTime periodEnd;

    @Schema(description = "Change effective date", example = "2024-01-15T00:00:00Z")
    @JsonProperty("changeDate")
    private ZonedDateTime changeDate;

    @Schema(description = "Total days in current period", example = "31")
    @JsonProperty("totalDaysInPeriod")
    private Integer totalDaysInPeriod;

    @Schema(description = "Days remaining in period", example = "17")
    @JsonProperty("daysRemaining")
    private Integer daysRemaining;

    @Schema(description = "Days used in period", example = "14")
    @JsonProperty("daysUsed")
    private Integer daysUsed;

    @Schema(description = "Unused amount from original plan", example = "13.54")
    @JsonProperty("unusedAmount")
    private BigDecimal unusedAmount;

    @Schema(description = "Prorated amount for new plan", example = "27.41")
    @JsonProperty("proratedAmount")
    private BigDecimal proratedAmount;

    @Schema(description = "Net proration amount (credit if negative, charge if positive)", example = "13.87")
    @JsonProperty("netAmount")
    private BigDecimal netAmount;

    @Schema(description = "Whether this is a credit (negative) or charge (positive)", example = "CHARGE")
    @JsonProperty("type")
    private String type; // CREDIT or CHARGE

    @Schema(description = "Detailed calculation explanation")
    @JsonProperty("explanation")
    private String explanation;

    @Schema(description = "Currency code", example = "USD")
    @JsonProperty("currency")
    private String currency;

    @Schema(description = "Whether proration applies", example = "true")
    @JsonProperty("prorationApplies")
    private Boolean prorationApplies;

    @Schema(description = "Reason why proration does/doesn't apply")
    @JsonProperty("prorationReason")
    private String prorationReason;

    // Default constructor
    public ProrationCalculation() {
    }

    // Constructor for no proration case
    public ProrationCalculation(String reason) {
        this.prorationApplies = false;
        this.prorationReason = reason;
        this.netAmount = BigDecimal.ZERO;
        this.type = "NONE";
    }

    // Getters and Setters
    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(BigDecimal originalAmount) {
        this.originalAmount = originalAmount;
    }

    public BigDecimal getNewAmount() {
        return newAmount;
    }

    public void setNewAmount(BigDecimal newAmount) {
        this.newAmount = newAmount;
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

    public ZonedDateTime getChangeDate() {
        return changeDate;
    }

    public void setChangeDate(ZonedDateTime changeDate) {
        this.changeDate = changeDate;
    }

    public Integer getTotalDaysInPeriod() {
        return totalDaysInPeriod;
    }

    public void setTotalDaysInPeriod(Integer totalDaysInPeriod) {
        this.totalDaysInPeriod = totalDaysInPeriod;
    }

    public Integer getDaysRemaining() {
        return daysRemaining;
    }

    public void setDaysRemaining(Integer daysRemaining) {
        this.daysRemaining = daysRemaining;
    }

    public Integer getDaysUsed() {
        return daysUsed;
    }

    public void setDaysUsed(Integer daysUsed) {
        this.daysUsed = daysUsed;
    }

    public BigDecimal getUnusedAmount() {
        return unusedAmount;
    }

    public void setUnusedAmount(BigDecimal unusedAmount) {
        this.unusedAmount = unusedAmount;
    }

    public BigDecimal getProratedAmount() {
        return proratedAmount;
    }

    public void setProratedAmount(BigDecimal proratedAmount) {
        this.proratedAmount = proratedAmount;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(BigDecimal netAmount) {
        this.netAmount = netAmount;
        this.type = netAmount.compareTo(BigDecimal.ZERO) >= 0 ? "CHARGE" : "CREDIT";
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Boolean getProrationApplies() {
        return prorationApplies;
    }

    public void setProrationApplies(Boolean prorationApplies) {
        this.prorationApplies = prorationApplies;
    }

    public String getProrationReason() {
        return prorationReason;
    }

    public void setProrationReason(String prorationReason) {
        this.prorationReason = prorationReason;
    }

    public boolean isCredit() {
        return "CREDIT".equals(type);
    }

    public boolean isCharge() {
        return "CHARGE".equals(type);
    }

    public boolean hasAmount() {
        return netAmount != null && netAmount.compareTo(BigDecimal.ZERO) != 0;
    }

    public String getFormattedNetAmount() {
        if (netAmount == null) return "0.00";
        return String.format("$%.2f", netAmount.abs());
    }

    @Override
    public String toString() {
        return "ProrationCalculation{" +
                "originalAmount=" + originalAmount +
                ", newAmount=" + newAmount +
                ", netAmount=" + netAmount +
                ", type='" + type + '\'' +
                ", daysRemaining=" + daysRemaining +
                ", totalDaysInPeriod=" + totalDaysInPeriod +
                ", prorationApplies=" + prorationApplies +
                '}';
    }
}
