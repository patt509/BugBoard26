package com.bugboard.service;

import com.bugboard.model.Comment;
import com.bugboard.model.User;
import com.bugboard.repository.CommentRepository;
import com.bugboard.repository.IssueRepository;
import com.bugboard.repository.UserRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CommentServiceTest {

   @Mock
   private CommentRepository commentRepository;

   @Mock
   private IssueRepository issueRepository;

   @Mock
   private UserRepository userRepository;

   @InjectMocks
   private CommentService commentService;

   @Test
   public void updateComment_allowsAuthor() {
      Comment comment = org.mockito.Mockito.mock(Comment.class);
      User author = org.mockito.Mockito.mock(User.class);
      User actor = org.mockito.Mockito.mock(User.class);

      when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
      when(comment.getAuthor()).thenReturn(author);
      when(author.getId()).thenReturn(10L);
      when(userRepository.findById(10L)).thenReturn(Optional.of(actor));
      when(actor.getId()).thenReturn(10L);
      when(actor.isAdmin()).thenReturn(false);

      commentService.updateComment(1L, "Updated text", 10L);

      verify(comment).setText("Updated text");
      verify(commentRepository).save(comment);
   }

   @Test
   public void updateComment_allowsAdmin() {
      Comment comment = org.mockito.Mockito.mock(Comment.class);
      User author = org.mockito.Mockito.mock(User.class);
      User admin = org.mockito.Mockito.mock(User.class);

      when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
      when(comment.getAuthor()).thenReturn(author);
      when(author.getId()).thenReturn(10L);
      when(userRepository.findById(99L)).thenReturn(Optional.of(admin));
      when(admin.getId()).thenReturn(99L);
      when(admin.isAdmin()).thenReturn(true);

      commentService.updateComment(1L, "Updated by admin", 99L);

      verify(comment).setText("Updated by admin");
      verify(commentRepository).save(comment);
   }

   @Test(expected = SecurityException.class)
   public void updateComment_blocksUnauthorizedUser() {
      Comment comment = org.mockito.Mockito.mock(Comment.class);
      User author = org.mockito.Mockito.mock(User.class);
      User actor = org.mockito.Mockito.mock(User.class);

      when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
      when(comment.getAuthor()).thenReturn(author);
      when(author.getId()).thenReturn(10L);
      when(userRepository.findById(11L)).thenReturn(Optional.of(actor));
      when(actor.getId()).thenReturn(11L);
      when(actor.isAdmin()).thenReturn(false);

      commentService.updateComment(1L, "Unauthorized update", 11L);
   }

   @Test
   public void deleteComment_allowsAuthor() {
      Comment comment = org.mockito.Mockito.mock(Comment.class);
      User author = org.mockito.Mockito.mock(User.class);
      User actor = org.mockito.Mockito.mock(User.class);

      when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
      when(comment.getAuthor()).thenReturn(author);
      when(author.getId()).thenReturn(10L);
      when(userRepository.findById(10L)).thenReturn(Optional.of(actor));
      when(actor.getId()).thenReturn(10L);
      when(actor.isAdmin()).thenReturn(false);

      commentService.deleteComment(1L, 10L);

      verify(commentRepository).delete(comment);
   }

   @Test
   public void deleteComment_allowsAdmin() {
      Comment comment = org.mockito.Mockito.mock(Comment.class);
      User author = org.mockito.Mockito.mock(User.class);
      User admin = org.mockito.Mockito.mock(User.class);

      when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
      when(comment.getAuthor()).thenReturn(author);
      when(author.getId()).thenReturn(10L);
      when(userRepository.findById(99L)).thenReturn(Optional.of(admin));
      when(admin.getId()).thenReturn(99L);
      when(admin.isAdmin()).thenReturn(true);

      commentService.deleteComment(1L, 99L);

      verify(commentRepository).delete(comment);
   }

   @Test(expected = SecurityException.class)
   public void deleteComment_blocksUnauthorizedUser() {
      Comment comment = org.mockito.Mockito.mock(Comment.class);
      User author = org.mockito.Mockito.mock(User.class);
      User actor = org.mockito.Mockito.mock(User.class);

      when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
      when(comment.getAuthor()).thenReturn(author);
      when(author.getId()).thenReturn(10L);
      when(userRepository.findById(11L)).thenReturn(Optional.of(actor));
      when(actor.getId()).thenReturn(11L);
      when(actor.isAdmin()).thenReturn(false);

      commentService.deleteComment(1L, 11L);

      verify(commentRepository, never()).delete(comment);
   }
}
