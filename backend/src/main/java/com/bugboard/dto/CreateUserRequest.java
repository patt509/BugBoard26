package com.bugboard.dto;

/**
 * DTO for user creation by admin.
 * Contains email, role, and an optional password.
 * If password is omitted, the backend generates a temporary password.
 */
public class CreateUserRequest {
   private String email;
   private String password;
   private String role; // "USER", "ADMIN", or "STAKEHOLDER"

   public CreateUserRequest() {
   }

   public CreateUserRequest(String email, String role) {
      this.email = email;
      this.role = role;
   }

   public CreateUserRequest(String email, String password, String role) {
      this.email = email;
      this.password = password;
      this.role = role;
   }

   public String getEmail() {
      return email;
   }

   public void setEmail(String email) {
      this.email = email;
   }

   public String getPassword() {
      return password;
   }

   public void setPassword(String password) {
      this.password = password;
   }

   public String getRole() {
      return role;
   }

   public void setRole(String role) {
      this.role = role;
   }
}
