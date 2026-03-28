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
   private static final String MESSAGE_KEY = "message";
   private static final String USER_HEADER_REQUIRED_MESSAGE = "X-User-Id header is required";
   private static final String ERROR_CREATING_COMMENT_MESSAGE = "Error creating comment";

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
         logger.log(Level.SEVERE, e, () -> "Error retrieving comments for issue " + issueId);
         return ApiResponses.error(Response.Status.INTERNAL_SERVER_ERROR, "Error retrieving comments");
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
            return ApiResponses.error(Response.Status.BAD_REQUEST, USER_HEADER_REQUIRED_MESSAGE);
         }

         if (commentDTO.getText() == null || commentDTO.getText().trim().isEmpty()) {
            return ApiResponses.error(Response.Status.BAD_REQUEST, "Comment text is required");
         }

         Long authorId = userId;
         
         Long commentId = commentService.createComment(issueId, commentDTO.getText(), authorId);
         if (commentId == null) {
            return ApiResponses.error(Response.Status.INTERNAL_SERVER_ERROR, ERROR_CREATING_COMMENT_MESSAGE);
         }
         
         return Response.status(Response.Status.CREATED)
               .entity(Map.of("id", commentId, MESSAGE_KEY, "Comment created successfully"))
               .build();
      } catch (IllegalArgumentException e) {
         return ApiResponses.error(Response.Status.BAD_REQUEST, e.getMessage());
      } catch (IllegalStateException e) {
         logger.log(Level.SEVERE, e, () -> "Comment persistence failed for issue " + issueId);
         return ApiResponses.error(Response.Status.INTERNAL_SERVER_ERROR, ERROR_CREATING_COMMENT_MESSAGE);
      } catch (Exception e) {
         logger.log(Level.SEVERE, e, () -> "Error creating comment for issue " + issueId);
         return ApiResponses.error(Response.Status.INTERNAL_SERVER_ERROR, ERROR_CREATING_COMMENT_MESSAGE);
      }
   }

   /**
    * Update an existing comment.
    */
   @PUT
   @Path("/{commentId}")
   public Response updateComment(
         @PathParam("issueId") Long issueId,
         @PathParam("commentId") Long commentId,
         @HeaderParam("X-User-Id") Long userId,
         CommentDTO commentDTO) {
      try {
         if (userId == null) {
            return ApiResponses.error(Response.Status.BAD_REQUEST, USER_HEADER_REQUIRED_MESSAGE);
         }

         if (commentDTO.getText() == null || commentDTO.getText().trim().isEmpty()) {
            return ApiResponses.error(Response.Status.BAD_REQUEST, "Comment text is required");
         }

         commentService.updateComment(commentId, commentDTO.getText(), userId);
         
         return Response.ok(Map.of(MESSAGE_KEY, "Comment updated successfully")).build();
      } catch (SecurityException e) {
         return ApiResponses.error(Response.Status.FORBIDDEN, e.getMessage());
      } catch (IllegalArgumentException e) {
         return ApiResponses.error(Response.Status.NOT_FOUND, e.getMessage());
      } catch (Exception e) {
         logger.log(Level.SEVERE, e, () -> "Error updating comment " + commentId);
         return ApiResponses.error(Response.Status.INTERNAL_SERVER_ERROR, "Error updating comment");
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
            return ApiResponses.error(Response.Status.BAD_REQUEST, USER_HEADER_REQUIRED_MESSAGE);
         }

         commentService.deleteComment(commentId, userId);
         
         return Response.ok(Map.of(MESSAGE_KEY, "Comment deleted successfully")).build();
      } catch (SecurityException e) {
         return ApiResponses.error(Response.Status.FORBIDDEN, e.getMessage());
      } catch (IllegalArgumentException e) {
         return ApiResponses.error(Response.Status.NOT_FOUND, e.getMessage());
      } catch (Exception e) {
         logger.log(Level.SEVERE, e, () -> "Error deleting comment " + commentId);
         return ApiResponses.error(Response.Status.INTERNAL_SERVER_ERROR, "Error deleting comment");
      }
   }
}
