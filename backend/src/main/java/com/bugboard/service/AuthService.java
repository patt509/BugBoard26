package com.bugboard.service;

import com.bugboard.model.User;
import com.bugboard.repository.UserRepository;
import com.bugboard.security.PasswordHasher;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Arrays;

@ApplicationScoped
public class AuthService {
   
   @Inject
   private UserRepository userRepository;

   // Method to register a new user
   public void registerUser(User user, char[] rawPassword) {
      try { // 1. Call hash function before saving
         // Convert char[] to String for hashing
         String hashedPassword = PasswordHasher.hash(new String(rawPassword));
         user.setPassword(hashedPassword);

         userRepository.save(user);
      } finally { // 2. Clear raw password from memory to prevent leaks
         Arrays.fill(rawPassword, '\0');
      }
   }

   // Login method
   public boolean authenticate(String email, char[] rawPassword) {
      try {
         return userRepository.findByEmail(email)
                 .map(user -> PasswordHasher.verify(new String(rawPassword), user.getPassword()))
                 .orElse(false); // User not found or password mismatch
      } finally {
         // Clear password from memory for security
         Arrays.fill(rawPassword, '\0');
      }
   }
}