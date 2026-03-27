package com.bugboard.repository.specification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.bugboard.enums.IssueStatus;
import com.bugboard.enums.IssueType;
import com.bugboard.enums.PriorityLevel;

public class IssueSpecificationsTest {

   @Test
   public void testAllOf_AppliesAllProvidedFilters() {
      IssueQueryContext context = new IssueQueryContext();

      IssueSpecification spec = IssueSpecifications.allOf(
            IssueSpecifications.byTerm("  bug  "),
            IssueSpecifications.byPriority(PriorityLevel.HIGH),
            IssueSpecifications.byStatus(IssueStatus.TODO),
            IssueSpecifications.byType(IssueType.BUG),
            IssueSpecifications.byAssigneeId(42L));

      spec.apply(context);

      assertEquals("PostCond failed: all 5 predicates should be present", 5, context.getPredicates().size());
      assertEquals("PostCond failed: term parameter should be normalized and wrapped", "%bug%",
            context.getParameters().get("term"));
      assertEquals("PostCond failed: priority parameter should match", PriorityLevel.HIGH,
            context.getParameters().get("priority"));
      assertEquals("PostCond failed: status parameter should match", IssueStatus.TODO,
            context.getParameters().get("status"));
      assertEquals("PostCond failed: type parameter should match", IssueType.BUG,
            context.getParameters().get("type"));
      assertEquals("PostCond failed: assigneeId parameter should match", 42L,
            context.getParameters().get("assigneeId"));
   }

   @Test
   public void testByTerm_BlankInputProducesNoClause() {
      IssueQueryContext context = new IssueQueryContext();

      IssueSpecifications.byTerm("   ").apply(context);

      assertTrue("PostCond failed: blank term should not add predicates", context.getPredicates().isEmpty());
      assertTrue("PostCond failed: blank term should not add parameters", context.getParameters().isEmpty());
   }

   @Test
   public void testAllOf_IgnoresNullSpecifications() {
      IssueQueryContext context = new IssueQueryContext();

      IssueSpecifications.allOf(
            null,
            IssueSpecifications.byStatus(IssueStatus.CLOSED),
            null).apply(context);

      assertEquals("PostCond failed: only one predicate should be added", 1, context.getPredicates().size());
      assertEquals("PostCond failed: status parameter should be set", IssueStatus.CLOSED,
            context.getParameters().get("status"));
   }

   @Test
   public void testByAssigneeId_NullInputProducesNoClause() {
      IssueQueryContext context = new IssueQueryContext();

      IssueSpecifications.byAssigneeId(null).apply(context);

      assertTrue("PostCond failed: null assignee should not add predicates", context.getPredicates().isEmpty());
      assertTrue("PostCond failed: null assignee should not add parameters", context.getParameters().isEmpty());
   }
}
