package com.bugboard.repository.specification;

/**
 * Specification contract for Issue repository queries.
 * Each specification can contribute one or more JPQL predicates and parameters.
 */
public interface IssueSpecification {

   void apply(IssueQueryContext context);
}
