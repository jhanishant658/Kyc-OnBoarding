import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Navbar from '../components/Navbar';
import StatusBadge from '../components/StatusBadge';
import { getQueue, getStats } from '../services/api';

export default function ReviewerDashboard() {
  const [queue, setQueue] = useState([]);
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    Promise.all([getQueue(), getStats()])
      .then(([queueRes, statsRes]) => {
        setQueue(queueRes.data);
        setStats(statsRes.data);
      })
      .catch(err => console.error('Failed to load reviewer data', err))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />
      <div className="max-w-5xl mx-auto p-6">
        <h1 className="text-2xl font-bold text-gray-800 mb-6">Reviewer Dashboard</h1>

        {/* Stats Cards */}
        {stats && (
          <div className="grid grid-cols-3 gap-4 mb-6">
            <StatCard label="Queue Size" value={stats.queueSize} color="blue" />
            <StatCard label="Avg Queue Time" value={`${stats.avgQueueTimeHours}h`} color="yellow" />
            <StatCard label="Approval Rate (7d)" value={`${stats.approvalRateLast7Days}%`} color="green" />
          </div>
        )}

        {/* Queue Table */}
        <div className="bg-white rounded shadow">
          <div className="px-4 py-3 border-b">
            <h2 className="font-semibold text-gray-700">Review Queue (Oldest First)</h2>
          </div>

          {loading ? (
            <p className="p-6 text-gray-500">Loading queue...</p>
          ) : queue.length === 0 ? (
            <p className="p-6 text-gray-500 text-center">Queue is empty! 🎉</p>
          ) : (
            <table className="w-full text-sm">
              <thead className="bg-gray-50 text-gray-600 text-left">
                <tr>
                  <th className="px-4 py-3 font-medium">Merchant</th>
                  <th className="px-4 py-3 font-medium">Business</th>
                  <th className="px-4 py-3 font-medium">Status</th>
                  <th className="px-4 py-3 font-medium">Submitted</th>
                  <th className="px-4 py-3 font-medium">SLA</th>
                  <th className="px-4 py-3 font-medium">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {queue.map(sub => (
                  <tr key={sub.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3">
                      <div className="font-medium">{sub.merchantUsername}</div>
                      <div className="text-gray-400 text-xs">{sub.email}</div>
                    </td>
                    <td className="px-4 py-3">
                      <div>{sub.businessName || '—'}</div>
                      <div className="text-gray-400 text-xs">{sub.businessType}</div>
                    </td>
                    <td className="px-4 py-3">
                      <StatusBadge status={sub.status} />
                    </td>
                    <td className="px-4 py-3 text-gray-500 text-xs">
                      {sub.submittedAt ? new Date(sub.submittedAt).toLocaleString() : '—'}
                    </td>
                    <td className="px-4 py-3">
                      {sub.atRisk ? (
                        <span className="text-red-500 font-semibold text-xs">⚠ AT RISK</span>
                      ) : (
                        <span className="text-green-600 text-xs">OK</span>
                      )}
                    </td>
                    <td className="px-4 py-3">
                      <button
                        onClick={() => navigate(`/reviewer/submissions/${sub.id}`)}
                        className="bg-blue-600 text-white px-3 py-1 rounded text-xs hover:bg-blue-700"
                      >
                        Review
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}

function StatCard({ label, value, color }) {
  const colors = {
    blue: 'border-blue-400 bg-blue-50 text-blue-700',
    yellow: 'border-yellow-400 bg-yellow-50 text-yellow-700',
    green: 'border-green-400 bg-green-50 text-green-700',
  };
  return (
    <div className={`border-l-4 rounded p-4 ${colors[color]}`}>
      <p className="text-xs font-medium opacity-75">{label}</p>
      <p className="text-2xl font-bold mt-1">{value}</p>
    </div>
  );
}
