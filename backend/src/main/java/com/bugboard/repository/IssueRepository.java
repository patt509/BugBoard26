package com.bugboard.repository;

import com.bugboard.enums.PriorityLevel;
import com.bugboard.model.Issue;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class IssueRepository {

   @PersistenceContext
   private EntityManager em;

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

   // Find a issue by its ID (mainly needed by the service layer)
   public Issue findById(Long id) {
      return em.find(Issue.class, id);
   }

   // Dynamic filtering and search of issues (Requisito 3)
   public List<Issue> search(String term, PriorityLevel priority) {
      StringBuilder jpql = new StringBuilder("SELECT i FROM Issue i JOIN i.reporter u WHERE 1=1");

      if (term != null && !term.trim().isEmpty()) {
         jpql.append(" AND (LOWER(i.title) LIKE LOWER(:term) OR LOWER(u.username) LIKE LOWER(:term))");
      }
      if (priority != null) {
         jpql.append(" AND i.priority = :priority");
      }

      jpql.append(" ORDER BY i.createdAt DESC");

      var query = em.createQuery(jpql.toString(), Issue.class);

      if (term != null && !term.trim().isEmpty()) {
         query.setParameter("term", "%" + term + "%");
      }
      if (priority != null) {
         query.setParameter("priority", priority);
      }

      return query.getResultList();
   }
}