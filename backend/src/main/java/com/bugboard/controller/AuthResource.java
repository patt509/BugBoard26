package com.bugboard.controller;

import com.bugboard.dto.*;
import com.bugboard.enums.UserRole;
import com.bugboard.model.User;
import com.bugboard.repository.UserRepository;
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
 * 1. Admin creates user with POST /auth/admin/users -> receives temporary password
 * 2. Admin communicates credentials physically to the employee
 * 3. Employee logs in with POST /auth/login
 * 4. If firstLogin=true, employee chooses username with PUT /auth/profile/username
 * 5. If password forgotten, requests reset with POST /auth/password-reset-request
 * 6. Admin sees requests with GET /auth/admin/password-reset-requests
 * 7. Admin processes request with POST /auth/admin/password-reset-requests/{id}
 */
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

   private static final Logger logger = Logger.getLogger(AuthResource.class.getName());

   private final AuthService authService;
   private final UserRepository userRepository;

   @Inject
   public AuthResource(AuthService authService, UserRepository userRepository) {
      this.authService = authService;
      this.userRepository = userRepository;
   }

   // ==================== USER ENDPOINTS ====================

   /**
    * User login.
    * @return UserDTO with firstLogin=true if username must be chosen
    */
   @POST
   @Path("/login")
   public Response login(LoginRequest request) {
      try {
         if (request.getEmail() == null || request.getPassword() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
               .entity(Map.of("error", "Email and password are required"))
               .build();
         }

         Optional<UserDTO> userOpt = authService.login(
            request.getEmail(), 
            request.getPassword().toCharArray()
         );

         if (userOpt.isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED)
               .entity(Map.of("error", "Invalid credentials"))
               .build();
         }

         UserDTO user = userOpt.get();
         
         // Se è il primo login, il client dovrà chiedere lo username
         return Response.ok(user).build();

      } catch (Exception e) {
         logger.log(Level.SEVERE, "Login error", e);
         return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(Map.of("error", "Internal server error"))
            .build();
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
            return Response.status(Response.Status.UNAUTHORIZED)
               .entity(Map.of("error", "User not authenticated"))
               .build();
         }

         String username = body.get("username");
         if (username == null || username.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
               .entity(Map.of("error", "Username is required"))
               .build();
         }

         UserDTO updatedUser = authService.finalizeProfile(userId, username.trim());
         return Response.ok(updatedUser).build();

      } catch (IllegalArgumentException e) {
         return Response.status(Response.Status.BAD_REQUEST)
            .entity(Map.of("error", e.getMessage()))
            .build();
      } catch (IllegalStateException e) {
         return Response.status(Response.Status.CONFLICT)
            .entity(Map.of("error", e.getMessage()))
            .build();
      } catch (Exception e) {
         logger.log(Level.SEVERE, "Error setting username", e);
         return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(Map.of("error", "Internal server error"))
            .build();
      }
   }

   /**
    * Request password reset (creates notification for admin).
    */
   @POST
   @Path("/password-reset-request")
   public Response requestPasswordReset(Map<String, String> body) {
      try {
         String email = body.get("email");
         if (email == null || email.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
               .entity(Map.of("error", "Email is required"))
               .build();
         }

         authService.requestPasswordReset(email.trim());
         
         return Response.ok(Map.of(
            "message", "Password reset request submitted. An administrator will process your request."
         )).build();

      } catch (IllegalArgumentException e) {
         // Non rivelare se l'email esiste o meno per sicurezza
         return Response.ok(Map.of(
            "message", "If the email exists, a password reset request has been submitted."
         )).build();
      } catch (IllegalStateException e) {
         return Response.status(Response.Status.CONFLICT)
            .entity(Map.of("error", "A reset request is already pending for this account."))
            .build();
      } catch (Exception e) {
         logger.log(Level.SEVERE, "Error requesting password reset", e);
         return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(Map.of("error", "Internal server error"))
            .build();
      }
   }

   // ==================== ADMIN ENDPOINTS ====================

   /**
    * Admin creates a new user.
    * @return the temporary password to communicate physically to the employee
    */
   @POST
   @Path("/admin/users")
   public Response createUser(
         @HeaderParam("X-User-Id") Long adminId,
         CreateUserRequest request) {
      try {
         User admin = getAuthenticatedAdmin(adminId);

         if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
               .entity(Map.of("error", "Email is required"))
               .build();
         }

         UserRole role = UserRole.USER; // Default
         if (request.getRole() != null) {
            try {
               role = UserRole.valueOf(request.getRole().toUpperCase());
            } catch (IllegalArgumentException e) {
               return Response.status(Response.Status.BAD_REQUEST)
                  .entity(Map.of("error", "Invalid role. Use USER or ADMIN"))
                  .build();
            }
         }

         String tempPassword = authService.createUser(request.getEmail().trim(), role, admin);

         return Response.status(Response.Status.CREATED)
            .entity(Map.of(
               "message", "User created successfully",
               "email", request.getEmail(),
               "temporaryPassword", tempPassword,
               "note", "Communicate this password to the employee in person"
            ))
            .build();

      } catch (SecurityException e) {
         return Response.status(Response.Status.FORBIDDEN)
            .entity(Map.of("error", e.getMessage()))
            .build();
      } catch (IllegalArgumentException e) {
         return Response.status(Response.Status.BAD_REQUEST)
            .entity(Map.of("error", e.getMessage()))
            .build();
      } catch (Exception e) {
         logger.log(Level.SEVERE, "Error creating user", e);
         return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(Map.of("error", "Internal server error"))
            .build();
      }
   }

   /**
    * Admin views pending password reset requests.
    */
   @GET
   @Path("/admin/password-reset-requests")
   public Response getPendingResetRequests(@HeaderParam("X-User-Id") Long adminId) {
      try {
         getAuthenticatedAdmin(adminId); // Verifica che sia admin

         List<PasswordResetRequestDTO> requests = authService.getPendingResetRequests();
         return Response.ok(requests).build();

      } catch (SecurityException e) {
         return Response.status(Response.Status.FORBIDDEN)
            .entity(Map.of("error", e.getMessage()))
            .build();
      } catch (Exception e) {
         logger.log(Level.SEVERE, "Error getting reset requests", e);
         return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(Map.of("error", "Internal server error"))
            .build();
      }
   }

   /**
    * Admin processes a password reset request (approves or rejects).
    * @return the new temporary password if approved
    */
   @POST
   @Path("/admin/password-reset-requests/{requestId}")
   public Response processResetRequest(
         @HeaderParam("X-User-Id") Long adminId,
         @PathParam("requestId") Long requestId,
         Map<String, Boolean> body) {
      try {
         User admin = getAuthenticatedAdmin(adminId);

         Boolean approve = body.get("approve");
         if (approve == null) {
            return Response.status(Response.Status.BAD_REQUEST)
               .entity(Map.of("error", "Field 'approve' (true/false) is required"))
               .build();
         }

         String newPassword = authService.processPasswordResetRequest(requestId, approve, admin);

         if (approve) {
            return Response.ok(Map.of(
               "message", "Password reset approved",
               "newTemporaryPassword", newPassword,
               "note", "Communicate this password to the employee in person"
            )).build();
         } else {
            return Response.ok(Map.of(
               "message", "Password reset request rejected"
            )).build();
         }

      } catch (SecurityException e) {
         return Response.status(Response.Status.FORBIDDEN)
            .entity(Map.of("error", e.getMessage()))
            .build();
      } catch (IllegalArgumentException e) {
         return Response.status(Response.Status.NOT_FOUND)
            .entity(Map.of("error", e.getMessage()))
            .build();
      } catch (IllegalStateException e) {
         return Response.status(Response.Status.CONFLICT)
            .entity(Map.of("error", e.getMessage()))
            .build();
      } catch (Exception e) {
         logger.log(Level.SEVERE, "Error processing reset request", e);
         return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(Map.of("error", "Internal server error"))
            .build();
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
         User admin = getAuthenticatedAdmin(adminId);

         String newPassword = authService.resetUserPassword(userId, admin);

         return Response.ok(Map.of(
            "message", "Password reset successfully",
            "newTemporaryPassword", newPassword,
            "note", "Communicate this password to the employee in person"
         )).build();

      } catch (SecurityException e) {
         return Response.status(Response.Status.FORBIDDEN)
            .entity(Map.of("error", e.getMessage()))
            .build();
      } catch (IllegalArgumentException e) {
         return Response.status(Response.Status.NOT_FOUND)
            .entity(Map.of("error", e.getMessage()))
            .build();
      } catch (Exception e) {
         logger.log(Level.SEVERE, "Error resetting password", e);
         return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(Map.of("error", "Internal server error"))
            .build();
      }
   }

   // ==================== HELPER METHODS ====================

   private User getAuthenticatedAdmin(Long userId) {
      if (userId == null) {
         throw new SecurityException("Authentication required");
      }

      User user = userRepository.findById(userId)
         .orElseThrow(() -> new SecurityException("User not found"));

      if (!user.isAdmin()) {
         throw new SecurityException("Admin privileges required");
      }

      return user;
   }
}
