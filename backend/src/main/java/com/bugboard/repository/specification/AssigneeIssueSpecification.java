package com.bugboard.repository.specification;

/**
 * Specification for issue assignee filtering.
 */
final class AssigneeIssueSpecification implements IssueSpecification {

   private final Long assigneeId;

   AssigneeIssueSpecification(Long assigneeId) {
      this.assigneeId = assigneeId;
   }

   @Override
   public void apply(IssueQueryContext context) {
      if (assigneeId == null) {
         return;
      }
      context.addPredicate("i.assignee.id = :assigneeId");
      context.addParameter("assigneeId", assigneeId);
   }
}
