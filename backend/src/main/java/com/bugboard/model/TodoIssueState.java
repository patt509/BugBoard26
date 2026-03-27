package com.bugboard.model;

import com.bugboard.enums.IssueStatus;

final class TodoIssueState extends AbstractIssueState {

   @Override
   public IssueStatus status() {
      return IssueStatus.TODO;
   }
}
