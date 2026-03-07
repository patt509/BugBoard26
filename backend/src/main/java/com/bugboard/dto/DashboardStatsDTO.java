package com.bugboard.dto;

import java.util.Map;

/**
 * DTO for admin dashboard statistics.
 * Contains aggregated data about issues for real-time monitoring.
 */
public class DashboardStatsDTO {
   private long totalIssues;
   private long openIssues; // TODO + IN_PROGRESS
   private long resolvedIssues; // RESOLVED
   private long closedIssues; // CLOSED
   private long duplicateIssues; // Issues marked as duplicate

   private Map<String, Long> issuesByStatus; // Count per status
   private Map<String, Long> issuesByPriority; // Count per priority
   private Map<String, Long> issuesCreatedPerDay; // Last 7 days trend
   private Map<String, Long> issuesAssignedPerUser; // Open issues per assignee (R7)

   private double avgResolutionTimeHours; // Average time to resolve
   private long issuesCreatedToday;
   private long issuesClosedToday;

   public DashboardStatsDTO() {
   }

   private DashboardStatsDTO(Builder builder) {
      this.totalIssues = builder.totalIssues;
      this.openIssues = builder.openIssues;
      this.resolvedIssues = builder.resolvedIssues;
      this.closedIssues = builder.closedIssues;
      this.duplicateIssues = builder.duplicateIssues;
      this.issuesByStatus = builder.issuesByStatus;
      this.issuesByPriority = builder.issuesByPriority;
      this.issuesCreatedPerDay = builder.issuesCreatedPerDay;
      this.issuesAssignedPerUser = builder.issuesAssignedPerUser;
      this.avgResolutionTimeHours = builder.avgResolutionTimeHours;
      this.issuesCreatedToday = builder.issuesCreatedToday;
      this.issuesClosedToday = builder.issuesClosedToday;
   }

   public static Builder builder() {
      return new Builder();
   }

   public static class Builder {
      private long totalIssues;
      private long openIssues;
      private long resolvedIssues;
      private long closedIssues;
      private long duplicateIssues;
      private Map<String, Long> issuesByStatus;
      private Map<String, Long> issuesByPriority;
      private Map<String, Long> issuesCreatedPerDay;
      private Map<String, Long> issuesAssignedPerUser;
      private double avgResolutionTimeHours;
      private long issuesCreatedToday;
      private long issuesClosedToday;

      public Builder totalIssues(long totalIssues) {
         this.totalIssues = totalIssues;
         return this;
      }

      public Builder openIssues(long openIssues) {
         this.openIssues = openIssues;
         return this;
      }

      public Builder resolvedIssues(long resolvedIssues) {
         this.resolvedIssues = resolvedIssues;
         return this;
      }

      public Builder closedIssues(long closedIssues) {
         this.closedIssues = closedIssues;
         return this;
      }

      public Builder duplicateIssues(long duplicateIssues) {
         this.duplicateIssues = duplicateIssues;
         return this;
      }

      public Builder issuesByStatus(Map<String, Long> issuesByStatus) {
         this.issuesByStatus = issuesByStatus;
         return this;
      }

      public Builder issuesByPriority(Map<String, Long> issuesByPriority) {
         this.issuesByPriority = issuesByPriority;
         return this;
      }

      public Builder issuesCreatedPerDay(Map<String, Long> issuesCreatedPerDay) {
         this.issuesCreatedPerDay = issuesCreatedPerDay;
         return this;
      }

      public Builder issuesAssignedPerUser(Map<String, Long> issuesAssignedPerUser) {
         this.issuesAssignedPerUser = issuesAssignedPerUser;
         return this;
      }

      public Builder avgResolutionTimeHours(double avgResolutionTimeHours) {
         this.avgResolutionTimeHours = avgResolutionTimeHours;
         return this;
      }

      public Builder issuesCreatedToday(long issuesCreatedToday) {
         this.issuesCreatedToday = issuesCreatedToday;
         return this;
      }

      public Builder issuesClosedToday(long issuesClosedToday) {
         this.issuesClosedToday = issuesClosedToday;
         return this;
      }

      public DashboardStatsDTO build() {
         return new DashboardStatsDTO(this);
      }
   }

   // Getters
   public long getTotalIssues() {
      return totalIssues;
   }

   public long getOpenIssues() {
      return openIssues;
   }

   public long getResolvedIssues() {
      return resolvedIssues;
   }

   public long getClosedIssues() {
      return closedIssues;
   }

   public long getDuplicateIssues() {
      return duplicateIssues;
   }

   public Map<String, Long> getIssuesByStatus() {
      return issuesByStatus;
   }

   public Map<String, Long> getIssuesByPriority() {
      return issuesByPriority;
   }

   public Map<String, Long> getIssuesCreatedPerDay() {
      return issuesCreatedPerDay;
   }

   public Map<String, Long> getIssuesAssignedPerUser() {
      return issuesAssignedPerUser;
   }

   public double getAvgResolutionTimeHours() {
      return avgResolutionTimeHours;
   }

   public long getIssuesCreatedToday() {
      return issuesCreatedToday;
   }

   public long getIssuesClosedToday() {
      return issuesClosedToday;
   }
}
