package com.bugboard.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing a comment on an issue.
 * Each comment can have at most one attachment (JPG/PNG, max 5MB).
 */
@Entity
@Table(name = "comments")
public class Comment {

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;

   @Column(nullable = false, length = 2000)
   private String text;

   @ManyToOne
   @JoinColumn(name = "issue_id", nullable = false)
   private Issue issue;

   @ManyToOne
   @JoinColumn(name = "author_id", nullable = false)
   private User author;

   private LocalDateTime createdAt;
   private LocalDateTime updatedAt;

   private String attachmentPath; // Max 1 attachment per comment

   protected Comment() {
   } // JPA

   public Comment(String text, Issue issue, User author) {
      if (text == null || text.trim().isEmpty()) {
         throw new IllegalArgumentException("Comment text cannot be empty.");
      }
      if (text.length() > 2000) {
         throw new IllegalArgumentException("Comment text cannot exceed 2000 characters.");
      }
      if (issue == null) {
         throw new IllegalArgumentException("Issue cannot be null.");
      }
      if (author == null) {
         throw new IllegalArgumentException("Author cannot be null.");
      }

      this.text = text;
      this.issue = issue;
      this.author = author;
      this.createdAt = LocalDateTime.now();
   }

   // Getters
   public Long getId() {
      return id;
   }

   public String getText() {
      return text;
   }

   public Issue getIssue() {
      return issue;
   }

   public User getAuthor() {
      return author;
   }

   public LocalDateTime getCreatedAt() {
      return createdAt;
   }

   public LocalDateTime getUpdatedAt() {
      return updatedAt;
   }

   public String getAttachmentPath() {
      return attachmentPath;
   }

   // Setters with validation
   public void setText(String text) {
      if (text == null || text.trim().isEmpty()) {
         throw new IllegalArgumentException("Comment text cannot be empty.");
      }
      if (text.length() > 2000) {
         throw new IllegalArgumentException("Comment text cannot exceed 2000 characters.");
      }
      this.text = text;
      this.updatedAt = LocalDateTime.now();
   }

   public void setAttachmentPath(String attachmentPath) {
      this.attachmentPath = attachmentPath;
   }

   public void removeAttachment() {
      this.attachmentPath = null;
   }
}
