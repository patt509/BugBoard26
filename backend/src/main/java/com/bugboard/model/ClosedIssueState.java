package com.bugboard.model;

import com.bugboard.enums.IssueStatus;

final class ClosedIssueState extends AbstractIssueState {

   @Override
   public IssueStatus status() {
      return IssueStatus.CLOSED;
   }
}
