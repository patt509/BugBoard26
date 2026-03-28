package com.bugboard.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.bugboard.dto.CreateUserRequest;
import com.bugboard.enums.UserRole;
import com.bugboard.service.AuthService;

import jakarta.ws.rs.core.Response;

@RunWith(MockitoJUnitRunner.class)
public class AuthResourceTest {

   @Mock
   private AuthService authService;

   private AuthResource authResource;

   @Before
   public void setUp() {
      authResource = new AuthResource(authService);
   }

   /**
    * TC1: when password is omitted, API response includes temporaryPassword and
    * note.
    */
   @Test
   @SuppressWarnings("unchecked")
   public void testCreateUser_TC1_WithoutPassword_ReturnsTemporaryPassword() {
      CreateUserRequest request = new CreateUserRequest("new.user@test.com", null, "USER");
      when(authService.createUser("new.user@test.com", UserRole.USER, 1L, null)).thenReturn("TmpPass1234");

      Response response = authResource.createUser(1L, request);

      assertEquals("PostCond failed: status should be 201", 201, response.getStatus());
      Map<String, Object> payload = (Map<String, Object>) response.getEntity();
      assertNotNull("PostCond failed: payload should not be null", payload);
      assertEquals("PostCond failed: message should match", "User created successfully", payload.get("message"));
      assertEquals("PostCond failed: role should match", "USER", payload.get("role"));
      assertEquals("PostCond failed: temporary password should be present", "TmpPass1234", payload.get("temporaryPassword"));
      assertTrue("PostCond failed: note should be present when password is generated", payload.containsKey("note"));
      verify(authService).createUser("new.user@test.com", UserRole.USER, 1L, null);
   }

   /**
    * TC2: when password is provided, API response does not expose
    * temporaryPassword.
    */
   @Test
   @SuppressWarnings("unchecked")
   public void testCreateUser_TC2_WithCustomPassword_DoesNotReturnTemporaryPassword() {
      CreateUserRequest request = new CreateUserRequest("secure.user@test.com", "CustomPass123", "ADMIN");
      when(authService.createUser("secure.user@test.com", UserRole.ADMIN, 1L, "CustomPass123"))
            .thenReturn("CustomPass123");

      Response response = authResource.createUser(1L, request);

      assertEquals("PostCond failed: status should be 201", 201, response.getStatus());
      Map<String, Object> payload = (Map<String, Object>) response.getEntity();
      assertNotNull("PostCond failed: payload should not be null", payload);
      assertEquals("PostCond failed: message should match", "User created successfully", payload.get("message"));
      assertEquals("PostCond failed: role should match", "ADMIN", payload.get("role"));
      assertFalse("PostCond failed: temporaryPassword must be omitted for custom password",
            payload.containsKey("temporaryPassword"));
      assertFalse("PostCond failed: note must be omitted for custom password",
            payload.containsKey("note"));
      verify(authService).createUser("secure.user@test.com", UserRole.ADMIN, 1L, "CustomPass123");
   }

   /**
    * TC3: blank password is treated as omitted and temporaryPassword is returned.
    */
   @Test
   @SuppressWarnings("unchecked")
   public void testCreateUser_TC3_BlankPassword_TreatedAsOptional() {
      CreateUserRequest request = new CreateUserRequest("blank.user@test.com", "   ", "USER");
      when(authService.createUser("blank.user@test.com", UserRole.USER, 1L, null))
            .thenReturn("TmpPass9999");

      Response response = authResource.createUser(1L, request);

      assertEquals("PostCond failed: status should be 201", 201, response.getStatus());
      Map<String, Object> payload = (Map<String, Object>) response.getEntity();
      assertEquals("PostCond failed: temporary password should be present", "TmpPass9999", payload.get("temporaryPassword"));
      verify(authService).createUser("blank.user@test.com", UserRole.USER, 1L, null);
   }
}
