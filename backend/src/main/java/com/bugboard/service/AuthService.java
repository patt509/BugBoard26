package com.bugboard.service;

import com.bugboard.model.User;
import com.bugboard.repository.UserRepository;
import com.bugboard.security.PasswordHasher;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AuthService {
   
   @Inject
   private UserRepository userRepository;

   // Method to register a new user
   public void registerUser(User user, String rawPassword) {
      // Call hash function before saving
      String hashedPassword = PasswordHasher.hash(rawPassword);
      user.setPassword(hashedPassword);

      UserRepository.save(user);
   }

   // Login method
   public boolean authenticate(String email, String rawPassword) {
      return userRepository.findByEmail(email)
               .map(user -> PasswordHasher.verify(rawPassword, user.getPassword()))
               .orElse(false); // User not found or password mismatch
   }
}