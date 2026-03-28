import { useCallback, useEffect, useMemo, useState } from 'react';
import { Activity, CheckCircle2, CircleSlash, Clock3, FolderOpen } from 'lucide-react';
import Sidebar from '../components/Sidebar';
import ThemeToggle from '../components/ThemeToggle';
import UserIdentity from '../components/UserIdentity';
import { issueService } from '../services/issue.service';

const STATUS_COLORS = {
  TODO: '#22c55e',
  IN_PROGRESS: '#3b82f6',
  RESOLVED: '#a855f7',
  CLOSED: '#6b7280'
};

const PRIORITY_COLORS = {
  CRITICAL: '#dc2626',
  HIGH: '#ef4444',
  MEDIUM: '#eab308',
  LOW: '#22c55e'
};

const formatLabel = (value) =>
  value
    .toLowerCase()
    .split('_')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');

const formatAverageResolutionTime = (hours) => {
  if (typeof hours !== 'number' || Number.isNaN(hours)) {
    return 'N/A';
  }

  return `${hours.toFixed(1)} h`;
};

const formatHoursLabel = (hours) => {
  const numericHours = Number(hours);
  if (Number.isNaN(numericHours)) {
    return '0.0 h';
  }
  return `${numericHours.toFixed(1)} h`;
};

function MetricCard({ title, value, icon: Icon, accentClass }) {
  return (
    <div className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-sm font-medium text-gray-500">{title}</p>
          <p className="mt-2 text-2xl font-bold text-gray-900">{value}</p>
        </div>
        <div className={`rounded-lg p-2 ${accentClass}`}>
          <Icon className="h-5 w-5 text-white" />
        </div>
      </div>
    </div>
  );
}

function DistributionList({ title, data, colorsMap }) {
  const entries = useMemo(() => Object.entries(data || {}), [data]);
  const maxValue = useMemo(() => {
    if (entries.length === 0) {
      return 1;
    }
    return Math.max(...entries.map(([_, count]) => Number(count) || 0), 1);
  }, [entries]);

  return (
    <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
      <h2 className="text-lg font-semibold text-gray-900">{title}</h2>
      <div className="mt-4 space-y-4">
        {entries.length === 0 ? (
          <p className="text-sm text-gray-500">No data available.</p>
        ) : (
          entries.map(([key, value]) => {
            const numericValue = Number(value) || 0;
            const width = `${Math.round((numericValue / maxValue) * 100)}%`;
            const barColor = colorsMap[key] || '#9ca3af';

            return (
              <div key={key}>
                <div className="mb-1 flex items-center justify-between text-sm">
                  <span className="font-medium text-gray-700">{formatLabel(key)}</span>
                  <span className="text-gray-600">{numericValue}</span>
                </div>
                <div className="h-2 w-full rounded-full bg-gray-100">
                  <div
                    className="h-2 rounded-full"
                    style={{ width, backgroundColor: barColor }}
                  />
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}

function PieChartCard({ title, data, colorsMap }) {
  const entries = useMemo(
    () =>
      Object.entries(data || {}).map(([key, value]) => ({
        key,
        value: Number(value) || 0,
        color: colorsMap[key] || '#9ca3af'
      })),
    [colorsMap, data]
  );

  const total = useMemo(
    () => entries.reduce((sum, entry) => sum + entry.value, 0),
    [entries]
  );

  const radius = 44;
  const circumference = 2 * Math.PI * radius;
  let cumulativeRatio = 0;

  return (
    <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
      <h2 className="text-lg font-semibold text-gray-900">{title}</h2>
      {entries.length === 0 ? (
        <p className="mt-4 text-sm text-gray-500">No data available.</p>
      ) : (
        <div className="mt-4 grid grid-cols-1 gap-5 md:grid-cols-[170px,1fr] md:items-center">
          <div className="mx-auto flex w-[150px] items-center justify-center">
            <svg viewBox="0 0 120 120" className="h-[150px] w-[150px]">
              <circle cx="60" cy="60" r={radius} fill="none" stroke="#e5e7eb" strokeWidth="20" />
              {total > 0 &&
                entries.map((entry) => {
                  const sliceRatio = entry.value / total;
                  const dashLength = sliceRatio * circumference;
                  const dashOffset = -cumulativeRatio * circumference;
                  cumulativeRatio += sliceRatio;

                  return (
                    <circle
                      key={entry.key}
                      cx="60"
                      cy="60"
                      r={radius}
                      fill="none"
                      stroke={entry.color}
                      strokeWidth="20"
                      strokeDasharray={`${dashLength} ${circumference - dashLength}`}
                      strokeDashoffset={dashOffset}
                      transform="rotate(-90 60 60)"
                      strokeLinecap="butt"
                    />
                  );
                })}
              <text x="60" y="58" textAnchor="middle" className="fill-white text-[9px] font-medium">
                Total
              </text>
              <text x="60" y="72" textAnchor="middle" className="fill-white text-[12px] font-semibold">
                {total}
              </text>
            </svg>
          </div>
          <div className="space-y-3">
            {entries.map((entry) => {
              const percentage = total > 0 ? ((entry.value / total) * 100).toFixed(1) : '0.0';
              return (
                <div key={entry.key} className="flex items-center justify-between gap-4">
                  <div className="flex min-w-0 items-center gap-2">
                    <span
                      className="h-2.5 w-2.5 rounded-full"
                      style={{ backgroundColor: entry.color }}
                    />
                    <span className="truncate text-sm font-medium text-gray-700">{formatLabel(entry.key)}</span>
                  </div>
                  <span className="whitespace-nowrap text-sm text-gray-600">{entry.value} ({percentage}%)</span>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}

function ResolutionTimeBarChart({ data }) {
  const entries = useMemo(
    () =>
      Object.entries(data || {})
        .map(([username, value]) => ({
          username,
          value: Number(value) || 0
        }))
        .sort((a, b) => b.value - a.value),
    [data]
  );

  const maxValue = useMemo(
    () => Math.max(...entries.map((entry) => entry.value), 1),
    [entries]
  );

  return (
    <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
      <h2 className="text-lg font-semibold text-gray-900">Avg Resolution Time by Assignee</h2>
      <p className="mt-1 text-sm text-gray-500">Grouped per user (hours)</p>

      {entries.length === 0 ? (
        <p className="mt-4 text-sm text-gray-500">No data available.</p>
      ) : (
        <div className="mt-5 overflow-x-auto">
          <div className="min-w-[560px]">
            <div className="flex h-64 items-end gap-3 border-b border-gray-200 pb-2">
              {entries.map((entry) => {
                const heightPct = Math.max(4, (entry.value / maxValue) * 100);
                return (
                  <div key={entry.username} className="flex min-w-[74px] flex-1 flex-col items-center">
                    <span className="mb-2 text-xs font-medium text-gray-600">{formatHoursLabel(entry.value)}</span>
                    <div className="flex h-48 w-full items-end">
                      <div
                        className="w-full rounded-t-md bg-indigo-500 transition-all"
                        style={{ height: `${heightPct}%` }}
                      />
                    </div>
                    <span className="mt-2 w-full truncate text-center text-xs font-medium text-gray-700">
                      {entry.username}
                    </span>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function AdminDashboard({ user, onLogout, onNavigate, isDarkMode, onToggleTheme }) {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const currentPage = 'dashboard';

  const loadDashboardStats = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await issueService.getDashboardStats(user?.id);
      setStats(data);
    } catch (err) {
      console.error('Error loading dashboard stats:', err);
      setError(err.message || 'Failed to load dashboard stats');
    } finally {
      setLoading(false);
    }
  }, [user?.id]);

  useEffect(() => {
    loadDashboardStats();
  }, [loadDashboardStats]);

  const metricCards = [
    {
      title: 'Total Issues',
      value: stats?.totalIssues ?? 0,
      icon: FolderOpen,
      accentClass: 'bg-blue-600'
    },
    {
      title: 'Open Issues',
      value: stats?.openIssues ?? 0,
      icon: Activity,
      accentClass: 'bg-amber-500'
    },
    {
      title: 'Resolved',
      value: stats?.resolvedIssues ?? 0,
      icon: CheckCircle2,
      accentClass: 'bg-emerald-600'
    },
    {
      title: 'Closed',
      value: stats?.closedIssues ?? 0,
      icon: CircleSlash,
      accentClass: 'bg-slate-600'
    },
    {
      title: 'Avg Resolution Time',
      value: formatAverageResolutionTime(stats?.avgResolutionTimeHours),
      icon: Clock3,
      accentClass: 'bg-indigo-600'
    }
  ];

  return (
    <div className="flex h-screen bg-gray-50">
      <Sidebar currentPage={currentPage} onNavigate={onNavigate} userRole={user?.role} />

      <main className="flex-1 overflow-auto">
        <header className="fixed inset-x-0 top-0 z-40 border-b border-gray-200 bg-white/95 px-8 py-4 pl-20 backdrop-blur">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">Admin Dashboard</h1>
              <p className="text-sm text-gray-500">Live issue analytics overview</p>
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

        <div className="space-y-6 p-8 pt-28">
          {loading ? (
            <div className="flex items-center justify-center py-24">
              <div className="text-center">
                <div className="mx-auto mb-4 h-12 w-12 animate-spin rounded-full border-b-2 border-blue-600" />
                <p className="text-gray-600">Loading dashboard...</p>
              </div>
            </div>
          ) : error ? (
            <div className="rounded-xl border border-red-200 bg-red-50 p-6">
              <p className="font-medium text-red-700">Error loading dashboard data</p>
              <p className="mt-1 text-sm text-red-600">{error}</p>
              <button
                onClick={loadDashboardStats}
                className="mt-4 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
              >
                Retry
              </button>
            </div>
          ) : (
            <>
              <section className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-5">
                {metricCards.map((card) => (
                  <MetricCard
                    key={card.title}
                    title={card.title}
                    value={card.value}
                    icon={card.icon}
                    accentClass={card.accentClass}
                  />
                ))}
              </section>

              <section className="grid grid-cols-1 gap-6 xl:grid-cols-2">
                <PieChartCard
                  title="Status Distribution"
                  data={stats?.issuesByStatus}
                  colorsMap={STATUS_COLORS}
                />
                <PieChartCard
                  title="Priority Distribution"
                  data={stats?.issuesByPriority}
                  colorsMap={PRIORITY_COLORS}
                />
              </section>

              <section>
                <ResolutionTimeBarChart data={stats?.avgResolutionTimeHoursPerUser} />
              </section>

              <section>
                <DistributionList
                  title="Open Issues by Assignee"
                  data={stats?.issuesAssignedPerUser}
                  colorsMap={{}}
                />
              </section>
            </>
          )}
        </div>
      </main>
    </div>
  );
}

export default AdminDashboard;
