package com.talentica.paymentgateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentica.paymentgateway.dto.user.UpdateUserRequest;
import com.talentica.paymentgateway.dto.user.UserResponse;
import com.talentica.paymentgateway.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for UserController.
 * Tests all REST endpoints with proper security context and validation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private UserResponse userResponse;
    private UpdateUserRequest updateUserRequest;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        
        userResponse = new UserResponse();
        userResponse.setId(userId);
        userResponse.setEmail("test@example.com");
        userResponse.setFirstName("John");
        userResponse.setLastName("Doe");
        userResponse.setRole("USER");
        userResponse.setIsActive(true);
        userResponse.setCreatedAt(LocalDateTime.now());
        userResponse.setUpdatedAt(LocalDateTime.now());

        updateUserRequest = new UpdateUserRequest();
        updateUserRequest.setFirstName("John Updated");
        updateUserRequest.setLastName("Doe Updated");
        updateUserRequest.setPhone("1234567890");
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetCurrentUserProfile_WithUserRole_ReturnsUserProfile() throws Exception {
        // Given
        when(userService.getCurrentUserProfile()).thenReturn(userResponse);

        // When & Then
        mockMvc.perform(get("/users/profile"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.first_name").value("John"))
                .andExpect(jsonPath("$.last_name").value("Doe"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.is_active").value(true));

        verify(userService).getCurrentUserProfile();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetCurrentUserProfile_WithAdminRole_ReturnsUserProfile() throws Exception {
        // Given
        when(userService.getCurrentUserProfile()).thenReturn(userResponse);

        // When & Then
        mockMvc.perform(get("/users/profile"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.email").value("test@example.com"));

        verify(userService).getCurrentUserProfile();
    }

    @Test
    @WithMockUser(roles = "MERCHANT")
    void testGetCurrentUserProfile_WithMerchantRole_ReturnsUserProfile() throws Exception {
        // Given
        when(userService.getCurrentUserProfile()).thenReturn(userResponse);

        // When & Then
        mockMvc.perform(get("/users/profile"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        verify(userService).getCurrentUserProfile();
    }

    @Test
    void testGetCurrentUserProfile_WithoutAuthentication_ReturnsUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/users/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testUpdateCurrentUserProfile_WithValidRequest_ReturnsUpdatedUser() throws Exception {
        // Given
        UserResponse updatedResponse = new UserResponse();
        updatedResponse.setId(userId);
        updatedResponse.setEmail("test@example.com");
        updatedResponse.setFirstName("John Updated");
        updatedResponse.setLastName("Doe Updated");
        updatedResponse.setRole("USER");
        updatedResponse.setIsActive(true);

        when(userService.updateCurrentUserProfile(any(UpdateUserRequest.class))).thenReturn(updatedResponse);

        // When & Then
        mockMvc.perform(put("/users/profile")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateUserRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.first_name").value("John Updated"))
                .andExpect(jsonPath("$.last_name").value("Doe Updated"));

        verify(userService).updateCurrentUserProfile(any(UpdateUserRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetUserById_WithAdminRole_ReturnsUser() throws Exception {
        // Given
        when(userService.getUserById(userId)).thenReturn(userResponse);

        // When & Then
        mockMvc.perform(get("/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("test@example.com"));

        verify(userService).getUserById(userId);
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetUserById_WithUserRole_ReturnsForbidden() throws Exception {
        // When & Then
        mockMvc.perform(get("/users/{userId}", userId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateUserById_WithAdminRole_ReturnsUpdatedUser() throws Exception {
        // Given
        UserResponse updatedResponse = new UserResponse();
        updatedResponse.setId(userId);
        updatedResponse.setFirstName("John Updated");
        updatedResponse.setLastName("Doe Updated");

        when(userService.updateUserById(eq(userId), any(UpdateUserRequest.class))).thenReturn(updatedResponse);

        // When & Then
        mockMvc.perform(put("/users/{userId}", userId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateUserRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.first_name").value("John Updated"));

        verify(userService).updateUserById(eq(userId), any(UpdateUserRequest.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testUpdateUserById_WithUserRole_ReturnsForbidden() throws Exception {
        // When & Then
        mockMvc.perform(put("/users/{userId}", userId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateUserRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateUserRole_WithAdminRole_ReturnsUpdatedUser() throws Exception {
        // Given
        UserResponse updatedResponse = new UserResponse();
        updatedResponse.setId(userId);
        updatedResponse.setRole("MERCHANT");

        when(userService.updateUserRole(userId, "MERCHANT")).thenReturn(updatedResponse);

        // When & Then
        mockMvc.perform(put("/users/{userId}/role", userId)
                .with(csrf())
                .param("role", "MERCHANT"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.role").value("MERCHANT"));

        verify(userService).updateUserRole(userId, "MERCHANT");
    }

    @Test
    @WithMockUser(roles = "USER")
    void testUpdateUserRole_WithUserRole_ReturnsForbidden() throws Exception {
        // When & Then
        mockMvc.perform(put("/users/{userId}/role", userId)
                .with(csrf())
                .param("role", "MERCHANT"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateUserStatus_WithAdminRole_ReturnsUpdatedUser() throws Exception {
        // Given
        UserResponse updatedResponse = new UserResponse();
        updatedResponse.setId(userId);
        updatedResponse.setIsActive(false);

        when(userService.updateUserStatus(userId, false)).thenReturn(updatedResponse);

        // When & Then
        mockMvc.perform(put("/users/{userId}/status", userId)
                .with(csrf())
                .param("isActive", "false"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.is_active").value(false));

        verify(userService).updateUserStatus(userId, false);
    }

    @Test
    @WithMockUser(roles = "USER")
    void testUpdateUserStatus_WithUserRole_ReturnsForbidden() throws Exception {
        // When & Then
        mockMvc.perform(put("/users/{userId}/status", userId)
                .with(csrf())
                .param("isActive", "false"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetAllUsers_WithAdminRole_ReturnsPagedUsers() throws Exception {
        // Given
        Page<UserResponse> userPage = new PageImpl<>(Arrays.asList(userResponse), PageRequest.of(0, 10), 1);
        when(userService.getAllUsers(any(Pageable.class))).thenReturn(userPage);

        // When & Then
        mockMvc.perform(get("/users")
                .param("page", "0")
                .param("size", "10")
                .param("sortBy", "createdAt")
                .param("sortDir", "desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].email").value("test@example.com"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.size").value(10));

        verify(userService).getAllUsers(any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetAllUsers_WithUserRole_ReturnsForbidden() throws Exception {
        // When & Then
        mockMvc.perform(get("/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testSearchUsers_WithAdminRole_ReturnsSearchResults() throws Exception {
        // Given
        Page<UserResponse> userPage = new PageImpl<>(Arrays.asList(userResponse), PageRequest.of(0, 10), 1);
        when(userService.searchUsers(eq("john"), any(Pageable.class))).thenReturn(userPage);

        // When & Then
        mockMvc.perform(get("/users/search")
                .param("query", "john")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].first_name").value("John"));

        verify(userService).searchUsers(eq("john"), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testSearchUsers_WithUserRole_ReturnsForbidden() throws Exception {
        // When & Then
        mockMvc.perform(get("/users/search")
                .param("query", "john"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDeleteUser_WithAdminRole_ReturnsNoContent() throws Exception {
        // When & Then
        mockMvc.perform(delete("/users/{userId}", userId)
                .with(csrf()))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(userId);
    }

    @Test
    @WithMockUser(roles = "USER")
    void testDeleteUser_WithUserRole_ReturnsForbidden() throws Exception {
        // When & Then
        mockMvc.perform(delete("/users/{userId}", userId)
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetAllUsers_WithDefaultParameters_ReturnsPagedUsers() throws Exception {
        // Given
        Page<UserResponse> userPage = new PageImpl<>(Arrays.asList(userResponse), PageRequest.of(0, 10), 1);
        when(userService.getAllUsers(any(Pageable.class))).thenReturn(userPage);

        // When & Then
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray());

        verify(userService).getAllUsers(any(Pageable.class));
    }
}
