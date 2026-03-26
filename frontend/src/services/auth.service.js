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
    console.warn('Unable to parse localStorage user for auth request header', error);
    return null;
  }
};

/**
 * Authentication Service
 * Handles all authentication-related API calls
 */
export const authService = {
  /**
   * Login user
   * @param {Object} credentials - User credentials
   * @param {string} credentials.email - User email
   * @param {string} credentials.password - User password
   * @returns {Promise<Object>} User data
   */
  login(credentials) {
    return httpClient.post(API_ENDPOINTS.AUTH_LOGIN, credentials);
  },

  getAssignableUsers(userId) {
    const resolvedUserId = userId ?? resolveStoredUserId();
    if (resolvedUserId == null || resolvedUserId === '') {
      throw new Error('User authentication is required. Please login again.');
    }

    return httpClient.get(API_ENDPOINTS.AUTH_USERS_ASSIGNABLE, {
      headers: {
        'X-User-Id': String(resolvedUserId),
      },
    });
  },
};

export default authService;
