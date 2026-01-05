package com.talentica.paymentgateway.mapper;

import com.talentica.paymentgateway.dto.payment.AddressRequest;
import javax.annotation.processing.Generated;
import net.authorize.api.contract.v1.CustomerAddressType;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-01-05T19:18:43+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.12 (Oracle Corporation)"
)
@Component
public class AuthorizeNetApiMapperImpl implements AuthorizeNetApiMapper {

    @Override
    public CustomerAddressType toCustomerAddress(AddressRequest address) {
        if ( address == null ) {
            return null;
        }

        CustomerAddressType customerAddressType = new CustomerAddressType();

        customerAddressType.setFirstName( address.getFirstName() );
        customerAddressType.setLastName( address.getLastName() );
        customerAddressType.setCompany( address.getCompany() );
        customerAddressType.setAddress( address.getAddress1() );
        customerAddressType.setCity( address.getCity() );
        customerAddressType.setState( address.getState() );
        customerAddressType.setZip( address.getZipCode() );
        customerAddressType.setCountry( address.getCountry() );
        customerAddressType.setPhoneNumber( address.getPhoneNumber() );
        customerAddressType.setEmail( address.getEmail() );

        return customerAddressType;
    }
}
