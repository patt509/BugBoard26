import httpClient from '../utils/httpClient';
import { API_ENDPOINTS } from '../constants/api';

/**
 * Comment Service
 * Handles all comment-related API calls
 */
export const commentService = {
  /**
   * Get all comments for an issue
   * @param {number} issueId - Issue ID
   * @returns {Promise<Array>} List of comments
   */
  getByIssueId(issueId) {
    return httpClient.get(API_ENDPOINTS.ISSUE_COMMENTS(issueId));
  },

  /**
   * Create new comment
   * @param {number} issueId - Issue ID
   * @param {Object} commentData - Comment data
   * @returns {Promise<Object>} Created comment
   */
  create(issueId, commentData) {
    return httpClient.post(API_ENDPOINTS.ISSUE_COMMENTS(issueId), commentData);
  },

  /**
   * Delete comment
   * @param {number} issueId - Issue ID
   * @param {number} commentId - Comment ID
   * @returns {Promise<void>}
   */
  delete(issueId, commentId) {
    return httpClient.delete(API_ENDPOINTS.ISSUE_COMMENT_BY_ID(issueId, commentId));
  },
};

export default commentService;
