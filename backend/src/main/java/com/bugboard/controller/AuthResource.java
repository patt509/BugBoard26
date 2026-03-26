package com.bugboard.controller;

import com.bugboard.dto.*;
import com.bugboard.enums.UserRole;
import com.bugboard.service.AuthService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REST Controller for authentication and user management.
 *
 * User flow:
 * 1. Admin creates user with POST /auth/admin/users -> receives temporary
 * password
 * 2. Admin communicates credentials physically to the employee
 * 3. Employee logs in with POST /auth/login
 * 4. If firstLogin=true, employee chooses username with PUT
 * /auth/profile/username
 * 5. If password forgotten, requests reset with POST
 * /auth/password-reset-request
 * 6. Admin sees requests with GET /auth/admin/password-reset-requests
 * 7. Admin processes request with POST /auth/admin/password-reset-requests/{id}
 */
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

   private static final Logger logger = Logger.getLogger(AuthResource.class.getName());

   private final AuthService authService;

   @Inject
   public AuthResource(AuthService authService) {
      this.authService = authService;
   }

   // ==================== USER ENDPOINTS ====================

   /**
    * User login.
    * 
    * @return UserDTO with firstLogin=true if username must be chosen
    */
   @POST
   @Path("/login")
   public Response login(LoginRequest request) {
      try {
         if (request == null) {
            return ApiResponses.error(Response.Status.BAD_REQUEST, "Request body is required");
         }

         if (request.getEmail() == null || request.getPassword() == null) {
            return ApiResponses.error(Response.Status.BAD_REQUEST, "Email and password are required");
         }

         Optional<UserDTO> userOpt = authService.login(
               request.getEmail(),
               request.getPassword().toCharArray());

         if (userOpt.isEmpty()) {
            return ApiResponses.error(Response.Status.UNAUTHORIZED, "Invalid credentials");
         }

         UserDTO user = userOpt.get();

         // Se è il primo login, il client dovrà chiedere lo username
         return Response.ok(user).build();

      } catch (Exception e) {
         logger.log(Level.SEVERE, "Login error", e);
         return ApiResponses.error(Response.Status.INTERNAL_SERVER_ERROR, "Internal server error");
      }
   }

   /**
    * Sets username on first login.
    */
   @PUT
   @Path("/profile/username")
   public Response setUsername(
         @HeaderParam("X-User-Id") Long userId,
         Map<String, String> body) {
      try {
         if (userId == null) {
            return ApiResponses.error(Response.Status.UNAUTHORIZED, "User not authenticated");
         }

         if (body == null) {
            return ApiResponses.error(Response.Status.BAD_REQUEST, "Request body is required");
         }

         String username = body.get("username");
         if (username == null || username.trim().isEmpty()) {
            return ApiResponses.error(Response.Status.BAD_REQUEST, "Username is required");
         }

         UserDTO updatedUser = authService.finalizeProfile(userId, username.trim());
         return Response.ok(updatedUser).build();

      } catch (IllegalArgumentException e) {
         return ApiResponses.error(Response.Status.BAD_REQUEST, e.getMessage());
      } catch (IllegalStateException e) {
         return ApiResponses.error(Response.Status.CONFLICT, e.getMessage());
      } catch (Exception e) {
         logger.log(Level.SEVERE, "Error setting username", e);
         return ApiResponses.error(Response.Status.INTERNAL_SERVER_ERROR, "Internal server error");
      }
   }

   /**
    * Request password reset (creates notification for admin).
    */
   @POST
   @Path("/password-reset-request")
   public Response requestPasswordReset(Map<String, String> body) {
      try {
         if (body == null) {
            return ApiResponses.error(Response.Status.BAD_REQUEST, "Request body is required");
         }

         String email = body.get("email");
         if (email == null || email.trim().isEmpty()) {
            return ApiResponses.error(Response.Status.BAD_REQUEST, "Email is required");
         }

         authService.requestPasswordReset(email.trim());

         return Response.ok(Map.of(
               "message", "Password reset request submitted. An administrator will process your request.")).build();

      } catch (IllegalArgumentException e) {
         // Non rivelare se l'email esiste o meno per sicurezza
         return Response.ok(Map.of(
               "message", "If the email exists, a password reset request has been submitted.")).build();
      } catch (IllegalStateException e) {
         return ApiResponses.error(Response.Status.CONFLICT, "A reset request is already pending for this account.");
      } catch (Exception e) {
         logger.log(Level.SEVERE, "Error requesting password reset", e);
         return ApiResponses.error(Response.Status.INTERNAL_SERVER_ERROR, "Internal server error");
      }
   }

   // ==================== ADMIN ENDPOINTS ====================

   /**
    * Admin creates a new user.
    * 
    * @return the temporary password to communicate physically to the employee
    */
   @POST
   @Path("/admin/users")
   public Response createUser(
         @HeaderParam("X-User-Id") Long adminId,
         CreateUserRequest request) {
      try {
         // Admin validation is done inside the service method
         if (request == null) {
            return ApiResponses.error(Response.Status.BAD_REQUEST, "Request body is required");
         }

         if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            return ApiResponses.error(Response.Status.BAD_REQUEST, "Email is required");
         }

         UserRole role = UserRole.USER; // Default
         if (request.getRole() != null) {
            try {
               role = UserRole.valueOf(request.getRole().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
               return ApiResponses.error(Response.Status.BAD_REQUEST, "Invalid role. Use USER or ADMIN");
            }
         }

         String tempPassword = authService.createUser(request.getEmail().trim(), role, adminId);

         return Response.status(Response.Status.CREATED)
               .entity(Map.of(
                     "message", "User created successfully",
                     "email", request.getEmail(),
                     "temporaryPassword", tempPassword,
                     "note", "Communicate this password to the employee in person"))
               .build();

      } catch (SecurityException e) {
         return ApiResponses.error(Response.Status.FORBIDDEN, e.getMessage());
      } catch (IllegalArgumentException e) {
         return ApiResponses.error(Response.Status.BAD_REQUEST, e.getMessage());
      } catch (Exception e) {
         logger.log(Level.SEVERE, "Error creating user", e);
         return ApiResponses.error(Response.Status.INTERNAL_SERVER_ERROR, "Internal server error");
      }
   }

   /**
    * Admin views pending password reset requests.
    */
   @GET
   @Path("/admin/password-reset-requests")
   public Response getPendingResetRequests(@HeaderParam("X-User-Id") Long adminId) {
      try {
         authService.validateAdminPrivileges(adminId); // Verifica che sia admin

         List<PasswordResetRequestDTO> requests = authService.getPendingResetRequests();
         return Response.ok(requests).build();

      } catch (SecurityException e) {
         return ApiResponses.error(Response.Status.FORBIDDEN, e.getMessage());
      } catch (Exception e) {
         logger.log(Level.SEVERE, "Error getting reset requests", e);
         return ApiResponses.error(Response.Status.INTERNAL_SERVER_ERROR, "Internal server error");
      }
   }

   /**
    * Admin processes a password reset request (approves or rejects).
    * 
    * @return the new temporary password if approved
    */
   @POST
   @Path("/admin/password-reset-requests/{requestId}")
   public Response processResetRequest(
         @HeaderParam("X-User-Id") Long adminId,
         @PathParam("requestId") Long requestId,
         Map<String, Boolean> body) {
      try {
         // Admin validation is done inside the service method
         if (body == null) {
            return ApiResponses.error(Response.Status.BAD_REQUEST, "Request body is required");
         }

         Boolean approve = body.get("approve");
         if (approve == null) {
            return ApiResponses.error(Response.Status.BAD_REQUEST, "Field 'approve' (true/false) is required");
         }

         String newPassword = authService.processPasswordResetRequest(requestId, approve, adminId);

         if (approve) {
            return Response.ok(Map.of(
                  "message", "Password reset approved",
                  "newTemporaryPassword", newPassword,
                  "note", "Communicate this password to the employee in person")).build();
         } else {
            return Response.ok(Map.of(
                  "message", "Password reset request rejected")).build();
         }

      } catch (SecurityException e) {
         return ApiResponses.error(Response.Status.FORBIDDEN, e.getMessage());
      } catch (IllegalArgumentException e) {
         return ApiResponses.error(Response.Status.NOT_FOUND, e.getMessage());
      } catch (IllegalStateException e) {
         return ApiResponses.error(Response.Status.CONFLICT, e.getMessage());
      } catch (Exception e) {
         logger.log(Level.SEVERE, "Error processing reset request", e);
         return ApiResponses.error(Response.Status.INTERNAL_SERVER_ERROR, "Internal server error");
      }
   }

   /**
    * Admin directly resets a user's password.
    */
   @POST
   @Path("/admin/users/{userId}/reset-password")
   public Response resetUserPassword(
         @HeaderParam("X-User-Id") Long adminId,
         @PathParam("userId") Long userId) {
      try {
         // Admin validation is done inside the service method
         String newPassword = authService.resetUserPassword(userId, adminId);

         return Response.ok(Map.of(
               "message", "Password reset successfully",
               "newTemporaryPassword", newPassword,
               "note", "Communicate this password to the employee in person")).build();

      } catch (SecurityException e) {
         return ApiResponses.error(Response.Status.FORBIDDEN, e.getMessage());
      } catch (IllegalArgumentException e) {
         return ApiResponses.error(Response.Status.NOT_FOUND, e.getMessage());
      } catch (Exception e) {
         logger.log(Level.SEVERE, "Error resetting password", e);
         return ApiResponses.error(Response.Status.INTERNAL_SERVER_ERROR, "Internal server error");
      }
   }
}
