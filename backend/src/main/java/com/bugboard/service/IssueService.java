package com.bugboard.service;

import com.bugboard.dto.IssueDTO;
import com.bugboard.enums.PriorityLevel;
import com.bugboard.model.Issue;
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

   // Create a new issue
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

      // Call the internal method to mark as duplicate
      duplicate.markAsDuplicateOf(original);
      repository.save(duplicate);

      // TODO: Optionally, notify users about the duplication (parte del Requisito 6)
      System.out.println("LOG: Issue #" + duplicateIssueId + " marked as duplicate of Issue #" + originalIssueId);
   }

   // Search issues with dynamic filters (support function)
   // Convert an object to DTO to pass it to the controller
   public List<IssueDTO> searchIssues(String query, PriorityLevel priority) {
      // 1. Take entities from the repository
      List<Issue> issues = repository.search(query, priority);

      // 2. Convert entities to DTOs
      return issues.stream().map(issue -> new IssueDTO(
              issue.getId(),
              issue.getTitle(),
              issue.getStatus().toString(),
              issue.getPriority().toString(),
              issue.getReporter().getUsername(),
              issue.getCreatedAt(),
              issue.getClosedAt(),
              issue.getAttachmentPath()
      )).toList();
   }
}
