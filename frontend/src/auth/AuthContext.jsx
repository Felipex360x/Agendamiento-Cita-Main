import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { api } from '../api/client.js';

const TOKEN_KEY = 'nicolet_jwt_token';
const USER_KEY = 'nicolet_jwt_user';
const AuthContext = createContext(null);

function storedUser() {
  try { return JSON.parse(localStorage.getItem(USER_KEY) || 'null'); }
  catch { return null; }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(storedUser);
  const [loading, setLoading] = useState(Boolean(localStorage.getItem(TOKEN_KEY)));

  const logout = () => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    setUser(null);
  };

  useEffect(() => {
    const onUnauthorized = () => logout();
    window.addEventListener('nicolet:unauthorized', onUnauthorized);
    return () => window.removeEventListener('nicolet:unauthorized', onUnauthorized);
  }, []);

  useEffect(() => {
    if (!localStorage.getItem(TOKEN_KEY)) return;
    api.me()
      .then((profile) => {
        setUser(profile);
        localStorage.setItem(USER_KEY, JSON.stringify(profile));
      })
      .catch(logout)
      .finally(() => setLoading(false));
  }, []);

  const login = async (correo, password) => {
    const session = await api.login(correo, password);
    localStorage.setItem(TOKEN_KEY, session.token);
    const profile = {
      id: session.id,
      nombre: session.nombre,
      apellidoP: session.apellidoP,
      correo: session.correo,
      rol: session.rol,
    };
    localStorage.setItem(USER_KEY, JSON.stringify(profile));
    setUser(profile);
    return profile;
  };

  const value = useMemo(() => ({ user, loading, login, logout }), [user, loading]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth debe usarse dentro de AuthProvider');
  return context;
}
