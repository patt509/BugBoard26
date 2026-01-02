package com.bugboard.controller;

import com.bugboard.dto.IssueDTO;
import com.bugboard.enums.PriorityLevel;
import com.bugboard.service.IssueService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Path("/issues")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class IssueResource {

    private static final Logger logger = LoggerFactory.getLogger(IssueResource.class);

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

        if (duplicateId == null || originalId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Both duplicateId and originalId must be provided.")
                    .build();
        }

        try {
            issueService.processDuplicate(duplicateId, originalId);
            return Response.ok().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid argument for duplicate processing: {}", e.getMessage());
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(e.getMessage())
                    .build();
        } catch (IllegalStateException e) {
            logger.warn("Invalid state for duplicate processing: {}", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .build();
        } catch (Exception e) {
            // Log exception details for debugging
            logger.error("Error processing duplicate request for duplicateId={}, originalId={}",
                    duplicateId, originalId, e);
            e.printStackTrace();

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("An unexpected error occurred: " + e.getMessage())
                    .build();
        }
    }
}