package com.bugboard.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;

/**
 * Unit tests for the PasswordHasher utility class.
 * Tests cover hashing, verification, and edge cases.
 */
@DisplayName("PasswordHasher Tests")
class PasswordHasherTest {

    // ==================== CONSTRUCTOR TESTS ====================

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should throw exception when trying to instantiate utility class")
        void shouldThrowExceptionWhenInstantiating() throws Exception {
            // Act & Assert
            java.lang.reflect.InvocationTargetException exception = assertThrows(
                    java.lang.reflect.InvocationTargetException.class,
                    () -> {
                        Constructor<PasswordHasher> constructor = PasswordHasher.class.getDeclaredConstructor();
                        constructor.setAccessible(true);
                        constructor.newInstance();
                    });

            assertTrue(exception.getCause() instanceof UnsupportedOperationException);
            assertEquals("Utility class cannot be instantiated", exception.getCause().getMessage());
        }
    }

    // ==================== HASH TESTS ====================

    @Nested
    @DisplayName("Hash Method Tests")
    class HashTests {

        @Test
        @DisplayName("Should hash a password successfully")
        void shouldHashPassword() {
            // Arrange
            String plainPassword = "SecurePassword123!";

            // Act
            String hashedPassword = PasswordHasher.hash(plainPassword);

            // Assert
            assertNotNull(hashedPassword);
            assertNotEquals(plainPassword, hashedPassword);
            assertTrue(hashedPassword.startsWith("$2a$"), "BCrypt hash should start with $2a$");
        }

        @Test
        @DisplayName("Should generate different hashes for same password")
        void shouldGenerateDifferentHashes() {
            // Arrange
            String plainPassword = "SamePassword123!";

            // Act
            String hash1 = PasswordHasher.hash(plainPassword);
            String hash2 = PasswordHasher.hash(plainPassword);

            // Assert
            assertNotEquals(hash1, hash2, "Different salts should produce different hashes");
        }

        @Test
        @DisplayName("Should throw exception for null password")
        void shouldThrowExceptionForNullPassword() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> PasswordHasher.hash(null));
            assertEquals("Password cannot be null.", exception.getMessage());
        }

        @Test
        @DisplayName("Should hash empty password")
        void shouldHashEmptyPassword() {
            // Act
            String hashedPassword = PasswordHasher.hash("");

            // Assert - Empty password is technically valid for BCrypt
            assertNotNull(hashedPassword);
        }
    }

    // ==================== VERIFY TESTS ====================

    @Nested
    @DisplayName("Verify Method Tests")
    class VerifyTests {

        @Test
        @DisplayName("Should verify correct password")
        void shouldVerifyCorrectPassword() {
            // Arrange
            String plainPassword = "CorrectPassword123!";
            String hashedPassword = PasswordHasher.hash(plainPassword);

            // Act
            boolean result = PasswordHasher.verify(plainPassword, hashedPassword);

            // Assert
            assertTrue(result, "Correct password should verify successfully");
        }

        @Test
        @DisplayName("Should reject incorrect password")
        void shouldRejectIncorrectPassword() {
            // Arrange
            String correctPassword = "CorrectPassword123!";
            String wrongPassword = "WrongPassword456!";
            String hashedPassword = PasswordHasher.hash(correctPassword);

            // Act
            boolean result = PasswordHasher.verify(wrongPassword, hashedPassword);

            // Assert
            assertFalse(result, "Incorrect password should not verify");
        }

        @Test
        @DisplayName("Should return false for null plain password")
        void shouldReturnFalseForNullPlainPassword() {
            // Arrange
            String hashedPassword = PasswordHasher.hash("SomePassword");

            // Act
            boolean result = PasswordHasher.verify(null, hashedPassword);

            // Assert
            assertFalse(result);
        }

        @Test
        @DisplayName("Should return false for null hashed password")
        void shouldReturnFalseForNullHashedPassword() {
            // Act
            boolean result = PasswordHasher.verify("SomePassword", null);

            // Assert
            assertFalse(result);
        }

        @Test
        @DisplayName("Should return false for invalid hash format")
        void shouldReturnFalseForInvalidHash() {
            // Arrange
            String invalidHash = "not-a-valid-bcrypt-hash";

            // Act
            boolean result = PasswordHasher.verify("SomePassword", invalidHash);

            // Assert
            assertFalse(result, "Invalid hash format should return false");
        }

        @Test
        @DisplayName("Should be case sensitive")
        void shouldBeCaseSensitive() {
            // Arrange
            String password = "CaseSensitive123!";
            String hashedPassword = PasswordHasher.hash(password);

            // Act
            boolean resultLower = PasswordHasher.verify("casesensitive123!", hashedPassword);
            boolean resultUpper = PasswordHasher.verify("CASESENSITIVE123!", hashedPassword);

            // Assert
            assertFalse(resultLower, "Password verification should be case sensitive");
            assertFalse(resultUpper, "Password verification should be case sensitive");
        }

        @Test
        @DisplayName("Should verify password with special characters")
        void shouldVerifyPasswordWithSpecialCharacters() {
            // Arrange
            String password = "P@$$w0rd!#%&*()_+-=[]{}|;':\",./<>?";
            String hashedPassword = PasswordHasher.hash(password);

            // Act
            boolean result = PasswordHasher.verify(password, hashedPassword);

            // Assert
            assertTrue(result, "Password with special characters should verify");
        }

        @Test
        @DisplayName("Should verify password with unicode characters")
        void shouldVerifyPasswordWithUnicode() {
            // Arrange
            String password = "Pässwörd123!€";
            String hashedPassword = PasswordHasher.hash(password);

            // Act
            boolean result = PasswordHasher.verify(password, hashedPassword);

            // Assert
            assertTrue(result, "Password with unicode characters should verify");
        }
    }

    // ==================== INTEGRATION TESTS ====================

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should work for typical login flow")
        void shouldWorkForTypicalLoginFlow() {
            // Simulate user registration
            String userPassword = "MySecurePassword123!";
            String storedHash = PasswordHasher.hash(userPassword);

            // Simulate login attempt with correct password
            boolean loginSuccess = PasswordHasher.verify(userPassword, storedHash);
            assertTrue(loginSuccess, "User should be able to login with correct password");

            // Simulate login attempt with wrong password
            boolean loginFail = PasswordHasher.verify("WrongPassword", storedHash);
            assertFalse(loginFail, "User should not be able to login with wrong password");
        }

        @Test
        @DisplayName("Should work for password change flow")
        void shouldWorkForPasswordChangeFlow() {
            // Original password
            String oldPassword = "OldPassword123!";
            String oldHash = PasswordHasher.hash(oldPassword);

            // User changes password
            String newPassword = "NewPassword456!";
            String newHash = PasswordHasher.hash(newPassword);

            // Old password should no longer work with new hash
            assertFalse(PasswordHasher.verify(oldPassword, newHash));

            // New password should work with new hash
            assertTrue(PasswordHasher.verify(newPassword, newHash));

            // Old password should still work with old hash (for audit purposes)
            assertTrue(PasswordHasher.verify(oldPassword, oldHash));
        }
    }
}
