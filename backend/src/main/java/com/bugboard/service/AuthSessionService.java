package com.bugboard.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import com.bugboard.dto.AuthLoginResponse;
import com.bugboard.dto.UserDTO;
import com.bugboard.model.AuthSession;
import com.bugboard.model.User;
import com.bugboard.repository.AuthSessionRepository;
import com.bugboard.repository.UserRepository;
import com.bugboard.security.AuthTokenClaims;
import com.bugboard.security.AuthTokenService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class AuthSessionService {

   private static final int SESSION_ID_BYTES = 32;
   private static final Duration LAST_SEEN_TOUCH_INTERVAL = Duration.ofMinutes(5);
   private static final Duration DEFAULT_ACCESS_TOKEN_TTL = Duration.ofHours(8);

   private final AuthSessionRepository authSessionRepository;
   private final UserRepository userRepository;
   private final AuthTokenService authTokenService;
   private final SecureRandom secureRandom;

   // CDI proxy constructor
   protected AuthSessionService() {
      this.authSessionRepository = null;
      this.userRepository = null;
      this.authTokenService = null;
      this.secureRandom = null;
   }

   @Inject
   public AuthSessionService(
         AuthSessionRepository authSessionRepository,
         UserRepository userRepository,
         AuthTokenService authTokenService) {
      this.authSessionRepository = authSessionRepository;
      this.userRepository = userRepository;
      this.authTokenService = authTokenService;
      this.secureRandom = new SecureRandom();
   }

   @Transactional
   public AuthLoginResponse createLoginResponse(UserDTO userDTO) {
      if (userDTO == null || userDTO.getId() == null) {
         throw new IllegalArgumentException("Valid user data is required to create a session.");
      }

      User user = userRepository.findById(userDTO.getId())
            .orElseThrow(() -> new SecurityException("User not found."));

      Duration sessionTtl = resolveAccessTokenTtl();
      LocalDateTime createdAt = LocalDateTime.now();
      LocalDateTime expiresAt = createdAt.plus(sessionTtl);
      String sessionId = generateSessionId();

      AuthSession authSession = new AuthSession(sessionId, user, createdAt, expiresAt);
      authSessionRepository.save(authSession);

      long issuedAtEpochSeconds = createdAt.toEpochSecond(ZoneOffset.UTC);
      long expiresAtEpochSeconds = expiresAt.toEpochSecond(ZoneOffset.UTC);
      String accessToken = authTokenService.issueToken(
            user.getId(),
            user.getRole().name(),
            sessionId,
            issuedAtEpochSeconds,
            expiresAtEpochSeconds);

      return new AuthLoginResponse(userDTO, accessToken, "Bearer", expiresAtEpochSeconds);
   }

   @Transactional
   public AuthTokenClaims validateActiveSession(String rawToken) {
      AuthTokenClaims claims = authTokenService.verifyToken(rawToken);
      LocalDateTime now = LocalDateTime.now();

      AuthSession session = authSessionRepository.findBySessionId(claims.sessionId())
            .orElseThrow(() -> new SecurityException("Session is invalid."));

      if (session.getUser() == null || !claims.userId().equals(session.getUser().getId())) {
         throw new SecurityException("Token session does not match user.");
      }
      if (!session.isActive(now)) {
         throw new SecurityException("Session is no longer active.");
      }

      LocalDateTime lastSeenAt = session.getLastSeenAt();
      if (lastSeenAt == null || lastSeenAt.isBefore(now.minus(LAST_SEEN_TOUCH_INTERVAL))) {
         session.touch(now);
         authSessionRepository.save(session);
      }

      return claims;
   }

   @Transactional
   public void revokeCurrentSession(String authorizationHeader) {
      String rawToken = authTokenService.extractBearerToken(authorizationHeader);
      AuthTokenClaims claims = authTokenService.verifyTokenIgnoringExpiry(rawToken);
      Optional<AuthSession> sessionOpt = authSessionRepository.findBySessionId(claims.sessionId());
      if (sessionOpt.isPresent()) {
         AuthSession session = sessionOpt.get();
         session.revoke(LocalDateTime.now());
         authSessionRepository.save(session);
      }
   }

   @Transactional
   public void revokeAllSessionsForUser(Long userId) {
      if (userId == null) {
         return;
      }
      User user = userRepository.findById(userId).orElse(null);
      if (user == null) {
         return;
      }

      LocalDateTime now = LocalDateTime.now();
      List<AuthSession> sessions = authSessionRepository.findActiveByUser(user);
      for (AuthSession session : sessions) {
         if (session.isActive(now)) {
            session.revoke(now);
            authSessionRepository.save(session);
         }
      }
   }

   public String extractBearerToken(String authorizationHeader) {
      return authTokenService.extractBearerToken(authorizationHeader);
   }

   private Duration resolveAccessTokenTtl() {
      String configuredMinutes = System.getenv("AUTH_ACCESS_TOKEN_TTL_MINUTES");
      if (configuredMinutes == null || configuredMinutes.isBlank()) {
         return DEFAULT_ACCESS_TOKEN_TTL;
      }
      try {
         int minutes = Integer.parseInt(configuredMinutes.trim());
         if (minutes <= 0) {
            return DEFAULT_ACCESS_TOKEN_TTL;
         }
         return Duration.ofMinutes(minutes);
      } catch (NumberFormatException ex) {
         return DEFAULT_ACCESS_TOKEN_TTL;
      }
   }

   private String generateSessionId() {
      byte[] randomBytes = new byte[SESSION_ID_BYTES];
      secureRandom.nextBytes(randomBytes);
      return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
   }
}
