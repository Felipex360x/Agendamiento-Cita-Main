// ========================================================
// Nicolett Studio Fantasy - Administración de Servicios
// ========================================================

var API_BASE = window.API_BASE || '/api/v2/nicolet';
const apiFetch = (url, opts) => (window.authenticatedFetch ? window.authenticatedFetch(url, opts) : fetch(url, opts));

let listaServicios = [];

// Elementos DOM
const statusBadge = document.getElementById('statusBadge');
const statusText = document.getElementById('statusText');
const alertMessage = document.getElementById('alertMessage');
const totalServiciosCountEl = document.getElementById('totalServiciosCount');
const duracionPromedioEl = document.getElementById('duracionPromedio');
const precioPromedioEl = document.getElementById('precioPromedio');
const serviciosTableBody = document.getElementById('serviciosTableBody');
const inputBuscar = document.getElementById('inputBuscarServicio');

const servicioForm = document.getElementById('servicioForm');
const serviceFormTitle = document.getElementById('serviceFormTitle');
const serviceFormBadge = document.getElementById('serviceFormBadge');
const editServicioIdInput = document.getElementById('editServicioId');
const serviceEditAlert = document.getElementById('serviceEditAlert');
const serviceEditTargetId = document.getElementById('serviceEditTargetId');
const btnSubmitServicio = document.getElementById('btnSubmitServicio');
const btnCancelServicio = document.getElementById('btnCancelServicio');

const nombreInput = document.getElementById('servicioNombre');
const duracionInput = document.getElementById('servicioDuracion');
const precioInput = document.getElementById('servicioPrecio');
const descripcionInput = document.getElementById('servicioDescripcion');

document.addEventListener('DOMContentLoaded', () => {
  if (typeof requireAuth === 'function' && !requireAuth()) return;
  verificarConexion();
  cargarServicios();

  servicioForm.addEventListener('submit', manejarGuardarServicio);
  if (inputBuscar) {
    inputBuscar.addEventListener('input', filtrarServicios);
  }
});

// Mostrar Alerta Visual
function mostrarAlerta(mensaje, esError = false, esHtml = false) {
  alertMessage.className = `alert-banner ${esError ? 'error' : 'success'}`;
  if (esHtml) {
    alertMessage.innerHTML = mensaje;
  } else {
    alertMessage.textContent = mensaje;
  }
  alertMessage.style.display = 'block';
  if (!esHtml) {
    setTimeout(() => {
      alertMessage.style.display = 'none';
    }, 6000);
  }
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

async function cargarServicios() {
  try {
    serviciosTableBody.innerHTML = `
      <tr>
        <td colspan="6" style="text-align: center; color: var(--muted); padding: 2rem;">
          Cargando catálogo de servicios...
        </td>
      </tr>
    `;

    const res = await apiFetch(`${API_BASE}/servicios`);
    if (!res.ok) throw new Error('Error al consultar catálogo de servicios');

    listaServicios = await res.json();
    actualizarKPIs(listaServicios);
    renderizarTabla(listaServicios);
  } catch (err) {
    console.error('Error cargando servicios:', err);
    serviciosTableBody.innerHTML = `
      <tr>
        <td colspan="6" style="text-align: center; color: var(--danger); padding: 2rem;">
          Error de conexión al cargar servicios: ${err.message}
        </td>
      </tr>
    `;
  }
}

function actualizarKPIs(servicios) {
  totalServiciosCountEl.textContent = servicios.length;

  if (servicios.length === 0) {
    duracionPromedioEl.textContent = '0 min';
    precioPromedioEl.textContent = '$0';
    return;
  }

  const sumaDuracion = servicios.reduce((acc, s) => acc + (s.duracionMinutos || 0), 0);
  const promedioDuracion = Math.round(sumaDuracion / servicios.length);
  duracionPromedioEl.textContent = `${promedioDuracion} min`;

  const sumaPrecio = servicios.reduce((acc, s) => acc + Number(s.precio || 0), 0);
  const promedioPrecio = Math.round(sumaPrecio / servicios.length);
  precioPromedioEl.textContent = `$${promedioPrecio.toLocaleString('es-CL')}`;
}

function filtrarServicios() {
  const query = (inputBuscar.value || '').trim().toLowerCase();
  if (!query) {
    renderizarTabla(listaServicios);
    return;
  }
  const filtrados = listaServicios.filter(s =>
    (s.nombre && s.nombre.toLowerCase().includes(query)) ||
    (s.descripcion && s.descripcion.toLowerCase().includes(query))
  );
  renderizarTabla(filtrados);
}

function renderizarTabla(servicios) {
  if (!servicios || servicios.length === 0) {
    serviciosTableBody.innerHTML = `
      <tr>
        <td colspan="6" style="text-align: center; color: var(--muted); padding: 2rem;">
          No se encontraron servicios registrados. ¡Registra el primero con el formulario!
        </td>
      </tr>
    `;
    return;
  }

  serviciosTableBody.innerHTML = servicios.map(s => {
    const precioFmt = Number(s.precio).toLocaleString('es-CL');
    return `
      <tr>
        <td><strong>#${s.id}</strong></td>
        <td>
          <div style="font-weight: 700; color: var(--fg); font-size: 13px;">${s.nombre}</div>
          <div style="font-size: 11px; color: var(--muted); margin-top: 2px;">
            ${s.descripcion || 'Sin descripción detallada'}
          </div>
        </td>
        <td>
          <span style="font-weight: 600;">${s.duracionMinutos} min</span>
        </td>
        <td>
          <strong style="color: var(--primary); font-size: 13px;">$${precioFmt}</strong>
        </td>
        <td>
          <span class="status online" style="font-size: 9px;">Habilitado</span>
        </td>
        <td style="text-align: right;">
          <div class="btn-action-group" style="justify-content: flex-end;">
            <a href="citas.html?servicioId=${s.id}" class="btn sm" title="Agendar cita con este servicio">
              Agendar &rarr;
            </a>
            <button type="button" class="btn sm outline" onclick="prepararEdicionServicio(${s.id})" title="Editar servicio">
              Editar
            </button>
            <button type="button" class="btn sm btn-danger" onclick="desactivarServicio(${s.id})" title="Desactivar del catálogo">
              ✕
            </button>
          </div>
        </td>
      </tr>
    `;
  }).join('');
}

async function manejarGuardarServicio(e) {
  e.preventDefault();

  const idParaEditar = editServicioIdInput.value;
  const esEdicion = Boolean(idParaEditar);

  const nombre = nombreInput.value.trim();
  const duracionMinutos = Number(duracionInput.value);
  const precio = Number(precioInput.value);
  const descripcion = descripcionInput.value.trim();

  if (!nombre || !duracionMinutos || !precio) {
    mostrarAlerta('Por favor completa todos los campos requeridos', true);
    return;
  }

  const payload = {
    nombre,
    duracionMinutos,
    precio,
    descripcion: descripcion || null
  };

  btnSubmitServicio.disabled = true;
  btnSubmitServicio.textContent = 'Guardando...';

  try {
    const url = esEdicion ? `${API_BASE}/servicios/${idParaEditar}` : `${API_BASE}/servicios`;
    const metodo = esEdicion ? 'PUT' : 'POST';

    const res = await apiFetch(url, {
      method: metodo,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    const data = await res.json().catch(() => null);

    if (!res.ok) {
      const errorMsg = data?.error || (data?.errores ? Object.values(data.errores).join(', ') : 'Error al guardar servicio');
      mostrarAlerta(`Error: ${errorMsg}`, true);
      btnSubmitServicio.disabled = false;
      btnSubmitServicio.textContent = esEdicion ? 'Actualizar Servicio' : '+ Guardar Servicio';
      return;
    }

    const nuevoId = esEdicion ? idParaEditar : data?.id;
    const actionHtml = `
      <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:8px;">
        <span>¡Servicio "<strong>${nombre}</strong>" guardado exitosamente! Ahora se encuentra disponible en agendamiento.</span>
        <div style="display:flex; gap:6px;">
          <a href="citas.html?servicioId=${nuevoId}" class="btn sm" style="text-decoration:none;">Agendar en Gestión Citas &rarr;</a>
          <a href="reserva-cliente.html?servicioId=${nuevoId}" class="btn sm outline" style="text-decoration:none;">Agendar como Cliente</a>
        </div>
      </div>
    `;
    mostrarAlerta(actionHtml, false, true);

    cancelarEdicionServicio();
    await cargarServicios();
  } catch (err) {
    mostrarAlerta(`Error de conexión: ${err.message}`, true);
  } finally {
    btnSubmitServicio.disabled = false;
    btnSubmitServicio.textContent = esEdicion ? 'Actualizar Servicio' : '+ Guardar Servicio';
  }
}

function prepararEdicionServicio(id) {
  const serv = listaServicios.find(s => s.id === id);
  if (!serv) return;

  editServicioIdInput.value = serv.id;
  nombreInput.value = serv.nombre || '';
  duracionInput.value = serv.duracionMinutos || '';
  precioInput.value = serv.precio || '';
  descripcionInput.value = serv.descripcion || '';

  serviceFormTitle.textContent = `Editar Servicio #${serv.id}`;
  serviceFormBadge.textContent = 'PUT /servicios';
  serviceFormBadge.className = 'status';
  btnSubmitServicio.textContent = 'Actualizar Servicio (PUT)';
  btnCancelServicio.style.display = 'block';

  serviceEditTargetId.textContent = `#${serv.id} (${serv.nombre})`;
  serviceEditAlert.style.display = 'flex';

  document.getElementById('formServiceCard').scrollIntoView({ behavior: 'smooth', block: 'start' });
  nombreInput.focus();
}

function cancelarEdicionServicio() {
  editServicioIdInput.value = '';
  servicioForm.reset();

  serviceFormTitle.textContent = 'Nuevo Servicio';
  serviceFormBadge.textContent = 'POST /servicios';
  serviceFormBadge.className = 'status online';
  btnSubmitServicio.textContent = '+ Guardar Servicio';
  btnCancelServicio.style.display = 'none';
  serviceEditAlert.style.display = 'none';
}

async function desactivarServicio(id) {
  const serv = listaServicios.find(s => s.id === id);
  const nom = serv ? serv.nombre : `#${id}`;
  if (!confirm(`¿Estás seguro de que deseas desactivar el servicio "${nom}"? Ya no aparecerá para nuevos agendamientos.`)) {
    return;
  }

  try {
    const res = await apiFetch(`${API_BASE}/servicios/${id}`, {
      method: 'DELETE'
    });

    if (res.ok || res.status === 204) {
      mostrarAlerta(`Servicio "${nom}" desactivado del catálogo.`);
      if (editServicioIdInput.value === String(id)) {
        cancelarEdicionServicio();
      }
      cargarServicios();
    } else {
      const err = await res.json().catch(() => null);
      mostrarAlerta(`No se pudo desactivar el servicio: ${err?.error || res.statusText}`, true);
    }
  } catch (err) {
    mostrarAlerta(`Error al desactivar servicio: ${err.message}`, true);
  }
}
