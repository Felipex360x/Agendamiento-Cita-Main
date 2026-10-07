const API_URL = 'http://localhost:8080/api/v2/nicolet/usuarios';

// Elementos DOM
const userTableBody = document.getElementById('userTableBody');
const userForm = document.getElementById('userForm');
const totalUsersEl = document.getElementById('totalUsers');
const statusBadge = document.getElementById('statusBadge');
const statusText = document.getElementById('statusText');
const btnRefresh = document.getElementById('btnRefresh');

// Iniciar aplicación
document.addEventListener('DOMContentLoaded', () => {
  cargarUsuarios();
  userForm.addEventListener('submit', handleCrearUsuario);
  btnRefresh.addEventListener('click', cargarUsuarios);
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

// Cargar usuarios
async function cargarUsuarios() {
  try {
    userTableBody.innerHTML = `<tr><td colspan="5" style="text-align:center; color: var(--gray);">Cargando usuarios...</td></tr>`;
    
    const response = await fetch(API_URL);
    if (!response.ok) throw new Error('Error al conectar con la API');

    const usuarios = await response.json();
    setStatus(true);
    totalUsersEl.innerText = usuarios.length;

    if (usuarios.length === 0) {
      userTableBody.innerHTML = `<tr><td colspan="5" style="text-align:center; color: var(--gray);">No hay usuarios registrados aún.</td></tr>`;
      return;
    }

    userTableBody.innerHTML = '';
    usuarios.forEach(u => {
      const initial = (u.nombre ? u.nombre.charAt(0) : 'U').toUpperCase();
      const tr = document.createElement('tr');
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
          <button class="btn btn-sm btn-danger" onclick="handleEliminarUsuario(${u.id})">
            Eliminar
          </button>
        </td>
      `;
      userTableBody.appendChild(tr);
    });

  } catch (error) {
    console.error('Error cargando usuarios:', error);
    setStatus(false);
    userTableBody.innerHTML = `<tr><td colspan="5" style="text-align:center; color: var(--danger);">No se pudo conectar al backend en ${API_URL}. ¿Está el servidor iniciado?</td></tr>`;
  }
}

// Crear usuario
async function handleCrearUsuario(e) {
  e.preventDefault();

  const nombre = document.getElementById('nombre').value.trim();
  const apellidoP = document.getElementById('apellidoP').value.trim();
  const correo = document.getElementById('correo').value.trim();
  const password = document.getElementById('password').value.trim();

  if (!nombre || !apellidoP || !correo || !password) {
    showToast('Por favor completa todos los campos', 'error');
    return;
  }

  const nuevoUsuario = { nombre, apellidoP, correo, password };

  try {
    const response = await fetch(API_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(nuevoUsuario)
    });

    if (response.ok) {
      showToast('¡Usuario creado exitosamente!', 'success');
      userForm.reset();
      cargarUsuarios();
    } else {
      const errorData = await response.json().catch(() => null);
      const msg = errorData?.error || 'Error al crear usuario en la base de datos';
      showToast(msg, 'error');
    }
  } catch (error) {
    console.error('Error al registrar:', error);
    showToast('Error de conexión al servidor', 'error');
  }
}

// Eliminar usuario
async function handleEliminarUsuario(id) {
  if (!confirm(`¿Estás seguro de que deseas eliminar al usuario #${id}?`)) {
    return;
  }

  try {
    const response = await fetch(`${API_URL}/${id}`, {
      method: 'DELETE'
    });

    if (response.ok) {
      showToast(`Usuario #${id} eliminado`, 'success');
      cargarUsuarios();
    } else {
      showToast('Error al eliminar usuario', 'error');
    }
  } catch (error) {
    console.error('Error al eliminar:', error);
    showToast('Error de conexión al servidor', 'error');
  }
}
