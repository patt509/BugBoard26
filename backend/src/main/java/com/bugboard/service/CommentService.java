package com.bugboard.service;

import com.bugboard.dto.CommentDTO;
import com.bugboard.model.Comment;
import com.bugboard.model.Issue;
import com.bugboard.model.User;
import com.bugboard.repository.CommentRepository;
import com.bugboard.repository.IssueRepository;
import com.bugboard.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
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
   private final IssueRepository issueRepository;
   private final UserRepository userRepository;

   // CDI requires no-arg constructor for proxy
   protected CommentService() {
      this.commentRepository = null;
      this.issueRepository = null;
      this.userRepository = null;
   }

   @Inject
   public CommentService(CommentRepository commentRepository, IssueRepository issueRepository, UserRepository userRepository) {
      this.commentRepository = commentRepository;
      this.issueRepository = issueRepository;
      this.userRepository = userRepository;
   }

   // ==================== COMMENT CRUD OPERATIONS ====================

   /**
    * Get all comments for an issue.
    * @param issueId the issue ID
    * @return list of comment DTOs
    */
   public List<CommentDTO> getCommentsByIssueId(Long issueId) {
      List<Comment> comments = commentRepository.findByIssueId(issueId);
      return comments.stream()
            .map(this::convertToDTO)
            .toList();
   }

   /**
    * Create a new comment on an issue.
    * @param issueId the issue ID
    * @param text the comment text
    * @param authorId the author user ID
    * @return the created comment ID
    */
   @Transactional
   public Long createComment(Long issueId, String text, Long authorId) {
      Issue issue = issueRepository.findById(issueId);
      if (issue == null) {
         throw new IllegalArgumentException("Issue not found");
      }

      User author = userRepository.findById(authorId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

      Comment comment = new Comment(text, issue, author);
      commentRepository.save(comment);
      
      logger.log(Level.INFO, "Created comment {0} on issue {1} by user {2}", 
            new Object[]{comment.getId(), issueId, authorId});
      
      return comment.getId();
   }

   /**
    * Delete a comment.
    * @param commentId the comment ID
    */
   @Transactional
   public void deleteComment(Long commentId) {
      Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new IllegalArgumentException("Comment not found"));
      commentRepository.delete(comment);
      logger.log(Level.INFO, "Deleted comment {0}", commentId);
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

   // ==================== HELPER METHODS ====================

   private CommentDTO convertToDTO(Comment comment) {
      return CommentDTO.builder()
            .id(comment.getId())
            .text(comment.getText())
            .issueId(comment.getIssue().getId())
            .authorId(comment.getAuthor().getId())
            .authorUsername(comment.getAuthor().getUsername())
            .createdAt(comment.getCreatedAt())
            .updatedAt(comment.getUpdatedAt())
            .attachmentPath(comment.getAttachmentPath())
            .build();
   }
}
