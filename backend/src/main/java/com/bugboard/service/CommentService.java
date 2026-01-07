package com.bugboard.service;

import com.bugboard.model.Comment;
import com.bugboard.repository.CommentRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service layer for Comment-related business logic.
 * Provides encapsulation for Comment operations, hiding the model from the controller layer.
 */
@ApplicationScoped
public class CommentService {

   private static final Logger logger = Logger.getLogger(CommentService.class.getName());

   private final CommentRepository commentRepository;

   // CDI requires no-arg constructor for proxy
   protected CommentService() {
      this.commentRepository = null;
   }

   @Inject
   public CommentService(CommentRepository commentRepository) {
      this.commentRepository = commentRepository;
   }

   // ==================== VALIDATION OPERATIONS ====================

   /**
    * Validates that a comment exists.
    * @param commentId the comment ID to validate
    * @return true if comment exists, false otherwise
    */
   public boolean validateCommentExists(Long commentId) {
      Comment comment = commentRepository.findById(commentId).orElse(null);
      return comment != null;
   }

   // ==================== ATTACHMENT OPERATIONS ====================

   /**
    * Gets the current attachment path for a comment.
    * @param commentId the comment ID
    * @return the attachment path, or null if no attachment
    * @throws IllegalArgumentException if comment not found
    */
   public String getCommentAttachmentPath(Long commentId) {
      Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new IllegalArgumentException("Comment not found"));
      return comment.getAttachmentPath();
   }

   /**
    * Checks if a comment has an attachment.
    * @param commentId the comment ID
    * @return true if comment has an attachment
    * @throws IllegalArgumentException if comment not found
    */
   public boolean commentHasAttachment(Long commentId) {
      return getCommentAttachmentPath(commentId) != null;
   }

   /**
    * Sets the attachment path for a comment.
    * Called after the file has been saved by AttachmentService.
    * @param commentId the comment ID
    * @param attachmentPath the path to set
    * @throws IllegalArgumentException if comment not found
    */
   @Transactional
   public void setCommentAttachmentPath(Long commentId, String attachmentPath) {
      Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new IllegalArgumentException("Comment not found"));
      comment.setAttachmentPath(attachmentPath);
      commentRepository.save(comment);
   }

   /**
    * Removes the attachment from a comment.
    * @param commentId the comment ID
    * @return the old attachment path (for deletion by AttachmentService)
    * @throws IllegalArgumentException if comment not found or has no attachment
    */
   @Transactional
   public String removeCommentAttachment(Long commentId) {
      Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new IllegalArgumentException("Comment not found"));
      
      String oldPath = comment.getAttachmentPath();
      if (oldPath == null) {
         throw new IllegalArgumentException("No attachment found");
      }
      
      comment.removeAttachment();
      commentRepository.save(comment);
      
      logger.log(Level.INFO, "Removed attachment from comment {0}", commentId);
      return oldPath;
   }
}
