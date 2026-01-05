package com.talentica.paymentgateway.mapper;

import com.talentica.paymentgateway.dto.subscription.CreateSubscriptionRequest;
import com.talentica.paymentgateway.dto.subscription.PlanResponse;
import com.talentica.paymentgateway.dto.subscription.SubscriptionResponse;
import com.talentica.paymentgateway.entity.Customer;
import com.talentica.paymentgateway.entity.Subscription;
import com.talentica.paymentgateway.entity.SubscriptionPlan;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-01-05T19:18:43+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.12 (Oracle Corporation)"
)
@Component
public class SubscriptionMapperImpl implements SubscriptionMapper {

    @Override
    public SubscriptionResponse toSubscriptionResponse(Subscription subscription) {
        if ( subscription == null ) {
            return null;
        }

        SubscriptionResponse subscriptionResponse = new SubscriptionResponse();

        subscriptionResponse.setSubscriptionId( subscription.getSubscriptionId() );
        subscriptionResponse.setCustomerId( subscriptionCustomerCustomerReference( subscription ) );
        subscriptionResponse.setPlanCode( subscriptionPlanPlanCode( subscription ) );
        subscriptionResponse.setPlanName( subscriptionPlanName( subscription ) );
        subscriptionResponse.setPlanAmount( subscriptionPlanAmount( subscription ) );
        subscriptionResponse.setCurrency( subscriptionPlanCurrency( subscription ) );
        subscriptionResponse.setStatus( subscription.getStatus() );
        subscriptionResponse.setCreatedAt( map( subscription.getCreatedAt() ) );
        subscriptionResponse.setCurrentPeriodStart( subscription.getCurrentPeriodStart() );
        subscriptionResponse.setCurrentPeriodEnd( subscription.getCurrentPeriodEnd() );
        subscriptionResponse.setTrialStart( subscription.getTrialStart() );
        subscriptionResponse.setTrialEnd( subscription.getTrialEnd() );
        subscriptionResponse.setNextBillingDate( subscription.getNextBillingDate() );
        subscriptionResponse.setBillingCycleAnchor( subscription.getBillingCycleAnchor() );
        subscriptionResponse.setCancelledAt( subscription.getCancelledAt() );
        subscriptionResponse.setCancellationReason( subscription.getCancellationReason() );
        subscriptionResponse.setDaysUntilNextBilling( subscription.getDaysUntilNextBilling() );
        Map<String, Object> map = subscription.getMetadata();
        if ( map != null ) {
            subscriptionResponse.setMetadata( new LinkedHashMap<String, Object>( map ) );
        }
        subscriptionResponse.setUpdatedAt( map( subscription.getUpdatedAt() ) );

        return subscriptionResponse;
    }

    @Override
    public PlanResponse toPlanResponse(SubscriptionPlan plan) {
        if ( plan == null ) {
            return null;
        }

        PlanResponse planResponse = new PlanResponse();

        planResponse.setPlanCode( plan.getPlanCode() );
        planResponse.setName( plan.getName() );
        planResponse.setDescription( plan.getDescription() );
        planResponse.setAmount( plan.getAmount() );
        planResponse.setCurrency( plan.getCurrency() );
        planResponse.setIntervalUnit( plan.getIntervalUnit() );
        planResponse.setIntervalCount( plan.getIntervalCount() );
        planResponse.setTrialPeriodDays( plan.getTrialPeriodDays() );
        planResponse.setIsActive( plan.getIsActive() );
        planResponse.setCreatedAt( map( plan.getCreatedAt() ) );
        planResponse.setUpdatedAt( map( plan.getUpdatedAt() ) );
        Map<String, Object> map = plan.getMetadata();
        if ( map != null ) {
            planResponse.setMetadata( new LinkedHashMap<String, Object>( map ) );
        }
        planResponse.setFormattedInterval( plan.getFormattedInterval() );
        planResponse.setFormattedPrice( plan.getFormattedPrice() );
        planResponse.setDisplayName( plan.getDisplayName() );

        return planResponse;
    }

    @Override
    public Subscription toSubscription(CreateSubscriptionRequest request) {
        if ( request == null ) {
            return null;
        }

        Subscription subscription = new Subscription();

        return subscription;
    }

    private String subscriptionCustomerCustomerReference(Subscription subscription) {
        if ( subscription == null ) {
            return null;
        }
        Customer customer = subscription.getCustomer();
        if ( customer == null ) {
            return null;
        }
        String customerReference = customer.getCustomerReference();
        if ( customerReference == null ) {
            return null;
        }
        return customerReference;
    }

    private String subscriptionPlanPlanCode(Subscription subscription) {
        if ( subscription == null ) {
            return null;
        }
        SubscriptionPlan plan = subscription.getPlan();
        if ( plan == null ) {
            return null;
        }
        String planCode = plan.getPlanCode();
        if ( planCode == null ) {
            return null;
        }
        return planCode;
    }

    private String subscriptionPlanName(Subscription subscription) {
        if ( subscription == null ) {
            return null;
        }
        SubscriptionPlan plan = subscription.getPlan();
        if ( plan == null ) {
            return null;
        }
        String name = plan.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }

    private BigDecimal subscriptionPlanAmount(Subscription subscription) {
        if ( subscription == null ) {
            return null;
        }
        SubscriptionPlan plan = subscription.getPlan();
        if ( plan == null ) {
            return null;
        }
        BigDecimal amount = plan.getAmount();
        if ( amount == null ) {
            return null;
        }
        return amount;
    }

    private String subscriptionPlanCurrency(Subscription subscription) {
        if ( subscription == null ) {
            return null;
        }
        SubscriptionPlan plan = subscription.getPlan();
        if ( plan == null ) {
            return null;
        }
        String currency = plan.getCurrency();
        if ( currency == null ) {
            return null;
        }
        return currency;
    }
}
