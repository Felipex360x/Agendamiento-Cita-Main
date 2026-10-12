// ========================================================
// Nicolett Studio Fantasy - Historial de Tratamientos
// ========================================================

var API_BASE = window.API_BASE || '/api/v2/nicolet';
const apiFetch = (url, opts) => (window.authenticatedFetch ? window.authenticatedFetch(url, opts) : fetch(url, opts));

let listaTrabajadores = [];
let trabajadorSeleccionado = null;
let citasHistorial = [];
let usuarioActual = null;

// Elementos DOM
const statusBadge = document.getElementById('statusBadge');
const statusText = document.getElementById('statusText');
const alertMessage = document.getElementById('alertMessage');

const avatarHistorialTrabajador = document.getElementById('avatarHistorialTrabajador');
const nombreTrabajadorFiltro = document.getElementById('nombreTrabajadorFiltro');
const cargoTrabajadorFiltro = document.getElementById('cargoTrabajadorFiltro');
const selectHistorialTrabajador = document.getElementById('selectHistorialTrabajador');

const kpiTotalTratamientos = document.getElementById('kpiTotalTratamientos');
const kpiClientasUnicas = document.getElementById('kpiClientasUnicas');
const kpiVentasTotales = document.getElementById('kpiVentasTotales');

const tablaHistorialBody = document.getElementById('tablaHistorialBody');
const inputBuscarHistorial = document.getElementById('inputBuscarHistorial');

const modalFichaClienta = document.getElementById('modalFichaClienta');
const modalNombreClienta = document.getElementById('modalNombreClienta');
const modalCorreoClienta = document.getElementById('modalCorreoClienta');
const modalTelefonoClienta = document.getElementById('modalTelefonoClienta');
const modalListaSesiones = document.getElementById('modalListaSesiones');

document.addEventListener('DOMContentLoaded', async () => {
  if (typeof requireAuth === 'function' && !requireAuth()) return;
  verificarConexion();

  usuarioActual = typeof getAuthUser === 'function' ? getAuthUser() : null;
  await cargarTrabajadores();

  if (inputBuscarHistorial) {
    inputBuscarHistorial.addEventListener('input', filtrarHistorial);
  }
});

function mostrarAlerta(mensaje, esError = false) {
  alertMessage.className = `alert-banner ${esError ? 'error' : 'success'}`;
  alertMessage.textContent = mensaje;
  alertMessage.style.display = 'block';
  setTimeout(() => {
    alertMessage.style.display = 'none';
  }, 6000);
}

async function verificarConexion() {
  try {
    const res = await apiFetch(`${API_BASE}/servicios`);
    if (res.ok) {
      statusBadge.className = 'status online';
      statusText.textContent = 'API Conectada (8080)';
    } else {
      statusBadge.className = 'status offline';
      statusText.textContent = 'Error API';
    }
  } catch (e) {
    statusBadge.className = 'status offline';
    statusText.textContent = 'Sin conexión';
  }
}

async function cargarTrabajadores() {
  try {
    const res = await apiFetch(`${API_BASE}/trabajadores`);
    if (!res.ok) throw new Error('Error al cargar catálogo de trabajadores');

    listaTrabajadores = await res.json();
    if (listaTrabajadores.length === 0) {
      selectHistorialTrabajador.innerHTML = '<option value="" disabled selected>No hay especialistas registrados</option>';
      nombreTrabajadorFiltro.textContent = 'Sin especialistas';
      return;
    }

    // Si el usuario autenticado es un trabajador, seleccionarlo a él por defecto
    if (usuarioActual) {
      trabajadorSeleccionado = listaTrabajadores.find(t => t.usuario && t.usuario.id === usuarioActual.id);
    }
    if (!trabajadorSeleccionado) {
      trabajadorSeleccionado = listaTrabajadores[0];
    }

    selectHistorialTrabajador.innerHTML = listaTrabajadores.map(t => {
      const nom = t.usuario ? `${t.usuario.nombre} ${t.usuario.apellidoP || ''}`.trim() : `Trabajador #${t.id}`;
      const sel = t.id === trabajadorSeleccionado.id ? 'selected' : '';
      return `<option value="${t.id}" ${sel}>${nom} (${t.cargoEspecialidad || 'Especialista'})</option>`;
    }).join('');

    actualizarCabeceraTrabajador();
    await cargarHistorialTratamientos();
  } catch (err) {
    mostrarAlerta(`Error al cargar especialistas: ${err.message}`, true);
  }
}

function actualizarCabeceraTrabajador() {
  if (!trabajadorSeleccionado) return;
  const nom = trabajadorSeleccionado.usuario ? 
    `${trabajadorSeleccionado.usuario.nombre} ${trabajadorSeleccionado.usuario.apellidoP || ''}`.trim() : `Especialista #${trabajadorSeleccionado.id}`;
  nombreTrabajadorFiltro.textContent = nom;
  cargoTrabajadorFiltro.textContent = trabajadorSeleccionado.cargoEspecialidad || 'Especialista en Estética';
  avatarHistorialTrabajador.textContent = (nom.charAt(0) || 'H').toUpperCase();
}

function cambiarTrabajadorHistorial() {
  const tId = Number(selectHistorialTrabajador.value);
  const encontrado = listaTrabajadores.find(t => t.id === tId);
  if (encontrado) {
    trabajadorSeleccionado = encontrado;
    actualizarCabeceraTrabajador();
    cargarHistorialTratamientos();
  }
}

async function cargarHistorialTratamientos() {
  if (!trabajadorSeleccionado) return;

  tablaHistorialBody.innerHTML = `
    <tr>
      <td colspan="7" style="text-align:center; padding:2rem; color:var(--muted);">
        Consultando historial de atenciones...
      </td>
    </tr>
  `;

  try {
    const res = await apiFetch(`${API_BASE}/citas/trabajador/${trabajadorSeleccionado.id}`);
    if (!res.ok) throw new Error('Error al obtener citas del profesional');

    citasHistorial = await res.json();
    actualizarKPIsHistorial(citasHistorial);
    renderizarTablaHistorial(citasHistorial);
  } catch (err) {
    tablaHistorialBody.innerHTML = `
      <tr>
        <td colspan="7" style="text-align:center; padding:2rem; color:var(--danger);">
          Error al cargar historial: ${err.message}
        </td>
      </tr>
    `;
  }
}

function actualizarKPIsHistorial(citas) {
  kpiTotalTratamientos.textContent = citas.length;

  // Clientes únicos
  const idsClientes = new Set();
  citas.forEach(c => {
    if (c.cliente?.id) idsClientes.add(c.cliente.id);
  });
  kpiClientasUnicas.textContent = idsClientes.size;

  // Ventas totales (excluyendo canceladas)
  const suma = citas
    .filter(c => c.estado !== 'CANCELADA')
    .reduce((acc, c) => acc + Number(c.precioFinal || 0), 0);
  kpiVentasTotales.textContent = `$${suma.toLocaleString('es-CL')}`;
}

function filtrarHistorial() {
  const q = (inputBuscarHistorial.value || '').trim().toLowerCase();
  if (!q) {
    renderizarTablaHistorial(citasHistorial);
    return;
  }

  const filtrados = citasHistorial.filter(c => {
    const cliNom = c.cliente?.usuario ? `${c.cliente.usuario.nombre} ${c.cliente.usuario.apellidoP}`.toLowerCase() : '';
    const cliCorreo = c.cliente?.usuario?.correo?.toLowerCase() || '';
    const servNom = c.servicio?.nombre?.toLowerCase() || '';
    const codigo = c.codigoReserva?.toLowerCase() || '';
    return cliNom.includes(q) || cliCorreo.includes(q) || servNom.includes(q) || codigo.includes(q);
  });

  renderizarTablaHistorial(filtrados);
}

function renderizarTablaHistorial(citas) {
  if (!citas || citas.length === 0) {
    tablaHistorialBody.innerHTML = `
      <tr>
        <td colspan="7" style="text-align:center; padding:2.5rem; color:var(--muted);">
          No se encontraron tratamientos registrados para este especialista.
        </td>
      </tr>
    `;
    return;
  }

  // Ordenar cronológicamente descendente (más recientes primero)
  const ordenadas = [...citas].sort((a, b) => new Date(b.fechaHoraInicio) - new Date(a.fechaHoraInicio));

  tablaHistorialBody.innerHTML = ordenadas.map(cita => {
    const fecha = new Date(cita.fechaHoraInicio).toLocaleString('es-CL', {
      dateStyle: 'short',
      timeStyle: 'short'
    });
    const cliNom = cita.cliente?.usuario ? 
      `${cita.cliente.usuario.nombre} ${cita.cliente.usuario.apellidoP}` : `Cliente #${cita.cliente?.id}`;
    const cliTel = cita.cliente?.telefono || 'Sin teléfono';
    const cliCorreo = cita.cliente?.usuario?.correo || 'Sin correo';
    const servNom = cita.servicio?.nombre || 'Tratamiento';
    const duracion = cita.servicio?.duracionMinutos || 45;
    const precio = Number(cita.precioFinal).toLocaleString('es-CL');

    return `
      <tr>
        <td>
          <div style="font-weight:700; color:var(--fg);">${fecha}</div>
          <div style="font-size:10px; color:var(--muted); font-family:monospace;">${cita.codigoReserva}</div>
        </td>
        <td>
          <strong>${cliNom}</strong>
        </td>
        <td>
          <div style="font-size:11px;">📞 ${cliTel}</div>
          <div style="font-size:10px; color:var(--muted);">${cliCorreo}</div>
        </td>
        <td>
          <div style="font-weight:600; color:var(--primary);">${servNom}</div>
          <div style="font-size:10px; color:var(--muted);">${duracion} min de sesión</div>
        </td>
        <td>
          <strong>$${precio}</strong>
        </td>
        <td>
          <span class="badge-estado ${cita.estado}">${cita.estado}</span>
        </td>
        <td style="text-align: right;">
          <button type="button" class="btn sm outline" onclick="abrirFichaClienta(${cita.cliente?.id}, '${cliNom.replace(/'/g, "\\'")}', '${cliCorreo}', '${cliTel}')">
            Ver Ficha &rarr;
          </button>
        </td>
      </tr>
    `;
  }).join('');
}

// ----------------------------------------------------
// MODAL: FICHA TÉCNICA DE LA CLIENTA (REQUISITO 25)
// ----------------------------------------------------
function abrirFichaClienta(clienteId, nombre, correo, telefono) {
  if (!clienteId) return;

  modalNombreClienta.textContent = `Ficha de ${nombre}`;
  modalCorreoClienta.textContent = correo || 'Sin correo';
  modalTelefonoClienta.textContent = telefono || 'Sin teléfono';

  // Filtrar todas las citas de esta clienta con el trabajador actual
  const citasClienta = citasHistorial.filter(c => c.cliente && c.cliente.id === clienteId);
  const ordenadas = [...citasClienta].sort((a, b) => new Date(b.fechaHoraInicio) - new Date(a.fechaHoraInicio));

  if (ordenadas.length === 0) {
    modalListaSesiones.innerHTML = '<div style="color:var(--muted); padding:1rem; text-align:center;">No hay registros previos para esta clienta.</div>';
  } else {
    modalListaSesiones.innerHTML = ordenadas.map(c => {
      const fecha = new Date(c.fechaHoraInicio).toLocaleString('es-CL', {
        dateStyle: 'medium',
        timeStyle: 'short'
      });
      const serv = c.servicio?.nombre || 'Tratamiento';
      const notas = c.notasCliente ? `Notas de clienta: "${c.notasCliente}"` : 'Sin notas registradas';
      const obsTrabajador = c.notasTrabajador ? `Observación técnica: "${c.notasTrabajador}"` : '';

      return `
        <div class="client-treatment-card">
          <div class="row between">
            <strong style="color:var(--primary); font-size:13px;">${serv}</strong>
            <span class="badge-estado ${c.estado}">${c.estado}</span>
          </div>
          <div style="font-size:11px; color:var(--muted); margin-top:2px;">📅 ${fecha} &bull; Tarifa: $${Number(c.precioFinal).toLocaleString('es-CL')}</div>
          <div style="font-size:11px; color:var(--fg); margin-top:6px; font-style:italic;">${notas}</div>
          ${obsTrabajador ? `<div style="font-size:11px; color:var(--primary); margin-top:4px; font-weight:600;">${obsTrabajador}</div>` : ''}
        </div>
      `;
    }).join('');
  }

  modalFichaClienta.style.display = 'flex';
}

function cerrarModalFicha() {
  modalFichaClienta.style.display = 'none';
}
