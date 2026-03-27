package com.bugboard.repository.specification;

/**
 * Specification for free-text search over issue title, description and reporter.
 */
final class TermIssueSpecification implements IssueSpecification {

   private final String term;

   TermIssueSpecification(String term) {
      this.term = term;
   }

   @Override
   public void apply(IssueQueryContext context) {
      if (term == null) {
         return;
      }

      String normalizedTerm = term.trim();
      if (normalizedTerm.isEmpty()) {
         return;
      }

      context.addPredicate(
            "(LOWER(i.title) LIKE LOWER(:term) OR LOWER(i.description) LIKE LOWER(:term) OR LOWER(reporter.username) LIKE LOWER(:term))");
      context.addParameter("term", "%" + normalizedTerm + "%");
   }
}
