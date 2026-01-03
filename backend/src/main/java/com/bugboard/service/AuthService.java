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

   @Inject
   public AuthService(UserRepository userRepository, PasswordResetRequestRepository resetRequestRepository) {
      this.userRepository = userRepository;
      this.resetRequestRepository = resetRequestRepository;
   }

   // ==================== ADMIN OPERATIONS ====================

   /**
    * Crea un nuovo utente (solo admin può chiamare questo metodo).
    * Genera una password temporanea che l'admin comunicherà fisicamente al dipendente.
    * 
    * @param email email del nuovo utente
    * @param role ruolo dell'utente
    * @param adminUser l'admin che sta creando l'utente
    * @return la password temporanea generata (da comunicare al dipendente)
    */
   @Transactional
   public String createUser(String email, UserRole role, User adminUser) {
      // Verifica che chi chiama sia admin
      if (!adminUser.isAdmin()) {
         throw new SecurityException("Only administrators can create users.");
      }

      // Verifica che l'email non sia già registrata
      if (userRepository.existsByEmail(email)) {
         throw new IllegalArgumentException("Email already registered.");
      }

      // Genera password temporanea
      String tempPassword = generateTemporaryPassword();
      String hashedPassword = PasswordHasher.hash(tempPassword);

      // Crea e salva l'utente
      User newUser = new User(email, hashedPassword, role);
      userRepository.save(newUser);

      logger.log(Level.INFO, "Admin {0} created new user with email {1}", 
         new Object[]{adminUser.getEmail(), email});

      // Ritorna la password temporanea (l'admin la comunicherà fisicamente)
      return tempPassword;
   }

   /**
    * Admin resetta la password di un utente.
    * @return la nuova password temporanea
    */
   @Transactional
   public String resetUserPassword(Long userId, User adminUser) {
      if (!adminUser.isAdmin()) {
         throw new SecurityException("Only administrators can reset passwords.");
      }

      User user = userRepository.findById(userId)
         .orElseThrow(() -> new IllegalArgumentException("User not found."));

      // Genera nuova password temporanea
      String newTempPassword = generateTemporaryPassword();
      String hashedPassword = PasswordHasher.hash(newTempPassword);
      
      user.setPassword(hashedPassword);
      user.setFirstLogin(true); // Forza la scelta di un nuovo username al prossimo login
      userRepository.save(user);

      logger.log(Level.INFO, "Admin {0} reset password for user {1}", 
         new Object[]{adminUser.getEmail(), user.getEmail()});

      return newTempPassword;
   }

   /**
    * Admin processa una richiesta di reset password.
    * @param requestId ID della richiesta
    * @param approve true per approvare (resetta la password), false per rifiutare
    * @return la nuova password se approvata, null se rifiutata
    */
   @Transactional
   public String processPasswordResetRequest(Long requestId, boolean approve, User adminUser) {
      if (!adminUser.isAdmin()) {
         throw new SecurityException("Only administrators can process reset requests.");
      }

      PasswordResetRequest request = resetRequestRepository.findById(requestId)
         .orElseThrow(() -> new IllegalArgumentException("Reset request not found."));

      if (!request.isPending()) {
         throw new IllegalStateException("Request has already been processed.");
      }

      if (approve) {
         request.markAsCompleted(adminUser);
         resetRequestRepository.save(request);
         // Resetta effettivamente la password
         return resetUserPassword(request.getUser().getId(), adminUser);
      } else {
         request.markAsRejected(adminUser);
         resetRequestRepository.save(request);
         logger.log(Level.INFO, "Admin {0} rejected password reset for user {1}", 
            new Object[]{adminUser.getEmail(), request.getUser().getEmail()});
         return null;
      }
   }

   /**
    * Ottiene tutte le richieste di reset password pendenti (per dashboard admin).
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
    * Login utente. Ritorna i dati utente se le credenziali sono corrette.
    */
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

         // Ritorna il DTO con le info dell'utente (incluso se è il primo login)
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
    * Imposta lo username scelto dall'utente al primo login.
    */
   @Transactional
   public UserDTO finalizeProfile(Long userId, String chosenUsername) {
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
    * Utente richiede il reset della password.
    * Crea una notifica per l'admin.
    */
   @Transactional
   public void requestPasswordReset(String email) {
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