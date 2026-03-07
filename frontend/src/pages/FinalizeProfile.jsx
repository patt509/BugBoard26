/**
 * Finalize Profile Page - Set username on first login
 */

import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { finalizeProfile as finalizeProfileAPI } from '../services/api';

export default function FinalizeProfile() {
  const navigate = useNavigate();
  const { user, updateUser } = useAuth();

  const [username, setUsername] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  // Redirect if not first login or not logged in
  if (!user) {
    navigate('/login');
    return null;
  }

  if (!user.firstLogin) {
    navigate('/');
    return null;
  }

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const updatedUser = await finalizeProfileAPI(user.id, username);
      updateUser(updatedUser);
      navigate('/');
    } catch (err) {
      setError(err.message || 'Failed to set username. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="finalize-profile-container">
      <h1>Set Your Username</h1>
      <p>Welcome! Please choose a username for your account.</p>

      <form onSubmit={handleSubmit} className="finalize-form">
        <div className="form-group">
          <label htmlFor="username">Username (min 3 characters):</label>
          <input
            id="username"
            type="text"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            minLength={3}
            required
            placeholder="my-username"
            disabled={loading}
          />
        </div>

        {error && <div className="error-message">{error}</div>}

        <button type="submit" disabled={loading || username.length < 3}>
          {loading ? 'Setting username...' : 'Continue'}
        </button>
      </form>
    </div>
  );
}
