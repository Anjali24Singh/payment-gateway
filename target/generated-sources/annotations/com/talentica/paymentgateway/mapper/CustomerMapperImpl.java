package com.talentica.paymentgateway.mapper;

import com.talentica.paymentgateway.dto.customer.CustomerProfileRequest;
import com.talentica.paymentgateway.dto.customer.CustomerProfileResponse;
import com.talentica.paymentgateway.entity.Customer;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-01-05T19:18:44+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.12 (Oracle Corporation)"
)
@Component
public class CustomerMapperImpl implements CustomerMapper {

    @Override
    public CustomerProfileResponse toCustomerProfileResponse(Customer customer) {
        if ( customer == null ) {
            return null;
        }

        CustomerProfileResponse.CustomerProfileResponseBuilder customerProfileResponse = CustomerProfileResponse.builder();

        customerProfileResponse.customerProfileId( customer.getAuthorizeNetCustomerProfileId() );
        customerProfileResponse.merchantCustomerId( customer.getCustomerReference() );
        customerProfileResponse.email( customer.getEmail() );
        customerProfileResponse.description( customer.getCompany() );
        customerProfileResponse.createdAt( customer.getCreatedAt() );
        customerProfileResponse.updatedAt( customer.getUpdatedAt() );

        return customerProfileResponse.build();
    }

    @Override
    public Customer toCustomer(CustomerProfileRequest request) {
        if ( request == null ) {
            return null;
        }

        Customer customer = new Customer();

        customer.setEmail( request.getEmail() );
        customer.setFirstName( request.getFirstName() );
        customer.setLastName( request.getLastName() );
        customer.setBillingAddressLine1( request.getBillingAddressLine1() );
        customer.setBillingAddressLine2( request.getBillingAddressLine2() );
        customer.setBillingCity( request.getBillingCity() );
        customer.setBillingState( request.getBillingState() );
        customer.setBillingPostalCode( request.getBillingPostalCode() );
        customer.setBillingCountry( request.getBillingCountry() );

        return customer;
    }

    @Override
    public void updateCustomerFromRequest(CustomerProfileRequest request, Customer customer) {
        if ( request == null ) {
            return;
        }

        customer.setEmail( request.getEmail() );
        customer.setFirstName( request.getFirstName() );
        customer.setLastName( request.getLastName() );
        customer.setBillingAddressLine1( request.getBillingAddressLine1() );
        customer.setBillingAddressLine2( request.getBillingAddressLine2() );
        customer.setBillingCity( request.getBillingCity() );
        customer.setBillingState( request.getBillingState() );
        customer.setBillingPostalCode( request.getBillingPostalCode() );
        customer.setBillingCountry( request.getBillingCountry() );
    }
}
