package com.bugboard.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Issues and verifies signed bearer tokens.
 */
@ApplicationScoped
public class AuthTokenService {

   private static final Logger logger = Logger.getLogger(AuthTokenService.class.getName());
   private static final String HMAC_ALGORITHM = "HmacSHA256";
   private static final String DEFAULT_DEV_SECRET = "bugboard-dev-secret-change-me";

   private final byte[] secretKey;

   public AuthTokenService() {
      String configuredSecret = System.getenv("AUTH_TOKEN_SECRET");
      if (configuredSecret == null || configuredSecret.isBlank()) {
         logger.warning("AUTH_TOKEN_SECRET is not set. Using development fallback secret.");
         configuredSecret = DEFAULT_DEV_SECRET;
      }
      this.secretKey = configuredSecret.getBytes(StandardCharsets.UTF_8);
   }

   public String issueToken(Long userId, String role, String sessionId, long issuedAtEpochSeconds, long expiresAtEpochSeconds) {
      if (userId == null || role == null || role.isBlank() || sessionId == null || sessionId.isBlank()) {
         throw new IllegalArgumentException("Missing claims for token issuance.");
      }
      String payload = String.format(
            "v1|uid=%d|sid=%s|role=%s|iat=%d|exp=%d",
            userId,
            sessionId,
            role,
            issuedAtEpochSeconds,
            expiresAtEpochSeconds);
      byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
      byte[] signatureBytes = sign(payloadBytes);
      return base64UrlEncode(payloadBytes) + "." + base64UrlEncode(signatureBytes);
   }

   public AuthTokenClaims verifyToken(String rawToken) {
      return verifyToken(rawToken, true);
   }

   public AuthTokenClaims verifyTokenIgnoringExpiry(String rawToken) {
      return verifyToken(rawToken, false);
   }

   public String extractBearerToken(String authorizationHeader) {
      if (authorizationHeader == null || authorizationHeader.isBlank()) {
         throw new SecurityException("Missing Authorization header.");
      }
      if (!authorizationHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
         throw new SecurityException("Authorization scheme must be Bearer.");
      }
      String token = authorizationHeader.substring(7).trim();
      if (token.isBlank()) {
         throw new SecurityException("Bearer token is missing.");
      }
      return token;
   }

   private AuthTokenClaims verifyToken(String rawToken, boolean checkExpiry) {
      if (rawToken == null || rawToken.isBlank()) {
         throw new SecurityException("Missing access token.");
      }

      String[] tokenParts = rawToken.split("\\.");
      if (tokenParts.length != 2) {
         throw new SecurityException("Token format is invalid.");
      }

      byte[] payloadBytes = base64UrlDecode(tokenParts[0], "payload");
      byte[] signatureBytes = base64UrlDecode(tokenParts[1], "signature");
      byte[] expectedSignature = sign(payloadBytes);
      if (!MessageDigest.isEqual(signatureBytes, expectedSignature)) {
         throw new SecurityException("Token signature is invalid.");
      }

      String payload = new String(payloadBytes, StandardCharsets.UTF_8);
      AuthTokenClaims claims = parseClaims(payload);

      if (checkExpiry) {
         long now = System.currentTimeMillis() / 1000;
         if (claims.expiresAtEpochSeconds() <= now) {
            throw new SecurityException("Session has expired.");
         }
      }

      return claims;
   }

   private AuthTokenClaims parseClaims(String payload) {
      String[] parts = payload.split("\\|");
      if (parts.length < 6 || !"v1".equals(parts[0])) {
         throw new SecurityException("Token payload is invalid.");
      }

      Map<String, String> values = new HashMap<>();
      for (int i = 1; i < parts.length; i++) {
         String[] keyValue = parts[i].split("=", 2);
         if (keyValue.length == 2) {
            values.put(keyValue[0], keyValue[1]);
         }
      }

      try {
         Long userId = Long.parseLong(requiredClaim(values, "uid"));
         String sessionId = requiredClaim(values, "sid");
         String role = requiredClaim(values, "role");
         long issuedAt = Long.parseLong(requiredClaim(values, "iat"));
         long expiresAt = Long.parseLong(requiredClaim(values, "exp"));
         return new AuthTokenClaims(userId, role, sessionId, issuedAt, expiresAt);
      } catch (NumberFormatException ex) {
         throw new SecurityException("Token numeric claim is invalid.");
      }
   }

   private String requiredClaim(Map<String, String> values, String key) {
      String value = values.get(key);
      if (value == null || value.isBlank()) {
         throw new SecurityException("Token claim '" + key + "' is missing.");
      }
      return value;
   }

   private byte[] sign(byte[] payloadBytes) {
      try {
         Mac mac = Mac.getInstance(HMAC_ALGORITHM);
         mac.init(new SecretKeySpec(secretKey, HMAC_ALGORITHM));
         return mac.doFinal(payloadBytes);
      } catch (GeneralSecurityException ex) {
         throw new IllegalStateException("Unable to sign token.", ex);
      }
   }

   private String base64UrlEncode(byte[] data) {
      return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
   }

   private byte[] base64UrlDecode(String value, String label) {
      try {
         return Base64.getUrlDecoder().decode(value);
      } catch (IllegalArgumentException ex) {
         throw new SecurityException("Token " + label + " is not valid base64.");
      }
   }
}
