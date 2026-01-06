package com.bugboard.service;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.bugboard.enums.IssueStatus;
import com.bugboard.enums.UserRole;
import com.bugboard.model.Issue;
import com.bugboard.model.User;
import com.bugboard.repository.IssueRepository;
import com.bugboard.repository.UserRepository;

import java.util.Optional;

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
      Issue duplicate = new Issue("Title for Issue A (DUPLICATE", "Description A", reporter);
      Issue original = new Issue("Title for Issue B (ORIGINAL)", "Description B", reporter);

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
      Issue original = new Issue("Original", "Desc", testReporter);
      
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
      Issue duplicate = new Issue("Title for Issue A", "Description A", testReporter);

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
      Issue issue = new Issue("Title must be long enough", "Description", reporter);
      Issue spyIssue = spy(issue);

      when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
      when(repository.findById(10L)).thenReturn(spyIssue);

      issueService.processDuplicate(10L, 10L, 1L);
   }

   /**
    * TC10: Failure scenario - Attempting to mark an already closed issue as
    * duplicate.
    * Expected Output: IllegalStateException
    */
   @Test(expected = IllegalStateException.class)
   public void testProcessDuplicate_TC10_AlreadyClosed() {
      Issue alreadyClosedIssue = new Issue("Title for Closed Issue", "Description", reporter);
      alreadyClosedIssue.setStatus(IssueStatus.CLOSED);
      Issue original = new Issue("Title for Original Issue", "Description", reporter);

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
}