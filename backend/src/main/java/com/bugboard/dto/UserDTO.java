package com.bugboard.dto;

import java.time.LocalDateTime;

/**
 * DTO per il trasferimento dati utente.
 * Non espone mai la password hashata.
 */
public class UserDTO {
   private Long id;
   private String email;
   private String username;
   private String role;
   private boolean firstLogin;
   private LocalDateTime createdAt;

   public UserDTO() {}

   private UserDTO(Builder builder) {
      this.id = builder.id;
      this.email = builder.email;
      this.username = builder.username;
      this.role = builder.role;
      this.firstLogin = builder.firstLogin;
      this.createdAt = builder.createdAt;
   }

   public static Builder builder() {
      return new Builder();
   }

   public static class Builder {
      private Long id;
      private String email;
      private String username;
      private String role;
      private boolean firstLogin;
      private LocalDateTime createdAt;

      public Builder id(Long id) {
         this.id = id;
         return this;
      }

      public Builder email(String email) {
         this.email = email;
         return this;
      }

      public Builder username(String username) {
         this.username = username;
         return this;
      }

      public Builder role(String role) {
         this.role = role;
         return this;
      }

      public Builder firstLogin(boolean firstLogin) {
         this.firstLogin = firstLogin;
         return this;
      }

      public Builder createdAt(LocalDateTime createdAt) {
         this.createdAt = createdAt;
         return this;
      }

      public UserDTO build() {
         return new UserDTO(this);
      }
   }

   // Getters
   public Long getId() { return id; }
   public String getEmail() { return email; }
   public String getUsername() { return username; }
   public String getRole() { return role; }
   public boolean isFirstLogin() { return firstLogin; }
   public LocalDateTime getCreatedAt() { return createdAt; }

   // Setters (per deserializzazione JSON)
   public void setId(Long id) { this.id = id; }
   public void setEmail(String email) { this.email = email; }
   public void setUsername(String username) { this.username = username; }
   public void setRole(String role) { this.role = role; }
   public void setFirstLogin(boolean firstLogin) { this.firstLogin = firstLogin; }
   public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
