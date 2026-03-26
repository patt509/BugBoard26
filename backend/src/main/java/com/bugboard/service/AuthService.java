package com.bugboard.service;

import com.bugboard.dto.PasswordResetRequestDTO;
import com.bugboard.dto.UserDTO;
import com.bugboard.enums.UserRole;
import com.bugboard.model.PasswordResetRequest;
import com.bugboard.model.User;
import com.bugboard.repository.PasswordResetRequestRepository;
import com.bugboard.repository.UserRepository;
import com.bugboard.security.PasswordHasher;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class AuthService {

   private static final Logger logger = Logger.getLogger(AuthService.class.getName());
   private static final String TEMP_PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
   private static final int TEMP_PASSWORD_LENGTH = 12;

   private final UserRepository userRepository;
   private final PasswordResetRequestRepository resetRequestRepository;

   // CDI requires no-arg constructor for proxy
   protected AuthService() {
      this.userRepository = null;
      this.resetRequestRepository = null;
   }

   @Inject
   public AuthService(UserRepository userRepository, PasswordResetRequestRepository resetRequestRepository) {
      this.userRepository = userRepository;
      this.resetRequestRepository = resetRequestRepository;
   }

   // ==================== ADMIN OPERATIONS ====================

   /**
    * Creates a new user (only admin can call this method).
    * Generates a temporary password that the admin will communicate physically to
    * the employee.
    * 
    * @param email   email of the new user
    * @param role    user role
    * @param adminId the ID of the admin creating the user
    * @return the generated temporary password (to be communicated to the employee)
    */
   @Transactional
   public String createUser(String email, UserRole role, Long adminId) {
      // Validate admin privileges
      User admin = getValidatedAdmin(adminId);
      
      // Verify if inputs are valid
      if (email == null || email.trim().isEmpty()) {
         throw new IllegalArgumentException("Email is required.");
      }
      if (role == null) {
         throw new IllegalArgumentException("User role is required.");
      }

      // Verify that the email is not already registered
      if (userRepository.existsByEmail(email)) {
         throw new IllegalArgumentException("Email already registered.");
      }

      // Generate temporary password
      String tempPassword = generateTemporaryPassword();
      String hashedPassword = PasswordHasher.hash(tempPassword);

      // Create and save the user
      User newUser = new User(email, hashedPassword, role);
      userRepository.save(newUser);

      logger.log(Level.INFO, "Admin {0} created new user with email {1}",
            new Object[] { admin.getEmail(), email });

      // Return the temporary password (admin will communicate it physically)
      return tempPassword;
   }

   /**
    * Admin resets a user's password.
    * 
    * @param userId  the ID of the user whose password will be reset
    * @param adminId the ID of the admin performing the reset
    * @return the new temporary password
    */
   @Transactional
   public String resetUserPassword(Long userId, Long adminId) {
      // Validate admin privileges
      User admin = getValidatedAdmin(adminId);
      
      if (userId == null) {
         throw new IllegalArgumentException("User ID is required.");
      }

      User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found."));

      // Generate new temporary password
      String newTempPassword = generateTemporaryPassword();
      String hashedPassword = PasswordHasher.hash(newTempPassword);

      user.setPassword(hashedPassword);
      user.setFirstLogin(true); // Force username choice on next login
      userRepository.save(user);

      logger.log(Level.INFO, "Admin {0} reset password for user {1}",
            new Object[] { admin.getEmail(), user.getEmail() });

      return newTempPassword;
   }

   /**
    * Admin processes a password reset request.
    * 
    * @param requestId request ID
    * @param approve   true to approve (resets password), false to reject
    * @param adminId   the ID of the admin processing the request
    * @return the new password if approved, null if rejected
    */
   @Transactional
   public String processPasswordResetRequest(Long requestId, boolean approve, Long adminId) {
      // Validate admin privileges
      User admin = getValidatedAdmin(adminId);
      
      if (requestId == null) {
         throw new IllegalArgumentException("Request ID is required.");
      }

      PasswordResetRequest request = resetRequestRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("Reset request not found."));

      if (!request.isPending()) {
         throw new IllegalStateException("Request has already been processed.");
      }

      if (approve) {
         request.markAsCompleted(admin);
         resetRequestRepository.save(request);
         // Actually reset the password
         return resetUserPassword(request.getUser().getId(), adminId);
      } else {
         request.markAsRejected(admin);
         resetRequestRepository.save(request);
         logger.log(Level.INFO, "Admin {0} rejected password reset for user {1}",
               new Object[] { admin.getEmail(), request.getUser().getEmail() });
         return null;
      }
   }

   /**
    * Gets all pending password reset requests (for admin dashboard).
    */
   public List<PasswordResetRequestDTO> getPendingResetRequests() {
      return resetRequestRepository.findAllPending().stream()
            .map(req -> PasswordResetRequestDTO.builder()
                  .id(req.getId())
                  .userId(req.getUser().getId())
                  .userEmail(req.getUser().getEmail())
                  .username(req.getUser().getUsername())
                  .requestedAt(req.getRequestedAt())
                  .status(req.getStatus().toString())
                  .build())
            .toList();
   }

   // ==================== USER OPERATIONS ====================

   /**
    * User login. Returns user data if credentials are correct.
    */
   // TODO: consider adding account lockout after multiple failed attempts
   // TODO: consider adding login with username when profile is finalized
   // (possibility to login with email or username in the same field)
   public Optional<UserDTO> login(String email, char[] rawPassword) {
      try {
         Optional<User> userOpt = userRepository.findByEmail(email);

         if (userOpt.isEmpty()) {
            return Optional.empty();
         }

         User user = userOpt.get();
         boolean passwordValid = PasswordHasher.verify(new String(rawPassword), user.getPassword());

         if (!passwordValid) {
            return Optional.empty();
         }

         // Return the DTO with user info (including if it's first login)
         return Optional.of(UserDTO.builder()
               .id(user.getId())
               .email(user.getEmail())
               .username(user.getUsername())
               .role(user.getRole().toString())
               .firstLogin(user.isFirstLogin())
               .createdAt(user.getCreatedAt())
               .build());

      } finally {
         // Clear password from memory
         Arrays.fill(rawPassword, '\0');
      }
   }

   /**
    * Sets the username chosen by the user on first login.
    */
   @Transactional
   public UserDTO finalizeProfile(Long userId, String chosenUsername) {
      if (userId == null) {
         throw new IllegalArgumentException("User ID is required.");
      }
      if (chosenUsername == null || chosenUsername.trim().isEmpty()) {
         throw new IllegalArgumentException("Username is required.");
      }
      // Verifica che lo username non sia già usato
      if (userRepository.existsByUsername(chosenUsername)) {
         throw new IllegalArgumentException("Username already taken.");
      }

      User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found."));

      // Usa il metodo del domain model che gestisce la logica
      user.finalizeProfile(chosenUsername);
      userRepository.save(user);

      return UserDTO.builder()
            .id(user.getId())
            .email(user.getEmail())
            .username(user.getUsername())
            .role(user.getRole().toString())
            .firstLogin(user.isFirstLogin())
            .createdAt(user.getCreatedAt())
            .build();
   }

   /**
    * User requests password reset.
    * Creates a notification for the admin.
    */
   @Transactional
   public void requestPasswordReset(String email) {
      if (email == null || email.trim().isEmpty()) {
         throw new IllegalArgumentException("Email is required.");
      }
      User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Email not found."));

      // Verifica che non ci sia già una richiesta pendente
      if (resetRequestRepository.hasPendingRequest(user)) {
         throw new IllegalStateException("A password reset request is already pending.");
      }

      // Crea la richiesta
      PasswordResetRequest request = new PasswordResetRequest(user);
      resetRequestRepository.save(request);

      logger.log(Level.INFO, "Password reset requested for user {0}", user.getEmail());
   }

   // ==================== LEGACY METHODS ====================

   /**
    * @deprecated Use createUser() instead
    */
   @Deprecated
   public void registerUser(User user, char[] rawPassword) {
      try {
         String hashedPassword = PasswordHasher.hash(new String(rawPassword));
         user.setPassword(hashedPassword);
         userRepository.save(user);
      } finally {
         Arrays.fill(rawPassword, '\0');
      }
   }

   /**
    * @deprecated Use login() instead
    */
   @Deprecated
   public boolean authenticate(String email, char[] rawPassword) {
      try {
         return userRepository.findByEmail(email)
               .map(user -> PasswordHasher.verify(new String(rawPassword), user.getPassword()))
               .orElse(false);
      } finally {
         Arrays.fill(rawPassword, '\0');
      }
   }

   // ==================== HELPER METHODS ====================

   /**
    * Validates that the given user ID corresponds to an authenticated admin.
    * This method is used internally by the service layer to verify admin privileges.
    * 
    * @param adminId the user ID to validate
    * @throws SecurityException if user is null, not found, or not an admin
    */
   public void validateAdminPrivileges(Long adminId) {
      if (adminId == null) {
         throw new SecurityException("Authentication required.");
      }
      
      User admin = userRepository.findById(adminId)
            .orElseThrow(() -> new SecurityException("User not found."));
      
      if (!admin.isAdmin()) {
         throw new SecurityException("Admin privileges required.");
      }
   }

   /**
    * Internal helper to get admin User after validation.
    * Used by service methods that need to log admin actions.
    */
   private User getValidatedAdmin(Long adminId) {
      validateAdminPrivileges(adminId);
      return userRepository.findById(adminId).orElseThrow();
   }

   private String generateTemporaryPassword() {
      SecureRandom random = new SecureRandom();
      StringBuilder sb = new StringBuilder(TEMP_PASSWORD_LENGTH);
      for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
         int index = random.nextInt(TEMP_PASSWORD_CHARS.length());
         sb.append(TEMP_PASSWORD_CHARS.charAt(index));
      }
      return sb.toString();
   }
}