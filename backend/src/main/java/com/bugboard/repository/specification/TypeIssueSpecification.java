package com.bugboard.repository.specification;

import com.bugboard.enums.IssueType;

/**
 * Specification for issue type filtering.
 */
final class TypeIssueSpecification implements IssueSpecification {

   private final IssueType type;

   TypeIssueSpecification(IssueType type) {
      this.type = type;
   }

   @Override
   public void apply(IssueQueryContext context) {
      if (type == null) {
         return;
      }
      context.addPredicate("i.type = :type");
      context.addParameter("type", type);
   }
}
