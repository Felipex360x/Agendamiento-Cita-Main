// ========================================================
// Módulo de Autenticación y Manejo de Sesión (Frontend)
// Nicolet Estudio - Sistema de Agendamiento
// ========================================================

const AUTH_API_URL = 'http://localhost:8080/api/v2/nicolet/auth';
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
    window.location.href = 'login.html';
  }
}

// Iniciar sesión llamando a la API
async function login(correo, password) {
  const response = await fetch(`${AUTH_API_URL}/login`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ correo, password })
  });

  const data = await response.json();
  if (!response.ok) {
    if (data.intentosRestantes !== undefined && data.intentosRestantes > 0) {
      throw new Error(`${data.mensaje} (Intentos restantes antes de bloqueo temporal: ${data.intentosRestantes})`);
    }
    throw new Error(data.mensaje || 'Error al iniciar sesión');
  }

  setAuthSession(data);
  return data;
}

// Wrapper para llamadas fetch con token JWT automático
async function authenticatedFetch(url, options = {}) {
  const token = getAuthToken();
  const headers = Object.assign({}, options.headers || {});

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  options.headers = headers;

  const response = await fetch(url, options);

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
  if (!user) return;

  const navbar = document.querySelector('.navbar');
  if (!navbar) return;

  // Evitar duplicados
  if (document.getElementById('navUserContainer')) return;

  const container = document.createElement('div');
  container.id = 'navUserContainer';
  container.style.display = 'flex';
  container.style.alignItems = 'center';
  container.style.gap = '0.75rem';
  container.style.marginLeft = 'auto';

  const userBadge = document.createElement('div');
  userBadge.style.display = 'flex';
  userBadge.style.flexDirection = 'column';
  userBadge.style.alignItems = 'flex-end';
  userBadge.style.lineHeight = '1.2';

  const nameSpan = document.createElement('span');
  nameSpan.style.fontWeight = '700';
  nameSpan.style.fontSize = '0.85rem';
  nameSpan.style.color = '#1f2937';
  nameSpan.innerText = `${user.nombre} ${user.apellidoP || ''}`.trim();

  const roleSpan = document.createElement('span');
  roleSpan.style.fontSize = '0.72rem';
  roleSpan.style.color = '#4f46e5';
  roleSpan.style.fontWeight = '700';
  roleSpan.style.textTransform = 'uppercase';
  roleSpan.innerText = user.rol || 'USUARIO';

  userBadge.appendChild(nameSpan);
  userBadge.appendChild(roleSpan);

  const btnLogout = document.createElement('button');
  btnLogout.innerText = 'Cerrar Sesión';
  btnLogout.title = 'Cerrar sesión';
  btnLogout.style.background = '#fee2e2';
  btnLogout.style.color = '#b91c1c';
  btnLogout.style.border = '1px solid #fca5a5';
  btnLogout.style.padding = '0.35rem 0.65rem';
  btnLogout.style.borderRadius = '6px';
  btnLogout.style.fontWeight = '600';
  btnLogout.style.fontSize = '0.8rem';
  btnLogout.style.cursor = 'pointer';
  btnLogout.onclick = logout;

  container.appendChild(userBadge);
  container.appendChild(btnLogout);

  navbar.appendChild(container);
}

// Auto-inicializar renderizado de usuario si la página ya cargó
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', renderNavUserBadge);
} else {
  renderNavUserBadge();
}
