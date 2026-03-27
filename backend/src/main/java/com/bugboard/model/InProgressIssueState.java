package com.bugboard.model;

import com.bugboard.enums.IssueStatus;

final class InProgressIssueState extends AbstractIssueState {

   @Override
   public IssueStatus status() {
      return IssueStatus.IN_PROGRESS;
   }
}
