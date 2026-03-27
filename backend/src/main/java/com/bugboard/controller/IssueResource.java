package com.bugboard.controller;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.bugboard.dto.DashboardStatsDTO;
import com.bugboard.dto.IssueDTO;
import com.bugboard.dto.IssueHistoryDTO;
import com.bugboard.enums.IssueStatus;
import com.bugboard.enums.IssueType;
import com.bugboard.enums.PriorityLevel;
import com.bugboard.service.AuthService;
import com.bugboard.service.IssueService;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST Controller for Issue management.
 * 
 * Public endpoints (all authenticated users):
 * - GET /issues - View all issues on the board
 * - GET /issues/search - Search with filters
 * - GET /issues/{id} - View single issue details
 * - POST /issues - Create new issue
 * - PATCH /issues/{id}/status - Update issue status
 * 
 * Admin-only endpoints:
 * - GET /issues/admin/dashboard - Real-time statistics dashboard
 * - POST /issues/{id}/duplicate/{originalId} - Mark as duplicate
 */
@Path("/issues")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class IssueResource {

   private static final Logger logger = Logger.getLogger(IssueResource.class.getName());

   private final IssueService issueService;
   private final AuthService authService;

   @Inject
   public IssueResource(IssueService issueService, AuthService authService) {
      this.issueService = issueService;
      this.authService = authService;
   }

   // ==================== PUBLIC ENDPOINTS (ALL USERS) ====================

   /**
    * Get all issues for the board view.
    * Available to all authenticated users.
    */
   @GET
   public Response getAllIssues() {
      try {
         List<IssueDTO> issues = issueService.getAllIssues();
         return Response.ok(issues).build();
      } catch (Exception e) {
         logger.log(Level.SEVERE, "Error retrieving all issues", e);
         return ApiResponses.error(Response.Status.INTERNAL_SERVER_ERROR, "Error retrieving issues");
      }
   }

   /**
    * Search issues with optional filters.
    * Available to all authenticated users.
    */
   @GET
   @Path("/search")
   public Response search(
         @QueryParam("term") String term,
         @QueryParam("priority") String rawPriority,
         @QueryParam("status") String rawStatus,
         @QueryParam("type") String rawType,
         @QueryParam("assigneeId") Long assigneeId) {
      try {
         PriorityLevel priority = parsePriority(rawPriority);
         IssueStatus status = parseStatus(rawStatus);
         IssueType type = parseType(rawType);
         if (assigneeId != null && assigneeId <= 0) {
            return ApiResponses.error(Response.Status.BAD_REQUEST, "assigneeId must be a positive number");
         }
         List<IssueDTO> results = issueService.searchIssues(term, priority, status, type, assigneeId);
         return Response.ok(results).build();
      } catch (IllegalArgumentException e) {
         return ApiResponses.error(Response.Status.BAD_REQUEST, e.getMessage());
      } catch (Exception e) {
         logger.log(Level.SEVERE, "Error searching issues", e);
         return ApiResponses.error(Response.Status.INTERNAL_SERVER_ERROR, "Error searching issues");
      }
   }

   /**
    * Get a single issue by ID.
    */
   @GET
   @Path("/{id}")
   public Response getIssueById(@PathParam("id") Long id) {
      try {
         IssueDTO issue = issueService.getIssueById(id);
         return Response.ok(issue).build();
      } catch (IllegalArgumentException e) {
         return ApiResponses.error(Response.Status.NOT_FOUND, e.getMessage());
      } catch (Exception e) {
         logger.log(Level.SEVERE, "Error retrieving issue", e);
         return ApiResponses.error(Response.Status.INTERNAL_SERVER_ERROR, "Error retrieving issue");
      }
   }

   /**
    * Get issue history timeline by issue ID.
    */
   @GET
   @Path("/{id}/history")
   public Response getIssueHistory(@PathParam("id") Long id) {
      try {
         List<IssueHistoryDTO> historyEntries = issueService.getIssueHistory(id);
         return Response.ok(historyEntries).build();
      } catch (IllegalArgumentException e) {
         return ApiResponses.error(Response.Status.NOT_FOUND, e.getMessage());
      } catch (Exception e) {
         logger.log(Level.SEVERE, "Error retrieving issue history", e);
         return ApiResponses.error(Response.Status.INTERNAL_SERVER_ERROR, "Error retrieving issue history");
      }
   }

   /**
    * Create a new issue.
    */
   @POST
   public Response create(
         @HeaderParam("X-User-Id") Long userId,
         IssueDTO issueDTO) {
      try {
         if (issueDTO == null) {
            return ApiResponses.error(Response.Status.BAD_REQUEST, "Request body is required");
         }

         // Validate userId is provided
         if (userId == null) {
            return ApiResponses.error(Response.Status.BAD_REQUEST, "X-User-Id header is required");
         }
         
         Long id = issueService.createIssue(issueDTO, userId);
         
         if (id == null) {
            return ApiResponses.error(Response.Status.INTERNAL_SERVER_ERROR, "Failed to create issue - no ID returned");
         }
         
         return Response.status(Response.Status.CREATED)
               .entity(Map.of("id", id, "message", "Issue created successfully"))
               .build();
      } catch (IllegalArgumentException e) {
         return ApiResponses.error(Response.Status.BAD_REQUEST, e.getMessage());
      } catch (Exception e) {
         logger.log(Level.SEVERE, "Error creating issue", e);
         return ApiResponses.error(Response.Status.INTERNAL_SERVER_ERROR, "Error creating issue: " + e.getMessage());
      }
   }

   /**
    * Update an existing issue.
    */
   @PUT
   @Path("/{id}")
   public Response updateIssue(
         @PathParam("id") Long id,
         IssueDTO issueDTO) {
      try {
         if (issueDTO == null) {
            return ApiResponses.error(Response.Status.BAD_REQUEST, "Request body is required");
         }

         issueService.updateIssue(id, issueDTO);
         return Response.ok(Map.of("message", "Issue updated successfully")).build();
      } catch (IllegalArgumentException e) {
         return ApiResponses.error(Response.Status.NOT_FOUND, e.getMessage());
      } catch (Exception e) {
         logger.log(Level.SEVERE, "Error updating issue", e);
         return ApiResponses.error(Response.Status.INTERNAL_SERVER_ERROR, "Error updating issue: " + e.getMessage());
      }
   }

   /**
    * Update the status of an issue.
    */
   @PATCH
   @Path("/{id}/status")
   public Response updateStatus(
         @PathParam("id") Long id,
         @QueryParam("newStatus") String rawNewStatus) {
      try {
         IssueStatus newStatus = parseStatus(rawNewStatus);
         if (newStatus == null) {
            return ApiResponses.error(Response.Status.BAD_REQUEST, "newStatus parameter is required");
         }

         issueService.updateStatus(id, newStatus);
         return Response.ok(Map.of("message", "Status updated successfully")).build();
      } catch (IllegalArgumentException e) {
         return ApiResponses.error(Response.Status.NOT_FOUND, e.getMessage());
      } catch (Exception e) {
         logger.log(Level.SEVERE, "Error updating issue status", e);
         return ApiResponses.error(Response.Status.INTERNAL_SERVER_ERROR, "Error updating status");
      }
   }

   // ==================== ADMIN-ONLY ENDPOINTS ====================

   /**
    * Get real-time dashboard statistics.
    * Only accessible by administrators.
    */
   @GET
   @Path("/admin/dashboard")
   public Response getDashboardStats(@HeaderParam("X-User-Id") Long adminId) {
      try {
         // Validate admin privileges via AuthService
         authService.validateAdminPrivileges(adminId);

         DashboardStatsDTO stats = issueService.getDashboardStats();
         return Response.ok(stats).build();
      } catch (SecurityException e) {
         return ApiResponses.error(Response.Status.FORBIDDEN, e.getMessage());
      } catch (Exception e) {
         logger.log(Level.SEVERE, "Error retrieving dashboard stats", e);
         return ApiResponses.error(Response.Status.INTERNAL_SERVER_ERROR, "Error retrieving statistics");
      }
   }

   /**
    * Mark an issue as duplicate of another.
    * Only accessible by administrators.
    */
   @POST
   @Path("/{id}/duplicate/{originalId}")
   public Response flagAsDuplicate(
         @HeaderParam("X-User-Id") Long adminId,
         @PathParam("id") Long duplicateId,
         @PathParam("originalId") Long originalId) {
      try {
         // Admin validation is done inside the service method
         issueService.processDuplicate(duplicateId, originalId, adminId);
         return Response.ok(Map.of(
               "message", "Issue marked as duplicate successfully",
               "duplicateId", duplicateId,
               "originalId", originalId)).build();
      } catch (SecurityException e) {
         return ApiResponses.error(Response.Status.FORBIDDEN, e.getMessage());
      } catch (IllegalArgumentException e) {
         logger.log(Level.WARNING, "Invalid argument for duplicate processing: {0}", e.getMessage());
         return ApiResponses.error(Response.Status.NOT_FOUND, e.getMessage());
      } catch (IllegalStateException e) {
         logger.log(Level.WARNING, "Invalid state for duplicate processing: {0}", e.getMessage());
         return ApiResponses.error(Response.Status.BAD_REQUEST, e.getMessage());
      } catch (Exception e) {
         logger.log(Level.SEVERE, String.format(
               "Error processing duplicate request for duplicateId=%d, originalId=%d",
               duplicateId, originalId), e);
         return ApiResponses.error(Response.Status.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
      }
   }

   private PriorityLevel parsePriority(String rawPriority) {
      if (rawPriority == null || rawPriority.trim().isEmpty()) {
         return null;
      }
      try {
         return PriorityLevel.valueOf(rawPriority.trim().toUpperCase());
      } catch (IllegalArgumentException ex) {
         throw new IllegalArgumentException("Invalid priority: " + rawPriority);
      }
   }

   private IssueStatus parseStatus(String rawStatus) {
      if (rawStatus == null || rawStatus.trim().isEmpty()) {
         return null;
      }
      try {
         return IssueStatus.valueOf(rawStatus.trim().toUpperCase());
      } catch (IllegalArgumentException ex) {
         throw new IllegalArgumentException("Invalid status: " + rawStatus);
      }
   }

   private IssueType parseType(String rawType) {
      if (rawType == null || rawType.trim().isEmpty()) {
         return null;
      }
      try {
         return IssueType.valueOf(rawType.trim().toUpperCase());
      } catch (IllegalArgumentException ex) {
         throw new IllegalArgumentException("Invalid type: " + rawType);
      }
   }
}
