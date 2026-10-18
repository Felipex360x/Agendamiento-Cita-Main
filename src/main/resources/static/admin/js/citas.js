// ========================================================
// Nicolett Studio Fantasy - Frontend Agendamiento de Citas
// ========================================================

var API_BASE = window.API_BASE || '/api/v2/nicolet';
function getApiBase() {
  return window.API_BASE || API_BASE || '/api/v2/nicolet';
}
const apiFetch = (url, opts) => (window.authenticatedFetch ? window.authenticatedFetch(url, opts) : fetch(url, opts));

// Estado local en memoria
let serviciosDisponibles = [];
let clientesDisponibles = [];
let trabajadoresDisponibles = [];

// Elementos DOM
const statusBadge = document.getElementById('statusBadge');
const statusText = document.getElementById('statusText');
const alertMessage = document.getElementById('alertMessage');
const totalCitasEl = document.getElementById('totalCitas');
const citasConfirmadasEl = document.getElementById('citasConfirmadas');
const totalTrabajadoresKPI = document.getElementById('totalTrabajadoresKPI');
const totalClientesKPI = document.getElementById('totalClientesKPI');
const citasTableBody = document.getElementById('citasTableBody');

const clienteSelect = document.getElementById('clienteId');
const trabajadorSelect = document.getElementById('trabajadorId');
const servicioSelect = document.getElementById('servicioId');
const servicioInfoEl = document.getElementById('servicioInfo');
const fechaHoraInicioInput = document.getElementById('fechaHoraInicio');
const notasClienteInput = document.getElementById('notasCliente');
const citaForm = document.getElementById('citaForm');

const spanCountTrabajadores = document.getElementById('spanCountTrabajadores');
const spanCountClientes = document.getElementById('spanCountClientes');
const listaTrabajadoresRender = document.getElementById('listaTrabajadoresRender');
const listaClientesRender = document.getElementById('listaClientesRender');

// Iniciar al cargar
document.addEventListener('DOMContentLoaded', async () => {
  if (typeof requireAuth === 'function' && !requireAuth()) return;
  establecerFechaMinima();
  verificarConexion();
  await cargarDatosFormulario();
  await cargarCitas();

  // Soporte para preselección desde URL (ej: citas.html?servicioId=2&trabajadorId=1&fechaHora=2026-10-20T10:00:00)
  const params = new URLSearchParams(window.location.search);
  const sIdParam = params.get('servicioId');
  const tIdParam = params.get('trabajadorId');
  const fechaHoraParam = params.get('fechaHora');

  if (sIdParam && servicioSelect) {
    servicioSelect.value = sIdParam;
    actualizarInfoServicio();
  }
  if (tIdParam && trabajadorSelect) {
    trabajadorSelect.value = tIdParam;
  }
  if (fechaHoraParam && fechaHoraInicioInput) {
    fechaHoraInicioInput.value = fechaHoraParam.slice(0, 16);
  }

  // Event listener en select de servicio para mostrar detalles
  servicioSelect.addEventListener('change', actualizarInfoServicio);
  citaForm.addEventListener('submit', manejarAgendamiento);
});

function actualizarInfoServicio() {
  const sId = Number(servicioSelect.value);
  const servicio = serviciosDisponibles.find(s => s.id === sId);
  if (servicio) {
    servicioInfoEl.textContent = `Duración: ${servicio.duracionMinutos} min | Tarifa: $${Number(servicio.precio).toLocaleString('es-CL')}`;
  } else {
    servicioInfoEl.textContent = '';
  }
}

// Configurar fecha mínima como momento actual
function establecerFechaMinima() {
  const now = new Date();
  now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
  fechaHoraInicioInput.min = now.toISOString().slice(0, 16);
}

// Alertas visuales
function mostrarAlerta(mensaje, esError = false) {
  alertMessage.className = `alert-banner ${esError ? 'error' : 'success'}`;
  alertMessage.textContent = mensaje;
  alertMessage.style.display = 'block';
  setTimeout(() => {
    alertMessage.style.display = 'none';
  }, 6000);
}

// Verificar conexión con el backend
async function verificarConexion() {
  const base = getApiBase();
  try {
    const res = await apiFetch(`${base}/servicios`, { method: 'GET' });
    if (res.ok) {
      statusBadge.className = 'status online';
      statusText.textContent = 'API Conectada (8080)';
    } else {
      marcarDesconectado();
    }
  } catch (e) {
    marcarDesconectado();
  }
}

function marcarDesconectado() {
  statusBadge.className = 'status offline';
  statusText.textContent = 'Sin conexión';
}

// Cargar Clientes, Trabajadores y Servicios para los Selects y Tableros
async function cargarDatosFormulario() {
  const base = getApiBase();
  try {
    // 1. Cargar Servicios
    const resServicios = await apiFetch(`${base}/servicios`);
    if (resServicios.ok) {
      serviciosDisponibles = await resServicios.json();
      servicioSelect.innerHTML = '<option value="" disabled selected>Seleccione un servicio...</option>' +
        serviciosDisponibles.map(s => `
          <option value="${s.id}">
            ${s.nombre} ($${Number(s.precio).toLocaleString('es-CL')} - ${s.duracionMinutos} min)
          </option>
        `).join('');
    } else {
      servicioSelect.innerHTML = '<option value="" disabled>Error al cargar servicios</option>';
    }

    // 2. Cargar Clientes (Rol Cliente)
    const resClientes = await apiFetch(`${base}/clientes`);
    if (resClientes.ok) {
      clientesDisponibles = await resClientes.json();
      if (totalClientesKPI) totalClientesKPI.textContent = clientesDisponibles.length;
      if (spanCountClientes) spanCountClientes.textContent = clientesDisponibles.length;

      clienteSelect.innerHTML = '<option value="" disabled selected>Seleccione una clienta...</option>' +
        clientesDisponibles.map(c => {
          const nom = c.usuario ? `${c.usuario.nombre || ''} ${c.usuario.apellidoP || ''}`.trim() : `Clienta #${c.id}`;
          const correo = c.usuario && c.usuario.correo ? ` (${c.usuario.correo})` : '';
          return `<option value="${c.id}">${nom}${correo}</option>`;
        }).join('');

      renderizarListaClientes(clientesDisponibles);
    } else {
      clienteSelect.innerHTML = '<option value="" disabled>Error al cargar clientas</option>';
    }

    // 3. Cargar Especialistas (Rol Trabajador)
    const resTrabajadores = await apiFetch(`${base}/trabajadores`);
    if (resTrabajadores.ok) {
      trabajadoresDisponibles = await resTrabajadores.json();
      if (totalTrabajadoresKPI) totalTrabajadoresKPI.textContent = trabajadoresDisponibles.length;
      if (spanCountTrabajadores) spanCountTrabajadores.textContent = trabajadoresDisponibles.length;

      trabajadorSelect.innerHTML = '<option value="" disabled selected>Seleccione un especialista...</option>' +
        trabajadoresDisponibles.map(t => {
          const nom = t.usuario ? `${t.usuario.nombre || ''} ${t.usuario.apellidoP || ''}`.trim() : `Especialista #${t.id}`;
          const cargo = t.cargoEspecialidad ? ` · ${t.cargoEspecialidad}` : '';
          return `<option value="${t.id}">${nom}${cargo}</option>`;
        }).join('');

      renderizarListaTrabajadores(trabajadoresDisponibles);
    } else {
      trabajadorSelect.innerHTML = '<option value="" disabled>Error al cargar especialistas</option>';
    }
  } catch (error) {
    console.error('Error cargando catálogos:', error);
  }
}

// Renderizado de usuarios con rol Trabajador (Requisito 8)
function renderizarListaTrabajadores(trabajadores) {
  if (!listaTrabajadoresRender) return;
  if (!trabajadores || trabajadores.length === 0) {
    listaTrabajadoresRender.innerHTML = '<div style="color:var(--muted); font-size:11px; padding:12px; text-align:center;">No hay trabajadores registrados aún.</div>';
    return;
  }
  listaTrabajadoresRender.innerHTML = trabajadores.map(t => {
    const nom = t.usuario ? `${t.usuario.nombre} ${t.usuario.apellidoP || ''}`.trim() : `Trabajador #${t.id}`;
    const correo = t.usuario?.correo || 'Sin correo';
    const cargo = t.cargoEspecialidad || 'Especialista';
    const initial = (nom.charAt(0) || 'T').toUpperCase();
    return `
      <div style="display:flex; justify-content:space-between; align-items:center; padding:8px 10px; background:var(--secondary); border-radius:4px; border:1px solid var(--border);">
        <div style="display:flex; align-items:center;">
          <span class="avatar">${initial}</span>
          <div>
            <div style="font-weight:600; font-size:12px; color:var(--fg);">${nom}</div>
            <div style="font-size:10px; color:var(--muted);">${correo} &bull; ${cargo}</div>
          </div>
        </div>
        <span class="badge-role profesional" style="font-size:9px;">#${t.id}</span>
      </div>
    `;
  }).join('');
}

// Renderizado de usuarios con rol Cliente (Requisito 8)
function renderizarListaClientes(clientes) {
  if (!listaClientesRender) return;
  if (!clientes || clientes.length === 0) {
    listaClientesRender.innerHTML = '<div style="color:var(--muted); font-size:11px; padding:12px; text-align:center;">No hay clientes registrados aún.</div>';
    return;
  }
  listaClientesRender.innerHTML = clientes.map(c => {
    const nom = c.usuario ? `${c.usuario.nombre} ${c.usuario.apellidoP || ''}`.trim() : `Cliente #${c.id}`;
    const correo = c.usuario?.correo || 'Sin correo';
    const tel = c.telefono ? ` &bull; ${c.telefono}` : '';
    const initial = (nom.charAt(0) || 'C').toUpperCase();
    return `
      <div style="display:flex; justify-content:space-between; align-items:center; padding:8px 10px; background:var(--secondary); border-radius:4px; border:1px solid var(--border);">
        <div style="display:flex; align-items:center;">
          <span class="avatar">${initial}</span>
          <div>
            <div style="font-weight:600; font-size:12px; color:var(--fg);">${nom}</div>
            <div style="font-size:10px; color:var(--muted);">${correo}${tel}</div>
          </div>
        </div>
        <span class="badge-role cliente" style="font-size:9px;">#${c.id}</span>
      </div>
    `;
  }).join('');
}

// Cargar lista de citas
async function cargarCitas() {
  try {
    citasTableBody.innerHTML = `
      <tr>
        <td colspan="7" style="text-align: center; color: var(--muted); padding: 2rem;">
          Cargando citas...
        </td>
      </tr>
    `;

    const res = await apiFetch(`${API_BASE}/citas`);
    if (!res.ok) throw new Error('Error al obtener citas');

    const citas = await res.json();
    totalCitasEl.textContent = citas.length;
    citasConfirmadasEl.textContent = citas.filter(c => c.estado === 'CONFIRMADA').length;

    if (citas.length === 0) {
      citasTableBody.innerHTML = `
        <tr>
          <td colspan="7" style="text-align: center; color: var(--muted); padding: 2rem;">
            No hay citas agendadas actualmente. ¡Agende la primera desde el formulario!
          </td>
        </tr>
      `;
      return;
    }

    citasTableBody.innerHTML = citas.map(cita => {
      const fechaInicio = new Date(cita.fechaHoraInicio).toLocaleString('es-CL', {
        dateStyle: 'short',
        timeStyle: 'short'
      });
      const fechaFin = new Date(cita.fechaHoraFin).toLocaleTimeString('es-CL', {
        timeStyle: 'short'
      });

      const clienteNombre = cita.cliente?.usuario ? 
        `${cita.cliente.usuario.nombre} ${cita.cliente.usuario.apellidoP}` : `Cliente #${cita.cliente?.id}`;
      
      const trabajadorNombre = cita.trabajador?.usuario ? 
        `${cita.trabajador.usuario.nombre} ${cita.trabajador.usuario.apellidoP}` : `Profesional #${cita.trabajador?.id}`;

      const precioFormat = Number(cita.precioFinal).toLocaleString('es-CL');

      const puedeCompletar = cita.estado !== 'COMPLETADA' && cita.estado !== 'CANCELADA';
      const puedeCancelar = cita.estado !== 'COMPLETADA' && cita.estado !== 'CANCELADA';

      return `
        <tr>
          <td><strong style="color: var(--primary);">${cita.codigoReserva}</strong></td>
          <td>${clienteNombre}</td>
          <td>${trabajadorNombre}</td>
          <td>
            <strong>${cita.servicio?.nombre || 'Servicio'}</strong><br>
            <span style="font-size:0.8rem; color: var(--muted);">$${precioFormat}</span>
          </td>
          <td>
            ${fechaInicio}<br>
            <span style="font-size:0.8rem; color: var(--muted);">Hasta ${fechaFin}</span>
          </td>
          <td>
            <span class="badge-estado ${cita.estado}">${cita.estado}</span>
          </td>
          <td>
            <div class="btn-action-group">
              ${puedeCompletar ? `
                <button class="btn btn-sm btn-secondary" onclick="cambiarEstadoCita(${cita.id}, 'COMPLETADA')" title="Marcar como atendida">
                  Finalizar
                </button>
              ` : ''}
              ${puedeCancelar ? `
                <button class="btn btn-sm btn-danger" onclick="cambiarEstadoCita(${cita.id}, 'CANCELADA')" title="Cancelar cita">
                  Cancelar
                </button>
              ` : ''}
              ${!puedeCompletar && !puedeCancelar ? `
                <span style="font-size: 0.8rem; color: var(--muted); font-style: italic;">Sin acciones</span>
              ` : ''}
            </div>
          </td>
        </tr>
      `;
    }).join('');
  } catch (error) {
    console.error('Error al cargar citas:', error);
    citasTableBody.innerHTML = `
      <tr>
        <td colspan="7" style="text-align: center; color: var(--danger); padding: 2rem;">
          Error al conectar con el servidor: ${error.message}
        </td>
      </tr>
    `;
  }
}

// Enviar formulario para agendar una cita
async function manejarAgendamiento(e) {
  e.preventDefault();

  const payload = {
    clienteId: Number(clienteSelect.value),
    trabajadorId: Number(trabajadorSelect.value),
    servicioId: Number(servicioSelect.value),
    fechaHoraInicio: fechaHoraInicioInput.value,
    notasCliente: notasClienteInput.value || null
  };

  try {
    const res = await apiFetch(`${API_BASE}/citas`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    const data = await res.json();

    if (!res.ok) {
      const errorMsg = data.error || (data.errores ? Object.values(data.errores).join(', ') : 'No se pudo agendar la cita');
      mostrarAlerta(`Error: ${errorMsg}`, true);
      return;
    }

    mostrarAlerta(`Cita agendada con éxito: Código ${data.codigoReserva}`);
    citaForm.reset();
    servicioInfoEl.textContent = '';
    cargarCitas();
  } catch (error) {
    mostrarAlerta(`Error de red: ${error.message}`, true);
  }
}

// Cambiar estado de una cita (COMPLETADA o CANCELADA)
async function cambiarEstadoCita(id, nuevoEstado) {
  let motivo = '';
  if (nuevoEstado === 'CANCELADA') {
    motivo = prompt('Ingrese motivo de la cancelación:') || 'Cancelada por el usuario';
  }

  try {
    const res = await apiFetch(`${API_BASE}/citas/${id}/estado`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        nuevoEstado: nuevoEstado,
        motivoONotas: motivo
      })
    });

    const data = await res.json();

    if (!res.ok) {
      mostrarAlerta(`Error: ${data.error || 'No se pudo actualizar el estado'}`, true);
      return;
    }

    mostrarAlerta(`Estado de la cita actualizado a ${nuevoEstado}`);
    cargarCitas();
  } catch (error) {
    mostrarAlerta(`Error de red: ${error.message}`, true);
  }
}

// Modales de creación rápida de Cliente y Trabajador
function abrirModalCliente() {
  document.getElementById('modalCliente').style.display = 'flex';
}

function cerrarModalCliente() {
  document.getElementById('modalCliente').style.display = 'none';
  document.getElementById('formNuevoCliente').reset();
}

async function guardarNuevoCliente(e) {
  e.preventDefault();
  const nombre = document.getElementById('nuevoClienteNombre').value.trim();
  const apellidoP = document.getElementById('nuevoClienteApellido').value.trim();
  const correo = document.getElementById('nuevoClienteCorreo').value.trim();
  const telefono = document.getElementById('nuevoClienteTelefono').value.trim();

  try {
    let tipoClienteId = 2;
    const resTipos = await apiFetch(`${API_BASE}/tipos-usuario`);
    if (resTipos.ok) {
      const tipos = await resTipos.json();
      const rolCli = tipos.find(t => t.nombre && t.nombre.toUpperCase().includes('CLIENTE'));
      if (rolCli) tipoClienteId = rolCli.id;
    }

    const resUser = await apiFetch(`${API_BASE}/usuarios`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        nombre,
        apellidoP,
        correo,
        password: 'cliente_password_123',
        tipoUsuarioId: tipoClienteId
      })
    });

    const userData = await resUser.json().catch(() => ({}));
    if (!resUser.ok) {
      mostrarAlerta(`Error creando cliente: ${userData.error || 'Correo ya existe o datos inválidos'}`, true);
      return;
    }

    // Actualizar el teléfono en el perfil de cliente sincronizado
    if (telefono && userData.id) {
      const resCli = await apiFetch(`${API_BASE}/clientes/usuario/${userData.id}`);
      if (resCli.ok) {
        const cliPerfil = await resCli.json();
        await apiFetch(`${API_BASE}/clientes/${cliPerfil.id}`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            usuarioId: userData.id,
            telefono
          })
        });
      }
    }

    cerrarModalCliente();
    mostrarAlerta(`Cliente ${nombre} ${apellidoP} registrado exitosamente`);
    await cargarDatosFormulario();

    // Seleccionar el nuevo cliente creado
    const clienteCreado = clientesDisponibles.find(c => c.usuario?.correo?.toLowerCase() === correo.toLowerCase());
    if (clienteCreado) {
      clienteSelect.value = clienteCreado.id;
    }
  } catch (error) {
    mostrarAlerta(`Error de conexión: ${error.message}`, true);
  }
}

function abrirModalTrabajador() {
  document.getElementById('modalTrabajador').style.display = 'flex';
}

function cerrarModalTrabajador() {
  document.getElementById('modalTrabajador').style.display = 'none';
  document.getElementById('formNuevoTrabajador').reset();
}

async function guardarNuevoTrabajador(e) {
  e.preventDefault();
  const nombre = document.getElementById('nuevoTrabajadorNombre').value.trim();
  const apellidoP = document.getElementById('nuevoTrabajadorApellido').value.trim();
  const correo = document.getElementById('nuevoTrabajadorCorreo').value.trim();
  const cargo = document.getElementById('nuevoTrabajadorCargo').value.trim();

  try {
    let tipoProId = 3;
    const resTipos = await apiFetch(`${API_BASE}/tipos-usuario`);
    if (resTipos.ok) {
      const tipos = await resTipos.json();
      const rolPro = tipos.find(t => t.nombre && (
        t.nombre.toUpperCase().includes('PROFESIONAL') ||
        t.nombre.toUpperCase().includes('TRABAJADOR') ||
        t.nombre.toUpperCase().includes('ESPECIALISTA')
      ));
      if (rolPro) tipoProId = rolPro.id;
    }

    const resUser = await apiFetch(`${API_BASE}/usuarios`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        nombre,
        apellidoP,
        correo,
        password: 'pro_password_123',
        tipoUsuarioId: tipoProId
      })
    });

    const userData = await resUser.json().catch(() => ({}));
    if (!resUser.ok) {
      mostrarAlerta(`Error creando profesional: ${userData.error || 'Correo ya existe o datos inválidos'}`, true);
      return;
    }

    // Actualizar la especialidad/cargo en el perfil de trabajador sincronizado
    if (cargo && userData.id) {
      const resTrab = await apiFetch(`${API_BASE}/trabajadores/usuario/${userData.id}`);
      if (resTrab.ok) {
        const trabPerfil = await resTrab.json();
        await apiFetch(`${API_BASE}/trabajadores/${trabPerfil.id}`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            usuarioId: userData.id,
            cargoEspecialidad: cargo
          })
        });
      }
    }

    cerrarModalTrabajador();
    mostrarAlerta(`Profesional ${nombre} ${apellidoP} registrado exitosamente`);
    await cargarDatosFormulario();

    // Seleccionar el nuevo trabajador creado
    const trabajadorCreado = trabajadoresDisponibles.find(t => t.usuario?.correo?.toLowerCase() === correo.toLowerCase());
    if (trabajadorCreado) {
      trabajadorSelect.value = trabajadorCreado.id;
    }
  } catch (error) {
    mostrarAlerta(`Error de conexión: ${error.message}`, true);
  }
}
