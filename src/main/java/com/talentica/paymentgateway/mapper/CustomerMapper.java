package com.talentica.paymentgateway.mapper;

import com.talentica.paymentgateway.dto.customer.CustomerProfileRequest;
import com.talentica.paymentgateway.dto.customer.CustomerProfileResponse;
import com.talentica.paymentgateway.entity.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

/**
 * MapStruct mapper for Customer entity to DTO conversions.
 * Handles mapping between Customer domain entities and data transfer objects.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Mapper(componentModel = "spring")
public interface CustomerMapper {

    CustomerMapper INSTANCE = Mappers.getMapper(CustomerMapper.class);

    /**
     * Map Customer entity to CustomerProfileResponse DTO.
     */
    @Mapping(source = "authorizeNetCustomerProfileId", target = "customerProfileId")
    @Mapping(source = "customerReference", target = "merchantCustomerId")
    @Mapping(source = "email", target = "email")
    @Mapping(source = "company", target = "description")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "updatedAt", target = "updatedAt")
    @Mapping(target = "paymentProfiles", ignore = true)
    @Mapping(target = "shippingAddresses", ignore = true)
    CustomerProfileResponse toCustomerProfileResponse(Customer customer);

    /**
     * Map CustomerProfileRequest DTO to Customer entity.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "authorizeNetCustomerProfileId", ignore = true)
    @Mapping(target = "paymentMethods", ignore = true)
    @Mapping(target = "transactions", ignore = true)
    @Mapping(target = "subscriptions", ignore = true)
    @Mapping(target = "orders", ignore = true)
    Customer toCustomer(CustomerProfileRequest request);

    /**
     * Update existing Customer entity from CustomerProfileRequest.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "authorizeNetCustomerProfileId", ignore = true)
    @Mapping(target = "paymentMethods", ignore = true)
    @Mapping(target = "transactions", ignore = true)
    @Mapping(target = "subscriptions", ignore = true)
    @Mapping(target = "orders", ignore = true)
    void updateCustomerFromRequest(CustomerProfileRequest request, @MappingTarget Customer customer);
}
