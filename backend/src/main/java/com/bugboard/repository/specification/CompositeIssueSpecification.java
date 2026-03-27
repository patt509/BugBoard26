package com.bugboard.repository.specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Composite specification that applies multiple specifications with AND semantics.
 */
public class CompositeIssueSpecification implements IssueSpecification {

   private final List<IssueSpecification> specifications;

   public CompositeIssueSpecification(List<IssueSpecification> specifications) {
      this.specifications = specifications != null ? specifications : List.of();
   }

   @Override
   public void apply(IssueQueryContext context) {
      for (IssueSpecification specification : specifications) {
         if (specification != null) {
            specification.apply(context);
         }
      }
   }

   public static CompositeIssueSpecification and(IssueSpecification... specifications) {
      List<IssueSpecification> collected = new ArrayList<>();
      if (specifications != null) {
         for (IssueSpecification specification : specifications) {
            if (specification != null) {
               collected.add(specification);
            }
         }
      }
      return new CompositeIssueSpecification(collected);
   }
}
