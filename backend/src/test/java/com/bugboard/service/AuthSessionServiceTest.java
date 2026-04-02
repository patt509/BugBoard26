package com.bugboard.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.bugboard.dto.AuthLoginResponse;
import com.bugboard.dto.UserDTO;
import com.bugboard.enums.UserRole;
import com.bugboard.model.AuthSession;
import com.bugboard.model.User;
import com.bugboard.repository.AuthSessionRepository;
import com.bugboard.repository.UserRepository;
import com.bugboard.security.AuthTokenClaims;
import com.bugboard.security.AuthTokenService;

@RunWith(MockitoJUnitRunner.class)
public class AuthSessionServiceTest {

   @Mock
   private AuthSessionRepository authSessionRepository;

   @Mock
   private UserRepository userRepository;

   @Mock
   private AuthTokenService authTokenService;

   @InjectMocks
   private AuthSessionService authSessionService;

   @Test
   public void createLoginResponse_TC1_ReturnsBearerTokenAndPersistsSession() {
      UserDTO userDTO = UserDTO.builder()
            .id(7L)
            .email("admin@test.com")
            .role("ADMIN")
            .build();

      User persistedUser = org.mockito.Mockito.mock(User.class);
      when(persistedUser.getId()).thenReturn(7L);
      when(persistedUser.getRole()).thenReturn(UserRole.ADMIN);
      when(userRepository.findById(7L)).thenReturn(Optional.of(persistedUser));
      when(authTokenService.issueToken(eq(7L), eq("ADMIN"), anyString(), anyLong(), anyLong()))
            .thenReturn("signed-token");

      AuthLoginResponse response = authSessionService.createLoginResponse(userDTO);

      assertNotNull("PostCond failed: login response should not be null", response);
      assertEquals("PostCond failed: token type should be Bearer", "Bearer", response.getTokenType());
      assertEquals("PostCond failed: access token should match mocked token", "signed-token", response.getAccessToken());
      assertEquals("PostCond failed: response should include original user", Long.valueOf(7L), response.getUser().getId());
      assertTrue("PostCond failed: expiration should be in the future",
            response.getExpiresAtEpochSeconds() > (System.currentTimeMillis() / 1000));

      ArgumentCaptor<AuthSession> sessionCaptor = ArgumentCaptor.forClass(AuthSession.class);
      verify(authSessionRepository).save(sessionCaptor.capture());
      AuthSession savedSession = sessionCaptor.getValue();
      assertNotNull("PostCond failed: saved session should not be null", savedSession);
      assertNotNull("PostCond failed: generated session id should be present", savedSession.getSessionId());
      assertEquals("PostCond failed: session user should match", Long.valueOf(7L), savedSession.getUser().getId());
   }

   @Test
   public void validateActiveSession_TC2_ReturnsClaimsForActiveSession() {
      long nowEpoch = System.currentTimeMillis() / 1000;
      AuthTokenClaims expectedClaims = new AuthTokenClaims(7L, "ADMIN", "session-123", nowEpoch, nowEpoch + 3600);
      when(authTokenService.verifyToken("raw-token")).thenReturn(expectedClaims);

      User sessionUser = org.mockito.Mockito.mock(User.class);
      when(sessionUser.getId()).thenReturn(7L);
      AuthSession storedSession = new AuthSession(
            "session-123",
            sessionUser,
            LocalDateTime.now().minusMinutes(10),
            LocalDateTime.now().plusMinutes(30));
      when(authSessionRepository.findBySessionId("session-123")).thenReturn(Optional.of(storedSession));

      AuthTokenClaims actualClaims = authSessionService.validateActiveSession("raw-token");

      assertEquals("PostCond failed: claims should match token payload", expectedClaims, actualClaims);
      verify(authSessionRepository).save(storedSession);
   }

   @Test(expected = SecurityException.class)
   public void validateActiveSession_TC3_RejectsRevokedSession() {
      long nowEpoch = System.currentTimeMillis() / 1000;
      AuthTokenClaims claims = new AuthTokenClaims(7L, "ADMIN", "session-123", nowEpoch, nowEpoch + 3600);
      when(authTokenService.verifyToken("raw-token")).thenReturn(claims);

      User sessionUser = org.mockito.Mockito.mock(User.class);
      when(sessionUser.getId()).thenReturn(7L);
      AuthSession revokedSession = new AuthSession(
            "session-123",
            sessionUser,
            LocalDateTime.now().minusMinutes(10),
            LocalDateTime.now().plusMinutes(30));
      revokedSession.revoke(LocalDateTime.now().minusMinutes(1));
      when(authSessionRepository.findBySessionId("session-123")).thenReturn(Optional.of(revokedSession));

      try {
         authSessionService.validateActiveSession("raw-token");
      } finally {
         verify(authSessionRepository, never()).save(any(AuthSession.class));
      }
   }

   @Test
   public void revokeCurrentSession_TC4_RevokesAndPersistsSession() {
      long nowEpoch = System.currentTimeMillis() / 1000;
      AuthTokenClaims claims = new AuthTokenClaims(7L, "ADMIN", "session-logout", nowEpoch - 60, nowEpoch + 3600);
      when(authTokenService.extractBearerToken("Bearer abc")).thenReturn("abc");
      when(authTokenService.verifyTokenIgnoringExpiry("abc")).thenReturn(claims);

      User sessionUser = org.mockito.Mockito.mock(User.class);
      AuthSession activeSession = new AuthSession(
            "session-logout",
            sessionUser,
            LocalDateTime.now().minusMinutes(2),
            LocalDateTime.now().plusMinutes(20));
      when(authSessionRepository.findBySessionId("session-logout")).thenReturn(Optional.of(activeSession));

      authSessionService.revokeCurrentSession("Bearer abc");

      assertNotNull("PostCond failed: session should be revoked", activeSession.getRevokedAt());
      verify(authSessionRepository).save(activeSession);
   }
}
