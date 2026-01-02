package com.bugboard.service;

import com.bugboard.model.Issue;
import com.bugboard.model.PriorityLevel;
import com.bugboard.model.User;
import com.bugboard.repository.IssueRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class IssueService {

   @Inject
   private IssueRepository repository;

   // Create new issue
   @Transactional
   public void createIssue(String title, User reporter) {
      Issue issue = new Issue(title, reporter);
      repository.save(issue);
   }

   // Duplicate management by admins
   @Transactional
   public void processDuplicate(Long duplicateIssueId, Long originalIssueId) {
      Issue duplicate = repository.findById(duplicateIssueId);
      Issue original = repository.findById(originalIssueId);

      if (duplicate == null || original == null) {
         throw new IllegalArgumentException("One or both issues not found.");
      }

      // Call internal method to mark as duplicate
      duplicate.markAsDuplicate(original);
      repository.save(duplicate);

      // TODO: Optionally, notify users about the duplication (parte del Requisito 6)
      System.out.println("LOG: Issue #" + duplicateIssueId + " marked as duplicate of Issue #" + originalIssueId);
   }

   // Search issues with dynamic filters (support function)
   public List<Issue> searchIssues(String query, PriorityLevel priority) {
      return repository.search(query, priority); // Call repository method
   }
}
