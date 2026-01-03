package com.bugboard.dto;

import java.time.LocalDateTime;

/**
 * DTO for comment data transfer.
 */
public class CommentDTO {
   private Long id;
   private String text;
   private Long issueId;
   private Long authorId;
   private String authorUsername;
   private LocalDateTime createdAt;
   private LocalDateTime updatedAt;
   private String attachmentPath;

   public CommentDTO() {
   }

   private CommentDTO(Builder builder) {
      this.id = builder.id;
      this.text = builder.text;
      this.issueId = builder.issueId;
      this.authorId = builder.authorId;
      this.authorUsername = builder.authorUsername;
      this.createdAt = builder.createdAt;
      this.updatedAt = builder.updatedAt;
      this.attachmentPath = builder.attachmentPath;
   }

   public static Builder builder() {
      return new Builder();
   }

   public static class Builder {
      private Long id;
      private String text;
      private Long issueId;
      private Long authorId;
      private String authorUsername;
      private LocalDateTime createdAt;
      private LocalDateTime updatedAt;
      private String attachmentPath;

      public Builder id(Long id) {
         this.id = id;
         return this;
      }

      public Builder text(String text) {
         this.text = text;
         return this;
      }

      public Builder issueId(Long issueId) {
         this.issueId = issueId;
         return this;
      }

      public Builder authorId(Long authorId) {
         this.authorId = authorId;
         return this;
      }

      public Builder authorUsername(String authorUsername) {
         this.authorUsername = authorUsername;
         return this;
      }

      public Builder createdAt(LocalDateTime createdAt) {
         this.createdAt = createdAt;
         return this;
      }

      public Builder updatedAt(LocalDateTime updatedAt) {
         this.updatedAt = updatedAt;
         return this;
      }

      public Builder attachmentPath(String attachmentPath) {
         this.attachmentPath = attachmentPath;
         return this;
      }

      public CommentDTO build() {
         return new CommentDTO(this);
      }
   }

   // Getters
   public Long getId() {
      return id;
   }

   public String getText() {
      return text;
   }

   public Long getIssueId() {
      return issueId;
   }

   public Long getAuthorId() {
      return authorId;
   }

   public String getAuthorUsername() {
      return authorUsername;
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

   // Setters
   public void setId(Long id) {
      this.id = id;
   }

   public void setText(String text) {
      this.text = text;
   }

   public void setIssueId(Long issueId) {
      this.issueId = issueId;
   }

   public void setAuthorId(Long authorId) {
      this.authorId = authorId;
   }

   public void setAuthorUsername(String authorUsername) {
      this.authorUsername = authorUsername;
   }

   public void setCreatedAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
   }

   public void setUpdatedAt(LocalDateTime updatedAt) {
      this.updatedAt = updatedAt;
   }

   public void setAttachmentPath(String attachmentPath) {
      this.attachmentPath = attachmentPath;
   }
}
