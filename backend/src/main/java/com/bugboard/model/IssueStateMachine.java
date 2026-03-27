package com.bugboard.model;

import java.util.Map;

import com.bugboard.enums.IssueStatus;

/**
 * Context for Issue State pattern.
 */
final class IssueStateMachine {

   private final Map<IssueStatus, IssueState> states;
   private final IssueState fallbackState;

   private IssueStateMachine(Map<IssueStatus, IssueState> states, IssueState fallbackState) {
      this.states = states;
      this.fallbackState = fallbackState;
   }

   static IssueStateMachine defaultMachine() {
      IssueState todo = new TodoIssueState();
      IssueState inProgress = new InProgressIssueState();
      IssueState resolved = new ResolvedIssueState();
      IssueState closed = new ClosedIssueState();

      return new IssueStateMachine(
            Map.of(
                  IssueStatus.TODO, todo,
                  IssueStatus.IN_PROGRESS, inProgress,
                  IssueStatus.RESOLVED, resolved,
                  IssueStatus.CLOSED, closed),
            todo);
   }

   void transition(Issue issue, IssueStatus nextStatus) {
      IssueStatus currentStatus = issue.getStatus();
      IssueState currentState = states.getOrDefault(currentStatus, fallbackState);
      currentState.transition(issue, nextStatus);
   }
}
