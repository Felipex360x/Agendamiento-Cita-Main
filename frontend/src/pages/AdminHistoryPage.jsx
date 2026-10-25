import { useEffect, useMemo, useState } from 'react';
import { api } from '../api/client.js';
import { AdminShell, EmptyState, RequestMessage, personName } from './AdminShell.jsx';

export default function AdminHistoryPage() {
  const [workers, setWorkers] = useState([]);
  const [workerId, setWorkerId] = useState('');
  const [appointments, setAppointments] = useState([]);
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selected, setSelected] = useState(null);

  useEffect(() => {
    api.trabajadores().then((list) => setWorkers(list || [])).catch((requestError) => setError(requestError.message)).finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (!workerId) { setAppointments([]); return; }
    let active = true;
    setLoading(true);
    api.citasDeTrabajador(workerId)
      .then((list) => { if (active) setAppointments(list || []); })
      .catch((requestError) => { if (active) setError(requestError.message); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [workerId]);

  const filtered = useMemo(() => appointments.filter((appointment) => `${personName(appointment.cliente)} ${appointment.servicio?.nombre || ''} ${appointment.codigoReserva || ''}`.toLowerCase().includes(query.toLowerCase())), [appointments, query]);
  const totals = useMemo(() => ({ count: filtered.length, clients: new Set(filtered.map((appointment) => appointment.cliente?.id)).size, revenue: filtered.reduce((sum, appointment) => sum + Number(appointment.precioFinal || 0), 0) }), [filtered]);

  return <AdminShell title="Historial de tratamientos" active="/admin/historial">
    <RequestMessage error={error} />
    <section className="admin-panel admin-form-grid"><label>Profesional<select value={workerId} onChange={(event) => setWorkerId(event.target.value)}><option value="">Seleccionar profesional</option>{workers.map((worker) => <option key={worker.id} value={worker.id}>{personName(worker)}</option>)}</select></label><label>Buscar clienta o servicio<input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Nombre, servicio o código" /></label></section>
    {workerId && <div className="admin-metrics"><article><small>Atenciones</small><strong>{totals.count}</strong></article><article><small>Clientas únicas</small><strong>{totals.clients}</strong></article><article><small>Venta registrada</small><strong>{new Intl.NumberFormat('es-CL', { style: 'currency', currency: 'CLP', maximumFractionDigits: 0 }).format(totals.revenue)}</strong></article></div>}
    <section className="admin-panel"><h2>Atenciones</h2><EmptyState loading={loading} error={error} empty={!workerId || !filtered.length}><div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Fecha</th><th>Clienta</th><th>Servicio</th><th>Estado</th><th>Precio</th><th></th></tr></thead><tbody>{filtered.map((appointment) => <tr key={appointment.id}><td>{appointment.fechaHoraInicio?.replace('T', ' ').slice(0, 16)}</td><td>{personName(appointment.cliente)}</td><td>{appointment.servicio?.nombre || '—'}</td><td><span className={`admin-status ${appointment.estado?.toLowerCase()}`}>{appointment.estado}</span></td><td>{appointment.precioFinal}</td><td><button className="admin-row-button" type="button" onClick={() => setSelected(appointment)}>Ver ficha</button></td></tr>)}</tbody></table></div></EmptyState></section>
    {selected && <div className="admin-modal-backdrop" role="presentation" onClick={() => setSelected(null)}><section className="admin-modal" role="dialog" aria-modal="true" aria-labelledby="history-modal-title" onClick={(event) => event.stopPropagation()}><button type="button" className="admin-modal-close" onClick={() => setSelected(null)}>Cerrar</button><p className="site-eyebrow">Ficha de atención</p><h2 id="history-modal-title">{personName(selected.cliente)}</h2><p>{selected.servicio?.nombre} · {selected.fechaHoraInicio?.replace('T', ' ').slice(0, 16)}</p><p><strong>Notas de cliente:</strong> {selected.notasCliente || 'Sin notas'}</p><p><strong>Notas del profesional:</strong> {selected.notasTrabajador || 'Sin notas registradas'}</p><p><strong>Cancelación:</strong> {selected.motivoCancelacion || 'No aplica'}</p></section></div>}
  </AdminShell>;
}
