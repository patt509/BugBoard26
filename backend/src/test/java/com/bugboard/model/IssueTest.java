package com.bugboard.model;

import com.bugboard.enums.IssueStatus;
import com.bugboard.enums.PriorityLevel;
import com.bugboard.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Issue model class.
 * Tests cover constructor validation, status transitions, and duplicate marking.
 */
@DisplayName("Issue Model Tests")
class IssueTest {

    private User reporter;

    @BeforeEach
    void setUp() {
        reporter = new User("reporter@company.com", "password", UserRole.USER);
    }

    // ==================== CONSTRUCTOR TESTS ====================

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create issue with valid title and description")
        void shouldCreateIssueWithValidData() {
            // Arrange & Act
            Issue issue = new Issue("This is a valid title for the issue", "Description of the bug", reporter);

            // Assert
            assertEquals("This is a valid title for the issue", issue.getTitle());
            assertEquals("Description of the bug", issue.getDescription());
            assertEquals(reporter, issue.getReporter());
            assertEquals(IssueStatus.TODO, issue.getStatus(), "New issue should have TODO status");
            assertEquals(PriorityLevel.MEDIUM, issue.getPriority(), "New issue should have MEDIUM priority by default");
            assertNotNull(issue.getCreatedAt());
            assertNull(issue.getClosedAt());
        }

        @Test
        @DisplayName("Should throw exception for title shorter than 10 characters")
        void shouldThrowExceptionForShortTitle() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Issue("Short", "Valid description", reporter)
            );
            assertEquals("Title must be at least 10 characters.", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception for null title")
        void shouldThrowExceptionForNullTitle() {
            // Act & Assert
            assertThrows(
                IllegalArgumentException.class,
                () -> new Issue(null, "Valid description", reporter)
            );
        }

        @Test
        @DisplayName("Should throw exception for empty description")
        void shouldThrowExceptionForEmptyDescription() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Issue("Valid title for issue", "", reporter)
            );
            assertEquals("Description cannot be empty.", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception for null description")
        void shouldThrowExceptionForNullDescription() {
            // Act & Assert
            assertThrows(
                IllegalArgumentException.class,
                () -> new Issue("Valid title for issue", null, reporter)
            );
        }

        @Test
        @DisplayName("Should accept title with exactly 10 characters")
        void shouldAcceptTitleWithExactly10Characters() {
            // Arrange & Act
            Issue issue = new Issue("1234567890", "Description", reporter);

            // Assert
            assertEquals("1234567890", issue.getTitle());
        }
    }

    // ==================== STATUS TRANSITION TESTS ====================

    @Nested
    @DisplayName("Status Transition Tests")
    class StatusTransitionTests {

        @Test
        @DisplayName("Should transition from TODO to IN_PROGRESS")
        void shouldTransitionToInProgress() {
            // Arrange
            Issue issue = new Issue("Valid title for issue", "Description", reporter);
            
            // Act
            issue.setStatus(IssueStatus.IN_PROGRESS);

            // Assert
            assertEquals(IssueStatus.IN_PROGRESS, issue.getStatus());
            assertNull(issue.getClosedAt(), "ClosedAt should be null for non-closed status");
        }

        @Test
        @DisplayName("Should set closedAt when transitioning to CLOSED")
        void shouldSetClosedAtWhenClosing() {
            // Arrange
            Issue issue = new Issue("Valid title for issue", "Description", reporter);
            
            // Act
            issue.setStatus(IssueStatus.CLOSED);

            // Assert
            assertEquals(IssueStatus.CLOSED, issue.getStatus());
            assertNotNull(issue.getClosedAt(), "ClosedAt should be set when status is CLOSED");
        }

        @Test
        @DisplayName("Should clear closedAt when reopening issue")
        void shouldClearClosedAtWhenReopening() {
            // Arrange
            Issue issue = new Issue("Valid title for issue", "Description", reporter);
            issue.setStatus(IssueStatus.CLOSED);
            assertNotNull(issue.getClosedAt()); // Verify it was set
            
            // Act - Reopen the issue
            issue.setStatus(IssueStatus.IN_PROGRESS);

            // Assert
            assertEquals(IssueStatus.IN_PROGRESS, issue.getStatus());
            assertNull(issue.getClosedAt(), "ClosedAt should be cleared when issue is reopened");
        }

        @Test
        @DisplayName("Should transition through all statuses")
        void shouldTransitionThroughAllStatuses() {
            // Arrange
            Issue issue = new Issue("Valid title for issue", "Description", reporter);
            
            // Act & Assert - Full workflow
            assertEquals(IssueStatus.TODO, issue.getStatus());
            
            issue.setStatus(IssueStatus.IN_PROGRESS);
            assertEquals(IssueStatus.IN_PROGRESS, issue.getStatus());
            
            issue.setStatus(IssueStatus.RESOLVED);
            assertEquals(IssueStatus.RESOLVED, issue.getStatus());
            
            issue.setStatus(IssueStatus.CLOSED);
            assertEquals(IssueStatus.CLOSED, issue.getStatus());
            assertNotNull(issue.getClosedAt());
        }
    }

    // ==================== DUPLICATE MARKING TESTS ====================

    @Nested
    @DisplayName("Duplicate Marking Tests")
    class DuplicateMarkingTests {

        @Test
        @DisplayName("Should mark issue as duplicate of another")
        void shouldMarkAsDuplicate() {
            // Arrange
            Issue original = new Issue("Original issue title", "Original description", reporter);
            Issue duplicate = new Issue("Duplicate issue title", "Duplicate description", reporter);
            
            // We need to simulate IDs being set (normally done by JPA)
            // For this test, we'll rely on object identity checks
            
            // Act
            duplicate.markAsDuplicateOf(original);

            // Assert
            assertEquals(IssueStatus.CLOSED, duplicate.getStatus());
            assertNotNull(duplicate.getClosedAt());
        }

        @Test
        @DisplayName("Should throw exception when marking null as original")
        void shouldThrowExceptionForNullOriginal() {
            // Arrange
            Issue duplicate = new Issue("Duplicate issue title", "Description", reporter);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> duplicate.markAsDuplicateOf(null)
            );
            assertEquals("Original issue is not valid.", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when marking same issue as duplicate of itself")
        void shouldThrowExceptionForSelfDuplicate() {
            // Arrange
            Issue issue = new Issue("Issue title for test", "Description", reporter);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> issue.markAsDuplicateOf(issue)
            );
            assertEquals("An issue cannot be a duplicate of itself.", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when marking closed issue as duplicate")
        void shouldThrowExceptionForClosedIssueDuplicate() {
            // Arrange
            Issue original = new Issue("Original issue title", "Description", reporter);
            Issue duplicate = new Issue("Duplicate issue title", "Description", reporter);
            duplicate.setStatus(IssueStatus.CLOSED);

            // Act & Assert
            IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> duplicate.markAsDuplicateOf(original)
            );
            assertEquals("The issue is already closed.", exception.getMessage());
        }
    }

    // ==================== PRIORITY TESTS ====================

    @Nested
    @DisplayName("Priority Tests")
    class PriorityTests {

        @Test
        @DisplayName("Should update priority")
        void shouldUpdatePriority() {
            // Arrange
            Issue issue = new Issue("Valid title for issue", "Description", reporter);
            
            // Act
            issue.setPriority(PriorityLevel.HIGH);

            // Assert
            assertEquals(PriorityLevel.HIGH, issue.getPriority());
        }

        @Test
        @DisplayName("Should set priority to LOW")
        void shouldSetPriorityToLow() {
            // Arrange
            Issue issue = new Issue("Valid title for issue", "Description", reporter);
            
            // Act
            issue.setPriority(PriorityLevel.LOW);

            // Assert
            assertEquals(PriorityLevel.LOW, issue.getPriority());
        }
    }

    // ==================== ATTACHMENT TESTS ====================

    @Nested
    @DisplayName("Attachment Tests")
    class AttachmentTests {

        @Test
        @DisplayName("Should set attachment path")
        void shouldSetAttachmentPath() {
            // Arrange
            Issue issue = new Issue("Valid title for issue", "Description", reporter);
            
            // Act
            issue.setAttachmentPath("/uploads/123/screenshot.png");

            // Assert
            assertEquals("/uploads/123/screenshot.png", issue.getAttachmentPath());
        }

        @Test
        @DisplayName("Should clear attachment path")
        void shouldClearAttachmentPath() {
            // Arrange
            Issue issue = new Issue("Valid title for issue", "Description", reporter);
            issue.setAttachmentPath("/uploads/123/screenshot.png");
            
            // Act
            issue.setAttachmentPath(null);

            // Assert
            assertNull(issue.getAttachmentPath());
        }
    }

    // ==================== TITLE UPDATE TESTS ====================

    @Nested
    @DisplayName("Title Update Tests")
    class TitleUpdateTests {

        @Test
        @DisplayName("Should update title with valid value")
        void shouldUpdateTitle() {
            // Arrange
            Issue issue = new Issue("Original title here", "Description", reporter);
            
            // Act
            issue.setTitle("New updated title here");

            // Assert
            assertEquals("New updated title here", issue.getTitle());
        }

        @Test
        @DisplayName("Should throw exception when updating with short title")
        void shouldThrowExceptionForShortTitleUpdate() {
            // Arrange
            Issue issue = new Issue("Original title here", "Description", reporter);

            // Act & Assert
            assertThrows(
                IllegalArgumentException.class,
                () -> issue.setTitle("Short")
            );
        }
    }
}
