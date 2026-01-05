package com.talentica.paymentgateway.mapper;

import com.talentica.paymentgateway.dto.user.UpdateUserRequest;
import com.talentica.paymentgateway.dto.user.UserResponse;
import com.talentica.paymentgateway.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

/**
 * MapStruct mapper for User entity to DTO conversions.
 * Handles mapping between User domain entities and data transfer objects.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    /**
     * Map User entity to UserResponse DTO.
     */
    @Mapping(source = "id", target = "id")
    @Mapping(source = "email", target = "email")
    @Mapping(source = "firstName", target = "firstName")
    @Mapping(source = "lastName", target = "lastName")
    @Mapping(source = "isActive", target = "isActive")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(target = "phone", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    UserResponse toUserResponse(User user);

    /**
     * Update existing User entity from UpdateUserRequest.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "isVerified", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "apiKeys", ignore = true)
    @Mapping(target = "customers", ignore = true)
    @Mapping(target = "orders", ignore = true)
    @Mapping(target = "auditLogs", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateUserFromRequest(UpdateUserRequest request, @MappingTarget User user);
}
