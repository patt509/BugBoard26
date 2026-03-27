package com.bugboard.model;

import java.time.LocalDateTime;

import com.bugboard.enums.IssueStatus;

/**
 * Base implementation for issue states.
 * Handles closedAt lifecycle consistently across transitions.
 */
abstract class AbstractIssueState implements IssueState {

   @Override
   public void transition(Issue issue, IssueStatus nextStatus) {
      boolean wasClosedLike = isClosedLike(status());
      boolean willBeClosedLike = isClosedLike(nextStatus);

      if (willBeClosedLike && !wasClosedLike) {
         issue.setClosedAtInternal(LocalDateTime.now());
      } else if (!willBeClosedLike) {
         issue.setClosedAtInternal(null);
      }

      issue.setStatusInternal(nextStatus);
   }

   protected boolean isClosedLike(IssueStatus status) {
      return status == IssueStatus.CLOSED || status == IssueStatus.RESOLVED;
   }
}
