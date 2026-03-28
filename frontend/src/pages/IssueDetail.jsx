import { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { ArrowLeft, Download, Flag, Edit, ChevronDown, Send, AlertCircle, X, CheckCircle, Search, Upload, Paperclip } from 'lucide-react';
import Sidebar from '../components/Sidebar';
import ThemeToggle from '../components/ThemeToggle';
import UserIdentity from '../components/UserIdentity';
import { issueService } from '../services/issue.service';
import { commentService } from '../services/comment.service';
import { attachmentService } from '../services/attachment.service';

const API_TIMESTAMP_WITH_TIMEZONE_REGEX = /(Z|[+-]\d{2}:?\d{2})$/i;
const DICEBEAR_BASE_URL = 'https://api.dicebear.com/9.x/glass/svg';

const parseApiDate = (input) => {
  if (!input) {
    return null;
  }

  const rawValue = String(input).trim();
  if (!rawValue) {
    return null;
  }

  const normalizedValue = rawValue.includes('T')
    ? rawValue
    : rawValue.replace(' ', 'T');
  const valueWithTimezone = API_TIMESTAMP_WITH_TIMEZONE_REGEX.test(normalizedValue)
    ? normalizedValue
    : `${normalizedValue}Z`;
  const parsedDate = new Date(valueWithTimezone);

  return Number.isNaN(parsedDate.getTime()) ? null : parsedDate;
};

const resolveDisplayInitial = (value, fallback = 'U') => {
  const normalized = String(value ?? '').trim();
  if (!normalized) {
    return fallback;
  }
  return normalized.charAt(0).toUpperCase();
};

const buildAvatarSeed = ({ username, id, fallback = 'User' }) => {
  const normalizedName = String(username ?? '').trim() || fallback;
  const normalizedId = id != null ? String(id) : 'unknown';
  return `${normalizedName}-${normalizedId}`;
};

const getAvatarUrlBySeed = (seed, size = 64) => {
  const normalizedSeed = encodeURIComponent(String(seed ?? 'unknown'));
  return `${DICEBEAR_BASE_URL}?seed=${normalizedSeed}&radius=50&size=${size}`;
};

function AvatarCircle({
  seed,
  label,
  fallback = 'U',
  sizeClass = 'w-10 h-10',
  textClass = 'text-sm'
}) {
  const [imageLoadFailed, setImageLoadFailed] = useState(false);

  useEffect(() => {
    setImageLoadFailed(false);
  }, [seed]);

  const avatarUrl = getAvatarUrlBySeed(seed);
  const initial = resolveDisplayInitial(label, resolveDisplayInitial(fallback));

  return (
    <div className={`${sizeClass} overflow-hidden rounded-full border border-gray-200 bg-gray-100 flex-shrink-0`}>
      {imageLoadFailed ? (
        <div className="flex h-full w-full items-center justify-center">
          <span className={`font-medium text-gray-600 ${textClass}`}>{initial}</span>
        </div>
      ) : (
        <img
          src={avatarUrl}
          alt={`${label || 'User'} avatar`}
          className="h-full w-full rounded-full object-cover"
          loading="lazy"
          referrerPolicy="no-referrer"
          onError={() => setImageLoadFailed(true)}
        />
      )}
    </div>
  );
}

const formatRelativeTime = (dateString, nowMs) => {
  const parsedDate = parseApiDate(dateString);
  if (!parsedDate) {
    return 'N/A';
  }

  const diffMs = Math.max(0, nowMs - parsedDate.getTime());
  const diffMins = Math.floor(diffMs / 60000);

  if (diffMins < 1) {
    return 'Less than 1 min ago';
  }

  if (diffMins < 60) {
    return `${diffMins} min ago`;
  }

  const diffHours = Math.floor(diffMins / 60);
  const remainingMins = diffMins % 60;
  if (diffHours < 24) {
    return remainingMins > 0
      ? `${diffHours} h ${remainingMins} min ago`
      : `${diffHours} h ago`;
  }

  const diffDays = Math.floor(diffHours / 24);
  const remainingHours = diffHours % 24;
  return remainingHours > 0
    ? `${diffDays} d ${remainingHours} h ago`
    : `${diffDays} d ago`;
};

const resolveCurrentUserId = (user) => {
  if (user?.id != null) {
    return user.id;
  }

  const storedUserId = localStorage.getItem('userId');
  if (storedUserId && !Number.isNaN(Number(storedUserId))) {
    return Number(storedUserId);
  }

  const storedUser = localStorage.getItem('user');
  if (!storedUser) {
    return null;
  }

  try {
    const parsedUser = JSON.parse(storedUser);
    return parsedUser?.id ?? null;
  } catch (error) {
    console.warn('Unable to parse localStorage user while resolving comment author ID', error);
    return null;
  }
};

const resolveCurrentUserRole = (user) => {
  if (user?.role != null && String(user.role).trim() !== '') {
    return String(user.role);
  }

  const storedUser = localStorage.getItem('user');
  if (!storedUser) {
    return '';
  }

  try {
    const parsedUser = JSON.parse(storedUser);
    return parsedUser?.role != null ? String(parsedUser.role) : '';
  } catch (error) {
    console.warn('Unable to parse localStorage user while resolving role', error);
    return '';
  }
};

function IssueDetail({
  user,
  onLogout,
  issueId,
  onBack,
  onEditIssue,
  onNavigate,
  successMessage,
  onDismissSuccess,
  isDarkMode,
  onToggleTheme
}) {
  const currentPage = 'issues';
  const [issue, setIssue] = useState(null);
  const [comments, setComments] = useState([]);
  const [newComment, setNewComment] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [commentLoading, setCommentLoading] = useState(false);
  const [statusDropdownOpen, setStatusDropdownOpen] = useState(false);
  
  // Comment attachment state
  const [commentAttachment, setCommentAttachment] = useState(null);
  const [commentAttachmentError, setCommentAttachmentError] = useState(null);
  const [attachInfo, setAttachInfo] = useState({ maxFileSizeMB: 5, allowedExtensions: ['.jpg', '.png'] });
  const [previewAttachment, setPreviewAttachment] = useState(null);
  
  // Flag as Duplicate modal state
  const [showDuplicateModal, setShowDuplicateModal] = useState(false);
  const [allIssues, setAllIssues] = useState([]);
  const [selectedOriginalIssue, setSelectedOriginalIssue] = useState(null);
  const [duplicateSearchQuery, setDuplicateSearchQuery] = useState('');
  const [flaggingDuplicate, setFlaggingDuplicate] = useState(false);
  const [duplicateSuccess, setDuplicateSuccess] = useState(null);
  const [issueHistory, setIssueHistory] = useState([]);
  const [showHistoryModal, setShowHistoryModal] = useState(false);
  const [nowMs, setNowMs] = useState(() => Date.now());
  const duplicateCandidatesCacheRef = useRef(null);
  const currentUserId = resolveCurrentUserId(user);
  const normalizedRole = resolveCurrentUserRole(user).trim().toUpperCase();
  const isAdminUser = normalizedRole.includes('ADMIN');

  useEffect(() => {
    const timerId = setInterval(() => {
      setNowMs(Date.now());
    }, 60000);

    return () => clearInterval(timerId);
  }, []);

  // Fetch attachment constraints
  useEffect(() => {
    let mounted = true;
    (async () => {
      try {
        const info = await attachmentService.getInfo();
        if (mounted && info) setAttachInfo(info);
      } catch (e) {
        console.warn('Could not fetch attachment info, using defaults', e);
      }
    })();
    return () => { mounted = false; };
  }, []);

  const fetchIssueData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      
      const [issueData, commentsData, historyData] = await Promise.all([
        issueService.getById(issueId),
        commentService.getByIssueId(issueId).catch(() => []),
        issueService.getHistory(issueId).catch(() => [])
      ]);
      
      setIssue(issueData);
      setComments(commentsData || []);
      setIssueHistory(Array.isArray(historyData) ? historyData : []);
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

  useEffect(() => {
    duplicateCandidatesCacheRef.current = null;
  }, [issueId]);

  // Auto-dismiss duplicate success message
  useEffect(() => {
    if (duplicateSuccess) {
      const timer = setTimeout(() => {
        setDuplicateSuccess(null);
      }, 4000);
      return () => clearTimeout(timer);
    }
  }, [duplicateSuccess]);

  useEffect(() => {
    if (!previewAttachment) {
      return undefined;
    }

    const handleEscapeKey = (event) => {
      if (event.key === 'Escape') {
        setPreviewAttachment(null);
      }
    };

    document.addEventListener('keydown', handleEscapeKey);
    return () => document.removeEventListener('keydown', handleEscapeKey);
  }, [previewAttachment]);

  useEffect(() => {
    if (!showHistoryModal) {
      return undefined;
    }

    const handleEscapeKey = (event) => {
      if (event.key === 'Escape') {
        setShowHistoryModal(false);
      }
    };

    document.addEventListener('keydown', handleEscapeKey);
    return () => document.removeEventListener('keydown', handleEscapeKey);
  }, [showHistoryModal]);

  const handleStatusChange = async (newStatus) => {
    try {
      await issueService.updateStatus(issueId, newStatus, currentUserId);
      setIssue(prev => ({ ...prev, status: newStatus }));
      setStatusDropdownOpen(false);
    } catch (err) {
      console.error('Error updating status:', err);
      setError(err.message || 'Failed to update status');
    }
  };

  const openAttachmentPreview = (source, name) => {
    setPreviewAttachment({
      source,
      name: name || 'Attachment preview'
    });
  };

  // Handle comment attachment selection
  const handleCommentAttachment = (e) => {
    setCommentAttachmentError(null);
    const file = e.target.files && e.target.files[0];
    if (!file) return;

    // Validate size
    const maxBytes = attachInfo.maxFileSizeMB * 1024 * 1024;
    if (file.size > maxBytes) {
      setCommentAttachmentError(`File is too large (Max ${attachInfo.maxFileSizeMB}MB).`);
      return;
    }

    if (file.size <= 0) {
      setCommentAttachmentError('File is empty');
      return;
    }

    // Validate extension
    const fileName = file.name || '';
    const ext = fileName.includes('.') ? fileName.substring(fileName.lastIndexOf('.')).toLowerCase() : '';
    const allowed = (attachInfo.allowedExtensions || ['.jpg', '.png']).map(a => {
      const lower = a.toLowerCase();
      return lower.startsWith('.') ? lower : `.${lower}`;
    });
    if (!allowed.includes(ext)) {
      setCommentAttachmentError(`Invalid file type. Only ${allowed.join(', ')} images are allowed.`);
      return;
    }

    setCommentAttachment(file);
  };

  // Remove comment attachment
  const removeCommentAttachment = () => {
    setCommentAttachment(null);
    setCommentAttachmentError(null);
  };

  const handleSubmitComment = async (e) => {
    e.preventDefault();
    if (!newComment.trim()) return;
    setError(null);

    const authorId = currentUserId;
    if (!authorId) {
      setError('User not authenticated. Please login again.');
      return;
    }

    // Validate attachment before submitting
    if (commentAttachment) {
      const maxBytes = attachInfo.maxFileSizeMB * 1024 * 1024;
      if (commentAttachment.size > maxBytes) {
        setCommentAttachmentError(`File is too large (Max ${attachInfo.maxFileSizeMB}MB).`);
        return;
      }

      const fileName = commentAttachment.name || '';
      const ext = fileName.includes('.') ? fileName.substring(fileName.lastIndexOf('.')).toLowerCase() : '';
      const allowed = (attachInfo.allowedExtensions || ['.jpg', '.png']).map(a => {
        const lower = a.toLowerCase();
        return lower.startsWith('.') ? lower : `.${lower}`;
      });
      if (!allowed.includes(ext)) {
        setCommentAttachmentError(`Invalid file type. Only ${allowed.join(', ')} images are allowed.`);
        return;
      }
    }

    try {
      setCommentLoading(true);
      const commentData = {
        text: newComment.trim(),
        authorId
      };
      
      const result = await commentService.create(issueId, commentData);
      const commentId = result?.id;
      
      // If there's an attachment, upload it
      if (commentAttachment && commentId) {
        try {
          await attachmentService.uploadCommentAttachment(commentId, commentAttachment, authorId);
        } catch (uploadErr) {
          console.error('Error uploading comment attachment:', uploadErr);
          // Comment was created but attachment failed - still show comment
        }
      }
      
      setNewComment('');
      setCommentAttachment(null);
      setCommentAttachmentError(null);
      
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

  // Handle Edit Issue button click
  const handleEditClick = () => {
    if (onEditIssue && issue) {
      onEditIssue(issue);
    }
  };

  // Handle Flag as Duplicate button click
  const handleFlagDuplicateClick = async () => {
    if (!isAdminUser) {
      return;
    }

    try {
      let otherIssues = duplicateCandidatesCacheRef.current;
      if (!otherIssues) {
        // Fetch all issues once and keep a local cache for repeated modal openings
        const issues = await issueService.getAll();
        otherIssues = issues.filter((candidateIssue) =>
          candidateIssue.id !== issueId &&
          !['CLOSED', 'RESOLVED'].includes(
            String(candidateIssue?.status ?? '').trim().toUpperCase().replace(/\s+/g, '_')
          )
        );
        duplicateCandidatesCacheRef.current = otherIssues;
      }

      setAllIssues(otherIssues);
      setSelectedOriginalIssue(null);
      setDuplicateSearchQuery('');
      setShowDuplicateModal(true);
    } catch (err) {
      console.error('Error fetching issues:', err);
      setError(err.message || 'Failed to load issues');
    }
  };

  // Handle confirm flag as duplicate
  const handleConfirmDuplicate = async () => {
    if (!selectedOriginalIssue) return;
    if (currentUserId == null) {
      setError('User not authenticated. Please login again.');
      return;
    }
    
    try {
      setFlaggingDuplicate(true);
      await issueService.flagAsDuplicate(issueId, selectedOriginalIssue.id, currentUserId);
      
      // Close modal and show success
      setShowDuplicateModal(false);
      setDuplicateSuccess({
        issueId: issueId,
        issueTitle: issue?.title,
        originalId: selectedOriginalIssue.id
      });
      duplicateCandidatesCacheRef.current = null;
      
      // Refresh issue data to show updated status
      await fetchIssueData();
    } catch (err) {
      console.error('Error flagging as duplicate:', err);
      setError(err.message || 'Failed to flag as duplicate');
    } finally {
      setFlaggingDuplicate(false);
    }
  };

  // Filter issues based on search query
  const filteredIssues = useMemo(() => {
    const normalizedQuery = duplicateSearchQuery.toLowerCase();
    return allIssues.filter((candidateIssue) =>
      candidateIssue.title?.toLowerCase().includes(normalizedQuery) ||
      `#${candidateIssue.id}`.includes(duplicateSearchQuery)
    );
  }, [allIssues, duplicateSearchQuery]);
  const duplicateRowHoverClass = isDarkMode ? 'hover:bg-[#151515]' : 'hover:bg-gray-50';
  const duplicateRowSelectedClass = isDarkMode ? 'bg-[#1f1f1f]' : 'bg-blue-50';

  const getStatusColor = (status) => {
    const colors = {
      'TODO': 'bg-green-200 text-green-900',
      'IN_PROGRESS': 'bg-blue-200 text-blue-900',
      'RESOLVED': 'bg-purple-200 text-purple-900',
      'CLOSED': 'bg-red-200 text-red-900',
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
      'CRITICAL': 'bg-red-700 text-white',
      'HIGH': 'bg-red-200 text-red-900',
      'MEDIUM': 'bg-yellow-200 text-yellow-900',
      'LOW': 'bg-green-200 text-green-900',
    };
    return colors[priority] || 'bg-gray-500 text-white';
  };

  const getTypeLabel = (type) => {
    const labels = {
      BUG: 'Bug',
      FEATURE: 'Feature',
      DOCUMENTATION: 'Documentation',
      QUESTION: 'Question'
    };
    return labels[type] || type || 'Bug';
  };

  const formatTimeAgo = (dateString) => {
    return formatRelativeTime(dateString, nowMs);
  };

  const formatTime = (dateString) => {
    const parsedDate = parseApiDate(dateString);
    if (!parsedDate) return '';
    return parsedDate.toLocaleTimeString('it-IT', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: false
    });
  };

  const formatDateTime = (dateString) => {
    const parsedDate = parseApiDate(dateString);
    if (!parsedDate) return 'N/A';
    return parsedDate.toLocaleString('it-IT', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      hour12: false
    });
  };

  const historyEvents = useMemo(() => {
    const normalizedHistory = (issueHistory || [])
      .map((historyEntry, index) => ({
        id: historyEntry.id ?? `history-${index}`,
        timestamp: historyEntry.timestamp || historyEntry.createdAt || null,
        title: historyEntry.title || 'Issue event',
        description: historyEntry.description || '',
        changedBy: historyEntry.changedBy || 'System'
      }))
      .filter((event) => event.timestamp);

    if (normalizedHistory.length > 0) {
      return normalizedHistory.sort((a, b) => {
        const dateA = parseApiDate(a.timestamp);
        const dateB = parseApiDate(b.timestamp);
        const aMs = dateA ? dateA.getTime() : 0;
        const bMs = dateB ? dateB.getTime() : 0;
        return bMs - aMs;
      });
    }

    // Fallback for legacy issues created before backend history support.
    if (!issue?.createdAt) {
      return [];
    }

    return [
      {
        id: 'legacy-created',
        timestamp: issue.createdAt,
        title: 'Issue created',
        description: `Reported by ${issue.reporterName || 'Unknown'}`,
        changedBy: issue.reporterName || 'Unknown'
      }
    ];
  }, [issue, issueHistory]);

  const statusOptions = ['TODO', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];

  if (loading) {
    return (
      <div className="flex h-screen bg-gray-50">
        <Sidebar currentPage={currentPage} onNavigate={onNavigate} userRole={user?.role} />
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
        <Sidebar currentPage={currentPage} onNavigate={onNavigate} userRole={user?.role} />
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
      <Sidebar currentPage={currentPage} onNavigate={onNavigate} userRole={user?.role} />

      <main className="flex-1 overflow-auto">
        {/* Header */}
        <header className="fixed inset-x-0 top-0 z-40 border-b border-gray-200 bg-white/95 px-8 py-4 pl-20 backdrop-blur">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <button
                onClick={onBack}
                className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
              >
                <ArrowLeft className="w-5 h-5 text-gray-600" />
              </button>
              <div className="relative">
                <h1 className="text-2xl font-bold text-gray-900">
                  Issue #{issue?.id}: {issue?.title}
                </h1>
                {/* DUPLICATE Stamp Overlay on Title */}
                {issue?.originalIssueId && (
                  <div 
                    className="absolute inset-0 flex items-center justify-start pointer-events-none"
                    style={{ marginLeft: '-10px' }}
                  >
                    <span 
                      className="text-red-600 font-bold text-4xl uppercase tracking-wider select-none"
                      style={{ 
                        transform: 'rotate(-12deg)',
                        opacity: 0.4,
                        fontFamily: 'Impact, Haettenschweiler, Arial Narrow Bold, sans-serif',
                        textShadow: '2px 2px 0 rgba(255,255,255,0.5)',
                        letterSpacing: '4px'
                      }}
                    >
                      DUPLICATE
                    </span>
                  </div>
                )}
              </div>
            </div>
            <div className="flex items-center gap-3">
              <ThemeToggle isDarkMode={isDarkMode} onToggle={onToggleTheme} />
              <UserIdentity user={user} />
              <button
                onClick={onLogout}
                className="rounded-lg border border-gray-300 px-3 py-1.5 text-sm text-gray-600 transition-colors hover:bg-gray-50 hover:text-gray-900"
              >
                Logout
              </button>
            </div>
          </div>
        </header>

        {/* Content */}
        <div className="p-8 pt-28">
          <div className="flex gap-8">
            {/* Main Content */}
            <div className="flex-1">
              {/* Action Buttons */}
              <div className="flex items-center gap-3 mb-6">
                <button 
                  onClick={handleEditClick}
                  className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
                >
                  <Edit className="w-4 h-4" />
                  Edit Issue
                </button>
                <button 
                  onClick={handleFlagDuplicateClick}
                  disabled={!isAdminUser}
                  title={!isAdminUser ? 'Only admins can flag duplicates' : 'Flag this issue as duplicate'}
                  className="flex items-center gap-2 rounded-lg border border-gray-300 bg-transparent px-4 py-2 text-gray-700 transition-colors hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  <Flag className="w-4 h-4" />
                  Flag as Duplicate
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
                    <button
                      type="button"
                      onClick={() => openAttachmentPreview(`/api/attachments/issues/${issue.id}`, issue.attachmentPath?.split('/').pop())}
                      className="w-16 h-16 bg-gray-200 rounded flex items-center justify-center overflow-hidden transition-transform hover:scale-[1.02]"
                    >
                      <img
                        src={`/api/attachments/issues/${issue.id}`}
                        alt="Attachment preview"
                        className="w-full h-full object-cover"
                        onError={(e) => {
                          e.target.style.display = 'none';
                          e.target.parentElement.innerHTML = '📎';
                        }}
                      />
                    </button>
                    <div className="flex-1">
                      <button
                        type="button"
                        onClick={() => openAttachmentPreview(`/api/attachments/issues/${issue.id}`, issue.attachmentPath?.split('/').pop())}
                        className="text-sm font-medium text-gray-900 hover:underline"
                      >
                        {issue.attachmentPath.split('/').pop()}
                      </button>
                    </div>
                    <a
                      href={`/api/attachments/issues/${issue.id}`}
                      download
                      className="group p-2 rounded transition-colors hover:bg-gray-200"
                      title="Download attachment"
                    >
                      <Download className="w-5 h-5 text-gray-500 transition-colors group-hover:text-gray-900" />
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
                    comments.map((comment) => {
                      const isOwnComment = comment.authorId === currentUserId;
                      const usesLightBubble = isOwnComment || !isDarkMode;
                      const bubbleClass = isOwnComment
                        ? 'bg-blue-100 text-slate-900'
                        : isDarkMode
                          ? 'bg-gray-800 text-gray-100'
                          : 'bg-gray-100 text-gray-900';
                      const authorTextClass = usesLightBubble ? 'text-slate-900' : 'text-gray-100';
                      const bodyTextClass = usesLightBubble ? 'text-slate-900' : 'text-gray-100';
                      const timeTextClass = usesLightBubble ? 'text-[#1f2937]' : 'text-gray-300';
                      const attachmentPanelClass = usesLightBubble
                        ? 'mt-2 flex items-center gap-2 rounded border border-gray-300 bg-white/80 p-2'
                        : 'mt-2 flex items-center gap-2 rounded border border-gray-600 bg-black/25 p-2';
                      const attachmentThumbClass = usesLightBubble
                        ? 'w-10 h-10 bg-gray-100 rounded overflow-hidden flex-shrink-0 transition-transform hover:scale-[1.03]'
                        : 'w-10 h-10 bg-gray-700 rounded overflow-hidden flex-shrink-0 transition-transform hover:scale-[1.03]';
                      const attachmentNameClass = usesLightBubble
                        ? 'text-xs text-slate-900 truncate flex-1 text-left hover:underline'
                        : 'text-xs text-gray-100 truncate flex-1 text-left hover:underline';
                      const downloadButtonClass = usesLightBubble
                        ? 'group p-1 rounded transition-colors hover:bg-gray-200'
                        : 'group p-1 rounded transition-colors hover:bg-white/10';
                      const downloadIconClass = usesLightBubble
                        ? 'w-4 h-4 text-gray-800 transition-colors group-hover:text-black'
                        : 'w-4 h-4 text-gray-300 transition-colors group-hover:text-white';

                      return (
                      <div
                        key={comment.id}
                        className={`flex gap-3 ${isOwnComment ? 'flex-row-reverse' : ''}`}
                      >
                        <AvatarCircle
                          seed={buildAvatarSeed({
                            username: comment.authorUsername,
                            id: comment.authorId,
                            fallback: 'Unknown user'
                          })}
                          label={comment.authorUsername || 'Unknown'}
                          fallback={comment.authorUsername || 'U'}
                          sizeClass="w-10 h-10"
                          textClass="text-sm"
                        />
                        <div className={`max-w-md px-4 py-3 rounded-lg ${bubbleClass}`}>
                          <div className="flex items-center gap-2 mb-1">
                            <span className={`text-sm font-medium ${authorTextClass}`}>
                              {comment.authorUsername || 'Unknown'}
                            </span>
                          </div>
                          <p className={`text-sm ${bodyTextClass}`}>{comment.text}</p>
                          {/* Comment Attachment */}
                          {comment.attachmentPath && (
                            <div className={attachmentPanelClass}>
                              <button
                                type="button"
                                onClick={() => openAttachmentPreview(`/api/attachments/comments/${comment.id}`, comment.attachmentPath?.split('/').pop())}
                                className={attachmentThumbClass}
                              >
                                <img
                                  src={`/api/attachments/comments/${comment.id}`}
                                  alt="Attachment"
                                  className="w-full h-full object-cover"
                                  onError={(e) => {
                                    e.target.style.display = 'none';
                                    e.target.parentElement.innerHTML = '[file]';
                                  }}
                                />
                              </button>
                              <button
                                type="button"
                                onClick={() => openAttachmentPreview(`/api/attachments/comments/${comment.id}`, comment.attachmentPath?.split('/').pop())}
                                className={attachmentNameClass}
                              >
                                {comment.attachmentPath.split('/').pop()}
                              </button>
                              <a
                                href={`/api/attachments/comments/${comment.id}`}
                                download
                                className={downloadButtonClass}
                                title="Download attachment"
                              >
                                <Download className={downloadIconClass} />
                              </a>
                            </div>
                          )}
                          <span className={`mt-1 block text-xs ${timeTextClass}`}>
                            {formatTime(comment.createdAt)}
                          </span>
                        </div>
                      </div>
                      );
                    })
                  )}
                </div>

                {/* Add Comment Form */}
                <form onSubmit={handleSubmitComment} className="space-y-3">
                  <div className="flex gap-3">
                    <input
                      type="text"
                      value={newComment}
                      onChange={(e) => setNewComment(e.target.value)}
                      placeholder="Add a comment..."
                      className="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                      disabled={commentLoading}
                    />
                    {/* Attachment button */}
                    <label className="p-2 border border-gray-300 rounded-lg hover:bg-gray-50 cursor-pointer transition-colors">
                      <input
                        type="file"
                        onChange={handleCommentAttachment}
                        className="hidden"
                        accept={(attachInfo.allowedExtensions || ['.jpg', '.png']).join(',')}
                        disabled={commentLoading}
                      />
                      <Paperclip className="w-5 h-5 text-gray-500" />
                    </label>
                    <button
                      type="submit"
                      disabled={commentLoading || !newComment.trim() || commentAttachmentError}
                      className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                    >
                      {commentLoading ? 'Sending...' : <Send className="w-5 h-5" />}
                    </button>
                  </div>
                  
                  {/* Selected Attachment Preview */}
                  {commentAttachment && (
                    <div className="flex items-center gap-3 p-3 bg-gray-50 rounded-lg border border-gray-200">
                      <Upload className="w-5 h-5 text-gray-400 flex-shrink-0" />
                      <div className="flex-1 min-w-0">
                        <p className="text-sm text-gray-900 truncate">{commentAttachment.name}</p>
                        <p className="text-xs text-gray-500">{(commentAttachment.size / (1024 * 1024)).toFixed(2)} MB</p>
                      </div>
                      <button
                        type="button"
                        onClick={removeCommentAttachment}
                        className="p-1 hover:bg-gray-200 rounded transition-colors"
                      >
                        <X className="w-4 h-4 text-gray-500" />
                      </button>
                    </div>
                  )}
                  
                  {/* Attachment Error */}
                  {commentAttachmentError && (
                    <div className="flex items-center gap-2 px-3 py-2 bg-red-50 text-red-700 rounded-lg text-sm">
                      <AlertCircle className="w-4 h-4 flex-shrink-0" />
                      <span>{commentAttachmentError}</span>
                    </div>
                  )}
                  
                  {/* Helper text */}
                  <p className="text-xs text-gray-400">
                    PNG, JPG up to {attachInfo.maxFileSizeMB} MB
                  </p>
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
                    <span className="text-sm font-medium text-gray-900">
                      {getTypeLabel(issue?.type)}
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
                      <AvatarCircle
                        seed={buildAvatarSeed({
                          username: issue?.assigneeUsername,
                          id: issue?.assigneeId,
                          fallback: 'Unassigned'
                        })}
                        label={issue?.assigneeUsername || 'Unassigned'}
                        fallback={issue?.assigneeUsername || 'U'}
                        sizeClass="w-6 h-6"
                        textClass="text-xs"
                      />
                      <span className="text-sm font-medium text-gray-900">
                        {issue?.assigneeUsername || 'Unassigned'}
                      </span>
                    </div>
                  </div>

                  {/* Reporter */}
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-500">Reporter</span>
                    <div className="flex items-center gap-2">
                      <AvatarCircle
                        seed={buildAvatarSeed({
                          username: issue?.reporterName,
                          fallback: 'Unknown reporter'
                        })}
                        label={issue?.reporterName || 'Unknown'}
                        fallback={issue?.reporterName || 'U'}
                        sizeClass="w-6 h-6"
                        textClass="text-xs"
                      />
                      <span className="text-sm font-medium text-gray-900">
                        {issue?.reporterName || 'Unknown'}
                      </span>
                    </div>
                  </div>

                  {/* Created */}
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-500">Created</span>
                    <span className="text-sm text-gray-900" title={formatDateTime(issue?.createdAt)}>
                      {formatTimeAgo(issue?.createdAt)}
                    </span>
                  </div>

                  {/* Updated */}
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-500">Updated</span>
                    <span className="text-sm text-gray-900" title={formatDateTime(issue?.updatedAt || issue?.createdAt)}>
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
                  <button
                    type="button"
                    onClick={() => setShowHistoryModal(true)}
                    className="flex w-full items-center justify-between rounded-lg border border-gray-200 bg-gray-50 px-3 py-2.5 text-left transition-colors hover:bg-gray-100"
                  >
                    <span className="text-lg font-semibold text-gray-900">History</span>
                    <span className="rounded-full bg-gray-200 px-2 py-0.5 text-xs font-medium text-gray-700">
                      {historyEvents.length}
                    </span>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Overlay Notifications */}
        {(duplicateSuccess || successMessage || (error && issue)) && (
          <div className="pointer-events-none fixed inset-x-0 top-6 z-50 flex flex-col items-center gap-3 px-4">
            {duplicateSuccess && (
              <div className="pointer-events-auto w-full max-w-3xl animate-toast-down rounded-lg border border-green-300 bg-green-100 px-4 py-3 shadow-lg">
                <div className="flex items-center gap-3">
                  <CheckCircle className="h-5 w-5 text-green-600" />
                  <span className="flex-1 text-green-800 font-medium">
                    Issue #{duplicateSuccess.issueId} '{duplicateSuccess.issueTitle}' flagged as duplicate of #{duplicateSuccess.originalId} and closed successfully!
                  </span>
                  <button
                    onClick={() => setDuplicateSuccess(null)}
                    className="text-green-600 hover:text-green-800"
                  >
                    <X className="h-5 w-5" />
                  </button>
                </div>
              </div>
            )}

            {successMessage && (
              <div className="pointer-events-auto w-full max-w-3xl animate-toast-down rounded-lg border border-green-300 bg-green-100 px-4 py-3 shadow-lg">
                <div className="flex items-center gap-3">
                  <CheckCircle className="h-5 w-5 text-green-600" />
                  <span className="flex-1 text-green-800 font-medium">{successMessage}</span>
                  <button
                    onClick={onDismissSuccess}
                    className="text-green-600 hover:text-green-800"
                  >
                    <X className="h-5 w-5" />
                  </button>
                </div>
              </div>
            )}

            {error && issue && (
              <div className="pointer-events-auto w-full max-w-3xl animate-toast-down rounded-lg border border-red-300 bg-red-100 px-4 py-3 shadow-lg">
                <div className="flex items-center gap-3">
                  <div className="flex h-6 w-6 flex-shrink-0 items-center justify-center rounded-full bg-red-500">
                    <X className="h-4 w-4 text-white" />
                  </div>
                  <span className="flex-1 text-red-800 font-medium">{error}</span>
                  <button
                    onClick={() => setError(null)}
                    className="text-red-600 hover:text-red-800"
                  >
                    <X className="h-5 w-5" />
                  </button>
                </div>
              </div>
            )}
          </div>
        )}

        {showHistoryModal && (
          <div
            className="fixed inset-0 z-[58] flex items-center justify-center bg-black/55 px-4 backdrop-blur-sm"
            onClick={() => setShowHistoryModal(false)}
          >
            <div
              className="flex w-[min(92vw,760px)] max-h-[calc(100vh-2.5rem)] flex-col overflow-hidden rounded-xl border border-gray-200 bg-white shadow-2xl"
              onClick={(event) => event.stopPropagation()}
            >
              <div className="flex items-center justify-between border-b border-gray-200 px-4 py-3">
                <div>
                  <h3 className="text-lg font-semibold text-gray-900">Issue History</h3>
                  <p className="text-xs text-gray-500">{historyEvents.length} event{historyEvents.length === 1 ? '' : 's'}</p>
                </div>
                <button
                  type="button"
                  onClick={() => setShowHistoryModal(false)}
                  className="rounded-lg p-1 text-gray-500 transition-colors hover:bg-gray-100 hover:text-gray-700"
                >
                  <X className="h-5 w-5" />
                </button>
              </div>
              <div className="stable-scroll max-h-[min(68vh,640px)] overflow-y-auto px-5 py-4">
                {historyEvents.length === 0 ? (
                  <p className="text-sm text-gray-500">No history events yet.</p>
                ) : (
                  <div className="space-y-4">
                    {historyEvents.map((event, index) => (
                      <div key={`${event.id}-${event.timestamp}-${index}`} className="relative pl-6">
                        <span className="absolute left-0 top-1.5 h-2.5 w-2.5 rounded-full bg-blue-500" />
                        {index < historyEvents.length - 1 && (
                          <span className="absolute left-[4px] top-4 h-[calc(100%+8px)] w-px bg-gray-200" />
                        )}
                        <p className="text-sm font-medium text-gray-900">{event.title}</p>
                        <p className="text-xs text-gray-500">{formatDateTime(event.timestamp)}</p>
                        <p className="text-[11px] font-medium text-gray-500">By: {event.changedBy || 'System'}</p>
                        {event.description && (
                          <p className="mt-1 text-xs leading-relaxed text-gray-600">{event.description}</p>
                        )}
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          </div>
        )}

        {previewAttachment && (
          <div
            className="fixed inset-0 z-[60] flex items-center justify-center bg-black/70 px-4 backdrop-blur-sm"
            onClick={() => setPreviewAttachment(null)}
          >
            <div
              className="flex h-[min(80vh,560px)] w-[min(88vw,820px)] max-h-[calc(100vh-2.5rem)] flex-col rounded-xl border border-gray-200 bg-white shadow-2xl"
              onClick={(event) => event.stopPropagation()}
            >
              <div className="flex items-center justify-between border-b border-gray-200 px-4 py-3">
                <p className="truncate text-sm font-medium text-gray-900">{previewAttachment.name}</p>
                <button
                  type="button"
                  onClick={() => setPreviewAttachment(null)}
                  className="rounded-lg p-1 text-gray-500 transition-colors hover:bg-gray-100 hover:text-gray-700"
                >
                  <X className="h-5 w-5" />
                </button>
              </div>
              <div className="flex min-h-0 flex-1 bg-black p-3">
                <div className="flex h-full w-full items-center justify-center overflow-hidden rounded-md bg-black">
                  <img
                    src={previewAttachment.source}
                    alt={previewAttachment.name}
                    className="h-full w-full object-contain"
                  />
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Flag as Duplicate Modal */}
        {showDuplicateModal && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/45 backdrop-blur-sm">
            <div className="bg-white rounded-lg shadow-xl w-full max-w-lg mx-4">
              {/* Modal Header */}
              <div className="px-6 py-4 border-b border-gray-200">
                <h2 className="text-xl font-semibold text-gray-900">
                  Flag Issue #{issueId} as Duplicate
                </h2>
              </div>

              {/* Modal Body */}
              <div className="px-6 py-4">
                {/* Search Input */}
                <div className="relative mb-4">
                  <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
                  <input
                    type="text"
                    placeholder="Search for original issue..."
                    value={duplicateSearchQuery}
                    onChange={(e) => setDuplicateSearchQuery(e.target.value)}
                    className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>

                {/* Issues List */}
                <div className="max-h-64 overflow-y-auto border border-gray-200 rounded-lg">
                  {filteredIssues.length === 0 ? (
                    <div className="p-8 text-center text-gray-500">
                      No issues found.
                    </div>
                  ) : (
                    filteredIssues.map((otherIssue) => (
                      <button
                        key={otherIssue.id}
                        onClick={() => setSelectedOriginalIssue(otherIssue)}
                        className={`w-full text-left px-4 py-3 border-b border-gray-100 last:border-b-0 transition-colors ${duplicateRowHoverClass} ${
                          selectedOriginalIssue?.id === otherIssue.id ? duplicateRowSelectedClass : ''
                        }`}
                      >
                        <span className="font-medium text-gray-900">
                          Issue #{otherIssue.id}: {otherIssue.title}
                        </span>
                      </button>
                    ))
                  )}
                </div>
              </div>

              {/* Modal Footer */}
              <div className="px-6 py-4 border-t border-gray-200 flex justify-end gap-3">
                <button
                  onClick={() => {
                    setShowDuplicateModal(false);
                    setSelectedOriginalIssue(null);
                  }}
                  className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
                >
                  Cancel
                </button>
                <button
                  onClick={handleConfirmDuplicate}
                  disabled={!selectedOriginalIssue || flaggingDuplicate}
                  className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                >
                  {flaggingDuplicate ? 'Processing...' : 'Flag & Close Issue'}
                </button>
              </div>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}

export default IssueDetail;
