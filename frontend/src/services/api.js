import axios from 'axios';

const API_BASE = '/api/v1';

// Create an axios instance
const api = axios.create({
  baseURL: API_BASE,
});

// Attach JWT token to every request automatically
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// --- Auth ---
export const login = (username, password) =>
  api.post('/auth/login', { username, password });

export const signup = (username, password, role = 'MERCHANT') =>
  api.post('/auth/signup', { username, password, role });

// --- Merchant ---
export const getMySubmissions = () =>
  api.get('/merchant/submissions');

export const getSubmission = (id) =>
  api.get(`/merchant/submissions/${id}`);

export const createDraft = (data) =>
  api.post('/merchant/submissions', data);

export const updateDraft = (id, data) =>
  api.put(`/merchant/submissions/${id}`, data);

export const submitForReview = (id) =>
  api.post(`/merchant/submissions/${id}/submit`);

export const uploadFile = (file, field) => {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('field', field);
  return api.post('/merchant/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
};

// --- Reviewer ---
export const getQueue = () =>
  api.get('/reviewer/queue');

export const getAllSubmissions = () =>
  api.get('/reviewer/submissions');

export const getSubmissionAsReviewer = (id) =>
  api.get(`/reviewer/submissions/${id}`);

export const takeAction = (id, newStatus, notes) =>
  api.patch(`/reviewer/submissions/${id}/action`, { newStatus, notes });

export const getStats = () =>
  api.get('/reviewer/stats');
