package com.talentica.paymentgateway.dto.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating or updating customer profiles in Authorize.Net CIM
 */
public class CustomerProfileRequest {

    @NotBlank(message = "Customer ID is required")
    @Size(max = 20, message = "Customer ID must not exceed 20 characters")
    private String customerId;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @Size(max = 50, message = "First name must not exceed 50 characters")
    private String firstName;

    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String lastName;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    // Billing Address Fields
    @Size(max = 60, message = "Address line 1 must not exceed 60 characters")
    private String billingAddressLine1;

    @Size(max = 60, message = "Address line 2 must not exceed 60 characters")
    private String billingAddressLine2;

    @Size(max = 40, message = "City must not exceed 40 characters")
    private String billingCity;

    @Size(max = 40, message = "State must not exceed 40 characters")
    private String billingState;

    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    private String billingPostalCode;

    @Size(max = 60, message = "Country must not exceed 60 characters")
    private String billingCountry;

    @Size(max = 25, message = "Phone number must not exceed 25 characters")
    private String phoneNumber;

    @Size(max = 25, message = "Fax number must not exceed 25 characters")
    private String faxNumber;

    // Default constructor
    public CustomerProfileRequest() {}

    // Constructor with required fields
    public CustomerProfileRequest(String customerId, String email) {
        this.customerId = customerId;
        this.email = email;
    }

    // Getters and Setters
    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getFaxNumber() {
        return faxNumber;
    }

    public void setFaxNumber(String faxNumber) {
        this.faxNumber = faxNumber;
    }

    @Override
    public String toString() {
        return "CustomerProfileRequest{" +
                "customerId='" + customerId + '\'' +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", description='" + description + '\'' +
                ", billingCity='" + billingCity + '\'' +
                ", billingState='" + billingState + '\'' +
                ", billingCountry='" + billingCountry + '\'' +
                '}';
    }
}
