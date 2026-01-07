import { useState } from 'react';
import './Dashboard.css';

function Dashboard({ user, onLogout }) {
  const [issues] = useState([]);

  const handleLogout = () => {
    localStorage.removeItem('user');
    onLogout();
  };

  return (
    <div className="dashboard-container">
      <header className="dashboard-header">
        <div className="header-content">
          <h1>🐛 BugBoard</h1>
          <div className="user-info">
            <span className="user-name">
              {user.username || user.email}
            </span>
            <span className="user-role">{user.role}</span>
            <button onClick={handleLogout} className="logout-button">
              Logout
            </button>
          </div>
        </div>
      </header>

      <main className="dashboard-main">
        <div className="dashboard-content">
          <div className="page-header">
            <h2>Issues Dashboard</h2>
            <button className="new-issue-button">+ New Issue</button>
          </div>

          <div className="issues-container">
            {issues.length === 0 ? (
              <div className="empty-state">
                <div className="empty-icon">📋</div>
                <h3>No issues yet</h3>
                <p>Create your first issue to get started</p>
              </div>
            ) : (
              <div className="issues-list">
                {/* Issue list will be implemented here */}
                {issues.map((issue) => (
                  <div key={issue.id} className="issue-card">
                    <h3>{issue.title}</h3>
                    <p>{issue.description}</p>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </main>
    </div>
  );
}

export default Dashboard;
