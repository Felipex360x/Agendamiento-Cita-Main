const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api/v2/nicolet';
const TOKEN_KEY = 'nicolet_jwt_token';

export class ApiError extends Error {
  constructor(message, status, payload) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.payload = payload;
  }
}

export async function apiRequest(path, options = {}) {
  const headers = new Headers(options.headers || {});
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) headers.set('Authorization', `Bearer ${token}`);
  if (options.body && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }

  let response;
  try {
    response = await fetch(`${API_BASE}${path}`, { ...options, headers });
  } catch {
    throw new ApiError('No se pudo conectar con el backend. Verifica que Spring Boot esté activo en localhost:8080.', 0);
  }

  const text = await response.text();
  let payload = null;
  if (text) {
    try { payload = JSON.parse(text); } catch { payload = text; }
  }

  if (!response.ok) {
    const message = payload?.mensaje || payload?.message || payload?.error || `Error HTTP ${response.status}`;
    if (response.status === 401) {
      window.dispatchEvent(new Event('nicolet:unauthorized'));
    }
    throw new ApiError(message, response.status, payload);
  }
  return payload;
}

export const api = {
  login: (correo, password) => apiRequest('/auth/login', {
    method: 'POST', body: JSON.stringify({ correo, password }),
  }),
  me: () => apiRequest('/auth/me'),
  servicios: () => apiRequest('/servicios'),
  trabajadores: () => apiRequest('/trabajadores'),
};
