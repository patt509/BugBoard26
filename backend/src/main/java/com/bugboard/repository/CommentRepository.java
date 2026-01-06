package com.bugboard.repository;

import com.bugboard.model.Comment;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CommentRepository {

   @PersistenceContext
   private EntityManager em;

   @Transactional
   public void save(Comment comment) {
      if (comment.getId() == null) {
         em.persist(comment);
      } else {
         em.merge(comment);
      }
   }

   @Transactional
   public void delete(Comment comment) {
      em.remove(em.contains(comment) ? comment : em.merge(comment));
   }

   public Optional<Comment> findById(Long id) {
      return Optional.ofNullable(em.find(Comment.class, id));
   }

   /**
    * Find all comments for a specific issue, ordered by creation date.
    */
   public List<Comment> findByIssueId(Long issueId) {
      return em.createQuery(
            "SELECT c FROM Comment c WHERE c.issue.id = :issueId ORDER BY c.createdAt ASC",
            Comment.class)
            .setParameter("issueId", issueId)
            .getResultList();
   }

   /**
    * Count comments for a specific issue.
    */
   public long countByIssueId(Long issueId) {
      return em.createQuery(
            "SELECT COUNT(c) FROM Comment c WHERE c.issue.id = :issueId",
            Long.class)
            .setParameter("issueId", issueId)
            .getSingleResult();
   }
}
