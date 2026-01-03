package com.bugboard.dto;

/**
 * DTO per la creazione utente da parte dell'admin.
 * Contiene solo email e ruolo, la password temporanea viene generata dal sistema.
 */
public class CreateUserRequest {
   private String email;
   private String role; // "USER" o "ADMIN"

   public CreateUserRequest() {}

   public CreateUserRequest(String email, String role) {
      this.email = email;
      this.role = role;
   }

   public String getEmail() { return email; }
   public void setEmail(String email) { this.email = email; }

   public String getRole() { return role; }
   public void setRole(String role) { this.role = role; }
}
