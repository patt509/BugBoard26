import httpClient from '../utils/httpClient';
import { API_ENDPOINTS } from '../constants/api';

/**
 * Issue Service
 * Handles all issue-related API calls
 */
export const issueService = {
  /**
   * Get all issues
   * @returns {Promise<Array>} List of issues
   */
  getAll() {
    return httpClient.get(API_ENDPOINTS.ISSUES);
  },

  /**
   * Get issue by ID
   * @param {number} id - Issue ID
   * @returns {Promise<Object>} Issue data
   */
  getById(id) {
    return httpClient.get(API_ENDPOINTS.ISSUES_BY_ID(id));
  },

  /**
   * Create new issue
   * @param {Object} issueData - Issue data
   * @param {number} userId - User ID for the X-User-Id header
   * @returns {Promise<Object>} Created issue response with id
   */
  create(issueData, userId) {
    return httpClient.post(API_ENDPOINTS.ISSUES, issueData, {
      headers: {
        'X-User-Id': userId
      }
    });
  },

  /**
   * Update issue
   * @param {number} id - Issue ID
   * @param {Object} issueData - Updated issue data
   * @returns {Promise<Object>} Updated issue
   */
  update(id, issueData) {
    return httpClient.put(API_ENDPOINTS.ISSUES_BY_ID(id), issueData);
  },

  /**
   * Update issue status
   * @param {number} id - Issue ID
   * @param {string} status - New status (TODO, IN_PROGRESS, RESOLVED, CLOSED)
   * @returns {Promise<Object>} Updated issue
   */
  updateStatus(id, status) {
    return httpClient.patch(`${API_ENDPOINTS.ISSUE_STATUS(id)}?newStatus=${status}`);
  },

  /**
   * Delete issue
   * @param {number} id - Issue ID
   * @returns {Promise<void>}
   */
  delete(id) {
    return httpClient.delete(API_ENDPOINTS.ISSUES_BY_ID(id));
  },

  /**
   * Search issues with filters
   * @param {Object} params - Search parameters
   * @param {string} params.term - Search term
   * @param {string} params.priority - Priority filter
   * @param {string} params.status - Status filter
   * @returns {Promise<Array>} Filtered issues
   */
  search(params) {
    const queryString = new URLSearchParams(
      Object.entries(params).filter(([_, v]) => v != null)
    ).toString();
    
    return httpClient.get(`${API_ENDPOINTS.ISSUES_SEARCH}?${queryString}`);
  },

  /**
   * Flag an issue as duplicate of another (Admin only)
   * @param {number} duplicateId - ID of the issue to mark as duplicate
   * @param {number} originalId - ID of the original issue
   * @param {number} adminId - Admin user ID
   * @returns {Promise<Object>} Response with message
   */
  flagAsDuplicate(duplicateId, originalId, adminId) {
    return httpClient.post(API_ENDPOINTS.ISSUE_FLAG_DUPLICATE(duplicateId, originalId), null, {
      headers: {
        'X-User-Id': adminId
      }
    });
  },
};

export default issueService;
