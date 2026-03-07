/**
 * Centralized API client for backend communication.
 * Handles Fetch API calls, automatic header injection, and response processing.
 */

const API_BASE_URL = 'http://localhost:8080/api';

/**
 * Adds headers including X-User-Id from localStorage if available.
 */
function getHeaders(additionalHeaders = {}) {
  const headers = {
    'Content-Type': 'application/json',
    ...additionalHeaders,
  };

  const userId = localStorage.getItem('userId');
  if (userId) {
    headers['X-User-Id'] = userId;
  }

  return headers;
}

/**
 * Wrapper around fetch that handles errors and JSON parsing.
 * @param {string} endpoint - API endpoint (e.g., '/auth/login')
 * @param {string} method - HTTP method (GET, POST, etc.)
 * @param {object} body - Request body (for POST/PUT requests)
 * @returns {Promise<object>} - Parsed JSON response
 * @throws {Error} - If response status is not ok
 */
async function apiCall(endpoint, method = 'GET', body = null) {
  const url = `${API_BASE_URL}${endpoint}`;
  const options = {
    method,
    headers: getHeaders(),
  };

  if (body) {
    options.body = JSON.stringify(body);
  }

  const response = await fetch(url, options);

  // Handle non-2xx responses
  if (!response.ok) {
    let errorMessage = `HTTP ${response.status}`;
    try {
      const errorData = await response.json();
      errorMessage = errorData.message || errorMessage;
    } catch {
      // Response not JSON, use default error message
    }
    throw new Error(errorMessage);
  }

  // Parse and return JSON
  return response.json();
}

/**
 * POST /auth/login
 * @param {string} email - User email
 * @param {string} password - User password
 * @returns {Promise<object>} - { id, email, username, role, firstLogin, createdAt }
 */
export async function login(email, password) {
  return apiCall('/auth/login', 'POST', { email, password });
}

/**
 * POST /auth/finalize-profile
 * @param {number} userId - User ID
 * @param {string} chosenUsername - Username to set
 * @returns {Promise<object>} - Updated user DTO
 */
export async function finalizeProfile(userId, chosenUsername) {
  return apiCall('/auth/finalize-profile', 'POST', {
    userId,
    chosenUsername,
  });
}

/**
 * GET /issues/search (with optional filters)
 * @param {object} filters - { type, assigneeId, status }
 * @returns {Promise<array>} - List of issue DTOs
 */
export async function searchIssues(filters = {}) {
  const query = new URLSearchParams(filters).toString();
  const endpoint = query ? `/issues/search?${query}` : '/issues/search';
  return apiCall(endpoint, 'GET');
}

/**
 * GET /issues
 * @returns {Promise<array>} - List of all issue DTOs
 */
export async function getAllIssues() {
  return apiCall('/issues', 'GET');
}

/**
 * GET /dashboard/stats
 * @returns {Promise<object>} - Dashboard stats
 */
export async function getDashboardStats() {
  return apiCall('/dashboard/stats', 'GET');
}
