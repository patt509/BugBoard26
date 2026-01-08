package com.bugboard.controller;

import com.bugboard.dto.CommentDTO;
import com.bugboard.service.CommentService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REST Controller for Comment management.
 * 
 * Endpoints:
 * - GET /issues/{issueId}/comments - Get all comments for an issue
 * - POST /issues/{issueId}/comments - Create a new comment
 * - DELETE /issues/{issueId}/comments/{commentId} - Delete a comment
 */
@Path("/issues/{issueId}/comments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CommentResource {

   private static final Logger logger = Logger.getLogger(CommentResource.class.getName());

   private final CommentService commentService;

   @Inject
   public CommentResource(CommentService commentService) {
      this.commentService = commentService;
   }

   /**
    * Get all comments for an issue.
    */
   @GET
   public Response getComments(@PathParam("issueId") Long issueId) {
      try {
         List<CommentDTO> comments = commentService.getCommentsByIssueId(issueId);
         return Response.ok(comments).build();
      } catch (Exception e) {
         logger.log(Level.SEVERE, "Error retrieving comments for issue " + issueId, e);
         return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
               .entity(Map.of("error", "Error retrieving comments"))
               .build();
      }
   }

   /**
    * Create a new comment on an issue.
    */
   @POST
   public Response createComment(
         @PathParam("issueId") Long issueId,
         @HeaderParam("X-User-Id") Long userId,
         CommentDTO commentDTO) {
      try {
         if (userId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                  .entity(Map.of("error", "X-User-Id header is required"))
                  .build();
         }

         if (commentDTO.getText() == null || commentDTO.getText().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                  .entity(Map.of("error", "Comment text is required"))
                  .build();
         }

         // Use authorId from DTO if provided, otherwise use userId from header
         Long authorId = commentDTO.getAuthorId() != null ? commentDTO.getAuthorId() : userId;
         
         Long commentId = commentService.createComment(issueId, commentDTO.getText(), authorId);
         
         return Response.status(Response.Status.CREATED)
               .entity(Map.of("id", commentId, "message", "Comment created successfully"))
               .build();
      } catch (IllegalArgumentException e) {
         return Response.status(Response.Status.BAD_REQUEST)
               .entity(Map.of("error", e.getMessage()))
               .build();
      } catch (Exception e) {
         logger.log(Level.SEVERE, "Error creating comment for issue " + issueId, e);
         return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
               .entity(Map.of("error", "Error creating comment"))
               .build();
      }
   }

   /**
    * Delete a comment.
    */
   @DELETE
   @Path("/{commentId}")
   public Response deleteComment(
         @PathParam("issueId") Long issueId,
         @PathParam("commentId") Long commentId,
         @HeaderParam("X-User-Id") Long userId) {
      try {
         if (userId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                  .entity(Map.of("error", "X-User-Id header is required"))
                  .build();
         }

         commentService.deleteComment(commentId);
         
         return Response.ok(Map.of("message", "Comment deleted successfully")).build();
      } catch (IllegalArgumentException e) {
         return Response.status(Response.Status.NOT_FOUND)
               .entity(Map.of("error", e.getMessage()))
               .build();
      } catch (Exception e) {
         logger.log(Level.SEVERE, "Error deleting comment " + commentId, e);
         return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
               .entity(Map.of("error", "Error deleting comment"))
               .build();
      }
   }
}
