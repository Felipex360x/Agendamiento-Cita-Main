const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api/v2/nicolet';
const TOKEN_KEY = 'nicolet_jwt_token';

export class ApiError extends Error {
  constructor(message, status, payload, fieldErrors = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.payload = payload;
    this.fieldErrors = fieldErrors; // { campo: mensaje } cuando la API devuelve errores de validación
  }
}

const FIELD_LABELS = {
  nombre: 'Nombre', apellidoP: 'Apellido', correo: 'Correo', password: 'Contraseña', telefono: 'Teléfono',
  rutDni: 'RUT/DNI', cargoEspecialidad: 'Especialidad', comisionPorcentaje: 'Comisión', precio: 'Precio',
  duracionMinutos: 'Duración', descripcion: 'Descripción', motivo: 'Motivo', motivoONotas: 'Motivo o notas',
  notasCliente: 'Notas', tipoUsuarioId: 'Rol', fechaHoraInicio: 'Fecha y hora',
};

// Contrato de errores: { status, codigo, mensaje, error, errores?: { campo: mensaje } }.
// Se aceptan también los formatos anteriores: { campo: mensaje } plano, { message } y { error }.
export function parseApiError(payload, status) {
  if (payload && typeof payload === 'object') {
    let fields = {};
    if (payload.errores && typeof payload.errores === 'object') {
      fields = payload.errores;
    } else if (status === 400 && !payload.mensaje && !payload.message && !payload.error) {
      fields = Object.fromEntries(Object.entries(payload).filter(([, value]) => typeof value === 'string'));
    }
    const entries = Object.entries(fields).filter(([, value]) => typeof value === 'string' && value);
    const summary = entries.map(([field, value]) => (FIELD_LABELS[field] ? `${FIELD_LABELS[field]}: ${value}` : value)).join(' ');
    const message = payload.mensaje || payload.message || (typeof payload.error === 'string' ? payload.error : '') || summary;
    return { message: message || `Error HTTP ${status}`, fieldErrors: Object.fromEntries(entries) };
  }
  if (typeof payload === 'string' && payload.trim() && payload.length < 300 && !payload.trim().startsWith('<')) {
    return { message: payload.trim(), fieldErrors: {} };
  }
  return { message: `Error HTTP ${status}`, fieldErrors: {} };
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
    const { message, fieldErrors } = parseApiError(payload, response.status);
    if (response.status === 401) {
      window.dispatchEvent(new Event('nicolet:unauthorized'));
    }
    throw new ApiError(message, response.status, payload, fieldErrors);
  }
  return payload;
}

export const api = {
  login: (correo, password) => apiRequest('/auth/login', {
    method: 'POST', body: JSON.stringify({ correo, password }),
  }),
  register: (nombre, apellidoP, correo, password, telefono = '') => apiRequest('/auth/register', {
    method: 'POST', body: JSON.stringify({ nombre, apellidoP, correo, password, telefono }),
  }),
  me: () => apiRequest('/auth/me'),
  servicios: () => apiRequest('/servicios'),
  trabajadores: () => apiRequest('/trabajadores'),
  trabajadorPorUsuario: (usuarioId) => apiRequest(`/trabajadores/usuario/${usuarioId}`),
  horarioTrabajador: (trabajadorId) => apiRequest(`/horarios/trabajador/${trabajadorId}`),
  actualizarHorarioTrabajador: (trabajadorId, horario) => apiRequest(`/horarios/trabajador/${trabajadorId}`, { method: 'PUT', body: JSON.stringify(horario) }),
  disponibilidad: (trabajadorId, fecha, servicioId) => {
    const query = new URLSearchParams({ trabajadorId, fecha });
    if (servicioId) query.set('servicioId', servicioId);
    return apiRequest(`/disponibilidad?${query}`);
  },
  clientePorUsuario: (usuarioId) => apiRequest(`/clientes/usuario/${usuarioId}`),
  actualizarPerfilCliente: (id, perfil) => apiRequest(`/clientes/${id}/perfil`, { method: 'PUT', body: JSON.stringify(perfil) }),
  crearCita: (cita) => apiRequest('/citas', { method: 'POST', body: JSON.stringify(cita) }),
  citas: () => apiRequest('/citas'),
  citasDeTrabajador: (trabajadorId) => apiRequest(`/citas/trabajador/${trabajadorId}`),
  citasDeCliente: (clienteId) => apiRequest(`/citas/cliente/${clienteId}`),
  cambiarEstadoCita: (id, nuevoEstado, motivoONotas) => apiRequest(`/citas/${id}/estado`, {
    method: 'PATCH', body: JSON.stringify({ nuevoEstado, motivoONotas }),
  }),
  clientes: () => apiRequest('/clientes'),
  crearCliente: (cliente) => apiRequest('/clientes', { method: 'POST', body: JSON.stringify(cliente) }),
  serviciosAdmin: () => apiRequest('/servicios'),
  crearServicio: (servicio) => apiRequest('/servicios', { method: 'POST', body: JSON.stringify(servicio) }),
  actualizarServicio: (id, servicio) => apiRequest(`/servicios/${id}`, { method: 'PUT', body: JSON.stringify(servicio) }),
  eliminarServicio: (id) => apiRequest(`/servicios/${id}`, { method: 'DELETE' }),
  usuarios: () => apiRequest('/usuarios'),
  usuario: (id) => apiRequest(`/usuarios/${id}`),
  crearUsuario: (usuario) => apiRequest('/usuarios', { method: 'POST', body: JSON.stringify(usuario) }),
  actualizarUsuario: (id, usuario) => apiRequest(`/usuarios/${id}`, { method: 'PUT', body: JSON.stringify(usuario) }),
  desactivarUsuario: (id, motivo) => apiRequest(`/usuarios/${id}/desactivar`, { method: 'PATCH', body: JSON.stringify({ motivo }) }),
  activarUsuario: (id) => apiRequest(`/usuarios/${id}/activar`, { method: 'PATCH' }),
  tiposUsuario: () => apiRequest('/tipos-usuario'),
  correosBandeja: () => apiRequest('/correos'),
  probarFlujoCorreos: (datos) => apiRequest('/correos/probar-flujo', { method: 'POST', body: JSON.stringify(datos || {}) }),
  configurarSmtp: (config) => apiRequest('/correos/configurar-smtp', { method: 'POST', body: JSON.stringify(config) }),
};
