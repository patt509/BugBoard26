package com.bugboard.model;

import java.time.LocalDateTime;

public class User {
   // ATTRIBUTES
   private Long id;
   private String username;
   private String password;
   private String email;
   private UserRole role;
   private boolean isFirstLogin;
   private LocalDateTime createdAt;

   public User(String email, String password, UserRole role) {
      if (email == null || !email.contains("@")) {
         throw new IllegalArgumentException("Invalid email format.");
      }
      this.email = email;
      this.password = password;
      this.role = role;
      this.isFirstLogin = true;
      this.createdAt = LocalDateTime.now();
   }

   // GETTERS AND SETTERS
   public String getUsername() { return username; }
   public String getEmail() { return email; }
   public UserRole getRole() { return role; }
   public boolean isFirstLogin() { return isFirstLogin; }

   // OTHER METHODS
   // Method to finalize profile on first login
   public void finalizeProfile(String chosenUsername) {
      if (!isFirstLogin) {
         throw new IllegalStateException("Profile has already been finalized.");
      }
      if (chosenUsername == null || chosenUsername.trim().length() < 3) {
         throw new IllegalArgumentException("Username must be at least 3 characters.");
      }
      this.username = chosenUsername;
      this.isFirstLogin = false;
   }
}