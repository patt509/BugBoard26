package com.bugboard.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import com.bugboard.enums.UserRole;

/**
 * White-box unit tests for the {@link User} domain entity.
 * Covers all constructors, setters, getters, and business methods
 * with 100% branch coverage.
 */
public class UserTest {

   private User validUser;

   @Before
   public void setUp() {
      validUser = new User("test@example.com", "securePassword", UserRole.USER);
   }

   // ==================== CONSTRUCTOR TESTS ====================

   /**
    * TC1: Valid construction with USER role.
    * Expected: All fields set, isFirstLogin true, createdAt not null.
    */
   @Test
   public void testConstructor_TC1_ValidUserRole() {
      // Arrange & Act
      User user = new User("user@test.com", "password", UserRole.USER);

      // Assert
      assertEquals("PostCond failed: email should match", "user@test.com", user.getEmail());
      assertEquals("PostCond failed: password should match", "password", user.getPassword());
      assertEquals("PostCond failed: role should be USER", UserRole.USER, user.getRole());
      assertTrue("PostCond failed: isFirstLogin should be true", user.isFirstLogin());
      assertNotNull("PostCond failed: createdAt should be set", user.getCreatedAt());
      assertNull("PostCond failed: username should be null initially", user.getUsername());
      assertNull("PostCond failed: id should be null before persist", user.getId());
   }

   /**
    * TC2: Valid construction with ADMIN role.
    * Expected: Role is ADMIN.
    */
   @Test
   public void testConstructor_TC2_ValidAdminRole() {
      // Arrange & Act
      User user = new User("admin@test.com", "password", UserRole.ADMIN);

      // Assert
      assertEquals("PostCond failed: role should be ADMIN", UserRole.ADMIN, user.getRole());
   }

   /**
    * TC3: Null email in constructor.
    * Expected: IllegalArgumentException (first branch of OR: email == null).
    */
   @Test(expected = IllegalArgumentException.class)
   public void testConstructor_TC3_NullEmail() {
      // Act
      new User(null, "password", UserRole.USER);
   }

   /**
    * TC4: Email without '@' symbol.
    * Expected: IllegalArgumentException (second branch of OR: !email.contains("@")).
    */
   @Test(expected = IllegalArgumentException.class)
   public void testConstructor_TC4_EmailWithoutAt() {
      // Act
      new User("invalidemail", "password", UserRole.USER);
   }

   // ==================== DEFAULT CONSTRUCTOR ====================

   /**
    * TC5: JPA no-arg constructor.
    * Expected: User created with all fields null/default.
    */
   @Test
   public void testDefaultConstructor_TC5_JPA() {
      // Act
      User user = new User();

      // Assert
      assertNull("PostCond failed: id should be null", user.getId());
      assertNull("PostCond failed: email should be null", user.getEmail());
      assertNull("PostCond failed: username should be null", user.getUsername());
   }

   // ==================== setPassword TESTS ====================

   /**
    * TC6: Set valid password.
    * Expected: Password updated.
    */
   @Test
   public void testSetPassword_TC6_ValidPassword() {
      // Act
      validUser.setPassword("newPassword");

      // Assert
      assertEquals("PostCond failed: password should be updated", "newPassword", validUser.getPassword());
   }

   /**
    * TC7: Null password.
    * Expected: IllegalArgumentException (first branch of OR: password == null).
    */
   @Test(expected = IllegalArgumentException.class)
   public void testSetPassword_TC7_NullPassword() {
      // Act
      validUser.setPassword(null);
   }

   /**
    * TC8: Empty string password.
    * Expected: IllegalArgumentException (second branch of OR: trim().isEmpty()).
    */
   @Test(expected = IllegalArgumentException.class)
   public void testSetPassword_TC8_EmptyPassword() {
      // Act
      validUser.setPassword("");
   }

   /**
    * TC9: Whitespace-only password.
    * Expected: IllegalArgumentException (second branch of OR after trim).
    */
   @Test(expected = IllegalArgumentException.class)
   public void testSetPassword_TC9_WhitespaceOnlyPassword() {
      // Act
      validUser.setPassword("   ");
   }

   // ==================== finalizeProfile TESTS ====================

   /**
    * TC10: Successful profile finalization with valid username.
    * Expected: Username set, isFirstLogin becomes false.
    */
   @Test
   public void testFinalizeProfile_TC10_Success() {
      // Act
      validUser.finalizeProfile("validUser");

      // Assert
      assertEquals("PostCond failed: username should be set", "validUser", validUser.getUsername());
      assertFalse("PostCond failed: isFirstLogin should be false", validUser.isFirstLogin());
   }

   /**
    * TC11: Profile already finalized (isFirstLogin == false).
    * Expected: IllegalStateException.
    */
   @Test(expected = IllegalStateException.class)
   public void testFinalizeProfile_TC11_AlreadyFinalized() {
      // Arrange
      validUser.finalizeProfile("validUser");

      // Act (second call — isFirstLogin is now false)
      validUser.finalizeProfile("anotherName");
   }

   /**
    * TC12: Null username parameter.
    * Expected: IllegalArgumentException (first branch of OR: chosenUsername == null).
    */
   @Test(expected = IllegalArgumentException.class)
   public void testFinalizeProfile_TC12_NullUsername() {
      // Act
      validUser.finalizeProfile(null);
   }

   /**
    * TC13: Username with 2 characters (below minimum of 3).
    * Expected: IllegalArgumentException (second branch: trim().length() < 3).
    */
   @Test(expected = IllegalArgumentException.class)
   public void testFinalizeProfile_TC13_ShortUsername() {
      // Act
      validUser.finalizeProfile("ab");
   }

   /**
    * TC14: Username with exactly 3 characters (boundary value).
    * Expected: Username set successfully.
    */
   @Test
   public void testFinalizeProfile_TC14_ExactThreeChars() {
      // Act
      validUser.finalizeProfile("abc");

      // Assert
      assertEquals("PostCond failed: username should be set", "abc", validUser.getUsername());
   }

   /**
    * TC15: Empty string username.
    * Expected: IllegalArgumentException (trim().length() < 3 after trim of "").
    */
   @Test(expected = IllegalArgumentException.class)
   public void testFinalizeProfile_TC15_EmptyUsername() {
      // Act
      validUser.finalizeProfile("");
   }

   // ==================== isAdmin TESTS ====================

   /**
    * TC16: User with ADMIN role.
    * Expected: isAdmin returns true.
    */
   @Test
   public void testIsAdmin_TC16_AdminRole() {
      // Arrange
      User admin = new User("admin@test.com", "password", UserRole.ADMIN);

      // Assert
      assertTrue("PostCond failed: should be admin", admin.isAdmin());
   }

   /**
    * TC17: User with USER role.
    * Expected: isAdmin returns false.
    */
   @Test
   public void testIsAdmin_TC17_UserRole() {
      // Assert
      assertFalse("PostCond failed: should not be admin", validUser.isAdmin());
   }

   // ==================== SETTER/GETTER TESTS ====================

   /**
    * TC18: setUsername and getUsername.
    */
   @Test
   public void testSetUsername_TC18() {
      // Act
      validUser.setUsername("newUsername");

      // Assert
      assertEquals("PostCond failed: username should be updated", "newUsername", validUser.getUsername());
   }

   /**
    * TC19: setRole and getRole.
    */
   @Test
   public void testSetRole_TC19() {
      // Act
      validUser.setRole(UserRole.ADMIN);

      // Assert
      assertEquals("PostCond failed: role should be ADMIN", UserRole.ADMIN, validUser.getRole());
   }

   /**
    * TC20: setFirstLogin and isFirstLogin.
    */
   @Test
   public void testSetFirstLogin_TC20() {
      // Act
      validUser.setFirstLogin(false);

      // Assert
      assertFalse("PostCond failed: isFirstLogin should be false", validUser.isFirstLogin());
   }
}
