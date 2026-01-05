package com.talentica.paymentgateway.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionPlanHelperTest {

    @Test
    void formatted_interval_price_active_count_and_metadata() {
        SubscriptionPlan plan = new SubscriptionPlan("P1", "Pro", new BigDecimal("30.00"));
        plan.setIntervalUnit("MONTH");
        plan.setIntervalCount(1);
        plan.setTrialPeriodDays(14);
        assertThat(plan.getFormattedInterval()).contains("Every");
        assertThat(plan.getFormattedPrice()).isEqualTo("$30.00");
        assertThat(plan.getDisplayName()).contains("Pro");
        assertThat(plan.hasTrialPeriod()).isTrue();

        // Active subscriptions count
        plan.setSubscriptions(new ArrayList<>());
        Customer customer = new Customer("c@example.com");
        PaymentMethod pm = new PaymentMethod(customer, "tok_1", "CREDIT_CARD");
        Subscription s1 = new Subscription("SUB-1", customer, plan, pm);
        s1.setStatus(SubscriptionStatus.ACTIVE);
        Subscription s2 = new Subscription("SUB-2", customer, plan, pm);
        s2.setStatus(SubscriptionStatus.CANCELLED);
        plan.getSubscriptions().add(s1);
        plan.getSubscriptions().add(s2);
        assertThat(plan.getActiveSubscriptionsCount()).isEqualTo(1);
        assertThat(plan.getTotalMonthlyRevenue()).isGreaterThanOrEqualTo(BigDecimal.ZERO);

        // Metadata
        plan.addMetadata("region", "US");
        assertThat(plan.getMetadata("region")).isEqualTo("US");

        // Other intervals
        plan.setIntervalUnit("DAY"); plan.setIntervalCount(1); plan.getDisplayName();
        plan.setIntervalUnit("WEEK"); plan.setIntervalCount(1); plan.getDisplayName();
        plan.setIntervalUnit("YEAR"); plan.setIntervalCount(1); plan.getDisplayName();
    }
}
