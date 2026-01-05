package com.talentica.paymentgateway.service;

import com.talentica.paymentgateway.dto.subscription.ProrationCalculation;
import com.talentica.paymentgateway.entity.Subscription;
import com.talentica.paymentgateway.entity.SubscriptionPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Service for calculating proration amounts for subscription changes.
 * Handles plan upgrades, downgrades, and cancellation refunds with precise calculations.
 * 
 * Features:
 * - Accurate daily proration calculations
 * - Support for different billing intervals
 * - Upgrade and downgrade scenarios
 * - Cancellation refund calculations
 * - Detailed calculation explanations
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Slf4j
@Service
public class ProrationService {

    /**
     * Calculates proration for a plan change.
     * 
     * @param subscription Current subscription
     * @param newPlan Target plan
     * @param changeDate When the change takes effect
     * @return Proration calculation details
     */
    public ProrationCalculation calculateProration(Subscription subscription, 
                                                 SubscriptionPlan newPlan, 
                                                 ZonedDateTime changeDate) {
        
        log.debug("Calculating proration - Subscription: {}, New Plan: {}, Change Date: {}", 
                    subscription.getSubscriptionId(), newPlan.getPlanCode(), changeDate);

        SubscriptionPlan currentPlan = subscription.getPlan();
        
        // Check if proration applies
        if (!shouldApplyProration(subscription, changeDate)) {
            return new ProrationCalculation("Change occurs at period boundary, no proration needed");
        }

        if (currentPlan.getAmount().compareTo(newPlan.getAmount()) == 0) {
            return new ProrationCalculation("Plans have same amount, no proration needed");
        }

        ProrationCalculation calculation = new ProrationCalculation();
        calculation.setOriginalAmount(currentPlan.getAmount());
        calculation.setNewAmount(newPlan.getAmount());
        calculation.setCurrency(currentPlan.getCurrency());
        calculation.setPeriodStart(subscription.getCurrentPeriodStart());
        calculation.setPeriodEnd(subscription.getCurrentPeriodEnd());
        calculation.setChangeDate(changeDate);
        calculation.setProrationApplies(true);

        // Calculate days
        calculateDays(calculation);

        // Calculate unused amount from current plan
        BigDecimal dailyRate = currentPlan.getAmount().divide(
            BigDecimal.valueOf(calculation.getTotalDaysInPeriod()), 4, RoundingMode.HALF_UP);
        calculation.setUnusedAmount(dailyRate.multiply(BigDecimal.valueOf(calculation.getDaysRemaining())));

        // Calculate prorated amount for new plan
        BigDecimal newDailyRate = newPlan.getAmount().divide(
            BigDecimal.valueOf(calculation.getTotalDaysInPeriod()), 4, RoundingMode.HALF_UP);
        calculation.setProratedAmount(newDailyRate.multiply(BigDecimal.valueOf(calculation.getDaysRemaining())));

        // Calculate net amount (positive = charge, negative = credit)
        BigDecimal netAmount = calculation.getProratedAmount().subtract(calculation.getUnusedAmount());
        calculation.setNetAmount(netAmount.setScale(2, RoundingMode.HALF_UP));

        // Generate explanation
        generateExplanation(calculation, currentPlan, newPlan);

        calculation.setProrationReason("Plan amount difference requires proration");

        log.debug("Proration calculated - Net Amount: {}, Type: {}", 
                    calculation.getNetAmount(), calculation.getType());

        return calculation;
    }

    /**
     * Calculates proration for subscription cancellation refund.
     * 
     * @param subscription Subscription being cancelled
     * @param cancellationDate When the cancellation takes effect
     * @return Proration calculation for refund
     */
    public ProrationCalculation calculateRefundProration(Subscription subscription, 
                                                       ZonedDateTime cancellationDate) {
        
        log.debug("Calculating refund proration - Subscription: {}, Cancellation Date: {}", 
                    subscription.getSubscriptionId(), cancellationDate);

        if (!shouldApplyRefundProration(subscription, cancellationDate)) {
            return new ProrationCalculation("Cancellation at period boundary, no refund proration");
        }

        SubscriptionPlan plan = subscription.getPlan();
        
        ProrationCalculation calculation = new ProrationCalculation();
        calculation.setOriginalAmount(plan.getAmount());
        calculation.setNewAmount(BigDecimal.ZERO);
        calculation.setCurrency(plan.getCurrency());
        calculation.setPeriodStart(subscription.getCurrentPeriodStart());
        calculation.setPeriodEnd(subscription.getCurrentPeriodEnd());
        calculation.setChangeDate(cancellationDate);
        calculation.setProrationApplies(true);

        // Calculate days
        calculateDays(calculation);

        // Calculate refund amount for unused days
        BigDecimal dailyRate = plan.getAmount().divide(
            BigDecimal.valueOf(calculation.getTotalDaysInPeriod()), 4, RoundingMode.HALF_UP);
        BigDecimal refundAmount = dailyRate.multiply(BigDecimal.valueOf(calculation.getDaysRemaining()));
        
        calculation.setUnusedAmount(refundAmount);
        calculation.setProratedAmount(BigDecimal.ZERO);
        calculation.setNetAmount(refundAmount.negate().setScale(2, RoundingMode.HALF_UP)); // Negative for credit

        // Generate explanation
        calculation.setExplanation(String.format(
            "Refund for %d unused days out of %d total days in period. " +
            "Daily rate: $%.4f × %d days = $%.2f refund",
            calculation.getDaysRemaining(),
            calculation.getTotalDaysInPeriod(),
            dailyRate,
            calculation.getDaysRemaining(),
            refundAmount
        ));

        calculation.setProrationReason("Cancellation before period end, refund for unused time");

        log.debug("Refund proration calculated - Refund Amount: {}", refundAmount);

        return calculation;
    }

    /**
     * Calculates proration for mid-cycle billing period adjustments.
     * 
     * @param subscription Current subscription
     * @param adjustmentAmount Amount to prorate
     * @param startDate Start of proration period
     * @param endDate End of proration period
     * @return Proration calculation
     */
    public ProrationCalculation calculateAdjustmentProration(Subscription subscription,
                                                           BigDecimal adjustmentAmount,
                                                           ZonedDateTime startDate,
                                                           ZonedDateTime endDate) {
        
        log.debug("Calculating adjustment proration - Amount: {}, Period: {} to {}", 
                    adjustmentAmount, startDate, endDate);

        ProrationCalculation calculation = new ProrationCalculation();
        calculation.setOriginalAmount(BigDecimal.ZERO);
        calculation.setNewAmount(adjustmentAmount);
        calculation.setCurrency(subscription.getPlan().getCurrency());
        calculation.setPeriodStart(startDate);
        calculation.setPeriodEnd(endDate);
        calculation.setChangeDate(startDate);
        calculation.setProrationApplies(true);

        // Calculate days in adjustment period
        long adjustmentDays = ChronoUnit.DAYS.between(startDate, endDate);
        if (adjustmentDays <= 0) {
            return new ProrationCalculation("Invalid adjustment period");
        }

        calculation.setTotalDaysInPeriod((int) adjustmentDays);
        calculation.setDaysUsed(0);
        calculation.setDaysRemaining((int) adjustmentDays);

        // Full adjustment amount applies
        calculation.setProratedAmount(adjustmentAmount);
        calculation.setUnusedAmount(BigDecimal.ZERO);
        calculation.setNetAmount(adjustmentAmount.setScale(2, RoundingMode.HALF_UP));

        calculation.setExplanation(String.format(
            "Adjustment amount $%.2f for %d days period",
            adjustmentAmount, adjustmentDays
        ));

        calculation.setProrationReason("Mid-cycle adjustment");

        return calculation;
    }

    // Private helper methods

    private boolean shouldApplyProration(Subscription subscription, ZonedDateTime changeDate) {
        if (subscription.getCurrentPeriodStart() == null || subscription.getCurrentPeriodEnd() == null) {
            return false;
        }

        // Don't prorate if change is at period boundaries
        return !changeDate.isEqual(subscription.getCurrentPeriodStart()) && 
               !changeDate.isEqual(subscription.getCurrentPeriodEnd());
    }

    private boolean shouldApplyRefundProration(Subscription subscription, ZonedDateTime cancellationDate) {
        if (subscription.getCurrentPeriodStart() == null || subscription.getCurrentPeriodEnd() == null) {
            return false;
        }

        // Only refund if cancelling before period end with significant time remaining
        if (cancellationDate.isAfter(subscription.getCurrentPeriodEnd())) {
            return false;
        }

        long daysRemaining = ChronoUnit.DAYS.between(cancellationDate, subscription.getCurrentPeriodEnd());
        return daysRemaining > 0; // At least 1 day remaining
    }

    private void calculateDays(ProrationCalculation calculation) {
        long totalDays = ChronoUnit.DAYS.between(calculation.getPeriodStart(), calculation.getPeriodEnd());
        long usedDays = ChronoUnit.DAYS.between(calculation.getPeriodStart(), calculation.getChangeDate());
        long remainingDays = totalDays - usedDays;

        calculation.setTotalDaysInPeriod((int) totalDays);
        calculation.setDaysUsed((int) Math.max(0, usedDays));
        calculation.setDaysRemaining((int) Math.max(0, remainingDays));
    }

    private void generateExplanation(ProrationCalculation calculation, 
                                   SubscriptionPlan currentPlan, 
                                   SubscriptionPlan newPlan) {
        
        BigDecimal currentDaily = currentPlan.getAmount().divide(
            BigDecimal.valueOf(calculation.getTotalDaysInPeriod()), 4, RoundingMode.HALF_UP);
        BigDecimal newDaily = newPlan.getAmount().divide(
            BigDecimal.valueOf(calculation.getTotalDaysInPeriod()), 4, RoundingMode.HALF_UP);

        String explanation = String.format(
            "Plan change from %s ($%.2f) to %s ($%.2f) with %d days remaining in period. " +
            "Unused amount: $%.4f/day × %d days = $%.2f. " +
            "New plan amount: $%.4f/day × %d days = $%.2f. " +
            "Net %s: $%.2f",
            currentPlan.getName(), currentPlan.getAmount(),
            newPlan.getName(), newPlan.getAmount(),
            calculation.getDaysRemaining(),
            currentDaily, calculation.getDaysRemaining(), calculation.getUnusedAmount(),
            newDaily, calculation.getDaysRemaining(), calculation.getProratedAmount(),
            calculation.isCharge() ? "charge" : "credit",
            calculation.getNetAmount().abs()
        );

        calculation.setExplanation(explanation);
    }

    /**
     * Validates if a proration calculation is reasonable.
     * 
     * @param calculation Proration calculation to validate
     * @return true if calculation is valid
     */
    public boolean validateProration(ProrationCalculation calculation) {
        if (!calculation.getProrationApplies()) {
            return true;
        }

        // Check for reasonable amounts
        if (calculation.getNetAmount() != null && 
            calculation.getNetAmount().abs().compareTo(new BigDecimal("10000")) > 0) {
            log.warn("Proration amount seems unreasonably high: {}", calculation.getNetAmount());
            return false;
        }

        // Check for reasonable time periods
        if (calculation.getTotalDaysInPeriod() != null && 
            (calculation.getTotalDaysInPeriod() < 1 || calculation.getTotalDaysInPeriod() > 400)) {
            log.warn("Proration period seems unreasonable: {} days", calculation.getTotalDaysInPeriod());
            return false;
        }

        // Check for negative days
        if (calculation.getDaysRemaining() != null && calculation.getDaysRemaining() < 0) {
            log.warn("Negative days remaining in proration: {}", calculation.getDaysRemaining());
            return false;
        }

        return true;
    }

    /**
     * Formats a proration calculation for display.
     * 
     * @param calculation Proration calculation
     * @return Formatted description
     */
    public String formatProrationSummary(ProrationCalculation calculation) {
        if (!calculation.getProrationApplies()) {
            return "No proration required: " + calculation.getProrationReason();
        }

        if (!calculation.hasAmount()) {
            return "Proration calculated with no net amount due";
        }

        return String.format("Proration %s of %s for %d remaining days in billing period",
            calculation.getType().toLowerCase(),
            calculation.getFormattedNetAmount(),
            calculation.getDaysRemaining());
    }
}
