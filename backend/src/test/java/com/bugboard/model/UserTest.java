package com.bugboard.model;

import com.bugboard.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the User model class.
 * Tests cover constructor validation, profile finalization, and admin checks.
 */
@DisplayName("User Model Tests")
class UserTest {

    // ==================== CONSTRUCTOR TESTS ====================

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create user with valid email, password and role")
        void shouldCreateUserWithValidData() {
            // Arrange & Act
            User user = new User("test@example.com", "hashedPassword123", UserRole.USER);

            // Assert
            assertEquals("test@example.com", user.getEmail());
            assertEquals("hashedPassword123", user.getPassword());
            assertEquals(UserRole.USER, user.getRole());
            assertTrue(user.isFirstLogin(), "New user should have firstLogin = true");
            assertNull(user.getUsername(), "Username should be null until profile is finalized");
            assertNotNull(user.getCreatedAt(), "CreatedAt should be automatically set");
        }

        @Test
        @DisplayName("Should throw exception for null email")
        void shouldThrowExceptionForNullEmail() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new User(null, "password", UserRole.USER));
            assertEquals("Invalid email format.", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception for email without @")
        void shouldThrowExceptionForInvalidEmail() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new User("invalidemail.com", "password", UserRole.USER));
            assertEquals("Invalid email format.", exception.getMessage());
        }

        @Test
        @DisplayName("Should create admin user correctly")
        void shouldCreateAdminUser() {
            // Arrange & Act
            User admin = new User("admin@company.com", "adminPass", UserRole.ADMIN);

            // Assert
            assertEquals(UserRole.ADMIN, admin.getRole());
            assertTrue(admin.isAdmin(), "Admin user should return true for isAdmin()");
        }
    }

    // ==================== PROFILE FINALIZATION TESTS ====================

    @Nested
    @DisplayName("Profile Finalization Tests")
    class ProfileFinalizationTests {

        @Test
        @DisplayName("Should finalize profile with valid username")
        void shouldFinalizeProfileWithValidUsername() {
            // Arrange
            User user = new User("test@example.com", "password", UserRole.USER);

            // Act
            user.finalizeProfile("JohnDoe");

            // Assert
            assertEquals("JohnDoe", user.getUsername());
            assertFalse(user.isFirstLogin(), "FirstLogin should be false after profile finalization");
        }

        @Test
        @DisplayName("Should throw exception when finalizing already finalized profile")
        void shouldThrowExceptionWhenFinalizingTwice() {
            // Arrange
            User user = new User("test@example.com", "password", UserRole.USER);
            user.finalizeProfile("JohnDoe");

            // Act & Assert
            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> user.finalizeProfile("NewUsername"));
            assertEquals("Profile has already been finalized.", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception for username shorter than 3 characters")
        void shouldThrowExceptionForShortUsername() {
            // Arrange
            User user = new User("test@example.com", "password", UserRole.USER);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> user.finalizeProfile("AB"));
            assertEquals("Username must be at least 3 characters.", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception for null username")
        void shouldThrowExceptionForNullUsername() {
            // Arrange
            User user = new User("test@example.com", "password", UserRole.USER);

            // Act & Assert
            assertThrows(
                    IllegalArgumentException.class,
                    () -> user.finalizeProfile(null));
        }

        @Test
        @DisplayName("Should throw exception for whitespace-only username")
        void shouldThrowExceptionForWhitespaceUsername() {
            // Arrange
            User user = new User("test@example.com", "password", UserRole.USER);

            // Act & Assert
            assertThrows(
                    IllegalArgumentException.class,
                    () -> user.finalizeProfile("   "));
        }
    }

    // ==================== PASSWORD TESTS ====================

    @Nested
    @DisplayName("Password Tests")
    class PasswordTests {

        @Test
        @DisplayName("Should update password with valid value")
        void shouldUpdatePassword() {
            // Arrange
            User user = new User("test@example.com", "oldPassword", UserRole.USER);

            // Act
            user.setPassword("newSecurePassword");

            // Assert
            assertEquals("newSecurePassword", user.getPassword());
        }

        @Test
        @DisplayName("Should throw exception for null password")
        void shouldThrowExceptionForNullPassword() {
            // Arrange
            User user = new User("test@example.com", "password", UserRole.USER);

            // Act & Assert
            assertThrows(
                    IllegalArgumentException.class,
                    () -> user.setPassword(null));
        }

        @Test
        @DisplayName("Should throw exception for empty password")
        void shouldThrowExceptionForEmptyPassword() {
            // Arrange
            User user = new User("test@example.com", "password", UserRole.USER);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> user.setPassword("   "));
            assertEquals("Password cannot be empty.", exception.getMessage());
        }
    }

    // ==================== ROLE TESTS ====================

    @Nested
    @DisplayName("Role Tests")
    class RoleTests {

        @Test
        @DisplayName("Employee should not be admin")
        void employeeShouldNotBeAdmin() {
            // Arrange
            User employee = new User("employee@company.com", "pass", UserRole.USER);

            // Assert
            assertFalse(employee.isAdmin());
        }

        @Test
        @DisplayName("Admin should be admin")
        void adminShouldBeAdmin() {
            // Arrange
            User admin = new User("admin@company.com", "pass", UserRole.ADMIN);

            // Assert
            assertTrue(admin.isAdmin());
        }

        @Test
        @DisplayName("Should be able to change user role")
        void shouldChangeUserRole() {
            // Arrange
            User user = new User("test@example.com", "pass", UserRole.USER);

            // Act
            user.setRole(UserRole.ADMIN);

            // Assert
            assertEquals(UserRole.ADMIN, user.getRole());
            assertTrue(user.isAdmin());
        }
    }
}
