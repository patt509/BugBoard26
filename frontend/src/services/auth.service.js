import httpClient from '../utils/httpClient';
import { API_ENDPOINTS } from '../constants/api';

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

  /**
   * Logout user
   * @returns {Promise<void>}
   */
  logout() {
    return httpClient.post(API_ENDPOINTS.AUTH_LOGOUT);
  },

  /**
   * Register new user
   * @param {Object} userData - User registration data
   * @returns {Promise<Object>} Created user data
   */
  register(userData) {
    return httpClient.post(API_ENDPOINTS.AUTH_REGISTER, userData);
  },
};

export default authService;
