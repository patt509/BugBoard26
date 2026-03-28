package com.bugboard.dto;

import java.time.LocalDateTime;

public class IssueHistoryDTO {
   private Long id;
   private LocalDateTime timestamp;
   private String title;
   private String description;
   private String changedBy;

   public IssueHistoryDTO() {
   }

   public IssueHistoryDTO(Long id, LocalDateTime timestamp, String title, String description) {
      this(id, timestamp, title, description, null);
   }

   public IssueHistoryDTO(Long id, LocalDateTime timestamp, String title, String description, String changedBy) {
      this.id = id;
      this.timestamp = timestamp;
      this.title = title;
      this.description = description;
      this.changedBy = changedBy;
   }

   public Long getId() {
      return id;
   }

   public void setId(Long id) {
      this.id = id;
   }

   public LocalDateTime getTimestamp() {
      return timestamp;
   }

   public void setTimestamp(LocalDateTime timestamp) {
      this.timestamp = timestamp;
   }

   public String getTitle() {
      return title;
   }

   public void setTitle(String title) {
      this.title = title;
   }

   public String getDescription() {
      return description;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   public String getChangedBy() {
      return changedBy;
   }

   public void setChangedBy(String changedBy) {
      this.changedBy = changedBy;
   }
}
