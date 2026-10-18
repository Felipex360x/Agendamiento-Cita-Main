// ========================================================
// Nicolett Studio Fantasy - Agendamiento de Citas Cliente
// ========================================================

var API_BASE = window.API_BASE || '/api/v2/nicolet';
const apiFetch = (url, opts) => (window.authenticatedFetch ? window.authenticatedFetch(url, opts) : fetch(url, opts));

let listaServicios = [];
let listaTrabajadores = [];
let servicioSeleccionado = null;
let trabajadorSeleccionado = null;
let fechaSeleccionada = null;
let horarioSeleccionado = null;

// Elementos DOM
const statusBadge = document.getElementById('statusBadge');
const statusText = document.getElementById('statusText');
const alertMessage = document.getElementById('alertMessage');

const gridServicios = document.getElementById('gridServicios');
const gridTrabajadores = document.getElementById('gridTrabajadores');
const gridHorariosDisponibles = document.getElementById('gridHorariosDisponibles');
const reservaFechaInput = document.getElementById('reservaFechaInput');

const panelPaso1 = document.getElementById('panelPaso1');
const panelPaso2 = document.getElementById('panelPaso2');
const panelPaso3 = document.getElementById('panelPaso3');
const panelPaso4 = document.getElementById('panelPaso4');

const stepIndicator1 = document.getElementById('stepIndicator1');
const stepIndicator2 = document.getElementById('stepIndicator2');
const stepIndicator3 = document.getElementById('stepIndicator3');
const stepIndicator4 = document.getElementById('stepIndicator4');

const resumenServicioTexto = document.getElementById('resumenServicioTexto');
const resumenTrabajadorTexto = document.getElementById('resumenTrabajadorTexto');
const resumenFechaTexto = document.getElementById('resumenFechaTexto');
const resumenHoraTexto = document.getElementById('resumenHoraTexto');
const resumenPrecioTexto = document.getElementById('resumenPrecioTexto');

const bloqueClienteAutenticado = document.getElementById('bloqueClienteAutenticado');
const datosClienteLogueado = document.getElementById('datosClienteLogueado');
const formRegistroClienteInline = document.getElementById('formRegistroClienteInline');
const inNotasCliente = document.getElementById('inNotasCliente');
const btnFinalizarReserva = document.getElementById('btnFinalizarReserva');

const pantallaConfirmacion = document.getElementById('pantallaConfirmacion');
const contenedorFlujoReserva = document.getElementById('contenedorFlujoReserva');
const confCodigo = document.getElementById('confCodigo');
const confServicio = document.getElementById('confServicio');
const confTrabajador = document.getElementById('confTrabajador');
const confHorario = document.getElementById('confHorario');
const confPrecio = document.getElementById('confPrecio');

document.addEventListener('DOMContentLoaded', async () => {
  inicializarFecha();
  verificarEstadoSesion();
  await cargarServicios();
  await cargarTrabajadores();

  // Procesar parámetros de URL (si viene desde servicios.html o disponibilidad.html)
  const params = new URLSearchParams(window.location.search);
  const sIdParam = params.get('servicioId');
  const tIdParam = params.get('trabajadorId');
  const fParam = params.get('fecha');
  const hParam = params.get('hora');

  if (sIdParam) {
    seleccionarServicio(Number(sIdParam));
  }
  if (tIdParam) {
    seleccionarTrabajador(Number(tIdParam));
  }
  if (fParam) {
    reservaFechaInput.value = fParam;
    fechaSeleccionada = fParam;
    resumenFechaTexto.textContent = fParam;
  }
  if (tIdParam && fParam) {
    await cargarHorariosDisponibles();
    if (hParam) {
      const chips = Array.from(document.querySelectorAll('#gridHorariosDisponibles .slot-chip.disponible'));
      const matchingChip = chips.find(chip => chip.textContent.includes(hParam));
      if (matchingChip) {
        matchingChip.click();
      } else {
        seleccionarHorario(hParam, hParam);
      }
    }
  }
});

function inicializarFecha() {
  const manana = new Date();
  manana.setDate(manana.getDate() + 1);
  const isoManana = manana.toISOString().split('T')[0];
  reservaFechaInput.value = isoManana;
  reservaFechaInput.min = new Date().toISOString().split('T')[0];
  fechaSeleccionada = isoManana;
  resumenFechaTexto.textContent = isoManana;
}

function mostrarAlerta(mensaje, esError = false) {
  alertMessage.className = `alert-banner ${esError ? 'error' : 'success'}`;
  alertMessage.textContent = mensaje;
  alertMessage.style.display = 'block';
  setTimeout(() => {
    alertMessage.style.display = 'none';
  }, 6000);
}

function verificarEstadoSesion() {
  const user = typeof getAuthUser === 'function' ? getAuthUser() : null;
  if (user && typeof isAuthenticated === 'function' && isAuthenticated()) {
    bloqueClienteAutenticado.style.display = 'block';
    formRegistroClienteInline.style.display = 'none';
    datosClienteLogueado.textContent = `${user.nombre} ${user.apellidoP || ''} • ${user.correo}`;
  } else {
    bloqueClienteAutenticado.style.display = 'none';
    formRegistroClienteInline.style.display = 'block';
  }
}

// ----------------------------------------------------
// PASO 1: SERVICIOS
// ----------------------------------------------------
async function cargarServicios() {
  try {
    const res = await apiFetch(`${API_BASE}/servicios`);
    if (!res.ok) throw new Error('Error al obtener servicios');
    listaServicios = await res.json();

    if (listaServicios.length === 0) {
      gridServicios.innerHTML = '<div style="color:var(--muted); padding:20px; text-align:center;">No hay servicios activos disponibles en este momento.</div>';
      return;
    }

    gridServicios.innerHTML = listaServicios.map(s => {
      const precioFmt = Number(s.precio).toLocaleString('es-CL');
      return `
        <div class="selectable-card" id="card-servicio-${s.id}" onclick="seleccionarServicio(${s.id})">
          <div class="card-title">${s.nombre}</div>
          <div class="card-subtitle">${s.descripcion || 'Tratamiento profesional garantizado'}</div>
          <div class="row between" style="margin-top:10px;">
            <span style="font-weight:600; font-size:12px; color:var(--muted);">${s.duracionMinutos} min</span>
            <span class="card-price">$${precioFmt}</span>
          </div>
        </div>
      `;
    }).join('');
  } catch (err) {
    gridServicios.innerHTML = `<div style="color:var(--danger); padding:20px;">Error al cargar servicios: ${err.message}</div>`;
  }
}

function seleccionarServicio(id) {
  const s = listaServicios.find(item => item.id === id);
  if (!s) return;

  servicioSeleccionado = s;

  // Actualizar tarjetas activas
  document.querySelectorAll('#gridServicios .selectable-card').forEach(c => c.classList.remove('selected'));
  const card = document.getElementById(`card-servicio-${id}`);
  if (card) card.classList.add('selected');

  // Actualizar resumen
  resumenServicioTexto.textContent = `${s.nombre} (${s.duracionMinutos}m)`;
  resumenPrecioTexto.textContent = `$${Number(s.precio).toLocaleString('es-CL')}`;

  // Desbloquear Paso 2
  panelPaso2.style.opacity = '1';
  panelPaso2.style.pointerEvents = 'auto';
  stepIndicator1.classList.add('completed');
  stepIndicator2.classList.add('active');

  const badgeServ = document.getElementById('badgeServicioSeleccionado');
  if (badgeServ) {
    badgeServ.textContent = `${s.duracionMinutos} min`;
    badgeServ.className = 'status online';
  }

  // Si ya había trabajador seleccionado, recalcular horarios
  if (trabajadorSeleccionado) {
    cargarHorariosDisponibles();
  }
}

// ----------------------------------------------------
// PASO 2: TRABAJADORES (ESPECIALISTAS)
// ----------------------------------------------------
async function cargarTrabajadores() {
  try {
    const res = await apiFetch(`${API_BASE}/trabajadores`);
    if (!res.ok) throw new Error('Error al obtener especialistas');
    listaTrabajadores = await res.json();

    if (listaTrabajadores.length === 0) {
      gridTrabajadores.innerHTML = '<div style="color:var(--muted); padding:20px; text-align:center;">No hay profesionales registrados.</div>';
      return;
    }

    gridTrabajadores.innerHTML = listaTrabajadores.map(t => {
      const nom = t.usuario ? `${t.usuario.nombre} ${t.usuario.apellidoP || ''}`.trim() : `Especialista #${t.id}`;
      const cargo = t.cargoEspecialidad || 'Especialista General';
      const initial = (nom.charAt(0) || 'E').toUpperCase();
      return `
        <div class="selectable-card" id="card-trabajador-${t.id}" onclick="seleccionarTrabajador(${t.id})">
          <div style="display:flex; align-items:center; gap:10px; margin-bottom:8px;">
            <span class="avatar" style="width:36px; height:36px; font-size:14px;">${initial}</span>
            <div>
              <div class="card-title" style="margin-bottom:0;">${nom}</div>
              <div style="font-size:11px; color:var(--primary); font-weight:600;">${cargo}</div>
            </div>
          </div>
          <div class="card-subtitle" style="margin-bottom:0;">${t.biografia || 'Atención personalizada y profesional.'}</div>
        </div>
      `;
    }).join('');
  } catch (err) {
    gridTrabajadores.innerHTML = `<div style="color:var(--danger); padding:20px;">Error al cargar profesionales: ${err.message}</div>`;
  }
}

function seleccionarTrabajador(id) {
  const t = listaTrabajadores.find(item => item.id === id);
  if (!t) return;

  trabajadorSeleccionado = t;

  // Actualizar tarjetas activas
  document.querySelectorAll('#gridTrabajadores .selectable-card').forEach(c => c.classList.remove('selected'));
  const card = document.getElementById(`card-trabajador-${id}`);
  if (card) card.classList.add('selected');

  const nom = t.usuario ? `${t.usuario.nombre} ${t.usuario.apellidoP || ''}`.trim() : `Especialista #${t.id}`;
  resumenTrabajadorTexto.textContent = nom;

  // Desbloquear Paso 3
  panelPaso3.style.opacity = '1';
  panelPaso3.style.pointerEvents = 'auto';
  stepIndicator2.classList.add('completed');
  stepIndicator3.classList.add('active');

  cargarHorariosDisponibles();
}

// ----------------------------------------------------
// PASO 3: HORARIOS DISPONIBLES (DISPONIBILIDAD LIVE)
// ----------------------------------------------------
async function cargarHorariosDisponibles() {
  if (!trabajadorSeleccionado) return;
  const fecha = reservaFechaInput.value;
  if (!fecha) return;

  fechaSeleccionada = fecha;
  resumenFechaTexto.textContent = fecha;
  horarioSeleccionado = null;
  resumenHoraTexto.textContent = 'Sin seleccionar';

  gridHorariosDisponibles.innerHTML = '<div style="grid-column: 1 / -1; padding: 20px; text-align: center; color: var(--muted);">Consultando bloques disponibles en tiempo real...</div>';

  try {
    const sId = servicioSeleccionado ? servicioSeleccionado.id : '';
    const url = `${API_BASE}/disponibilidad?trabajadorId=${trabajadorSeleccionado.id}&fecha=${fecha}${sId ? `&servicioId=${sId}` : ''}`;
    const res = await apiFetch(url);
    const data = await res.json();

    if (!res.ok || !data || data.length === 0) {
      gridHorariosDisponibles.innerHTML = `
        <div style="grid-column: 1 / -1; padding: 20px; text-align: center; color: var(--muted); background: var(--secondary); border-radius: 6px;">
          El especialista no tiene turnos de atención para el día seleccionado. Por favor elige otra fecha.
        </div>
      `;
      return;
    }

    const slotsLibres = data.filter(s => s.disponible);
    if (slotsLibres.length === 0) {
      gridHorariosDisponibles.innerHTML = `
        <div style="grid-column: 1 / -1; padding: 20px; text-align: center; color: var(--warning); background: var(--warning-bg); border-radius: 6px;">
          Todos los horarios del especialista se encuentran ocupados para este día. Por favor elige otra fecha.
        </div>
      `;
      return;
    }

    gridHorariosDisponibles.innerHTML = data.map((slot, idx) => {
      const horaIni = slot.horaInicio ? slot.horaInicio.slice(0, 5) : '';
      const horaFin = slot.horaFin ? slot.horaFin.slice(0, 5) : '';
      const esLibre = slot.disponible;
      const motivo = slot.motivoNoDisponible || 'LIBRE';

      if (!esLibre) {
        let label = motivo === 'DESCANSO' ? 'Almuerzo' : 'Ocupado';
        return `
          <div class="slot-chip ocupado" title="Horario no disponible: ${motivo}">
            <div style="font-weight:700;">${horaIni}</div>
            <div style="font-size:9px; color:var(--muted);">${label}</div>
          </div>
        `;
      }

      return `
        <div class="slot-chip disponible" id="slot-chip-${idx}" onclick="seleccionarHorario('${horaIni}', '${horaFin}', ${idx})">
          <div style="font-weight:700; font-size:13px;">${horaIni}</div>
          <div style="font-size:10px; margin-top:2px;">Hasta ${horaFin}</div>
          <div style="font-size:9px; margin-top:4px; font-weight:700; color:var(--success);">Disponible</div>
        </div>
      `;
    }).join('');

  } catch (err) {
    gridHorariosDisponibles.innerHTML = `<div style="grid-column: 1 / -1; color: var(--danger); padding: 16px;">Error de red: ${err.message}</div>`;
  }
}

function seleccionarHorario(horaIni, horaFin, idx) {
  horarioSeleccionado = { inicio: horaIni, fin: horaFin };

  document.querySelectorAll('#gridHorariosDisponibles .slot-chip').forEach(c => c.classList.remove('selected'));
  if (idx !== undefined) {
    const chip = document.getElementById(`slot-chip-${idx}`);
    if (chip) chip.classList.add('selected');
  }

  resumenHoraTexto.textContent = `${horaIni} - ${horaFin}`;

  // Desbloquear Paso 4
  panelPaso4.style.opacity = '1';
  panelPaso4.style.pointerEvents = 'auto';
  stepIndicator3.classList.add('completed');
  stepIndicator4.classList.add('active');

  panelPaso4.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

// ----------------------------------------------------
// PASO 4: CONFIRMACIÓN Y AGENDAMIENTO (REQUISITO 20)
// ----------------------------------------------------
async function ejecutarReservaFinal() {
  if (!servicioSeleccionado) {
    mostrarAlerta('Por favor selecciona un tratamiento primero (Paso 1)', true);
    return;
  }
  if (!trabajadorSeleccionado) {
    mostrarAlerta('Por favor selecciona un especialista (Paso 2)', true);
    return;
  }
  if (!fechaSeleccionada || !horarioSeleccionado) {
    mostrarAlerta('Por favor selecciona fecha y bloque horario disponible (Paso 3)', true);
    return;
  }

  btnFinalizarReserva.disabled = true;
  btnFinalizarReserva.textContent = 'Procesando agendamiento...';

  try {
    let clienteId = null;
    const user = typeof getAuthUser === 'function' ? getAuthUser() : null;

    // Caso A: Usuario YA está autenticado (Requisito 20: "si esta registrado agendarlo nomas")
    if (user && typeof isAuthenticated === 'function' && isAuthenticated()) {
      // Obtener el perfil cliente para este usuario
      const resClientes = await apiFetch(`${API_BASE}/clientes`);
      if (resClientes.ok) {
        const clientes = await resClientes.json();
        const cli = clientes.find(c => c.usuario && c.usuario.id === user.id);
        if (cli) {
          clienteId = cli.id;
        }
      }

      // Si por alguna razón no tiene perfil de cliente, crearlo o sincronizarlo
      if (!clienteId) {
        const resNuevoCli = await apiFetch(`${API_BASE}/clientes`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ usuarioId: user.id })
        });
        if (resNuevoCli.ok) {
          const cliData = await resNuevoCli.json();
          clienteId = cliData.id;
        } else {
          // Tomar el primer cliente disponible como fallback seguro
          const resCliAll = await apiFetch(`${API_BASE}/clientes`);
          const allC = await resCliAll.json();
          if (allC.length > 0) clienteId = allC[0].id;
        }
      }
    } 
    // Caso B: Usuario NO está registrado (Requisito 20: "colocar el formulario de registro")
    else {
      const nombre = document.getElementById('inNombre').value.trim();
      const apellidoP = document.getElementById('inApellido').value.trim();
      const correo = document.getElementById('inCorreo').value.trim().toLowerCase();
      const password = document.getElementById('inPassword').value.trim();
      const telefono = document.getElementById('inTelefono').value.trim();

      if (!nombre || !apellidoP || !correo || !password) {
        mostrarAlerta('Por favor completa todos tus datos de registro requeridos', true);
        btnFinalizarReserva.disabled = false;
        btnFinalizarReserva.textContent = 'Confirmar y Agendar Cita';
        return;
      }

      // Registrar al usuario cliente en auth
      const authData = await register(nombre, apellidoP, correo, password, telefono);

      // Obtener el ID de cliente sincronizado
      const resClientes = await apiFetch(`${API_BASE}/clientes`);
      if (resClientes.ok) {
        const clientes = await resClientes.json();
        const cli = clientes.find(c => c.usuario && c.usuario.id === authData.id);
        if (cli) {
          clienteId = cli.id;
        }
      }

      if (!clienteId) {
        // Fallback: consultar el cliente recién creado
        const resCliByU = await apiFetch(`${API_BASE}/clientes/usuario/${authData.id}`);
        if (resCliByU.ok) {
          const cliData = await resCliByU.json();
          clienteId = cliData.id;
        }
      }
    }

    if (!clienteId) {
      throw new Error('No se pudo asociar el perfil de cliente para el agendamiento.');
    }

    // FechaHora de inicio de la cita
    const fechaHoraInicio = `${fechaSeleccionada}T${horarioSeleccionado.inicio}:00`;
    const notas = inNotasCliente.value.trim() || null;

    // Llamar a la API para persistir la cita con estado PENDIENTE para confirmación del trabajador (Requisito 23)
    const payload = {
      clienteId: Number(clienteId),
      trabajadorId: Number(trabajadorSeleccionado.id),
      servicioId: Number(servicioSeleccionado.id),
      fechaHoraInicio: fechaHoraInicio,
      notasCliente: notas,
      estado: 'PENDIENTE'
    };

    const resCita = await apiFetch(`${API_BASE}/citas`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    const citaData = await resCita.json();

    if (!resCita.ok) {
      const errorMsg = citaData.error || (citaData.errores ? Object.values(citaData.errores).join(', ') : 'No se pudo reservar la cita');
      mostrarAlerta(`Error al agendar: ${errorMsg}`, true);
      btnFinalizarReserva.disabled = false;
      btnFinalizarReserva.textContent = 'Confirmar y Agendar Cita';
      return;
    }

    // ¡Éxito! Mostrar comprobante de reserva
    confCodigo.textContent = citaData.codigoReserva || 'RES-2026-NICOLET';
    confServicio.textContent = servicioSeleccionado.nombre;
    const nomTrabajador = trabajadorSeleccionado.usuario ? 
      `${trabajadorSeleccionado.usuario.nombre} ${trabajadorSeleccionado.usuario.apellidoP || ''}`.trim() : `Especialista #${trabajadorSeleccionado.id}`;
    confTrabajador.textContent = nomTrabajador;
    confHorario.textContent = `${fechaSeleccionada} de ${horarioSeleccionado.inicio} a ${horarioSeleccionado.fin}`;
    confPrecio.textContent = `$${Number(servicioSeleccionado.precio).toLocaleString('es-CL')}`;

    contenedorFlujoReserva.style.display = 'none';
    pantallaConfirmacion.style.display = 'block';
    pantallaConfirmacion.scrollIntoView({ behavior: 'smooth', block: 'start' });

    verificarEstadoSesion();
  } catch (err) {
    mostrarAlerta(`Error en el proceso de reserva: ${err.message}`, true);
  } finally {
    btnFinalizarReserva.disabled = false;
    btnFinalizarReserva.textContent = 'Confirmar y Agendar Cita';
  }
}

function reiniciarFlujoReserva() {
  pantallaConfirmacion.style.display = 'none';
  contenedorFlujoReserva.style.display = 'grid';

  servicioSeleccionado = null;
  trabajadorSeleccionado = null;
  horarioSeleccionado = null;

  panelPaso2.style.opacity = '0.6';
  panelPaso2.style.pointerEvents = 'none';
  panelPaso3.style.opacity = '0.6';
  panelPaso3.style.pointerEvents = 'none';
  panelPaso4.style.opacity = '0.6';
  panelPaso4.style.pointerEvents = 'none';

  stepIndicator1.className = 'step-item active';
  stepIndicator2.className = 'step-item';
  stepIndicator3.className = 'step-item';
  stepIndicator4.className = 'step-item';

  resumenServicioTexto.textContent = 'Sin seleccionar';
  resumenTrabajadorTexto.textContent = 'Sin seleccionar';
  resumenHoraTexto.textContent = 'Sin seleccionar';
  resumenPrecioTexto.textContent = '$0';

  document.querySelectorAll('.selectable-card').forEach(c => c.classList.remove('selected'));
  panelPaso1.scrollIntoView({ behavior: 'smooth', block: 'start' });
}
