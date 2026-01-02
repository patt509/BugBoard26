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

    @Inject
    private IssueService issueService;

    // Real-time search with dynamic filters
    @GET
    @Path("/search")
    public Response search(
            @QueryParam("term") String term,
            @QueryParam("priority") PriorityLevel priority) {

        List<IssueDTO> results = issueService.searchIssues(term, priority);
        return Response.ok(results).build();
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