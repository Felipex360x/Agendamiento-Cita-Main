import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App.jsx';
import { AuthProvider } from './auth/AuthContext.jsx';
import './styles/base.css';
import './styles/home.css';
import './styles/auth-booking.css';
import './styles/admin.css';
import './styles/responsive.css';
import './styles/email-preview.css';

ReactDOM.createRoot(document.getElementById('root')).render(
  <AuthProvider>
    <App />
  </AuthProvider>,
);
