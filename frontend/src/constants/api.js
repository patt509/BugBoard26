/**
 * API Configuration Constants
 */
export const API_BASE_URL = '/api';

/**
 * API Endpoints
 */
export const API_ENDPOINTS = {
  // Auth
  AUTH_LOGIN: '/auth/login',
  AUTH_LOGOUT: '/auth/logout',
  AUTH_USERS_ASSIGNABLE: '/auth/users/assignable',
  AUTH_ADMIN_USERS: '/auth/admin/users',
  AUTH_PROFILE_USERNAME: '/auth/profile/username',
  
  // Issues
  ISSUES: '/issues',
  ISSUES_BY_ID: (id) => `/issues/${id}`,
  ISSUE_HISTORY: (id) => `/issues/${id}/history`,
  ISSUES_SEARCH: '/issues/search',
  ISSUES_ADMIN_DASHBOARD: '/issues/admin/dashboard',
  ISSUE_STATUS: (id) => `/issues/${id}/status`,
  ISSUE_FLAG_DUPLICATE: (duplicateId, originalId) => `/issues/${duplicateId}/duplicate/${originalId}`,
  
  // Comments
  ISSUE_COMMENTS: (issueId) => `/issues/${issueId}/comments`,
  ISSUE_COMMENT_BY_ID: (issueId, commentId) => `/issues/${issueId}/comments/${commentId}`,
};

/**
 * HTTP Methods
 */
export const HTTP_METHODS = {
  GET: 'GET',
  POST: 'POST',
  PUT: 'PUT',
  PATCH: 'PATCH',
  DELETE: 'DELETE',
};
