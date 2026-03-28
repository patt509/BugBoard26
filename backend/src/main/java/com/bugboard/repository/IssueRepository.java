package com.bugboard.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.bugboard.enums.IssueStatus;
import com.bugboard.enums.IssueType;
import com.bugboard.enums.PriorityLevel;
import com.bugboard.model.Issue;
import com.bugboard.repository.specification.IssueQueryContext;
import com.bugboard.repository.specification.IssueSpecification;
import com.bugboard.repository.specification.IssueSpecifications;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class IssueRepository {

   private static final String FETCH_REPORTER_CLAUSE = "LEFT JOIN FETCH i.reporter ";
   private static final String FETCH_ASSIGNEE_CLAUSE = "LEFT JOIN FETCH i.assignee ";

   @PersistenceContext
   private EntityManager em;

   // Save or update an issue using JPA persistence methods
   @Transactional
   public void save(Issue issue) {
      if (issue.getId() == null) {
         em.persist(issue); // New issue
         em.flush(); // Force ID generation
      } else {
         em.merge(issue); // Updates already existing issue
      }
   }

   // Show all issues in the main board (Requisito 3)
   public List<Issue> findAll() {
      return em.createQuery(
            "SELECT DISTINCT i FROM Issue i " +
                  FETCH_REPORTER_CLAUSE +
                  FETCH_ASSIGNEE_CLAUSE +
                  "ORDER BY i.createdAt DESC, i.id DESC",
            Issue.class)
            .getResultList();
   }

   // Find an issue by its ID (mainly needed by the service layer)
   public Issue findById(Long id) {
      try {
         return em.createQuery(
               "SELECT i FROM Issue i " +
                     FETCH_REPORTER_CLAUSE +
                     FETCH_ASSIGNEE_CLAUSE +
                     "WHERE i.id = :id",
               Issue.class)
               .setParameter("id", id)
               .getSingleResult();
      } catch (NoResultException ex) {
         return null;
      }
   }

   // Dynamic filtering and search of issues (Requisito 3)
   public List<Issue> search(String term, PriorityLevel priority, IssueStatus status, IssueType type, Long assigneeId) {
      StringBuilder jpql = new StringBuilder(
            "SELECT DISTINCT i FROM Issue i " +
                  "LEFT JOIN FETCH i.reporter reporter " +
                  "LEFT JOIN FETCH i.assignee assignee");

      IssueQueryContext queryContext = new IssueQueryContext();
      IssueSpecification filters = IssueSpecifications.allOf(
            IssueSpecifications.byTerm(term),
            IssueSpecifications.byPriority(priority),
            IssueSpecifications.byStatus(status),
            IssueSpecifications.byType(type),
            IssueSpecifications.byAssigneeId(assigneeId));
      filters.apply(queryContext);

      if (!queryContext.getPredicates().isEmpty()) {
         jpql.append(" WHERE ")
               .append(String.join(" AND ", queryContext.getPredicates()));
      }

      jpql.append(" ORDER BY i.createdAt DESC, i.id DESC");

      var query = em.createQuery(jpql.toString(), Issue.class);
      queryContext.getParameters().forEach(query::setParameter);

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
    * Count open and in-progress issues grouped by assignee username.
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

   public List<Object[]> countByStatusGrouped() {
      return em.createQuery(
            "SELECT i.status, COUNT(i) FROM Issue i GROUP BY i.status",
            Object[].class)
            .getResultList();
   }

   public List<Object[]> countByPriorityGrouped() {
      return em.createQuery(
            "SELECT i.priority, COUNT(i) FROM Issue i GROUP BY i.priority",
            Object[].class)
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

   public List<Issue> findClosedResolvedIssuesWithAssignee() {
      return em.createQuery(
            "SELECT i FROM Issue i " +
                  FETCH_ASSIGNEE_CLAUSE +
                  "WHERE i.assignee IS NOT NULL " +
                  "AND i.createdAt IS NOT NULL " +
                  "AND i.closedAt IS NOT NULL " +
                  "AND (i.status = :closed OR i.status = :resolved)",
            Issue.class)
            .setParameter("closed", IssueStatus.CLOSED)
            .setParameter("resolved", IssueStatus.RESOLVED)
            .getResultList();
   }
}
