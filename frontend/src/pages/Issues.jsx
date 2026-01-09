import { useState, useEffect } from 'react';
import { Search, Plus, CheckCircle, X } from 'lucide-react';
import Sidebar from '../components/Sidebar';
import { issueService } from '../services/issue.service';

function Issues({ user, onLogout, onCreateIssue, onIssueClick, successMessage, onDismissSuccess }) {
  const [currentPage, setCurrentPage] = useState('issues');
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');
  const [priorityFilter, setPriorityFilter] = useState('all');
  const [issues, setIssues] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchIssues();
  }, []);

  const fetchIssues = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await issueService.getAll();
      setIssues(data);
    } catch (err) {
      console.error('Error fetching issues:', err);
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const getStatusBadgeClass = (status) => {
    const classes = {
      'TODO': 'bg-green-100 text-green-700',
      'Open': 'bg-green-100 text-green-700',
      'IN_PROGRESS': 'bg-blue-100 text-blue-700',
      'In Progress': 'bg-blue-100 text-blue-700',
      'RESOLVED': 'bg-purple-100 text-purple-700',
      'Resolved': 'bg-purple-100 text-purple-700',
      'CLOSED': 'bg-gray-100 text-gray-700',
      'Closed': 'bg-gray-100 text-gray-700',
    };
    return classes[status] || 'bg-gray-100 text-gray-700';
  };

  const getStatusLabel = (status) => {
    const labels = {
      'TODO': 'TODO',
      'IN_PROGRESS': 'In Progress',
      'RESOLVED': 'Resolved',
      'CLOSED': 'Closed',
    };
    return labels[status] || status;
  };

  const getPriorityBadgeClass = (priority) => {
    const classes = {
      'CRITICAL': 'bg-red-500 text-white',
      'Critical': 'bg-red-500 text-white',
      'HIGH': 'bg-red-100 text-red-700',
      'High': 'bg-red-100 text-red-700',
      'MEDIUM': 'bg-yellow-100 text-yellow-700',
      'Medium': 'bg-yellow-100 text-yellow-700',
      'LOW': 'bg-green-100 text-green-700',
      'Low': 'bg-green-100 text-green-700',
    };
    return classes[priority] || 'bg-gray-100 text-gray-700';
  };

  const getPriorityLabel = (priority) => {
    const labels = {
      'CRITICAL': 'Critical',
      'HIGH': 'High',
      'MEDIUM': 'Medium',
      'LOW': 'Low',
    };
    return labels[priority] || priority;
  };

  return (
    <div className="flex h-screen bg-gray-50">
      <Sidebar currentPage={currentPage} onNavigate={setCurrentPage} />

      <main className="flex-1 overflow-auto">
        {/* Header */}
        <header className="bg-white border-b border-gray-200 px-8 py-4">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">All Issues</h1>
            </div>
            <div className="flex items-center gap-4">
              <span className="text-sm text-gray-600">
                {user.username || user.email}
              </span>
              <button
                onClick={onLogout}
                className="text-sm text-gray-600 hover:text-gray-900"
              >
                Logout
              </button>
            </div>
          </div>
        </header>

        {/* Content */}
        <div className="p-8">
          {/* Filters and Search */}
          <div className="flex flex-wrap items-center gap-4 mb-6">
            {/* Search */}
            <div className="flex-1 min-w-[300px]">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={20} />
                <input
                  type="text"
                  placeholder="Search issues..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full pl-10 pr-4 py-2.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
              </div>
            </div>

            {/* Filters */}
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="px-4 py-2.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white"
            >
              <option value="all">Status</option>
              <option value="open">Open</option>
              <option value="in-progress">In Progress</option>
              <option value="closed">Closed</option>
            </select>

            <select
              value={priorityFilter}
              onChange={(e) => setPriorityFilter(e.target.value)}
              className="px-4 py-2.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white"
            >
              <option value="all">Priority</option>
              <option value="high">High</option>
              <option value="medium">Medium</option>
              <option value="low">Low</option>
            </select>

            <select className="px-4 py-2.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white">
              <option>Assignee</option>
            </select>

            <select className="px-4 py-2.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white">
              <option>Sort by</option>
            </select>

            {/* New Issue Button */}
            <button 
              onClick={onCreateIssue}
              className="ml-auto flex items-center gap-2 px-6 py-2.5 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors font-medium"
            >
              <Plus size={20} />
              New Issue
            </button>
          </div>

          {/* Table */}
          <div className="bg-white rounded-lg border border-gray-200 overflow-hidden">
            {loading ? (
              <div className="flex items-center justify-center py-20">
                <div className="text-center">
                  <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
                  <p className="text-gray-600">Loading issues...</p>
                </div>
              </div>
            ) : error ? (
              <div className="flex items-center justify-center py-20">
                <div className="text-center">
                  <p className="text-red-600 mb-2">Error loading issues</p>
                  <p className="text-gray-500 text-sm">{error}</p>
                  <button 
                    onClick={fetchIssues}
                    className="mt-4 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
                  >
                    Retry
                  </button>
                </div>
              </div>
            ) : issues.length === 0 ? (
              <div className="flex items-center justify-center py-20">
                <div className="text-center">
                  <p className="text-gray-500 text-lg">No issues found</p>
                  <p className="text-gray-400 text-sm mt-2">Create your first issue to get started</p>
                </div>
              </div>
            ) : (
              <table className="w-full">
                <thead className="bg-gray-50 border-b border-gray-200">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      ID
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Title
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Status
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Priority
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Updated
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {issues.map((issue) => (
                    <tr 
                      key={issue.id} 
                      className="hover:bg-gray-50 cursor-pointer transition-colors"
                      onClick={() => onIssueClick && onIssueClick(issue.id)}
                    >
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                        #{issue.id}
                      </td>
                      <td className="px-6 py-4 text-sm text-gray-900">
                        {issue.title}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <span className={`inline-flex px-2.5 py-1 rounded-full text-xs font-medium ${getStatusBadgeClass(issue.status)}`}>
                          {getStatusLabel(issue.status)}
                        </span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <span className={`inline-flex px-2.5 py-1 rounded-full text-xs font-medium ${getPriorityBadgeClass(issue.priority)}`}>
                          {getPriorityLabel(issue.priority)}
                        </span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {issue.updatedAt ? new Date(issue.updatedAt).toLocaleString() : 'N/A'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>
      </main>

      {/* Success Toast Notification */}
      {successMessage && (
        <div className="fixed bottom-8 left-1/2 transform -translate-x-1/2 animate-slide-up z-50">
          <div className="bg-green-50 border border-green-200 rounded-lg shadow-lg px-6 py-4 flex items-center gap-3 min-w-[400px]">
            <CheckCircle className="w-6 h-6 text-green-600 flex-shrink-0" />
            <span className="text-green-800 font-medium flex-1">{successMessage}</span>
            <button
              onClick={onDismissSuccess}
              className="text-green-600 hover:text-green-800 transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

export default Issues;
