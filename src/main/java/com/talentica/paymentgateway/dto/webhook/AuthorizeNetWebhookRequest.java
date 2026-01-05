package com.talentica.paymentgateway.dto.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.ZonedDateTime;
import java.util.Map;

/**
 * DTO for incoming Authorize.Net webhook requests.
 * Represents the structure of webhook payloads sent by Authorize.Net.
 * 
 * Authorize.Net webhook events include:
 * - net.authorize.payment.authcapture.created
 * - net.authorize.payment.authorization.created
 * - net.authorize.payment.capture.created
 * - net.authorize.payment.refund.created
 * - net.authorize.payment.void.created
 * - net.authorize.payment.fraud.approved
 * - net.authorize.payment.fraud.declined
 * - net.authorize.payment.fraud.held
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
public class AuthorizeNetWebhookRequest {
    
    @JsonProperty("notificationId")
    @NotBlank(message = "Notification ID is required")
    private String notificationId;
    
    @JsonProperty("eventType")
    @NotBlank(message = "Event type is required")
    private String eventType;
    
    @JsonProperty("eventDate")
    @NotNull(message = "Event date is required")
    private ZonedDateTime eventDate;
    
    @JsonProperty("webhookId")
    @NotBlank(message = "Webhook ID is required")
    private String webhookId;
    
    @JsonProperty("payload")
    @NotNull(message = "Payload is required")
    private AuthorizeNetPayload payload;
    
    // Default constructor
    public AuthorizeNetWebhookRequest() {}
    
    // Parameterized constructor
    public AuthorizeNetWebhookRequest(String notificationId, String eventType, ZonedDateTime eventDate, 
                                    String webhookId, AuthorizeNetPayload payload) {
        this.notificationId = notificationId;
        this.eventType = eventType;
        this.eventDate = eventDate;
        this.webhookId = webhookId;
        this.payload = payload;
    }
    
    // Getters and Setters
    public String getNotificationId() {
        return notificationId;
    }
    
    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public ZonedDateTime getEventDate() {
        return eventDate;
    }
    
    public void setEventDate(ZonedDateTime eventDate) {
        this.eventDate = eventDate;
    }
    
    public String getWebhookId() {
        return webhookId;
    }
    
    public void setWebhookId(String webhookId) {
        this.webhookId = webhookId;
    }
    
    public AuthorizeNetPayload getPayload() {
        return payload;
    }
    
    public void setPayload(AuthorizeNetPayload payload) {
        this.payload = payload;
    }
    
    // Utility methods
    public boolean isPaymentEvent() {
        return eventType != null && eventType.startsWith("net.authorize.payment.");
    }
    
    public boolean isSuccessfulPayment() {
        return isPaymentEvent() && 
               (eventType.contains(".authcapture.created") || 
                eventType.contains(".authorization.created") || 
                eventType.contains(".capture.created"));
    }
    
    public boolean isRefundEvent() {
        return eventType != null && eventType.contains(".refund.created");
    }
    
    public boolean isVoidEvent() {
        return eventType != null && eventType.contains(".void.created");
    }
    
    public boolean isFraudEvent() {
        return eventType != null && eventType.contains(".fraud.");
    }
    
    public String getTransactionId() {
        return payload != null ? payload.getId() : null;
    }
    
    public String getTransactionType() {
        if (eventType == null) return "UNKNOWN";
        
        if (eventType.contains(".authcapture.")) return "PURCHASE";
        if (eventType.contains(".authorization.")) return "AUTHORIZE";
        if (eventType.contains(".capture.")) return "CAPTURE";
        if (eventType.contains(".refund.")) return "REFUND";
        if (eventType.contains(".void.")) return "VOID";
        if (eventType.contains(".fraud.")) return "FRAUD_REVIEW";
        
        return "UNKNOWN";
    }
    
    @Override
    public String toString() {
        return "AuthorizeNetWebhookRequest{" +
               "notificationId='" + notificationId + '\'' +
               ", eventType='" + eventType + '\'' +
               ", eventDate=" + eventDate +
               ", webhookId='" + webhookId + '\'' +
               ", transactionId='" + getTransactionId() + '\'' +
               '}';
    }
    
    /**
     * Nested class representing the webhook payload structure.
     */
    public static class AuthorizeNetPayload {
        
        @JsonProperty("responseCode")
        private Integer responseCode;
        
        @JsonProperty("authCode")
        private String authCode;
        
        @JsonProperty("avsResponse")
        private String avsResponse;
        
        @JsonProperty("id")
        private String id; // Transaction ID
        
        @JsonProperty("accountType")
        private String accountType;
        
        @JsonProperty("accountNumber")
        private String accountNumber; // Masked
        
        @JsonProperty("invoiceNumber")
        private String invoiceNumber;
        
        @JsonProperty("description")
        private String description;
        
        @JsonProperty("authAmount")
        private Double authAmount;
        
        @JsonProperty("settleAmount")
        private Double settleAmount;
        
        @JsonProperty("merchantReferenceId")
        private String merchantReferenceId;
        
        @JsonProperty("customerProfileId")
        private String customerProfileId;
        
        @JsonProperty("customerPaymentProfileId")
        private String customerPaymentProfileId;
        
        @JsonProperty("customerShippingAddressId")
        private String customerShippingAddressId;
        
        @JsonProperty("order")
        private Map<String, Object> order;
        
        @JsonProperty("billTo")
        private Map<String, Object> billTo;
        
        @JsonProperty("shipTo")
        private Map<String, Object> shipTo;
        
        @JsonProperty("recurringBilling")
        private Boolean recurringBilling;
        
        @JsonProperty("cardCodeResponse")
        private String cardCodeResponse;
        
        @JsonProperty("cavvResponse")
        private String cavvResponse;
        
        @JsonProperty("emvResponse")
        private Map<String, Object> emvResponse;
        
        @JsonProperty("transHashSha2")
        private String transHashSha2;
        
        @JsonProperty("profile")
        private Map<String, Object> profile;
        
        @JsonProperty("solution")
        private Map<String, Object> solution;
        
        @JsonProperty("emvDetails")
        private Map<String, Object> emvDetails;
        
        // Default constructor
        public AuthorizeNetPayload() {}
        
        // Getters and Setters
        public Integer getResponseCode() {
            return responseCode;
        }
        
        public void setResponseCode(Integer responseCode) {
            this.responseCode = responseCode;
        }
        
        public String getAuthCode() {
            return authCode;
        }
        
        public void setAuthCode(String authCode) {
            this.authCode = authCode;
        }
        
        public String getAvsResponse() {
            return avsResponse;
        }
        
        public void setAvsResponse(String avsResponse) {
            this.avsResponse = avsResponse;
        }
        
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getAccountType() {
            return accountType;
        }
        
        public void setAccountType(String accountType) {
            this.accountType = accountType;
        }
        
        public String getAccountNumber() {
            return accountNumber;
        }
        
        public void setAccountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
        }
        
        public String getInvoiceNumber() {
            return invoiceNumber;
        }
        
        public void setInvoiceNumber(String invoiceNumber) {
            this.invoiceNumber = invoiceNumber;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public Double getAuthAmount() {
            return authAmount;
        }
        
        public void setAuthAmount(Double authAmount) {
            this.authAmount = authAmount;
        }
        
        public Double getSettleAmount() {
            return settleAmount;
        }
        
        public void setSettleAmount(Double settleAmount) {
            this.settleAmount = settleAmount;
        }
        
        public String getMerchantReferenceId() {
            return merchantReferenceId;
        }
        
        public void setMerchantReferenceId(String merchantReferenceId) {
            this.merchantReferenceId = merchantReferenceId;
        }
        
        public String getCustomerProfileId() {
            return customerProfileId;
        }
        
        public void setCustomerProfileId(String customerProfileId) {
            this.customerProfileId = customerProfileId;
        }
        
        public String getCustomerPaymentProfileId() {
            return customerPaymentProfileId;
        }
        
        public void setCustomerPaymentProfileId(String customerPaymentProfileId) {
            this.customerPaymentProfileId = customerPaymentProfileId;
        }
        
        public String getCustomerShippingAddressId() {
            return customerShippingAddressId;
        }
        
        public void setCustomerShippingAddressId(String customerShippingAddressId) {
            this.customerShippingAddressId = customerShippingAddressId;
        }
        
        public Map<String, Object> getOrder() {
            return order;
        }
        
        public void setOrder(Map<String, Object> order) {
            this.order = order;
        }
        
        public Map<String, Object> getBillTo() {
            return billTo;
        }
        
        public void setBillTo(Map<String, Object> billTo) {
            this.billTo = billTo;
        }
        
        public Map<String, Object> getShipTo() {
            return shipTo;
        }
        
        public void setShipTo(Map<String, Object> shipTo) {
            this.shipTo = shipTo;
        }
        
        public Boolean getRecurringBilling() {
            return recurringBilling;
        }
        
        public void setRecurringBilling(Boolean recurringBilling) {
            this.recurringBilling = recurringBilling;
        }
        
        public String getCardCodeResponse() {
            return cardCodeResponse;
        }
        
        public void setCardCodeResponse(String cardCodeResponse) {
            this.cardCodeResponse = cardCodeResponse;
        }
        
        public String getCavvResponse() {
            return cavvResponse;
        }
        
        public void setCavvResponse(String cavvResponse) {
            this.cavvResponse = cavvResponse;
        }
        
        public Map<String, Object> getEmvResponse() {
            return emvResponse;
        }
        
        public void setEmvResponse(Map<String, Object> emvResponse) {
            this.emvResponse = emvResponse;
        }
        
        public String getTransHashSha2() {
            return transHashSha2;
        }
        
        public void setTransHashSha2(String transHashSha2) {
            this.transHashSha2 = transHashSha2;
        }
        
        public Map<String, Object> getProfile() {
            return profile;
        }
        
        public void setProfile(Map<String, Object> profile) {
            this.profile = profile;
        }
        
        public Map<String, Object> getSolution() {
            return solution;
        }
        
        public void setSolution(Map<String, Object> solution) {
            this.solution = solution;
        }
        
        public Map<String, Object> getEmvDetails() {
            return emvDetails;
        }
        
        public void setEmvDetails(Map<String, Object> emvDetails) {
            this.emvDetails = emvDetails;
        }
        
        @Override
        public String toString() {
            return "AuthorizeNetPayload{" +
                   "id='" + id + '\'' +
                   ", responseCode=" + responseCode +
                   ", authCode='" + authCode + '\'' +
                   ", authAmount=" + authAmount +
                   ", settleAmount=" + settleAmount +
                   '}';
        }
    }
}
