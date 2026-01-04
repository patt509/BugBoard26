package com.bugboard.service;

import com.bugboard.dto.DashboardStatsDTO;
import com.bugboard.dto.IssueDTO;
import com.bugboard.enums.IssueStatus;
import com.bugboard.enums.PriorityLevel;
import com.bugboard.model.Issue;
import com.bugboard.model.User;
import com.bugboard.repository.IssueRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

   // ==================== ISSUE CRUD OPERATIONS ====================

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
      if (issue == null) {
         throw new IllegalArgumentException("Issue not found");
      }

      // Issue.java handles automatically setting closedAt when status changes to
      // CLOSED
      issue.setStatus(newStatus);
      repository.save(issue);
   }

   // ==================== ATTACHMENT OPERATIONS ====================

   /**
    * Sets the attachment path for an issue.
    * Called after the file has been saved by AttachmentService.
    */
   @Transactional
   public void setAttachmentPath(Long issueId, String attachmentPath) {
      Issue issue = repository.findById(issueId);
      if (issue == null) {
         throw new IllegalArgumentException("Issue not found");
      }
      issue.setAttachmentPath(attachmentPath);
      repository.save(issue);
   }

   /**
    * Removes the attachment from an issue.
    */
   @Transactional
   public String removeAttachment(Long issueId) {
      Issue issue = repository.findById(issueId);
      if (issue == null) {
         throw new IllegalArgumentException("Issue not found");
      }
      String oldPath = issue.getAttachmentPath();
      issue.setAttachmentPath(null);
      repository.save(issue);
      return oldPath;
   }

   // ==================== BOARD VIEW OPERATIONS ====================

   /**
    * Get all issues for the board view.
    * Available to all authenticated users.
    */
   public List<IssueDTO> getAllIssues() {
      List<Issue> issues = repository.findAll();
      return convertToDTO(issues);
   }

   /**
    * Search issues with optional filters.
    * Available to all authenticated users.
    */
   public List<IssueDTO> searchIssues(String query, PriorityLevel priority, IssueStatus status) {
      List<Issue> issues = repository.search(query, priority, status);
      return convertToDTO(issues);
   }

   // Overload for backward compatibility
   public List<IssueDTO> searchIssues(String query, PriorityLevel priority) {
      return searchIssues(query, priority, null);
   }

   /**
    * Get a single issue by ID.
    */
   public IssueDTO getIssueById(Long id) {
      Issue issue = repository.findById(id);
      if (issue == null) {
         throw new IllegalArgumentException("Issue not found");
      }
      return convertSingleToDTO(issue);
   }

   // ==================== DUPLICATE MANAGEMENT (ADMIN ONLY) ====================

   /**
    * Mark an issue as duplicate of another.
    * Only admins can perform this action.
    */
   @Transactional
   public void processDuplicate(Long duplicateIssueId, Long originalIssueId, User admin) {
      if (admin == null || !admin.isAdmin()) {
         throw new SecurityException("Only administrators can mark issues as duplicate.");
      }

      if (duplicateIssueId == null || originalIssueId == null) {
         throw new IllegalArgumentException("Both duplicateId and originalId must be provided.");
      }

      Issue duplicate = repository.findById(duplicateIssueId);
      Issue original = repository.findById(originalIssueId);

      if (duplicate == null || original == null) {
         throw new IllegalArgumentException("One or both issues not found.");
      }

      // Delegate to domain model for business logic
      duplicate.markAsDuplicateOf(original);
      repository.save(duplicate);

      logger.log(Level.INFO, "Admin {0} marked Issue #{1} as duplicate of Issue #{2}",
            new Object[] { admin.getEmail(), duplicateIssueId, originalIssueId });
   }

   // ==================== ADMIN DASHBOARD STATISTICS ====================

   /**
    * Get real-time statistics for admin dashboard.
    * Only admins should call this method.
    */
   public DashboardStatsDTO getDashboardStats() {
      // Count by status
      Map<String, Long> issuesByStatus = new LinkedHashMap<>();
      for (IssueStatus status : IssueStatus.values()) {
         issuesByStatus.put(status.name(), repository.countByStatus(status));
      }

      // Count by priority
      Map<String, Long> issuesByPriority = new LinkedHashMap<>();
      for (PriorityLevel priority : PriorityLevel.values()) {
         issuesByPriority.put(priority.name(), repository.countByPriority(priority));
      }

      // Issues created per day (last 7 days)
      LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
      Map<String, Long> issuesCreatedPerDay = new LinkedHashMap<>();
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

      List<Object[]> dailyData = repository.getIssuesCreatedPerDaySince(sevenDaysAgo);
      for (Object[] row : dailyData) {
         String date = row[0].toString();
         Long count = (Long) row[1];
         issuesCreatedPerDay.put(date, count);
      }

      // Calculate open issues (TODO + IN_PROGRESS)
      long openIssues = repository.countByStatus(IssueStatus.TODO) +
            repository.countByStatus(IssueStatus.IN_PROGRESS);

      return DashboardStatsDTO.builder()
            .totalIssues(repository.countAll())
            .openIssues(openIssues)
            .resolvedIssues(repository.countByStatus(IssueStatus.RESOLVED))
            .closedIssues(repository.countByStatus(IssueStatus.CLOSED))
            .duplicateIssues(repository.countDuplicates())
            .issuesByStatus(issuesByStatus)
            .issuesByPriority(issuesByPriority)
            .issuesCreatedPerDay(issuesCreatedPerDay)
            .avgResolutionTimeHours(repository.getAverageResolutionTimeHours())
            .issuesCreatedToday(repository.countCreatedToday())
            .issuesClosedToday(repository.countClosedToday())
            .build();
   }

   // ==================== PRIVATE HELPER METHODS ====================

   private List<IssueDTO> convertToDTO(List<Issue> issues) {
      return issues.stream()
            .map(this::convertSingleToDTO)
            .toList();
   }

   private IssueDTO convertSingleToDTO(Issue issue) {
      String reporterName = issue.getReporter() != null
            ? issue.getReporter().getUsername()
            : "Unknown";

      return IssueDTO.builder()
            .id(issue.getId())
            .title(issue.getTitle())
            .description(issue.getDescription())
            .status(issue.getStatus().toString())
            .priority(issue.getPriority().toString())
            .reporterName(reporterName)
            .createdAt(issue.getCreatedAt())
            .closedAt(issue.getClosedAt())
            .attachmentPath(issue.getAttachmentPath())
            .originalIssueId(issue.getOriginalIssueId())
            .build();
   }
}
