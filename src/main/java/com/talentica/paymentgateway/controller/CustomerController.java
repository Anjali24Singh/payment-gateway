package com.talentica.paymentgateway.controller;

import com.talentica.paymentgateway.dto.customer.*;
import com.talentica.paymentgateway.dto.payment.PaymentMethodRequest;
import com.talentica.paymentgateway.entity.Customer;
import com.talentica.paymentgateway.service.AuthorizeNetCustomerService;
import com.talentica.paymentgateway.service.CustomerService;
import com.talentica.paymentgateway.util.CorrelationIdUtil;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import net.authorize.api.contract.v1.CustomerPaymentProfileMaskedType;
import net.authorize.api.contract.v1.CustomerProfileMaskedType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Authorize.Net Customer Information Manager (CIM) operations
 */
@Slf4j
@RestController
@RequestMapping("/api/customers")
@CrossOrigin(origins = "*")
public class CustomerController {

    @Autowired
    private AuthorizeNetCustomerService authorizeNetCustomerService;

    @Autowired
    private CustomerService customerService;

    /**
     * Create a new customer profile in Authorize.Net CIM
     */
    @PostMapping("/profiles")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT') or hasRole('USER')")
    public ResponseEntity<Map<String, Object>> createCustomerProfile(
            @Valid @RequestBody CustomerProfileRequest request) {
        
        String correlationId = CorrelationIdUtil.getOrGenerate();
        log.info("Creating customer profile - Customer ID: {}, CorrelationId: {}", 
                   request.getCustomerId(), correlationId);

        try {
            // Create or get existing customer from local database
            Customer customer = customerService.findByCustomerId(request.getCustomerId());
            if (customer == null) {
                customer = new Customer();
                customer.setCustomerReference(request.getCustomerId());
                customer.setEmail(request.getEmail());
                customer.setFirstName(request.getFirstName());
                customer.setLastName(request.getLastName());
                customer.setBillingAddressLine1(request.getBillingAddressLine1());
                customer.setBillingCity(request.getBillingCity());
                customer.setBillingState(request.getBillingState());
                customer.setBillingPostalCode(request.getBillingPostalCode());
                customer.setBillingCountry(request.getBillingCountry());
                customer = customerService.createCustomer(customer);
            }

            // Create customer profile in Authorize.Net
            String customerProfileId = authorizeNetCustomerService.createCustomerProfile(customer, null);
            
            // Update local customer with Authorize.Net profile ID
            customer.setAuthorizeNetCustomerProfileId(customerProfileId);
            customerService.updateCustomer(customer);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("customerProfileId", customerProfileId);
            response.put("customerId", customer.getCustomerId());
            response.put("correlationId", correlationId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to create customer profile - CorrelationId: {}", correlationId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("correlationId", correlationId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get customer profile from Authorize.Net CIM
     */
    @GetMapping("/profiles/{customerProfileId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT') or hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getCustomerProfile(
            @PathVariable String customerProfileId) {
        
        String correlationId = CorrelationIdUtil.getOrGenerate();
        log.info("Retrieving customer profile - Profile ID: {}, CorrelationId: {}", 
                   customerProfileId, correlationId);

        try {
            CustomerProfileMaskedType profile = authorizeNetCustomerService.getCustomerProfile(customerProfileId);
            
            CustomerProfileResponse response = new CustomerProfileResponse();
            response.setCustomerProfileId(profile.getCustomerProfileId());
            response.setMerchantCustomerId(profile.getMerchantCustomerId());
            response.setEmail(profile.getEmail());
            response.setDescription(profile.getDescription());

            // Convert payment profiles
            List<CustomerPaymentProfileResponse> paymentProfiles = new ArrayList<>();
            if (profile.getPaymentProfiles() != null) {
                for (var pp : profile.getPaymentProfiles()) {
                    CustomerPaymentProfileResponse ppResponse = new CustomerPaymentProfileResponse();
                    ppResponse.setCustomerPaymentProfileId(pp.getCustomerPaymentProfileId());
                    ppResponse.setCustomerProfileId(customerProfileId);
                    
                    if (pp.getPayment() != null && pp.getPayment().getCreditCard() != null) {
                        ppResponse.setCardNumber(pp.getPayment().getCreditCard().getCardNumber());
                        ppResponse.setExpirationDate(pp.getPayment().getCreditCard().getExpirationDate());
                        ppResponse.setCardType(pp.getPayment().getCreditCard().getCardType());
                    }
                    
                    paymentProfiles.add(ppResponse);
                }
            }
            response.setPaymentProfiles(paymentProfiles);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("profile", response);
            result.put("correlationId", correlationId);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Failed to retrieve customer profile - CorrelationId: {}", correlationId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("correlationId", correlationId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Update customer profile in Authorize.Net CIM
     */
    @PutMapping("/profiles/{customerProfileId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT') or hasRole('USER')")
    public ResponseEntity<Map<String, Object>> updateCustomerProfile(
            @PathVariable String customerProfileId,
            @Valid @RequestBody CustomerProfileRequest request) {
        
        String correlationId = CorrelationIdUtil.getOrGenerate();
        log.info("Updating customer profile - Profile ID: {}, CorrelationId: {}", 
                   customerProfileId, correlationId);

        try {
            // Get customer from local database
            Customer customer = customerService.findByAuthorizeNetCustomerProfileId(customerProfileId);
            if (customer == null) {
                customer = customerService.findByCustomerId(request.getCustomerId());
                if (customer == null) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("error", "Customer not found");
                    errorResponse.put("correlationId", correlationId);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
                }
            }

            // Update customer data
            customer.setEmail(request.getEmail());
            customer.setFirstName(request.getFirstName());
            customer.setLastName(request.getLastName());
            customer.setBillingAddressLine1(request.getBillingAddressLine1());
            customer.setBillingCity(request.getBillingCity());
            customer.setBillingState(request.getBillingState());
            customer.setBillingPostalCode(request.getBillingPostalCode());
            customer.setBillingCountry(request.getBillingCountry());

            // Update in Authorize.Net
            boolean success = authorizeNetCustomerService.updateCustomerProfile(customerProfileId, customer);
            
            if (success) {
                customerService.updateCustomer(customer);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("customerProfileId", customerProfileId);
            response.put("correlationId", correlationId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to update customer profile - CorrelationId: {}", correlationId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("correlationId", correlationId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Delete customer profile from Authorize.Net CIM
     */
    @DeleteMapping("/profiles/{customerProfileId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    public ResponseEntity<Map<String, Object>> deleteCustomerProfile(
            @PathVariable String customerProfileId) {
        
        String correlationId = CorrelationIdUtil.getOrGenerate();
        log.info("Deleting customer profile - Profile ID: {}, CorrelationId: {}", 
                   customerProfileId, correlationId);

        try {
            boolean success = authorizeNetCustomerService.deleteCustomerProfile(customerProfileId);
            
            // Update local customer to remove Authorize.Net profile ID
            Customer customer = customerService.findByAuthorizeNetCustomerProfileId(customerProfileId);
            if (customer != null) {
                customer.setAuthorizeNetCustomerProfileId(null);
                customerService.updateCustomer(customer);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("customerProfileId", customerProfileId);
            response.put("correlationId", correlationId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to delete customer profile - CorrelationId: {}", correlationId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("correlationId", correlationId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Create payment profile for a customer
     */
    @PostMapping("/profiles/{customerProfileId}/payment-profiles")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT') or hasRole('USER')")
    public ResponseEntity<Map<String, Object>> createPaymentProfile(
            @PathVariable String customerProfileId,
            @Valid @RequestBody PaymentProfileRequest request) {
        
        String correlationId = CorrelationIdUtil.getOrGenerate();
        log.info("Creating payment profile - Customer Profile ID: {}, CorrelationId: {}", 
                   customerProfileId, correlationId);

        try {
            // Get customer from local database
            Customer customer = customerService.findByAuthorizeNetCustomerProfileId(customerProfileId);
            if (customer == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Customer profile not found");
                errorResponse.put("correlationId", correlationId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            // Convert PaymentProfileRequest to PaymentMethodRequest
            PaymentMethodRequest paymentMethod = new PaymentMethodRequest();
            paymentMethod.setType("CREDIT_CARD");
            paymentMethod.setCardNumber(request.getCardNumber());
            paymentMethod.setExpiryMonth(request.getExpiryMonth());
            paymentMethod.setExpiryYear(request.getExpiryYear());
            paymentMethod.setCvv(request.getCvv());

            // Create payment profile in Authorize.Net
            String paymentProfileId = authorizeNetCustomerService.createPaymentProfile(
                customerProfileId, paymentMethod, customer);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("customerProfileId", customerProfileId);
            response.put("paymentProfileId", paymentProfileId);
            response.put("correlationId", correlationId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to create payment profile - CorrelationId: {}", correlationId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("correlationId", correlationId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get payment profile details
     */
    @GetMapping("/profiles/{customerProfileId}/payment-profiles/{paymentProfileId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT') or hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getPaymentProfile(
            @PathVariable String customerProfileId,
            @PathVariable String paymentProfileId) {
        
        String correlationId = CorrelationIdUtil.getOrGenerate();
        log.info("Retrieving payment profile - Customer Profile ID: {}, Payment Profile ID: {}, CorrelationId: {}", 
                   customerProfileId, paymentProfileId, correlationId);

        try {
            CustomerPaymentProfileMaskedType paymentProfile = 
                authorizeNetCustomerService.getCustomerPaymentProfile(customerProfileId, paymentProfileId);

            CustomerPaymentProfileResponse response = new CustomerPaymentProfileResponse();
            response.setCustomerPaymentProfileId(paymentProfile.getCustomerPaymentProfileId());
            response.setCustomerProfileId(customerProfileId);
            
            if (paymentProfile.getPayment() != null && paymentProfile.getPayment().getCreditCard() != null) {
                response.setCardNumber(paymentProfile.getPayment().getCreditCard().getCardNumber());
                response.setExpirationDate(paymentProfile.getPayment().getCreditCard().getExpirationDate());
                response.setCardType(paymentProfile.getPayment().getCreditCard().getCardType());
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("paymentProfile", response);
            result.put("correlationId", correlationId);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Failed to retrieve payment profile - CorrelationId: {}", correlationId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("correlationId", correlationId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Update payment profile
     */
    @PutMapping("/profiles/{customerProfileId}/payment-profiles/{paymentProfileId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT') or hasRole('USER')")
    public ResponseEntity<Map<String, Object>> updatePaymentProfile(
            @PathVariable String customerProfileId,
            @PathVariable String paymentProfileId,
            @Valid @RequestBody PaymentProfileRequest request) {
        
        String correlationId = CorrelationIdUtil.getOrGenerate();
        log.info("Updating payment profile - Customer Profile ID: {}, Payment Profile ID: {}, CorrelationId: {}", 
                   customerProfileId, paymentProfileId, correlationId);

        try {
            // Get customer from local database
            Customer customer = customerService.findByAuthorizeNetCustomerProfileId(customerProfileId);
            if (customer == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Customer profile not found");
                errorResponse.put("correlationId", correlationId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            // Convert PaymentProfileRequest to PaymentMethodRequest
            PaymentMethodRequest paymentMethod = new PaymentMethodRequest();
            paymentMethod.setType("CREDIT_CARD");
            paymentMethod.setCardNumber(request.getCardNumber());
            paymentMethod.setExpiryMonth(request.getExpiryMonth());
            paymentMethod.setExpiryYear(request.getExpiryYear());
            paymentMethod.setCvv(request.getCvv());

            // Update payment profile in Authorize.Net
            boolean success = authorizeNetCustomerService.updateCustomerPaymentProfile(
                customerProfileId, paymentProfileId, paymentMethod, customer);

            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("customerProfileId", customerProfileId);
            response.put("paymentProfileId", paymentProfileId);
            response.put("correlationId", correlationId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to update payment profile - CorrelationId: {}", correlationId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("correlationId", correlationId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Delete payment profile
     */
    @DeleteMapping("/profiles/{customerProfileId}/payment-profiles/{paymentProfileId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    public ResponseEntity<Map<String, Object>> deletePaymentProfile(
            @PathVariable String customerProfileId,
            @PathVariable String paymentProfileId) {
        
        String correlationId = CorrelationIdUtil.getOrGenerate();
        log.info("Deleting payment profile - Customer Profile ID: {}, Payment Profile ID: {}, CorrelationId: {}", 
                   customerProfileId, paymentProfileId, correlationId);

        try {
            boolean success = authorizeNetCustomerService.deleteCustomerPaymentProfile(
                customerProfileId, paymentProfileId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("customerProfileId", customerProfileId);
            response.put("paymentProfileId", paymentProfileId);
            response.put("correlationId", correlationId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to delete payment profile - CorrelationId: {}", correlationId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("correlationId", correlationId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Validate payment profile
     */
    @PostMapping("/profiles/{customerProfileId}/payment-profiles/{paymentProfileId}/validate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT') or hasRole('USER')")
    public ResponseEntity<Map<String, Object>> validatePaymentProfile(
            @PathVariable String customerProfileId,
            @PathVariable String paymentProfileId) {
        
        String correlationId = CorrelationIdUtil.getOrGenerate();
        log.info("Validating payment profile - Customer Profile ID: {}, Payment Profile ID: {}, CorrelationId: {}", 
                   customerProfileId, paymentProfileId, correlationId);

        try {
            boolean isValid = authorizeNetCustomerService.validateCustomerPaymentProfile(
                customerProfileId, paymentProfileId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("valid", isValid);
            response.put("customerProfileId", customerProfileId);
            response.put("paymentProfileId", paymentProfileId);
            response.put("correlationId", correlationId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to validate payment profile - CorrelationId: {}", correlationId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("correlationId", correlationId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
