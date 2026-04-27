import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { login } from '../services/api';
import { useAuth } from '../context/AuthContext';

export default function LoginPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const { login: setAuth } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();

    setError('');
    setLoading(true);

    try {
      const res = await login(username, password);

      const { token, username: user, role } = res.data;

      setAuth(token, user, role);

      navigate(role === 'REVIEWER' ? '/reviewer' : '/merchant');
    } catch (err) {
      setError(
        err.response?.data?.error ||
        'Login failed. Check credentials.'
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-100 px-4">
      <div className="bg-white p-8 rounded shadow w-full max-w-sm">
        <h1 className="text-2xl font-bold text-center text-blue-700 mb-6">
          KYC Portal
        </h1>

        <h2 className="text-lg font-semibold mb-4 text-gray-700">
          Login
        </h2>

        {error && (
          <div className="bg-red-50 text-red-600 border border-red-200 px-3 py-2 rounded mb-4 text-sm">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">

          <div>
            <label className="block text-sm font-medium text-gray-600 mb-1">
              Username
            </label>

            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className="w-full border rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-300"
              placeholder="e.g. merchant1"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-600 mb-1">
              Password
            </label>

            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full border rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-300"
              placeholder="password123"
              required
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full bg-blue-600 text-white py-2 rounded font-medium hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
          >
            {loading && (
              <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
            )}

            {loading ? 'Logging in...' : 'Login'}
          </button>
        </form>

        {/* Free server notice */}
        {loading && (
          <div className="mt-4 bg-yellow-50 border border-yellow-200 text-yellow-700 text-xs rounded p-3">
            This project is deployed on a free server for assignment purposes.
            The first login/signup request may take 30–60 seconds if the server is waking up from inactivity.
          </div>
        )}

        <p className="text-sm text-center text-gray-500 mt-4">
          No account?{' '}
          <Link
            to="/signup"
            className="text-blue-600 hover:underline"
          >
            Sign up as merchant
          </Link>
        </p>

        {/* Test credentials */}
        <div className="mt-6 bg-gray-50 p-3 rounded text-xs text-gray-500">
          <p className="font-semibold mb-1">Test credentials:</p>

          <p>merchant1 / password123</p>
          <p>merchant2 / password123</p>
          <p>reviewer1 / password123</p>
        </div>
      </div>
    </div>
  );
}