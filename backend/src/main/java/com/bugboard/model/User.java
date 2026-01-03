package com.bugboard.model;

import com.bugboard.enums.UserRole;
import jakarta.persistence.*;

import java.time.LocalDateTime;

// JPA annotations to map this class to a database table
@Entity
@Table(name = "users")
public class User {
   // ATTRIBUTES
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;

   @Column(unique = true, nullable = true)
   private String username;

   @Column(nullable = false)
   private String password;

   @Column(nullable = false, unique = true)
   private String email;

   @Enumerated(EnumType.STRING)
   private UserRole role;

   private boolean isFirstLogin;
   private LocalDateTime createdAt;

   // CONSTRUCTOR
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

   public User() {
   } // Default constructor for JPA

   // GETTERS AND SETTERS
   public Long getId() {
      return id;
   }

   public String getUsername() {
      return username;
   }

   public void setUsername(String username) {
      this.username = username;
   }

   public String getEmail() {
      return email;
   }

   public String getPassword() {
      return password;
   }

   public void setPassword(String password) {
      if (password == null || password.trim().isEmpty()) {
         throw new IllegalArgumentException("Password cannot be empty.");
      }
      this.password = password;
   }

   public UserRole getRole() {
      return role;
   }

   public void setRole(UserRole role) {
      this.role = role;
   }

   public boolean isFirstLogin() {
      return isFirstLogin;
   }

   public void setFirstLogin(boolean firstLogin) {
      isFirstLogin = firstLogin;
   }

   public LocalDateTime getCreatedAt() {
      return createdAt;
   }

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

   // Quick admin check
   public boolean isAdmin() {
      return this.role == UserRole.ADMIN;
   }
}