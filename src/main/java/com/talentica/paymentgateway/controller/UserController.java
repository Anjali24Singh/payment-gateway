package com.talentica.paymentgateway.controller;

import com.talentica.paymentgateway.dto.user.UpdateUserRequest;
import com.talentica.paymentgateway.dto.user.UserResponse;
import com.talentica.paymentgateway.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for user management operations.
 * Provides endpoints for CRUD operations on user accounts.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/users")
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Get current user profile information.
     * 
     * @return UserResponse with current user details
     */
    @GetMapping("/profile")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MERCHANT')")
    public ResponseEntity<UserResponse> getCurrentUserProfile() {
        log.info("Getting current user profile");
        UserResponse userResponse = userService.getCurrentUserProfile();
        return ResponseEntity.ok(userResponse);
    }

    /**
     * Update current user profile.
     * 
     * @param updateRequest the update request
     * @return updated UserResponse
     */
    @PutMapping("/profile")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MERCHANT')")
    public ResponseEntity<UserResponse> updateCurrentUserProfile(@Valid @RequestBody UpdateUserRequest updateRequest) {
        log.info("Updating current user profile: {}", updateRequest);
        UserResponse updatedUser = userService.updateCurrentUserProfile(updateRequest);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Get user by ID (Admin only).
     * 
     * @param userId the user ID
     * @return UserResponse with user details
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID userId) {
        log.info("Getting user by ID: {}", userId);
        UserResponse userResponse = userService.getUserById(userId);
        return ResponseEntity.ok(userResponse);
    }

    /**
     * Update user by ID (Admin only).
     * 
     * @param userId the user ID
     * @param updateRequest the update request
     * @return updated UserResponse
     */
    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUserById(@PathVariable UUID userId, 
                                                      @Valid @RequestBody UpdateUserRequest updateRequest) {
        log.info("Updating user {} with data: {}", userId, updateRequest);
        UserResponse updatedUser = userService.updateUserById(userId, updateRequest);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Update user role (Admin only).
     * 
     * @param userId the user ID
     * @param role the new role
     * @return updated UserResponse
     */
    @PutMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUserRole(@PathVariable UUID userId, 
                                                      @RequestParam String role) {
        log.info("Updating user {} role to: {}", userId, role);
        UserResponse updatedUser = userService.updateUserRole(userId, role);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Activate/Deactivate user (Admin only).
     * 
     * @param userId the user ID
     * @param isActive the active status
     * @return updated UserResponse
     */
    @PutMapping("/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUserStatus(@PathVariable UUID userId, 
                                                        @RequestParam Boolean isActive) {
        log.info("Updating user {} status to: {}", userId, isActive);
        UserResponse updatedUser = userService.updateUserStatus(userId, isActive);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Get all users with pagination (Admin only).
     * 
     * @param page page number (0-based)
     * @param size page size
     * @param sortBy field to sort by
     * @param sortDir sort direction (asc/desc)
     * @return paginated list of users
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        log.info("Getting all users - page: {}, size: {}, sortBy: {}, sortDir: {}", 
                   page, size, sortBy, sortDir);
        
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? 
                                  Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<UserResponse> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(users);
    }

    /**
     * Search users by email or name (Admin only).
     * 
     * @param query search query
     * @param page page number (0-based)
     * @param size page size
     * @return paginated search results
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponse>> searchUsers(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        log.info("Searching users with query: '{}' - page: {}, size: {}", query, page, size);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<UserResponse> users = userService.searchUsers(query, pageable);
        return ResponseEntity.ok(users);
    }

    /**
     * Delete user (Admin only).
     * 
     * @param userId the user ID
     * @return success response
     */
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        log.info("Deleting user: {}", userId);
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
