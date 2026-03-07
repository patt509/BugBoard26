/**
 * Dashboard - Main page after login
 */

import React from 'react';
import { useAuth } from '../context/AuthContext';

export default function Dashboard() {
  const { user, logout } = useAuth();

  if (!user) {
    return <div>Loading...</div>;
  }

  return (
    <div className="dashboard-container">
      <header className="dashboard-header">
        <h1>Benvenuto nella Dashboard</h1>
        <button onClick={logout} className="logout-btn">
          Logout
        </button>
      </header>

      <div className="user-info">
        <p>
          <strong>Email:</strong> {user.email}
        </p>
        <p>
          <strong>Username:</strong> {user.username || 'Not set'}
        </p>
        <p>
          <strong>Role:</strong> {user.role}
        </p>
      </div>

      <section className="dashboard-content">
        <h2>Dashboard Content</h2>
        <p>This is the main dashboard area. More features coming soon!</p>
      </section>
    </div>
  );
}
