package com.bugboard.repository;

import com.bugboard.model.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class UserRepository {
   
   @PersistenceContext
   private EntityManager em;

   @Transactional
   public void save(User user) {
      em.merge(user);
   }

   public Optional<User> findById(Long id) {
      return Optional.ofNullable(em.find(User.class, id));
   }

   public Optional<User> findByEmail(String email) {
      return em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
               .setParameter("email", email)
               .getResultStream()
               .findFirst();
   }

   public Optional<User> findByUsername(String username) {
      return em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class)
               .setParameter("username", username)
               .getResultStream()
               .findFirst();
   }

   public boolean existsByEmail(String email) {
      Long count = em.createQuery("SELECT COUNT(u) FROM User u WHERE u.email = :email", Long.class)
               .setParameter("email", email)
               .getSingleResult();
      return count > 0;
   }

   public boolean existsByUsername(String username) {
      Long count = em.createQuery("SELECT COUNT(u) FROM User u WHERE u.username = :username", Long.class)
               .setParameter("username", username)
               .getSingleResult();
      return count > 0;
   }

   public List<User> findAll() {
      return em.createQuery("SELECT u FROM User u ORDER BY u.createdAt DESC", User.class)
               .getResultList();
   }
}