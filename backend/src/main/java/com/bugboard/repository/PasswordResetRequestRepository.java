package com.bugboard.repository;

import com.bugboard.model.PasswordResetRequest;
import com.bugboard.model.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PasswordResetRequestRepository {

   @PersistenceContext
   private EntityManager em;

   @Transactional
   public void save(PasswordResetRequest request) {
      em.merge(request);
   }

   public Optional<PasswordResetRequest> findById(Long id) {
      return Optional.ofNullable(em.find(PasswordResetRequest.class, id));
   }

   /**
    * Trova tutte le richieste pendenti (per la dashboard admin)
    */
   public List<PasswordResetRequest> findAllPending() {
      return em.createQuery(
         "SELECT r FROM PasswordResetRequest r WHERE r.status = :status ORDER BY r.requestedAt ASC",
         PasswordResetRequest.class)
         .setParameter("status", PasswordResetRequest.RequestStatus.PENDING)
         .getResultList();
   }

   /**
    * Verifica se l'utente ha già una richiesta pendente
    */
   public boolean hasPendingRequest(User user) {
      Long count = em.createQuery(
         "SELECT COUNT(r) FROM PasswordResetRequest r WHERE r.user = :user AND r.status = :status",
         Long.class)
         .setParameter("user", user)
         .setParameter("status", PasswordResetRequest.RequestStatus.PENDING)
         .getSingleResult();
      return count > 0;
   }
}
