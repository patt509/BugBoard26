package com.bugboard.model;

import java.time.LocalDateTime; // Library to manage date and time

public class Issue {
   // 1. ATTRIBUTES
   // We use encapsulation to protect the data (private access modifier)
   private Long id;
   private String title;
   private String description;

   // For Status and Priority, we can use enums to restrict values
   private IssueStatus status;
   private PriorityLevel priority;

   // Pointer to original issue (in case of duplicates)
   private Issue originalIssue;

   private LocalDateTime createdAt;
   private LocalDateTime closedAt;

   // 2. CONSTRUCTOR
   public Issue(String title) {
      // Initialize status to "TODO", and createdAt to current
      // date and timeby default
      this.status = IssueStatus.TODO;
      this.createdAt = LocalDateTime.now();
      if (title == null || title.trim().length() < 10) {
         throw new IllegalArgumentException("Title must be at least 10 characters.");
      }
      this.title = title;
   }

   // 3. GETTERS AND SETTERS
   public String getTitle() { return title; }
   // In case someone wants to change the title later
   public void setTitle(String title) { 
      if (title == null || title.trim().length() < 10) {
         throw new IllegalArgumentException("Title must be at least 10 characters.");
      }
      this.title = title;
   }
   public String getDescription() { return description; }
   public void setDescription(String description) { this.description = description; }
   public Long getId() { return id; }
   public void setId(Long id) { this.id = id; }
   public IssueStatus getStatus() { return status; }
   public void setStatus(IssueStatus status) { this.status = status; }
   public LocalDateTime getCreatedAt() { return createdAt; }
   public LocalDateTime getClosedAt() { return closedAt; }
   public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }

   // 4. OTHER METHODS
   public void markAsDuplicateOf(Issue original) {
      if (original == null) {
         throw new IllegalArgumentException("Original issue is not valid.");
      }

      if (this == original || (this.id != null && this.id.equals(original.getId()))) {
         throw new IllegalArgumentException("An issue cannot be a duplicate of itself.");
      }

      if (this.status == IssueStatus.CLOSED) {
         throw new IllegalStateException("The issue is already closed.");
      }

      this.originalIssue = original;
      this.status = IssueStatus.CLOSED;

      // Keep track of when the issue was closed
      this.closedAt = LocalDateTime.now();
   }
}