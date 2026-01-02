package com.bugboard.model;

import java.time.LocalDateTime; // Library to manage date and time

public class Issue {
   // 1. ATTRIBUTES
   // We use encapsulation to protect the data (private access modifier)
   private Long id;
   private String title;
   private String description;

   // For Status and Priority, we can use enums to restrict values
   public IssueStatus status;
   public PriorityLevel priority;

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
      this.title = title;
   }
}