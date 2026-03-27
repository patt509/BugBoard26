package com.bugboard.repository.specification;

import com.bugboard.enums.IssueStatus;

/**
 * Specification for issue status filtering.
 */
final class StatusIssueSpecification implements IssueSpecification {

   private final IssueStatus status;

   StatusIssueSpecification(IssueStatus status) {
      this.status = status;
   }

   @Override
   public void apply(IssueQueryContext context) {
      if (status == null) {
         return;
      }
      context.addPredicate("i.status = :status");
      context.addParameter("status", status);
   }
}
