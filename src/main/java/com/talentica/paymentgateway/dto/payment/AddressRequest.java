package com.talentica.paymentgateway.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * Address request DTO for billing and shipping information.
 * Used in payment processing for address verification and fraud prevention.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Schema(description = "Address information for billing or shipping")
public class AddressRequest {

    @Size(max = 100, message = "First name must not exceed 100 characters")
    @Schema(description = "First name", example = "John")
    @JsonProperty("firstName")
    private String firstName;

    @Size(max = 100, message = "Last name must not exceed 100 characters")
    @Schema(description = "Last name", example = "Doe")
    @JsonProperty("lastName")
    private String lastName;

    @Size(max = 100, message = "Company name must not exceed 100 characters")
    @Schema(description = "Company name", example = "Acme Corporation")
    @JsonProperty("company")
    private String company;

    @Size(max = 60, message = "Address line 1 must not exceed 60 characters")
    @Schema(description = "Address line 1", example = "123 Main Street")
    @JsonProperty("address1")
    private String address1;

    @Size(max = 60, message = "Address line 2 must not exceed 60 characters")
    @Schema(description = "Address line 2", example = "Apt 4B")
    @JsonProperty("address2")
    private String address2;

    @Size(max = 40, message = "City must not exceed 40 characters")
    @Schema(description = "City", example = "New York")
    @JsonProperty("city")
    private String city;

    @Size(max = 40, message = "State must not exceed 40 characters")
    @Schema(description = "State or province", example = "NY")
    @JsonProperty("state")
    private String state;

    @Size(max = 20, message = "ZIP code must not exceed 20 characters")
    @Pattern(regexp = "^[A-Za-z0-9\\s-]{3,20}$", 
             message = "ZIP code must be 3-20 characters and contain only letters, numbers, spaces, and hyphens")
    @Schema(description = "ZIP or postal code", example = "10001")
    @JsonProperty("zipCode")
    private String zipCode;

    @Size(max = 60, message = "Country must not exceed 60 characters")
    @Schema(description = "Country", example = "United States")
    @JsonProperty("country")
    private String country;

    @Pattern(regexp = "^[\\+]?[0-9\\s\\-\\(\\)]{10,20}$", 
             message = "Phone number must be 10-20 characters and contain only numbers, spaces, hyphens, parentheses, and plus sign")
    @Schema(description = "Phone number", example = "+1-555-123-4567")
    @JsonProperty("phoneNumber")
    private String phoneNumber;

    @Email(message = "Email must be a valid email address")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    @Schema(description = "Email address", example = "john.doe@example.com")
    @JsonProperty("email")
    private String email;

    // Default constructor
    public AddressRequest() {
    }

    // Constructor with required fields
    public AddressRequest(String firstName, String lastName, String address1, 
                         String city, String state, String zipCode, String country) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.address1 = address1;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
        this.country = country;
    }

    // Getters and Setters
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

    public String getAddress1() {
        return address1;
    }

    public void setAddress1(String address1) {
        this.address1 = address1;
    }

    public String getAddress2() {
        return address2;
    }

    public void setAddress2(String address2) {
        this.address2 = address2;
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

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Returns the full name by combining first and last name.
     */
    public String getFullName() {
        if (firstName == null && lastName == null) {
            return null;
        }
        if (firstName == null) {
            return lastName;
        }
        if (lastName == null) {
            return firstName;
        }
        return firstName + " " + lastName;
    }

    /**
     * Returns the full address as a single string.
     */
    public String getFullAddress() {
        StringBuilder address = new StringBuilder();
        
        if (address1 != null && !address1.trim().isEmpty()) {
            address.append(address1);
        }
        
        if (address2 != null && !address2.trim().isEmpty()) {
            if (address.length() > 0) {
                address.append(", ");
            }
            address.append(address2);
        }
        
        if (city != null && !city.trim().isEmpty()) {
            if (address.length() > 0) {
                address.append(", ");
            }
            address.append(city);
        }
        
        if (state != null && !state.trim().isEmpty()) {
            if (address.length() > 0) {
                address.append(", ");
            }
            address.append(state);
        }
        
        if (zipCode != null && !zipCode.trim().isEmpty()) {
            if (address.length() > 0) {
                address.append(" ");
            }
            address.append(zipCode);
        }
        
        if (country != null && !country.trim().isEmpty()) {
            if (address.length() > 0) {
                address.append(", ");
            }
            address.append(country);
        }
        
        return address.toString();
    }

    @Override
    public String toString() {
        return "AddressRequest{" +
                "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", company='" + company + '\'' +
                ", address1='" + address1 + '\'' +
                ", address2='" + address2 + '\'' +
                ", city='" + city + '\'' +
                ", state='" + state + '\'' +
                ", zipCode='" + zipCode + '\'' +
                ", country='" + country + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
