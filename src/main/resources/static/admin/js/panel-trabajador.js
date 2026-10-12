// ========================================================
// Nicolett Studio Fantasy - Portal Exclusivo de Trabajadores
// ========================================================

var API_BASE = window.API_BASE || '/api/v2/nicolet';
const apiFetch = (url, opts) => (window.authenticatedFetch ? window.authenticatedFetch(url, opts) : fetch(url, opts));

let usuarioActual = null;
let trabajadorActual = null;
let listaTodosTrabajadores = [];
let citasDelTrabajador = [];

// Elementos DOM
const statusBadge = document.getElementById('statusBadge');
const statusText = document.getElementById('statusText');
const alertMessage = document.getElementById('alertMessage');

const accesoRestringidoBox = document.getElementById('accesoRestringidoBox');
const rolActualUsuarioEl = document.getElementById('rolActualUsuario');
const contenidoPortalTrabajador = document.getElementById('contenidoPortalTrabajador');

const nombreTrabajadorHeader = document.getElementById('nombreTrabajadorHeader');
const cargoTrabajadorHeader = document.getElementById('cargoTrabajadorHeader');
const correoTrabajadorHeader = document.getElementById('correoTrabajadorHeader');
const badgeInicialTrabajador = document.getElementById('badgeInicialTrabajador');
const selectorAdminTrabajadorContainer = document.getElementById('selectorAdminTrabajadorContainer');
const adminSelectTrabajador = document.getElementById('adminSelectTrabajador');

const kpiPendientes = document.getElementById('kpiPendientes');
const kpiConfirmadas = document.getElementById('kpiConfirmadas');
const kpiCompletadas = document.getElementById('kpiCompletadas');
const kpiTotal = document.getElementById('kpiTotal');

const tabCountPendientes = document.getElementById('tabCountPendientes');
const tabCountConfirmadas = document.getElementById('tabCountConfirmadas');
const tabCountTodas = document.getElementById('tabCountTodas');

const vistaCitasPendientes = document.getElementById('vistaCitasPendientes');
const vistaCitasConfirmadas = document.getElementById('vistaCitasConfirmadas');
const vistaTodasCitas = document.getElementById('vistaTodasCitas');

const tabPendientes = document.getElementById('tabPendientes');
const tabConfirmadas = document.getElementById('tabConfirmadas');
const tabTodas = document.getElementById('tabTodas');

const contenedorTarjetasPendientes = document.getElementById('contenedorTarjetasPendientes');
const tablaConfirmadasBody = document.getElementById('tablaConfirmadasBody');
const tablaTodasBody = document.getElementById('tablaTodasBody');

document.addEventListener('DOMContentLoaded', async () => {
  if (typeof requireAuth === 'function' && !requireAuth()) return;
  verificarConexion();

  usuarioActual = typeof getAuthUser === 'function' ? getAuthUser() : null;
  if (!verificarAccesoTrabajador()) return;

  await cargarDatosIniciales();
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

// Control de Acceso: Solo Trabajadores y Administradores (Requisito 22)
function verificarAccesoTrabajador() {
  if (!usuarioActual) {
    window.location.href = 'login.html?redirect=panel-trabajador.html';
    return false;
  }

  const rol = (usuarioActual.rol || '').toUpperCase();
  const esTrabajador = rol.includes('PROFESIONAL') || rol.includes('TRABAJADOR') || rol.includes('ESPECIALISTA');
  const esAdmin = rol.includes('ADMIN');

  if (!esTrabajador && !esAdmin) {
    rolActualUsuarioEl.textContent = usuarioActual.rol || 'CLIENTE';
    accesoRestringidoBox.style.display = 'block';
    contenidoPortalTrabajador.style.display = 'none';
    return false;
  }

  accesoRestringidoBox.style.display = 'none';
  contenidoPortalTrabajador.style.display = 'block';
  return true;
}

async function cargarDatosIniciales() {
  try {
    // 1. Cargar lista de trabajadores
    const resTrab = await apiFetch(`${API_BASE}/trabajadores`);
    if (!resTrab.ok) throw new Error('Error al cargar trabajadores');
    listaTodosTrabajadores = await resTrab.json();

    const rol = (usuarioActual.rol || '').toUpperCase();
    const esAdmin = rol.includes('ADMIN');

    // Identificar qué trabajador corresponde al usuario logueado
    trabajadorActual = listaTodosTrabajadores.find(t => t.usuario && t.usuario.id === usuarioActual.id);

    if (!trabajadorActual) {
      // Si es admin pero no tiene perfil directo de trabajador, usar el primer especialista y mostrar selector
      if (esAdmin && listaTodosTrabajadores.length > 0) {
        trabajadorActual = listaTodosTrabajadores[0];
      }
    }

    if (esAdmin) {
      selectorAdminTrabajadorContainer.style.display = 'block';
      adminSelectTrabajador.innerHTML = listaTodosTrabajadores.map(t => {
        const nom = t.usuario ? `${t.usuario.nombre} ${t.usuario.apellidoP || ''}`.trim() : `Trabajador #${t.id}`;
        const selected = trabajadorActual && t.id === trabajadorActual.id ? 'selected' : '';
        return `<option value="${t.id}" ${selected}>${nom} (${t.cargoEspecialidad || 'Especialista'})</option>`;
      }).join('');
    }

    renderizarHeaderTrabajador();
    await cargarCitasTrabajador();
  } catch (err) {
    mostrarAlerta(`Error al inicializar portal: ${err.message}`, true);
  }
}

function renderizarHeaderTrabajador() {
  if (!trabajadorActual) {
    nombreTrabajadorHeader.textContent = 'Sin perfil de especialista';
    cargoTrabajadorHeader.textContent = 'No se encontró registro de trabajador asociado';
    correoTrabajadorHeader.textContent = usuarioActual.correo;
    return;
  }

  const nom = trabajadorActual.usuario ? 
    `${trabajadorActual.usuario.nombre} ${trabajadorActual.usuario.apellidoP || ''}`.trim() : `Especialista #${trabajadorActual.id}`;
  nombreTrabajadorHeader.textContent = nom;
  cargoTrabajadorHeader.textContent = trabajadorActual.cargoEspecialidad || 'Especialista en Estética Integral';
  correoTrabajadorHeader.textContent = trabajadorActual.usuario?.correo || usuarioActual.correo;
  badgeInicialTrabajador.textContent = (nom.charAt(0) || 'T').toUpperCase();
}

function cambiarTrabajadorSeleccionado() {
  const nuevoId = Number(adminSelectTrabajador.value);
  const encontrado = listaTodosTrabajadores.find(t => t.id === nuevoId);
  if (encontrado) {
    trabajadorActual = encontrado;
    renderizarHeaderTrabajador();
    cargarCitasTrabajador();
  }
}

async function cargarCitasTrabajador() {
  if (!trabajadorActual) return;

  contenedorTarjetasPendientes.innerHTML = '<div style="padding:24px; text-align:center; color:var(--muted);">Cargando citas...</div>';
  tablaConfirmadasBody.innerHTML = '<tr><td colspan="7" style="text-align:center; padding:2rem;">Cargando...</td></tr>';
  tablaTodasBody.innerHTML = '<tr><td colspan="6" style="text-align:center; padding:2rem;">Cargando...</td></tr>';

  try {
    const res = await apiFetch(`${API_BASE}/citas/trabajador/${trabajadorActual.id}`);
    if (!res.ok) throw new Error('Error al consultar citas del trabajador');

    citasDelTrabajador = await res.json();

    const pendientes = citasDelTrabajador.filter(c => c.estado === 'PENDIENTE');
    const confirmadas = citasDelTrabajador.filter(c => c.estado === 'CONFIRMADA' || c.estado === 'EN_ATENCION');
    const completadas = citasDelTrabajador.filter(c => c.estado === 'COMPLETADA');

    // Actualizar KPIs y Contadores
    kpiPendientes.textContent = pendientes.length;
    kpiConfirmadas.textContent = confirmadas.length;
    kpiCompletadas.textContent = completadas.length;
    kpiTotal.textContent = citasDelTrabajador.length;

    tabCountPendientes.textContent = pendientes.length;
    tabCountConfirmadas.textContent = confirmadas.length;
    tabCountTodas.textContent = citasDelTrabajador.length;

    renderizarCitasPendientes(pendientes);
    renderizarCitasConfirmadas(confirmadas);
    renderizarTodasLasCitas(citasDelTrabajador);
  } catch (err) {
    mostrarAlerta(`Error al cargar citas: ${err.message}`, true);
  }
}

// ----------------------------------------------------
// VISTA 1: CITAS PENDIENTES (REQUISITO 23)
// El trabajador tiene que confirmar la cita con el cliente
// ----------------------------------------------------
function renderizarCitasPendientes(pendientes) {
  if (!pendientes || pendientes.length === 0) {
    contenedorTarjetasPendientes.innerHTML = `
      <div style="padding: 36px 20px; text-align: center; color: var(--muted); background: var(--secondary); border-radius: var(--radius); border: 1px dashed var(--border);">
        <strong style="font-size: 15px; color: var(--fg);">No tienes citas pendientes de confirmación</strong>
        <p class="small" style="margin-top: 6px;">Todas las solicitudes están confirmadas o no hay nuevas reservas en este momento.</p>
      </div>
    `;
    return;
  }

  contenedorTarjetasPendientes.innerHTML = pendientes.map(cita => {
    const cliNom = cita.cliente?.usuario ? 
      `${cita.cliente.usuario.nombre} ${cita.cliente.usuario.apellidoP}` : `Cliente #${cita.cliente?.id}`;
    const cliCorreo = cita.cliente?.usuario?.correo || 'Sin correo registrado';
    const cliTel = cita.cliente?.telefono || 'Sin teléfono';
    const servicioNom = cita.servicio?.nombre || 'Servicio Profesional';
    const duracion = cita.servicio?.duracionMinutos || 45;
    const precio = Number(cita.precioFinal).toLocaleString('es-CL');
    const notas = cita.notasCliente ? `"${cita.notasCliente}"` : 'Sin notas adicionales';

    const fechaIni = new Date(cita.fechaHoraInicio).toLocaleString('es-CL', {
      dateStyle: 'full',
      timeStyle: 'short'
    });
    const fechaFin = new Date(cita.fechaHoraFin).toLocaleTimeString('es-CL', {
      timeStyle: 'short'
    });

    return `
      <div class="panel" style="border-left: 4px solid var(--warning); padding: 20px;">
        <div class="row between" style="margin-bottom: 12px;">
          <div>
            <span class="badge-estado PENDIENTE" style="font-size: 10px;">Solicitud Pendiente de Confirmar</span>
            <span style="font-size: 11px; color: var(--muted); margin-left: 8px;">Código: <strong>${cita.codigoReserva}</strong></span>
          </div>
          <span style="font-weight: 700; font-size: 16px; color: var(--primary); font-family: var(--display);">$${precio}</span>
        </div>

        <div class="grid g2" style="gap: 16px; margin-bottom: 16px; align-items: start;">
          <!-- Datos de la Clienta -->
          <div>
            <div style="font-size: 11px; text-transform: uppercase; letter-spacing: 0.5px; color: var(--muted); font-weight: 700;">Datos de la Clienta</div>
            <div style="font-size: 15px; font-weight: 700; color: var(--fg); margin-top: 2px;">${cliNom}</div>
            <div style="font-size: 12px; color: var(--fg); margin-top: 2px;">📞 Teléfono: <strong>${cliTel}</strong></div>
            <div style="font-size: 12px; color: var(--muted); margin-top: 2px;">✉ Correo: ${cliCorreo}</div>
          </div>

          <!-- Datos de la Atención -->
          <div>
            <div style="font-size: 11px; text-transform: uppercase; letter-spacing: 0.5px; color: var(--muted); font-weight: 700;">Tratamiento Solicitado</div>
            <div style="font-size: 15px; font-weight: 700; color: var(--primary); margin-top: 2px;">${servicioNom}</div>
            <div style="font-size: 12px; color: var(--fg); margin-top: 2px;">📅 ${fechaIni} &rarr; ${fechaFin} (${duracion} min)</div>
            <div style="font-size: 11px; color: var(--muted); font-style: italic; margin-top: 4px;">Preferencias: ${notas}</div>
          </div>
        </div>

        <!-- Botones de Acción para el Trabajador (Requisito 23) -->
        <div class="row between" style="padding-top: 14px; border-top: 1px solid var(--border);">
          <div style="font-size: 11px; color: var(--muted);">
            ¿Te comunicaste con la clienta o verificaste tu turno?
          </div>
          <div class="row" style="gap: 8px;">
            <button type="button" class="btn sm btn-danger" onclick="cancelarCitaTrabajador(${cita.id})" title="Cancelar o rechazar solicitud">
              ✕ Rechazar Cita
            </button>
            <button type="button" class="btn sm" onclick="confirmarCitaTrabajador(${cita.id})" style="background: var(--success); border-color: var(--success);" title="Confirmar formalmente la cita">
              ✓ Confirmar Cita con la Clienta
            </button>
          </div>
        </div>
      </div>
    `;
  }).join('');
}

// Acción: Confirmar Cita por el Trabajador (Requisito 23)
async function confirmarCitaTrabajador(id) {
  try {
    const res = await apiFetch(`${API_BASE}/citas/${id}/estado`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        nuevoEstado: 'CONFIRMADA',
        motivoONotas: 'Cita confirmada telefónicamente por el especialista con la clienta'
      })
    });

    const data = await res.json();
    if (!res.ok) {
      mostrarAlerta(`Error al confirmar cita: ${data.error || 'No se pudo actualizar'}`, true);
      return;
    }

    mostrarAlerta(`¡Cita ${data.codigoReserva} confirmada con éxito! La clienta ha sido registrada en tu agenda.`);
    await cargarCitasTrabajador();
  } catch (err) {
    mostrarAlerta(`Error de conexión: ${err.message}`, true);
  }
}

// Acción: Cancelar Cita
async function cancelarCitaTrabajador(id) {
  const motivo = prompt('Por favor ingresa el motivo del rechazo o cancelación de la cita:') || 'Cancelada por el especialista';
  try {
    const res = await apiFetch(`${API_BASE}/citas/${id}/estado`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        nuevoEstado: 'CANCELADA',
        motivoONotas: motivo
      })
    });

    if (!res.ok) {
      const err = await res.json();
      mostrarAlerta(`Error: ${err.error || 'No se pudo cancelar'}`, true);
      return;
    }

    mostrarAlerta('La cita ha sido cancelada correctamente.');
    await cargarCitasTrabajador();
  } catch (err) {
    mostrarAlerta(`Error: ${err.message}`, true);
  }
}

// Acción: Cambiar estado a EN_ATENCION o COMPLETADA
async function cambiarEstadoGeneral(id, nuevoEstado) {
  try {
    const res = await apiFetch(`${API_BASE}/citas/${id}/estado`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        nuevoEstado: nuevoEstado,
        motivoONotas: `Atención marcada como ${nuevoEstado}`
      })
    });

    if (!res.ok) {
      const err = await res.json();
      mostrarAlerta(`Error: ${err.error || 'No se pudo cambiar estado'}`, true);
      return;
    }

    mostrarAlerta(`Estado de la cita actualizado a ${nuevoEstado}`);
    await cargarCitasTrabajador();
  } catch (err) {
    mostrarAlerta(`Error: ${err.message}`, true);
  }
}

// ----------------------------------------------------
// VISTA 2: CITAS CONFIRMADAS
// ----------------------------------------------------
function renderizarCitasConfirmadas(confirmadas) {
  if (!confirmadas || confirmadas.length === 0) {
    tablaConfirmadasBody.innerHTML = `
      <tr>
        <td colspan="7" style="text-align: center; color: var(--muted); padding: 2rem;">
          No hay citas confirmadas actualmente.
        </td>
      </tr>
    `;
    return;
  }

  tablaConfirmadasBody.innerHTML = confirmadas.map(cita => {
    const cliNom = cita.cliente?.usuario ? 
      `${cita.cliente.usuario.nombre} ${cita.cliente.usuario.apellidoP}` : `Cliente #${cita.cliente?.id}`;
    const cliTel = cita.cliente?.telefono || 'Sin teléfono';
    const fechaIni = new Date(cita.fechaHoraInicio).toLocaleString('es-CL', {
      dateStyle: 'short',
      timeStyle: 'short'
    });

    const esEnAtencion = cita.estado === 'EN_ATENCION';

    return `
      <tr>
        <td><strong>${cita.codigoReserva}</strong></td>
        <td><strong>${cliNom}</strong></td>
        <td><span style="font-size:11px;">📞 ${cliTel}</span></td>
        <td>${cita.servicio?.nombre || 'Servicio'}</td>
        <td>${fechaIni}</td>
        <td><span class="badge-estado ${cita.estado}">${cita.estado}</span></td>
        <td style="text-align: right;">
          <div class="btn-action-group" style="justify-content: flex-end;">
            ${!esEnAtencion ? `
              <button class="btn sm" onclick="cambiarEstadoGeneral(${cita.id}, 'EN_ATENCION')" style="background:#6d28d9; border-color:#6d28d9;" title="Iniciar sesión de atención">
                Iniciar
              </button>
            ` : `
              <button class="btn sm" onclick="cambiarEstadoGeneral(${cita.id}, 'COMPLETADA')" style="background:var(--success); border-color:var(--success);" title="Marcar como finalizada">
                Finalizar
              </button>
            `}
            <button class="btn sm btn-danger" onclick="cancelarCitaTrabajador(${cita.id})" title="Cancelar">
              ✕
            </button>
          </div>
        </td>
      </tr>
    `;
  }).join('');
}

// ----------------------------------------------------
// VISTA 3: TODAS LAS CITAS
// ----------------------------------------------------
function renderizarTodasLasCitas(citas) {
  if (!citas || citas.length === 0) {
    tablaTodasBody.innerHTML = `
      <tr>
        <td colspan="6" style="text-align: center; color: var(--muted); padding: 2rem;">
          No hay historial de citas para este trabajador.
        </td>
      </tr>
    `;
    return;
  }

  tablaTodasBody.innerHTML = citas.map(cita => {
    const cliNom = cita.cliente?.usuario ? 
      `${cita.cliente.usuario.nombre} ${cita.cliente.usuario.apellidoP}` : `Cliente #${cita.cliente?.id}`;
    const fechaIni = new Date(cita.fechaHoraInicio).toLocaleString('es-CL', {
      dateStyle: 'short',
      timeStyle: 'short'
    });
    const precio = Number(cita.precioFinal).toLocaleString('es-CL');

    return `
      <tr>
        <td><strong>${cita.codigoReserva}</strong></td>
        <td>${cliNom}</td>
        <td>${cita.servicio?.nombre || 'Servicio'}</td>
        <td>${fechaIni}</td>
        <td><strong>$${precio}</strong></td>
        <td><span class="badge-estado ${cita.estado}">${cita.estado}</span></td>
      </tr>
    `;
  }).join('');
}

// Alternar Pestañas del Portal
function cambiarTabPortal(tab) {
  tabPendientes.classList.remove('active');
  tabConfirmadas.classList.remove('active');
  tabTodas.classList.remove('active');

  vistaCitasPendientes.style.display = 'none';
  vistaCitasConfirmadas.style.display = 'none';
  vistaTodasCitas.style.display = 'none';

  if (tab === 'pendientes') {
    tabPendientes.classList.add('active');
    vistaCitasPendientes.style.display = 'block';
  } else if (tab === 'confirmadas') {
    tabConfirmadas.classList.add('active');
    vistaCitasConfirmadas.style.display = 'block';
  } else if (tab === 'todas') {
    tabTodas.classList.add('active');
    vistaTodasCitas.style.display = 'block';
  }
}
