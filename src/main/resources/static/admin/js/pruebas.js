// Consola de pruebas y diagnóstico

var API_BASE = window.API_BASE || '/api/v2/nicolet';
const apiFetch = (url, opts) => (window.authenticatedFetch ? window.authenticatedFetch(url, opts) : fetch(url, opts));

let trabajadores = [];
let servicios = [];

document.addEventListener('DOMContentLoaded', () => {
  if (typeof requireAuth === 'function' && !requireAuth()) return;

  inicializarFechaPorDefecto();
  verificarConexion();
  cargarCatalogos();

  document.getElementById('formTestDisponibilidad').addEventListener('submit', probarDisponibilidad);
  document.getElementById('btnEjecutarHealthCheck').addEventListener('click', ejecutarDiagnosticoEndpoints);
});

function inicializarFechaPorDefecto() {
  const inputFecha = document.getElementById('testFecha');
  const manana = new Date();
  manana.setDate(manana.getDate() + 1);
  // Formato YYYY-MM-DD
  inputFecha.value = manana.toISOString().split('T')[0];
  inputFecha.min = new Date().toISOString().split('T')[0];
}

async function verificarConexion() {
  const badge = document.getElementById('statusBadge');
  const text = document.getElementById('statusText');
  try {
    const res = await apiFetch(`${API_BASE}/servicios`);
    if (res.ok) {
      badge.className = 'status online';
      text.textContent = 'API Conectada (8080)';
    } else {
      badge.className = 'status offline';
      text.textContent = `Error HTTP ${res.status}`;
    }
  } catch (err) {
    badge.className = 'status offline';
    text.textContent = 'Sin conexión';
  }
}

async function cargarCatalogos() {
  try {
    const [resTrabajadores, resServicios] = await Promise.all([
      apiFetch(`${API_BASE}/trabajadores`),
      apiFetch(`${API_BASE}/servicios`)
    ]);

    if (resTrabajadores.ok) {
      trabajadores = await resTrabajadores.json();
      const selectTrabajador = document.getElementById('testTrabajador');
      if (trabajadores.length === 0) {
        selectTrabajador.innerHTML = '<option value="" disabled selected>No hay especialistas activos</option>';
      } else {
        selectTrabajador.innerHTML = trabajadores.map(t => {
          const nom = t.usuario ? `${t.usuario.nombre || ''} ${t.usuario.apellidoP || ''}`.trim() : `Especialista #${t.id}`;
          const cargo = t.cargoEspecialidad ? ` · ${t.cargoEspecialidad}` : '';
          return `<option value="${t.id}">${nom}${cargo}</option>`;
        }).join('');
      }
    }

    if (resServicios.ok) {
      servicios = await resServicios.json();
      const selectServicio = document.getElementById('testServicio');
      selectServicio.innerHTML = '<option value="">Todos / Predeterminado (45 min)</option>' + 
        servicios.map(s => 
          `<option value="${s.id}">${s.nombre} (${s.duracionMinutos} min - $${Number(s.precio).toLocaleString('es-CL')})</option>`
        ).join('');
    }
  } catch (e) {
    console.error('Error cargando catálogos de prueba:', e);
  }
}

async function probarDisponibilidad(e) {
  e.preventDefault();
  const trabajadorSelect = document.getElementById('testTrabajador');
  const fechaInput = document.getElementById('testFecha');
  const servicioSelect = document.getElementById('testServicio');

  const trabajadorId = trabajadorSelect.value;
  const fecha = fechaInput.value;
  const servicioId = servicioSelect.value;

  const contenedorResultados = document.getElementById('resultadoSlots');
  const jsonViewer = document.getElementById('rawJsonViewer');
  const endpointUrlEl = document.getElementById('endpointEjecutado');
  const resumenConteoEl = document.getElementById('conteoSlots');

  if (!trabajadorId || !fecha) {
    contenedorResultados.innerHTML = '<div class="alert-banner error" style="display:block;">Por favor selecciona un especialista y una fecha para consultar.</div>';
    return;
  }

  const url = `${API_BASE}/disponibilidad?trabajadorId=${trabajadorId}&fecha=${fecha}${servicioId ? `&servicioId=${servicioId}` : ''}`;
  endpointUrlEl.textContent = `GET ${url}`;
  contenedorResultados.innerHTML = '<div style="padding: 1.5rem; color: var(--muted); text-align: center;">Calculando bloques de disponibilidad en backend...</div>';

  const startTime = performance.now();
  try {
    const res = await apiFetch(url);
    const elapsed = Math.round(performance.now() - startTime);
    const data = await res.json();

    jsonViewer.textContent = JSON.stringify(data, null, 2);

    if (!res.ok) {
      contenedorResultados.innerHTML = `<div class="alert-banner error" style="display:block;">Error ${res.status}: ${JSON.stringify(data)}</div>`;
      return;
    }

    if (!data || data.length === 0) {
      resumenConteoEl.textContent = `0 bloques (Día no laboral o sin turnos) · ${elapsed}ms`;
      contenedorResultados.innerHTML = '<div style="padding: 1.5rem; text-align: center; color: var(--muted); background: var(--secondary); border-radius: 6px;">El especialista no atiende en el día seleccionado o no tiene turnos configurados.</div>';
      return;
    }

    const disponibles = data.filter(s => s.disponible).length;
    const ocupados = data.filter(s => !s.disponible).length;
    resumenConteoEl.textContent = `${data.length} bloques totales (${disponibles} libres, ${ocupados} no disponibles) · Latencia: ${elapsed}ms`;

    contenedorResultados.innerHTML = data.map(slot => {
      const horaIni = slot.horaInicio ? slot.horaInicio.slice(0, 5) : '';
      const horaFin = slot.horaFin ? slot.horaFin.slice(0, 5) : '';
      const esLibre = slot.disponible;
      const motivo = slot.motivoNoDisponible || 'LIBRE';
      
      let badgeClass = 'badge-estado CONFIRMADA'; // Verde
      let bgCard = '#f0fdf4';
      let borderColor = '#86efac';

      if (!esLibre) {
        if (motivo === 'DESCANSO') {
          badgeClass = 'badge-estado PENDIENTE'; // Amarillo/Naranja
          bgCard = '#fffbeb';
          borderColor = '#fde68a';
        } else if (motivo === 'HORA_PASADA') {
          badgeClass = 'badge-estado NO_ASISTIO'; // Gris
          bgCard = '#f9fafb';
          borderColor = '#e5e7eb';
        } else {
          badgeClass = 'badge-estado CANCELADA'; // Rojo
          bgCard = '#fef2f2';
          borderColor = '#fca5a5';
        }
      }

      return `
        <div style="background:${bgCard}; border: 1px solid ${borderColor}; border-radius: 8px; padding: 0.75rem 1rem; display: flex; justify-content: space-between; align-items: center;">
          <div>
            <div style="font-weight: 700; font-size: 1.05rem; color: var(--dark);">${horaIni} - ${horaFin}</div>
            <div style="font-size: 0.75rem; color: var(--gray); margin-top: 2px;">Duración calculada</div>
          </div>
          <div style="text-align: right;">
            <span class="${badgeClass}">${esLibre ? 'DISPONIBLE' : motivo}</span>
          </div>
        </div>
      `;
    }).join('');

  } catch (err) {
    contenedorResultados.innerHTML = `<div class="alert-banner error" style="display:block;">Error de red al consultar el endpoint: ${err.message}</div>`;
  }
}

async function ejecutarDiagnosticoEndpoints() {
  const endpoints = [
    { name: 'Catálogo de Servicios', url: `${API_BASE}/servicios`, publico: true },
    { name: 'Lista de Profesionales', url: `${API_BASE}/trabajadores`, publico: true },
    { name: 'Cálculo de Disponibilidad', url: `${API_BASE}/disponibilidad?trabajadorId=1&fecha=2026-10-15`, publico: true },
    { name: 'Gestión de Citas (Auth Requerida)', url: `${API_BASE}/citas`, publico: false },
    { name: 'Gestión de Usuarios (Auth Requerida)', url: `${API_BASE}/usuarios`, publico: false },
    { name: 'Spring Actuator Health', url: `/actuator/health`, publico: true }
  ];

  const tbody = document.getElementById('diagnosticoTableBody');
  tbody.innerHTML = '<tr><td colspan="4" style="text-align:center; padding: 1rem;">Ejecutando sondeo de endpoints...</td></tr>';

  const rows = [];
  for (const ep of endpoints) {
    const start = performance.now();
    try {
      const res = await apiFetch(ep.url);
      const latency = Math.round(performance.now() - start);
      const isOk = res.ok || (!ep.publico && res.status === 401 && !localStorage.getItem('token'));
      const statusText = res.ok ? `${res.status} OK` : `${res.status} ${res.statusText}`;

      rows.push(`
        <tr>
          <td><strong>${ep.name}</strong><br><span style="font-size:0.75rem; color: var(--gray);">${ep.url}</span></td>
          <td>${ep.publico ? '<span class="badge-estado COMPLETADA">Público (PermitAll)</span>' : '<span class="badge-estado EN_ATENCION">Protegido JWT</span>'}</td>
          <td><span class="badge-estado ${res.ok ? 'CONFIRMADA' : 'CANCELADA'}">${statusText}</span></td>
          <td><span style="font-weight: 600; color: ${latency < 200 ? 'var(--success)' : 'var(--dark)'};">${latency} ms</span></td>
        </tr>
      `);
    } catch (e) {
      rows.push(`
        <tr>
          <td><strong>${ep.name}</strong><br><span style="font-size:0.75rem; color: var(--gray);">${ep.url}</span></td>
          <td>${ep.publico ? 'Público' : 'Protegido'}</td>
          <td><span class="badge-estado CANCELADA">Inaccesible</span></td>
          <td>-</td>
        </tr>
      `);
    }
  }

  tbody.innerHTML = rows.join('');
}
