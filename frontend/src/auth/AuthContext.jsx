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

  const saveSession = (session) => {
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

  const login = async (correo, password) => saveSession(await api.login(correo, password));
  const register = async (nombre, apellidoP, correo, password, telefono = '') =>
    saveSession(await api.register(nombre, apellidoP, correo, password, telefono));

  const updateProfile = (changes) => {
    setUser((current) => {
      if (!current) return current;
      const updated = { ...current, ...changes };
      localStorage.setItem(USER_KEY, JSON.stringify(updated));
      return updated;
    });
  };

  const value = useMemo(() => ({ user, loading, login, register, logout, updateProfile }), [user, loading]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth debe usarse dentro de AuthProvider');
  return context;
}
