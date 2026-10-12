// ========================================================
// Módulo de Autenticación y Manejo de Sesión (Frontend)
// Nicolet Estudio - Sistema de Agendamiento
// ========================================================

// Detección automática del host del backend (resiliente a Live Server port 5500, puertos alternativos o file://)
function getApiBaseUrl() {
  if (typeof window !== 'undefined') {
    if (window.location.protocol === 'file:' || (window.location.port && window.location.port !== '8080')) {
      return 'http://localhost:8080/api/v2/nicolet';
    }
  }
  return '/api/v2/nicolet';
}

window.API_BASE = getApiBaseUrl();
window.AUTH_API_URL = `${window.API_BASE}/auth`;
const TOKEN_KEY = 'nicolet_jwt_token';
const USER_KEY = 'nicolet_jwt_user';

// Obtener token almacenado
function getAuthToken() {
  return localStorage.getItem(TOKEN_KEY);
}

// Obtener datos del usuario logueado
function getAuthUser() {
  try {
    const data = localStorage.getItem(USER_KEY);
    return data ? JSON.parse(data) : null;
  } catch (e) {
    return null;
  }
}

// Guardar datos de sesión
function setAuthSession(data) {
  if (data && data.token) {
    localStorage.setItem(TOKEN_KEY, data.token);
    const user = {
      id: data.id,
      nombre: data.nombre,
      apellidoP: data.apellidoP,
      correo: data.correo,
      rol: data.rol
    };
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  }
}

// Cerrar sesión
function logout() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  window.location.href = 'login.html';
}

// Verificar si existe sesión activa
function isAuthenticated() {
  const token = getAuthToken();
  return !!token;
}

// Proteger vista: Si no está autenticado, redirigir a login.html
function requireAuth() {
  if (!isAuthenticated()) {
    const currentPage = window.location.pathname.split('/').pop() || 'citas.html';
    window.location.href = `login.html?redirect=${encodeURIComponent(currentPage)}`;
    return false;
  }
  return true;
}

// Iniciar sesión llamando a la API
async function login(correo, password) {
  const authUrl = window.AUTH_API_URL || `${getApiBaseUrl()}/auth`;
  let response;
  try {
    response = await fetch(`${authUrl}/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ correo, password })
    });
  } catch (netErr) {
    throw new Error('No se pudo conectar con el servidor backend (http://localhost:8080). Asegúrese de que el backend Spring Boot esté ejecutándose.');
  }

  let data;
  try {
    data = await response.json();
  } catch (jsonErr) {
    throw new Error(`Respuesta inválida del servidor (HTTP ${response.status}).`);
  }

  if (!response.ok) {
    if (data.intentosRestantes !== undefined && data.intentosRestantes > 0) {
      throw new Error(`${data.mensaje} (Intentos restantes antes de bloqueo temporal: ${data.intentosRestantes})`);
    }
    throw new Error(data.mensaje || 'Error al iniciar sesión');
  }

  setAuthSession(data);
  return data;
}

// Registrar nuevo usuario/cliente para primer acceso
async function register(nombre, apellidoP, correo, password, telefono = '') {
  const authUrl = window.AUTH_API_URL || `${getApiBaseUrl()}/auth`;
  let response;
  try {
    response = await fetch(`${authUrl}/register`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ nombre, apellidoP, correo, password, telefono })
    });
  } catch (netErr) {
    throw new Error('No se pudo conectar con el servidor backend (http://localhost:8080). Asegúrese de que el backend Spring Boot esté ejecutándose.');
  }

  let data;
  try {
    data = await response.json();
  } catch (jsonErr) {
    throw new Error(`Respuesta inválida del servidor (HTTP ${response.status}).`);
  }

  if (!response.ok) {
    throw new Error(data.mensaje || 'Error al registrar la cuenta');
  }

  setAuthSession(data);
  return data;
}

// Wrapper para llamadas fetch con token JWT automático y resolución dinámica de URL
async function authenticatedFetch(url, options = {}) {
  const token = getAuthToken();
  const headers = Object.assign({}, options.headers || {});

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  options.headers = headers;

  let finalUrl = url;
  if (window.API_BASE && window.API_BASE.startsWith('http')) {
    if (url.startsWith('/api/v2/nicolet')) {
      finalUrl = url.replace('/api/v2/nicolet', window.API_BASE);
    } else if (url.startsWith('/')) {
      finalUrl = `http://localhost:8080${url}`;
    }
  }

  const response = await fetch(finalUrl, options);

  // Si el token expiró o no es válido (401), redirigir al login
  if (response.status === 401) {
    console.warn('Sesión expirada o token inválido. Redirigiendo a login...');
    logout();
    throw new Error('Sesión expirada. Por favor inicie sesión nuevamente.');
  }

  return response;
}

// Renderizar badge de usuario y botón de cerrar sesión en el navbar
function renderNavUserBadge() {
  const user = getAuthUser();
  if (!user) {
    const slot = document.getElementById('navUserBadgeSlot');
    if (slot && !document.getElementById('navLoginBtn')) {
      slot.innerHTML = `<a id="navLoginBtn" href="login.html" class="btn sm outline" style="font-size: 10px; height: 30px; padding: 0 12px;">Iniciar Sesión</a>`;
    }
    return;
  }

  // Evitar duplicados
  if (document.getElementById('navUserContainer')) return;

  const slot = document.getElementById('navUserBadgeSlot');
  const navbar = document.querySelector('.navbar');
  const targetParent = slot || navbar;
  if (!targetParent) return;

  const container = document.createElement('div');
  container.id = 'navUserContainer';
  container.style.display = 'inline-flex';
  container.style.alignItems = 'center';
  container.style.gap = '10px';

  const userBadge = document.createElement('div');
  userBadge.style.display = 'flex';
  userBadge.style.flexDirection = 'column';
  userBadge.style.alignItems = 'flex-end';
  userBadge.style.lineHeight = '1.2';

  const nameSpan = document.createElement('span');
  nameSpan.style.fontWeight = '600';
  nameSpan.style.fontSize = '12px';
  nameSpan.style.color = 'var(--fg, #4a3f35)';
  nameSpan.innerText = `${user.nombre} ${user.apellidoP || ''}`.trim();

  const roleSpan = document.createElement('span');
  roleSpan.style.fontSize = '9px';
  roleSpan.style.color = 'var(--primary, #a88752)';
  roleSpan.style.fontWeight = '700';
  roleSpan.style.letterSpacing = '1px';
  roleSpan.style.textTransform = 'uppercase';
  roleSpan.innerText = user.rol || 'USUARIO';

  userBadge.appendChild(nameSpan);
  userBadge.appendChild(roleSpan);

  const btnLogout = document.createElement('button');
  btnLogout.innerText = 'Cerrar Sesión';
  btnLogout.title = 'Cerrar sesión';
  btnLogout.className = 'btn sm outline';
  btnLogout.style.fontSize = '9px';
  btnLogout.style.height = '28px';
  btnLogout.style.padding = '0 8px';
  btnLogout.onclick = logout;

  container.appendChild(userBadge);
  container.appendChild(btnLogout);

  targetParent.appendChild(container);
}

// Auto-inicializar renderizado de usuario si la página ya cargó
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', renderNavUserBadge);
} else {
  renderNavUserBadge();
}
