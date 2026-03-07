package com.bugboard.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.bugboard.enums.IssueStatus;
import com.bugboard.enums.IssueType;
import com.bugboard.enums.PriorityLevel;
import com.bugboard.model.Issue;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class IssueRepository {

   @PersistenceContext
   private EntityManager em;

   // Save or update an issue using JPA persistence methods
   @Transactional
   public void save(Issue issue) {
      if (issue.getId() == null) {
         em.persist(issue); // New issue
      } else {
         em.merge(issue); // Updates already existing issue
      }
   }

   // Show all issues in the main board (Requisito 3)
   public List<Issue> findAll() {
      return em.createQuery("SELECT i FROM Issue i ORDER BY i.createdAt DESC", Issue.class)
            .getResultList();
   }

   // Find an issue by its ID (mainly needed by the service layer)
   public Issue findById(Long id) {
      return em.find(Issue.class, id);
   }

   // Dynamic filtering and search of issues (Requisito 3)
   public List<Issue> search(String term, PriorityLevel priority, IssueStatus status, IssueType type, Long assigneeId) {
      StringBuilder jpql = new StringBuilder("SELECT i FROM Issue i LEFT JOIN i.reporter u WHERE 1=1");

      if (term != null && !term.trim().isEmpty()) {
         jpql.append(
               " AND (LOWER(i.title) LIKE LOWER(:term) OR LOWER(i.description) LIKE LOWER(:term) OR LOWER(u.username) LIKE LOWER(:term))");
      }
      if (priority != null) {
         jpql.append(" AND i.priority = :priority");
      }
      if (status != null) {
         jpql.append(" AND i.status = :status");
      }
      if (type != null) {
         jpql.append(" AND i.type = :type");
      }
      if (assigneeId != null) {
         jpql.append(" AND i.assignee.id = :assigneeId");
      }

      jpql.append(" ORDER BY i.createdAt DESC");

      var query = em.createQuery(jpql.toString(), Issue.class);

      if (term != null && !term.trim().isEmpty()) {
         query.setParameter("term", "%" + term + "%");
      }
      if (priority != null) {
         query.setParameter("priority", priority);
      }
      if (status != null) {
         query.setParameter("status", status);
      }
      if (type != null) {
         query.setParameter("type", type);
      }
      if (assigneeId != null) {
         query.setParameter("assigneeId", assigneeId);
      }

      return query.getResultList();
   }

   // Overload for backward compatibility (3-param)
   public List<Issue> search(String term, PriorityLevel priority, IssueStatus status) {
      return search(term, priority, status, null, null);
   }

   // Overload for backward compatibility
   public List<Issue> search(String term, PriorityLevel priority) {
      return search(term, priority, null);
   }

   // ==================== STATISTICS FOR ADMIN DASHBOARD ====================

   /**
    * Count open issues (TODO or IN_PROGRESS) grouped by assignee username.
    * @return list of Object[] where [0] = username (String), [1] = count (Long)
    */
   public List<Object[]> countOpenIssuesPerAssignee() {
      return em.createQuery(
            "SELECT i.assignee.username, COUNT(i) FROM Issue i " +
                  "WHERE i.assignee IS NOT NULL AND (i.status = :todo OR i.status = :inProgress) " +
                  "GROUP BY i.assignee.username",
            Object[].class)
            .setParameter("todo", IssueStatus.TODO)
            .setParameter("inProgress", IssueStatus.IN_PROGRESS)
            .getResultList();
   }

   public long countAll() {
      return em.createQuery("SELECT COUNT(i) FROM Issue i", Long.class)
            .getSingleResult();
   }

   public long countByStatus(IssueStatus status) {
      return em.createQuery("SELECT COUNT(i) FROM Issue i WHERE i.status = :status", Long.class)
            .setParameter("status", status)
            .getSingleResult();
   }

   public long countByPriority(PriorityLevel priority) {
      return em.createQuery("SELECT COUNT(i) FROM Issue i WHERE i.priority = :priority", Long.class)
            .setParameter("priority", priority)
            .getSingleResult();
   }

   public long countDuplicates() {
      return em.createQuery("SELECT COUNT(i) FROM Issue i WHERE i.originalIssue IS NOT NULL", Long.class)
            .getSingleResult();
   }

   public long countCreatedToday() {
      LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
      return em.createQuery("SELECT COUNT(i) FROM Issue i WHERE i.createdAt >= :startOfDay", Long.class)
            .setParameter("startOfDay", startOfDay)
            .getSingleResult();
   }

   public long countClosedToday() {
      LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
      return em.createQuery("SELECT COUNT(i) FROM Issue i WHERE i.closedAt >= :startOfDay", Long.class)
            .setParameter("startOfDay", startOfDay)
            .getSingleResult();
   }

   public long countCreatedSince(LocalDateTime since) {
      return em.createQuery("SELECT COUNT(i) FROM Issue i WHERE i.createdAt >= :since", Long.class)
            .setParameter("since", since)
            .getSingleResult();
   }

   /**
    * Get issues created in the last N days grouped by date.
    * Returns list of Object[] where [0] = date, [1] = count
    */
   public List<Object[]> getIssuesCreatedPerDaySince(LocalDateTime since) {
      return em.createQuery(
            "SELECT FUNCTION('DATE', i.createdAt) as day, COUNT(i) " +
                  "FROM Issue i WHERE i.createdAt >= :since " +
                  "GROUP BY FUNCTION('DATE', i.createdAt) " +
                  "ORDER BY day ASC",
            Object[].class)
            .setParameter("since", since)
            .getResultList();
   }

   /**
    * Calculate average resolution time in hours for resolved/closed issues.
    */
   public Double getAverageResolutionTimeHours() {
      // Only count issues that have both createdAt and closedAt
      List<Issue> closedIssues = em.createQuery(
            "SELECT i FROM Issue i WHERE i.closedAt IS NOT NULL AND i.createdAt IS NOT NULL",
            Issue.class)
            .getResultList();

      if (closedIssues.isEmpty()) {
         return 0.0;
      }

      double totalHours = closedIssues.stream()
            .mapToDouble(i -> java.time.Duration.between(i.getCreatedAt(), i.getClosedAt()).toHours())
            .sum();

      return totalHours / closedIssues.size();
   }
}