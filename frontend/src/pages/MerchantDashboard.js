import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Navbar from '../components/Navbar';
import StatusBadge from '../components/StatusBadge';
import { getMySubmissions } from '../services/api';

export default function MerchantDashboard() {
  const [submissions, setSubmissions] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    getMySubmissions()
      .then(res => setSubmissions(res.data))
      .catch(err => console.error('Failed to load submissions', err))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />
      <div className="max-w-4xl mx-auto p-6">
        <div className="flex justify-between items-center mb-6">
          <h1 className="text-2xl font-bold text-gray-800">My KYC Submissions</h1>
          <button
            onClick={() => navigate('/merchant/submissions/new')}
            className="bg-blue-600 text-white px-4 py-2 rounded font-medium hover:bg-blue-700"
          >
            + New Submission
          </button>
        </div>

        {loading ? (
          <p className="text-gray-500">Loading...</p>
        ) : submissions.length === 0 ? (
          <div className="bg-white rounded shadow p-8 text-center text-gray-500">
            <p>No submissions yet.</p>
            <button
              onClick={() => navigate('/merchant/submissions/new')}
              className="mt-4 bg-blue-600 text-white px-4 py-2 rounded"
            >
              Start a KYC Application
            </button>
          </div>
        ) : (
          <div className="bg-white rounded shadow overflow-hidden">
            <table className="w-full text-sm">
              <thead className="bg-gray-50 text-gray-600 text-left">
                <tr>
                  <th className="px-4 py-3 font-medium">Business</th>
                  <th className="px-4 py-3 font-medium">Type</th>
                  <th className="px-4 py-3 font-medium">Status</th>
                  <th className="px-4 py-3 font-medium">Updated</th>
                  <th className="px-4 py-3 font-medium">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {submissions.map(sub => (
                  <tr key={sub.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3">
                      <div className="font-medium">{sub.businessName || '—'}</div>
                      <div className="text-gray-400 text-xs">{sub.name}</div>
                    </td>
                    <td className="px-4 py-3 text-gray-600">{sub.businessType || '—'}</td>
                    <td className="px-4 py-3">
                      <StatusBadge status={sub.status} />
                      {sub.atRisk && (
                        <span className="ml-1 text-red-500 text-xs font-medium">⚠ SLA Risk</span>
                      )}
                    </td>
                    <td className="px-4 py-3 text-gray-500 text-xs">
                      {sub.updatedAt ? new Date(sub.updatedAt).toLocaleDateString() : '—'}
                    </td>
                    <td className="px-4 py-3 flex gap-2">
                      <button
                        onClick={() => navigate(`/merchant/submissions/${sub.id}`)}
                        className="text-blue-600 hover:underline text-xs"
                      >
                        View
                      </button>
                      {(sub.status === 'DRAFT' || sub.status === 'MORE_INFO_REQUESTED') && (
                        <button
                          onClick={() => navigate(`/merchant/submissions/${sub.id}/edit`)}
                          className="text-green-600 hover:underline text-xs"
                        >
                          Edit
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Reviewer notes for MORE_INFO_REQUESTED */}
        {submissions.filter(s => s.status === 'MORE_INFO_REQUESTED' && s.reviewerNotes).map(sub => (
          <div key={sub.id} className="mt-4 bg-orange-50 border border-orange-200 rounded p-4 text-sm">
            <strong>Action needed for "{sub.businessName}":</strong>
            <p className="mt-1 text-orange-700">{sub.reviewerNotes}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
