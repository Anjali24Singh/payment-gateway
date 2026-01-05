package com.talentica.paymentgateway.service;

import com.talentica.paymentgateway.dto.RegistrationRequest;
import com.talentica.paymentgateway.dto.user.UpdateUserRequest;
import com.talentica.paymentgateway.dto.user.UserResponse;
import com.talentica.paymentgateway.entity.User;
import com.talentica.paymentgateway.exception.ResourceNotFoundException;
import com.talentica.paymentgateway.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private UserService userService;

    private User testUser;
    private RegistrationRequest registrationRequest;
    private UpdateUserRequest updateUserRequest;

    @BeforeEach
    void setUp() {
        // Mock the initialization calls that happen in constructor
        when(userRepository.existsByEmailIgnoreCase("admin@paymentgateway.com")).thenReturn(true);
        when(userRepository.existsByEmailIgnoreCase("manager@paymentgateway.com")).thenReturn(true);
        when(userRepository.existsByEmailIgnoreCase("user@paymentgateway.com")).thenReturn(true);
        
        userService = new UserService(userRepository, passwordEncoder);
        
        // Reset mock interactions after service initialization
        reset(userRepository);
        
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("test@example.com");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("hashedPassword");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setIsActive(true);
        testUser.setIsVerified(false);
        testUser.setCreatedAt(ZonedDateTime.now().toLocalDateTime());
        testUser.setUpdatedAt(ZonedDateTime.now().toLocalDateTime());

        registrationRequest = new RegistrationRequest();
        registrationRequest.setEmail("newuser@example.com");
        registrationRequest.setPassword("password123");
        registrationRequest.setFirstName("New");
        registrationRequest.setLastName("User");

        updateUserRequest = new UpdateUserRequest();
        updateUserRequest.setFirstName("Updated");
        updateUserRequest.setLastName("Name");
        updateUserRequest.setEmail("updated@example.com");
    }

    @Test
    void loadUserByUsername_WithValidEmail_ShouldReturnUserDetails() {
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(testUser));

        UserDetails userDetails = userService.loadUserByUsername("test@example.com");

        assertNotNull(userDetails);
        assertEquals("test@example.com", userDetails.getUsername());
        assertEquals("hashedPassword", userDetails.getPassword());
        assertTrue(userDetails.isEnabled());
        verify(userRepository).findByEmailIgnoreCase("test@example.com");
    }

    @Test
    void loadUserByUsername_WithNonExistentEmail_ShouldThrowUsernameNotFoundException() {
        when(userRepository.findByEmailIgnoreCase("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> {
            userService.loadUserByUsername("nonexistent@example.com");
        });

        verify(userRepository).findByEmailIgnoreCase("nonexistent@example.com");
    }

    @Test
    void loadUserByUsername_WithInactiveUser_ShouldThrowUsernameNotFoundException() {
        testUser.setIsActive(false);
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(testUser));

        assertThrows(UsernameNotFoundException.class, () -> {
            userService.loadUserByUsername("test@example.com");
        });

        verify(userRepository).findByEmailIgnoreCase("test@example.com");
    }

    @Test
    void loadUserByUsername_WithAdminEmail_ShouldReturnAdminAuthorities() {
        testUser.setEmail("admin@example.com");
        testUser.setUsername("admin@example.com");
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(testUser));

        UserDetails userDetails = userService.loadUserByUsername("admin@example.com");

        assertNotNull(userDetails);
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")));
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_USER")));
        verify(userRepository).findByEmailIgnoreCase("admin@example.com");
    }

    @Test
    void loadUserByUsername_WithManagerEmail_ShouldReturnMerchantAuthorities() {
        testUser.setEmail("manager@example.com");
        testUser.setUsername("manager@example.com");
        when(userRepository.findByEmailIgnoreCase("manager@example.com")).thenReturn(Optional.of(testUser));

        UserDetails userDetails = userService.loadUserByUsername("manager@example.com");

        assertNotNull(userDetails);
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_MERCHANT")));
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_USER")));
        verify(userRepository).findByEmailIgnoreCase("manager@example.com");
    }

    @Test
    void registerUser_WithValidRequest_ShouldCreateUser() {
        when(userRepository.existsByEmailIgnoreCase("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userService.registerUser(registrationRequest);

        assertNotNull(result);
        verify(userRepository).existsByEmailIgnoreCase("newuser@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_WithExistingEmail_ShouldThrowException() {
        when(userRepository.existsByEmailIgnoreCase("newuser@example.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser(registrationRequest);
        });

        verify(userRepository).existsByEmailIgnoreCase("newuser@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void findByEmail_WithValidEmail_ShouldReturnUser() {
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(testUser));

        Optional<User> result = userService.findByEmail("test@example.com");

        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
        verify(userRepository).findByEmailIgnoreCase("test@example.com");
    }

    @Test
    void findByEmail_WithNonExistentEmail_ShouldReturnEmpty() {
        when(userRepository.findByEmailIgnoreCase("nonexistent@example.com")).thenReturn(Optional.empty());

        Optional<User> result = userService.findByEmail("nonexistent@example.com");

        assertFalse(result.isPresent());
        verify(userRepository).findByEmailIgnoreCase("nonexistent@example.com");
    }

    @Test
    void findById_WithValidId_ShouldReturnUser() {
        UUID userId = testUser.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        Optional<User> result = userService.findById(userId);

        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
        verify(userRepository).findById(userId);
    }

    @Test
    void findById_WithNonExistentId_ShouldReturnEmpty() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        Optional<User> result = userService.findById(userId);

        assertFalse(result.isPresent());
        verify(userRepository).findById(userId);
    }

    @Test
    void updateLastLogin_WithValidUserId_ShouldUpdateTimestamp() {
        UUID userId = testUser.getId();

        userService.updateLastLogin(userId);

        verify(userRepository).updateLastLoginAt(eq(userId), any(ZonedDateTime.class));
    }

    @Test
    void validateCredentials_WithValidCredentials_ShouldReturnTrue() {
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);

        boolean result = userService.validateCredentials("test@example.com", "password123");

        assertTrue(result);
        verify(userRepository).findByEmailIgnoreCase("test@example.com");
        verify(passwordEncoder).matches("password123", "hashedPassword");
    }

    @Test
    void validateCredentials_WithInvalidPassword_ShouldReturnFalse() {
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", "hashedPassword")).thenReturn(false);

        boolean result = userService.validateCredentials("test@example.com", "wrongpassword");

        assertFalse(result);
        verify(userRepository).findByEmailIgnoreCase("test@example.com");
        verify(passwordEncoder).matches("wrongpassword", "hashedPassword");
    }

    @Test
    void validateCredentials_WithNonExistentUser_ShouldReturnFalse() {
        when(userRepository.findByEmailIgnoreCase("nonexistent@example.com")).thenReturn(Optional.empty());

        boolean result = userService.validateCredentials("nonexistent@example.com", "password123");

        assertFalse(result);
        verify(userRepository).findByEmailIgnoreCase("nonexistent@example.com");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void validateCredentials_WithInactiveUser_ShouldReturnFalse() {
        testUser.setIsActive(false);
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);

        boolean result = userService.validateCredentials("test@example.com", "password123");

        assertFalse(result);
        verify(userRepository).findByEmailIgnoreCase("test@example.com");
        verify(passwordEncoder).matches("password123", "hashedPassword");
    }

    @Test
    void validateCredentials_WithException_ShouldReturnFalse() {
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenThrow(new RuntimeException("Database error"));

        boolean result = userService.validateCredentials("test@example.com", "password123");

        assertFalse(result);
        verify(userRepository).findByEmailIgnoreCase("test@example.com");
    }

    @Test
    void getCurrentUserProfile_WithValidUser_ShouldReturnUserResponse() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("test@example.com");
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(testUser));

        UserResponse result = userService.getCurrentUserProfile();

        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        assertEquals(testUser.getEmail(), result.getEmail());
        assertEquals(testUser.getFirstName(), result.getFirstName());
        assertEquals(testUser.getLastName(), result.getLastName());
        verify(userRepository).findByEmailIgnoreCase("test@example.com");
    }

    @Test
    void getCurrentUserProfile_WithNonExistentUser_ShouldThrowException() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("nonexistent@example.com");
        when(userRepository.findByEmailIgnoreCase("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            userService.getCurrentUserProfile();
        });

        verify(userRepository).findByEmailIgnoreCase("nonexistent@example.com");
    }

    @Test
    void updateCurrentUserProfile_WithValidRequest_ShouldUpdateUser() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("test@example.com");
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmailIgnoreCase("updated@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserResponse result = userService.updateCurrentUserProfile(updateUserRequest);

        assertNotNull(result);
        verify(userRepository).findByEmailIgnoreCase("test@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateCurrentUserProfile_WithExistingEmail_ShouldThrowException() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("test@example.com");
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmailIgnoreCase("updated@example.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> {
            userService.updateCurrentUserProfile(updateUserRequest);
        });

        verify(userRepository).findByEmailIgnoreCase("test@example.com");
        verify(userRepository).existsByEmailIgnoreCase("updated@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getUserById_WithValidId_ShouldReturnUserResponse() {
        UUID userId = testUser.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        UserResponse result = userService.getUserById(userId);

        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        assertEquals(testUser.getEmail(), result.getEmail());
        verify(userRepository).findById(userId);
    }

    @Test
    void getUserById_WithNonExistentId_ShouldThrowException() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            userService.getUserById(userId);
        });

        verify(userRepository).findById(userId);
    }

    @Test
    void updateUserById_WithValidRequest_ShouldUpdateUser() {
        UUID userId = testUser.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmailIgnoreCase("updated@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserResponse result = userService.updateUserById(userId, updateUserRequest);

        assertNotNull(result);
        verify(userRepository).findById(userId);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUserById_WithNonExistentUser_ShouldThrowException() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            userService.updateUserById(userId, updateUserRequest);
        });

        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUserRole_WithValidUserId_ShouldUpdateRole() {
        UUID userId = testUser.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserResponse result = userService.updateUserRole(userId, "ADMIN");

        assertNotNull(result);
        verify(userRepository).findById(userId);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUserRole_WithNonExistentUser_ShouldThrowException() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            userService.updateUserRole(userId, "ADMIN");
        });

        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUserStatus_WithValidUserId_ShouldUpdateStatus() {
        UUID userId = testUser.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserResponse result = userService.updateUserStatus(userId, false);

        assertNotNull(result);
        verify(userRepository).findById(userId);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUserStatus_WithNonExistentUser_ShouldThrowException() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            userService.updateUserStatus(userId, false);
        });

        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getAllUsers_WithValidPageable_ShouldReturnPageOfUsers() {
        List<User> users = Arrays.asList(testUser);
        Page<User> userPage = new PageImpl<>(users);
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findAll(pageable)).thenReturn(userPage);

        Page<UserResponse> result = userService.getAllUsers(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testUser.getId(), result.getContent().get(0).getId());
        verify(userRepository).findAll(pageable);
    }

    @Test
    void searchUsers_WithValidQuery_ShouldReturnPageOfUsers() {
        List<User> users = Arrays.asList(testUser);
        Page<User> userPage = new PageImpl<>(users);
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findAll(pageable)).thenReturn(userPage);

        Page<UserResponse> result = userService.searchUsers("test", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(userRepository).findAll(pageable);
    }

    @Test
    void deleteUser_WithValidUserId_ShouldDeleteUser() {
        UUID userId = testUser.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        userService.deleteUser(userId);

        verify(userRepository).findById(userId);
        verify(userRepository).delete(testUser);
    }

    @Test
    void deleteUser_WithNonExistentUser_ShouldThrowException() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            userService.deleteUser(userId);
        });

        verify(userRepository).findById(userId);
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void convertToUserResponse_WithAdminUser_ShouldReturnCorrectRole() {
        testUser.setEmail("admin@example.com");
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

        UserResponse result = userService.getUserById(testUser.getId());

        assertEquals("ROLE_ADMIN", result.getRole());
    }

    @Test
    void convertToUserResponse_WithManagerUser_ShouldReturnCorrectRole() {
        testUser.setEmail("manager@example.com");
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

        UserResponse result = userService.getUserById(testUser.getId());

        assertEquals("ROLE_MERCHANT", result.getRole());
    }

    @Test
    void convertToUserResponse_WithRegularUser_ShouldReturnCorrectRole() {
        testUser.setEmail("user@example.com");
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

        UserResponse result = userService.getUserById(testUser.getId());

        assertEquals("ROLE_USER", result.getRole());
    }

    @Test
    void updateUserFields_WithNullEmail_ShouldNotUpdateEmail() {
        updateUserRequest.setEmail(null);
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.updateUserById(testUser.getId(), updateUserRequest);

        assertEquals("test@example.com", testUser.getEmail());
        verify(userRepository, never()).existsByEmailIgnoreCase(anyString());
    }

    @Test
    void updateUserFields_WithSameEmail_ShouldNotCheckExistence() {
        updateUserRequest.setEmail("test@example.com");
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.updateUserById(testUser.getId(), updateUserRequest);

        verify(userRepository, never()).existsByEmailIgnoreCase(anyString());
    }

    @Test
    void updateUserFields_WithPhoneUpdate_ShouldLogWarning() {
        updateUserRequest.setPhone("1234567890");
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.updateUserById(testUser.getId(), updateUserRequest);

        verify(userRepository).save(any(User.class));
    }
}
