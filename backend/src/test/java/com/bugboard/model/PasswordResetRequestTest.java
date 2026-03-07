package com.bugboard.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import com.bugboard.enums.UserRole;
import com.bugboard.model.PasswordResetRequest.RequestStatus;

/**
 * White-box unit tests for the {@link PasswordResetRequest} domain entity.
 * Covers all constructors, business methods, and getters
 * with 100% branch coverage.
 */
public class PasswordResetRequestTest {

   private User regularUser;
   private User adminUser;

   @Before
   public void setUp() {
      regularUser = new User("user@test.com", "password", UserRole.USER);
      adminUser = new User("admin@test.com", "password", UserRole.ADMIN);
   }

   // ==================== CONSTRUCTOR TESTS ====================

   /**
    * TC1: Valid construction with a non-null user.
    * Expected: All fields correctly initialized, status PENDING.
    */
   @Test
   public void testConstructor_TC1_ValidUser() {
      // Act
      PasswordResetRequest request = new PasswordResetRequest(regularUser);

      // Assert
      assertEquals("PostCond failed: user should match", regularUser, request.getUser());
      assertNotNull("PostCond failed: requestedAt should be set", request.getRequestedAt());
      assertEquals("PostCond failed: status should be PENDING", RequestStatus.PENDING, request.getStatus());
      assertNull("PostCond failed: processedAt should be null", request.getProcessedAt());
      assertNull("PostCond failed: processedByAdmin should be null", request.getProcessedByAdmin());
      assertNull("PostCond failed: id should be null before persist", request.getId());
   }

   /**
    * TC2: Null user in constructor.
    * Expected: IllegalArgumentException.
    */
   @Test(expected = IllegalArgumentException.class)
   public void testConstructor_TC2_NullUser() {
      // Act
      new PasswordResetRequest(null);
   }

   /**
    * TC3: Protected JPA no-arg constructor.
    * Expected: Object created with null/default fields.
    */
   @Test
   public void testConstructor_TC3_JpaNoArg() {
      // Act (accessible from same package)
      PasswordResetRequest request = new PasswordResetRequest();

      // Assert
      assertNull("PostCond failed: id should be null", request.getId());
      assertNull("PostCond failed: user should be null", request.getUser());
      assertNull("PostCond failed: status should be null", request.getStatus());
   }

   // ==================== markAsCompleted TESTS ====================

   /**
    * TC4: Admin marks request as completed.
    * Expected: Status COMPLETED, processedAt and processedByAdmin set.
    */
   @Test
   public void testMarkAsCompleted_TC4_ByAdmin() {
      // Arrange
      PasswordResetRequest request = new PasswordResetRequest(regularUser);

      // Act
      request.markAsCompleted(adminUser);

      // Assert
      assertEquals("PostCond failed: status should be COMPLETED", RequestStatus.COMPLETED, request.getStatus());
      assertNotNull("PostCond failed: processedAt should be set", request.getProcessedAt());
      assertEquals("PostCond failed: processedByAdmin should match", adminUser, request.getProcessedByAdmin());
   }

   /**
    * TC5: Non-admin attempts to mark as completed.
    * Expected: IllegalArgumentException.
    */
   @Test(expected = IllegalArgumentException.class)
   public void testMarkAsCompleted_TC5_ByNonAdmin() {
      // Arrange
      PasswordResetRequest request = new PasswordResetRequest(regularUser);

      // Act
      request.markAsCompleted(regularUser);
   }

   // ==================== markAsRejected TESTS ====================

   /**
    * TC6: Admin marks request as rejected.
    * Expected: Status REJECTED, processedAt and processedByAdmin set.
    */
   @Test
   public void testMarkAsRejected_TC6_ByAdmin() {
      // Arrange
      PasswordResetRequest request = new PasswordResetRequest(regularUser);

      // Act
      request.markAsRejected(adminUser);

      // Assert
      assertEquals("PostCond failed: status should be REJECTED", RequestStatus.REJECTED, request.getStatus());
      assertNotNull("PostCond failed: processedAt should be set", request.getProcessedAt());
      assertEquals("PostCond failed: processedByAdmin should match", adminUser, request.getProcessedByAdmin());
   }

   /**
    * TC7: Non-admin attempts to mark as rejected.
    * Expected: IllegalArgumentException.
    */
   @Test(expected = IllegalArgumentException.class)
   public void testMarkAsRejected_TC7_ByNonAdmin() {
      // Arrange
      PasswordResetRequest request = new PasswordResetRequest(regularUser);

      // Act
      request.markAsRejected(regularUser);
   }

   // ==================== isPending TESTS ====================

   /**
    * TC8: Request is in PENDING status.
    * Expected: isPending returns true.
    */
   @Test
   public void testIsPending_TC8_WhenPending() {
      // Arrange
      PasswordResetRequest request = new PasswordResetRequest(regularUser);

      // Assert
      assertTrue("PostCond failed: should be pending", request.isPending());
   }

   /**
    * TC9: Request is in COMPLETED status.
    * Expected: isPending returns false.
    */
   @Test
   public void testIsPending_TC9_WhenCompleted() {
      // Arrange
      PasswordResetRequest request = new PasswordResetRequest(regularUser);
      request.markAsCompleted(adminUser);

      // Assert
      assertFalse("PostCond failed: should not be pending", request.isPending());
   }

   /**
    * TC10: Request is in REJECTED status.
    * Expected: isPending returns false.
    */
   @Test
   public void testIsPending_TC10_WhenRejected() {
      // Arrange
      PasswordResetRequest request = new PasswordResetRequest(regularUser);
      request.markAsRejected(adminUser);

      // Assert
      assertFalse("PostCond failed: should not be pending", request.isPending());
   }
}
