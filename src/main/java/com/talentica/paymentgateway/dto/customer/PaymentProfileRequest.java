package com.talentica.paymentgateway.dto.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating or updating payment profiles
 */
public class PaymentProfileRequest {

    @NotBlank(message = "Card number is required")
    @Pattern(regexp = "\\d{13,19}", message = "Card number must be 13-19 digits")
    private String cardNumber;

    @NotBlank(message = "Expiry month is required")
    @Pattern(regexp = "^(0[1-9]|1[0-2])$", message = "Expiry month must be 01-12")
    private String expiryMonth;

    @NotBlank(message = "Expiry year is required")
    @Pattern(regexp = "^\\d{4}$", message = "Expiry year must be 4 digits")
    private String expiryYear;

    @Pattern(regexp = "\\d{3,4}", message = "CVV must be 3-4 digits")
    private String cvv;

    // Billing Address Fields
    @Size(max = 50, message = "First name must not exceed 50 characters")
    private String firstName;

    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String lastName;

    @Size(max = 50, message = "Company must not exceed 50 characters")
    private String company;

    @Size(max = 60, message = "Address must not exceed 60 characters")
    private String address;

    @Size(max = 40, message = "City must not exceed 40 characters")
    private String city;

    @Size(max = 40, message = "State must not exceed 40 characters")
    private String state;

    @Size(max = 20, message = "ZIP code must not exceed 20 characters")
    private String zip;

    @Size(max = 60, message = "Country must not exceed 60 characters")
    private String country;

    @Size(max = 25, message = "Phone number must not exceed 25 characters")
    private String phoneNumber;

    @Size(max = 25, message = "Fax number must not exceed 25 characters")
    private String faxNumber;

    private boolean defaultPaymentProfile = false;

    // Default constructor
    public PaymentProfileRequest() {}

    // Getters and Setters
    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getExpiryMonth() {
        return expiryMonth;
    }

    public void setExpiryMonth(String expiryMonth) {
        this.expiryMonth = expiryMonth;
    }

    public String getExpiryYear() {
        return expiryYear;
    }

    public void setExpiryYear(String expiryYear) {
        this.expiryYear = expiryYear;
    }

    public String getCvv() {
        return cvv;
    }

    public void setCvv(String cvv) {
        this.cvv = cvv;
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

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
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

    public boolean isDefaultPaymentProfile() {
        return defaultPaymentProfile;
    }

    public void setDefaultPaymentProfile(boolean defaultPaymentProfile) {
        this.defaultPaymentProfile = defaultPaymentProfile;
    }

    @Override
    public String toString() {
        return "PaymentProfileRequest{" +
                "cardNumber='****" + (cardNumber != null && cardNumber.length() >= 4 ? 
                    cardNumber.substring(cardNumber.length() - 4) : "****") + '\'' +
                ", expiryMonth='" + expiryMonth + '\'' +
                ", expiryYear='" + expiryYear + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", city='" + city + '\'' +
                ", state='" + state + '\'' +
                ", defaultPaymentProfile=" + defaultPaymentProfile +
                '}';
    }
}
