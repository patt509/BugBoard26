package com.bugboard.dto;

/**
 * Login response payload containing user information and access token metadata.
 */
public class AuthLoginResponse {
   private UserDTO user;
   private String accessToken;
   private String tokenType;
   private long expiresAtEpochSeconds;

   public AuthLoginResponse() {
   }

   public AuthLoginResponse(UserDTO user, String accessToken, String tokenType, long expiresAtEpochSeconds) {
      this.user = user;
      this.accessToken = accessToken;
      this.tokenType = tokenType;
      this.expiresAtEpochSeconds = expiresAtEpochSeconds;
   }

   public UserDTO getUser() {
      return user;
   }

   public void setUser(UserDTO user) {
      this.user = user;
   }

   public String getAccessToken() {
      return accessToken;
   }

   public void setAccessToken(String accessToken) {
      this.accessToken = accessToken;
   }

   public String getTokenType() {
      return tokenType;
   }

   public void setTokenType(String tokenType) {
      this.tokenType = tokenType;
   }

   public long getExpiresAtEpochSeconds() {
      return expiresAtEpochSeconds;
   }

   public void setExpiresAtEpochSeconds(long expiresAtEpochSeconds) {
      this.expiresAtEpochSeconds = expiresAtEpochSeconds;
   }
}
