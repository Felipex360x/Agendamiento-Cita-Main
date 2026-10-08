// ========================================================
// Nicolet Estudio - Frontend Agendamiento de Citas
// ========================================================

const API_BASE = 'http://localhost:8080/api/v2/nicolet';

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
const totalServiciosEl = document.getElementById('totalServicios');
const citasTableBody = document.getElementById('citasTableBody');

const clienteSelect = document.getElementById('clienteId');
const trabajadorSelect = document.getElementById('trabajadorId');
const servicioSelect = document.getElementById('servicioId');
const servicioInfoEl = document.getElementById('servicioInfo');
const fechaHoraInicioInput = document.getElementById('fechaHoraInicio');
const notasClienteInput = document.getElementById('notasCliente');
const citaForm = document.getElementById('citaForm');

// Iniciar al cargar
document.addEventListener('DOMContentLoaded', () => {
  establecerFechaMinima();
  verificarConexion();
  cargarDatosFormulario();
  cargarCitas();

  // Event listener en select de servicio para mostrar detalles
  servicioSelect.addEventListener('change', () => {
    const sId = Number(servicioSelect.value);
    const servicio = serviciosDisponibles.find(s => s.id === sId);
    if (servicio) {
      servicioInfoEl.textContent = `⏱️ Duración: ${servicio.duracionMinutos} min | 💰 Precio: $${Number(servicio.precio).toLocaleString('es-CL')}`;
    } else {
      servicioInfoEl.textContent = '';
    }
  });

  citaForm.addEventListener('submit', manejarAgendamiento);
});

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
  try {
    const res = await fetch(`${API_BASE}/servicios`, { method: 'GET' });
    if (res.ok) {
      statusBadge.className = 'status-badge connected';
      statusText.textContent = 'API Conectada (8080)';
    } else {
      marcarDesconectado();
    }
  } catch (e) {
    marcarDesconectado();
  }
}

function marcarDesconectado() {
  statusBadge.className = 'status-badge error';
  statusText.textContent = 'Sin conexión a API';
}

// Cargar Clientes, Trabajadores y Servicios para los Selects
async function cargarDatosFormulario() {
  try {
    // 1. Cargar Servicios
    const resServicios = await fetch(`${API_BASE}/servicios`);
    if (resServicios.ok) {
      serviciosDisponibles = await resServicios.json();
      totalServiciosEl.textContent = serviciosDisponibles.length;
      servicioSelect.innerHTML = '<option value="" disabled selected>Seleccione un servicio...</option>' +
        serviciosDisponibles.map(s => `
          <option value="${s.id}">
            ${s.nombre} ($${Number(s.precio).toLocaleString('es-CL')} - ${s.duracionMinutos} min)
          </option>
        `).join('');
    }

    // 2. Cargar Clientes
    const resClientes = await fetch(`${API_BASE}/clientes`);
    if (resClientes.ok) {
      clientesDisponibles = await resClientes.json();
      clienteSelect.innerHTML = '<option value="" disabled selected>Seleccione un cliente...</option>' +
        clientesDisponibles.map(c => `
          <option value="${c.id}">
            ${c.usuario.nombre} ${c.usuario.apellidoP} (${c.usuario.correo})
          </option>
        `).join('');
    }

    // 3. Cargar Trabajadores
    const resTrabajadores = await fetch(`${API_BASE}/trabajadores`);
    if (resTrabajadores.ok) {
      trabajadoresDisponibles = await resTrabajadores.json();
      trabajadorSelect.innerHTML = '<option value="" disabled selected>Seleccione un profesional...</option>' +
        trabajadoresDisponibles.map(t => `
          <option value="${t.id}">
            ${t.usuario.nombre} ${t.usuario.apellidoP} - ${t.cargoEspecialidad}
          </option>
        `).join('');
    }
  } catch (error) {
    console.error('Error cargando catálogos:', error);
  }
}

// Cargar lista de citas
async function cargarCitas() {
  try {
    citasTableBody.innerHTML = `
      <tr>
        <td colspan="7" style="text-align: center; color: var(--gray); padding: 2rem;">
          Cargando citas...
        </td>
      </tr>
    `;

    const res = await fetch(`${API_BASE}/citas`);
    if (!res.ok) throw new Error('Error al obtener citas');

    const citas = await res.json();
    totalCitasEl.textContent = citas.length;
    citasConfirmadasEl.textContent = citas.filter(c => c.estado === 'CONFIRMADA').length;

    if (citas.length === 0) {
      citasTableBody.innerHTML = `
        <tr>
          <td colspan="7" style="text-align: center; color: var(--gray); padding: 2rem;">
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
            <span style="font-size:0.8rem; color: var(--gray);">$${precioFormat}</span>
          </td>
          <td>
            📅 ${fechaInicio}<br>
            <span style="font-size:0.8rem; color: var(--gray);">Hasta ${fechaFin}</span>
          </td>
          <td>
            <span class="badge-estado ${cita.estado}">${cita.estado}</span>
          </td>
          <td>
            <div class="btn-action-group">
              ${puedeCompletar ? `
                <button class="btn btn-sm btn-secondary" onclick="cambiarEstadoCita(${cita.id}, 'COMPLETADA')" title="Marcar como atendida">
                  ✓ Finalizar
                </button>
              ` : ''}
              ${puedeCancelar ? `
                <button class="btn btn-sm btn-danger" onclick="cambiarEstadoCita(${cita.id}, 'CANCELADA')" title="Cancelar cita">
                  ✕ Cancelar
                </button>
              ` : ''}
              ${!puedeCompletar && !puedeCancelar ? `
                <span style="font-size: 0.8rem; color: var(--gray); font-style: italic;">Sin acciones</span>
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
    const res = await fetch(`${API_BASE}/citas`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    const data = await res.json();

    if (!res.ok) {
      const errorMsg = data.error || (data.errores ? Object.values(data.errores).join(', ') : 'No se pudo agendar la cita');
      mostrarAlerta(`❌ Error: ${errorMsg}`, true);
      return;
    }

    mostrarAlerta(`✅ Cita agendada con éxito: Código ${data.codigoReserva}`);
    citaForm.reset();
    servicioInfoEl.textContent = '';
    cargarCitas();
  } catch (error) {
    mostrarAlerta(`❌ Error de red: ${error.message}`, true);
  }
}

// Cambiar estado de una cita (COMPLETADA o CANCELADA)
async function cambiarEstadoCita(id, nuevoEstado) {
  let motivo = '';
  if (nuevoEstado === 'CANCELADA') {
    motivo = prompt('Ingrese motivo de la cancelación:') || 'Cancelada por el usuario';
  }

  try {
    const res = await fetch(`${API_BASE}/citas/${id}/estado`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        nuevoEstado: nuevoEstado,
        motivoONotas: motivo
      })
    });

    const data = await res.json();

    if (!res.ok) {
      mostrarAlerta(`❌ Error: ${data.error || 'No se pudo actualizar el estado'}`, true);
      return;
    }

    mostrarAlerta(`✅ Estado de la cita actualizado a ${nuevoEstado}`);
    cargarCitas();
  } catch (error) {
    mostrarAlerta(`❌ Error de red: ${error.message}`, true);
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
    // Crear el usuario con rol CLIENTE (id: 2)
    const resUser = await fetch(`${API_BASE}/usuarios`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        nombre,
        apellidoP,
        correo,
        password: 'cliente_password_123',
        tipoUsuarioId: 2
      })
    });

    if (!resUser.ok) {
      const err = await resUser.json();
      mostrarAlerta(`❌ Error creando cliente: ${err.error || 'Correo ya existe o datos inválidos'}`, true);
      return;
    }

    cerrarModalCliente();
    mostrarAlerta(`✅ Cliente ${nombre} ${apellidoP} registrado exitosamente`);
    await cargarDatosFormulario();

    // Seleccionar el nuevo cliente creado
    const clienteCreado = clientesDisponibles.find(c => c.usuario.correo.toLowerCase() === correo.toLowerCase());
    if (clienteCreado) {
      clienteSelect.value = clienteCreado.id;
    }
  } catch (error) {
    mostrarAlerta(`❌ Error de conexión: ${error.message}`, true);
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
    // Crear el usuario con rol PROFESIONAL (id: 3)
    const resUser = await fetch(`${API_BASE}/usuarios`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        nombre,
        apellidoP,
        correo,
        password: 'pro_password_123',
        tipoUsuarioId: 3
      })
    });

    if (!resUser.ok) {
      const err = await resUser.json();
      mostrarAlerta(`❌ Error creando profesional: ${err.error || 'Correo ya existe o datos inválidos'}`, true);
      return;
    }

    cerrarModalTrabajador();
    mostrarAlerta(`✅ Profesional ${nombre} ${apellidoP} registrado exitosamente`);
    await cargarDatosFormulario();

    // Seleccionar el nuevo trabajador creado
    const trabajadorCreado = trabajadoresDisponibles.find(t => t.usuario.correo.toLowerCase() === correo.toLowerCase());
    if (trabajadorCreado) {
      trabajadorSelect.value = trabajadorCreado.id;
    }
  } catch (error) {
    mostrarAlerta(`❌ Error de conexión: ${error.message}`, true);
  }
}
