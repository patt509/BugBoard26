package com.bugboard.repository;

import com.bugboard.model.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transtaction.Transactional;
import java.util.Optional;

@ApplicationScoped
public class UserRepository {
   
   @PersistenceContext
   private EntityManager em;

   @Transactional
   public void save(User user) {
      em.persist(user);
   }

   public Optional<User> findByEmail(String email) {
      return em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
               .setParameter("email", email)
               .getResultStream();
               .findFirst();
   }
}