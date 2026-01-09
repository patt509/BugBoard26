import { useState, useEffect, useCallback } from 'react';
import { ArrowLeft, Download, Flag, Edit, ChevronDown, Send, AlertCircle, X } from 'lucide-react';
import Sidebar from '../components/Sidebar';
import { issueService } from '../services/issue.service';
import { commentService } from '../services/comment.service';

function IssueDetail({ user, onLogout, issueId, onBack }) {
  const [currentPage] = useState('issues');
  const [issue, setIssue] = useState(null);
  const [comments, setComments] = useState([]);
  const [newComment, setNewComment] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [commentLoading, setCommentLoading] = useState(false);
  const [statusDropdownOpen, setStatusDropdownOpen] = useState(false);

  const fetchIssueData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      
      const [issueData, commentsData] = await Promise.all([
        issueService.getById(issueId),
        commentService.getByIssueId(issueId).catch(() => [])
      ]);
      
      setIssue(issueData);
      setComments(commentsData || []);
    } catch (err) {
      console.error('Error fetching issue:', err);
      setError(err.message || 'Failed to load issue');
    } finally {
      setLoading(false);
    }
  }, [issueId]);

  useEffect(() => {
    fetchIssueData();
  }, [fetchIssueData]);

  const handleStatusChange = async (newStatus) => {
    try {
      await issueService.updateStatus(issueId, newStatus);
      setIssue(prev => ({ ...prev, status: newStatus }));
      setStatusDropdownOpen(false);
    } catch (err) {
      console.error('Error updating status:', err);
      setError(err.message || 'Failed to update status');
    }
  };

  const handleSubmitComment = async (e) => {
    e.preventDefault();
    if (!newComment.trim()) return;

    try {
      setCommentLoading(true);
      const commentData = {
        text: newComment.trim(),
        authorId: user?.id
      };
      
      await commentService.create(issueId, commentData);
      setNewComment('');
      
      // Refresh comments
      const updatedComments = await commentService.getByIssueId(issueId);
      setComments(updatedComments || []);
    } catch (err) {
      console.error('Error adding comment:', err);
      setError(err.message || 'Failed to add comment');
    } finally {
      setCommentLoading(false);
    }
  };

  const getStatusColor = (status) => {
    const colors = {
      'TODO': 'bg-green-100 text-green-800',
      'IN_PROGRESS': 'bg-blue-100 text-blue-800',
      'RESOLVED': 'bg-purple-100 text-purple-800',
      'CLOSED': 'bg-gray-100 text-gray-800',
    };
    return colors[status] || 'bg-gray-100 text-gray-800';
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

  const getPriorityColor = (priority) => {
    const colors = {
      'CRITICAL': 'bg-red-600 text-white',
      'HIGH': 'bg-red-500 text-white',
      'MEDIUM': 'bg-yellow-500 text-white',
      'LOW': 'bg-green-500 text-white',
    };
    return colors[priority] || 'bg-gray-500 text-white';
  };

  const getTypeIcon = (type) => {
    const icons = {
      'Bug': '🐛',
      'Feature': '✨',
      'Task': '📋',
      'Improvement': '🔧',
    };
    return icons[type] || '📋';
  };

  const formatTimeAgo = (dateString) => {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now - date;
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins} minutes ago`;
    if (diffHours < 24) return `${diffHours} hours ago`;
    if (diffDays === 1) return 'Yesterday';
    return `${diffDays} days ago`;
  };

  const formatTime = (dateString) => {
    if (!dateString) return '';
    return new Date(dateString).toLocaleTimeString('en-US', {
      hour: 'numeric',
      minute: '2-digit',
      hour12: true
    });
  };

  const statusOptions = ['TODO', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];

  if (loading) {
    return (
      <div className="flex h-screen bg-gray-50">
        <Sidebar currentPage={currentPage} onNavigate={() => {}} />
        <main className="flex-1 flex items-center justify-center">
          <div className="text-center">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
            <p className="text-gray-600">Loading issue...</p>
          </div>
        </main>
      </div>
    );
  }

  if (error && !issue) {
    return (
      <div className="flex h-screen bg-gray-50">
        <Sidebar currentPage={currentPage} onNavigate={() => {}} />
        <main className="flex-1 flex items-center justify-center">
          <div className="text-center">
            <AlertCircle className="w-12 h-12 text-red-500 mx-auto mb-4" />
            <p className="text-red-600 mb-2">Error loading issue</p>
            <p className="text-gray-500 text-sm mb-4">{error}</p>
            <button
              onClick={onBack}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
            >
              Go Back
            </button>
          </div>
        </main>
      </div>
    );
  }

  return (
    <div className="flex h-screen bg-gray-50">
      <Sidebar currentPage={currentPage} onNavigate={() => {}} />

      <main className="flex-1 overflow-auto">
        {/* Header */}
        <header className="bg-white border-b border-gray-200 px-8 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <button
                onClick={onBack}
                className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
              >
                <ArrowLeft className="w-5 h-5 text-gray-600" />
              </button>
              <h1 className="text-2xl font-bold text-gray-900">
                Issue #{issue?.id}: {issue?.title}
              </h1>
            </div>
            <div className="flex items-center gap-4">
              <span className="text-sm text-gray-600">
                {user?.username || user?.email}
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
          {/* Error Banner */}
          {error && (
            <div className="mb-6 flex items-center gap-3 px-4 py-3 bg-red-100 border border-red-300 rounded-lg">
              <div className="flex-shrink-0 w-6 h-6 bg-red-500 rounded-full flex items-center justify-center">
                <X className="w-4 h-4 text-white" />
              </div>
              <span className="text-red-800 font-medium">{error}</span>
              <button
                onClick={() => setError(null)}
                className="ml-auto text-red-600 hover:text-red-800"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
          )}

          <div className="flex gap-8">
            {/* Main Content */}
            <div className="flex-1">
              {/* Action Buttons */}
              <div className="flex items-center gap-3 mb-6">
                <button className="flex items-center gap-2 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors">
                  <Flag className="w-4 h-4" />
                  Flag as Duplicate
                </button>
                <button className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors">
                  <Edit className="w-4 h-4" />
                  Edit Issue
                </button>
                
                {/* Status Dropdown */}
                <div className="relative ml-auto">
                  <button
                    onClick={() => setStatusDropdownOpen(!statusDropdownOpen)}
                    className={`flex items-center gap-2 px-4 py-2 rounded-lg border ${getStatusColor(issue?.status)}`}
                  >
                    Status: {getStatusLabel(issue?.status)}
                    <ChevronDown className="w-4 h-4" />
                  </button>
                  
                  {statusDropdownOpen && (
                    <div className="absolute right-0 mt-2 w-48 bg-white border border-gray-200 rounded-lg shadow-lg z-10">
                      {statusOptions.map((status) => (
                        <button
                          key={status}
                          onClick={() => handleStatusChange(status)}
                          className={`w-full text-left px-4 py-2 hover:bg-gray-50 first:rounded-t-lg last:rounded-b-lg ${
                            issue?.status === status ? 'bg-gray-100' : ''
                          }`}
                        >
                          {getStatusLabel(status)}
                        </button>
                      ))}
                    </div>
                  )}
                </div>
              </div>

              {/* Description */}
              <div className="bg-white rounded-lg border border-gray-200 p-6 mb-6">
                <h2 className="text-lg font-semibold text-gray-900 mb-4">Description</h2>
                <div className="text-gray-700 whitespace-pre-wrap">
                  {issue?.description || 'No description provided.'}
                </div>
              </div>

              {/* Attachments */}
              {issue?.attachmentPath && (
                <div className="bg-white rounded-lg border border-gray-200 p-6 mb-6">
                  <h2 className="text-lg font-semibold text-gray-900 mb-4">Attachments</h2>
                  <div className="flex items-center gap-4 p-4 bg-gray-50 rounded-lg border border-gray-200">
                    <div className="w-16 h-16 bg-gray-200 rounded flex items-center justify-center overflow-hidden">
                      <img
                        src={`/api/attachments/download?path=${encodeURIComponent(issue.attachmentPath)}`}
                        alt="Attachment preview"
                        className="w-full h-full object-cover"
                        onError={(e) => {
                          e.target.style.display = 'none';
                          e.target.parentElement.innerHTML = '📎';
                        }}
                      />
                    </div>
                    <div className="flex-1">
                      <p className="text-sm font-medium text-gray-900">
                        {issue.attachmentPath.split('/').pop()}
                      </p>
                    </div>
                    <a
                      href={`/api/attachments/download?path=${encodeURIComponent(issue.attachmentPath)}`}
                      download
                      className="p-2 hover:bg-gray-200 rounded transition-colors"
                    >
                      <Download className="w-5 h-5 text-gray-600" />
                    </a>
                  </div>
                </div>
              )}

              {/* Comments Section */}
              <div className="bg-white rounded-lg border border-gray-200 p-6">
                <h2 className="text-lg font-semibold text-gray-900 mb-4">Comments</h2>
                
                {/* Comments List */}
                <div className="space-y-4 mb-6">
                  {comments.length === 0 ? (
                    <p className="text-gray-500 text-center py-4">No comments yet</p>
                  ) : (
                    comments.map((comment) => (
                      <div
                        key={comment.id}
                        className={`flex gap-3 ${
                          comment.authorId === user?.id ? 'flex-row-reverse' : ''
                        }`}
                      >
                        <div className="w-10 h-10 bg-gray-200 rounded-full flex items-center justify-center flex-shrink-0">
                          <span className="text-gray-600 text-sm font-medium">
                            {(comment.authorUsername || 'U').charAt(0).toUpperCase()}
                          </span>
                        </div>
                        <div
                          className={`max-w-md px-4 py-3 rounded-lg ${
                            comment.authorId === user?.id
                              ? 'bg-blue-100 text-blue-900'
                              : 'bg-gray-100 text-gray-900'
                          }`}
                        >
                          <div className="flex items-center gap-2 mb-1">
                            <span className="text-sm font-medium">
                              {comment.authorUsername || 'Unknown'}
                            </span>
                          </div>
                          <p className="text-sm">{comment.text}</p>
                          <span className="text-xs text-gray-500 mt-1 block">
                            {formatTime(comment.createdAt)}
                          </span>
                        </div>
                      </div>
                    ))
                  )}
                </div>

                {/* Add Comment Form */}
                <form onSubmit={handleSubmitComment} className="flex gap-3">
                  <input
                    type="text"
                    value={newComment}
                    onChange={(e) => setNewComment(e.target.value)}
                    placeholder="Add a comment..."
                    className="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    disabled={commentLoading}
                  />
                  <button
                    type="submit"
                    disabled={commentLoading || !newComment.trim()}
                    className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                  >
                    {commentLoading ? 'Sending...' : 'Submit'}
                  </button>
                </form>
              </div>
            </div>

            {/* Sidebar - Details */}
            <div className="w-80">
              <div className="bg-white rounded-lg border border-gray-200 p-6">
                <h2 className="text-lg font-semibold text-gray-900 mb-4">Details</h2>
                
                <div className="space-y-4">
                  {/* Type */}
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-500">Type</span>
                    <span className="text-sm font-medium text-gray-900 flex items-center gap-1">
                      {getTypeIcon(issue?.type)} {issue?.type || 'Bug'}
                    </span>
                  </div>

                  {/* Priority */}
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-500">Priority</span>
                    <span className={`text-xs font-medium px-2.5 py-1 rounded-full ${getPriorityColor(issue?.priority)}`}>
                      {issue?.priority}
                    </span>
                  </div>

                  {/* Assignee */}
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-500">Assignee</span>
                    <div className="flex items-center gap-2">
                      <div className="w-6 h-6 bg-gray-200 rounded-full flex items-center justify-center">
                        <span className="text-xs text-gray-600">
                          {(issue?.assignee || 'U').charAt(0).toUpperCase()}
                        </span>
                      </div>
                      <span className="text-sm font-medium text-gray-900">
                        {issue?.assignee || 'Unassigned'}
                      </span>
                    </div>
                  </div>

                  {/* Reporter */}
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-500">Reporter</span>
                    <span className="text-sm font-medium text-gray-900">
                      {issue?.reporterName || 'Unknown'}
                    </span>
                  </div>

                  {/* Created */}
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-500">Created</span>
                    <span className="text-sm text-gray-900">
                      {formatTimeAgo(issue?.createdAt)}
                    </span>
                  </div>

                  {/* Updated */}
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-500">Updated</span>
                    <span className="text-sm text-gray-900">
                      {formatTimeAgo(issue?.updatedAt || issue?.createdAt)}
                    </span>
                  </div>

                  {/* Original Issue (if duplicate) */}
                  {issue?.originalIssueId && (
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-gray-500">Duplicate of</span>
                      <span className="text-sm text-blue-600 font-medium">
                        #{issue.originalIssueId}
                      </span>
                    </div>
                  )}
                </div>

                {/* History Section */}
                <div className="mt-6 pt-6 border-t border-gray-200">
                  <button className="flex items-center justify-between w-full text-left">
                    <span className="text-lg font-semibold text-gray-900">History</span>
                    <ChevronDown className="w-5 h-5 text-gray-400" />
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}

export default IssueDetail;
