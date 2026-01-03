package com.bugboard.dto;

import java.time.LocalDateTime;

/**
 * DTO per visualizzare le richieste di reset password (lato admin).
 */
public class PasswordResetRequestDTO {
   private Long id;
   private Long userId;
   private String userEmail;
   private String username;
   private LocalDateTime requestedAt;
   private String status;

   public PasswordResetRequestDTO() {}

   private PasswordResetRequestDTO(Builder builder) {
      this.id = builder.id;
      this.userId = builder.userId;
      this.userEmail = builder.userEmail;
      this.username = builder.username;
      this.requestedAt = builder.requestedAt;
      this.status = builder.status;
   }

   public static Builder builder() {
      return new Builder();
   }

   public static class Builder {
      private Long id;
      private Long userId;
      private String userEmail;
      private String username;
      private LocalDateTime requestedAt;
      private String status;

      public Builder id(Long id) {
         this.id = id;
         return this;
      }

      public Builder userId(Long userId) {
         this.userId = userId;
         return this;
      }

      public Builder userEmail(String userEmail) {
         this.userEmail = userEmail;
         return this;
      }

      public Builder username(String username) {
         this.username = username;
         return this;
      }

      public Builder requestedAt(LocalDateTime requestedAt) {
         this.requestedAt = requestedAt;
         return this;
      }

      public Builder status(String status) {
         this.status = status;
         return this;
      }

      public PasswordResetRequestDTO build() {
         return new PasswordResetRequestDTO(this);
      }
   }

   // Getters
   public Long getId() { return id; }
   public Long getUserId() { return userId; }
   public String getUserEmail() { return userEmail; }
   public String getUsername() { return username; }
   public LocalDateTime getRequestedAt() { return requestedAt; }
   public String getStatus() { return status; }
}
