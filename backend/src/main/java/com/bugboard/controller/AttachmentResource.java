package com.bugboard.controller;

import com.bugboard.service.AttachmentService;
import com.bugboard.service.CommentService;
import com.bugboard.service.IssueService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REST Controller for attachment uploads.
 * 
 * Supports:
 * - Issue attachments: POST /attachments/issues/{issueId}
 * - Comment attachments: POST /attachments/comments/{commentId}
 * 
 * Constraints:
 * - Max file size: 5 MB
 * - Allowed formats: JPG, PNG only
 * - Max 1 attachment per issue/comment
 */
@Path("/attachments")
@Produces(MediaType.APPLICATION_JSON)
public class AttachmentResource {

   private static final Logger logger = Logger.getLogger(AttachmentResource.class.getName());

   private final AttachmentService attachmentService;
   private final IssueService issueService;
   private final CommentService commentService;

   @Inject
   public AttachmentResource(
         AttachmentService attachmentService,
         IssueService issueService,
         CommentService commentService) {
      this.attachmentService = attachmentService;
      this.issueService = issueService;
      this.commentService = commentService;
   }

   // ==================== ISSUE ATTACHMENTS ====================

   /**
    * Upload attachment for an issue.
    * Replaces existing attachment if present.
    */
   @POST
   @Path("/issues/{issueId}")
   @Consumes(MediaType.MULTIPART_FORM_DATA)
   public Response uploadIssueAttachment(
         @HeaderParam("X-User-Id") Long userId,
         @PathParam("issueId") Long issueId,
         @HeaderParam("Content-Type") String contentType,
         @HeaderParam("X-File-Name") String fileName,
         @HeaderParam("X-File-Size") Long fileSize,
         InputStream fileInputStream) {

      try {
         // Verify user is authenticated
         if (userId == null) {
            return ApiResponses.error(Response.Status.UNAUTHORIZED, "Authentication required");
         }

         // Verify issue exists (throws if not found)
         if (!issueService.validateIssueExists(issueId)) {
            return ApiResponses.error(Response.Status.NOT_FOUND, "Issue not found");
         }

         // Derive content type from file name (more reliable than Content-Type header
         // for multipart)
         String actualContentType = deriveContentTypeFromFileName(fileName);
         if (actualContentType == null) {
            // Fallback to extracting from header
            actualContentType = extractContentType(contentType);
         }

         // Validate file before saving
         attachmentService.validateAttachment(actualContentType, fileSize, fileName);

         // Delete old attachment if exists
         String existingPath = issueService.getIssueAttachmentPath(issueId);
         if (existingPath != null) {
            attachmentService.deleteAttachment(existingPath);
         }

         // Extract actual file content from multipart body
         java.io.InputStream cleanFileStream = attachmentService.extractFileFromMultipart(fileInputStream);

         // Save new attachment
         String relativePath = attachmentService.saveAttachment(
               cleanFileStream, fileName, actualContentType, fileSize, "issues", issueId);

         // Update issue with new attachment path
         issueService.setAttachmentPath(issueId, relativePath, userId);

         logger.log(Level.INFO, "User {0} uploaded attachment to issue {1}",
               new Object[] { userId, issueId });

         return Response.ok(Map.of(
               "message", "Attachment uploaded successfully",
               "path", relativePath)).build();

      } catch (IllegalArgumentException e) {
         return ApiResponses.error(Response.Status.BAD_REQUEST, e.getMessage());
      } catch (Exception e) {
         logger.log(Level.SEVERE, "Error uploading issue attachment", e);
         return ApiResponses.error(Response.Status.INTERNAL_SERVER_ERROR, "Error uploading attachment");
      }
   }

   /**
    * Delete attachment from an issue.
    */
   @DELETE
   @Path("/issues/{issueId}")
   public Response deleteIssueAttachment(
         @HeaderParam("X-User-Id") Long userId,
         @PathParam("issueId") Long issueId) {

      try {
         if (userId == null) {
            return ApiResponses.error(Response.Status.UNAUTHORIZED, "Authentication required");
         }

         if (!issueService.validateIssueExists(issueId)) {
            return ApiResponses.error(Response.Status.NOT_FOUND, "Issue not found");
         }

         if (!issueService.issueHasAttachment(issueId)) {
            return ApiResponses.error(Response.Status.NOT_FOUND, "No attachment found");
         }

         // Delete file and clear path
         String oldPath = issueService.removeAttachment(issueId, userId);
         attachmentService.deleteAttachment(oldPath);

         return Response.ok(Map.of("message", "Attachment deleted successfully")).build();

      } catch (Exception e) {
         logger.log(Level.SEVERE, "Error deleting issue attachment", e);
         return ApiResponses.error(Response.Status.INTERNAL_SERVER_ERROR, "Error deleting attachment");
      }
   }

   /**
    * Download/view attachment from an issue.
    */
   @GET
   @Path("/issues/{issueId}")
   @Produces({ "image/jpeg", "image/png" })
   public Response getIssueAttachment(@PathParam("issueId") Long issueId) {
      try {
         if (!issueService.validateIssueExists(issueId)) {
            return Response.status(Response.Status.NOT_FOUND)
                  .entity("Issue not found")
                  .build();
         }

         String attachmentRelativePath = issueService.getIssueAttachmentPath(issueId);
         if (attachmentRelativePath == null) {
            return Response.status(Response.Status.NOT_FOUND)
                  .entity("No attachment found")
                  .build();
         }

         java.nio.file.Path filePath = attachmentService.getAttachmentPath(attachmentRelativePath);
         if (!Files.exists(filePath)) {
            return Response.status(Response.Status.NOT_FOUND)
                  .entity("Attachment file not found")
                  .build();
         }

         String contentType = Files.probeContentType(filePath);
         byte[] fileContent = Files.readAllBytes(filePath);

         return Response.ok(fileContent)
               .header("Content-Type", contentType)
               .header("Content-Disposition", "inline; filename=\"" + filePath.getFileName() + "\"")
               .build();

      } catch (Exception e) {
         logger.log(Level.SEVERE, "Error retrieving issue attachment", e);
         return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
               .entity("Error retrieving attachment")
               .build();
      }
   }

   // ==================== COMMENT ATTACHMENTS ====================

   /**
    * Upload attachment for a comment.
    * Replaces existing attachment if present.
    */
   @POST
   @Path("/comments/{commentId}")
   @Consumes(MediaType.MULTIPART_FORM_DATA)
   public Response uploadCommentAttachment(
         @HeaderParam("X-User-Id") Long userId,
         @PathParam("commentId") Long commentId,
         @HeaderParam("Content-Type") String contentType,
         @HeaderParam("X-File-Name") String fileName,
         @HeaderParam("X-File-Size") Long fileSize,
         InputStream fileInputStream) {

      try {
         if (userId == null) {
            return ApiResponses.error(Response.Status.UNAUTHORIZED, "Authentication required");
         }

         if (!commentService.validateCommentExists(commentId)) {
            return ApiResponses.error(Response.Status.NOT_FOUND, "Comment not found");
         }

         // Derive content type from file name (more reliable than Content-Type header
         // for multipart)
         String actualContentType = deriveContentTypeFromFileName(fileName);
         if (actualContentType == null) {
            // Fallback to extracting from header
            actualContentType = extractContentType(contentType);
         }
         attachmentService.validateAttachment(actualContentType, fileSize, fileName);

         // Delete old attachment if exists
         String existingPath = commentService.getCommentAttachmentPath(commentId);
         if (existingPath != null) {
            attachmentService.deleteAttachment(existingPath);
         }

         // Extract actual file content from multipart body
         java.io.InputStream cleanFileStream = attachmentService.extractFileFromMultipart(fileInputStream);

         // Save new attachment
         String relativePath = attachmentService.saveAttachment(
               cleanFileStream, fileName, actualContentType, fileSize, "comments", commentId);

         // Update comment with new attachment path via service
         commentService.setCommentAttachmentPath(commentId, relativePath);

         logger.log(Level.INFO, "User {0} uploaded attachment to comment {1}",
               new Object[] { userId, commentId });

         return Response.ok(Map.of(
               "message", "Attachment uploaded successfully",
               "path", relativePath)).build();

      } catch (IllegalArgumentException e) {
         return ApiResponses.error(Response.Status.BAD_REQUEST, e.getMessage());
      } catch (Exception e) {
         logger.log(Level.SEVERE, "Error uploading comment attachment", e);
         return ApiResponses.error(Response.Status.INTERNAL_SERVER_ERROR, "Error uploading attachment");
      }
   }

   /**
    * Delete attachment from a comment.
    */
   @DELETE
   @Path("/comments/{commentId}")
   public Response deleteCommentAttachment(
         @HeaderParam("X-User-Id") Long userId,
         @PathParam("commentId") Long commentId) {

      try {
         if (userId == null) {
            return ApiResponses.error(Response.Status.UNAUTHORIZED, "Authentication required");
         }

         if (!commentService.validateCommentExists(commentId)) {
            return ApiResponses.error(Response.Status.NOT_FOUND, "Comment not found");
         }

         if (!commentService.commentHasAttachment(commentId)) {
            return ApiResponses.error(Response.Status.NOT_FOUND, "No attachment found");
         }

         // Delete file and clear path via service
         String oldPath = commentService.removeCommentAttachment(commentId);
         attachmentService.deleteAttachment(oldPath);

         return Response.ok(Map.of("message", "Attachment deleted successfully")).build();

      } catch (Exception e) {
         logger.log(Level.SEVERE, "Error deleting comment attachment", e);
         return ApiResponses.error(Response.Status.INTERNAL_SERVER_ERROR, "Error deleting attachment");
      }
   }

   /**
    * Download/view attachment from a comment.
    */
   @GET
   @Path("/comments/{commentId}")
   @Produces({ "image/jpeg", "image/png" })
   public Response getCommentAttachment(@PathParam("commentId") Long commentId) {
      try {
         if (!commentService.validateCommentExists(commentId)) {
            return Response.status(Response.Status.NOT_FOUND)
                  .entity("Comment not found")
                  .build();
         }

         String attachmentRelativePath = commentService.getCommentAttachmentPath(commentId);
         if (attachmentRelativePath == null) {
            return Response.status(Response.Status.NOT_FOUND)
                  .entity("No attachment found")
                  .build();
         }

         java.nio.file.Path filePath = attachmentService.getAttachmentPath(attachmentRelativePath);
         if (!Files.exists(filePath)) {
            return Response.status(Response.Status.NOT_FOUND)
                  .entity("Attachment file not found")
                  .build();
         }

         String mimeType = Files.probeContentType(filePath);
         byte[] fileContent = Files.readAllBytes(filePath);

         return Response.ok(fileContent)
               .header("Content-Type", mimeType)
               .header("Content-Disposition", "inline; filename=\"" + filePath.getFileName() + "\"")
               .build();

      } catch (Exception e) {
         logger.log(Level.SEVERE, "Error retrieving comment attachment", e);
         return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
               .entity("Error retrieving attachment")
               .build();
      }
   }

   // ==================== INFO ENDPOINT ====================

   /**
    * Get attachment constraints information.
    */
   @GET
   @Path("/info")
   public Response getAttachmentInfo() {
      return Response.ok(Map.of(
            "maxFileSizeBytes", attachmentService.getMaxFileSize(),
            "maxFileSizeMB", attachmentService.getMaxFileSize() / (1024 * 1024),
            "allowedContentTypes", attachmentService.getAllowedContentTypes(),
            "allowedExtensions", attachmentService.getAllowedExtensions())).build();
   }

   // ==================== HELPER METHODS ====================

   private String extractContentType(String contentType) {
      if (contentType == null) {
         return null;
      }
      // Handle multipart content type header
      if (contentType.contains(";")) {
         return contentType.split(";")[0].trim();
      }
      return contentType;
   }

   /**
    * Derives the content type from the file extension.
    * More reliable than parsing multipart headers.
    */
   private String deriveContentTypeFromFileName(String fileName) {
      if (fileName == null) {
         return null;
      }
      String lowerName = fileName.toLowerCase();
      if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
         return "image/jpeg";
      } else if (lowerName.endsWith(".png")) {
         return "image/png";
      }
      return null;
   }
}
