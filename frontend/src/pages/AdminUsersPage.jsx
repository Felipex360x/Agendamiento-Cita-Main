import { useEffect, useState } from 'react';
import { api } from '../api/client.js';
import { AdminShell, EmptyState, RequestMessage } from './AdminShell.jsx';

const blankUser = { nombre: '', apellidoP: '', correo: '', password: '', tipoUsuarioId: '' };

export default function AdminUsersPage() {
  const [users, setUsers] = useState([]);
  const [roles, setRoles] = useState([]);
  const [form, setForm] = useState(blankUser);
  const [editingId, setEditingId] = useState(null);
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [deactivationTarget, setDeactivationTarget] = useState(null);
  const [deactivationReason, setDeactivationReason] = useState('');
  const [changingStatus, setChangingStatus] = useState(false);

  async function reload() {
    setError('');
    try {
      const [allUsers, allRoles] = await Promise.all([api.usuarios(), api.tiposUsuario()]);
      setUsers(allUsers || []);
      setRoles(allRoles || []);
    } catch (requestError) { setError(requestError.message); }
    finally { setLoading(false); }
  }

  useEffect(() => { reload(); }, []);
  const filtered = users.filter((item) => `${item.nombre} ${item.apellidoP} ${item.correo}`.toLowerCase().includes(query.toLowerCase()));

  async function save(event) {
    event.preventDefault();
    setSaving(true);
    setError('');
    setNotice('');
    const payload = { ...form, correo: form.correo.trim(), tipoUsuarioId: form.tipoUsuarioId ? Number(form.tipoUsuarioId) : null };
    if (editingId && !payload.password) delete payload.password;
    try {
      if (editingId) {
        const updated = await api.actualizarUsuario(editingId, payload);
        setUsers((current) => current.map((user) => user.id === editingId ? updated : user));
        setNotice('Usuario actualizado.');
      } else {
        const created = await api.crearUsuario(payload);
        setUsers((current) => [created, ...current]);
        setNotice('Usuario creado.');
      }
      reset();
    } catch (requestError) { setError(requestError.message); }
    finally { setSaving(false); }
  }

  function edit(user) {
    setEditingId(user.id);
    setForm({ nombre: user.nombre || '', apellidoP: user.apellidoP || '', correo: user.correo || '', password: '', tipoUsuarioId: user.tipoUsuario?.id || '' });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  function reset() { setEditingId(null); setForm(blankUser); }

  async function remove() {
    if (!deactivationTarget || !deactivationReason.trim()) return;
    setError('');
    setNotice('');
    setChangingStatus(true);
    try {
      const updated = await api.desactivarUsuario(deactivationTarget.id, deactivationReason.trim());
      setUsers((current) => current.map((item) => item.id === deactivationTarget.id ? updated : item));
      setNotice('Usuario desactivado. Se conservaron su perfil y sus registros.');
      if (editingId === deactivationTarget.id) reset();
      setDeactivationTarget(null);
      setDeactivationReason('');
    } catch (requestError) { setError(requestError.message); }
    finally { setChangingStatus(false); }
  }

  async function reactivate(user) {
    setError('');
    setNotice('');
    setChangingStatus(true);
    try {
      const updated = await api.activarUsuario(user.id);
      setUsers((current) => current.map((item) => item.id === user.id ? updated : item));
      setNotice('Usuario reactivado. Ya puede iniciar sesión nuevamente.');
    } catch (requestError) { setError(requestError.message); }
    finally { setChangingStatus(false); }
  }

  return <AdminShell title="Usuarios y roles" active="/admin/usuarios">
    <RequestMessage error={error} notice={notice} />
    <section className="admin-panel"><h2>{editingId ? `Editar usuario #${editingId}` : 'Registrar usuario'}</h2><form className="admin-form-grid" onSubmit={save}>
      <label>Nombre<input required value={form.nombre} onChange={(event) => setForm({ ...form, nombre: event.target.value })} /></label>
      <label>Apellido paterno<input required value={form.apellidoP} onChange={(event) => setForm({ ...form, apellidoP: event.target.value })} /></label>
      <label>Correo<input required type="email" value={form.correo} onChange={(event) => setForm({ ...form, correo: event.target.value })} /></label>
      <label>Contraseña<input required={!editingId} type="password" minLength="6" maxLength="72" autoComplete="new-password" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} placeholder={editingId ? 'Dejar vacía para conservarla' : ''} /></label>
      <label>Rol<select value={form.tipoUsuarioId} onChange={(event) => setForm({ ...form, tipoUsuarioId: event.target.value })}><option value="">Sin rol</option>{roles.map((role) => <option key={role.id} value={role.id}>{role.nombre}</option>)}</select></label>
      <div className="admin-row-actions"><button className="site-button" type="submit" disabled={saving}>{saving ? 'Guardando…' : editingId ? 'Guardar cambios' : 'Crear usuario'}</button>{editingId && <button className="site-button site-button-outline" type="button" onClick={reset}>Cancelar edición</button>}</div>
    </form></section>
    <section className="admin-panel"><div className="admin-section-title"><h2>Usuarios</h2><input aria-label="Buscar usuarios" placeholder="Buscar…" value={query} onChange={(event) => setQuery(event.target.value)} /></div><EmptyState loading={loading} error={error} empty={!filtered.length}><div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Nombre</th><th>Correo</th><th>Rol</th><th>Estado</th><th>Acciones</th></tr></thead><tbody>{filtered.map((user) => { const active = Number(user.estado ?? 1) === 1; return <tr key={user.id}><td>{user.nombre} {user.apellidoP}</td><td>{user.correo}</td><td>{user.tipoUsuario?.nombre || 'Sin rol'}</td><td><span className={`admin-status ${active ? 'confirmada' : 'cancelada'}`}>{active ? 'Activo' : 'Desactivado'}</span>{!active && user.motivoDesactivacion && <small className="admin-user-deactivation-reason">Motivo: {user.motivoDesactivacion}</small>}</td><td className="admin-row-actions"><button type="button" onClick={() => edit(user)}>Editar</button>{active ? <button type="button" onClick={() => { setError(''); setDeactivationTarget(user); setDeactivationReason(''); }}>Desactivar</button> : <button type="button" disabled={changingStatus} onClick={() => reactivate(user)}>Reactivar</button>}</td></tr>; })}</tbody></table></div></EmptyState></section>
    {deactivationTarget && <div className="admin-modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget && !changingStatus) setDeactivationTarget(null); }}>
      <section className="admin-modal" role="dialog" aria-modal="true" aria-labelledby="deactivation-title">
        <button className="admin-modal-close" type="button" disabled={changingStatus} onClick={() => setDeactivationTarget(null)}>Cerrar</button>
        <h2 id="deactivation-title">Desactivar usuario</h2>
        <p>Indica el motivo para desactivar a {deactivationTarget.nombre} {deactivationTarget.apellidoP}. El usuario no podrá iniciar sesión.</p>
        <label>Motivo obligatorio<textarea required rows={4} maxLength={1000} value={deactivationReason} onChange={(event) => setDeactivationReason(event.target.value)} placeholder="Escribe el motivo de la desactivación…" /></label>
        <div className="admin-row-actions"><button className="site-button" type="button" disabled={changingStatus || !deactivationReason.trim()} onClick={remove}>{changingStatus ? 'Desactivando…' : 'Confirmar desactivación'}</button><button className="site-button site-button-outline" type="button" disabled={changingStatus} onClick={() => setDeactivationTarget(null)}>Cancelar</button></div>
      </section>
    </div>}
  </AdminShell>;
}
