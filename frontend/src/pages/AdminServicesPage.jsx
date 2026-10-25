import { useEffect, useState } from 'react';
import { api } from '../api/client.js';
import { AdminShell, EmptyState, RequestMessage } from './AdminShell.jsx';

const blankService = { nombre: '', descripcion: '', duracionMinutos: '', precio: '' };

export default function AdminServicesPage() {
  const [services, setServices] = useState([]);
  const [form, setForm] = useState(blankService);
  const [editingId, setEditingId] = useState(null);
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  async function reload() {
    setError('');
    try { setServices(await api.servicios()); }
    catch (requestError) { setError(requestError.message); }
    finally { setLoading(false); }
  }

  useEffect(() => { reload(); }, []);

  const filtered = services.filter((service) => `${service.nombre} ${service.descripcion || ''}`.toLowerCase().includes(query.toLowerCase()));

  async function save(event) {
    event.preventDefault();
    setSaving(true);
    setError('');
    setNotice('');
    const payload = { ...form, duracionMinutos: Number(form.duracionMinutos), precio: Number(form.precio), activo: true };
    try {
      if (editingId) {
        const updated = await api.actualizarServicio(editingId, payload);
        setServices((current) => current.map((service) => service.id === editingId ? updated : service));
        setNotice('Servicio actualizado.');
      } else {
        const created = await api.crearServicio(payload);
        setServices((current) => [created, ...current]);
        setNotice('Servicio creado.');
      }
      setForm(blankService);
      setEditingId(null);
    } catch (requestError) { setError(requestError.message); }
    finally { setSaving(false); }
  }

  function edit(service) {
    setEditingId(service.id);
    setForm({ nombre: service.nombre || '', descripcion: service.descripcion || '', duracionMinutos: service.duracionMinutos || '', precio: service.precio || '' });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  async function remove(service) {
    if (!window.confirm(`¿Desactivar “${service.nombre}”?`)) return;
    setError('');
    try {
      await api.eliminarServicio(service.id);
      setServices((current) => current.filter((item) => item.id !== service.id));
      setNotice('Servicio desactivado.');
      if (editingId === service.id) { setEditingId(null); setForm(blankService); }
    } catch (requestError) { setError(requestError.message); }
  }

  return <AdminShell title="Servicios" active="/admin/servicios">
    <RequestMessage error={error} notice={notice} />
    <section className="admin-panel"><h2>{editingId ? `Editar servicio #${editingId}` : 'Nuevo servicio'}</h2><form className="admin-form-grid" onSubmit={save}>
      <label>Nombre<input required maxLength={120} value={form.nombre} onChange={(event) => setForm({ ...form, nombre: event.target.value })} /></label>
      <label>Duración (minutos)<input required type="number" min="1" value={form.duracionMinutos} onChange={(event) => setForm({ ...form, duracionMinutos: event.target.value })} /></label>
      <label>Precio<input required type="number" min="0" step="1" value={form.precio} onChange={(event) => setForm({ ...form, precio: event.target.value })} /></label>
      <label className="wide">Descripción<textarea rows={3} value={form.descripcion} onChange={(event) => setForm({ ...form, descripcion: event.target.value })} /></label>
      <div className="admin-row-actions"><button className="site-button" type="submit" disabled={saving}>{saving ? 'Guardando…' : editingId ? 'Guardar cambios' : 'Crear servicio'}</button>{editingId && <button type="button" className="site-button site-button-outline" onClick={() => { setEditingId(null); setForm(blankService); }}>Cancelar edición</button>}</div>
    </form></section>
    <section className="admin-panel"><div className="admin-section-title"><h2>Catálogo</h2><input aria-label="Buscar servicios" placeholder="Buscar…" value={query} onChange={(event) => setQuery(event.target.value)} /></div><EmptyState loading={loading} error={error} empty={!filtered.length}><div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Servicio</th><th>Duración</th><th>Precio</th><th>Acciones</th></tr></thead><tbody>{filtered.map((service) => <tr key={service.id}><td><strong>{service.nombre}</strong><br />{service.descripcion}</td><td>{service.duracionMinutos} min</td><td>{new Intl.NumberFormat('es-CL', { style: 'currency', currency: 'CLP', maximumFractionDigits: 0 }).format(Number(service.precio) || 0)}</td><td className="admin-row-actions"><button type="button" onClick={() => edit(service)}>Editar</button><button type="button" onClick={() => remove(service)}>Desactivar</button></td></tr>)}</tbody></table></div></EmptyState></section>
  </AdminShell>;
}
