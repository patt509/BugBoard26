package com.bugboard.repository;

import java.util.List;
import java.util.Optional;

import com.bugboard.model.AuthSession;
import com.bugboard.model.User;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class AuthSessionRepository {

   @PersistenceContext
   private EntityManager em;

   @Transactional
   public void save(AuthSession session) {
      em.merge(session);
   }

   public Optional<AuthSession> findBySessionId(String sessionId) {
      return em.createQuery("SELECT s FROM AuthSession s JOIN FETCH s.user WHERE s.sessionId = :sessionId", AuthSession.class)
            .setParameter("sessionId", sessionId)
            .getResultStream()
            .findFirst();
   }

   public List<AuthSession> findActiveByUser(User user) {
      return em.createQuery(
            "SELECT s FROM AuthSession s WHERE s.user = :user AND s.revokedAt IS NULL",
            AuthSession.class)
            .setParameter("user", user)
            .getResultList();
   }
}
