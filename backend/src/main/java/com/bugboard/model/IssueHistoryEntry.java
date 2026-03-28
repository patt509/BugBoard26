package com.bugboard.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "issue_history_entries")
public class IssueHistoryEntry {

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;

   @ManyToOne(fetch = FetchType.LAZY, optional = false)
   @JoinColumn(name = "issue_id", nullable = false)
   private Issue issue;

   @Column(nullable = false, length = 120)
   private String title;

   @Column(nullable = false, length = 1500)
   private String description;

   @Column(name = "changed_by", length = 160)
   private String changedBy;

   @Column(nullable = false)
   private LocalDateTime createdAt;

   protected IssueHistoryEntry() {
      // JPA
   }

   public IssueHistoryEntry(Issue issue, String title, String description) {
      this(issue, title, description, "System");
   }

   public IssueHistoryEntry(Issue issue, String title, String description, String changedBy) {
      if (issue == null) {
         throw new IllegalArgumentException("Issue is required.");
      }
      if (title == null || title.trim().isEmpty()) {
         throw new IllegalArgumentException("History title is required.");
      }
      if (description == null || description.trim().isEmpty()) {
         throw new IllegalArgumentException("History description is required.");
      }

      this.issue = issue;
      this.title = title.trim();
      this.description = description.trim();
      this.changedBy = normalizeChangedBy(changedBy);
      this.createdAt = LocalDateTime.now();
   }

   public Long getId() {
      return id;
   }

   public Issue getIssue() {
      return issue;
   }

   public String getTitle() {
      return title;
   }

   public String getDescription() {
      return description;
   }

   public LocalDateTime getCreatedAt() {
      return createdAt;
   }

   public String getChangedBy() {
      return changedBy;
   }

   private String normalizeChangedBy(String changedBy) {
      if (changedBy == null || changedBy.trim().isEmpty()) {
         return "System";
      }
      return changedBy.trim();
   }
}
