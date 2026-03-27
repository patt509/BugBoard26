package com.bugboard.repository;

import java.util.List;

import com.bugboard.model.IssueHistoryEntry;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class IssueHistoryRepository {

   @PersistenceContext
   private EntityManager em;

   @Transactional
   public void save(IssueHistoryEntry entry) {
      if (entry.getId() == null) {
         em.persist(entry);
      } else {
         em.merge(entry);
      }
   }

   public List<IssueHistoryEntry> findByIssueId(Long issueId) {
      return em.createQuery(
            "SELECT h FROM IssueHistoryEntry h WHERE h.issue.id = :issueId ORDER BY h.createdAt DESC, h.id DESC",
            IssueHistoryEntry.class)
            .setParameter("issueId", issueId)
            .getResultList();
   }
}
