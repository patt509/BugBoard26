package com.bugboard.model;

import com.bugboard.enums.IssueStatus;

/**
 * State contract for Issue lifecycle transitions.
 */
interface IssueState {

   IssueStatus status();

   void transition(Issue issue, IssueStatus nextStatus);
}
