import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Navbar() {
  const { auth, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <nav className="bg-blue-700 text-white px-6 py-3 flex justify-between items-center shadow">
      <div className="font-bold text-lg">KYC Portal</div>
      <div className="flex items-center gap-4">
        <span className="text-sm">
          {auth?.username} &bull; <span className="bg-blue-500 px-2 py-0.5 rounded text-xs">{auth?.role}</span>
        </span>
        <button
          onClick={handleLogout}
          className="bg-white text-blue-700 px-3 py-1 rounded text-sm font-medium hover:bg-blue-50"
        >
          Logout
        </button>
      </div>
    </nav>
  );
}
