package com.bugboard.dto;

/**
 * DTO per le richieste di login.
 * Contiene email e password in chiaro (da validare lato server).
 */
public class LoginRequest {
   private String email;
   private String password;

   public LoginRequest() {}

   public LoginRequest(String email, String password) {
      this.email = email;
      this.password = password;
   }

   public String getEmail() { return email; }
   public void setEmail(String email) { this.email = email; }

   public String getPassword() { return password; }
   public void setPassword(String password) { this.password = password; }
}
