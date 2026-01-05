package com.talentica.paymentgateway.service;

import com.talentica.paymentgateway.dto.payment.CustomerRequest;
import com.talentica.paymentgateway.dto.payment.PaymentMethodRequest;
import com.talentica.paymentgateway.entity.Customer;
import com.talentica.paymentgateway.exception.PaymentProcessingException;
import com.talentica.paymentgateway.util.CorrelationIdUtil;
import net.authorize.Environment;
import net.authorize.api.contract.v1.*;
import net.authorize.api.controller.CreateCustomerProfileController;
import net.authorize.api.controller.CreateCustomerPaymentProfileController;
import net.authorize.api.controller.GetCustomerProfileController;
import net.authorize.api.controller.GetCustomerPaymentProfileController;
import net.authorize.api.controller.UpdateCustomerProfileController;
import net.authorize.api.controller.UpdateCustomerPaymentProfileController;
import net.authorize.api.controller.DeleteCustomerProfileController;
import net.authorize.api.controller.DeleteCustomerPaymentProfileController;
import net.authorize.api.controller.ValidateCustomerPaymentProfileController;
import net.authorize.api.controller.base.ApiOperationBase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for managing Authorize.Net Customer Information Manager (CIM) operations.
 * This is the missing piece that makes customers appear in the Authorize.Net portal.
 * 
 * Key Functions:
 * 1. Create customer profiles in Authorize.Net (not just local DB)
 * 2. Create payment profiles under customer profiles
 * 3. Enable proper ARB subscription management
 */
@Slf4j
@Service
public class AuthorizeNetCustomerService {

    private final MerchantAuthenticationType merchant;
    private final Environment environment;

    public AuthorizeNetCustomerService(MerchantAuthenticationType merchant,
                                     Environment environment) {
        this.merchant = merchant;
        this.environment = environment;
    }

    /**
     * Creates a customer profile in Authorize.Net CIM.
     * This is the critical missing piece - customers must exist in Authorize.Net, not just local DB.
     * 
     * @param customer Local customer entity
     * @param customerRequest Customer request data
     * @return Authorize.Net customer profile ID
     */
    public String createCustomerProfile(Customer customer, CustomerRequest customerRequest) {
        String correlationId = CorrelationIdUtil.getOrGenerate();
        
        try {
            // Set the environment for this operation
            ApiOperationBase.setEnvironment(environment);
            
            // Create customer profile request
            CustomerProfileType profile = new CustomerProfileType();
            profile.setMerchantCustomerId(customer.getCustomerId());
            profile.setEmail(customer.getEmail());
            
            // Set description field - this is what appears as "Name" in the portal
            String customerName = "";
            if (customer.getFirstName() != null && customer.getLastName() != null) {
                customerName = customer.getFirstName() + " " + customer.getLastName();
            } else if (customer.getFirstName() != null) {
                customerName = customer.getFirstName();
            } else if (customer.getLastName() != null) {
                customerName = customer.getLastName();
            } else {
                customerName = "Customer " + customer.getCustomerId();
            }
            profile.setDescription(customerName);
            
            log.info("Setting customer profile description to: '{}' for customer: {}", 
                       customerName, customer.getEmail());
            
            CreateCustomerProfileRequest apiRequest = new CreateCustomerProfileRequest();
            apiRequest.setMerchantAuthentication(merchant);
            apiRequest.setProfile(profile);
            
            log.info("Creating Authorize.Net customer profile - Customer ID: {}, Email: {}, CorrelationId: {}", 
                       customer.getCustomerId(), customer.getEmail(), correlationId);
            
            // Execute the request
            CreateCustomerProfileController controller = new CreateCustomerProfileController(apiRequest);
            controller.execute();
            
            CreateCustomerProfileResponse response = controller.getApiResponse();
            
            if (response != null) {
                if (response.getMessages().getResultCode() == MessageTypeEnum.OK) {
                    String customerProfileId = response.getCustomerProfileId();
                    log.info("✅ SUCCESS: Customer profile created in Authorize.Net - Profile ID: {}, Customer ID: {}, CorrelationId: {}", 
                               customerProfileId, customer.getCustomerId(), correlationId);
                    return customerProfileId;
                } else {
                    String errorMessage = response.getMessages().getMessage().get(0).getText();
                    log.error("❌ FAILED: Customer profile creation - Customer ID: {}, Error: {}, CorrelationId: {}", 
                                customer.getCustomerId(), errorMessage, correlationId);
                    throw new PaymentProcessingException(
                        "Failed to create customer profile: " + errorMessage, correlationId);
                }
            } else {
                log.error("❌ NULL RESPONSE: Authorize.Net customer profile creation - Customer ID: {}, CorrelationId: {}", 
                            customer.getCustomerId(), correlationId);
                throw new PaymentProcessingException(
                    "No response received from Authorize.Net", correlationId);
            }

        } catch (Exception e) {
            log.error("❌ EXCEPTION: Customer profile creation - Customer ID: {}, CorrelationId: {}", 
                        customer.getCustomerId(), correlationId, e);
            throw new PaymentProcessingException(
                "Failed to create customer profile: " + e.getMessage(), correlationId);
        }
    }

    /**
     * Creates a payment profile under an existing customer profile.
     * 
     * @param customerProfileId Authorize.Net customer profile ID
     * @param paymentMethod Payment method details
     * @param customer Customer entity for billing address
     * @return Payment profile ID
     */
    public String createPaymentProfile(String customerProfileId, PaymentMethodRequest paymentMethod, Customer customer) {
        String correlationId = CorrelationIdUtil.getOrGenerate();
        
        log.info("Creating payment profile - Customer Profile ID: {}, Payment Type: {}, CorrelationId: {}", 
                   customerProfileId, paymentMethod.getType(), correlationId);

        try {
            // Set API credentials and environment
            ApiOperationBase.setEnvironment(environment);

            // Create payment profile
            CustomerPaymentProfileType paymentProfile = new CustomerPaymentProfileType();
            
            // Set billing address
            CustomerAddressType billingAddress = new CustomerAddressType();
            billingAddress.setFirstName(customer.getFirstName());
            billingAddress.setLastName(customer.getLastName());
            if (customer.getBillingAddressLine1() != null) {
                billingAddress.setAddress(customer.getBillingAddressLine1());
                billingAddress.setCity(customer.getBillingCity());
                billingAddress.setState(customer.getBillingState());
                billingAddress.setZip(customer.getBillingPostalCode());
                billingAddress.setCountry(customer.getBillingCountry());
            }
            paymentProfile.setBillTo(billingAddress);

            // Set payment method
            PaymentType payment = new PaymentType();
            if ("CREDIT_CARD".equals(paymentMethod.getType())) {
                CreditCardType creditCard = new CreditCardType();
                creditCard.setCardNumber(paymentMethod.getCardNumber());
                creditCard.setExpirationDate(paymentMethod.getExpiryMonth() + paymentMethod.getExpiryYear());
                creditCard.setCardCode(paymentMethod.getCvv());
                payment.setCreditCard(creditCard);
            }
            paymentProfile.setPayment(payment);

            CreateCustomerPaymentProfileRequest apiRequest = new CreateCustomerPaymentProfileRequest();
            apiRequest.setMerchantAuthentication(merchant);
            apiRequest.setCustomerProfileId(customerProfileId);
            apiRequest.setPaymentProfile(paymentProfile);

            CreateCustomerPaymentProfileController controller = new CreateCustomerPaymentProfileController(apiRequest);
            controller.execute();

            CreateCustomerPaymentProfileResponse response = controller.getApiResponse();

            if (response != null && response.getMessages().getResultCode() == MessageTypeEnum.OK) {
                String paymentProfileId = response.getCustomerPaymentProfileId();
                log.info("Successfully created payment profile - Profile ID: {}, Customer Profile ID: {}, CorrelationId: {}", 
                           paymentProfileId, customerProfileId, correlationId);
                return paymentProfileId;
            } else {
                String errorMessage = response != null && response.getMessages().getMessage().size() > 0 
                    ? response.getMessages().getMessage().get(0).getText()
                    : "Unknown error creating payment profile";
                
                log.error("Failed to create payment profile - Customer Profile ID: {}, Error: {}, CorrelationId: {}", 
                           customerProfileId, errorMessage, correlationId);
                
                throw new PaymentProcessingException(
                    "Failed to create payment profile in Authorize.Net: " + errorMessage, correlationId);
            }

        } catch (Exception e) {
            log.error("Exception creating payment profile - Customer Profile ID: {}, CorrelationId: {}", 
                        customerProfileId, correlationId, e);
            throw new PaymentProcessingException(
                "Failed to create payment profile: " + e.getMessage(), correlationId);
        }
    }

    /**
     * Retrieves a customer profile from Authorize.Net.
     * 
     * @param customerProfileId Authorize.Net customer profile ID
     * @return Customer profile information
     */
    public CustomerProfileMaskedType getCustomerProfile(String customerProfileId) {
        String correlationId = CorrelationIdUtil.getOrGenerate();
        
        log.info("Retrieving customer profile - Profile ID: {}, CorrelationId: {}", 
                   customerProfileId, correlationId);

        try {
            ApiOperationBase.setEnvironment(environment);

            GetCustomerProfileRequest apiRequest = new GetCustomerProfileRequest();
            apiRequest.setMerchantAuthentication(merchant);
            apiRequest.setCustomerProfileId(customerProfileId);

            GetCustomerProfileController controller = new GetCustomerProfileController(apiRequest);
            controller.execute();

            GetCustomerProfileResponse response = controller.getApiResponse();

            if (response != null && response.getMessages().getResultCode() == MessageTypeEnum.OK) {
                CustomerProfileMaskedType profile = response.getProfile();
                log.info("Successfully retrieved customer profile - Profile ID: {}, Email: {}, CorrelationId: {}", 
                           customerProfileId, profile.getEmail(), correlationId);
                return profile;
            } else {
                String errorMessage = response != null && response.getMessages().getMessage().size() > 0 
                    ? response.getMessages().getMessage().get(0).getText()
                    : "Unknown error retrieving customer profile";
                
                log.error("Failed to retrieve customer profile - Profile ID: {}, Error: {}, CorrelationId: {}", 
                           customerProfileId, errorMessage, correlationId);
                
                throw new PaymentProcessingException(
                    "Failed to retrieve customer profile: " + errorMessage, correlationId);
            }

        } catch (Exception e) {
            log.error("Exception retrieving customer profile - Profile ID: {}, CorrelationId: {}", 
                        customerProfileId, correlationId, e);
            throw new PaymentProcessingException(
                "Failed to retrieve customer profile: " + e.getMessage(), correlationId);
        }
    }

    /**
     * Retrieves a customer payment profile from Authorize.Net.
     * 
     * @param customerProfileId Authorize.Net customer profile ID
     * @param paymentProfileId Payment profile ID
     * @return Payment profile information
     */
    public CustomerPaymentProfileMaskedType getCustomerPaymentProfile(String customerProfileId, String paymentProfileId) {
        if (customerProfileId == null || customerProfileId.isBlank()) {
            throw new PaymentProcessingException("Failed to retrieve customer payment profile: Customer profile ID is required", "INVALID_PARAMETER");
        }
        if (paymentProfileId == null || paymentProfileId.isBlank()) {
            throw new PaymentProcessingException("Failed to retrieve customer payment profile: Payment profile ID is required", "INVALID_PARAMETER");
        }
        
        String correlationId = CorrelationIdUtil.getOrGenerate();
        
        log.info("Retrieving payment profile - Customer Profile ID: {}, Payment Profile ID: {}, CorrelationId: {}", 
                   customerProfileId, paymentProfileId, correlationId);

        try {
            ApiOperationBase.setEnvironment(environment);

            GetCustomerPaymentProfileRequest apiRequest = new GetCustomerPaymentProfileRequest();
            apiRequest.setMerchantAuthentication(merchant);
            apiRequest.setCustomerProfileId(customerProfileId);
            apiRequest.setCustomerPaymentProfileId(paymentProfileId);

            GetCustomerPaymentProfileController controller = new GetCustomerPaymentProfileController(apiRequest);
            controller.execute();

            GetCustomerPaymentProfileResponse response = controller.getApiResponse();

            if (response != null && response.getMessages().getResultCode() == MessageTypeEnum.OK) {
                CustomerPaymentProfileMaskedType paymentProfile = response.getPaymentProfile();
                log.info("Successfully retrieved payment profile - Profile ID: {}, CorrelationId: {}", 
                           paymentProfileId, correlationId);
                return paymentProfile;
            } else {
                String errorMessage = response != null && response.getMessages().getMessage().size() > 0 
                    ? response.getMessages().getMessage().get(0).getText()
                    : "Unknown error retrieving payment profile";
                
                log.error("Failed to retrieve payment profile - Profile ID: {}, Error: {}, CorrelationId: {}", 
                           paymentProfileId, errorMessage, correlationId);
                
                throw new PaymentProcessingException(
                    "Failed to retrieve payment profile: " + errorMessage, correlationId);
            }

        } catch (Exception e) {
            log.error("Exception retrieving payment profile - Profile ID: {}, CorrelationId: {}", 
                        paymentProfileId, correlationId, e);
            throw new PaymentProcessingException(
                "Failed to retrieve payment profile: " + e.getMessage(), correlationId);
        }
    }

    /**
     * Updates a customer profile in Authorize.Net.
     * 
     * @param customerProfileId Authorize.Net customer profile ID
     * @param customer Updated customer entity
     * @return Success status
     */
    public boolean updateCustomerProfile(String customerProfileId, Customer customer) {
        if (customerProfileId == null || customerProfileId.isBlank()) {
            throw new PaymentProcessingException("Failed to update customer profile: Customer profile ID is required", "INVALID_PARAMETER");
        }
        if (customer == null) {
            throw new PaymentProcessingException("Failed to update customer profile: Customer is required", "INVALID_PARAMETER");
        }
        
        String correlationId = CorrelationIdUtil.getOrGenerate();
        
        log.info("Updating customer profile - Profile ID: {}, Customer ID: {}, CorrelationId: {}", 
                   customerProfileId, customer.getCustomerId(), correlationId);

        try {
            ApiOperationBase.setEnvironment(environment);

            CustomerProfileExType profile = new CustomerProfileExType();
            profile.setCustomerProfileId(customerProfileId);
            profile.setMerchantCustomerId(customer.getCustomerId());
            profile.setEmail(customer.getEmail());
            
            // Set description field - this is what appears as "Name" in the portal
            String customerName = "";
            if (customer.getFirstName() != null && customer.getLastName() != null) {
                customerName = customer.getFirstName() + " " + customer.getLastName();
            } else if (customer.getFirstName() != null) {
                customerName = customer.getFirstName();
            } else if (customer.getLastName() != null) {
                customerName = customer.getLastName();
            } else {
                customerName = "Customer " + customer.getCustomerId();
            }
            profile.setDescription(customerName);

            UpdateCustomerProfileRequest apiRequest = new UpdateCustomerProfileRequest();
            apiRequest.setMerchantAuthentication(merchant);
            apiRequest.setProfile(profile);

            UpdateCustomerProfileController controller = new UpdateCustomerProfileController(apiRequest);
            controller.execute();

            UpdateCustomerProfileResponse response = controller.getApiResponse();

            if (response != null && response.getMessages().getResultCode() == MessageTypeEnum.OK) {
                log.info("Successfully updated customer profile - Profile ID: {}, CorrelationId: {}", 
                           customerProfileId, correlationId);
                return true;
            } else {
                String errorMessage = response != null && response.getMessages().getMessage().size() > 0 
                    ? response.getMessages().getMessage().get(0).getText()
                    : "Unknown error updating customer profile";
                
                log.error("Failed to update customer profile - Profile ID: {}, Error: {}, CorrelationId: {}", 
                           customerProfileId, errorMessage, correlationId);
                
                throw new PaymentProcessingException(
                    "Failed to update customer profile: " + errorMessage, correlationId);
            }

        } catch (Exception e) {
            log.error("Exception updating customer profile - Profile ID: {}, CorrelationId: {}", 
                        customerProfileId, correlationId, e);
            throw new PaymentProcessingException(
                "Failed to update customer profile: " + e.getMessage(), correlationId);
        }
    }

    /**
     * Updates a customer payment profile in Authorize.Net.
     * 
     * @param customerProfileId Authorize.Net customer profile ID
     * @param paymentProfileId Payment profile ID
     * @param paymentMethod Updated payment method details
     * @param customer Customer entity for billing address
     * @return Success status
     */
    public boolean updateCustomerPaymentProfile(String customerProfileId, String paymentProfileId, 
                                              PaymentMethodRequest paymentMethod, Customer customer) {
        String correlationId = CorrelationIdUtil.getOrGenerate();
        
        log.info("Updating payment profile - Customer Profile ID: {}, Payment Profile ID: {}, CorrelationId: {}", 
                   customerProfileId, paymentProfileId, correlationId);

        try {
            ApiOperationBase.setEnvironment(environment);

            CustomerPaymentProfileExType paymentProfile = new CustomerPaymentProfileExType();
            paymentProfile.setCustomerPaymentProfileId(paymentProfileId);
            
            // Set billing address
            CustomerAddressType billingAddress = new CustomerAddressType();
            billingAddress.setFirstName(customer.getFirstName());
            billingAddress.setLastName(customer.getLastName());
            if (customer.getBillingAddressLine1() != null) {
                billingAddress.setAddress(customer.getBillingAddressLine1());
                billingAddress.setCity(customer.getBillingCity());
                billingAddress.setState(customer.getBillingState());
                billingAddress.setZip(customer.getBillingPostalCode());
                billingAddress.setCountry(customer.getBillingCountry());
            }
            paymentProfile.setBillTo(billingAddress);

            // Set payment method
            PaymentType payment = new PaymentType();
            if ("CREDIT_CARD".equals(paymentMethod.getType())) {
                CreditCardType creditCard = new CreditCardType();
                creditCard.setCardNumber(paymentMethod.getCardNumber());
                creditCard.setExpirationDate(paymentMethod.getExpiryMonth() + paymentMethod.getExpiryYear());
                creditCard.setCardCode(paymentMethod.getCvv());
                payment.setCreditCard(creditCard);
            }
            paymentProfile.setPayment(payment);

            UpdateCustomerPaymentProfileRequest apiRequest = new UpdateCustomerPaymentProfileRequest();
            apiRequest.setMerchantAuthentication(merchant);
            apiRequest.setCustomerProfileId(customerProfileId);
            apiRequest.setPaymentProfile(paymentProfile);
            apiRequest.setValidationMode(ValidationModeEnum.TEST_MODE);

            UpdateCustomerPaymentProfileController controller = new UpdateCustomerPaymentProfileController(apiRequest);
            controller.execute();

            UpdateCustomerPaymentProfileResponse response = controller.getApiResponse();

            if (response != null && response.getMessages().getResultCode() == MessageTypeEnum.OK) {
                log.info("Successfully updated payment profile - Profile ID: {}, CorrelationId: {}", 
                           paymentProfileId, correlationId);
                return true;
            } else {
                String errorMessage = response != null && response.getMessages().getMessage().size() > 0 
                    ? response.getMessages().getMessage().get(0).getText()
                    : "Unknown error updating payment profile";
                
                log.error("Failed to update payment profile - Profile ID: {}, Error: {}, CorrelationId: {}", 
                           paymentProfileId, errorMessage, correlationId);
                
                throw new PaymentProcessingException(
                    "Failed to update payment profile: " + errorMessage, correlationId);
            }

        } catch (Exception e) {
            log.error("Exception updating payment profile - Profile ID: {}, CorrelationId: {}", 
                        paymentProfileId, correlationId, e);
            throw new PaymentProcessingException(
                "Failed to update payment profile: " + e.getMessage(), correlationId);
        }
    }

    /**
     * Deletes a customer profile from Authorize.Net.
     * 
     * @param customerProfileId Authorize.Net customer profile ID
     * @return Success status
     */
    public boolean deleteCustomerProfile(String customerProfileId) {
        String correlationId = CorrelationIdUtil.getOrGenerate();
        
        log.info("Deleting customer profile - Profile ID: {}, CorrelationId: {}", 
                   customerProfileId, correlationId);

        try {
            ApiOperationBase.setEnvironment(environment);

            DeleteCustomerProfileRequest apiRequest = new DeleteCustomerProfileRequest();
            apiRequest.setMerchantAuthentication(merchant);
            apiRequest.setCustomerProfileId(customerProfileId);

            DeleteCustomerProfileController controller = new DeleteCustomerProfileController(apiRequest);
            controller.execute();

            DeleteCustomerProfileResponse response = controller.getApiResponse();

            if (response != null && response.getMessages().getResultCode() == MessageTypeEnum.OK) {
                log.info("Successfully deleted customer profile - Profile ID: {}, CorrelationId: {}", 
                           customerProfileId, correlationId);
                return true;
            } else {
                String errorMessage = response != null && response.getMessages().getMessage().size() > 0 
                    ? response.getMessages().getMessage().get(0).getText()
                    : "Unknown error deleting customer profile";
                
                log.error("Failed to delete customer profile - Profile ID: {}, Error: {}, CorrelationId: {}", 
                           customerProfileId, errorMessage, correlationId);
                
                throw new PaymentProcessingException(
                    "Failed to delete customer profile: " + errorMessage, correlationId);
            }

        } catch (Exception e) {
            log.error("Exception deleting customer profile - Profile ID: {}, CorrelationId: {}", 
                        customerProfileId, correlationId, e);
            throw new PaymentProcessingException(
                "Failed to delete customer profile: " + e.getMessage(), correlationId);
        }
    }

    /**
     * Deletes a customer payment profile from Authorize.Net.
     * 
     * @param customerProfileId Authorize.Net customer profile ID
     * @param paymentProfileId Payment profile ID
     * @return Success status
     */
    public boolean deleteCustomerPaymentProfile(String customerProfileId, String paymentProfileId) {
        if (customerProfileId == null || customerProfileId.isBlank()) {
            throw new PaymentProcessingException("Failed to delete customer payment profile: Customer profile ID is required", "INVALID_PARAMETER");
        }
        if (paymentProfileId == null || paymentProfileId.isBlank()) {
            throw new PaymentProcessingException("Failed to delete customer payment profile: Payment profile ID is required", "INVALID_PARAMETER");
        }
        
        String correlationId = CorrelationIdUtil.getOrGenerate();
        
        log.info("Deleting payment profile - Customer Profile ID: {}, Payment Profile ID: {}, CorrelationId: {}", 
                   customerProfileId, paymentProfileId, correlationId);

        try {
            ApiOperationBase.setEnvironment(environment);

            DeleteCustomerPaymentProfileRequest apiRequest = new DeleteCustomerPaymentProfileRequest();
            apiRequest.setMerchantAuthentication(merchant);
            apiRequest.setCustomerProfileId(customerProfileId);
            apiRequest.setCustomerPaymentProfileId(paymentProfileId);

            DeleteCustomerPaymentProfileController controller = new DeleteCustomerPaymentProfileController(apiRequest);
            controller.execute();

            DeleteCustomerPaymentProfileResponse response = controller.getApiResponse();

            if (response != null && response.getMessages().getResultCode() == MessageTypeEnum.OK) {
                log.info("Successfully deleted payment profile - Profile ID: {}, CorrelationId: {}", 
                           paymentProfileId, correlationId);
                return true;
            } else {
                String errorMessage = response != null && response.getMessages().getMessage().size() > 0 
                    ? response.getMessages().getMessage().get(0).getText()
                    : "Unknown error deleting payment profile";
                
                log.error("Failed to delete payment profile - Profile ID: {}, Error: {}, CorrelationId: {}", 
                           paymentProfileId, errorMessage, correlationId);
                
                throw new PaymentProcessingException(
                    "Failed to delete payment profile: " + errorMessage, correlationId);
            }

        } catch (Exception e) {
            log.error("Exception deleting payment profile - Profile ID: {}, CorrelationId: {}", 
                        paymentProfileId, correlationId, e);
            throw new PaymentProcessingException(
                "Failed to delete payment profile: " + e.getMessage(), correlationId);
        }
    }

    /**
     * Validates a customer payment profile in Authorize.Net.
     * 
     * @param customerProfileId Authorize.Net customer profile ID
     * @param paymentProfileId Payment profile ID
     * @return Validation result
     */
    public boolean validateCustomerPaymentProfile(String customerProfileId, String paymentProfileId) {
        if (customerProfileId == null || customerProfileId.isBlank()) {
            throw new PaymentProcessingException("Failed to validate payment profile: Customer profile ID is required", "INVALID_PARAMETER");
        }
        if (paymentProfileId == null || paymentProfileId.isBlank()) {
            throw new PaymentProcessingException("Failed to validate payment profile: Payment profile ID is required", "INVALID_PARAMETER");
        }
        
        String correlationId = CorrelationIdUtil.getOrGenerate();
        
        log.info("Validating payment profile - Customer Profile ID: {}, Payment Profile ID: {}, CorrelationId: {}", 
                   customerProfileId, paymentProfileId, correlationId);

        try {
            ApiOperationBase.setEnvironment(environment);

            ValidateCustomerPaymentProfileRequest apiRequest = new ValidateCustomerPaymentProfileRequest();
            apiRequest.setMerchantAuthentication(merchant);
            apiRequest.setCustomerProfileId(customerProfileId);
            apiRequest.setCustomerPaymentProfileId(paymentProfileId);
            apiRequest.setValidationMode(ValidationModeEnum.TEST_MODE);

            ValidateCustomerPaymentProfileController controller = new ValidateCustomerPaymentProfileController(apiRequest);
            controller.execute();

            ValidateCustomerPaymentProfileResponse response = controller.getApiResponse();

            if (response != null && response.getMessages().getResultCode() == MessageTypeEnum.OK) {
                log.info("Payment profile validation successful - Profile ID: {}, CorrelationId: {}", 
                           paymentProfileId, correlationId);
                return true;
            } else {
                String errorMessage = response != null && response.getMessages().getMessage().size() > 0 
                    ? response.getMessages().getMessage().get(0).getText()
                    : "Unknown validation error";
                
                log.warn("Payment profile validation failed - Profile ID: {}, Error: {}, CorrelationId: {}", 
                           paymentProfileId, errorMessage, correlationId);
                return false;
            }

        } catch (Exception e) {
            log.error("Exception validating payment profile - Profile ID: {}, CorrelationId: {}", 
                        paymentProfileId, correlationId, e);
            return false;
        }
    }
}
