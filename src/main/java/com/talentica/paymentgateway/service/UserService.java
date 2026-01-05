package com.talentica.paymentgateway.service;

import com.talentica.paymentgateway.dto.RegistrationRequest;
import com.talentica.paymentgateway.dto.user.UpdateUserRequest;
import com.talentica.paymentgateway.dto.user.UserResponse;
import com.talentica.paymentgateway.entity.User;
import com.talentica.paymentgateway.exception.ResourceNotFoundException;
import com.talentica.paymentgateway.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

/**
 * Service class for user management operations.
 * Handles user registration, authentication, and profile management.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Slf4j
@Service
@Transactional
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        initializeDefaultUsers();
    }

    /**
     * Load user by username for Spring Security authentication.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            log.debug("Loading user by username: {}", username);
            
            User user = userRepository.findByEmailIgnoreCase(username)
                    .orElseThrow(() -> {
                        log.warn("User not found during loadUserByUsername: {}", username);
                        return new UsernameNotFoundException("User not found: " + username);
                    });

            log.debug("Found user: {}, Active: {}, Verified: {}", user.getEmail(), user.getIsActive(), user.getIsVerified());

            if (!user.getIsActive()) {
                log.warn("User account is inactive: {}", username);
                throw new UsernameNotFoundException("User account is inactive: " + username);
            }

            var authorities = getUserAuthorities(user);
            log.debug("User authorities for {}: {}", username, authorities);

            // For now, all users get ROLE_USER. In production, you'd have a roles table
            UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                    .username(user.getEmail())
                    .password(user.getPasswordHash())
                    .authorities(authorities)
                    .accountExpired(false)
                    .accountLocked(!user.getIsActive())
                    .credentialsExpired(false)
                    .disabled(!user.getIsActive())
                    .build();
            
            log.debug("Successfully loaded UserDetails for: {}", username);
            return userDetails;
        } catch (Exception e) {
            log.error("Error loading user by username {}: {}", username, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Register a new user.
     */
    public User registerUser(RegistrationRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        // Check if user already exists
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new IllegalArgumentException("User with email already exists: " + request.getEmail());
        }

        // Create new user
        User user = new User();
        user.setUsername(request.getEmail()); // Use email as username
        user.setEmail(request.getEmail().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setIsActive(true);
        user.setIsVerified(false); // Email verification would be implemented separately

        User savedUser = userRepository.save(user);
        log.info("Successfully registered user: {} with ID: {}", savedUser.getEmail(), savedUser.getId());
        
        return savedUser;
    }

    /**
     * Find user by email.
     */
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email);
    }

    /**
     * Find user by ID.
     */
    @Transactional(readOnly = true)
    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    /**
     * Update user's last login timestamp.
     */
    @Transactional
    public void updateLastLogin(UUID userId) {
        userRepository.updateLastLoginAt(userId, ZonedDateTime.now());
        log.debug("Updated last login for user ID: {}", userId);
    }

    /**
     * Validate user credentials.
     */
    @Transactional(readOnly = true)
    public boolean validateCredentials(String email, String password) {
        try {
            log.debug("Validating credentials for email: {}", email);
            Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
            if (userOpt.isEmpty()) {
                log.warn("User not found for email: {}", email);
                return false;
            }

            User user = userOpt.get();
            log.debug("Found user: {}, Active: {}", user.getEmail(), user.getIsActive());
            
            boolean passwordMatches = passwordEncoder.matches(password, user.getPasswordHash());
            log.debug("Password matches for user {}: {}", email, passwordMatches);
            
            boolean isValid = user.getIsActive() && passwordMatches;
            log.info("Credential validation result for {}: {}", email, isValid);
            
            return isValid;
        } catch (Exception e) {
            log.error("Error validating credentials for email {}: {}", email, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get current user profile.
     */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        User user = userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        
        return convertToUserResponse(user);
    }

    /**
     * Update current user profile.
     */
    @Transactional
    public UserResponse updateCurrentUserProfile(UpdateUserRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        User user = userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        
        updateUserFields(user, request, false); // Don't allow role changes for self-update
        User updatedUser = userRepository.save(user);
        
        log.info("User profile updated: {}", email);
        return convertToUserResponse(updatedUser);
    }

    /**
     * Get user by ID (Admin only).
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        return convertToUserResponse(user);
    }

    /**
     * Update user by ID (Admin only).
     */
    @Transactional
    public UserResponse updateUserById(UUID userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        updateUserFields(user, request, true); // Allow role changes for admin updates
        User updatedUser = userRepository.save(user);
        
        log.info("User updated by admin - ID: {}, Email: {}", userId, user.getEmail());
        return convertToUserResponse(updatedUser);
    }

    /**
     * Update user role (Admin only).
     */
    @Transactional
    public UserResponse updateUserRole(UUID userId, String role) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        String normalizedRole = normalizeRole(role);
        // Note: In a real application, you'd update a roles table
        // For now, we'll store it in a custom field or handle it differently
        
        User updatedUser = userRepository.save(user);
        log.info("User role updated - ID: {}, New Role: {}", userId, normalizedRole);
        
        return convertToUserResponse(updatedUser);
    }

    /**
     * Update user status (Admin only).
     */
    @Transactional
    public UserResponse updateUserStatus(UUID userId, Boolean isActive) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        user.setIsActive(isActive);
        User updatedUser = userRepository.save(user);
        
        log.info("User status updated - ID: {}, Active: {}", userId, isActive);
        return convertToUserResponse(updatedUser);
    }

    /**
     * Get all users with pagination (Admin only).
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);
        return users.map(this::convertToUserResponse);
    }

    /**
     * Search users by email or name (Admin only).
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> searchUsers(String query, Pageable pageable) {
        // Simplified search - would need custom repository method for full search
        Page<User> users = userRepository.findAll(pageable);
        // TODO: Implement proper search functionality
        return users.map(this::convertToUserResponse);
    }

    /**
     * Delete user (Admin only).
     */
    @Transactional
    public void deleteUser(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        userRepository.delete(user);
        log.info("User deleted - ID: {}, Email: {}", userId, user.getEmail());
    }

    /**
     * Convert User entity to UserResponse DTO.
     */
    private UserResponse convertToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setPhone(null); // Phone field not available in current User entity
        response.setRole(getUserRole(user)); // Get role based on email for now
        response.setIsActive(user.getIsActive());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }

    /**
     * Update user fields from request.
     */
    private void updateUserFields(User user, UpdateUserRequest request, boolean allowRoleChange) {
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            // Check if new email already exists
            if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
                throw new IllegalArgumentException("Email already exists: " + request.getEmail());
            }
            user.setEmail(request.getEmail().toLowerCase());
            user.setUsername(request.getEmail().toLowerCase());
        }
        
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        
        // Phone field not available in current User entity
        if (request.getPhone() != null) {
            log.warn("Phone update requested but not supported in current User entity");
        }
        
        if (request.getIsActive() != null && allowRoleChange) {
            user.setIsActive(request.getIsActive());
        }
        
        // Note: Role updates would require a proper roles table in production
        if (request.getRole() != null && allowRoleChange) {
            // For now, we can't store roles directly in the User entity
            // This would require database schema changes
            log.info("Role update requested but not implemented in current schema: {}", request.getRole());
        }
    }

    /**
     * Get user role based on email (temporary implementation).
     */
    private String getUserRole(User user) {
        if (user.getEmail().startsWith("admin@")) {
            return "ROLE_ADMIN";
        } else if (user.getEmail().startsWith("manager@")) {
            return "ROLE_MERCHANT";
        } else {
            return "ROLE_USER";
        }
    }

    /**
     * Normalize role string.
     */
    private String normalizeRole(String role) {
        if (role == null) {
            return "ROLE_USER";
        }
        return role.startsWith("ROLE_") ? role : "ROLE_" + role;
    }

    /**
     * Get user authorities based on user properties.
     * In a real application, this would be based on roles from a roles table.
     */
    private java.util.Collection<SimpleGrantedAuthority> getUserAuthorities(User user) {
        // For demo purposes, assign roles based on email
        if (user.getEmail().startsWith("admin@")) {
            return java.util.Arrays.asList(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ROLE_USER")
            );
        } else if (user.getEmail().startsWith("manager@")) {
            return java.util.Arrays.asList(
                new SimpleGrantedAuthority("ROLE_MERCHANT"),
                new SimpleGrantedAuthority("ROLE_USER")
            );
        } else {
            return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        }
    }

    /**
     * Initialize default users if they don't exist.
     */
    private void initializeDefaultUsers() {
        try {
            log.info("Starting default users initialization...");
            createDefaultUserIfNotExists("admin@paymentgateway.com", "Admin123!", "Admin", "User");
            createDefaultUserIfNotExists("manager@paymentgateway.com", "Manager123!", "Manager", "User");
            createDefaultUserIfNotExists("user@paymentgateway.com", "User123!", "Regular", "User");
            
            log.info("Default users initialization completed successfully");
        } catch (Exception e) {
            log.error("Error during default users initialization: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize default users", e);
        }
    }

    /**
     * Create a default user if it doesn't exist.
     */
    private void createDefaultUserIfNotExists(String email, String password, String firstName, String lastName) {
        try {
            log.debug("Checking if user exists: {}", email);
            if (!userRepository.existsByEmailIgnoreCase(email)) {
                log.info("Creating default user: {}", email);
                User user = new User();
                user.setUsername(email);
                user.setEmail(email.toLowerCase());
                user.setPasswordHash(passwordEncoder.encode(password));
                user.setFirstName(firstName);
                user.setLastName(lastName);
                user.setIsActive(true);
                user.setIsVerified(true);
                
                User savedUser = userRepository.save(user);
                log.info("Successfully created default user: {} with ID: {}", email, savedUser.getId());
            } else {
                log.debug("Default user already exists: {}", email);
            }
        } catch (Exception e) {
            log.error("Error creating default user {}: {}", email, e.getMessage(), e);
            throw new RuntimeException("Failed to create default user: " + email, e);
        }
    }
}
