import React from 'react';

// Color-coded badge for KYC status
const STATUS_COLORS = {
  DRAFT: 'bg-gray-100 text-gray-700',
  SUBMITTED: 'bg-blue-100 text-blue-700',
  UNDER_REVIEW: 'bg-yellow-100 text-yellow-700',
  APPROVED: 'bg-green-100 text-green-700',
  REJECTED: 'bg-red-100 text-red-700',
  MORE_INFO_REQUESTED: 'bg-orange-100 text-orange-700',
};

export default function StatusBadge({ status }) {
  const colorClass = STATUS_COLORS[status] || 'bg-gray-100 text-gray-700';
  return (
    <span className={`px-2 py-1 rounded text-xs font-semibold ${colorClass}`}>
      {status?.replace(/_/g, ' ')}
    </span>
  );
}
