import { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { Search, Plus, CheckCircle, X, ChevronDown, Check, Loader2 } from 'lucide-react';
import Sidebar from '../components/Sidebar';
import ThemeToggle from '../components/ThemeToggle';
import UserIdentity from '../components/UserIdentity';
import { issueService } from '../services/issue.service';
import { authService } from '../services/auth.service';

const API_TIMESTAMP_WITH_TIMEZONE_REGEX = /(Z|[+-]\d{2}:?\d{2})$/i;
const DICEBEAR_BASE_URL = 'https://api.dicebear.com/9.x/glass/svg';
const BADGE_BASE_CLASS = 'inline-flex rounded-full px-2.5 py-1 text-xs font-medium';

const STATUS_BADGE_CLASSES = {
  TODO: 'bg-green-200 text-green-900',
  OPEN: 'bg-green-200 text-green-900',
  IN_PROGRESS: 'bg-blue-200 text-blue-900',
  RESOLVED: 'bg-purple-200 text-purple-900',
  CLOSED: 'bg-red-200 text-red-900',
};

const PRIORITY_BADGE_CLASSES = {
  CRITICAL: 'bg-red-700 text-white',
  HIGH: 'bg-red-200 text-red-900',
  MEDIUM: 'bg-yellow-200 text-yellow-900',
  LOW: 'bg-green-200 text-green-900',
};

const TYPE_BADGE_CLASSES = {
  BUG: 'bg-rose-200 text-rose-900',
  FEATURE: 'bg-emerald-200 text-emerald-900',
  DOCUMENTATION: 'bg-sky-200 text-sky-900',
  QUESTION: 'bg-amber-200 text-amber-900',
};

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

const formatIssuePublicationDate = (createdAt) => {
  const parsedDate = parseApiDate(createdAt);
  if (!parsedDate) {
    return 'N/A';
  }

  return parsedDate.toLocaleDateString('en-US', {
    day: '2-digit',
    month: 'short',
    year: 'numeric'
  });
};

const getUserDisplayName = (targetUser) => targetUser?.username || targetUser?.email || 'Unknown user';

const getUserAvatarUrl = (targetUser) => {
  const seed = encodeURIComponent(`${getUserDisplayName(targetUser)}-${targetUser?.id ?? 'unknown'}`);
  return `${DICEBEAR_BASE_URL}?seed=${seed}&radius=50&size=64`;
};

const areStringArraysEqual = (first = [], second = []) => {
  if (first.length !== second.length) {
    return false;
  }

  return first.every((value, index) => value === second[index]);
};

const STATUS_FILTER_OPTIONS = [
  { value: 'all', label: 'Status' },
  { value: 'TODO', label: 'Open', badgeClass: STATUS_BADGE_CLASSES.TODO },
  { value: 'IN_PROGRESS', label: 'In Progress', badgeClass: STATUS_BADGE_CLASSES.IN_PROGRESS },
  { value: 'RESOLVED', label: 'Resolved', badgeClass: STATUS_BADGE_CLASSES.RESOLVED },
  { value: 'CLOSED', label: 'Closed', badgeClass: STATUS_BADGE_CLASSES.CLOSED },
];

const PRIORITY_FILTER_OPTIONS = [
  { value: 'all', label: 'Priority' },
  { value: 'CRITICAL', label: 'Critical', badgeClass: PRIORITY_BADGE_CLASSES.CRITICAL },
  { value: 'HIGH', label: 'High', badgeClass: PRIORITY_BADGE_CLASSES.HIGH },
  { value: 'MEDIUM', label: 'Medium', badgeClass: PRIORITY_BADGE_CLASSES.MEDIUM },
  { value: 'LOW', label: 'Low', badgeClass: PRIORITY_BADGE_CLASSES.LOW },
];

const TYPE_FILTER_OPTIONS = [
  { value: 'all', label: 'Type' },
  { value: 'BUG', label: 'Bug', badgeClass: TYPE_BADGE_CLASSES.BUG },
  { value: 'FEATURE', label: 'Feature', badgeClass: TYPE_BADGE_CLASSES.FEATURE },
  { value: 'DOCUMENTATION', label: 'Documentation', badgeClass: TYPE_BADGE_CLASSES.DOCUMENTATION },
  { value: 'QUESTION', label: 'Question', badgeClass: TYPE_BADGE_CLASSES.QUESTION },
];

function FilterDropdown({ value, options, onChange, disabled = false, loading = false }) {
  const [open, setOpen] = useState(false);
  const rootRef = useRef(null);
  const selectedOption = options.find((option) => option.value === value) || options[0];

  useEffect(() => {
    if (!open) {
      return undefined;
    }

    const handleOutsideClick = (event) => {
      if (rootRef.current && !rootRef.current.contains(event.target)) {
        setOpen(false);
      }
    };

    document.addEventListener('mousedown', handleOutsideClick);
    return () => {
      document.removeEventListener('mousedown', handleOutsideClick);
    };
  }, [open]);

  return (
    <div ref={rootRef} className="relative min-w-0 w-full">
      <button
        type="button"
        onClick={() => !disabled && setOpen((prev) => !prev)}
        disabled={disabled}
        className="inline-flex h-11 w-full items-center justify-between gap-2 rounded-xl border border-gray-300 bg-white px-3 text-sm font-medium text-gray-700 shadow-sm transition-colors hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-60"
      >
        {selectedOption?.badgeClass ? (
          <span className={`${BADGE_BASE_CLASS} max-w-[120px] truncate ${selectedOption.badgeClass}`}>
            {selectedOption.label}
          </span>
        ) : (
          <span className="truncate">{selectedOption?.label || 'Select'}</span>
        )}
        {loading ? (
          <Loader2 className="h-4 w-4 animate-spin text-gray-500" />
        ) : (
          <ChevronDown className={`h-4 w-4 text-gray-500 transition-transform ${open ? 'rotate-180' : ''}`} />
        )}
      </button>

      {open && !disabled && (
        <div className="absolute left-0 top-[calc(100%+8px)] z-30 w-full rounded-xl border border-gray-200 bg-white p-1.5 shadow-xl">
          {options.map((option) => (
            <button
              key={option.value}
              type="button"
              onClick={() => {
                onChange(option.value);
                setOpen(false);
              }}
              className="flex w-full items-center justify-between gap-2 rounded-lg px-3 py-2 text-left text-sm text-gray-700 transition-colors hover:bg-gray-100"
            >
              {option.badgeClass ? (
                <span className={`${BADGE_BASE_CLASS} ${option.badgeClass}`}>{option.label}</span>
              ) : (
                <span>{option.label}</span>
              )}
              {value === option.value && <Check className="h-4 w-4 text-blue-600" />}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

function Issues({
  user,
  onLogout,
  onCreateIssue,
  onIssueClick,
  onNavigate,
  successMessage,
  onDismissSuccess,
  isDarkMode,
  onToggleTheme
}) {
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');
  const [priorityFilter, setPriorityFilter] = useState('all');
  const [typeFilter, setTypeFilter] = useState('all');
  const [assigneeFilters, setAssigneeFilters] = useState([]);
  const [debouncedFilters, setDebouncedFilters] = useState({
    term: '',
    status: 'all',
    priority: 'all',
    type: 'all',
    assigneeIds: []
  });
  const [assignableUsers, setAssignableUsers] = useState([]);
  const [assignableUsersLoading, setAssignableUsersLoading] = useState(false);
  const [assignableUsersError, setAssignableUsersError] = useState(null);
  const [showAssigneeModal, setShowAssigneeModal] = useState(false);
  const [assigneeSearchQuery, setAssigneeSearchQuery] = useState('');
  const [pendingAssigneeFilters, setPendingAssigneeFilters] = useState([]);
  const [issues, setIssues] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const lastSearchKeyRef = useRef(null);
  const currentPage = 'issues';
  const normalizedRole = String(user?.role ?? '').trim().toUpperCase();
  const isStakeholderUser = normalizedRole === 'STAKEHOLDER';

  useEffect(() => {
    const timer = setTimeout(() => {
      const nextFilters = {
        term: searchQuery.trim(),
        status: statusFilter,
        priority: priorityFilter,
        type: typeFilter,
        assigneeIds: [...assigneeFilters]
      };
      setDebouncedFilters((prevFilters) => {
        const isUnchanged =
          prevFilters.term === nextFilters.term &&
          prevFilters.status === nextFilters.status &&
          prevFilters.priority === nextFilters.priority &&
          prevFilters.type === nextFilters.type &&
          areStringArraysEqual(prevFilters.assigneeIds, nextFilters.assigneeIds);

        return isUnchanged ? prevFilters : nextFilters;
      });
    }, 300);

    return () => clearTimeout(timer);
  }, [searchQuery, statusFilter, priorityFilter, typeFilter, assigneeFilters]);

  useEffect(() => {
    let mounted = true;

    (async () => {
      try {
        setAssignableUsersLoading(true);
        setAssignableUsersError(null);
        const users = await authService.getAssignableUsers(user?.id);
        if (!mounted) {
          return;
        }
        setAssignableUsers(Array.isArray(users) ? users : []);
      } catch (err) {
        console.warn('Unable to load assignable users for filters', err);
        if (mounted) {
          setAssignableUsers([]);
          setAssignableUsersError(err.message || 'Failed to load assignee options.');
        }
      } finally {
        if (mounted) {
          setAssignableUsersLoading(false);
        }
      }
    })();

    return () => {
      mounted = false;
    };
  }, [user?.id]);

  const buildSearchParams = useCallback(() => {
    const params = {};

    if (debouncedFilters.term) {
      params.term = debouncedFilters.term;
    }

    if (debouncedFilters.status !== 'all') {
      params.status = debouncedFilters.status;
    }

    if (debouncedFilters.priority !== 'all') {
      params.priority = debouncedFilters.priority;
    }

    if (debouncedFilters.type !== 'all') {
      params.type = debouncedFilters.type;
    }

    if (debouncedFilters.assigneeIds.length === 1) {
      params.assigneeId = debouncedFilters.assigneeIds[0];
    }

    return params;
  }, [debouncedFilters]);

  const fetchIssues = useCallback(async (forceRefresh = false) => {
    const searchParams = buildSearchParams();
    const searchKey = JSON.stringify({
      server: searchParams,
      selectedAssigneeIds: debouncedFilters.assigneeIds
    });
    if (!forceRefresh && lastSearchKeyRef.current === searchKey) {
      return;
    }

    try {
      lastSearchKeyRef.current = searchKey;
      setLoading(true);
      setError(null);
      const data = await issueService.search(searchParams);
      let filteredIssues = Array.isArray(data) ? data : [];

      if (debouncedFilters.assigneeIds.length > 1) {
        const selectedIds = new Set(debouncedFilters.assigneeIds.map((id) => String(id)));
        const selectedUsernames = new Set(
          assignableUsers
            .filter((assignableUser) => selectedIds.has(String(assignableUser.id)))
            .map((assignableUser) => assignableUser.username)
            .filter(Boolean)
        );

        filteredIssues = filteredIssues.filter((issue) => {
          const issueAssigneeId = issue?.assigneeId != null ? String(issue.assigneeId) : null;
          if (issueAssigneeId && selectedIds.has(issueAssigneeId)) {
            return true;
          }

          return issue?.assigneeUsername ? selectedUsernames.has(issue.assigneeUsername) : false;
        });
      }

      setIssues(filteredIssues);
    } catch (err) {
      lastSearchKeyRef.current = null;
      console.error('Error fetching issues:', err);
      setError(err.message || 'Failed to load issues.');
    } finally {
      setLoading(false);
    }
  }, [assignableUsers, buildSearchParams, debouncedFilters.assigneeIds]);

  useEffect(() => {
    fetchIssues(false);
  }, [fetchIssues]);

  useEffect(() => {
    if (!showAssigneeModal) {
      return undefined;
    }

    const handleEscapeKey = (event) => {
      if (event.key === 'Escape') {
        setShowAssigneeModal(false);
      }
    };

    document.addEventListener('keydown', handleEscapeKey);
    return () => {
      document.removeEventListener('keydown', handleEscapeKey);
    };
  }, [showAssigneeModal]);

  useEffect(() => {
    if (showAssigneeModal) {
      setPendingAssigneeFilters([...assigneeFilters]);
      setAssigneeSearchQuery('');
    }
  }, [showAssigneeModal, assigneeFilters]);

  const handleSidebarNavigate = (page) => {
    if (onNavigate) {
      onNavigate(page);
    }
  };

  const getStatusBadgeClass = (status) => {
    const normalizedStatus = String(status || '').trim().toUpperCase().replace(' ', '_');
    return STATUS_BADGE_CLASSES[normalizedStatus] || 'bg-gray-100 text-gray-700';
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
    const normalizedPriority = String(priority || '').trim().toUpperCase();
    return PRIORITY_BADGE_CLASSES[normalizedPriority] || 'bg-gray-100 text-gray-700';
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

  const getTypeLabel = (type) => {
    const labels = {
      BUG: 'Bug',
      FEATURE: 'Feature',
      DOCUMENTATION: 'Documentation',
      QUESTION: 'Question'
    };
    return labels[type] || type || 'Bug';
  };

  const getTypeBadgeClass = (type) => {
    const normalizedType = String(type || '').trim().toUpperCase();
    return TYPE_BADGE_CLASSES[normalizedType] || 'bg-gray-100 text-gray-700';
  };

  const selectedAssigneeUsers = assignableUsers.filter((assignableUser) =>
    assigneeFilters.includes(String(assignableUser.id))
  );
  const selectedAssigneeSummaryLabel =
    selectedAssigneeUsers.length === 0
      ? 'Assignee'
      : selectedAssigneeUsers.length === 1
        ? getUserDisplayName(selectedAssigneeUsers[0])
        : `${selectedAssigneeUsers.length} assignees selected`;
  const filteredAssigneeUsers = useMemo(() => {
    const normalizedQuery = assigneeSearchQuery.trim().toLowerCase();
    if (!normalizedQuery) {
      return assignableUsers;
    }

    return assignableUsers.filter((assignableUser) => {
      const username = (assignableUser.username || '').toLowerCase();
      const email = (assignableUser.email || '').toLowerCase();
      return username.includes(normalizedQuery) || email.includes(normalizedQuery);
    });
  }, [assignableUsers, assigneeSearchQuery]);
  const assigneeRowHoverClass = isDarkMode ? 'hover:bg-[#151515]' : 'hover:bg-gray-50';
  const assigneeRowSelectedClass = isDarkMode ? 'bg-[#1f1f1f]' : 'bg-blue-50';
  const assigneeSecondaryTextClass = isDarkMode ? 'text-gray-300' : 'text-gray-500';

  return (
    <div className="flex h-screen bg-gray-50">
      <Sidebar currentPage={currentPage} onNavigate={handleSidebarNavigate} userRole={user?.role} />

      <main className="flex-1 overflow-auto">
        {/* Header */}
        <header className="fixed inset-x-0 top-0 z-40 border-b border-gray-200 bg-white/95 px-8 py-4 pl-20 backdrop-blur">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">All Issues</h1>
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
          {/* Filters and Search */}
          <div className="mb-4">
            <div className="grid grid-cols-[minmax(0,2.4fr)_minmax(0,1fr)_minmax(0,1fr)_minmax(0,1fr)_minmax(0,1.5fr)_auto] items-center gap-3">
              <div className="min-w-0">
                <div className="relative">
                  <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={20} />
                  <input
                    type="text"
                    placeholder="Search issues..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="h-11 w-full rounded-xl border border-gray-300 bg-white pl-10 pr-4 text-sm shadow-sm transition-colors focus:border-transparent focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>
              </div>

              <FilterDropdown
                value={statusFilter}
                options={STATUS_FILTER_OPTIONS}
                onChange={setStatusFilter}
              />
              <FilterDropdown
                value={priorityFilter}
                options={PRIORITY_FILTER_OPTIONS}
                onChange={setPriorityFilter}
              />
              <FilterDropdown
                value={typeFilter}
                options={TYPE_FILTER_OPTIONS}
                onChange={setTypeFilter}
              />

              <button
                type="button"
                onClick={() => setShowAssigneeModal(true)}
                disabled={assignableUsersLoading}
                className="inline-flex h-11 min-w-0 w-full items-center gap-2 rounded-xl border border-gray-300 bg-white px-3 text-sm font-medium text-gray-700 shadow-sm transition-colors hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-60"
              >
                <div className="flex min-w-0 items-center gap-2">
                  {selectedAssigneeUsers.length === 1 ? (
                    <div className="h-6 w-6 overflow-hidden rounded-full border border-gray-200 bg-gray-100">
                      <img
                        src={getUserAvatarUrl(selectedAssigneeUsers[0])}
                        alt={`${getUserDisplayName(selectedAssigneeUsers[0])} avatar`}
                        className="h-full w-full object-cover"
                        loading="lazy"
                        referrerPolicy="no-referrer"
                      />
                    </div>
                  ) : selectedAssigneeUsers.length > 1 ? (
                    <div className="flex h-6 min-w-6 items-center justify-center rounded-full bg-blue-100 px-1.5 text-[11px] font-semibold text-blue-800">
                      {selectedAssigneeUsers.length}
                    </div>
                  ) : (
                    <div className="flex h-6 w-6 items-center justify-center rounded-full border border-gray-200 bg-gray-100 text-[11px] font-semibold text-gray-500">
                      All
                    </div>
                  )}
                  <span className="truncate">{selectedAssigneeSummaryLabel}</span>
                </div>
                {assignableUsersLoading && <Loader2 className="h-4 w-4 animate-spin text-gray-500" />}
              </button>

              {!isStakeholderUser ? (
                <button
                  onClick={onCreateIssue}
                  className="inline-flex h-11 items-center justify-center gap-2 whitespace-nowrap rounded-xl bg-blue-600 px-4 font-medium text-white transition-colors hover:bg-blue-700 xl:px-6"
                >
                  <Plus size={20} />
                  <span>New Issue</span>
                </button>
              ) : (
                <span className="inline-flex h-11 items-center rounded-xl border border-gray-300 bg-gray-100 px-4 text-sm font-medium text-gray-600">
                  Read-only stakeholder
                </span>
              )}
            </div>
          </div>

          <div className="mb-2 min-h-[20px]">
            {loading && (
              <div className="inline-flex items-center gap-2 text-xs text-gray-500">
                <Loader2 className="h-3.5 w-3.5 animate-spin" />
                Updating issues...
              </div>
            )}
          </div>

          {assignableUsersError && (
            <p className="mb-4 text-sm text-red-600">{assignableUsersError}</p>
          )}

          {showAssigneeModal && (
            <div
              className="fixed inset-0 z-50 flex items-center justify-center bg-black/45 px-4 backdrop-blur-sm"
              onClick={() => setShowAssigneeModal(false)}
            >
              <div
                className="w-full max-w-lg rounded-2xl border border-gray-200 bg-white shadow-xl"
                onClick={(event) => event.stopPropagation()}
              >
                <div className="flex items-center justify-between border-b border-gray-200 px-5 py-4">
                  <h2 className="text-lg font-semibold text-gray-900">Select assignees</h2>
                  <button
                    type="button"
                    onClick={() => setShowAssigneeModal(false)}
                    className="rounded-lg p-1 text-gray-500 transition-colors hover:bg-gray-100 hover:text-gray-700"
                  >
                    <X className="h-5 w-5" />
                  </button>
                </div>

                <div className="px-5 py-4">
                  <div className="relative mb-3">
                    <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                    <input
                      type="text"
                      value={assigneeSearchQuery}
                      onChange={(event) => setAssigneeSearchQuery(event.target.value)}
                      placeholder="Search users..."
                      className="w-full rounded-xl border border-gray-300 bg-white py-2.5 pl-9 pr-3 text-sm text-gray-700 focus:border-transparent focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                  </div>

                  <div className="stable-scroll max-h-80 overflow-y-auto rounded-xl border border-gray-200">
                    <button
                      type="button"
                      onClick={() => setPendingAssigneeFilters([])}
                      className={`flex w-full items-center justify-between px-4 py-3 text-left transition-colors ${assigneeRowHoverClass} ${
                        pendingAssigneeFilters.length === 0 ? assigneeRowSelectedClass : ''
                      }`}
                    >
                      <div className="flex items-center gap-3">
                        <div className="flex h-8 w-8 items-center justify-center rounded-full border border-gray-200 bg-gray-100 text-xs font-semibold text-gray-600">
                          All
                        </div>
                        <div>
                          <p className="text-sm font-medium text-gray-900">All assignees</p>
                          <p className={`text-xs ${assigneeSecondaryTextClass}`}>No assignee filter applied</p>
                        </div>
                      </div>
                      {pendingAssigneeFilters.length === 0 && <Check className="h-4 w-4 text-blue-600" />}
                    </button>

                    {filteredAssigneeUsers.length === 0 ? (
                      <div className="px-4 py-6 text-center text-sm text-gray-500">No users found.</div>
                    ) : (
                      filteredAssigneeUsers.map((assignableUser) => {
                        const optionValue = String(assignableUser.id);
                        const isSelected = pendingAssigneeFilters.includes(optionValue);
                        const displayName = getUserDisplayName(assignableUser);

                        return (
                          <button
                            key={assignableUser.id}
                            type="button"
                            onClick={() => {
                              setPendingAssigneeFilters((currentFilters) =>
                                currentFilters.includes(optionValue)
                                  ? currentFilters.filter((id) => id !== optionValue)
                                  : [...currentFilters, optionValue]
                              );
                            }}
                            className={`flex w-full items-center justify-between border-t border-gray-100 px-4 py-3 text-left transition-colors ${assigneeRowHoverClass} ${
                              isSelected ? assigneeRowSelectedClass : ''
                            }`}
                          >
                            <div className="flex min-w-0 items-center gap-3">
                              <div className="h-8 w-8 overflow-hidden rounded-full border border-gray-200 bg-gray-100">
                                <img
                                  src={getUserAvatarUrl(assignableUser)}
                                  alt={`${displayName} avatar`}
                                  className="h-full w-full object-cover"
                                  loading="lazy"
                                  referrerPolicy="no-referrer"
                                />
                              </div>
                              <div className="min-w-0">
                                <p className="truncate text-sm font-medium text-gray-900">{displayName}</p>
                                {assignableUser.username && assignableUser.email && (
                                  <p className={`truncate text-xs ${assigneeSecondaryTextClass}`}>{assignableUser.email}</p>
                                )}
                              </div>
                            </div>
                            {isSelected && <Check className="h-4 w-4 text-blue-600" />}
                          </button>
                        );
                      })
                    )}
                  </div>
                </div>

                <div className="flex items-center justify-between gap-3 border-t border-gray-200 px-5 py-3">
                  <button
                    type="button"
                    onClick={() => setPendingAssigneeFilters([])}
                    className="rounded-lg border border-gray-300 px-4 py-2 text-sm text-gray-700 transition-colors hover:bg-gray-50"
                  >
                    Clear
                  </button>
                  <div className="flex items-center gap-3">
                    <button
                      type="button"
                      onClick={() => setShowAssigneeModal(false)}
                      className="rounded-lg border border-gray-300 px-4 py-2 text-sm text-gray-700 transition-colors hover:bg-gray-50"
                    >
                      Cancel
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        const normalizedSelection = assignableUsers
                          .map((assignableUser) => String(assignableUser.id))
                          .filter((id) => pendingAssigneeFilters.includes(id));
                        setAssigneeFilters(normalizedSelection);
                        setShowAssigneeModal(false);
                      }}
                      className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-blue-700"
                    >
                      Apply
                    </button>
                  </div>
                </div>
              </div>
            </div>
          )}

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
                    onClick={() => fetchIssues(true)}
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
                      Type
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Assignee
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Published
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {issues.map((issue) => {
                    const isClosedIssue = issue.status === 'CLOSED';
                    const isResolvedIssue = issue.status === 'RESOLVED';
                    const isCompletedIssue = isClosedIssue || isResolvedIssue;
                    const rowClassName = isClosedIssue
                      ? 'issue-row-closed'
                      : isResolvedIssue
                        ? 'issue-row-resolved'
                        : 'issue-row-default';

                    return (
                      <tr 
                        key={issue.id} 
                        className={`cursor-pointer transition-colors ${rowClassName}`}
                        onClick={() => onIssueClick && onIssueClick(issue.id)}
                      >
                        <td className={`px-6 py-4 whitespace-nowrap border-l-4 text-sm font-medium ${
                          isCompletedIssue ? 'border-l-slate-700 text-slate-700' : 'border-l-transparent text-gray-900'
                        }`}>
                          #{issue.id}
                        </td>
                        <td className={`px-6 py-4 text-sm ${
                          isCompletedIssue ? 'text-slate-700 line-through' : 'text-gray-900'
                        }`}>
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
                        <td className="px-6 py-4 whitespace-nowrap">
                          <span className={`${BADGE_BASE_CLASS} ${getTypeBadgeClass(issue.type)}`}>
                            {getTypeLabel(issue.type)}
                          </span>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                          {issue.assigneeUsername || 'Unassigned'}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                          {formatIssuePublicationDate(issue.createdAt)}
                        </td>
                      </tr>
                    );
                  })}
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
