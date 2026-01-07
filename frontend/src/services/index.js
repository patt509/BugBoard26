/**
 * Services barrel export
 * Central point for importing all services
 */
export { authService } from './auth.service';
export { issueService } from './issue.service';
export { commentService } from './comment.service';

export default {
  auth: () => import('./auth.service').then(m => m.authService),
  issues: () => import('./issue.service').then(m => m.issueService),
  comments: () => import('./comment.service').then(m => m.commentService),
};
