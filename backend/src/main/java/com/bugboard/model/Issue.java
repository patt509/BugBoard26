package com.bugboard.model;

import java.time.LocalDateTime;

import com.bugboard.enums.IssueStatus;
import com.bugboard.enums.IssueType;
import com.bugboard.enums.PriorityLevel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "issues")
public class Issue {
   // 1. ATTRIBUTES
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;

   @Column(nullable = false)
   private String title;
   private String description;

   // For Status and Priority, we can use enums to restrict values
   @Enumerated(EnumType.STRING)
   private IssueStatus status;
   @Enumerated(EnumType.STRING)
   private PriorityLevel priority;

   @Enumerated(EnumType.STRING)
   @Column(nullable = false)
   private IssueType type;

   // User who reported the issue
   @ManyToOne
   @JoinColumn(name = "reporter_id", nullable = false)
   private User reporter;

   // User assigned to the issue (optional)
   @ManyToOne
   @JoinColumn(name = "assignee_id")
   private User assignee;

   // In case this issue is marked as duplicate
   @ManyToOne
   @JoinColumn(name = "original_issue_id")
   private Issue originalIssue;

   private LocalDateTime createdAt;
   private LocalDateTime updatedAt;
   private LocalDateTime closedAt;
   private String attachmentPath; // Requisito 7

   protected Issue() {
   }// JPA requires a default constructor

   // 2. CONSTRUCTOR
   /**
    * Creates a new Issue with mandatory fields.
    * @param title       must be at least 10 characters
    * @param description cannot be empty
    * @param reporter    the user reporting the issue
    * @param type        the issue type (QUESTION, BUG, DOCUMENTATION, FEATURE)
    */
   public Issue(String title, String description, User reporter, IssueType type) {
      if (title == null || title.trim().length() < 10) {
         throw new IllegalArgumentException("Title must be at least 10 characters.");
      }
      if (description == null || description.trim().isEmpty()) {
         throw new IllegalArgumentException("Description cannot be empty.");
      }
      if (type == null) {
         throw new IllegalArgumentException("Issue type is required.");
      }
      this.title = title;
      this.description = description;
      this.reporter = reporter;
      this.type = type;
      // Initialize default values
      this.status = IssueStatus.TODO;
      this.priority = PriorityLevel.MEDIUM;
      this.createdAt = LocalDateTime.now();
      this.updatedAt = LocalDateTime.now();
   }

   // 3. GETTERS AND SETTERS
   public String getTitle() {
      return title;
   }

   // In case someone wants to change the title later
   public void setTitle(String title) {
      if (title == null || title.trim().length() < 10) {
         throw new IllegalArgumentException("Title must be at least 10 characters.");
      }
      this.title = title;
      this.updatedAt = LocalDateTime.now();
   }

   public PriorityLevel getPriority() {
      return priority;
   }

   public void setPriority(PriorityLevel priority) {
      this.priority = priority;
      this.updatedAt = LocalDateTime.now();
   }

   public String getDescription() {
      return description;
   }

   public void setDescription(String description) {
      this.description = description;
      this.updatedAt = LocalDateTime.now();
   }

   public Long getId() {
      return id;
   }

   public IssueStatus getStatus() {
      return status;
   }

   public void setStatus(IssueStatus status) {
      // If the status is being set to CLOSED or RESOLVED, we should record the closing time
      // RESOLVED is also considered closed because a resolved issue is essentially closed
      if ((status == IssueStatus.CLOSED || status == IssueStatus.RESOLVED) 
          && this.status != IssueStatus.CLOSED && this.status != IssueStatus.RESOLVED) {
         this.closedAt = LocalDateTime.now();
      }
      // If the issue is being reopened (back to TODO or IN_PROGRESS), clear the closedAt timestamp
      else if (status != IssueStatus.CLOSED && status != IssueStatus.RESOLVED) {
         this.closedAt = null;
      }
      this.status = status;
      this.updatedAt = LocalDateTime.now();
   }

   public IssueType getType() {
      return type;
   }

   public void setType(IssueType type) {
      if (type == null) {
         throw new IllegalArgumentException("Issue type is required.");
      }
      this.type = type;
   }

   public User getReporter() {
      return reporter;
   }

   public User getAssignee() {
      return assignee;
   }

   public void setAssignee(User assignee) {
      this.assignee = assignee;
   }

   public Long getOriginalIssueId() {
      return originalIssue != null ? originalIssue.getId() : null;
   }

   public String getAttachmentPath() {
      return attachmentPath;
   }

   public void setAttachmentPath(String attachmentPath) {
      this.attachmentPath = attachmentPath;
      this.updatedAt = LocalDateTime.now();
   }

   public LocalDateTime getCreatedAt() {
      return createdAt;
   }

   public LocalDateTime getUpdatedAt() {
      return updatedAt;
   }

   public LocalDateTime getClosedAt() {
      return closedAt;
   }

   // 4. OTHER METHODS
   public void markAsDuplicateOf(Issue original) {
      if (original == null) {
         throw new IllegalArgumentException("Original issue is not valid.");
      }

      if (this == original || (this.id != null && this.id.equals(original.getId()))) {
         throw new IllegalArgumentException("An issue cannot be a duplicate of itself.");
      }

      // Check both CLOSED and RESOLVED - a resolved issue is also considered closed
      if (this.status == IssueStatus.CLOSED || this.status == IssueStatus.RESOLVED) {
         throw new IllegalStateException("The issue is already closed or resolved.");
      }

      this.originalIssue = original;
      this.status = IssueStatus.CLOSED;

      // Keep track of when the issue was closed
      this.closedAt = LocalDateTime.now();
   }
}