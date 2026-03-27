package com.bugboard.model;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;

import com.bugboard.enums.IssueStatus;
import com.bugboard.enums.IssueType;
import com.bugboard.enums.PriorityLevel;
import com.bugboard.enums.UserRole;

/**
 * White-box unit tests for the {@link Issue} domain entity.
 * Covers all constructors, setters, getters, and business methods
 * with 100% branch coverage.
 */
public class IssueTest {

   private User reporter;

   @Before
   public void setUp() {
      reporter = new User("reporter@test.com", "password", UserRole.USER);
   }

   // ==================== CONSTRUCTOR TESTS ====================

   /**
    * TC1: Valid construction with all mandatory fields.
    */
   @Test
   public void testConstructor_TC1_ValidCreation() {
      // Arrange
      String title = "Valid title for this issue";
      String description = "A valid description";

      // Act
      Issue issue = new Issue(title, description, reporter, IssueType.BUG);

      // Assert
      assertEquals("PostCond failed: title should match", title, issue.getTitle());
      assertEquals("PostCond failed: description should match", description, issue.getDescription());
      assertEquals("PostCond failed: reporter should match", reporter, issue.getReporter());
      assertEquals("PostCond failed: type should be BUG", IssueType.BUG, issue.getType());
      assertEquals("PostCond failed: default status should be TODO", IssueStatus.TODO, issue.getStatus());
      assertEquals("PostCond failed: default priority should be MEDIUM", PriorityLevel.MEDIUM, issue.getPriority());
      assertNotNull("PostCond failed: createdAt should be set", issue.getCreatedAt());
      assertNull("PostCond failed: closedAt should be null", issue.getClosedAt());
   }

   /**
    * TC2: Null title causes IllegalArgumentException.
    */
   @Test(expected = IllegalArgumentException.class)
   public void testConstructor_TC2_NullTitle() {
      // Arrange & Act
      new Issue(null, "Description", reporter, IssueType.BUG);
   }

   /**
    * TC3: Title shorter than 10 characters causes IllegalArgumentException.
    */
   @Test(expected = IllegalArgumentException.class)
   public void testConstructor_TC3_ShortTitle() {
      // Arrange & Act
      new Issue("Short", "Description", reporter, IssueType.BUG);
   }

   /**
    * TC4: Title exactly 10 characters (boundary) should succeed.
    */
   @Test
   public void testConstructor_TC4_BoundaryTitle10Chars() {
      // Arrange & Act
      Issue issue = new Issue("1234567890", "Description", reporter, IssueType.FEATURE);

      // Assert
      assertEquals("PostCond failed: title should be accepted", "1234567890", issue.getTitle());
   }

   /**
    * TC5: Null description causes IllegalArgumentException.
    */
   @Test(expected = IllegalArgumentException.class)
   public void testConstructor_TC5_NullDescription() {
      // Arrange & Act
      new Issue("Valid title here!", null, reporter, IssueType.BUG);
   }

   /**
    * TC6: Empty description causes IllegalArgumentException.
    */
   @Test(expected = IllegalArgumentException.class)
   public void testConstructor_TC6_EmptyDescription() {
      // Arrange & Act
      new Issue("Valid title here!", "", reporter, IssueType.BUG);
   }

   /**
    * TC7: Whitespace-only description causes IllegalArgumentException.
    */
   @Test(expected = IllegalArgumentException.class)
   public void testConstructor_TC7_WhitespaceDescription() {
      // Arrange & Act
      new Issue("Valid title here!", "   ", reporter, IssueType.BUG);
   }

   /**
    * TC8: Null type causes IllegalArgumentException.
    */
   @Test(expected = IllegalArgumentException.class)
   public void testConstructor_TC8_NullType() {
      // Arrange & Act
      new Issue("Valid title here!", "Description", reporter, null);
   }

   // ==================== setTitle TESTS ====================

   /**
    * TC9: setTitle with valid value updates the title.
    */
   @Test
   public void testSetTitle_TC9_ValidTitle() {
      // Arrange
      Issue issue = new Issue("Original valid title", "Desc", reporter, IssueType.BUG);

      // Act
      issue.setTitle("Updated valid title");

      // Assert
      assertEquals("PostCond failed: title should be updated", "Updated valid title", issue.getTitle());
   }

   /**
    * TC10: setTitle with null causes IllegalArgumentException.
    */
   @Test(expected = IllegalArgumentException.class)
   public void testSetTitle_TC10_NullTitle() {
      // Arrange
      Issue issue = new Issue("Original valid title", "Desc", reporter, IssueType.BUG);

      // Act
      issue.setTitle(null);
   }

   /**
    * TC11: setTitle with short string causes IllegalArgumentException.
    */
   @Test(expected = IllegalArgumentException.class)
   public void testSetTitle_TC11_ShortTitle() {
      // Arrange
      Issue issue = new Issue("Original valid title", "Desc", reporter, IssueType.BUG);

      // Act
      issue.setTitle("short");
   }

   // ==================== setStatus TESTS ====================

   /**
    * TC12: Transition from TODO to CLOSED sets closedAt.
    */
   @Test
   public void testSetStatus_TC12_TodoToClosedSetsClosedAt() {
      // Arrange
      Issue issue = new Issue("Valid title for issue", "Desc", reporter, IssueType.BUG);
      assertNull("PostCond failed: closedAt should initially be null", issue.getClosedAt());

      // Act
      issue.setStatus(IssueStatus.CLOSED);

      // Assert
      assertEquals("PostCond failed: status should be CLOSED", IssueStatus.CLOSED, issue.getStatus());
      assertNotNull("PostCond failed: closedAt should be set", issue.getClosedAt());
   }

   /**
    * TC13: Transition from CLOSED to CLOSED does NOT reset closedAt (else-if branch not taken).
    */
   @Test
   public void testSetStatus_TC13_ClosedToClosedNoChange() {
      // Arrange
      Issue issue = new Issue("Valid title for issue", "Desc", reporter, IssueType.BUG);
      issue.setStatus(IssueStatus.CLOSED);
      assertNotNull("PostCond failed: closedAt should be set after first close", issue.getClosedAt());

      // Act
      issue.setStatus(IssueStatus.CLOSED);

      // Assert
      assertEquals("PostCond failed: status should remain CLOSED", IssueStatus.CLOSED, issue.getStatus());
      assertNotNull("PostCond failed: closedAt should still be set", issue.getClosedAt());
   }

   /**
    * TC14: Transition from CLOSED to IN_PROGRESS clears closedAt (reopening).
    */
   @Test
   public void testSetStatus_TC14_ClosedToInProgressClearsClosedAt() {
      // Arrange
      Issue issue = new Issue("Valid title for issue", "Desc", reporter, IssueType.BUG);
      issue.setStatus(IssueStatus.CLOSED);
      assertNotNull("PostCond failed: closedAt should be set", issue.getClosedAt());

      // Act
      issue.setStatus(IssueStatus.IN_PROGRESS);

      // Assert
      assertEquals("PostCond failed: status should be IN_PROGRESS", IssueStatus.IN_PROGRESS, issue.getStatus());
      assertNull("PostCond failed: closedAt should be cleared", issue.getClosedAt());
   }

   /**
    * TC15: Transition from TODO to IN_PROGRESS (non-CLOSED to non-CLOSED) keeps closedAt null.
    */
   @Test
   public void testSetStatus_TC15_TodoToInProgress() {
      // Arrange
      Issue issue = new Issue("Valid title for issue", "Desc", reporter, IssueType.BUG);

      // Act
      issue.setStatus(IssueStatus.IN_PROGRESS);

      // Assert
      assertEquals("PostCond failed: status should be IN_PROGRESS", IssueStatus.IN_PROGRESS, issue.getStatus());
      assertNull("PostCond failed: closedAt should remain null", issue.getClosedAt());
   }

   /**
    * TC15b: Transition from TODO to RESOLVED sets closedAt.
    */
   @Test
   public void testSetStatus_TC15b_TodoToResolvedSetsClosedAt() {
      Issue issue = new Issue("Valid title for issue", "Desc", reporter, IssueType.BUG);

      issue.setStatus(IssueStatus.RESOLVED);

      assertEquals("PostCond failed: status should be RESOLVED", IssueStatus.RESOLVED, issue.getStatus());
      assertNotNull("PostCond failed: closedAt should be set", issue.getClosedAt());
   }

   /**
    * TC15c: Transition from RESOLVED to CLOSED preserves closedAt.
    */
   @Test
   public void testSetStatus_TC15c_ResolvedToClosedKeepsClosedAt() {
      Issue issue = new Issue("Valid title for issue", "Desc", reporter, IssueType.BUG);
      issue.setStatus(IssueStatus.RESOLVED);
      var resolvedAt = issue.getClosedAt();
      assertNotNull("PostCond failed: closedAt should be set", resolvedAt);

      issue.setStatus(IssueStatus.CLOSED);

      assertEquals("PostCond failed: status should be CLOSED", IssueStatus.CLOSED, issue.getStatus());
      assertNotNull("PostCond failed: closedAt should remain set", issue.getClosedAt());
      assertEquals("PostCond failed: closedAt should stay unchanged for closed-like transitions",
            resolvedAt, issue.getClosedAt());
   }

   // ==================== setType TESTS ====================

   /**
    * TC16: setType with valid value updates the type.
    */
   @Test
   public void testSetType_TC16_ValidType() {
      // Arrange
      Issue issue = new Issue("Valid title for issue", "Desc", reporter, IssueType.BUG);

      // Act
      issue.setType(IssueType.FEATURE);

      // Assert
      assertEquals("PostCond failed: type should be FEATURE", IssueType.FEATURE, issue.getType());
   }

   /**
    * TC17: setType with null causes IllegalArgumentException.
    */
   @Test(expected = IllegalArgumentException.class)
   public void testSetType_TC17_NullType() {
      // Arrange
      Issue issue = new Issue("Valid title for issue", "Desc", reporter, IssueType.BUG);

      // Act
      issue.setType(null);
   }

   // ==================== getOriginalIssueId TESTS ====================

   /**
    * TC18: getOriginalIssueId returns null when no original issue set.
    */
   @Test
   public void testGetOriginalIssueId_TC18_NullWhenNoOriginal() {
      // Arrange
      Issue issue = new Issue("Valid title for issue", "Desc", reporter, IssueType.BUG);

      // Act
      Long originalId = issue.getOriginalIssueId();

      // Assert
      assertNull("PostCond failed: originalIssueId should be null", originalId);
   }

   /**
    * TC19: getOriginalIssueId returns the original's ID after markAsDuplicateOf.
    */
   @Test
   public void testGetOriginalIssueId_TC19_ReturnsIdAfterDuplicate() {
      // Arrange
      Issue duplicate = new Issue("Valid duplicate title", "Desc", reporter, IssueType.BUG);
      Issue original = new Issue("Valid original title", "Desc", reporter, IssueType.BUG);

      // Act
      duplicate.markAsDuplicateOf(original);

      // Assert - originalIssue.getId() will be null (not persisted), but the branch is covered
      // The important thing is that originalIssue != null so it tries to call getId()
      assertNull("PostCond failed: getId returns null for non-persisted entity", duplicate.getOriginalIssueId());
   }

   // ==================== markAsDuplicateOf TESTS ====================

   /**
    * TC20: Valid markAsDuplicateOf sets status to CLOSED and records closedAt.
    */
   @Test
   public void testMarkAsDuplicateOf_TC20_ValidDuplicate() {
      // Arrange
      Issue duplicate = new Issue("Valid duplicate title", "Desc", reporter, IssueType.BUG);
      Issue original = new Issue("Valid original title", "Desc", reporter, IssueType.BUG);

      // Act
      duplicate.markAsDuplicateOf(original);

      // Assert
      assertEquals("PostCond failed: status should be CLOSED", IssueStatus.CLOSED, duplicate.getStatus());
      assertNotNull("PostCond failed: closedAt should be set", duplicate.getClosedAt());
   }

   /**
    * TC21: markAsDuplicateOf with null original causes IllegalArgumentException.
    */
   @Test(expected = IllegalArgumentException.class)
   public void testMarkAsDuplicateOf_TC21_NullOriginal() {
      // Arrange
      Issue issue = new Issue("Valid title for issue", "Desc", reporter, IssueType.BUG);

      // Act
      issue.markAsDuplicateOf(null);
   }

   /**
    * TC22: markAsDuplicateOf with same object reference causes IllegalArgumentException.
    */
   @Test(expected = IllegalArgumentException.class)
   public void testMarkAsDuplicateOf_TC22_SameObjectReference() {
      // Arrange
      Issue issue = new Issue("Valid title for issue", "Desc", reporter, IssueType.BUG);

      // Act
      issue.markAsDuplicateOf(issue);
   }

   /**
    * TC23: markAsDuplicateOf on already CLOSED issue causes IllegalStateException.
    */
   @Test(expected = IllegalStateException.class)
   public void testMarkAsDuplicateOf_TC23_AlreadyClosed() {
      // Arrange
      Issue issue = new Issue("Valid title for issue", "Desc", reporter, IssueType.BUG);
      issue.setStatus(IssueStatus.CLOSED);
      Issue original = new Issue("Valid original title", "Desc", reporter, IssueType.BUG);

      // Act
      issue.markAsDuplicateOf(original);
   }

   // ==================== SIMPLE GETTER/SETTER TESTS ====================

   /**
    * TC24: setPriority and getPriority work correctly.
    */
   @Test
   public void testSetPriority_TC24_ValidPriority() {
      // Arrange
      Issue issue = new Issue("Valid title for issue", "Desc", reporter, IssueType.BUG);

      // Act
      issue.setPriority(PriorityLevel.CRITICAL);

      // Assert
      assertEquals("PostCond failed: priority should be CRITICAL", PriorityLevel.CRITICAL, issue.getPriority());
   }

   /**
    * TC25: setDescription and getDescription work correctly.
    */
   @Test
   public void testSetDescription_TC25_ValidDescription() {
      // Arrange
      Issue issue = new Issue("Valid title for issue", "Old desc", reporter, IssueType.BUG);

      // Act
      issue.setDescription("New description");

      // Assert
      assertEquals("PostCond failed: description should be updated", "New description", issue.getDescription());
   }

   /**
    * TC26: setAssignee and getAssignee work correctly.
    */
   @Test
   public void testSetAssignee_TC26_ValidAssignee() {
      // Arrange
      Issue issue = new Issue("Valid title for issue", "Desc", reporter, IssueType.BUG);
      User assignee = new User("assignee@test.com", "pass", UserRole.USER);

      // Act
      issue.setAssignee(assignee);

      // Assert
      assertEquals("PostCond failed: assignee should be set", assignee, issue.getAssignee());
   }

   /**
    * TC27: setAttachmentPath and getAttachmentPath work correctly.
    */
   @Test
   public void testSetAttachmentPath_TC27_ValidPath() {
      // Arrange
      Issue issue = new Issue("Valid title for issue", "Desc", reporter, IssueType.BUG);

      // Act
      issue.setAttachmentPath("/uploads/file.png");

      // Assert
      assertEquals("PostCond failed: attachmentPath should be set", "/uploads/file.png", issue.getAttachmentPath());
   }

   /**
    * TC28: getId returns null for non-persisted entity.
    */
   @Test
   public void testGetId_TC28_NullForNewEntity() {
      // Arrange
      Issue issue = new Issue("Valid title for issue", "Desc", reporter, IssueType.BUG);

      // Act & Assert
      assertNull("PostCond failed: id should be null for non-persisted entity", issue.getId());
   }

   // ==================== markAsDuplicateOf branch: id-based equality TESTS ====================

   /**
    * TC29: markAsDuplicateOf with same id (non-null) causes IllegalArgumentException.
    * Covers branch: this.id != null (true) AND this.id.equals(original.getId()) (true).
    */
   @Test(expected = IllegalArgumentException.class)
   public void testMarkAsDuplicateOf_TC29_SameIdDifferentObjects() throws Exception {
      // Arrange
      Issue duplicate = new Issue("Valid duplicate title!", "Desc", reporter, IssueType.BUG);
      Issue original = new Issue("Valid original title!", "Desc", reporter, IssueType.BUG);
      setId(duplicate, 100L);
      setId(original, 100L);

      // Act
      duplicate.markAsDuplicateOf(original);
   }

   /**
    * TC30: markAsDuplicateOf with different non-null ids succeeds.
    * Covers branch: this.id != null (true) AND this.id.equals(original.getId()) (false).
    */
   @Test
   public void testMarkAsDuplicateOf_TC30_DifferentNonNullIds() throws Exception {
      // Arrange
      Issue duplicate = new Issue("Valid duplicate title!", "Desc", reporter, IssueType.BUG);
      Issue original = new Issue("Valid original title!", "Desc", reporter, IssueType.BUG);
      setId(duplicate, 100L);
      setId(original, 200L);

      // Act
      duplicate.markAsDuplicateOf(original);

      // Assert
      assertEquals("PostCond failed: status should be CLOSED", IssueStatus.CLOSED, duplicate.getStatus());
      assertNotNull("PostCond failed: closedAt should be set", duplicate.getClosedAt());
   }

   // ==================== HELPER METHODS ====================

   private void setId(Issue issue, Long id) throws Exception {
      Field idField = Issue.class.getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(issue, id);
   }
}
