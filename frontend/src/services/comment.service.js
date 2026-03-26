import httpClient from '../utils/httpClient';
import { API_ENDPOINTS } from '../constants/api';

const resolveStoredUserId = () => {
  const legacyUserId = localStorage.getItem('userId');
  if (legacyUserId) {
    return legacyUserId;
  }

  const storedUser = localStorage.getItem('user');
  if (!storedUser) {
    return null;
  }

  try {
    const parsedUser = JSON.parse(storedUser);
    return parsedUser?.id ?? null;
  } catch (error) {
    console.warn('Unable to parse localStorage user for comment request header', error);
    return null;
  }
};

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
   * @param {Object} commentData - Comment data (text, authorId)
   * @returns {Promise<Object>} Created comment
   */
  create(issueId, commentData) {
    const userId = commentData.authorId ?? resolveStoredUserId();
    return httpClient.post(API_ENDPOINTS.ISSUE_COMMENTS(issueId), commentData, {
      headers: {
        'X-User-Id': userId
      }
    });
  },

  /**
   * Delete comment
   * @param {number} issueId - Issue ID
   * @param {number} commentId - Comment ID
   * @param {number} userId - User ID for authorization
   * @returns {Promise<void>}
   */
  delete(issueId, commentId, userId) {
    return httpClient.delete(API_ENDPOINTS.ISSUE_COMMENT_BY_ID(issueId, commentId), {
      headers: {
        'X-User-Id': userId
      }
    });
  },
};

export default commentService;
