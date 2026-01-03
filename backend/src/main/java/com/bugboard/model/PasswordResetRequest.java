package com.bugboard.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity for password reset requests.
 * When a user requests a password reset, an entry is created here
 * that the admin can view and manage from their portal.
 */
@Entity
@Table(name = "password_reset_requests")
public class PasswordResetRequest {

   public enum RequestStatus {
      PENDING, // Waiting for admin to process
      COMPLETED, // Admin has reset the password
      REJECTED // Admin has rejected the request
   }

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;

   @ManyToOne
   @JoinColumn(name = "user_id", nullable = false)
   private User user;

   @Column(nullable = false)
   private LocalDateTime requestedAt;

   @Enumerated(EnumType.STRING)
   private RequestStatus status;

   private LocalDateTime processedAt;

   @ManyToOne
   @JoinColumn(name = "processed_by_admin_id")
   private User processedByAdmin;

   protected PasswordResetRequest() {
   } // JPA

   public PasswordResetRequest(User user) {
      if (user == null) {
         throw new IllegalArgumentException("User cannot be null");
      }
      this.user = user;
      this.requestedAt = LocalDateTime.now();
      this.status = RequestStatus.PENDING;
   }

   // Getters
   public Long getId() {
      return id;
   }

   public User getUser() {
      return user;
   }

   public LocalDateTime getRequestedAt() {
      return requestedAt;
   }

   public RequestStatus getStatus() {
      return status;
   }

   public LocalDateTime getProcessedAt() {
      return processedAt;
   }

   public User getProcessedByAdmin() {
      return processedByAdmin;
   }

   // Business methods
   public void markAsCompleted(User admin) {
      if (!admin.isAdmin()) {
         throw new IllegalArgumentException("Only admins can process reset requests");
      }
      this.status = RequestStatus.COMPLETED;
      this.processedAt = LocalDateTime.now();
      this.processedByAdmin = admin;
   }

   public void markAsRejected(User admin) {
      if (!admin.isAdmin()) {
         throw new IllegalArgumentException("Only admins can process reset requests");
      }
      this.status = RequestStatus.REJECTED;
      this.processedAt = LocalDateTime.now();
      this.processedByAdmin = admin;
   }

   public boolean isPending() {
      return this.status == RequestStatus.PENDING;
   }
}
