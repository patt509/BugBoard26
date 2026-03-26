package com.bugboard.service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.junit.MockitoJUnitRunner;

import com.bugboard.dto.DashboardStatsDTO;
import com.bugboard.dto.IssueDTO;
import com.bugboard.enums.IssueStatus;
import com.bugboard.enums.IssueType;
import com.bugboard.enums.PriorityLevel;
import com.bugboard.enums.UserRole;
import com.bugboard.model.Issue;
import com.bugboard.model.User;
import com.bugboard.repository.IssueRepository;
import com.bugboard.repository.UserRepository;

@RunWith(MockitoJUnitRunner.class) // Lenient runner to avoid unnecessary stubbing errors
public class IssueServiceTest {

   @Mock
   private IssueRepository repository; // Simulate the database layer with a mock

   @Mock
   private UserRepository userRepository; // Mock for user validation

   @InjectMocks
   private IssueService issueService; // Inject mocks into IssueService, the real service being tested

   private User admin;
   private User normalUser;
   private User reporter;

   @Before
   public void setUp() {
      // Initialize pre-conditions for tests
      admin = spy(new User("admin@test.com", "password", UserRole.ADMIN));
      normalUser = spy(new User("user@test.com", "password", UserRole.USER));
      reporter = spy(new User("reporter@test.com", "password", UserRole.USER));
   }

   /**
    * TC1: Success scenario - Admin marks an open issue as duplicate of another
    * issue.
    * Expected Output: None
    * PostConditions: Issue status changes to CLOSED, originalIssue is linked,
    * closedAt is set to current date/time.
    */
   @Test
   public void testProcessDuplicate_TC1_Success() {
      // Arrange
      Issue duplicate = new Issue("Title for Issue A (DUPLICATE", "Description A", reporter, IssueType.BUG);
      Issue original = new Issue("Title for Issue B (ORIGINAL)", "Description B", reporter, IssueType.BUG);

      Issue spyDuplicate = spy(duplicate);
      Issue spyOriginal = spy(original);

      when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
      when(repository.findById(10L)).thenReturn(spyDuplicate);
      when(repository.findById(20L)).thenReturn(spyOriginal);

      // Act
      issueService.processDuplicate(10L, 20L, 1L);

      // Assert
      assertEquals("PostCond failed: Status should be CLOSED", IssueStatus.CLOSED, spyDuplicate.getStatus());
      assertNotNull("PostCond failed: closedAt should be set", spyDuplicate.getClosedAt());
      verify(repository).save(spyDuplicate);
   }

   /**
    * TC2: Failure scenario - Admin ID is null.
    * Expected Output: SecurityException
    */
   @Test(expected = SecurityException.class)
   public void testProcessDuplicate_TC2_NullAdminId() {
      issueService.processDuplicate(10L, 20L, null);
   }

   /**
    * TC3: Failure scenario - Admin ID not found in database.
    * Expected Output: SecurityException
    */
   @Test(expected = SecurityException.class)
   public void testProcessDuplicate_TC3_AdminNotFound() {
      when(userRepository.findById(999L)).thenReturn(Optional.empty());
      issueService.processDuplicate(10L, 20L, 999L);
   }

   /**
    * TC4: Failure scenario - Non-admin user attempts to mark an issue as duplicate.
    * Expected Output: SecurityException
    */
   @Test(expected = SecurityException.class)
   public void testProcessDuplicate_TC4_NonAdminUser() {
      when(userRepository.findById(2L)).thenReturn(Optional.of(normalUser));
      issueService.processDuplicate(10L, 20L, 2L);
   }

   /**
    * TC5: Failure scenario - Duplicate issue ID is null.
    * Expected Output: IllegalArgumentException
    */
   @Test(expected = IllegalArgumentException.class)
   public void testProcessDuplicate_TC5_NullDuplicateID() {
      when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
      issueService.processDuplicate(null, 20L, 1L);
   }

   /**
    * TC6: Failure scenario - Original issue ID is null.
    * Expected Output: IllegalArgumentException
    */
   @Test(expected = IllegalArgumentException.class)
   public void testProcessDuplicate_TC6_NullOriginalID() {
      when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
      issueService.processDuplicate(10L, null, 1L);
   }

   /**
    * TC7: Failure scenario - Duplicate issue not found in repository.
    * Expected Output: IllegalArgumentException
    */
   @Test(expected = IllegalArgumentException.class)
   public void testProcessDuplicate_TC7_DuplicateIssueNotFound() {
      User testReporter = new User("test@test.com", "pass", UserRole.USER);
      Issue original = new Issue("Original Issue Title", "Desc", testReporter, IssueType.BUG);
      
      when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
      when(repository.findById(10L)).thenReturn(null);
      when(repository.findById(20L)).thenReturn(original);
      issueService.processDuplicate(10L, 20L, 1L);
   }

   /**
    * TC8: Failure scenario - Original issue not found in repository.
    * Expected Output: IllegalArgumentException
    */
   @Test(expected = IllegalArgumentException.class)
   public void testProcessDuplicate_TC8_OriginalIssueNotFound() {
      User testReporter = new User("test@test.com", "pass", UserRole.USER);
      Issue duplicate = new Issue("Title for Issue A", "Description A", testReporter, IssueType.BUG);

      when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
      when(repository.findById(10L)).thenReturn(duplicate);
      when(repository.findById(20L)).thenReturn(null);
      issueService.processDuplicate(10L, 20L, 1L);
   }

   /**
    * TC9: Failure scenario - Attempting to mark an issue as duplicate of itself.
    * Expected Output: IllegalArgumentException
    */
   @Test(expected = IllegalArgumentException.class)
   public void testProcessDuplicate_TC9_SelfDuplication() {
      Issue issue = new Issue("Title must be long enough", "Description", reporter, IssueType.BUG);

      when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

      issueService.processDuplicate(10L, 10L, 1L);
   }

   /**
    * TC10: Failure scenario - Attempting to mark an already closed issue as
    * duplicate.
    * Expected Output: IllegalStateException
    */
   @Test(expected = IllegalStateException.class)
   public void testProcessDuplicate_TC10_AlreadyClosed() {
      Issue alreadyClosedIssue = new Issue("Title for Closed Issue", "Description", reporter, IssueType.BUG);
      alreadyClosedIssue.setStatus(IssueStatus.CLOSED);
      Issue original = new Issue("Title for Original Issue", "Description", reporter, IssueType.BUG);

      Issue spyClosed = spy(alreadyClosedIssue);
      Issue spyOriginal = spy(original);

      when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
      when(repository.findById(10L)).thenReturn(spyClosed);
      when(repository.findById(20L)).thenReturn(spyOriginal);

      issueService.processDuplicate(10L, 20L, 1L);
   }

   /**
    * TC11: Failure scenario - Both duplicate and original issues not found in repository.
    * Expected Output: IllegalArgumentException
    */
   @Test(expected = IllegalArgumentException.class)
   public void testProcessDuplicate_TC11_BothIssuesNotFound() {
      when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
      when(repository.findById(10L)).thenReturn(null);
      when(repository.findById(20L)).thenReturn(null);
      issueService.processDuplicate(10L, 20L, 1L);
   }

   /**
    * TC12: Failure scenario - Attempting to mark an already resolved issue as
    * duplicate. A resolved issue is also considered closed.
    * Expected Output: IllegalStateException
    */
   @Test(expected = IllegalStateException.class)
   public void testProcessDuplicate_TC12_AlreadyResolved() {
      Issue alreadyResolvedIssue = new Issue("Title for Resolved Issue", "Description", reporter, IssueType.BUG);
      alreadyResolvedIssue.setStatus(IssueStatus.RESOLVED);
      Issue original = new Issue("Title for Original Issue", "Description", reporter, IssueType.BUG);

      Issue spyResolved = spy(alreadyResolvedIssue);
      Issue spyOriginal = spy(original);

      when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
      when(repository.findById(10L)).thenReturn(spyResolved);
      when(repository.findById(20L)).thenReturn(spyOriginal);

      issueService.processDuplicate(10L, 20L, 1L);
   }

   /**
    * TC13: Failure scenario - Duplicate issue is not a BUG.
    * Expected Output: IllegalArgumentException
    */
   @Test(expected = IllegalArgumentException.class)
   public void testProcessDuplicate_TC13_DuplicateNotBug() {
      Issue duplicate = new Issue("Title for Feature Issue", "Description", reporter, IssueType.FEATURE);
      Issue original = new Issue("Title for Original Bug", "Description", reporter, IssueType.BUG);

      when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
      when(repository.findById(10L)).thenReturn(duplicate);
      when(repository.findById(20L)).thenReturn(original);

      issueService.processDuplicate(10L, 20L, 1L);
   }

   /**
    * TC14: Failure scenario - Original issue is not a BUG.
    * Expected Output: IllegalArgumentException
    */
   @Test(expected = IllegalArgumentException.class)
   public void testProcessDuplicate_TC14_OriginalNotBug() {
      Issue duplicate = new Issue("Title for Duplicate Bug", "Description", reporter, IssueType.BUG);
      Issue original = new Issue("Title for Feature Original", "Description", reporter, IssueType.FEATURE);

      when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
      when(repository.findById(10L)).thenReturn(duplicate);
      when(repository.findById(20L)).thenReturn(original);

      issueService.processDuplicate(10L, 20L, 1L);
   }

   // ==================== searchIssues TESTS ====================

   /**
    * TC12: Search issues filtering by IssueType only.
    * Expected Output: List of IssueDTO matching the type filter.
    */
   @Test
   public void testSearchIssues_TC12_FilterByType() {
      // Arrange
      Issue bugIssue = spy(new Issue("Bug title long enough", "Bug description", reporter, IssueType.BUG));
      when(repository.search(null, null, null, IssueType.BUG, null))
            .thenReturn(List.of(bugIssue));

      // Act
      List<IssueDTO> results = issueService.searchIssues(null, null, null, IssueType.BUG, null);

      // Assert
      assertEquals("PostCond failed: should return 1 issue", 1, results.size());
      assertEquals("PostCond failed: type should be BUG", "BUG", results.get(0).getType());
   }

   /**
    * TC13: Search issues filtering by assigneeId only.
    * Expected Output: List of IssueDTO assigned to the given user.
    */
   @Test
   public void testSearchIssues_TC13_FilterByAssignee() {
      // Arrange
      Issue assigned = spy(new Issue("Assigned issue title", "Description assigned", reporter, IssueType.FEATURE));
      assigned.setAssignee(normalUser);
      when(repository.search(null, null, null, null, 2L))
            .thenReturn(List.of(assigned));

      // Act
      List<IssueDTO> results = issueService.searchIssues(null, null, null, null, 2L);

      // Assert
      assertEquals("PostCond failed: should return 1 issue", 1, results.size());
   }

   /**
    * TC14: Search with all filters combined returns empty when nothing matches.
    * Expected Output: Empty list.
    */
   @Test
   public void testSearchIssues_TC14_AllFiltersCombinedNoMatch() {
      // Arrange
      when(repository.search("xyz", PriorityLevel.CRITICAL, IssueStatus.TODO, IssueType.DOCUMENTATION, 999L))
            .thenReturn(List.of());

      // Act
      List<IssueDTO> results = issueService.searchIssues("xyz", PriorityLevel.CRITICAL, IssueStatus.TODO,
            IssueType.DOCUMENTATION, 999L);

      // Assert
      assertTrue("PostCond failed: result list should be empty", results.isEmpty());
   }

   /**
    * TC14b: Search trims query and forwards normalized term to repository.
    * Expected Output: repository is called with trimmed term.
    */
   @Test
   public void testSearchIssues_TC14b_TrimsQuery() {
      Issue bugIssue = spy(new Issue("Bug title long enough", "Bug description", reporter, IssueType.BUG));
      when(repository.search("bug", null, null, null, null)).thenReturn(List.of(bugIssue));

      List<IssueDTO> results = issueService.searchIssues("  bug  ", null, null, null, null);

      assertEquals("PostCond failed: should return 1 issue", 1, results.size());
      verify(repository).search(eq("bug"), eq(null), eq(null), eq(null), eq(null));
   }

   // ==================== getDashboardStats TESTS ====================

   /**
    * TC15: Dashboard stats include issuesAssignedPerUser.
    * Expected Output: DashboardStatsDTO with populated issuesAssignedPerUser map.
    */
   @Test
   public void testGetDashboardStats_TC15_IssuesAssignedPerUser() {
      // Arrange
      when(repository.countByStatusGrouped()).thenReturn(List.of(
            new Object[] { IssueStatus.TODO, 0L },
            new Object[] { IssueStatus.IN_PROGRESS, 0L },
            new Object[] { IssueStatus.RESOLVED, 0L },
            new Object[] { IssueStatus.CLOSED, 0L }));
      when(repository.countByPriorityGrouped()).thenReturn(List.of(
            new Object[] { PriorityLevel.LOW, 0L },
            new Object[] { PriorityLevel.MEDIUM, 0L },
            new Object[] { PriorityLevel.HIGH, 0L },
            new Object[] { PriorityLevel.CRITICAL, 0L }));
      when(repository.countAll()).thenReturn(5L);
      when(repository.countDuplicates()).thenReturn(0L);
      when(repository.getIssuesCreatedPerDaySince(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
      when(repository.getAverageResolutionTimeHours()).thenReturn(0.0);
      when(repository.countCreatedToday()).thenReturn(0L);
      when(repository.countClosedToday()).thenReturn(0L);
      when(userRepository.findAssignableUsers()).thenReturn(List.of(
            finalizedUser("user1@test.com", "user1"),
            finalizedUser("user2@test.com", "user2")));

      // Simulate 2 users with open assigned issues
      Object[] row1 = new Object[] { "user1", 3L };
      Object[] row2 = new Object[] { "user2", 1L };
      when(repository.countOpenIssuesPerAssignee()).thenReturn(Arrays.asList(row1, row2));

      // Act
      DashboardStatsDTO stats = issueService.getDashboardStats();

      // Assert
      assertNotNull("PostCond failed: issuesAssignedPerUser should not be null", stats.getIssuesAssignedPerUser());
      assertEquals("PostCond failed: should have 2 entries", 2, stats.getIssuesAssignedPerUser().size());
      assertEquals("PostCond failed: user1 should have 3 issues", Long.valueOf(3L), stats.getIssuesAssignedPerUser().get("user1"));
      assertEquals("PostCond failed: user2 should have 1 issue", Long.valueOf(1L), stats.getIssuesAssignedPerUser().get("user2"));
   }

   // ==================== createIssue(String, String, Long, IssueType) TESTS ====================

   /**
    * TC16: createIssue with non-null reporterId finds the reporter.
    */
   @Test
   public void testCreateIssue_TC16_WithReporter() {
      // Arrange
      when(userRepository.findById(1L)).thenReturn(Optional.of(reporter));

      // Act
      issueService.createIssue("Valid title for new issue", "Description", 1L, IssueType.BUG);

      // Assert
      verify(repository).save(org.mockito.ArgumentMatchers.any(Issue.class));
   }

   /**
    * TC17: createIssue with null reporterId creates issue with null reporter.
    */
   @Test
   public void testCreateIssue_TC17_NullReporter() {
      // Arrange - no stubbing needed for null reporterId

      // Act
      issueService.createIssue("Valid title for new issue", "Description", null, IssueType.FEATURE);

      // Assert
      verify(repository).save(org.mockito.ArgumentMatchers.any(Issue.class));
   }

   // ==================== createIssue(IssueDTO, Long) TESTS ====================

   /**
    * TC18: createIssue from DTO with all fields including assignee.
    */
   @Test
   public void testCreateIssueDTO_TC18_WithPriorityAndAssignee() {
      // Arrange
      IssueDTO dto = IssueDTO.builder()
            .title("Valid title from DTO test")
            .description("DTO description")
            .type("BUG")
            .priority("CRITICAL")
            .assigneeUsername("assigneeUser")
            .build();

      when(userRepository.findById(1L)).thenReturn(Optional.of(reporter));
      when(userRepository.findByUsername("assigneeUser")).thenReturn(Optional.of(normalUser));

      // Act
      issueService.createIssue(dto, 1L);

      // Assert
      verify(repository).save(org.mockito.ArgumentMatchers.any(Issue.class));
   }

   /**
    * TC19: createIssue from DTO without priority (null priority branch).
    */
   @Test
   public void testCreateIssueDTO_TC19_NullPriority() {
      // Arrange
      IssueDTO dto = IssueDTO.builder()
            .title("Valid title from DTO test")
            .description("DTO description")
            .type("FEATURE")
            .build();

      when(userRepository.findById(1L)).thenReturn(Optional.of(reporter));

      // Act
      issueService.createIssue(dto, 1L);

      // Assert
      verify(repository).save(org.mockito.ArgumentMatchers.any(Issue.class));
   }

   /**
    * TC20: createIssue from DTO with blank assignee (skips assignee lookup).
    */
   @Test
   public void testCreateIssueDTO_TC20_BlankAssignee() {
      // Arrange
      IssueDTO dto = IssueDTO.builder()
            .title("Valid title from DTO test")
            .description("DTO description")
            .type("BUG")
            .assigneeUsername("   ")
            .build();

      when(userRepository.findById(1L)).thenReturn(Optional.of(reporter));

      // Act
      issueService.createIssue(dto, 1L);

      // Assert
      verify(repository).save(org.mockito.ArgumentMatchers.any(Issue.class));
   }

   /**
    * TC21: createIssue from DTO with unknown assignee throws IllegalArgumentException.
    */
   @Test(expected = IllegalArgumentException.class)
   public void testCreateIssueDTO_TC21_UnknownAssignee() {
      // Arrange
      IssueDTO dto = IssueDTO.builder()
            .title("Valid title from DTO test")
            .description("DTO description")
            .type("BUG")
            .assigneeUsername("nonExistentUser")
            .build();

      when(userRepository.findById(1L)).thenReturn(Optional.of(reporter));
      when(userRepository.findByUsername("nonExistentUser")).thenReturn(Optional.empty());

      // Act
      issueService.createIssue(dto, 1L);
   }

   /**
    * TC22: createIssue from DTO with null type (passed to Issue constructor which throws).
    */
   @Test(expected = IllegalArgumentException.class)
   public void testCreateIssueDTO_TC22_NullType() {
      // Arrange
      IssueDTO dto = IssueDTO.builder()
            .title("Valid title from DTO test")
            .description("DTO description")
            .build();

      when(userRepository.findById(1L)).thenReturn(Optional.of(reporter));

      // Act
      issueService.createIssue(dto, 1L);
   }

   /**
    * TC23: createIssue from DTO with null reporterId.
    */
   @Test
   public void testCreateIssueDTO_TC23_NullReporterId() {
      // Arrange
      IssueDTO dto = IssueDTO.builder()
            .title("Valid title from DTO test")
            .description("DTO description")
            .type("DOCUMENTATION")
            .build();

      // Act
      issueService.createIssue(dto, null);

      // Assert
      verify(repository).save(org.mockito.ArgumentMatchers.any(Issue.class));
   }

   /**
    * TC23b: createIssue from DTO with assigneeId resolves assignee by ID.
    */
   @Test
   public void testCreateIssueDTO_TC23b_AssigneeId() {
      IssueDTO dto = IssueDTO.builder()
            .title("Valid title from DTO assignee id")
            .description("DTO description")
            .type("BUG")
            .assigneeId(2L)
            .build();

      when(userRepository.findById(1L)).thenReturn(Optional.of(reporter));
      when(userRepository.findById(2L)).thenReturn(Optional.of(normalUser));

      issueService.createIssue(dto, 1L);

      verify(repository).save(org.mockito.ArgumentMatchers.any(Issue.class));
   }

   // ==================== updateStatus TESTS ====================

   /**
    * TC24: updateStatus on existing issue succeeds.
    */
   @Test
   public void testUpdateStatus_TC24_Success() {
      // Arrange
      Issue issue = spy(new Issue("Valid title for status", "Desc", reporter, IssueType.BUG));
      when(repository.findById(1L)).thenReturn(issue);

      // Act
      issueService.updateStatus(1L, IssueStatus.IN_PROGRESS);

      // Assert
      assertEquals("PostCond failed: status should be IN_PROGRESS", IssueStatus.IN_PROGRESS, issue.getStatus());
      verify(repository).save(issue);
   }

   /**
    * TC25: updateStatus on non-existent issue throws IllegalArgumentException.
    */
   @Test(expected = IllegalArgumentException.class)
   public void testUpdateStatus_TC25_IssueNotFound() {
      // Arrange
      when(repository.findById(999L)).thenReturn(null);

      // Act
      issueService.updateStatus(999L, IssueStatus.CLOSED);
   }

   // ==================== setAttachmentPath TESTS ====================

   /**
    * TC26: setAttachmentPath on existing issue succeeds.
    */
   @Test
   public void testSetAttachmentPath_TC26_Success() {
      // Arrange
      Issue issue = spy(new Issue("Valid title for attach", "Desc", reporter, IssueType.BUG));
      when(repository.findById(1L)).thenReturn(issue);

      // Act
      issueService.setAttachmentPath(1L, "/uploads/test.png");

      // Assert
      assertEquals("PostCond failed: attachmentPath should be set", "/uploads/test.png", issue.getAttachmentPath());
      verify(repository).save(issue);
   }

   /**
    * TC27: setAttachmentPath on non-existent issue throws IllegalArgumentException.
    */
   @Test(expected = IllegalArgumentException.class)
   public void testSetAttachmentPath_TC27_IssueNotFound() {
      // Arrange
      when(repository.findById(999L)).thenReturn(null);

      // Act
      issueService.setAttachmentPath(999L, "/uploads/test.png");
   }

   // ==================== removeAttachment TESTS ====================

   /**
    * TC28: removeAttachment on existing issue returns old path.
    */
   @Test
   public void testRemoveAttachment_TC28_Success() {
      // Arrange
      Issue issue = spy(new Issue("Valid title for remove att", "Desc", reporter, IssueType.BUG));
      issue.setAttachmentPath("/uploads/old.png");
      when(repository.findById(1L)).thenReturn(issue);

      // Act
      String oldPath = issueService.removeAttachment(1L);

      // Assert
      assertEquals("PostCond failed: should return old path", "/uploads/old.png", oldPath);
      assertNull("PostCond failed: attachmentPath should be null after removal", issue.getAttachmentPath());
      verify(repository).save(issue);
   }

   /**
    * TC29: removeAttachment on non-existent issue throws IllegalArgumentException.
    */
   @Test(expected = IllegalArgumentException.class)
   public void testRemoveAttachment_TC29_IssueNotFound() {
      // Arrange
      when(repository.findById(999L)).thenReturn(null);

      // Act
      issueService.removeAttachment(999L);
   }

   // ==================== validateIssueExists TESTS ====================

   /**
    * TC30: validateIssueExists returns true when issue exists.
    */
   @Test
   public void testValidateIssueExists_TC30_Exists() {
      // Arrange
      Issue issue = new Issue("Valid title for validate", "Desc", reporter, IssueType.BUG);
      when(repository.findById(1L)).thenReturn(issue);

      // Act
      boolean result = issueService.validateIssueExists(1L);

      // Assert
      assertTrue("PostCond failed: should return true for existing issue", result);
   }

   /**
    * TC31: validateIssueExists returns false when issue does not exist.
    */
   @Test
   public void testValidateIssueExists_TC31_NotExists() {
      // Arrange
      when(repository.findById(999L)).thenReturn(null);

      // Act
      boolean result = issueService.validateIssueExists(999L);

      // Assert
      assertFalse("PostCond failed: should return false for non-existing issue", result);
   }

   // ==================== getIssueAttachmentPath TESTS ====================

   /**
    * TC32: getIssueAttachmentPath on existing issue returns path.
    */
   @Test
   public void testGetIssueAttachmentPath_TC32_Success() {
      // Arrange
      Issue issue = new Issue("Valid title for get path", "Desc", reporter, IssueType.BUG);
      issue.setAttachmentPath("/uploads/doc.pdf");
      when(repository.findById(1L)).thenReturn(issue);

      // Act
      String path = issueService.getIssueAttachmentPath(1L);

      // Assert
      assertEquals("PostCond failed: should return attachment path", "/uploads/doc.pdf", path);
   }

   /**
    * TC33: getIssueAttachmentPath on non-existent issue throws IllegalArgumentException.
    */
   @Test(expected = IllegalArgumentException.class)
   public void testGetIssueAttachmentPath_TC33_IssueNotFound() {
      // Arrange
      when(repository.findById(999L)).thenReturn(null);

      // Act
      issueService.getIssueAttachmentPath(999L);
   }

   // ==================== issueHasAttachment TESTS ====================

   /**
    * TC34: issueHasAttachment returns true when attachment exists.
    */
   @Test
   public void testIssueHasAttachment_TC34_HasAttachment() {
      // Arrange
      Issue issue = new Issue("Valid title for has attach", "Desc", reporter, IssueType.BUG);
      issue.setAttachmentPath("/uploads/file.png");
      when(repository.findById(1L)).thenReturn(issue);

      // Act
      boolean result = issueService.issueHasAttachment(1L);

      // Assert
      assertTrue("PostCond failed: should return true when attachment present", result);
   }

   /**
    * TC35: issueHasAttachment returns false when no attachment.
    */
   @Test
   public void testIssueHasAttachment_TC35_NoAttachment() {
      // Arrange
      Issue issue = new Issue("Valid title for no attach", "Desc", reporter, IssueType.BUG);
      when(repository.findById(1L)).thenReturn(issue);

      // Act
      boolean result = issueService.issueHasAttachment(1L);

      // Assert
      assertFalse("PostCond failed: should return false when no attachment", result);
   }

   // ==================== getAllIssues TESTS ====================

   /**
    * TC36: getAllIssues returns converted DTOs.
    */
   @Test
   public void testGetAllIssues_TC36_ReturnsList() {
      // Arrange
      Issue issue1 = spy(new Issue("First issue title long", "Desc1", reporter, IssueType.BUG));
      Issue issue2 = spy(new Issue("Second issue title long", "Desc2", reporter, IssueType.FEATURE));
      when(repository.findAll()).thenReturn(List.of(issue1, issue2));

      // Act
      List<IssueDTO> result = issueService.getAllIssues();

      // Assert
      assertEquals("PostCond failed: should return 2 DTOs", 2, result.size());
   }

   // ==================== getIssueById TESTS ====================

   /**
    * TC37: getIssueById returns DTO for existing issue.
    */
   @Test
   public void testGetIssueById_TC37_Success() {
      // Arrange
      Issue issue = spy(new Issue("Valid title for get by id", "Desc", reporter, IssueType.BUG));
      when(repository.findById(1L)).thenReturn(issue);

      // Act
      IssueDTO dto = issueService.getIssueById(1L);

      // Assert
      assertNotNull("PostCond failed: DTO should not be null", dto);
      assertEquals("PostCond failed: title should match", "Valid title for get by id", dto.getTitle());
   }

   /**
    * TC38: getIssueById throws IllegalArgumentException when issue not found.
    */
   @Test(expected = IllegalArgumentException.class)
   public void testGetIssueById_TC38_NotFound() {
      // Arrange
      when(repository.findById(999L)).thenReturn(null);

      // Act
      issueService.getIssueById(999L);
   }

   // ==================== convertSingleToDTO branch coverage ====================

   /**
    * TC39: convertSingleToDTO with null reporter produces "Unknown" reporterName.
    */
   @Test
   public void testConvertSingleToDTO_TC39_NullReporter() {
      // Arrange - create issue with null reporter
      Issue issue = spy(new Issue("Valid title null reporter", "Desc", null, IssueType.BUG));
      when(repository.findById(1L)).thenReturn(issue);

      // Act
      IssueDTO dto = issueService.getIssueById(1L);

      // Assert
      assertEquals("PostCond failed: reporterName should be 'Unknown'", "Unknown", dto.getReporterName());
   }

   /**
    * TC40: convertSingleToDTO with non-null reporter produces the username.
    */
   @Test
   public void testConvertSingleToDTO_TC40_WithReporter() {
      // Arrange
      when(reporter.getUsername()).thenReturn("reporterUser");
      Issue issue = spy(new Issue("Valid title with reporter", "Desc", reporter, IssueType.BUG));
      when(repository.findById(1L)).thenReturn(issue);

      // Act
      IssueDTO dto = issueService.getIssueById(1L);

      // Assert
      assertEquals("PostCond failed: reporterName should be 'reporterUser'", "reporterUser", dto.getReporterName());
   }

   /**
    * TC41: convertSingleToDTO with non-null assignee produces the assignee username.
    */
   @Test
   public void testConvertSingleToDTO_TC41_WithAssignee() {
      // Arrange
      when(normalUser.getUsername()).thenReturn("assignedUser");
      Issue issue = spy(new Issue("Valid title with assignee", "Desc", reporter, IssueType.BUG));
      issue.setAssignee(normalUser);
      when(repository.findById(1L)).thenReturn(issue);

      // Act
      IssueDTO dto = issueService.getIssueById(1L);

      // Assert
      assertEquals("PostCond failed: assigneeUsername should be 'assignedUser'", "assignedUser", dto.getAssigneeUsername());
   }

   /**
    * TC42: convertSingleToDTO with null assignee produces null assigneeUsername.
    */
   @Test
   public void testConvertSingleToDTO_TC42_NullAssignee() {
      // Arrange
      Issue issue = spy(new Issue("Valid title no assignee", "Desc", reporter, IssueType.BUG));
      when(repository.findById(1L)).thenReturn(issue);

      // Act
      IssueDTO dto = issueService.getIssueById(1L);

      // Assert
      assertNull("PostCond failed: assigneeUsername should be null", dto.getAssigneeUsername());
   }

   // ==================== getDashboardStats additional branch coverage ====================

   /**
    * TC43: getDashboardStats with non-empty daily data covers the for-loop body.
    */
   @Test
   public void testGetDashboardStats_TC43_WithDailyData() {
      // Arrange
      when(repository.countByStatusGrouped()).thenReturn(List.of(
            new Object[] { IssueStatus.TODO, 0L },
            new Object[] { IssueStatus.IN_PROGRESS, 0L },
            new Object[] { IssueStatus.RESOLVED, 0L },
            new Object[] { IssueStatus.CLOSED, 0L }));
      when(repository.countByPriorityGrouped()).thenReturn(List.of(
            new Object[] { PriorityLevel.LOW, 0L },
            new Object[] { PriorityLevel.MEDIUM, 0L },
            new Object[] { PriorityLevel.HIGH, 0L },
            new Object[] { PriorityLevel.CRITICAL, 0L }));
      when(repository.countAll()).thenReturn(3L);
      when(repository.countDuplicates()).thenReturn(0L);
      when(repository.getAverageResolutionTimeHours()).thenReturn(2.5);
      when(repository.countCreatedToday()).thenReturn(1L);
      when(repository.countClosedToday()).thenReturn(0L);
      when(repository.countOpenIssuesPerAssignee()).thenReturn(List.of());
      when(userRepository.findAssignableUsers()).thenReturn(List.of());

      // Non-empty daily data to cover the for-loop body
      Object[] day1 = new Object[] { "2026-03-06", 2L };
      Object[] day2 = new Object[] { "2026-03-07", 1L };
      when(repository.getIssuesCreatedPerDaySince(org.mockito.ArgumentMatchers.any()))
            .thenReturn(Arrays.asList(day1, day2));

      // Act
      DashboardStatsDTO stats = issueService.getDashboardStats();

      // Assert
      assertNotNull("PostCond failed: issuesCreatedPerDay should not be null", stats.getIssuesCreatedPerDay());
      assertEquals("PostCond failed: should have 2 daily entries", 2, stats.getIssuesCreatedPerDay().size());
      assertEquals("PostCond failed: day1 should have 2 issues", Long.valueOf(2L), stats.getIssuesCreatedPerDay().get("2026-03-06"));
      assertEquals("PostCond failed: day2 should have 1 issue", Long.valueOf(1L), stats.getIssuesCreatedPerDay().get("2026-03-07"));
   }

   private User finalizedUser(String email, String username) {
      User user = new User(email, "password", UserRole.USER);
      user.setUsername(username);
      user.setFirstLogin(false);
      return user;
   }
}
