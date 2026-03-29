package com.bugboard.security;

/**
 * Claims extracted from a verified access token.
 */
public record AuthTokenClaims(
      Long userId,
      String role,
      String sessionId,
      long issuedAtEpochSeconds,
      long expiresAtEpochSeconds) {
}
