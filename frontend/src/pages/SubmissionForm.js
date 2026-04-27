import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import Navbar from '../components/Navbar';
import { createDraft, updateDraft, submitForReview, uploadFile, getSubmission } from '../services/api';

export default function SubmissionForm() {
  const { id } = useParams(); // If id exists, we're editing an existing draft
  const navigate = useNavigate();
  const isEditing = !!id;

  const [form, setForm] = useState({
    name: '', email: '', phone: '',
    businessName: '', businessType: '', expectedMonthlyVolume: '',
    panFilePath: '', aadhaarFilePath: '', bankStatementFilePath: ''
  });

  const [fileStatus, setFileStatus] = useState({ pan: '', aadhaar: '', bank: '' });
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [savedId, setSavedId] = useState(id || null);

  // Load existing submission if editing
  useEffect(() => {
    if (id) {
      getSubmission(id).then(res => {
        const s = res.data;
        setForm({
          name: s.name || '', email: s.email || '', phone: s.phone || '',
          businessName: s.businessName || '', businessType: s.businessType || '',
          expectedMonthlyVolume: s.expectedMonthlyVolume || '',
          panFilePath: s.panFilePath || '', aadhaarFilePath: s.aadhaarFilePath || '',
          bankStatementFilePath: s.bankStatementFilePath || ''
        });
      }).catch(() => setError('Could not load submission.'));
    }
  }, [id]);

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  // Upload a file and store the returned path in the form
  const handleFileUpload = async (e, field, key) => {
    const file = e.target.files[0];
    if (!file) return;

    setFileStatus(prev => ({ ...prev, [key]: 'Uploading...' }));
    try {
      const res = await uploadFile(file, field);
      const path = res.data.filePath;
      setForm(prev => ({ ...prev, [`${key === 'bank' ? 'bankStatement' : key}FilePath`]: path }));
      setFileStatus(prev => ({ ...prev, [key]: `✓ ${file.name}` }));
    } catch (err) {
      const msg = err.response?.data?.error || 'Upload failed';
      setFileStatus(prev => ({ ...prev, [key]: `✗ ${msg}` }));
    }
  };

  // Save as draft
  const handleSaveDraft = async () => {
    setError('');
    setSaving(true);
    try {
      let res;
      if (savedId) {
        res = await updateDraft(savedId, form);
      } else {
        res = await createDraft(form);
        setSavedId(res.data.id);
      }
      alert('Draft saved!');
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to save draft.');
    } finally {
      setSaving(false);
    }
  };

  // Save and submit for review
  const handleSubmit = async () => {
    setError('');
    setSubmitting(true);
    try {
      // First save the current form data
      let currentId = savedId;
      if (currentId) {
        await updateDraft(currentId, form);
      } else {
        const res = await createDraft(form);
        currentId = res.data.id;
        setSavedId(currentId);
      }
      // Then submit
      await submitForReview(currentId);
      navigate('/merchant');
    } catch (err) {
      setError(err.response?.data?.error || 'Submission failed.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />
      <div className="max-w-2xl mx-auto p-6">
        <div className="flex items-center gap-3 mb-6">
          <button onClick={() => navigate('/merchant')} className="text-blue-600 hover:underline text-sm">← Back</button>
          <h1 className="text-xl font-bold text-gray-800">{isEditing ? 'Edit KYC Application' : 'New KYC Application'}</h1>
        </div>

        {error && <div className="bg-red-50 text-red-600 border border-red-200 px-4 py-3 rounded mb-4 text-sm">{error}</div>}

        <div className="bg-white rounded shadow p-6 space-y-5">
          {/* Personal Info */}
          <section>
            <h2 className="text-sm font-semibold text-gray-500 uppercase mb-3">Personal Information</h2>
            <div className="grid grid-cols-2 gap-4">
              <FormField label="Full Name" name="name" value={form.name} onChange={handleChange} />
              <FormField label="Email" name="email" type="email" value={form.email} onChange={handleChange} />
              <FormField label="Phone" name="phone" value={form.phone} onChange={handleChange} />
            </div>
          </section>

          {/* Business Info */}
          <section>
            <h2 className="text-sm font-semibold text-gray-500 uppercase mb-3">Business Information</h2>
            <div className="grid grid-cols-2 gap-4">
              <FormField label="Business Name" name="businessName" value={form.businessName} onChange={handleChange} />
              <div>
                <label className="block text-sm font-medium text-gray-600 mb-1">Business Type</label>
                <select
                  name="businessType"
                  value={form.businessType}
                  onChange={handleChange}
                  className="w-full border rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-300"
                >
                  <option value="">Select...</option>
                  <option>Retail</option>
                  <option>E-commerce</option>
                  <option>Services</option>
                  <option>Manufacturing</option>
                  <option>Export</option>
                  <option>Food & Beverage</option>
                  <option>Other</option>
                </select>
              </div>
              <FormField label="Expected Monthly Volume (₹)" name="expectedMonthlyVolume" value={form.expectedMonthlyVolume} onChange={handleChange} />
            </div>
          </section>

          {/* Document Uploads */}
          <section>
            <h2 className="text-sm font-semibold text-gray-500 uppercase mb-3">Documents</h2>
            <p className="text-xs text-gray-400 mb-3">Accepted: PDF, JPG, PNG. Max 5MB each.</p>
            <div className="space-y-3">
              <FileUpload label="PAN Card" fieldKey="pan" status={fileStatus.pan} currentPath={form.panFilePath}
                onChange={e => handleFileUpload(e, 'PAN Card', 'pan')} />
              <FileUpload label="Aadhaar Card" fieldKey="aadhaar" status={fileStatus.aadhaar} currentPath={form.aadhaarFilePath}
                onChange={e => handleFileUpload(e, 'Aadhaar Card', 'aadhaar')} />
              <FileUpload label="Bank Statement" fieldKey="bank" status={fileStatus.bank} currentPath={form.bankStatementFilePath}
                onChange={e => handleFileUpload(e, 'Bank Statement', 'bank')} />
            </div>
          </section>

          {/* Actions */}
          <div className="flex gap-3 pt-2">
            <button
              onClick={handleSaveDraft}
              disabled={saving}
              className="bg-gray-200 text-gray-700 px-4 py-2 rounded font-medium hover:bg-gray-300 disabled:opacity-50"
            >
              {saving ? 'Saving...' : 'Save Draft'}
            </button>
            <button
              onClick={handleSubmit}
              disabled={submitting}
              className="bg-blue-600 text-white px-4 py-2 rounded font-medium hover:bg-blue-700 disabled:opacity-50"
            >
              {submitting ? 'Submitting...' : 'Submit for Review'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

// Simple reusable form field
function FormField({ label, name, type = 'text', value, onChange }) {
  return (
    <div>
      <label className="block text-sm font-medium text-gray-600 mb-1">{label}</label>
      <input
        type={type}
        name={name}
        value={value}
        onChange={onChange}
        className="w-full border rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-300"
      />
    </div>
  );
}

// File upload field
function FileUpload({ label, fieldKey, status, currentPath, onChange }) {
  return (
    <div className="flex items-center gap-3">
      <label className="w-36 text-sm font-medium text-gray-600">{label}</label>
      <input type="file" accept=".pdf,.jpg,.jpeg,.png" onChange={onChange} className="text-sm" />
      <span className={`text-xs ${status?.startsWith('✓') ? 'text-green-600' : status?.startsWith('✗') ? 'text-red-500' : 'text-gray-400'}`}>
        {status || (currentPath ? `✓ ${currentPath}` : 'No file')}
      </span>
    </div>
  );
}
