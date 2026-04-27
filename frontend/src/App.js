import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import LoginPage from './pages/LoginPage';
import SignupPage from './pages/SignupPage';
import MerchantDashboard from './pages/MerchantDashboard';
import ReviewerDashboard from './pages/ReviewerDashboard';
import SubmissionForm from './pages/SubmissionForm';
import SubmissionDetail from './pages/SubmissionDetail';

// Protected route: redirects to login if not authenticated
function ProtectedRoute({ children, requiredRole }) {
  const { auth } = useAuth();
  if (!auth) return <Navigate to="/login" />;
  if (requiredRole && auth.role !== requiredRole) return <Navigate to="/login" />;
  return children;
}

function AppRoutes() {
  const { auth } = useAuth();

  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />

      {/* Merchant routes */}
      <Route path="/merchant" element={
        <ProtectedRoute requiredRole="MERCHANT"><MerchantDashboard /></ProtectedRoute>
      } />
      <Route path="/merchant/submissions/new" element={
        <ProtectedRoute requiredRole="MERCHANT"><SubmissionForm /></ProtectedRoute>
      } />
      <Route path="/merchant/submissions/:id/edit" element={
        <ProtectedRoute requiredRole="MERCHANT"><SubmissionForm /></ProtectedRoute>
      } />
      <Route path="/merchant/submissions/:id" element={
        <ProtectedRoute requiredRole="MERCHANT"><SubmissionDetail /></ProtectedRoute>
      } />

      {/* Reviewer routes */}
      <Route path="/reviewer" element={
        <ProtectedRoute requiredRole="REVIEWER"><ReviewerDashboard /></ProtectedRoute>
      } />
      <Route path="/reviewer/submissions/:id" element={
        <ProtectedRoute requiredRole="REVIEWER"><SubmissionDetail isReviewer /></ProtectedRoute>
      } />

      {/* Default redirect based on role */}
      <Route path="/" element={
        auth?.role === 'REVIEWER'
          ? <Navigate to="/reviewer" />
          : auth?.role === 'MERCHANT'
          ? <Navigate to="/merchant" />
          : <Navigate to="/login" />
      } />
    </Routes>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <AppRoutes />
      </BrowserRouter>
    </AuthProvider>
  );
}
