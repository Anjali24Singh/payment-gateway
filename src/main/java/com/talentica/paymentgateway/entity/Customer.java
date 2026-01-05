package com.talentica.paymentgateway.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a customer who can make payments and have subscriptions.
 * Customers can be associated with users and have multiple payment methods.
 */
@Entity
@Table(name = "customers",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = "customerReference")
       },
       indexes = {
           @Index(name = "idx_customers_user_id", columnList = "user_id"),
           @Index(name = "idx_customers_email", columnList = "email"),
           @Index(name = "idx_customers_reference", columnList = "customerReference")
       })
public class Customer extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Size(max = 100, message = "Customer reference must not exceed 100 characters")
    @Column(name = "customer_reference", unique = true, length = 100)
    private String customerReference;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Size(max = 100, message = "First name must not exceed 100 characters")
    @Column(name = "first_name", length = 100)
    private String firstName;

    @Size(max = 100, message = "Last name must not exceed 100 characters")
    @Column(name = "last_name", length = 100)
    private String lastName;

    @Size(max = 20, message = "Phone must not exceed 20 characters")
    @Column(name = "phone", length = 20)
    private String phone;

    @Size(max = 255, message = "Company must not exceed 255 characters")
    @Column(name = "company", length = 255)
    private String company;

    // Billing Address
    @Size(max = 255, message = "Billing address line 1 must not exceed 255 characters")
    @Column(name = "billing_address_line1", length = 255)
    private String billingAddressLine1;

    @Size(max = 255, message = "Billing address line 2 must not exceed 255 characters")
    @Column(name = "billing_address_line2", length = 255)
    private String billingAddressLine2;

    @Size(max = 100, message = "Billing city must not exceed 100 characters")
    @Column(name = "billing_city", length = 100)
    private String billingCity;

    @Size(max = 100, message = "Billing state must not exceed 100 characters")
    @Column(name = "billing_state", length = 100)
    private String billingState;

    @Size(max = 20, message = "Billing postal code must not exceed 20 characters")
    @Column(name = "billing_postal_code", length = 20)
    private String billingPostalCode;

    @Size(max = 2, message = "Billing country must be 2 characters")
    @Column(name = "billing_country", length = 2)
    private String billingCountry = "US";

    // Shipping Address
    @Size(max = 255, message = "Shipping address line 1 must not exceed 255 characters")
    @Column(name = "shipping_address_line1", length = 255)
    private String shippingAddressLine1;

    @Size(max = 255, message = "Shipping address line 2 must not exceed 255 characters")
    @Column(name = "shipping_address_line2", length = 255)
    private String shippingAddressLine2;

    @Size(max = 100, message = "Shipping city must not exceed 100 characters")
    @Column(name = "shipping_city", length = 100)
    private String shippingCity;

    @Size(max = 100, message = "Shipping state must not exceed 100 characters")
    @Column(name = "shipping_state", length = 100)
    private String shippingState;

    @Size(max = 20, message = "Shipping postal code must not exceed 20 characters")
    @Column(name = "shipping_postal_code", length = 20)
    private String shippingPostalCode;

    @Size(max = 2, message = "Shipping country must be 2 characters")
    @Column(name = "shipping_country", length = 2)
    private String shippingCountry = "US";

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Size(max = 50, message = "Authorize.Net customer profile ID must not exceed 50 characters")
    @Column(name = "authorizenet_customer_profile_id", length = 50)
    private String authorizeNetCustomerProfileId;

    // Relationships
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PaymentMethod> paymentMethods = new ArrayList<>();

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Order> orders = new ArrayList<>();

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> transactions = new ArrayList<>();

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Subscription> subscriptions = new ArrayList<>();

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SubscriptionInvoice> subscriptionInvoices = new ArrayList<>();

    // Constructors
    public Customer() {
        super();
    }

    public Customer(String email) {
        this();
        this.email = email;
    }

    public Customer(String email, String firstName, String lastName) {
        this(email);
        this.firstName = firstName;
        this.lastName = lastName;
    }

    // Getters and Setters
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getCustomerReference() {
        return customerReference;
    }

    public void setCustomerReference(String customerReference) {
        this.customerReference = customerReference;
    }

    public String getCustomerId() {
        return customerReference;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getBillingAddressLine1() {
        return billingAddressLine1;
    }

    public void setBillingAddressLine1(String billingAddressLine1) {
        this.billingAddressLine1 = billingAddressLine1;
    }

    public String getBillingAddressLine2() {
        return billingAddressLine2;
    }

    public void setBillingAddressLine2(String billingAddressLine2) {
        this.billingAddressLine2 = billingAddressLine2;
    }

    public String getBillingCity() {
        return billingCity;
    }

    public void setBillingCity(String billingCity) {
        this.billingCity = billingCity;
    }

    public String getBillingState() {
        return billingState;
    }

    public void setBillingState(String billingState) {
        this.billingState = billingState;
    }

    public String getBillingPostalCode() {
        return billingPostalCode;
    }

    public void setBillingPostalCode(String billingPostalCode) {
        this.billingPostalCode = billingPostalCode;
    }

    public String getBillingCountry() {
        return billingCountry;
    }

    public void setBillingCountry(String billingCountry) {
        this.billingCountry = billingCountry;
    }

    public String getShippingAddressLine1() {
        return shippingAddressLine1;
    }

    public void setShippingAddressLine1(String shippingAddressLine1) {
        this.shippingAddressLine1 = shippingAddressLine1;
    }

    public String getShippingAddressLine2() {
        return shippingAddressLine2;
    }

    public void setShippingAddressLine2(String shippingAddressLine2) {
        this.shippingAddressLine2 = shippingAddressLine2;
    }

    public String getShippingCity() {
        return shippingCity;
    }

    public void setShippingCity(String shippingCity) {
        this.shippingCity = shippingCity;
    }

    public String getShippingState() {
        return shippingState;
    }

    public void setShippingState(String shippingState) {
        this.shippingState = shippingState;
    }

    public String getShippingPostalCode() {
        return shippingPostalCode;
    }

    public void setShippingPostalCode(String shippingPostalCode) {
        this.shippingPostalCode = shippingPostalCode;
    }

    public String getShippingCountry() {
        return shippingCountry;
    }

    public void setShippingCountry(String shippingCountry) {
        this.shippingCountry = shippingCountry;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getAuthorizeNetCustomerProfileId() {
        return authorizeNetCustomerProfileId;
    }

    public void setAuthorizeNetCustomerProfileId(String authorizeNetCustomerProfileId) {
        this.authorizeNetCustomerProfileId = authorizeNetCustomerProfileId;
    }

    public List<PaymentMethod> getPaymentMethods() {
        return paymentMethods;
    }

    public void setPaymentMethods(List<PaymentMethod> paymentMethods) {
        this.paymentMethods = paymentMethods;
    }

    public List<Order> getOrders() {
        return orders;
    }

    public void setOrders(List<Order> orders) {
        this.orders = orders;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    public List<Subscription> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(List<Subscription> subscriptions) {
        this.subscriptions = subscriptions;
    }

    public List<SubscriptionInvoice> getSubscriptionInvoices() {
        return subscriptionInvoices;
    }

    public void setSubscriptionInvoices(List<SubscriptionInvoice> subscriptionInvoices) {
        this.subscriptionInvoices = subscriptionInvoices;
    }

    // Utility methods
    public String getFullName() {
        boolean hasFirstName = firstName != null && !firstName.trim().isEmpty();
        boolean hasLastName = lastName != null && !lastName.trim().isEmpty();
        
        if (hasFirstName && hasLastName) {
            return firstName.trim() + " " + lastName.trim();
        } else if (hasFirstName) {
            return firstName.trim();
        } else if (hasLastName) {
            return lastName.trim();
        }
        return email;
    }

    public PaymentMethod getDefaultPaymentMethod() {
        return paymentMethods.stream()
                .filter(PaymentMethod::getIsDefault)
                .findFirst()
                .orElse(null);
    }

    public void addPaymentMethod(PaymentMethod paymentMethod) {
        paymentMethods.add(paymentMethod);
        paymentMethod.setCustomer(this);
    }

    public void removePaymentMethod(PaymentMethod paymentMethod) {
        paymentMethods.remove(paymentMethod);
        paymentMethod.setCustomer(null);
    }

    public boolean hasBillingAddress() {
        return billingAddressLine1 != null && billingCity != null && 
               billingState != null && billingPostalCode != null;
    }

    public boolean hasShippingAddress() {
        return shippingAddressLine1 != null && shippingCity != null && 
               shippingState != null && shippingPostalCode != null;
    }
}
