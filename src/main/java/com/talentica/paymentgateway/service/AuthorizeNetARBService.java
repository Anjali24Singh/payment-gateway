package com.talentica.paymentgateway.service;

import com.talentica.paymentgateway.entity.Customer;
import com.talentica.paymentgateway.entity.PaymentMethod;
import com.talentica.paymentgateway.entity.SubscriptionPlan;
import com.talentica.paymentgateway.exception.PaymentProcessingException;
import net.authorize.Environment;
import net.authorize.api.contract.v1.*;
import net.authorize.api.controller.ARBCreateSubscriptionController;
import net.authorize.api.controller.ARBGetSubscriptionController;
import net.authorize.api.controller.ARBCancelSubscriptionController;
import net.authorize.api.controller.base.ApiOperationBase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Service for creating Authorize.Net ARB (Automatic Recurring Billing) subscriptions
 * that appear directly in the Authorize.Net merchant portal.
 */
@Slf4j
@Service
public class AuthorizeNetARBService {

    private final MerchantAuthenticationType merchant;
    private final Environment environment;

    public AuthorizeNetARBService(
            @Value("${app.authorize-net.api-login-id}") String apiLoginId,
            @Value("${app.authorize-net.transaction-key}") String transactionKey,
            @Value("${app.authorize-net.environment}") String env) {
        
        this.merchant = new MerchantAuthenticationType();
        this.merchant.setName(apiLoginId);
        this.merchant.setTransactionKey(transactionKey);
        
        this.environment = "sandbox".equalsIgnoreCase(env) ? Environment.SANDBOX : Environment.PRODUCTION;
        
        log.info("AuthorizeNetARBService initialized - Environment: {}", this.environment);
    }

    /**
     * Creates an ARB subscription in Authorize.Net that will appear in the portal.
     * 
     * @param customer Customer entity
     * @param plan Subscription plan
     * @param paymentMethod Payment method
     * @return Authorize.Net subscription ID
     */
    public String createARBSubscription(Customer customer, SubscriptionPlan plan, PaymentMethod paymentMethod) {
        try {
            ApiOperationBase.setEnvironment(environment);
            
            // Create subscription request
            ARBSubscriptionType subscription = new ARBSubscriptionType();
            
            // Set subscription name (appears in portal)
            subscription.setName(customer.getFirstName() + " " + customer.getLastName() + " - " + plan.getName());
            
            // Set payment schedule
            PaymentScheduleType paymentSchedule = new PaymentScheduleType();
            
            // Set interval
            PaymentScheduleType.Interval interval = new PaymentScheduleType.Interval();
            interval.setLength(plan.getIntervalCount().shortValue());
            
            // Map interval unit
            ARBSubscriptionUnitEnum intervalUnit;
            switch (plan.getIntervalUnit().toUpperCase()) {
                case "DAY":
                    intervalUnit = ARBSubscriptionUnitEnum.DAYS;
                    break;
                case "WEEK":
                    intervalUnit = ARBSubscriptionUnitEnum.DAYS;
                    interval.setLength((short) (plan.getIntervalCount() * 7)); // Convert weeks to days
                    break;
                case "MONTH":
                    intervalUnit = ARBSubscriptionUnitEnum.MONTHS;
                    break;
                case "YEAR":
                    intervalUnit = ARBSubscriptionUnitEnum.MONTHS;
                    interval.setLength((short) (plan.getIntervalCount() * 12)); // Convert years to months
                    break;
                default:
                    intervalUnit = ARBSubscriptionUnitEnum.MONTHS;
            }
            interval.setUnit(intervalUnit);
            paymentSchedule.setInterval(interval);
            
            // Set start date (today + 1 day to see it immediately)
            try {
                LocalDate startDate = LocalDate.now().plusDays(1);
                XMLGregorianCalendar xmlDate = DatatypeFactory.newInstance()
                    .newXMLGregorianCalendar(startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                paymentSchedule.setStartDate(xmlDate);
            } catch (Exception e) {
                log.error("Error setting start date", e);
                throw new PaymentProcessingException("Failed to set start date: " + e.getMessage(), "DATE_ERROR");
            }
            
            // Set total occurrences (999 = unlimited for practical purposes)
            paymentSchedule.setTotalOccurrences((short) 999);
            
            // Set trial occurrences if plan has trial
            if (plan.hasTrialPeriod()) {
                paymentSchedule.setTrialOccurrences(plan.getTrialPeriodDays().shortValue());
            }
            
            subscription.setPaymentSchedule(paymentSchedule);
            
            // Set amount
            subscription.setAmount(plan.getAmount());
            
            // Set trial amount (usually $0)
            if (plan.hasTrialPeriod()) {
                subscription.setTrialAmount(BigDecimal.ZERO);
            }
            
            // Set customer information
            NameAndAddressType billTo = new NameAndAddressType();
            billTo.setFirstName(customer.getFirstName());
            billTo.setLastName(customer.getLastName());
            billTo.setAddress(customer.getBillingAddressLine1());
            billTo.setCity(customer.getBillingCity());
            billTo.setState(customer.getBillingState());
            billTo.setZip(customer.getBillingPostalCode());
            billTo.setCountry(customer.getBillingCountry());
            subscription.setBillTo(billTo);
            
            // Set payment method
            PaymentType payment = new PaymentType();
            CreditCardType creditCard = new CreditCardType();
            creditCard.setCardNumber(paymentMethod.getCardNumber());
            creditCard.setExpirationDate(paymentMethod.getExpiryYear() + "-" + 
                String.format("%02d", Integer.parseInt(paymentMethod.getExpiryMonth())));
            creditCard.setCardCode(paymentMethod.getCvv());
            payment.setCreditCard(creditCard);
            subscription.setPayment(payment);
            
            // Create the request
            ARBCreateSubscriptionRequest request = new ARBCreateSubscriptionRequest();
            request.setMerchantAuthentication(merchant);
            request.setSubscription(subscription);
            
            log.info("Creating ARB subscription for customer: {} with plan: {}", 
                       customer.getCustomerId(), plan.getPlanCode());
            
            // Execute the request
            ARBCreateSubscriptionController controller = new ARBCreateSubscriptionController(request);
            controller.execute();
            
            ARBCreateSubscriptionResponse response = controller.getApiResponse();
            
            if (response != null && response.getMessages().getResultCode() == MessageTypeEnum.OK) {
                String subscriptionId = response.getSubscriptionId();
                log.info("✅ ARB subscription created successfully - Subscription ID: {}, Customer: {}", 
                           subscriptionId, customer.getCustomerId());
                return subscriptionId;
            } else {
                String errorMessage = "Failed to create ARB subscription";
                if (response != null && response.getMessages() != null && 
                    response.getMessages().getMessage() != null && 
                    !response.getMessages().getMessage().isEmpty()) {
                    errorMessage = response.getMessages().getMessage().get(0).getText();
                }
                log.error("❌ ARB subscription creation failed: {}", errorMessage);
                throw new PaymentProcessingException("ARB subscription creation failed: " + errorMessage, "ARB_CREATION_FAILED");
            }
            
        } catch (Exception e) {
            log.error("Error creating ARB subscription for customer: {}", customer.getCustomerId(), e);
            throw new PaymentProcessingException("ARB subscription creation error: " + e.getMessage(), "ARB_ERROR");
        }
    }
    
    /**
     * Gets ARB subscription details from Authorize.Net.
     * 
     * @param subscriptionId Authorize.Net subscription ID
     * @return Subscription details
     */
    public ARBGetSubscriptionResponse getARBSubscription(String subscriptionId) {
        try {
            ApiOperationBase.setEnvironment(environment);
            
            ARBGetSubscriptionRequest request = new ARBGetSubscriptionRequest();
            request.setMerchantAuthentication(merchant);
            request.setSubscriptionId(subscriptionId);
            
            ARBGetSubscriptionController controller = new ARBGetSubscriptionController(request);
            controller.execute();
            
            return controller.getApiResponse();
            
        } catch (Exception e) {
            log.error("Error getting ARB subscription: {}", subscriptionId, e);
            throw new PaymentProcessingException("Failed to get ARB subscription: " + e.getMessage(), "ARB_GET_ERROR");
        }
    }
    
    /**
     * Cancels an ARB subscription in Authorize.Net.
     * 
     * @param subscriptionId Authorize.Net subscription ID
     * @return true if cancelled successfully
     */
    public boolean cancelARBSubscription(String subscriptionId) {
        try {
            ApiOperationBase.setEnvironment(environment);
            
            ARBCancelSubscriptionRequest request = new ARBCancelSubscriptionRequest();
            request.setMerchantAuthentication(merchant);
            request.setSubscriptionId(subscriptionId);
            
            ARBCancelSubscriptionController controller = new ARBCancelSubscriptionController(request);
            controller.execute();
            
            ARBCancelSubscriptionResponse response = controller.getApiResponse();
            
            if (response != null && response.getMessages().getResultCode() == MessageTypeEnum.OK) {
                log.info("✅ ARB subscription cancelled successfully - Subscription ID: {}", subscriptionId);
                return true;
            } else {
                String errorMessage = "Failed to cancel ARB subscription";
                if (response != null && response.getMessages() != null && 
                    response.getMessages().getMessage() != null && 
                    !response.getMessages().getMessage().isEmpty()) {
                    errorMessage = response.getMessages().getMessage().get(0).getText();
                }
                log.error("❌ ARB subscription cancellation failed: {}", errorMessage);
                return false;
            }
            
        } catch (Exception e) {
            log.error("Error cancelling ARB subscription: {}", subscriptionId, e);
            return false;
        }
    }
}
