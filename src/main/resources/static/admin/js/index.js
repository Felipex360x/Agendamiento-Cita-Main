const API_URL = '/api/v2/nicolet/usuarios';
const TIPOS_API_URL = '/api/v2/nicolet/tipos-usuario';

// Elementos DOM
const userTableBody = document.getElementById('userTableBody');
const userForm = document.getElementById('userForm');
const formTitle = document.getElementById('formTitle');
const formMethodBadge = document.getElementById('formMethodBadge');
const btnSubmit = document.getElementById('btnSubmit');
const btnCancelEdit = document.getElementById('btnCancelEdit');
const editAlert = document.getElementById('editAlert');
const editTargetId = document.getElementById('editTargetId');
const editUserId = document.getElementById('editUserId');

const searchIdInput = document.getElementById('searchIdInput');
const btnSearchId = document.getElementById('btnSearchId');
const btnResetSearch = document.getElementById('btnResetSearch');
const searchNotice = document.getElementById('searchNotice');
const searchNoticeId = document.getElementById('searchNoticeId');

const totalUsersEl = document.getElementById('totalUsers');
const totalTiposEl = document.getElementById('totalTipos');
const tipoUsuarioSelect = document.getElementById('tipoUsuarioId');
const statusBadge = document.getElementById('statusBadge');
const statusText = document.getElementById('statusText');
const btnRefresh = document.getElementById('btnRefresh');

// Iniciar aplicación
document.addEventListener('DOMContentLoaded', () => {
  if (typeof requireAuth === 'function') requireAuth();
  cargarTiposUsuario();
  cargarUsuarios();

  userForm.addEventListener('submit', handleGuardarUsuario);
  btnRefresh.addEventListener('click', restablecerBusqueda);
  btnSearchId.addEventListener('click', handleBuscarPorId);
  searchIdInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      handleBuscarPorId();
    }
  });
  if (btnResetSearch) {
    btnResetSearch.addEventListener('click', restablecerBusqueda);
  }
});

// Mostrar Toast Notificación
function showToast(message, type = 'success') {
  const container = document.getElementById('toastContainer');
  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  toast.innerText = message;
  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    setTimeout(() => toast.remove(), 300);
  }, 3500);
}

// Actualizar indicador de estado
function setStatus(online) {
  if (online) {
    statusBadge.className = 'status-badge online';
    statusText.innerText = 'Backend Conectado (8080)';
  } else {
    statusBadge.className = 'status-badge offline';
    statusText.innerText = 'Backend Desconectado';
  }
}

// ==========================================
// 1. GET: Cargar tipos de usuario (Roles)
// ==========================================
async function cargarTiposUsuario() {
  try {
    const response = await (window.authenticatedFetch || fetch)(TIPOS_API_URL);
    if (!response.ok) return;

    const tipos = await response.json();
    if (totalTiposEl) {
      totalTiposEl.innerText = tipos.length;
    }

    if (tipoUsuarioSelect) {
      if (tipos.length === 0) {
        tipoUsuarioSelect.innerHTML = `<option value="" disabled selected>No hay roles disponibles</option>`;
        return;
      }

      tipoUsuarioSelect.innerHTML = `<option value="" disabled selected>Selecciona un rol...</option>` +
        tipos.map(t => `<option value="${t.id}">${t.nombre} ${t.descripcion ? '(' + t.descripcion + ')' : ''}</option>`).join('');
    }
  } catch (error) {
    console.error('Error al cargar tipos de usuario:', error);
  }
}

// ==========================================
// 2. GET: Listar todos los usuarios
// ==========================================
async function cargarUsuarios() {
  try {
    userTableBody.innerHTML = `<tr><td colspan="6" style="text-align:center; color: var(--gray);">Cargando usuarios...</td></tr>`;
    
    const response = await (window.authenticatedFetch || fetch)(API_URL);
    if (!response.ok) throw new Error('Error al conectar con la API');

    const usuarios = await response.json();
    setStatus(true);
    totalUsersEl.innerText = usuarios.length;

    renderizarUsuarios(usuarios);

  } catch (error) {
    console.error('Error cargando usuarios:', error);
    setStatus(false);
    userTableBody.innerHTML = `<tr><td colspan="6" style="text-align:center; color: var(--danger);">No se pudo conectar al backend en ${API_URL}. ¿Está el servidor iniciado?</td></tr>`;
  }
}

// Renderizar filas de la tabla
function renderizarUsuarios(usuarios) {
  if (!usuarios || usuarios.length === 0) {
    userTableBody.innerHTML = `<tr><td colspan="6" style="text-align:center; color: var(--gray);">No hay usuarios registrados aún.</td></tr>`;
    return;
  }

  userTableBody.innerHTML = '';
  usuarios.forEach(u => {
    const initial = (u.nombre ? u.nombre.charAt(0) : 'U').toUpperCase();
    const tipoNombre = u.tipoUsuario?.nombre || 'Sin Rol';
    const tipoClass = tipoNombre.toLowerCase();

    const tr = document.createElement('tr');
    tr.id = `row-usuario-${u.id}`;
    tr.innerHTML = `
      <td><strong>#${u.id}</strong></td>
      <td>
        <div style="display:flex; align-items:center;">
          <span class="avatar">${initial}</span>
          <span>${u.nombre || ''}</span>
        </div>
      </td>
      <td>${u.apellidoP || ''}</td>
      <td>${u.correo || ''}</td>
      <td>
        <span class="badge-role ${tipoClass}">${tipoNombre}</span>
      </td>
      <td>
        <div class="action-buttons">
          <button class="btn btn-sm btn-edit" title="Editar este usuario (PUT)" onclick="prepararEdicion(${u.id})">
            Editar
          </button>
          <button class="btn btn-sm btn-danger" title="Eliminar usuario (DELETE)" onclick="handleEliminarUsuario(${u.id})">
            Eliminar
          </button>
        </div>
      </td>
    `;
    userTableBody.appendChild(tr);
  });
}

// ==========================================
// 3. GET: Buscar usuario por ID
// ==========================================
async function handleBuscarPorId() {
  const id = searchIdInput.value.trim();
  if (!id) {
    showToast('Ingresa un ID numérico para buscar', 'error');
    searchIdInput.focus();
    return;
  }

  try {
    userTableBody.innerHTML = `<tr><td colspan="6" style="text-align:center; color: var(--gray);">Buscando usuario #${id}...</td></tr>`;
    
    const response = await (window.authenticatedFetch || fetch)(`${API_URL}/${id}`);
    
    if (response.status === 200) {
      const usuario = await response.json();
      renderizarUsuarios([usuario]);
      
      // Mostrar feedback de búsqueda activa
      searchNotice.style.display = 'flex';
      searchNoticeId.innerText = `#${usuario.id} (${usuario.nombre} ${usuario.apellidoP})`;
      btnResetSearch.style.display = 'inline-flex';
      
      showToast(`Usuario #${usuario.id} encontrado exitosamente (GET 200)`, 'success');
    } else if (response.status === 404) {
      userTableBody.innerHTML = `<tr><td colspan="6" style="text-align:center; color: var(--danger);">Usuario con ID #${id} no encontrado (HTTP 404)</td></tr>`;
      btnResetSearch.style.display = 'inline-flex';
      showToast(`No se encontró ningún usuario con ID #${id} (404)`, 'error');
    } else {
      throw new Error(`Respuesta HTTP ${response.status}`);
    }
  } catch (error) {
    console.error('Error al buscar usuario por ID:', error);
    showToast(`Error al consultar ID #${id}`, 'error');
    restablecerBusqueda();
  }
}

// Restablecer búsqueda y volver a listar todos
function restablecerBusqueda() {
  searchIdInput.value = '';
  searchNotice.style.display = 'none';
  btnResetSearch.style.display = 'none';
  cargarUsuarios();
}

// ==========================================
// 4. PUT: Modo edición y carga de datos
// ==========================================
async function prepararEdicion(id) {
  try {
    // Obtener los datos más recientes del usuario desde la API (GET /id)
    const response = await (window.authenticatedFetch || fetch)(`${API_URL}/${id}`);
    if (!response.ok) {
      showToast(`No se pudo obtener el usuario #${id} para editar`, 'error');
      return;
    }
    const usuario = await response.json();

    // Rellenar formulario con los datos existentes
    editUserId.value = usuario.id;
    document.getElementById('nombre').value = usuario.nombre || '';
    document.getElementById('apellidoP').value = usuario.apellidoP || '';
    document.getElementById('correo').value = usuario.correo || '';
    document.getElementById('password').value = usuario.password || '';

    // Asignar el tipo de usuario si lo tiene
    if (usuario.tipoUsuario && usuario.tipoUsuario.id) {
      tipoUsuarioSelect.value = usuario.tipoUsuario.id;
    }

    // Cambiar estado visual del formulario a Modo Edición (PUT)
    formTitle.innerText = `Editar Usuario #${usuario.id}`;
    formMethodBadge.innerText = 'PUT';
    formMethodBadge.className = 'badge-role administrador';
    btnSubmit.innerText = 'Actualizar Usuario (PUT)';
    btnSubmit.className = 'btn btn-primary';
    btnCancelEdit.style.display = 'block';
    
    editTargetId.innerText = `#${usuario.id} (${usuario.nombre})`;
    editAlert.style.display = 'flex';

    // Desplazar vista hacia el formulario si está en pantallas pequeñas
    document.getElementById('formCard').scrollIntoView({ behavior: 'smooth', block: 'start' });
    document.getElementById('nombre').focus();

    showToast(`Modo edición activado para Usuario #${usuario.id}`, 'success');

  } catch (error) {
    console.error('Error al preparar edición:', error);
    showToast('Error al cargar datos del usuario', 'error');
  }
}

// Cancelar modo edición y volver a modo creación (POST)
function cancelarEdicion() {
  editUserId.value = '';
  userForm.reset();

  formTitle.innerText = 'Registrar Usuario';
  formMethodBadge.innerText = 'POST';
  formMethodBadge.className = 'badge-role cliente';
  btnSubmit.innerText = '+ Guardar Usuario (POST)';
  btnSubmit.className = 'btn btn-primary';
  btnCancelEdit.style.display = 'none';
  editAlert.style.display = 'none';

  showToast('Modo edición cancelado', 'success');
}

// ==========================================
// 5. POST / PUT: Crear o Actualizar usuario
// ==========================================
async function handleGuardarUsuario(e) {
  e.preventDefault();

  const idParaEditar = editUserId.value;
  const esModoEdicion = Boolean(idParaEditar);

  const nombre = document.getElementById('nombre').value.trim();
  const apellidoP = document.getElementById('apellidoP').value.trim();
  const correo = document.getElementById('correo').value.trim();
  const password = document.getElementById('password').value.trim();
  const tipoUsuarioVal = tipoUsuarioSelect?.value;

  if (!nombre || !apellidoP || !correo || !password) {
    showToast('Por favor completa todos los campos requeridos', 'error');
    return;
  }

  const payload = { 
    nombre, 
    apellidoP, 
    correo, 
    password,
    tipoUsuarioId: tipoUsuarioVal ? Number(tipoUsuarioVal) : null
  };

  try {
    const url = esModoEdicion ? `${API_URL}/${idParaEditar}` : API_URL;
    const metodo = esModoEdicion ? 'PUT' : 'POST';

    const response = await (window.authenticatedFetch || fetch)(url, {
      method: metodo,
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(payload)
    });

    if (response.ok) {
      if (esModoEdicion) {
        showToast(`¡Usuario #${idParaEditar} actualizado con éxito! (PUT 200)`, 'success');
        cancelarEdicion();
      } else {
        showToast('¡Usuario creado con éxito! (POST 201)', 'success');
        userForm.reset();
      }
      cargarUsuarios();
    } else {
      const errorData = await response.json().catch(() => null);
      const msg = errorData?.error || (esModoEdicion ? 'Error al actualizar usuario' : 'Error al registrar usuario');
      showToast(msg, 'error');
    }
  } catch (error) {
    console.error('Error al guardar:', error);
    showToast('Error de conexión al servidor', 'error');
  }
}

// ==========================================
// 6. DELETE: Eliminar usuario por ID
// ==========================================
async function handleEliminarUsuario(id) {
  if (!confirm(`¿Estás seguro de que deseas eliminar permanentemente al usuario #${id}?`)) {
    return;
  }

  try {
    const response = await (window.authenticatedFetch || fetch)(`${API_URL}/${id}`, {
      method: 'DELETE'
    });

    if (response.ok) {
      showToast(`Usuario #${id} eliminado correctamente (DELETE 204)`, 'success');
      
      // Si se estaba editando a este usuario, cancelar modo edición
      if (editUserId.value === String(id)) {
        cancelarEdicion();
      }

      cargarUsuarios();
    } else {
      showToast(`Error al eliminar usuario #${id}`, 'error');
    }
  } catch (error) {
    console.error('Error al eliminar:', error);
    showToast('Error de conexión al servidor', 'error');
  }
}
