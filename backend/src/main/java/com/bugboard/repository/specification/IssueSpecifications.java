package com.bugboard.repository.specification;

import com.bugboard.enums.IssueStatus;
import com.bugboard.enums.IssueType;
import com.bugboard.enums.PriorityLevel;

/**
 * Factory methods for building issue query specifications.
 */
public final class IssueSpecifications {

   private IssueSpecifications() {
   }

   public static IssueSpecification allOf(IssueSpecification... specifications) {
      return CompositeIssueSpecification.and(specifications);
   }

   public static IssueSpecification byTerm(String term) {
      return new TermIssueSpecification(term);
   }

   public static IssueSpecification byPriority(PriorityLevel priority) {
      return new PriorityIssueSpecification(priority);
   }

   public static IssueSpecification byStatus(IssueStatus status) {
      return new StatusIssueSpecification(status);
   }

   public static IssueSpecification byType(IssueType type) {
      return new TypeIssueSpecification(type);
   }

   public static IssueSpecification byAssigneeId(Long assigneeId) {
      return new AssigneeIssueSpecification(assigneeId);
   }
}
