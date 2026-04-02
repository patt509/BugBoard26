package com.bugboard.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.MockitoJUnitRunner;

import com.bugboard.dto.PasswordResetRequestDTO;
import com.bugboard.dto.UserDTO;
import static com.bugboard.enums.UserRole.ADMIN;
import static com.bugboard.enums.UserRole.USER;
import static com.bugboard.enums.UserRole.STAKEHOLDER;
import com.bugboard.model.PasswordResetRequest;
import com.bugboard.model.User;
import com.bugboard.repository.PasswordResetRequestRepository;
import com.bugboard.repository.UserRepository;
import com.bugboard.security.PasswordHasher;

@RunWith(MockitoJUnitRunner.class)
public class AuthServiceTest {

   @Mock
   private UserRepository userRepository;

   @Mock
   private PasswordResetRequestRepository resetRequestRepository;

   @InjectMocks
   private AuthService authService;

   private User admin;
   private User normalUser;
   private User stakeholderUser;

   @Before
   public void setUp() {
      admin = spy(new User("admin@test.com", "password", ADMIN));
      normalUser = spy(new User("user@test.com", "password", USER));
      stakeholderUser = spy(new User("stakeholder@test.com", "password", STAKEHOLDER));
   }

   /* Test on createUser method */

   /**
    * TC1: Success scenario - Admin creates a new regular USER account.
    * Expected Output: tempPassword (String)
    * PostConditions: User is created and saved in the DB.
    */
   @Test
   public void testCreateUser_TC1_Success_RegularUser() {
      when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
      String tempPassword = authService.createUser("user1@test.com", USER, 1L);

      assertNotNull("PostCond failed: tempPassword should not be null", tempPassword);
      // Verify that the userRepository's save method was successfully called to
      // persist the new user
      verify(userRepository).save(any(User.class));
   }

   /**
    * TC2: Success scenario - Admin creates a new ADMIN account.
    * Expected Output: tempPassword (String)
    * PostConditions: Admin user is created and saved in the DB.
    */
   @Test
   public void testCreateUser_TC2_Success_AdminUser() {
      when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
      String tempPassword = authService.createUser("admin2@test.com", ADMIN, 1L);

      assertNotNull("PostCond failed: tempPassword should not be null", tempPassword);
      verify(userRepository).save(any(User.class));
   }

   /**
    * TC3: Failure scenario - Admin ID parameter is null.
    * Expected Output: SecurityException
    * PostConditions: No changes to the database.
    */
   @Test(expected = SecurityException.class)
   public void testCreateUser_TC3_NullAdminId() {
      authService.createUser("u@t.com", USER, null);
   }

   /**
    * TC4: Failure scenario - Admin ID not found in database.
    * Expected Output: SecurityException
    * PostConditions: No changes to the database.
    */
   @Test(expected = SecurityException.class)
   public void testCreateUser_TC4_AdminNotFound() {
      when(userRepository.findById(999L)).thenReturn(Optional.empty());
      authService.createUser("u@t.com", USER, 999L);
   }

   /**
    * TC5/TC6/TC10: Failure scenarios - invalid email inputs (null, empty, malformed).
    * Expected Output: IllegalArgumentException
    * PostConditions: No changes to the database.
    */
   @Test
   public void testCreateUser_InvalidEmailInputs() {
      when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

      for (String invalidEmail : new String[] { null, "", "not-an-email" }) {
         try {
            authService.createUser(invalidEmail, USER, 1L);
            fail("Expected IllegalArgumentException for email: " + invalidEmail);
         } catch (IllegalArgumentException e) {
            // expected
         }
      }
   }

   /**
    * TC7: Failure scenario - Role parameter is null.
    * Expected Output: IllegalArgumentException
    * PostConditions: No changes to the database.
    */
   @Test(expected = IllegalArgumentException.class)
   public void testCreateUser_TC7_NullRole() {
      when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
      authService.createUser("u@t.com", null, 1L);
   }

   /**
    * TC8: Failure scenario - A non-admin user attempts to create a new account.
    * Expected Output: SecurityException
    * PostConditions: No changes to the database.
    */
   @Test(expected = SecurityException.class)
   public void testCreateUser_TC8_NonAdminUser() {
      when(userRepository.findById(2L)).thenReturn(Optional.of(normalUser));
      authService.createUser("u@t.com", USER, 2L);
   }

   /**
    * TC9: Failure scenario - Email already exists in the database.
    * Expected Output: IllegalArgumentException
    * PostConditions: No changes to the database and no save called.
    */
   @Test
   public void testCreateUser_TC9_DuplicateEmail() {
      when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
      // Simulate that the email is already registered
      when(userRepository.existsByEmail("exist@test.com")).thenReturn(true);

      try {
         authService.createUser("exist@test.com", USER, 1L);
         fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException e) {
         // expected
      }

      // Ensure no user was saved
      verify(userRepository, never()).save(any(User.class));
   }

   /**
    * TC11: Success scenario - Email is normalized (trim + lowercase) before persistence checks.
    * Expected Output: tempPassword (String)
    * PostConditions: User is saved with normalized email.
    */
   @Test
   public void testCreateUser_TC11_NormalizesEmail() {
      when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
      when(userRepository.existsByEmail("new.user@test.com")).thenReturn(false);

      String tempPassword = authService.createUser("  New.User@Test.com  ", USER, 1L);

      assertNotNull("PostCond failed: tempPassword should not be null", tempPassword);
      verify(userRepository).existsByEmail("new.user@test.com");
      verify(userRepository).save(any(User.class));
   }

   /**
    * TC12: Success scenario - Admin provides a custom password.
    * Expected Output: the same provided password (not generated temporary one).
    * PostConditions: User is saved with hashed password.
    */
   @Test
   public void testCreateUser_TC12_CustomPasswordProvided() {
      when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
      when(userRepository.existsByEmail("custom@test.com")).thenReturn(false);

      String effectivePassword = authService.createUser("custom@test.com", USER, 1L, "CustomPass123");

      assertEquals("PostCond failed: should return provided custom password", "CustomPass123", effectivePassword);
      ArgumentCaptor<User> savedUserCaptor = ArgumentCaptor.forClass(User.class);
      verify(userRepository).save(savedUserCaptor.capture());
      User savedUser = savedUserCaptor.getValue();
      assertEquals("PostCond failed: persisted email should match", "custom@test.com", savedUser.getEmail());
      assertNotEquals("PostCond failed: persisted password must be hashed", "CustomPass123", savedUser.getPassword());
      assertTrue("PostCond failed: password hash should validate against raw password",
            PasswordHasher.verify("CustomPass123", savedUser.getPassword()));
   }

   /**
    * TC13: Failure scenario - Admin provides blank password.
    * Expected Output: IllegalArgumentException.
    * PostConditions: No changes to the database.
    */
   @Test
   public void testCreateUser_TC13_BlankCustomPassword() {
      when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
      when(userRepository.existsByEmail("blank@test.com")).thenReturn(false);

      try {
         authService.createUser("blank@test.com", USER, 1L, "   ");
         fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException e) {
         assertEquals("PostCond failed: message should explain invalid blank password", "Password is required.",
               e.getMessage());
      }

      verify(userRepository, never()).save(any(User.class));
   }

   /**
    * TC14: Failure scenario - Admin provides short custom password.
    * Expected Output: IllegalArgumentException.
    * PostConditions: No changes to the database.
    */
   @Test
   public void testCreateUser_TC14_ShortCustomPassword() {
      when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
      when(userRepository.existsByEmail("short@test.com")).thenReturn(false);

      try {
         authService.createUser("short@test.com", USER, 1L, "short");
         fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException e) {
         assertEquals("PostCond failed: message should enforce minimum length",
               "Password must be at least 8 characters long.", e.getMessage());
      }

      verify(userRepository, never()).save(any(User.class));
   }

   /* _________________________________________________________________________ */

   /* Test on finalizeProfile method */

   /**
    * TC1: Success scenario - User finalizes profile with valid username.
    * Expected Output: UserDTO with username.
    * PostConditions: User's username is updated in the DB, firstLogin flag is set
    * to false.
    */
   @Test
   public void testFinalizeProfile_TC1_Success() {
      when(userRepository.findById(5L)).thenReturn(Optional.of(normalUser));
      UserDTO user5 = authService.finalizeProfile(5L, "user5");
      assertEquals("PostCond failed: username should be updated", "user5", user5.getUsername());
      assertEquals("PostCond failed: username should be updated", "user5", normalUser.getUsername());
      assertFalse("PostCond failed: firstLogin should be false", normalUser.isFirstLogin());
      verify(userRepository).save(normalUser);
   }

   /**
    * TC2: Failure scenario - ID parameter is null.
    * Expected Output: IllegalArgumentException
    * PostConditions: No changes to the database.
    */
   @Test(expected = IllegalArgumentException.class)
   public void testFinalizeProfile_TC2_NullUsername() {
      authService.finalizeProfile(null, "User5");
   }

   /**
    * TC3: Failure scenario - Username parameter is null.
    * Expected Output: IllegalArgumentException
    * PostConditions: No changes to the database.
    */
   @Test(expected = IllegalArgumentException.class)
   public void testFinalizeProfile_TC3_EmptyUsername() {
      authService.finalizeProfile(5L, null);
   }

   /**
    * TC4: Failure scenario - Username parameter is empty string.
    * Expected Output: IllegalArgumentException
    * PostConditions: No changes to the database.
    */
   @Test(expected = IllegalArgumentException.class)
   public void testFinalizeProfile_TC4_EmptyUsername() {
      authService.finalizeProfile(5L, "");
   }

   /**
    * TC5: Failure scenario - Username already exists in the database.
    * Expected Output: IllegalArgumentException
    * PostConditions: No changes to the database.
    */
   @Test(expected = IllegalArgumentException.class)
   public void testFinalizeProfile_TC5_DuplicateUsername() {
      when(userRepository.existsByUsername("user5")).thenReturn(true);
      authService.finalizeProfile(5L, "user5");
   }

   /**
    * TC6: Failure scenario - User with userId = 5 does not exist in the database.
    * Expected Output: IllegalArgumentException
    * PostConditions: No changes to the database.
    */
   @Test(expected = IllegalArgumentException.class)
   public void testFinalizeProfile_TC6_UserNotFound() {
      when(userRepository.findById(5L)).thenReturn(Optional.empty());
      authService.finalizeProfile(5L, "user5");
   }

   /* _________________________________________________________________________ */

   /* Test on resetUserPassword method */

   /**
    * TC1: Success scenario - Admin resets a user's password.
    * Expected Output: New temporary password (String).
    * PostConditions: User's password is updated and firstLogin set to true.
    */
   @Test
   public void testResetUserPassword_TC1_Success() {
      // Arrange
      when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
      when(userRepository.findById(2L)).thenReturn(Optional.of(normalUser));

      // Act
      String newTempPassword = authService.resetUserPassword(2L, 1L);

      // Assert
      assertNotNull("PostCond failed: temp password should not be null", newTempPassword);
      assertTrue("PostCond failed: firstLogin should be true", normalUser.isFirstLogin());
      verify(userRepository).save(normalUser);
   }

   /**
    * TC2: Failure scenario - User ID is null.
    * Expected Output: IllegalArgumentException.
    */
   @Test(expected = IllegalArgumentException.class)
   public void testResetUserPassword_TC2_NullUserId() {
      // Arrange
      when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

      // Act
      authService.resetUserPassword(null, 1L);
   }

   /**
    * TC3: Failure scenario - User not found in database.
    * Expected Output: IllegalArgumentException.
    */
   @Test(expected = IllegalArgumentException.class)
   public void testResetUserPassword_TC3_UserNotFound() {
      // Arrange
      when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
      when(userRepository.findById(999L)).thenReturn(Optional.empty());

      // Act
      authService.resetUserPassword(999L, 1L);
   }

   /**
    * TC4: Failure scenario - Admin ID is null.
    * Expected Output: SecurityException.
    */
   @Test(expected = SecurityException.class)
   public void testResetUserPassword_TC4_NullAdminId() {
      // Act
      authService.resetUserPassword(2L, null);
   }

   /**
    * TC5: Failure scenario - Non-admin caller.
    * Expected Output: SecurityException.
    */
   @Test(expected = SecurityException.class)
   public void testResetUserPassword_TC5_NonAdmin() {
      // Arrange
      when(userRepository.findById(2L)).thenReturn(Optional.of(normalUser));

      // Act
      authService.resetUserPassword(3L, 2L);
   }

   /* _________________________________________________________________________ */

   /* Test on processPasswordResetRequest method */

   /**
    * TC1: Success scenario - Admin approves a pending request.
    * Expected Output: New temporary password (String).
    * PostConditions: Request marked completed, user password reset.
    */
   @Test
   public void testProcessPasswordResetRequest_TC1_Approve() {
      // Arrange
      when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
      PasswordResetRequest request = mock(PasswordResetRequest.class);
      when(resetRequestRepository.findById(10L)).thenReturn(Optional.of(request));
      when(request.isPending()).thenReturn(true);
      when(request.getUser()).thenReturn(normalUser);
      when(normalUser.getId()).thenReturn(2L);
      when(userRepository.findById(2L)).thenReturn(Optional.of(normalUser));

      // Act
      String result = authService.processPasswordResetRequest(10L, true, 1L);

      // Assert
      assertNotNull("PostCond failed: temp password should not be null", result);
      verify(request).markAsCompleted(admin);
      verify(resetRequestRepository).save(request);
   }

   /**
    * TC2: Success scenario - Admin rejects a pending request.
    * Expected Output: null.
    * PostConditions: Request marked as rejected.
    */
   @Test
   public void testProcessPasswordResetRequest_TC2_Reject() {
      // Arrange
      when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
      PasswordResetRequest request = mock(PasswordResetRequest.class);
      when(resetRequestRepository.findById(10L)).thenReturn(Optional.of(request));
      when(request.isPending()).thenReturn(true);
      User requestUser = mock(User.class);
      when(request.getUser()).thenReturn(requestUser);
      when(requestUser.getEmail()).thenReturn("user@test.com");

      // Act
      String result = authService.processPasswordResetRequest(10L, false, 1L);

      // Assert
      assertNull("PostCond failed: result should be null on reject", result);
      verify(request).markAsRejected(admin);
      verify(resetRequestRepository).save(request);
   }

   /**
    * TC3: Failure scenario - Request ID is null.
    * Expected Output: IllegalArgumentException.
    */
   @Test(expected = IllegalArgumentException.class)
   public void testProcessPasswordResetRequest_TC3_NullRequestId() {
      // Arrange
      when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

      // Act
      authService.processPasswordResetRequest(null, true, 1L);
   }

   /**
    * TC4: Failure scenario - Request not found.
    * Expected Output: IllegalArgumentException.
    */
   @Test(expected = IllegalArgumentException.class)
   public void testProcessPasswordResetRequest_TC4_RequestNotFound() {
      // Arrange
      when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
      when(resetRequestRepository.findById(999L)).thenReturn(Optional.empty());

      // Act
      authService.processPasswordResetRequest(999L, true, 1L);
   }

   /**
    * TC5: Failure scenario - Request already processed (not pending).
    * Expected Output: IllegalStateException.
    */
   @Test(expected = IllegalStateException.class)
   public void testProcessPasswordResetRequest_TC5_AlreadyProcessed() {
      // Arrange
      when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
      PasswordResetRequest request = mock(PasswordResetRequest.class);
      when(resetRequestRepository.findById(10L)).thenReturn(Optional.of(request));
      when(request.isPending()).thenReturn(false);

      // Act
      authService.processPasswordResetRequest(10L, true, 1L);
   }

   /**
    * TC6: Failure scenario - Admin ID is null.
    * Expected Output: SecurityException.
    */
   @Test(expected = SecurityException.class)
   public void testProcessPasswordResetRequest_TC6_NullAdminId() {
      // Act
      authService.processPasswordResetRequest(10L, true, null);
   }

   /* _________________________________________________________________________ */

   /* Test on login method */

   /**
    * TC1: Success scenario - Valid email and password.
    * Expected Output: Optional containing UserDTO.
    * PostConditions: DTO contains all user fields.
    */
   @Test
   public void testLogin_TC1_Success() {
      // Arrange
      String hash = PasswordHasher.hash("correctPassword");
      User userWithHash = spy(new User("login@test.com", hash, USER));
      when(userWithHash.getId()).thenReturn(5L);
      when(userRepository.findByEmail("login@test.com")).thenReturn(Optional.of(userWithHash));

      // Act
      Optional<UserDTO> result = authService.login("login@test.com", "correctPassword".toCharArray());

      // Assert
      assertTrue("PostCond failed: should return a UserDTO", result.isPresent());
      assertEquals("PostCond failed: email should match", "login@test.com", result.get().getEmail());
   }

   /**
    * TC1b: Success scenario - Valid username and password.
    * Expected Output: Optional containing UserDTO.
    * PostConditions: Repository lookup is performed by username.
    */
   @Test
   public void testLogin_TC1b_Success_WithUsername() {
      String hash = PasswordHasher.hash("correctPassword");
      User userWithHash = spy(new User("login@test.com", hash, USER));
      userWithHash.setUsername("loginUser");
      when(userWithHash.getId()).thenReturn(6L);
      when(userRepository.findByUsername("loginUser")).thenReturn(Optional.of(userWithHash));

      Optional<UserDTO> result = authService.login("loginUser", "correctPassword".toCharArray());

      assertTrue("PostCond failed: should return a UserDTO", result.isPresent());
      assertEquals("PostCond failed: username should match", "loginUser", result.get().getUsername());
      verify(userRepository).findByUsername("loginUser");
      verify(userRepository, never()).findByEmail("loginuser");
   }

   /**
    * TC2: Failure scenario - Email not found.
    * Expected Output: Empty Optional.
    */
   @Test
   public void testLogin_TC2_UserNotFound() {
      // Arrange
      when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

      // Act
      Optional<UserDTO> result = authService.login("unknown@test.com", "any".toCharArray());

      // Assert
      assertFalse("PostCond failed: should return empty", result.isPresent());
   }

   /**
    * TC3: Failure scenario - Wrong password.
    * Expected Output: Empty Optional.
    */
   @Test
   public void testLogin_TC3_WrongPassword() {
      // Arrange
      String hash = PasswordHasher.hash("correctPassword");
      User userWithHash = new User("login@test.com", hash, USER);
      when(userRepository.findByEmail("login@test.com")).thenReturn(Optional.of(userWithHash));

      // Act
      Optional<UserDTO> result = authService.login("login@test.com", "wrongPassword".toCharArray());

      // Assert
      assertFalse("PostCond failed: should return empty", result.isPresent());
   }

   /* _________________________________________________________________________ */

   /* Test on getPendingResetRequests method */

   /**
    * TC1: Success scenario - Pending requests exist.
    * Expected Output: List with one PasswordResetRequestDTO.
    */
   @Test
   public void testGetPendingResetRequests_TC1_WithRequests() {
      // Arrange
      PasswordResetRequest req = mock(PasswordResetRequest.class);
      User reqUser = mock(User.class);
      when(req.getId()).thenReturn(1L);
      when(req.getUser()).thenReturn(reqUser);
      when(reqUser.getId()).thenReturn(2L);
      when(reqUser.getEmail()).thenReturn("user@test.com");
      when(reqUser.getUsername()).thenReturn("testuser");
      when(req.getRequestedAt()).thenReturn(LocalDateTime.of(2025, 1, 1, 12, 0));
      when(req.getStatus()).thenReturn(PasswordResetRequest.RequestStatus.PENDING);
      when(resetRequestRepository.findAllPending()).thenReturn(List.of(req));

      // Act
      List<PasswordResetRequestDTO> result = authService.getPendingResetRequests();

      // Assert
      assertEquals("PostCond failed: should contain 1 request", 1, result.size());
      assertEquals("PostCond failed: userEmail should match", "user@test.com", result.get(0).getUserEmail());
   }

   /**
    * TC1: getAssignableUsers returns only users from repository query.
    * Expected Output: list of UserDTO mapped from finalized users.
    */
   @Test
   public void testGetAssignableUsers_TC1_WithUsers() {
      User userA = spy(new User("a@test.com", "password", USER));
      userA.setUsername("alice");
      userA.setFirstLogin(false);

      User userB = spy(new User("b@test.com", "password", USER));
      userB.setUsername("bob");
      userB.setFirstLogin(false);

      when(userRepository.findAssignableUsers()).thenReturn(List.of(userA, userB));

      List<UserDTO> users = authService.getAssignableUsers();

      assertNotNull("PostCond failed: list should not be null", users);
      assertEquals("PostCond failed: should contain 2 assignable users", 2, users.size());
      assertEquals("PostCond failed: first username should be alice", "alice", users.get(0).getUsername());
      assertEquals("PostCond failed: second username should be bob", "bob", users.get(1).getUsername());
   }

   /**
    * TC2: Success scenario - No pending requests.
    * Expected Output: Empty list.
    */
   @Test
   public void testGetPendingResetRequests_TC2_Empty() {
      // Arrange
      when(resetRequestRepository.findAllPending()).thenReturn(Collections.emptyList());

      // Act
      List<PasswordResetRequestDTO> result = authService.getPendingResetRequests();

      // Assert
      assertTrue("PostCond failed: should be empty", result.isEmpty());
   }

   /* _________________________________________________________________________ */

   /* Test on requestPasswordReset method */

   /**
    * TC1: Success scenario - User requests password reset.
    * PostConditions: PasswordResetRequest saved.
    */
   @Test
   public void testRequestPasswordReset_TC1_Success() {
      // Arrange
      when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(normalUser));
      when(resetRequestRepository.hasPendingRequest(normalUser)).thenReturn(false);

      // Act
      authService.requestPasswordReset("user@test.com");

      // Assert
      verify(resetRequestRepository).save(any(PasswordResetRequest.class));
   }

   /**
    * TC2: Failure scenario - Null email.
    * Expected Output: IllegalArgumentException.
    */
   @Test(expected = IllegalArgumentException.class)
   public void testRequestPasswordReset_TC2_NullEmail() {
      // Act
      authService.requestPasswordReset(null);
   }

   /**
    * TC3: Failure scenario - Empty email.
    * Expected Output: IllegalArgumentException.
    */
   @Test(expected = IllegalArgumentException.class)
   public void testRequestPasswordReset_TC3_EmptyEmail() {
      // Act
      authService.requestPasswordReset("");
   }

   /**
    * TC4: Failure scenario - Email not found in database.
    * Expected Output: IllegalArgumentException.
    */
   @Test(expected = IllegalArgumentException.class)
   public void testRequestPasswordReset_TC4_EmailNotFound() {
      // Arrange
      when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

      // Act
      authService.requestPasswordReset("unknown@test.com");
   }

   /**
    * TC5: Failure scenario - Pending request already exists.
    * Expected Output: IllegalStateException.
    */
   @Test(expected = IllegalStateException.class)
   public void testRequestPasswordReset_TC5_AlreadyPending() {
      // Arrange
      when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(normalUser));
      when(resetRequestRepository.hasPendingRequest(normalUser)).thenReturn(true);

      // Act
      authService.requestPasswordReset("user@test.com");
   }

   /* _________________________________________________________________________ */

   /* Test on validateAdminPrivileges method */

   /**
    * TC1: Success scenario - Valid admin.
    * Expected Output: No exception thrown.
    */
   @Test
   public void testValidateAdminPrivileges_TC1_ValidAdmin() {
      // Arrange
      when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

      // Act & Assert (no exception)
      authService.validateAdminPrivileges(1L);
   }

   /**
    * TC2: Failure scenario - Null admin ID.
    * Expected Output: SecurityException.
    */
   @Test(expected = SecurityException.class)
   public void testValidateAdminPrivileges_TC2_NullAdminId() {
      // Act
      authService.validateAdminPrivileges(null);
   }

   /**
    * TC3: Failure scenario - Admin not found.
    * Expected Output: SecurityException.
    */
   @Test(expected = SecurityException.class)
   public void testValidateAdminPrivileges_TC3_AdminNotFound() {
      // Arrange
      when(userRepository.findById(999L)).thenReturn(Optional.empty());

      // Act
      authService.validateAdminPrivileges(999L);
   }

   /**
    * TC4: Failure scenario - User is not an admin.
    * Expected Output: SecurityException.
    */
   @Test(expected = SecurityException.class)
   public void testValidateAdminPrivileges_TC4_NotAdmin() {
      // Arrange
      when(userRepository.findById(2L)).thenReturn(Optional.of(normalUser));

      // Act
      authService.validateAdminPrivileges(2L);
   }


   /* _________________________________________________________________________ */

   /* Test on validateWritableUser method */

   /**
    * TC1: Success scenario - Regular USER can perform write operations.
    * Expected Output: No exception thrown.
    */
   @Test
   public void testValidateWritableUser_TC1_RegularUser() {
      when(userRepository.findById(2L)).thenReturn(Optional.of(normalUser));

      authService.validateWritableUser(2L);
   }

   /**
    * TC2: Failure scenario - STAKEHOLDER account is read-only.
    * Expected Output: SecurityException.
    */
   @Test(expected = SecurityException.class)
   public void testValidateWritableUser_TC2_StakeholderReadOnly() {
      when(userRepository.findById(3L)).thenReturn(Optional.of(stakeholderUser));

      authService.validateWritableUser(3L);
   }

   /**
    * TC3: Failure scenario - User not found.
    * Expected Output: SecurityException.
    */
   @Test(expected = SecurityException.class)
   public void testValidateWritableUser_TC3_UserNotFound() {
      when(userRepository.findById(999L)).thenReturn(Optional.empty());

      authService.validateWritableUser(999L);
   }
}
