package com.bugboard.dto;

import java.time.LocalDateTime;

public class IssueDTO {
   private Long id;
   private String title;
   private String status;
   private String priority;
   private String reporterName;
   private LocalDateTime createdAt;
   private LocalDateTime closedAt;
   private String attachmentPath;

   public IssueDTO() {}

   private IssueDTO(Builder builder) {
      this.id = builder.id;
      this.title = builder.title;
      this.status = builder.status;
      this.priority = builder.priority;
      this.reporterName = builder.reporterName;
      this.createdAt = builder.createdAt;
      this.closedAt = builder.closedAt;
      this.attachmentPath = builder.attachmentPath;
   }

   public static Builder builder() {
      return new Builder();
   }

   public static class Builder {
      private Long id;
      private String title;
      private String status;
      private String priority;
      private String reporterName;
      private LocalDateTime createdAt;
      private LocalDateTime closedAt;
      private String attachmentPath;

      public Builder id(Long id) {
         this.id = id;
         return this;
      }

      public Builder title(String title) {
         this.title = title;
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

      public IssueDTO build() {
         return new IssueDTO(this);
      }
   }

   // Getters
   public Long getId() { return id; }
   public String getTitle() { return title; }
   public String getStatus() { return status; }
   public String getPriority() { return priority; }
   public String getReporterName() { return reporterName; }
   public LocalDateTime getCreatedAt() { return createdAt; }
   public LocalDateTime getClosedAt() { return closedAt; }
   public String getAttachmentPath() { return attachmentPath; }

   // Setters
   public void setId(Long id) { this.id = id; }
   public void setTitle(String title) { this.title = title; }
   public void setStatus(String status) { this.status = status; }
   public void setPriority(String priority) { this.priority = priority; }
   public void setReporterName(String reporterName) { this.reporterName = reporterName; }
   public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
   public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
   public void setAttachmentPath(String attachmentPath) { this.attachmentPath = attachmentPath; }
}