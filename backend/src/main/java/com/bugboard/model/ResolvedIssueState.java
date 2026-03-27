package com.bugboard.model;

import com.bugboard.enums.IssueStatus;

final class ResolvedIssueState extends AbstractIssueState {

   @Override
   public IssueStatus status() {
      return IssueStatus.RESOLVED;
   }
}
