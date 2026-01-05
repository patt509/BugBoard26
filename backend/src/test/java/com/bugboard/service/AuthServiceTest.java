package com.bugboard.service;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import com.bugboard.repository.UserRepository;
import com.bugboard.service.AuthService;
import com.bugboard.model.User;
import java.util.Optional;
import static com.bugboard.enums.UserRole.*;

@RunWith(MockitoJUnitRunner.class)
public class AuthServiceTest {

   @Mock
   private UserRepository userRepository;

   @InjectMocks
   private AuthService authService;

   private User admin;
   private User normalUser;

   @Before
   public void setUp() {
      admin = new User("admin@test.com", "password", ADMIN);
      normalUser = new User("user@test.com", "password", USER);
   }

   /**
    * TC1: Success scenario - Admin creates a new regular USER account.
    * Expected Output: tempPassword (String)
    * PostConditions: User is created and saved in the DB.
    */
   @Test
   public void testCreateUser_TC1_Success_RegularUser() {
      String tempPassword = authService.createUser("user1@test.com", USER, admin);

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
      String tempPassword = authService.createUser("admin2@test.com", ADMIN, admin);

      assertNotNull("PostCond failed: tempPassword should not be null", tempPassword);
      verify(userRepository).save(any(User.class));
   }

   /**
    * TC3: Failure scenario - Admin user parameter is null.
    * Expected Output: IllegalArgumentException
    * PostConditions: No changes to the database.
    */
   @Test(expected = IllegalArgumentException.class)
   public void testCreateUser_TC3_NullAdmin() {
      authService.createUser("u@t.com", USER, null);
   }

   /**
    * TC4: Failure scenario - Email parameter is null or empty.
    * Expected Output: IllegalArgumentException
    * PostConditions: No changes to the database.
    */
   @Test(expected = IllegalArgumentException.class)
   public void testCreateUser_TC4_NullEmail() {
      authService.createUser(null, USER, admin);
   }

   /**
    * TC5: Failure scenario - Email parameter is empty string.
    * Expected Output: IllegalArgumentException
    * PostConditions: No changes to the database.
    */
   @Test(expected = IllegalArgumentException.class)
   public void testCreateUser_TC5_EmptyEmail() {
      authService.createUser("", USER, admin);
   }

   /**
    * TC6: Failure scenario - Role parameter is null.
    * Expected Output: IllegalArgumentException
    * PostConditions: No changes to the database.
    */
   @Test(expected = IllegalArgumentException.class)
   public void testCreateUser_TC6_NullRole() {
      authService.createUser("u@t.com", null, admin);
   }

   /**
    * TC7: Failure scenario - A non-admin user attempts to create a new account.
    * Expected Output: SecurityException
    * PostConditions: No changes to the database.
    */
   @Test(expected = SecurityException.class)
   public void testCreateUser_TC7_NonAdminUser() {
      authService.createUser("u@t.com", USER, normalUser);
   }

   /**
    * TC8: Failure scenario - Email already exists in the database.
    * Expected Output: IllegalArgumentException
    * PostConditions: No changes to the database and no save called.
    */
   @Test
   public void testCreateUser_TC8_DuplicateEmail() {
      // Simulate that the email is already registered
      when(userRepository.existsByEmail("exist@test.com")).thenReturn(true);

      try {
         authService.createUser("exist@test.com", USER, admin);
         fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException e) {
         // expected
      }

      // Ensure no user was saved
      verify(userRepository, never()).save(any(User.class));
   }
}
