package com.bugboard.repository.specification;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mutable context used by issue specifications to build a JPQL query.
 */
public class IssueQueryContext {

   private final List<String> predicates = new ArrayList<>();
   private final Map<String, Object> parameters = new LinkedHashMap<>();

   public void addPredicate(String predicate) {
      if (predicate != null && !predicate.trim().isEmpty()) {
         predicates.add(predicate.trim());
      }
   }

   public void addParameter(String name, Object value) {
      if (name != null && !name.trim().isEmpty()) {
         parameters.put(name.trim(), value);
      }
   }

   public List<String> getPredicates() {
      return predicates;
   }

   public Map<String, Object> getParameters() {
      return parameters;
   }
}
