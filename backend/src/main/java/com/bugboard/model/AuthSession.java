package com.bugboard.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Server-side session backing store for signed access tokens.
 */
@Entity
@Table(name = "auth_sessions")
public class AuthSession {

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;

   @Column(name = "session_id", nullable = false, unique = true, updatable = false, length = 128)
   private String sessionId;

   @ManyToOne
   @JoinColumn(name = "user_id", nullable = false)
   private User user;

   @Column(name = "created_at", nullable = false, updatable = false)
   private LocalDateTime createdAt;

   @Column(name = "expires_at", nullable = false)
   private LocalDateTime expiresAt;

   @Column(name = "last_seen_at", nullable = false)
   private LocalDateTime lastSeenAt;

   @Column(name = "revoked_at")
   private LocalDateTime revokedAt;

   protected AuthSession() {
      // JPA
   }

   public AuthSession(String sessionId, User user, LocalDateTime createdAt, LocalDateTime expiresAt) {
      if (sessionId == null || sessionId.isBlank()) {
         throw new IllegalArgumentException("Session id is required.");
      }
      if (user == null) {
         throw new IllegalArgumentException("Session user is required.");
      }
      if (createdAt == null || expiresAt == null || !expiresAt.isAfter(createdAt)) {
         throw new IllegalArgumentException("Session validity interval is invalid.");
      }
      this.sessionId = sessionId;
      this.user = user;
      this.createdAt = createdAt;
      this.expiresAt = expiresAt;
      this.lastSeenAt = createdAt;
   }

   public Long getId() {
      return id;
   }

   public String getSessionId() {
      return sessionId;
   }

   public User getUser() {
      return user;
   }

   public LocalDateTime getCreatedAt() {
      return createdAt;
   }

   public LocalDateTime getExpiresAt() {
      return expiresAt;
   }

   public LocalDateTime getLastSeenAt() {
      return lastSeenAt;
   }

   public LocalDateTime getRevokedAt() {
      return revokedAt;
   }

   public boolean isActive(LocalDateTime now) {
      return revokedAt == null && now != null && now.isBefore(expiresAt);
   }

   public void touch(LocalDateTime now) {
      if (now != null) {
         this.lastSeenAt = now;
      }
   }

   public void revoke(LocalDateTime now) {
      this.revokedAt = now != null ? now : LocalDateTime.now();
   }
}
