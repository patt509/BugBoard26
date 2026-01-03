package com.bugboard.service;

import com.bugboard.dto.IssueDTO;
import com.bugboard.enums.IssueStatus;
import com.bugboard.enums.PriorityLevel;
import com.bugboard.model.Issue;
import com.bugboard.model.User;
import com.bugboard.repository.IssueRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class IssueService {

   private static final Logger logger = Logger.getLogger(IssueService.class.getName());

   private final IssueRepository repository;

   @Inject
   public IssueService(IssueRepository repository) {
      this.repository = repository;
   }

   // Create a new issue with minimal parameters
   @Transactional
   public Long createIssue(String title, String description, User reporter) {
      Issue issue = new Issue(title, description, reporter);
      repository.save(issue);
      return issue.getId();
   }

   @Transactional
   public Long createIssue(IssueDTO dto, User reporter) {
      // Service creates the entity from the DTO
      Issue issue = new Issue(dto.getTitle(), dto.getDescription(), reporter);
      
      if (dto.getPriority() != null) {
         issue.setPriority(PriorityLevel.valueOf(dto.getPriority()));
      }
      
      repository.save(issue);
      return issue.getId();
   }

   @Transactional
   public void updateStatus(Long id, IssueStatus newStatus) {
      Issue issue = repository.findById(id);
      if (issue == null) throw new IllegalArgumentException("Issue non trovata");
      
      // Issue.java should handle automatically setting closedAt when status changes to CLOSED
      issue.setStatus(newStatus);
      repository.save(issue);
   }

   // Duplicate management by admins
   @Transactional
   public void processDuplicate(Long duplicateIssueId, Long originalIssueId) {
        if (duplicateIssueId == null || originalIssueId == null) {
          throw new IllegalArgumentException("Both duplicateId and originalId must be provided.");
        }

      Issue duplicate = repository.findById(duplicateIssueId);
      Issue original = repository.findById(originalIssueId);

      if (duplicate == null || original == null) {
         throw new IllegalArgumentException("One or both issues not found.");
      }

      // Call the internal method to mark as duplicate
      duplicate.markAsDuplicateOf(original);
      repository.save(duplicate);

      // TODO: Optionally, notify users about the duplication (parte del Requisito 6)
      logger.log(Level.INFO, "Issue #{0} marked as duplicate of Issue #{1}", 
         new Object[]{duplicateIssueId, originalIssueId});
   }

   // Search issues with dynamic filters (support function)
   // Convert an object to DTO to pass it to the controller
   public List<IssueDTO> searchIssues(String query, PriorityLevel priority) {
      // 1. Take entities from the repository
      List<Issue> issues = repository.search(query, priority);

      // 2. Convert entities to DTOs
      return issues.stream().map(issue -> IssueDTO.builder()
              .id(issue.getId())
              .title(issue.getTitle())
              .status(issue.getStatus().toString())
              .priority(issue.getPriority().toString())
              .reporterName(issue.getReporter().getUsername())
              .createdAt(issue.getCreatedAt())
              .closedAt(issue.getClosedAt())
              .attachmentPath(issue.getAttachmentPath())
              .build()
      ).toList();
   }
}
