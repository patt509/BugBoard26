package com.bugboard.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class AuthTokenServiceTest {

   @Test
   public void issueAndVerifyToken_TC1_ValidTokenRoundTrip() {
      AuthTokenService tokenService = new AuthTokenService();
      long now = System.currentTimeMillis() / 1000;
      String token = tokenService.issueToken(42L, "ADMIN", "session-abc", now, now + 3600);

      AuthTokenClaims claims = tokenService.verifyToken(token);

      assertNotNull("PostCond failed: claims should not be null", claims);
      assertEquals("PostCond failed: user id should match", Long.valueOf(42L), claims.userId());
      assertEquals("PostCond failed: role should match", "ADMIN", claims.role());
      assertEquals("PostCond failed: session id should match", "session-abc", claims.sessionId());
   }

   @Test(expected = SecurityException.class)
   public void verifyToken_TC2_RejectsTamperedToken() {
      AuthTokenService tokenService = new AuthTokenService();
      long now = System.currentTimeMillis() / 1000;
      String token = tokenService.issueToken(42L, "ADMIN", "session-abc", now, now + 3600);
      String tampered = token + "x";

      tokenService.verifyToken(tampered);
   }

   @Test(expected = SecurityException.class)
   public void verifyToken_TC3_RejectsExpiredToken() {
      AuthTokenService tokenService = new AuthTokenService();
      long now = System.currentTimeMillis() / 1000;
      String token = tokenService.issueToken(42L, "ADMIN", "session-abc", now - 100, now - 10);

      tokenService.verifyToken(token);
   }
}
