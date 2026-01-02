package com.bugboard.security;

import org.mindrot.jbcrypt.BCrypt;

/*
 * Utility class for hashing and verifying passwords using BCrypt.
 */
public class PasswordHasher {
   //
   private static final int SALT_ROUNDS = 12;

   /**
    * Hashes a plain text password in BCrypt.
    * @param plainPassword The password to hash.
    * @return The hashed password to save in the database.
    */
   public static String hash(String plainPassword) {
      if (plainPassword == null) {
         throw new IllegalArgumentException("Password cannot be null.");
      }
      // TODO: Consider adding password strength validation here

      // gensalt() generates a random salt, hashpw() hashes the password
      return BCrypt.hashpw(plainPassword, BCrypt.gensalt(SALT_ROUNDS));
   }

   /**
    * Verifies a plain text password against a hashed password.
    * @param plainPassword The plain text password to verify.
    * @param hashedPassword The hashed password from the database.
    * @return True if the passwords match, false otherwise.
    */
   public static boolean verify(String plainPassword, String hashedPassword) {
      if (plainPassword == null || hashedPassword == null) {
         return false;
      }

      // BCrypt extracts the salt from the hashed password to compare them
      try {
         return BCrypt.checkpw(plainPassword, hashedPassword);
      } catch (IllegalArgumentException e) {
         // This exception is thrown if the hashedPassword is not a valid
         // BCrypt hash or if it's corrupted
         return false;
      }
   }
}