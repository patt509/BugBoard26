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
    console.warn('Unable to parse localStorage user for dashboard request header', error);
    return null;
  }
};

const resolveAuthHeaders = (userId, { required = false } = {}) => {
  const resolvedUserId = userId ?? resolveStoredUserId();
  if (required && (resolvedUserId == null || resolvedUserId === '')) {
    throw new Error('User authentication is required. Please login again.');
  }

  return resolvedUserId == null || resolvedUserId === ''
    ? {}
    : { 'X-User-Id': String(resolvedUserId) };
};

const buildQueryString = (params = {}) =>
  new URLSearchParams(
    Object.entries(params).filter(([_, value]) => {
      if (value == null) {
        return false;
      }
      if (typeof value === 'string') {
        return value.trim().length > 0;
      }
      return true;
    })
  ).toString();

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
   * Get issue history timeline by ID
   * @param {number} id - Issue ID
   * @returns {Promise<Array>} History events
   */
  getHistory(id) {
    return httpClient.get(API_ENDPOINTS.ISSUE_HISTORY(id));
  },

  /**
   * Create new issue
   * @param {Object} issueData - Issue data
   * @param {number} userId - User ID for the X-User-Id header
   * @returns {Promise<Object>} Created issue response with id
   */
  create(issueData, userId) {
    return httpClient.post(API_ENDPOINTS.ISSUES, issueData, {
      headers: resolveAuthHeaders(userId, { required: true })
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
   * Search issues with filters
   * @param {Object} params - Search parameters
   * @param {string} params.term - Search term
   * @param {string} params.priority - Priority filter
   * @param {string} params.status - Status filter
   * @returns {Promise<Array>} Filtered issues
   */
  search(params) {
    const queryString = buildQueryString(params);

    const endpoint = queryString
      ? `${API_ENDPOINTS.ISSUES_SEARCH}?${queryString}`
      : API_ENDPOINTS.ISSUES_SEARCH;

    return httpClient.get(endpoint);
  },

  /**
   * Get admin dashboard statistics
   * @param {number|string} [userId] - Optional user id for X-User-Id header
   * @returns {Promise<Object>} Dashboard stats
   */
  getDashboardStats(userId) {
    return httpClient.get(API_ENDPOINTS.ISSUES_ADMIN_DASHBOARD, {
      headers: resolveAuthHeaders(userId, { required: true })
    });
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
      headers: resolveAuthHeaders(adminId, { required: true })
    });
  },
};

export default issueService;
