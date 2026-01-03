package com.bugboard.controller;

import com.bugboard.dto.IssueDTO;
import com.bugboard.enums.PriorityLevel;
import com.bugboard.service.IssueService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/issues")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class IssueResource {

   private static final Logger logger = Logger.getLogger(IssueResource.class.getName());

   private final IssueService issueService;

   @Inject
   public IssueResource(IssueService issueService) {
      this.issueService = issueService;
   }

   // Real-time search with dynamic filters
   @GET
   @Path("/search")
   public Response search(
         @QueryParam("term") String term,
         @QueryParam("priority") PriorityLevel priority) {

      List<IssueDTO> results = issueService.searchIssues(term, priority);
      return Response.ok(results).build();
   }

   // Create a new issue from DTO
   @POST
   public Response create(IssueDTO issueDTO) {
      try {
         Long id = issueService.createIssue(issueDTO, null);
         return Response.status(Response.Status.CREATED).entity(id).build();
      } catch (IllegalArgumentException e) {
         // If the input data is invalid (title is too short, etc.)
         return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
      }
   }

   // Update the status of an issue
   @PATCH
   @Path("/{id}/status")
   public Response updateStatus(@PathParam("id") Long id, @QueryParam("newStatus") IssueStatus newStatus) {
      try {
         issueService.updateStatus(id, newStatus);
         return Response.ok.build();
      } catch (Exception e) {
         return Response.status(Response.Status.NOT_FOUND).build();
      }
   }

   @POST
   @Path("/{id}/duplicate/{originalId}")
   public Response flagAsDuplicate(
         @PathParam("id") Long duplicateId,
         @PathParam("originalId") Long originalId) {

      try {
         issueService.processDuplicate(duplicateId, originalId);
         return Response.ok().build();
      } catch (IllegalArgumentException e) {
         logger.log(Level.WARNING, "Invalid argument for duplicate processing: {0}", e.getMessage());
         return Response.status(Response.Status.NOT_FOUND)
                  .entity(e.getMessage())
                  .build();
      } catch (IllegalStateException e) {
         logger.log(Level.WARNING, "Invalid state for duplicate processing: {0}", e.getMessage());
         return Response.status(Response.Status.BAD_REQUEST)
                  .entity(e.getMessage())
                  .build();
      } catch (Exception e) {
         // Log exception details for debugging
         logger.log(Level.SEVERE, String.format("Error processing duplicate request for duplicateId=%d, originalId=%d",
                  duplicateId, originalId), e);

         return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                  .entity("An unexpected error occurred: " + e.getMessage())
                  .build();
      }
   }
}