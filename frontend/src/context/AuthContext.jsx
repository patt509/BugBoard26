/**
 * AuthContext - Global authentication state management
 * Provides user state and login/logout methods
 */

import React, { createContext, useState, useCallback, useEffect } from 'react';

export const AuthContext = createContext();

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  // Hydrate user from localStorage on mount
  useEffect(() => {
    try {
      const stored = localStorage.getItem('user');
      if (stored) {
        setUser(JSON.parse(stored));
      }
    } catch {
      // Ignore parse errors
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Login user - calls backend API
   * @param {string} email
   * @param {string} password
   * @returns {Promise<object>} - User object from backend
   */
  const login = useCallback(async (email, password) => {
    const { login: loginAPI } = await import('../services/api');
    const userData = await loginAPI(email, password);

    // Save user and userId to localStorage
    localStorage.setItem('user', JSON.stringify(userData));
    localStorage.setItem('userId', userData.id);

    setUser(userData);
    return userData;
  }, []);

  /**
   * Logout user - clears state and localStorage
   */
  const logout = useCallback(() => {
    localStorage.removeItem('user');
    localStorage.removeItem('userId');
    setUser(null);
  }, []);

  /**
   * Update user profile (e.g., after finalize-profile)
   * @param {object} updates - Fields to update
   */
  const updateUser = useCallback((updates) => {
    const updated = { ...user, ...updates };
    setUser(updated);
    localStorage.setItem('user', JSON.stringify(updated));
  }, [user]);

  const value = {
    user,
    loading,
    login,
    logout,
    updateUser,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

/**
 * Hook to use auth context
 */
export function useAuth() {
  const context = React.useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
}
