package com.bugboard.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.bugboard.dto.DashboardStatsDTO;
import com.bugboard.dto.IssueDTO;
import com.bugboard.dto.IssueHistoryDTO;
import com.bugboard.enums.IssueStatus;
import com.bugboard.enums.IssueType;
import com.bugboard.enums.PriorityLevel;
import com.bugboard.model.Issue;
import com.bugboard.model.IssueHistoryEntry;
import com.bugboard.model.User;
import com.bugboard.repository.IssueHistoryRepository;
import com.bugboard.repository.IssueRepository;
import com.bugboard.repository.UserRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class IssueService {

   private static final Logger logger = Logger.getLogger(IssueService.class.getName());

   private final IssueRepository repository;
   private final IssueHistoryRepository issueHistoryRepository;
   private final UserRepository userRepository;

   // CDI requires no-arg constructor for proxy
   protected IssueService() {
      this.repository = null;
      this.issueHistoryRepository = null;
      this.userRepository = null;
   }

   @Inject
   public IssueService(IssueRepository repository, IssueHistoryRepository issueHistoryRepository, UserRepository userRepository) {
      this.repository = repository;
      this.issueHistoryRepository = issueHistoryRepository;
      this.userRepository = userRepository;
   }

   // ==================== ISSUE CRUD OPERATIONS ====================

   // Create a new issue with minimal parameters
   @Transactional
   public Long createIssue(String title, String description, Long reporterId, IssueType type) {
      User reporter = reporterId != null ? userRepository.findById(reporterId).orElse(null) : null;
      Issue issue = new Issue(title, description, reporter, type);
      repository.save(issue);
      recordHistoryEntry(issue, "Issue created", "Reported by " + resolveUserLabel(reporter) + ".");
      return issue.getId();
   }

   @Transactional
   public Long createIssue(IssueDTO dto, Long reporterId) {
      if (dto == null) {
         throw new IllegalArgumentException("Issue payload is required.");
      }

      User reporter = null;
      if (reporterId != null) {
         reporter = userRepository.findById(reporterId)
               .orElseThrow(() -> new IllegalArgumentException("Reporter not found with ID: " + reporterId));
      }

      IssueType type = parseIssueType(dto.getType());

      // Service creates the entity from the DTO
      Issue issue = new Issue(dto.getTitle(), dto.getDescription(), reporter, type);

      if (dto.getPriority() != null) {
         issue.setPriority(parsePriority(dto.getPriority()));
      }

      // Handle optional assignee
      if (dto.getAssigneeId() != null) {
         User assignee = userRepository.findById(dto.getAssigneeId())
               .orElseThrow(() -> new IllegalArgumentException("Assignee user not found with ID: " + dto.getAssigneeId()));
         issue.setAssignee(assignee);
      } else if (dto.getAssigneeUsername() != null && !dto.getAssigneeUsername().isBlank()) {
         String normalizedAssigneeUsername = dto.getAssigneeUsername().trim();
         User assignee = userRepository.findByUsername(normalizedAssigneeUsername).orElse(null);
         if (assignee == null) {
            throw new IllegalArgumentException("Assignee user not found: " + normalizedAssigneeUsername);
         }
         issue.setAssignee(assignee);
      }

      repository.save(issue);
      recordHistoryEntry(issue, "Issue created", "Reported by " + resolveUserLabel(reporter) + ".");
      return issue.getId();
   }

   @Transactional
   public void updateIssue(Long id, IssueDTO dto) {
      Issue issue = repository.findById(id);
      if (issue == null) {
         throw new IllegalArgumentException("Issue not found");
      }

      List<String> changes = new ArrayList<>();

      // Update fields if provided
      if (dto.getTitle() != null && !dto.getTitle().trim().isEmpty()) {
         String normalizedTitle = dto.getTitle().trim();
         if (!normalizedTitle.equals(issue.getTitle())) {
            changes.add("Title: \"" + issue.getTitle() + "\" -> \"" + normalizedTitle + "\"");
            issue.setTitle(normalizedTitle);
         }
      }
      if (dto.getDescription() != null) {
         String normalizedDescription = dto.getDescription().trim();
         if (normalizedDescription.isEmpty()) {
            throw new IllegalArgumentException("Description cannot be empty.");
         }
         if (!normalizedDescription.equals(issue.getDescription())) {
            changes.add("Description updated.");
            issue.setDescription(normalizedDescription);
         }
      }
      if (dto.getPriority() != null) {
         PriorityLevel nextPriority = parsePriority(dto.getPriority());
         if (nextPriority != issue.getPriority()) {
            changes.add("Priority: " + issue.getPriority() + " -> " + nextPriority);
            issue.setPriority(nextPriority);
         }
      }

      if (dto.getType() != null && !dto.getType().isBlank()) {
         IssueType nextType = parseIssueType(dto.getType());
         if (nextType != issue.getType()) {
            changes.add("Type: " + issue.getType() + " -> " + nextType);
            issue.setType(nextType);
         }
      }

      User nextAssignee = null;
      boolean assigneeProvided = false;
      if (dto.getAssigneeId() != null) {
         assigneeProvided = true;
         nextAssignee = userRepository.findById(dto.getAssigneeId())
               .orElseThrow(() -> new IllegalArgumentException("Assignee user not found with ID: " + dto.getAssigneeId()));
      } else if (dto.getAssigneeUsername() != null && !dto.getAssigneeUsername().isBlank()) {
         assigneeProvided = true;
         String normalizedAssigneeUsername = dto.getAssigneeUsername().trim();
         nextAssignee = userRepository.findByUsername(normalizedAssigneeUsername)
               .orElseThrow(() -> new IllegalArgumentException("Assignee user not found: " + normalizedAssigneeUsername));
      }

      if (assigneeProvided && !isSameUser(issue.getAssignee(), nextAssignee)) {
         changes.add("Assignee: " + resolveUserLabel(issue.getAssignee()) + " -> " + resolveUserLabel(nextAssignee));
         issue.setAssignee(nextAssignee);
      }

      if (changes.isEmpty()) {
         return;
      }

      repository.save(issue);
      recordHistoryEntry(issue, "Issue updated", String.join(" | ", changes));
   }

   @Transactional
   public void updateStatus(Long id, IssueStatus newStatus) {
      Issue issue = repository.findById(id);
      if (issue == null) {
         throw new IllegalArgumentException("Issue not found");
      }

      IssueStatus previousStatus = issue.getStatus();
      if (previousStatus == newStatus) {
         return;
      }

      // Issue.java handles automatically setting closedAt when status changes to
      // CLOSED
      issue.setStatus(newStatus);
      repository.save(issue);
      recordHistoryEntry(issue, "Status changed", "Status: " + previousStatus + " -> " + newStatus);
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
      String oldPath = issue.getAttachmentPath();
      issue.setAttachmentPath(attachmentPath);
      repository.save(issue);
      if (oldPath == null && attachmentPath != null) {
         recordHistoryEntry(issue, "Attachment added", "Added attachment: " + extractFileName(attachmentPath));
      } else if (oldPath != null && attachmentPath != null && !oldPath.equals(attachmentPath)) {
         recordHistoryEntry(issue, "Attachment replaced",
               "Replaced attachment: " + extractFileName(oldPath) + " -> " + extractFileName(attachmentPath));
      }
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
      if (oldPath != null) {
         recordHistoryEntry(issue, "Attachment removed", "Removed attachment: " + extractFileName(oldPath));
      }
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
   public List<IssueDTO> searchIssues(String query, PriorityLevel priority, IssueStatus status,
         IssueType type, Long assigneeId) {
      String normalizedQuery = normalizeQuery(query);
      List<Issue> issues = repository.search(normalizedQuery, priority, status, type, assigneeId);
      return convertToDTO(issues);
   }

   // Overload for backward compatibility
   public List<IssueDTO> searchIssues(String query, PriorityLevel priority, IssueStatus status) {
      return searchIssues(query, priority, status, null, null);
   }

   // Overload for backward compatibility
   public List<IssueDTO> searchIssues(String query, PriorityLevel priority) {
      return searchIssues(query, priority, null, null, null);
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

   public List<IssueHistoryDTO> getIssueHistory(Long issueId) {
      Issue issue = repository.findById(issueId);
      if (issue == null) {
         throw new IllegalArgumentException("Issue not found");
      }

      return issueHistoryRepository.findByIssueId(issueId).stream()
            .map(entry -> new IssueHistoryDTO(
                  entry.getId(),
                  entry.getCreatedAt(),
                  entry.getTitle(),
                  entry.getDescription()))
            .toList();
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
      if (duplicateIssueId.equals(originalIssueId)) {
         throw new IllegalArgumentException("Duplicate and original issue IDs must be different.");
      }

      Issue duplicate = repository.findById(duplicateIssueId);
      Issue original = repository.findById(originalIssueId);

      if (duplicate == null || original == null) {
         throw new IllegalArgumentException("One or both issues not found.");
      }
      if (duplicate.getType() != IssueType.BUG || original.getType() != IssueType.BUG) {
         throw new IllegalArgumentException("Duplicate workflow is allowed only for BUG issues.");
      }

      // Delegate to domain model for business logic
      IssueStatus previousStatus = duplicate.getStatus();
      duplicate.markAsDuplicateOf(original);
      repository.save(duplicate);
      recordHistoryEntry(
            duplicate,
            "Marked as duplicate",
            "Duplicate of issue #" + originalIssueId + ". Status: " + previousStatus + " -> " + duplicate.getStatus());

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
         issuesByStatus.put(status.name(), 0L);
      }
      List<Object[]> statusRows = repository.countByStatusGrouped();
      if (statusRows != null) {
         for (Object[] row : statusRows) {
            IssueStatus status = (IssueStatus) row[0];
            Long count = (Long) row[1];
            issuesByStatus.put(status.name(), count);
         }
      }

      // Count by priority
      Map<String, Long> issuesByPriority = new LinkedHashMap<>();
      for (PriorityLevel priority : PriorityLevel.values()) {
         issuesByPriority.put(priority.name(), 0L);
      }
      List<Object[]> priorityRows = repository.countByPriorityGrouped();
      if (priorityRows != null) {
         for (Object[] row : priorityRows) {
            PriorityLevel priority = (PriorityLevel) row[0];
            Long count = (Long) row[1];
            issuesByPriority.put(priority.name(), count);
         }
      }

      // Issues created per day (last 7 days)
      LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
      Map<String, Long> issuesCreatedPerDay = new LinkedHashMap<>();

      List<Object[]> dailyData = repository.getIssuesCreatedPerDaySince(sevenDaysAgo);
      for (Object[] row : dailyData) {
         String date = row[0].toString();
         Long count = (Long) row[1];
         issuesCreatedPerDay.put(date, count);
      }

      // Calculate open issues (TODO + IN_PROGRESS)
      long openIssues = countFromMap(issuesByStatus, IssueStatus.TODO.name()) +
            countFromMap(issuesByStatus, IssueStatus.IN_PROGRESS.name());
      long resolvedIssues = countFromMap(issuesByStatus, IssueStatus.RESOLVED.name());
      long closedIssues = countFromMap(issuesByStatus, IssueStatus.CLOSED.name());

      // Count open issues per assignee (Requisito 7)
      Map<String, Long> issuesAssignedPerUser = new LinkedHashMap<>();
      userRepository.findAssignableUsers().forEach(user -> {
         if (user.getUsername() != null) {
            issuesAssignedPerUser.put(user.getUsername(), 0L);
         }
      });
      List<Object[]> assigneeData = repository.countOpenIssuesPerAssignee();
      for (Object[] row : assigneeData) {
         String username = (String) row[0];
         Long count = (Long) row[1];
         issuesAssignedPerUser.put(username, count);
      }

      // Average resolution time (hours) grouped by assignee username (R7)
      Map<String, Double> avgResolutionTimeHoursPerUser = new LinkedHashMap<>();
      userRepository.findAssignableUsers().forEach(user -> {
         if (user.getUsername() != null) {
            avgResolutionTimeHoursPerUser.put(user.getUsername(), 0.0);
         }
      });

      Map<String, Long> resolvedIssuesCountPerUser = new LinkedHashMap<>();
      Map<String, Double> totalResolutionHoursPerUser = new LinkedHashMap<>();
      List<Issue> closedResolvedAssignedIssues = repository.findClosedResolvedIssuesWithAssignee();
      if (closedResolvedAssignedIssues != null) {
         for (Issue resolvedIssue : closedResolvedAssignedIssues) {
            if (resolvedIssue.getAssignee() == null || resolvedIssue.getAssignee().getUsername() == null) {
               continue;
            }
            if (resolvedIssue.getCreatedAt() == null || resolvedIssue.getClosedAt() == null) {
               continue;
            }

            String username = resolvedIssue.getAssignee().getUsername();
            double resolutionHours = Duration.between(resolvedIssue.getCreatedAt(), resolvedIssue.getClosedAt()).toMinutes() / 60.0;

            totalResolutionHoursPerUser.merge(username, resolutionHours, Double::sum);
            resolvedIssuesCountPerUser.merge(username, 1L, Long::sum);
         }
      }

      for (Map.Entry<String, Double> totalByUserEntry : totalResolutionHoursPerUser.entrySet()) {
         String username = totalByUserEntry.getKey();
         long resolvedCount = resolvedIssuesCountPerUser.getOrDefault(username, 0L);
         if (resolvedCount > 0) {
            avgResolutionTimeHoursPerUser.put(username, totalByUserEntry.getValue() / resolvedCount);
         }
      }

      return DashboardStatsDTO.builder()
            .totalIssues(repository.countAll())
            .openIssues(openIssues)
            .resolvedIssues(resolvedIssues)
            .closedIssues(closedIssues)
            .duplicateIssues(repository.countDuplicates())
            .issuesByStatus(issuesByStatus)
            .issuesByPriority(issuesByPriority)
            .issuesCreatedPerDay(issuesCreatedPerDay)
            .avgResolutionTimeHours(repository.getAverageResolutionTimeHours())
            .issuesCreatedToday(repository.countCreatedToday())
            .issuesClosedToday(repository.countClosedToday())
            .issuesAssignedPerUser(issuesAssignedPerUser)
            .avgResolutionTimeHoursPerUser(avgResolutionTimeHoursPerUser)
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
      Long assigneeId = issue.getAssignee() != null
            ? issue.getAssignee().getId()
            : null;

      return IssueDTO.builder()
            .id(issue.getId())
            .title(issue.getTitle())
            .description(issue.getDescription())
            .status(issue.getStatus().toString())
            .priority(issue.getPriority().toString())
            .type(issue.getType() != null ? issue.getType().toString() : IssueType.BUG.toString())
            .reporterName(reporterName)
            .assigneeUsername(assigneeUsername)
            .assigneeId(assigneeId)
            .createdAt(issue.getCreatedAt())
            .updatedAt(issue.getUpdatedAt())
            .closedAt(issue.getClosedAt())
            .attachmentPath(issue.getAttachmentPath())
            .originalIssueId(issue.getOriginalIssueId())
            .build();
   }

   private PriorityLevel parsePriority(String rawPriority) {
      if (rawPriority == null || rawPriority.trim().isEmpty()) {
         throw new IllegalArgumentException("Priority is required.");
      }

      try {
         return PriorityLevel.valueOf(rawPriority.trim().toUpperCase());
      } catch (IllegalArgumentException ex) {
         throw new IllegalArgumentException("Invalid priority: " + rawPriority);
      }
   }

   private IssueType parseIssueType(String rawType) {
      if (rawType == null || rawType.isBlank()) {
         throw new IllegalArgumentException("Issue type is required.");
      }

      try {
         return IssueType.valueOf(rawType.trim().toUpperCase());
      } catch (IllegalArgumentException ex) {
         throw new IllegalArgumentException("Invalid issue type: " + rawType);
      }
   }

   private String normalizeQuery(String query) {
      if (query == null) {
         return null;
      }
      String normalized = query.trim();
      return normalized.isEmpty() ? null : normalized;
   }

   private long countFromMap(Map<String, Long> source, String key) {
      Long value = source.get(key);
      return value != null ? value : 0L;
   }

   private void recordHistoryEntry(Issue issue, String title, String description) {
      issueHistoryRepository.save(new IssueHistoryEntry(issue, title, description));
   }

   private boolean isSameUser(User first, User second) {
      if (first == null && second == null) {
         return true;
      }
      if (first == null || second == null) {
         return false;
      }

      return Objects.equals(first.getId(), second.getId());
   }

   private String resolveUserLabel(User user) {
      if (user == null) {
         return "Unassigned";
      }
      if (user.getUsername() != null && !user.getUsername().isBlank()) {
         return user.getUsername();
      }
      if (user.getEmail() != null && !user.getEmail().isBlank()) {
         return user.getEmail();
      }
      return "Unknown user";
   }

   private String extractFileName(String path) {
      if (path == null || path.isBlank()) {
         return "attachment";
      }
      int slashIndex = path.lastIndexOf('/');
      if (slashIndex >= 0 && slashIndex < path.length() - 1) {
         return path.substring(slashIndex + 1);
      }
      return path;
   }
}
