import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import Navbar from '../components/Navbar';
import StatusBadge from '../components/StatusBadge';
import { getSubmission, getSubmissionAsReviewer, takeAction } from '../services/api';

export default function SubmissionDetail({ isReviewer = false }) {
  const { id } = useParams();
  const navigate = useNavigate();

  const [submission, setSubmission] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Reviewer action state
  const [reviewNotes, setReviewNotes] = useState('');
  const [actionMsg, setActionMsg] = useState('');

  useEffect(() => {
    const fetchFn = isReviewer ? getSubmissionAsReviewer : getSubmission;
    fetchFn(id)
      .then(res => setSubmission(res.data))
      .catch(err => setError(err.response?.data?.error || 'Could not load submission.'))
      .finally(() => setLoading(false));
  }, [id, isReviewer]);

  const handleAction = async (newStatus) => {
    setActionMsg('');
    try {
      const res = await takeAction(id, newStatus, reviewNotes);
      setSubmission(res.data);
      setActionMsg('Action taken successfully!');
      setReviewNotes('');
    } catch (err) {
      setActionMsg('Error: ' + (err.response?.data?.error || 'Action failed'));
    }
  };

  if (loading) return <div className="p-8 text-gray-500">Loading...</div>;
  if (error) return <div className="p-8 text-red-500">{error}</div>;
  if (!submission) return null;

  const backPath = isReviewer ? '/reviewer' : '/merchant';

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />
      <div className="max-w-2xl mx-auto p-6">
        <div className="flex items-center gap-3 mb-6">
          <button onClick={() => navigate(backPath)} className="text-blue-600 hover:underline text-sm">← Back</button>
          <h1 className="text-xl font-bold text-gray-800">KYC Application #{submission.id}</h1>
          <StatusBadge status={submission.status} />
          {submission.atRisk && <span className="text-red-500 text-xs font-semibold">⚠ SLA AT RISK</span>}
        </div>

        <div className="bg-white rounded shadow p-6 space-y-4">
          {/* Personal Info */}
          <Section title="Personal Information">
            <Field label="Name" value={submission.name} />
            <Field label="Email" value={submission.email} />
            <Field label="Phone" value={submission.phone} />
          </Section>

          {/* Business Info */}
          <Section title="Business Information">
            <Field label="Business Name" value={submission.businessName} />
            <Field label="Business Type" value={submission.businessType} />
            <Field label="Expected Monthly Volume" value={submission.expectedMonthlyVolume ? `₹${submission.expectedMonthlyVolume}` : null} />
          </Section>

          {/* Documents */}
          <Section title="Uploaded Documents">
            <Field label="PAN Card" value={submission.panFilePath} />
            <Field label="Aadhaar Card" value={submission.aadhaarFilePath} />
            <Field label="Bank Statement" value={submission.bankStatementFilePath} />
          </Section>

          {/* Timestamps */}
          <Section title="Timeline">
            <Field label="Created" value={formatDate(submission.createdAt)} />
            <Field label="Submitted" value={formatDate(submission.submittedAt)} />
            <Field label="Last Updated" value={formatDate(submission.updatedAt)} />
          </Section>

          {/* Reviewer Notes */}
          {submission.reviewerNotes && (
            <div className="bg-orange-50 border border-orange-200 rounded p-3">
              <p className="text-xs font-semibold text-gray-600 mb-1">Reviewer Notes</p>
              <p className="text-sm text-orange-800">{submission.reviewerNotes}</p>
            </div>
          )}

          {/* Reviewer actions panel */}
          {isReviewer && (
            <div className="border-t pt-4">
              <h3 className="font-semibold text-gray-700 mb-3">Reviewer Actions</h3>

              {actionMsg && (
                <div className={`mb-3 px-3 py-2 rounded text-sm ${actionMsg.startsWith('Error') ? 'bg-red-50 text-red-600' : 'bg-green-50 text-green-700'}`}>
                  {actionMsg}
                </div>
              )}

              <textarea
                value={reviewNotes}
                onChange={e => setReviewNotes(e.target.value)}
                placeholder="Add notes or reason (required for rejection / more info)"
                className="w-full border rounded px-3 py-2 text-sm mb-3 focus:outline-none focus:ring-2 focus:ring-blue-300"
                rows={3}
              />

              <div className="flex flex-wrap gap-2">
                {submission.status === 'SUBMITTED' && (
                  <ActionButton label="Start Review" color="blue" onClick={() => handleAction('UNDER_REVIEW')} />
                )}
                {submission.status === 'UNDER_REVIEW' && (
                  <>
                    <ActionButton label="✓ Approve" color="green" onClick={() => handleAction('APPROVED')} />
                    <ActionButton label="✗ Reject" color="red" onClick={() => handleAction('REJECTED')} />
                    <ActionButton label="Request More Info" color="orange" onClick={() => handleAction('MORE_INFO_REQUESTED')} />
                  </>
                )}
                {(submission.status === 'APPROVED' || submission.status === 'REJECTED') && (
                  <p className="text-sm text-gray-500">This submission is in a final state.</p>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function Section({ title, children }) {
  return (
    <div>
      <h2 className="text-xs font-semibold text-gray-400 uppercase mb-2">{title}</h2>
      <div className="grid grid-cols-2 gap-2">{children}</div>
    </div>
  );
}

function Field({ label, value }) {
  return (
    <div>
      <p className="text-xs text-gray-400">{label}</p>
      <p className="text-sm text-gray-800">{value || '—'}</p>
    </div>
  );
}

function ActionButton({ label, color, onClick }) {
  const colors = {
    blue: 'bg-blue-600 hover:bg-blue-700',
    green: 'bg-green-600 hover:bg-green-700',
    red: 'bg-red-600 hover:bg-red-700',
    orange: 'bg-orange-500 hover:bg-orange-600',
  };
  return (
    <button
      onClick={onClick}
      className={`${colors[color]} text-white px-4 py-2 rounded text-sm font-medium`}
    >
      {label}
    </button>
  );
}

function formatDate(dateStr) {
  if (!dateStr) return null;
  return new Date(dateStr).toLocaleString();
}
