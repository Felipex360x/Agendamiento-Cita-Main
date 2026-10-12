// ========================================================
// Nicolett Studio Fantasy - Simulador de Disponibilidad
// Conectado con Especialistas / Profesionales (Trabajadores)
// ========================================================

var API_BASE = window.API_BASE || '/api/v2/nicolet';
const apiFetch = (url, opts) => (window.authenticatedFetch ? window.authenticatedFetch(url, opts) : fetch(url, opts));

let listaTrabajadores = [];
let listaServicios = [];
let slotSeleccionado = null;

// Elementos DOM
const statusBadge = document.getElementById('statusBadge');
const statusText = document.getElementById('statusText');
const alertMessage = document.getElementById('alertMessage');
const formSimulador = document.getElementById('formSimulador');
const selectTrabajador = document.getElementById('selectTrabajador');
const inputFecha = document.getElementById('inputFecha');
const selectServicio = document.getElementById('selectServicio');
const resultadoSlots = document.getElementById('resultadoSlots');
const conteoSlots = document.getElementById('conteoSlots');
const endpointEjecutado = document.getElementById('endpointEjecutado');
const rawJsonViewer = document.getElementById('rawJsonViewer');
const perfilEspecialistaCard = document.getElementById('perfilEspecialistaCard');
const espAvatar = document.getElementById('espAvatar');
const espNombre = document.getElementById('espNombre');
const espCargo = document.getElementById('espCargo');
const espBio = document.getElementById('espBio');

const bannerSlotSeleccionado = document.getElementById('bannerSlotSeleccionado');
const slotHoraTexto = document.getElementById('slotHoraTexto');
const btnAgendarGestion = document.getElementById('btnAgendarGestion');
const btnAgendarCliente = document.getElementById('btnAgendarCliente');

document.addEventListener('DOMContentLoaded', async () => {
  if (typeof requireAuth === 'function' && !requireAuth()) return;
  inicializarFechaPorDefecto();
  verificarConexion();
  await cargarCatalogos();

  selectTrabajador.addEventListener('change', mostrarPerfilEspecialista);
  formSimulador.addEventListener('submit', ejecutarCalculoDisponibilidad);
});

function inicializarFechaPorDefecto() {
  const manana = new Date();
  manana.setDate(manana.getDate() + 1);
  const isoFecha = manana.toISOString().split('T')[0];
  inputFecha.value = isoFecha;
  inputFecha.min = new Date().toISOString().split('T')[0];
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

async function cargarCatalogos() {
  try {
    const [resTrab, resServ] = await Promise.all([
      apiFetch(`${API_BASE}/trabajadores`),
      apiFetch(`${API_BASE}/servicios`)
    ]);

    if (resTrab.ok) {
      listaTrabajadores = await resTrab.json();
      if (listaTrabajadores.length === 0) {
        selectTrabajador.innerHTML = '<option value="" disabled selected>No hay especialistas activos</option>';
      } else {
        selectTrabajador.innerHTML = '<option value="" disabled selected>Selecciona un especialista...</option>' +
          listaTrabajadores.map(t => {
            const nom = t.usuario ? `${t.usuario.nombre} ${t.usuario.apellidoP || ''}`.trim() : `Trabajador #${t.id}`;
            const cargo = t.cargoEspecialidad ? ` · ${t.cargoEspecialidad}` : '';
            return `<option value="${t.id}">${nom}${cargo}</option>`;
          }).join('');

        // Preseleccionar el primer trabajador por defecto
        if (listaTrabajadores.length > 0) {
          selectTrabajador.value = listaTrabajadores[0].id;
          mostrarPerfilEspecialista();
        }
      }
    }

    if (resServ.ok) {
      listaServicios = await resServ.json();
      selectServicio.innerHTML = '<option value="">Todos / Predeterminado (45 min)</option>' +
        listaServicios.map(s => 
          `<option value="${s.id}">${s.nombre} (${s.duracionMinutos} min - $${Number(s.precio).toLocaleString('es-CL')})</option>`
        ).join('');
    }
  } catch (e) {
    console.error('Error cargando catálogos:', e);
  }
}

function mostrarPerfilEspecialista() {
  const tId = Number(selectTrabajador.value);
  const t = listaTrabajadores.find(item => item.id === tId);
  if (!t) {
    perfilEspecialistaCard.style.display = 'none';
    return;
  }

  const nom = t.usuario ? `${t.usuario.nombre} ${t.usuario.apellidoP || ''}`.trim() : `Especialista #${t.id}`;
  espNombre.textContent = nom;
  espCargo.textContent = t.cargoEspecialidad || 'Especialista General';
  espBio.textContent = t.biografia || 'Especialista activo de Nicolett Studio Fantasy con turnos semanales regulares.';
  espAvatar.textContent = (nom.charAt(0) || 'E').toUpperCase();
  perfilEspecialistaCard.style.display = 'block';
}

async function ejecutarCalculoDisponibilidad(e) {
  e.preventDefault();
  bannerSlotSeleccionado.style.display = 'none';
  slotSeleccionado = null;

  const trabajadorId = selectTrabajador.value;
  const fecha = inputFecha.value;
  const servicioId = selectServicio.value;

  if (!trabajadorId || !fecha) {
    resultadoSlots.innerHTML = '<div class="alert-banner error" style="display:block;">Selecciona un especialista y una fecha de atención.</div>';
    return;
  }

  const url = `${API_BASE}/disponibilidad?trabajadorId=${trabajadorId}&fecha=${fecha}${servicioId ? `&servicioId=${servicioId}` : ''}`;
  endpointEjecutado.textContent = `GET ${url}`;
  resultadoSlots.innerHTML = '<div style="padding: 24px; color: var(--muted); text-align: center;">Calculando bloques de disponibilidad en backend...</div>';

  const startTime = performance.now();
  try {
    const res = await apiFetch(url);
    const elapsed = Math.round(performance.now() - startTime);
    const data = await res.json();

    rawJsonViewer.textContent = JSON.stringify(data, null, 2);

    if (!res.ok) {
      resultadoSlots.innerHTML = `<div class="alert-banner error" style="display:block;">Error ${res.status}: ${JSON.stringify(data)}</div>`;
      return;
    }

    if (!data || data.length === 0) {
      conteoSlots.textContent = `0 bloques (Día sin turnos o especialista no labora en este día) · ${elapsed}ms`;
      resultadoSlots.innerHTML = `
        <div style="grid-column: 1 / -1; padding: 24px; text-align: center; color: var(--muted); background: var(--secondary); border-radius: 6px;">
          El especialista seleccionado no tiene turnos configurados para el día seleccionado de la semana.
        </div>
      `;
      return;
    }

    const disponibles = data.filter(s => s.disponible).length;
    const noDisponibles = data.length - disponibles;
    conteoSlots.textContent = `${data.length} bloques totales (${disponibles} disponibles, ${noDisponibles} no disponibles) · Latencia: ${elapsed}ms`;

    resultadoSlots.innerHTML = data.map((slot, idx) => {
      const horaIni = slot.horaInicio ? slot.horaInicio.slice(0, 5) : '';
      const horaFin = slot.horaFin ? slot.horaFin.slice(0, 5) : '';
      const esLibre = slot.disponible;
      const motivo = slot.motivoNoDisponible || 'LIBRE';

      let claseChip = 'disponible';
      let textoEstado = 'DISPONIBLE';

      if (!esLibre) {
        if (motivo === 'DESCANSO') {
          claseChip = 'descanso';
          textoEstado = 'Descanso';
        } else if (motivo === 'HORA_PASADA') {
          claseChip = 'pasada';
          textoEstado = 'Hora Pasada';
        } else {
          claseChip = 'ocupado';
          textoEstado = 'Ocupado';
        }
      }

      return `
        <div class="slot-chip ${claseChip}" data-index="${idx}" data-inicio="${horaIni}" data-fin="${horaFin}" data-libre="${esLibre}" onclick="seleccionarSlot(${idx}, '${horaIni}', '${horaFin}', ${esLibre})">
          <div style="font-weight: 700; font-size: 13px;">${horaIni}</div>
          <div style="font-size: 10px; margin-top: 2px;">Hasta ${horaFin}</div>
          <div style="font-size: 9px; margin-top: 4px; text-transform: uppercase; letter-spacing: 0.5px;">${textoEstado}</div>
        </div>
      `;
    }).join('');

  } catch (err) {
    resultadoSlots.innerHTML = `<div class="alert-banner error" style="display:block;">Error de conexión con el backend: ${err.message}</div>`;
  }
}

function seleccionarSlot(idx, horaIni, horaFin, esLibre) {
  if (!esLibre) return;

  // Desmarcar anteriores
  document.querySelectorAll('.slot-chip').forEach(el => el.classList.remove('selected'));
  const chipActual = document.querySelector(`.slot-chip[data-index="${idx}"]`);
  if (chipActual) chipActual.classList.add('selected');

  slotSeleccionado = { inicio: horaIni, fin: horaFin };
  slotHoraTexto.textContent = `${horaIni} a ${horaFin} (${inputFecha.value})`;

  const tId = selectTrabajador.value;
  const sId = selectServicio.value;
  const fecha = inputFecha.value;
  const fechaHoraIni = `${fecha}T${horaIni}:00`;

  btnAgendarGestion.href = `citas.html?trabajadorId=${tId}&fechaHora=${encodeURIComponent(fechaHoraIni)}${sId ? `&servicioId=${sId}` : ''}`;
  btnAgendarCliente.href = `reserva-cliente.html?trabajadorId=${tId}&fecha=${fecha}&hora=${horaIni}${sId ? `&servicioId=${sId}` : ''}`;

  bannerSlotSeleccionado.style.display = 'flex';
  bannerSlotSeleccionado.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}
