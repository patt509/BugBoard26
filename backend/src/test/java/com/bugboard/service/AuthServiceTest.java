package com.bugboard.service;

import com.bugboard.dto.UserDTO;
import com.bugboard.enums.UserRole;
import com.bugboard.model.PasswordResetRequest;
import com.bugboard.model.User;
import com.bugboard.repository.PasswordResetRequestRepository;
import com.bugboard.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the AuthService class.
 * Uses Mockito to mock repository dependencies.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetRequestRepository resetRequestRepository;

    @InjectMocks
    private AuthService authService;

    private User adminUser;
    private User employeeUser;

    @BeforeEach
    void setUp() {
        adminUser = new User("admin@company.com", "hashedPassword", UserRole.ADMIN);
        employeeUser = new User("employee@company.com", "hashedPassword", UserRole.USER);
    }

    // ==================== CREATE USER TESTS ====================

    @Nested
    @DisplayName("Create User Tests")
    class CreateUserTests {

        @Test
        @DisplayName("Admin should successfully create a new user")
        void adminShouldCreateUser() {
            // Arrange
            String newUserEmail = "newuser@company.com";
            when(userRepository.existsByEmail(newUserEmail)).thenReturn(false);

            // Act
            String tempPassword = authService.createUser(newUserEmail, UserRole.USER, adminUser);

            // Assert
            assertNotNull(tempPassword);
            assertEquals(12, tempPassword.length(), "Temporary password should be 12 characters");

            // Verify user was saved
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            User savedUser = userCaptor.getValue();
            assertEquals(newUserEmail, savedUser.getEmail());
            assertEquals(UserRole.USER, savedUser.getRole());
            assertTrue(savedUser.isFirstLogin());
        }

        @Test
        @DisplayName("Non-admin should not be able to create user")
        void nonAdminShouldNotCreateUser() {
            // Act & Assert
            SecurityException exception = assertThrows(
                    SecurityException.class,
                    () -> authService.createUser("new@company.com", UserRole.USER, employeeUser));
            assertEquals("Only administrators can create users.", exception.getMessage());

            // Verify no user was saved
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception for duplicate email")
        void shouldThrowExceptionForDuplicateEmail() {
            // Arrange
            String existingEmail = "existing@company.com";
            when(userRepository.existsByEmail(existingEmail)).thenReturn(true);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> authService.createUser(existingEmail, UserRole.USER, adminUser));
            assertEquals("Email already registered.", exception.getMessage());
        }
    }

    // ==================== LOGIN TESTS ====================

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully with correct credentials")
        void shouldLoginWithCorrectCredentials() {
            // Arrange
            String email = "user@company.com";
            String password = "correctPassword";

            // Create user with a real BCrypt hash
            User user = new User(email, com.bugboard.security.PasswordHasher.hash(password), UserRole.USER);
            user.finalizeProfile("TestUser");

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

            // Act
            Optional<UserDTO> result = authService.login(email, password.toCharArray());

            // Assert
            assertTrue(result.isPresent());
            assertEquals(email, result.get().getEmail());
            assertEquals("TestUser", result.get().getUsername());
        }

        @Test
        @DisplayName("Should fail login with incorrect password")
        void shouldFailLoginWithIncorrectPassword() {
            // Arrange
            String email = "user@company.com";
            User user = new User(email, com.bugboard.security.PasswordHasher.hash("correctPassword"), UserRole.USER);

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

            // Act
            Optional<UserDTO> result = authService.login(email, "wrongPassword".toCharArray());

            // Assert
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should fail login with non-existent email")
        void shouldFailLoginWithNonExistentEmail() {
            // Arrange
            when(userRepository.findByEmail("nonexistent@company.com")).thenReturn(Optional.empty());

            // Act
            Optional<UserDTO> result = authService.login("nonexistent@company.com", "anyPassword".toCharArray());

            // Assert
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should indicate first login in response")
        void shouldIndicateFirstLogin() {
            // Arrange
            String email = "newuser@company.com";
            String password = "tempPassword";
            User user = new User(email, com.bugboard.security.PasswordHasher.hash(password), UserRole.USER);
            // User hasn't finalized profile yet, so isFirstLogin = true

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

            // Act
            Optional<UserDTO> result = authService.login(email, password.toCharArray());

            // Assert
            assertTrue(result.isPresent());
            assertTrue(result.get().isFirstLogin(), "Should indicate it's the first login");
        }
    }

    // ==================== FINALIZE PROFILE TESTS ====================

    @Nested
    @DisplayName("Finalize Profile Tests")
    class FinalizeProfileTests {

        @Test
        @DisplayName("Should finalize profile with unique username")
        void shouldFinalizeProfileWithUniqueUsername() {
            // Arrange
            Long userId = 1L;
            String chosenUsername = "UniqueUser";
            User user = new User("user@company.com", "hash", UserRole.USER);

            when(userRepository.existsByUsername(chosenUsername)).thenReturn(false);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            // Act
            UserDTO result = authService.finalizeProfile(userId, chosenUsername);

            // Assert
            assertEquals(chosenUsername, result.getUsername());
            assertFalse(result.isFirstLogin());
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Should throw exception for taken username")
        void shouldThrowExceptionForTakenUsername() {
            // Arrange
            String takenUsername = "TakenUser";
            when(userRepository.existsByUsername(takenUsername)).thenReturn(true);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> authService.finalizeProfile(1L, takenUsername));
            assertEquals("Username already taken.", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception for non-existent user")
        void shouldThrowExceptionForNonExistentUser() {
            // Arrange
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(
                    IllegalArgumentException.class,
                    () -> authService.finalizeProfile(999L, "SomeUsername"));
        }
    }

    // ==================== PASSWORD RESET TESTS ====================

    @Nested
    @DisplayName("Password Reset Tests")
    class PasswordResetTests {

        @Test
        @DisplayName("Admin should reset user password")
        void adminShouldResetUserPassword() {
            // Arrange
            Long userId = 1L;
            User targetUser = new User("target@company.com", "oldHash", UserRole.USER);

            when(userRepository.findById(userId)).thenReturn(Optional.of(targetUser));

            // Act
            String newTempPassword = authService.resetUserPassword(userId, adminUser);

            // Assert
            assertNotNull(newTempPassword);
            assertEquals(12, newTempPassword.length());
            assertTrue(targetUser.isFirstLogin(), "User should be required to set new username");
            verify(userRepository).save(targetUser);
        }

        @Test
        @DisplayName("Non-admin should not be able to reset password")
        void nonAdminShouldNotResetPassword() {
            // Act & Assert
            SecurityException exception = assertThrows(
                    SecurityException.class,
                    () -> authService.resetUserPassword(1L, employeeUser));
            assertEquals("Only administrators can reset passwords.", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception for non-existent user")
        void shouldThrowExceptionForNonExistentUserReset() {
            // Arrange
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(
                    IllegalArgumentException.class,
                    () -> authService.resetUserPassword(999L, adminUser));
        }
    }

    // ==================== REQUEST PASSWORD RESET TESTS ====================

    @Nested
    @DisplayName("Request Password Reset Tests")
    class RequestPasswordResetTests {

        @Test
        @DisplayName("User should be able to request password reset")
        void userShouldRequestPasswordReset() {
            // Arrange
            String email = "user@company.com";
            User user = new User(email, "hash", UserRole.USER);

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
            when(resetRequestRepository.hasPendingRequest(user)).thenReturn(false);

            // Act
            authService.requestPasswordReset(email);

            // Assert
            ArgumentCaptor<PasswordResetRequest> requestCaptor = ArgumentCaptor.forClass(PasswordResetRequest.class);
            verify(resetRequestRepository).save(requestCaptor.capture());

            PasswordResetRequest savedRequest = requestCaptor.getValue();
            assertEquals(user, savedRequest.getUser());
        }

        @Test
        @DisplayName("Should throw exception if pending request exists")
        void shouldThrowExceptionIfPendingRequestExists() {
            // Arrange
            String email = "user@company.com";
            User user = new User(email, "hash", UserRole.USER);

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
            when(resetRequestRepository.hasPendingRequest(user)).thenReturn(true);

            // Act & Assert
            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> authService.requestPasswordReset(email));
            assertEquals("A password reset request is already pending.", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception for non-existent email")
        void shouldThrowExceptionForNonExistentEmail() {
            // Arrange
            when(userRepository.findByEmail("nonexistent@company.com")).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(
                    IllegalArgumentException.class,
                    () -> authService.requestPasswordReset("nonexistent@company.com"));
        }
    }
}
