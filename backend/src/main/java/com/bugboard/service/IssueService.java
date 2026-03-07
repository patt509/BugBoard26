package com.bugboard.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.bugboard.dto.DashboardStatsDTO;
import com.bugboard.dto.IssueDTO;
import com.bugboard.enums.IssueStatus;
import com.bugboard.enums.IssueType;
import com.bugboard.enums.PriorityLevel;
import com.bugboard.model.Issue;
import com.bugboard.model.User;
import com.bugboard.repository.IssueRepository;
import com.bugboard.repository.UserRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class IssueService {

   private static final Logger logger = Logger.getLogger(IssueService.class.getName());

   private final IssueRepository repository;
   private final UserRepository userRepository;

   @Inject
   public IssueService(IssueRepository repository, UserRepository userRepository) {
      this.repository = repository;
      this.userRepository = userRepository;
   }

   // ==================== ISSUE CRUD OPERATIONS ====================

   // Create a new issue with minimal parameters
   @Transactional
   public Long createIssue(String title, String description, Long reporterId, IssueType type) {
      User reporter = reporterId != null ? userRepository.findById(reporterId).orElse(null) : null;
      Issue issue = new Issue(title, description, reporter, type);
      repository.save(issue);
      return issue.getId();
   }

   @Transactional
   public Long createIssue(IssueDTO dto, Long reporterId) {
      // Fetch reporter from repository if provided
      User reporter = reporterId != null ? userRepository.findById(reporterId).orElse(null) : null;

      // Parse type from DTO (mandatory)
      IssueType type = dto.getType() != null ? IssueType.valueOf(dto.getType()) : null;

      // Service creates the entity from the DTO
      Issue issue = new Issue(dto.getTitle(), dto.getDescription(), reporter, type);

      if (dto.getPriority() != null) {
         issue.setPriority(PriorityLevel.valueOf(dto.getPriority()));
      }

      // Handle optional assignee
      if (dto.getAssigneeUsername() != null && !dto.getAssigneeUsername().isBlank()) {
         User assignee = userRepository.findByUsername(dto.getAssigneeUsername()).orElse(null);
         if (assignee == null) {
            throw new IllegalArgumentException("Assignee user not found: " + dto.getAssigneeUsername());
         }
         issue.setAssignee(assignee);
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
    * @return the old attachment path (for deletion by AttachmentService)
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

   /**
    * Checks if an issue exists.
    * @return true if issue exists, false otherwise
    */
   public boolean validateIssueExists(Long issueId) {
      Issue issue = repository.findById(issueId);
      return issue != null;
   }

   /**
    * Gets the current attachment path for an issue.
    * @return the attachment path, or null if no attachment
    * @throws IllegalArgumentException if issue not found
    */
   public String getIssueAttachmentPath(Long issueId) {
      Issue issue = repository.findById(issueId);
      if (issue == null) {
         throw new IllegalArgumentException("Issue not found");
      }
      return issue.getAttachmentPath();
   }

   /**
    * Checks if an issue has an attachment.
    * @throws IllegalArgumentException if issue not found
    */
   public boolean issueHasAttachment(Long issueId) {
      return getIssueAttachmentPath(issueId) != null;
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
    * 
    * @param duplicateIssueId the ID of the issue to mark as duplicate
    * @param originalIssueId  the ID of the original issue
    * @param adminId          the ID of the admin performing the action
    */
   @Transactional
   public void processDuplicate(Long duplicateIssueId, Long originalIssueId, Long adminId) {
      // Validate admin privileges
      if (adminId == null) {
         throw new SecurityException("Authentication required.");
      }
      
      User admin = userRepository.findById(adminId)
            .orElseThrow(() -> new SecurityException("User not found."));
      
      if (!admin.isAdmin()) {
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

      String assigneeUsername = issue.getAssignee() != null
            ? issue.getAssignee().getUsername()
            : null;

      return IssueDTO.builder()
            .id(issue.getId())
            .title(issue.getTitle())
            .description(issue.getDescription())
            .status(issue.getStatus().toString())
            .priority(issue.getPriority().toString())
            .type(issue.getType().toString())
            .reporterName(reporterName)
            .assigneeUsername(assigneeUsername)
            .createdAt(issue.getCreatedAt())
            .closedAt(issue.getClosedAt())
            .attachmentPath(issue.getAttachmentPath())
            .originalIssueId(issue.getOriginalIssueId())
            .build();
   }
}
