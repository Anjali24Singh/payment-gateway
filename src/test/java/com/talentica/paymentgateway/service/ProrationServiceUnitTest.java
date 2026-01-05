package com.talentica.paymentgateway.service;

import com.talentica.paymentgateway.dto.subscription.ProrationCalculation;
import com.talentica.paymentgateway.entity.Subscription;
import com.talentica.paymentgateway.entity.SubscriptionPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ProrationService.
 * Tests proration calculations for plan changes, cancellations, and adjustments.
 */
@ExtendWith(MockitoExtension.class)
class ProrationServiceUnitTest {

    @InjectMocks
    private ProrationService prorationService;

    private Subscription testSubscription;
    private SubscriptionPlan currentPlan;
    private SubscriptionPlan newPlan;
    private ZonedDateTime periodStart;
    private ZonedDateTime periodEnd;
    private ZonedDateTime changeDate;

    @BeforeEach
    void setUp() {
        // Set up test dates (30-day period)
        periodStart = ZonedDateTime.now().minusDays(10);
        periodEnd = ZonedDateTime.now().plusDays(20);
        changeDate = ZonedDateTime.now(); // 10 days into period, 20 days remaining

        // Create current plan ($100/month)
        currentPlan = new SubscriptionPlan();
        currentPlan.setPlanCode("BASIC");
        currentPlan.setName("Basic Plan");
        currentPlan.setAmount(new BigDecimal("100.00"));
        currentPlan.setCurrency("USD");

        // Create new plan ($200/month)
        newPlan = new SubscriptionPlan();
        newPlan.setPlanCode("PREMIUM");
        newPlan.setName("Premium Plan");
        newPlan.setAmount(new BigDecimal("200.00"));
        newPlan.setCurrency("USD");

        // Create subscription
        testSubscription = new Subscription();
        testSubscription.setSubscriptionId("SUB_001");
        testSubscription.setPlan(currentPlan);
        testSubscription.setCurrentPeriodStart(periodStart);
        testSubscription.setCurrentPeriodEnd(periodEnd);
    }

    @Test
    void calculateProration_WithPlanUpgrade_ShouldCalculateCorrectCharge() {
        // When
        ProrationCalculation result = prorationService.calculateProration(testSubscription, newPlan, changeDate);

        // Then
        assertThat(result.getProrationApplies()).isTrue();
        assertThat(result.getOriginalAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(result.getNewAmount()).isEqualTo(new BigDecimal("200.00"));
        assertThat(result.getTotalDaysInPeriod()).isEqualTo(30);
        assertThat(result.getDaysUsed()).isEqualTo(10);
        assertThat(result.getDaysRemaining()).isEqualTo(20);
        
        // Unused amount: $100/30 days * 20 days = $66.67
        assertThat(result.getUnusedAmount()).isEqualByComparingTo(new BigDecimal("66.6660"));
        
        // Prorated amount: $200/30 days * 20 days = $133.33
        assertThat(result.getProratedAmount()).isEqualByComparingTo(new BigDecimal("133.3340"));
        
        // Net charge: $133.33 - $66.67 = $66.67
        assertThat(result.getNetAmount()).isEqualByComparingTo(new BigDecimal("66.67"));
        assertThat(result.isCharge()).isTrue();
        assertThat(result.getExplanation()).contains("Plan change from Basic Plan");
        assertThat(result.getProrationReason()).isEqualTo("Plan amount difference requires proration");
    }

    @Test
    void calculateProration_WithPlanDowngrade_ShouldCalculateCorrectCredit() {
        // Given - downgrade from $200 to $100
        currentPlan.setAmount(new BigDecimal("200.00"));
        newPlan.setAmount(new BigDecimal("100.00"));

        // When
        ProrationCalculation result = prorationService.calculateProration(testSubscription, newPlan, changeDate);

        // Then
        assertThat(result.getProrationApplies()).isTrue();
        assertThat(result.getNetAmount()).isNegative(); // Credit
        assertThat(result.isCredit()).isTrue();
        
        // Net credit: $66.67 - $133.33 = -$66.67
        assertThat(result.getNetAmount()).isEqualByComparingTo(new BigDecimal("-66.67"));
    }

    @Test
    void calculateProration_WithSamePlanAmount_ShouldReturnNoProration() {
        // Given - same amount plans
        newPlan.setAmount(new BigDecimal("100.00"));

        // When
        ProrationCalculation result = prorationService.calculateProration(testSubscription, newPlan, changeDate);

        // Then
        assertThat(result.getProrationApplies()).isFalse();
        assertThat(result.getProrationReason()).isEqualTo("Plans have same amount, no proration needed");
    }

    @Test
    void calculateProration_AtPeriodStart_ShouldReturnNoProration() {
        // Given - change at period start
        changeDate = periodStart;

        // When
        ProrationCalculation result = prorationService.calculateProration(testSubscription, newPlan, changeDate);

        // Then
        assertThat(result.getProrationApplies()).isFalse();
        assertThat(result.getProrationReason()).isEqualTo("Change occurs at period boundary, no proration needed");
    }

    @Test
    void calculateProration_AtPeriodEnd_ShouldReturnNoProration() {
        // Given - change at period end
        changeDate = periodEnd;

        // When
        ProrationCalculation result = prorationService.calculateProration(testSubscription, newPlan, changeDate);

        // Then
        assertThat(result.getProrationApplies()).isFalse();
        assertThat(result.getProrationReason()).isEqualTo("Change occurs at period boundary, no proration needed");
    }

    @Test
    void calculateProration_WithNullPeriodDates_ShouldReturnNoProration() {
        // Given - null period dates
        testSubscription.setCurrentPeriodStart(null);
        testSubscription.setCurrentPeriodEnd(null);

        // When
        ProrationCalculation result = prorationService.calculateProration(testSubscription, newPlan, changeDate);

        // Then
        assertThat(result.getProrationApplies()).isFalse();
        assertThat(result.getProrationReason()).isEqualTo("Change occurs at period boundary, no proration needed");
    }

    @Test
    void calculateRefundProration_WithCancellationMidPeriod_ShouldCalculateRefund() {
        // Given - cancellation 20 days before period end
        ZonedDateTime cancellationDate = ZonedDateTime.now();

        // When
        ProrationCalculation result = prorationService.calculateRefundProration(testSubscription, cancellationDate);

        // Then
        assertThat(result.getProrationApplies()).isTrue();
        assertThat(result.getOriginalAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(result.getNewAmount()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.getDaysRemaining()).isEqualTo(20);
        
        // Refund amount: $100/30 days * 20 days = $66.67
        assertThat(result.getUnusedAmount()).isEqualByComparingTo(new BigDecimal("66.6660"));
        assertThat(result.getProratedAmount()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.getNetAmount()).isEqualByComparingTo(new BigDecimal("-66.67")); // Negative for credit
        assertThat(result.isCredit()).isTrue();
        assertThat(result.getExplanation()).contains("Refund for 20 unused days");
        assertThat(result.getProrationReason()).isEqualTo("Cancellation before period end, refund for unused time");
    }

    @Test
    void calculateRefundProration_AtPeriodEnd_ShouldReturnNoRefund() {
        // Given - cancellation at period end
        ZonedDateTime cancellationDate = periodEnd;

        // When
        ProrationCalculation result = prorationService.calculateRefundProration(testSubscription, cancellationDate);

        // Then
        assertThat(result.getProrationApplies()).isFalse();
        assertThat(result.getProrationReason()).isEqualTo("Cancellation at period boundary, no refund proration");
    }

    @Test
    void calculateRefundProration_AfterPeriodEnd_ShouldReturnNoRefund() {
        // Given - cancellation after period end
        ZonedDateTime cancellationDate = periodEnd.plusDays(1);

        // When
        ProrationCalculation result = prorationService.calculateRefundProration(testSubscription, cancellationDate);

        // Then
        assertThat(result.getProrationApplies()).isFalse();
        assertThat(result.getProrationReason()).isEqualTo("Cancellation at period boundary, no refund proration");
    }

    @Test
    void calculateRefundProration_WithNullPeriodDates_ShouldReturnNoRefund() {
        // Given - null period dates
        testSubscription.setCurrentPeriodStart(null);
        testSubscription.setCurrentPeriodEnd(null);
        ZonedDateTime cancellationDate = ZonedDateTime.now();

        // When
        ProrationCalculation result = prorationService.calculateRefundProration(testSubscription, cancellationDate);

        // Then
        assertThat(result.getProrationApplies()).isFalse();
        assertThat(result.getProrationReason()).isEqualTo("Cancellation at period boundary, no refund proration");
    }

    @Test
    void calculateAdjustmentProration_WithValidPeriod_ShouldCalculateCorrectly() {
        // Given
        BigDecimal adjustmentAmount = new BigDecimal("50.00");
        ZonedDateTime startDate = ZonedDateTime.now();
        ZonedDateTime endDate = startDate.plusDays(10);

        // When
        ProrationCalculation result = prorationService.calculateAdjustmentProration(
            testSubscription, adjustmentAmount, startDate, endDate);

        // Then
        assertThat(result.getProrationApplies()).isTrue();
        assertThat(result.getOriginalAmount()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.getNewAmount()).isEqualTo(new BigDecimal("50.00"));
        assertThat(result.getTotalDaysInPeriod()).isEqualTo(10);
        assertThat(result.getDaysUsed()).isEqualTo(0);
        assertThat(result.getDaysRemaining()).isEqualTo(10);
        assertThat(result.getProratedAmount()).isEqualTo(new BigDecimal("50.00"));
        assertThat(result.getUnusedAmount()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.getNetAmount()).isEqualTo(new BigDecimal("50.00"));
        assertThat(result.getExplanation()).contains("Adjustment amount $50.00 for 10 days period");
        assertThat(result.getProrationReason()).isEqualTo("Mid-cycle adjustment");
    }

    @Test
    void calculateAdjustmentProration_WithInvalidPeriod_ShouldReturnError() {
        // Given - end date before start date
        BigDecimal adjustmentAmount = new BigDecimal("50.00");
        ZonedDateTime startDate = ZonedDateTime.now();
        ZonedDateTime endDate = startDate.minusDays(1);

        // When
        ProrationCalculation result = prorationService.calculateAdjustmentProration(
            testSubscription, adjustmentAmount, startDate, endDate);

        // Then
        assertThat(result.getProrationApplies()).isFalse();
        assertThat(result.getProrationReason()).isEqualTo("Invalid adjustment period");
    }

    @Test
    void calculateAdjustmentProration_WithZeroDayPeriod_ShouldReturnError() {
        // Given - same start and end date
        BigDecimal adjustmentAmount = new BigDecimal("50.00");
        ZonedDateTime startDate = ZonedDateTime.now();
        ZonedDateTime endDate = startDate;

        // When
        ProrationCalculation result = prorationService.calculateAdjustmentProration(
            testSubscription, adjustmentAmount, startDate, endDate);

        // Then
        assertThat(result.getProrationApplies()).isFalse();
        assertThat(result.getProrationReason()).isEqualTo("Invalid adjustment period");
    }

    @Test
    void validateProration_WithValidCalculation_ShouldReturnTrue() {
        // Given
        ProrationCalculation calculation = prorationService.calculateProration(testSubscription, newPlan, changeDate);

        // When
        boolean isValid = prorationService.validateProration(calculation);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void validateProration_WithNoProrationApplied_ShouldReturnTrue() {
        // Given - no proration needed
        ProrationCalculation calculation = new ProrationCalculation("No proration needed");

        // When
        boolean isValid = prorationService.validateProration(calculation);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void validateProration_WithExcessiveAmount_ShouldReturnFalse() {
        // Given
        ProrationCalculation calculation = new ProrationCalculation();
        calculation.setProrationApplies(true);
        calculation.setNetAmount(new BigDecimal("15000.00")); // Excessive amount

        // When
        boolean isValid = prorationService.validateProration(calculation);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void validateProration_WithUnreasonablePeriod_ShouldReturnFalse() {
        // Given
        ProrationCalculation calculation = new ProrationCalculation();
        calculation.setProrationApplies(true);
        calculation.setNetAmount(new BigDecimal("100.00"));
        calculation.setTotalDaysInPeriod(500); // Excessive period

        // When
        boolean isValid = prorationService.validateProration(calculation);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void validateProration_WithZeroDayPeriod_ShouldReturnFalse() {
        // Given
        ProrationCalculation calculation = new ProrationCalculation();
        calculation.setProrationApplies(true);
        calculation.setNetAmount(new BigDecimal("100.00"));
        calculation.setTotalDaysInPeriod(0);

        // When
        boolean isValid = prorationService.validateProration(calculation);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void validateProration_WithNegativeDaysRemaining_ShouldReturnFalse() {
        // Given
        ProrationCalculation calculation = new ProrationCalculation();
        calculation.setProrationApplies(true);
        calculation.setNetAmount(new BigDecimal("100.00"));
        calculation.setTotalDaysInPeriod(30);
        calculation.setDaysRemaining(-5);

        // When
        boolean isValid = prorationService.validateProration(calculation);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void formatProrationSummary_WithNoProration_ShouldReturnCorrectFormat() {
        // Given
        ProrationCalculation calculation = new ProrationCalculation("No proration needed");

        // When
        String summary = prorationService.formatProrationSummary(calculation);

        // Then
        assertThat(summary).isEqualTo("No proration required: No proration needed");
    }

    @Test
    void formatProrationSummary_WithChargeProration_ShouldReturnCorrectFormat() {
        // Given
        ProrationCalculation calculation = prorationService.calculateProration(testSubscription, newPlan, changeDate);

        // When
        String summary = prorationService.formatProrationSummary(calculation);

        // Then
        assertThat(summary).contains("Proration charge of");
        assertThat(summary).contains("for 20 remaining days");
    }

    @Test
    void formatProrationSummary_WithCreditProration_ShouldReturnCorrectFormat() {
        // Given - downgrade scenario
        currentPlan.setAmount(new BigDecimal("200.00"));
        newPlan.setAmount(new BigDecimal("100.00"));
        ProrationCalculation calculation = prorationService.calculateProration(testSubscription, newPlan, changeDate);

        // When
        String summary = prorationService.formatProrationSummary(calculation);

        // Then
        assertThat(summary).contains("Proration credit of");
        assertThat(summary).contains("for 20 remaining days");
    }

    @Test
    void formatProrationSummary_WithNoAmount_ShouldReturnNoAmountMessage() {
        // Given
        ProrationCalculation calculation = new ProrationCalculation();
        calculation.setProrationApplies(true);
        calculation.setNetAmount(BigDecimal.ZERO);
        calculation.setDaysRemaining(20);

        // When
        String summary = prorationService.formatProrationSummary(calculation);

        // Then
        assertThat(summary).isEqualTo("Proration calculated with no net amount due");
    }

    @Test
    void calculateProration_WithPrecisionRounding_ShouldHandleCorrectly() {
        // Given - amounts that require precise rounding
        currentPlan.setAmount(new BigDecimal("99.99"));
        newPlan.setAmount(new BigDecimal("199.99"));
        
        // 7-day period with 3 days remaining
        testSubscription.setCurrentPeriodStart(ZonedDateTime.now().minusDays(4));
        testSubscription.setCurrentPeriodEnd(ZonedDateTime.now().plusDays(3));
        changeDate = ZonedDateTime.now();

        // When
        ProrationCalculation result = prorationService.calculateProration(testSubscription, newPlan, changeDate);

        // Then
        assertThat(result.getProrationApplies()).isTrue();
        assertThat(result.getTotalDaysInPeriod()).isEqualTo(7);
        assertThat(result.getDaysRemaining()).isEqualTo(3);
        
        // Check that amounts are properly rounded to 2 decimal places
        assertThat(result.getNetAmount().scale()).isEqualTo(2);
        assertThat(result.getUnusedAmount().scale()).isLessThanOrEqualTo(4);
        assertThat(result.getProratedAmount().scale()).isLessThanOrEqualTo(4);
    }

    @Test
    void calculateProration_WithSingleDayRemaining_ShouldCalculateCorrectly() {
        // Given - only 1 day remaining in period
        testSubscription.setCurrentPeriodStart(ZonedDateTime.now().minusDays(29));
        testSubscription.setCurrentPeriodEnd(ZonedDateTime.now().plusDays(1));
        changeDate = ZonedDateTime.now();

        // When
        ProrationCalculation result = prorationService.calculateProration(testSubscription, newPlan, changeDate);

        // Then
        assertThat(result.getProrationApplies()).isTrue();
        assertThat(result.getTotalDaysInPeriod()).isEqualTo(30);
        assertThat(result.getDaysUsed()).isEqualTo(29);
        assertThat(result.getDaysRemaining()).isEqualTo(1);
        
        // Daily rates: $100/30 = $3.3333, $200/30 = $6.6667
        // Net charge: $6.67 - $3.33 = $3.33
        assertThat(result.getNetAmount()).isEqualByComparingTo(new BigDecimal("3.33"));
    }

    @Test
    void calculateRefundProration_WithSingleDayRemaining_ShouldCalculateMinimalRefund() {
        // Given - only 1 day remaining
        testSubscription.setCurrentPeriodStart(ZonedDateTime.now().minusDays(29));
        testSubscription.setCurrentPeriodEnd(ZonedDateTime.now().plusDays(1));
        ZonedDateTime cancellationDate = ZonedDateTime.now();

        // When
        ProrationCalculation result = prorationService.calculateRefundProration(testSubscription, cancellationDate);

        // Then
        assertThat(result.getProrationApplies()).isTrue();
        assertThat(result.getDaysRemaining()).isEqualTo(1);
        
        // Refund: $100/30 * 1 = $3.3333 (rounded to 4 decimal places)
        assertThat(result.getUnusedAmount()).isEqualByComparingTo(new BigDecimal("3.3333"));
        assertThat(result.getNetAmount()).isEqualByComparingTo(new BigDecimal("-3.33"));
    }
}
