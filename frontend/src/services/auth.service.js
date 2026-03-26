import httpClient from '../utils/httpClient';
import { API_ENDPOINTS } from '../constants/api';

const ASSIGNABLE_USERS_TTL_MS = 30_000;

const assignableUsersCacheByUserId = new Map();

const resolveAssignableUsersCacheEntry = (userId) => {
  const cached = assignableUsersCacheByUserId.get(userId);
  if (cached) {
    return cached;
  }

  const initialEntry = {
    timestamp: 0,
    data: null,
    inFlight: null,
  };
  assignableUsersCacheByUserId.set(userId, initialEntry);
  return initialEntry;
};

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

  getAssignableUsers(userId, options = {}) {
    const resolvedUserId = userId ?? resolveStoredUserId();
    if (resolvedUserId == null || resolvedUserId === '') {
      throw new Error('User authentication is required. Please login again.');
    }

    const normalizedUserId = String(resolvedUserId);
    const forceRefresh = options?.forceRefresh === true;
    const now = Date.now();
    const cacheEntry = resolveAssignableUsersCacheEntry(normalizedUserId);
    const isCacheValid =
      !forceRefresh &&
      cacheEntry.data != null &&
      now - cacheEntry.timestamp < ASSIGNABLE_USERS_TTL_MS;

    if (isCacheValid) {
      return Promise.resolve([...cacheEntry.data]);
    }

    if (!forceRefresh && cacheEntry.inFlight) {
      return cacheEntry.inFlight;
    }

    const request = httpClient.get(API_ENDPOINTS.AUTH_USERS_ASSIGNABLE, {
      headers: {
        'X-User-Id': normalizedUserId,
      },
    });

    cacheEntry.inFlight = request
      .then((users) => {
        const normalizedUsers = Array.isArray(users) ? users : [];
        cacheEntry.data = normalizedUsers;
        cacheEntry.timestamp = Date.now();
        return [...normalizedUsers];
      })
      .finally(() => {
        cacheEntry.inFlight = null;
      });

    return cacheEntry.inFlight;
  },
};

export default authService;
