package com.bugboard.repository.specification;

import com.bugboard.enums.PriorityLevel;

/**
 * Specification for issue priority filtering.
 */
final class PriorityIssueSpecification implements IssueSpecification {

   private final PriorityLevel priority;

   PriorityIssueSpecification(PriorityLevel priority) {
      this.priority = priority;
   }

   @Override
   public void apply(IssueQueryContext context) {
      if (priority == null) {
         return;
      }
      context.addPredicate("i.priority = :priority");
      context.addParameter("priority", priority);
   }
}
