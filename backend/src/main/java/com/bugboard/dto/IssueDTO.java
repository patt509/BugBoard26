package com.bugboard.dto;

import java.time.LocalDateTime;

public class IssueDTO {
   private Long id;
   private String title;
   private String description;
   private String status;
   private String priority;
   private String reporterName;
   private LocalDateTime createdAt;
   private LocalDateTime closedAt;
   private String attachmentPath;
   private Long originalIssueId; // If this issue is a duplicate, reference to original

   public IssueDTO() {
   }

   private IssueDTO(Builder builder) {
      this.id = builder.id;
      this.title = builder.title;
      this.description = builder.description;
      this.status = builder.status;
      this.priority = builder.priority;
      this.reporterName = builder.reporterName;
      this.createdAt = builder.createdAt;
      this.closedAt = builder.closedAt;
      this.attachmentPath = builder.attachmentPath;
      this.originalIssueId = builder.originalIssueId;
   }

   public static Builder builder() {
      return new Builder();
   }

   public static class Builder {
      private Long id;
      private String title;
      private String description;
      private String status;
      private String priority;
      private String reporterName;
      private LocalDateTime createdAt;
      private LocalDateTime closedAt;
      private String attachmentPath;
      private Long originalIssueId;

      public Builder id(Long id) {
         this.id = id;
         return this;
      }

      public Builder title(String title) {
         this.title = title;
         return this;
      }

      public Builder description(String description) {
         this.description = description;
         return this;
      }

      public Builder status(String status) {
         this.status = status;
         return this;
      }

      public Builder priority(String priority) {
         this.priority = priority;
         return this;
      }

      public Builder reporterName(String reporterName) {
         this.reporterName = reporterName;
         return this;
      }

      public Builder createdAt(LocalDateTime createdAt) {
         this.createdAt = createdAt;
         return this;
      }

      public Builder closedAt(LocalDateTime closedAt) {
         this.closedAt = closedAt;
         return this;
      }

      public Builder attachmentPath(String attachmentPath) {
         this.attachmentPath = attachmentPath;
         return this;
      }

      public Builder originalIssueId(Long originalIssueId) {
         this.originalIssueId = originalIssueId;
         return this;
      }

      public IssueDTO build() {
         return new IssueDTO(this);
      }
   }

   // Getters
   public Long getId() {
      return id;
   }

   public String getTitle() {
      return title;
   }

   public String getDescription() {
      return description;
   }

   public String getStatus() {
      return status;
   }

   public String getPriority() {
      return priority;
   }

   public String getReporterName() {
      return reporterName;
   }

   public LocalDateTime getCreatedAt() {
      return createdAt;
   }

   public LocalDateTime getClosedAt() {
      return closedAt;
   }

   public String getAttachmentPath() {
      return attachmentPath;
   }

   public Long getOriginalIssueId() {
      return originalIssueId;
   }

   // Setters
   public void setId(Long id) {
      this.id = id;
   }

   public void setTitle(String title) {
      this.title = title;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   public void setStatus(String status) {
      this.status = status;
   }

   public void setPriority(String priority) {
      this.priority = priority;
   }

   public void setReporterName(String reporterName) {
      this.reporterName = reporterName;
   }

   public void setCreatedAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
   }

   public void setClosedAt(LocalDateTime closedAt) {
      this.closedAt = closedAt;
   }

   public void setAttachmentPath(String attachmentPath) {
      this.attachmentPath = attachmentPath;
   }

   public void setOriginalIssueId(Long originalIssueId) {
      this.originalIssueId = originalIssueId;
   }
}