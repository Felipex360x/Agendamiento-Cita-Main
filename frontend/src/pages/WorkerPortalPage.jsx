import { useEffect, useState } from 'react';
import { api } from '../api/client.js';
import { useAuth } from '../auth/AuthContext.jsx';
import { AdminShell, EmptyState, RequestMessage, personName } from './AdminShell.jsx';

const states = ['PENDIENTE', 'CONFIRMADA', 'COMPLETADA', 'CANCELADA'];

export default function WorkerPortalPage() {
  const { user } = useAuth();
  const [worker, setWorker] = useState(null);
  const [appointments, setAppointments] = useState([]);
  const [filter, setFilter] = useState('TODAS');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [cancelTarget, setCancelTarget] = useState(null);
  const [cancelReason, setCancelReason] = useState('');
  const [savingCancellation, setSavingCancellation] = useState(false);
  const [completionTarget, setCompletionTarget] = useState(null);
  const [workSummary, setWorkSummary] = useState('');
  const [savingCompletion, setSavingCompletion] = useState(false);

  useEffect(() => {
    if (!user) return;
    let active = true;
    api.trabajadorPorUsuario(user.id)
      .then(async (profile) => {
        if (!active) return;
        setWorker(profile);
        const ownAppointments = await api.citasDeTrabajador(profile.id);
        if (active) setAppointments(ownAppointments || []);
      })
      .catch((requestError) => { if (active) setError(requestError.message); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [user?.id]);

  async function update(appointment, nextState, reason = null) {
    setError('');
    setNotice('');
    try {
      const updated = await api.cambiarEstadoCita(appointment.id, nextState, reason);
      setAppointments((current) => current.map((item) => item.id === updated.id ? updated : item));
      setNotice(`Cita ${updated.codigoReserva}: ${updated.estado}.`);
      return true;
    } catch (requestError) { setError(requestError.message); return false; }
  }

  async function confirmCancellation(event) {
    event.preventDefault();
    const reason = cancelReason.trim();
    if (!cancelTarget || !reason) return;
    setSavingCancellation(true);
    const saved = await update(cancelTarget, 'CANCELADA', reason);
    setSavingCancellation(false);
    if (saved) {
      setCancelTarget(null);
      setCancelReason('');
    }
  }

  async function confirmCompletion(event) {
    event.preventDefault();
    const summary = workSummary.trim();
    if (!completionTarget || !summary) return;
    setSavingCompletion(true);
    const saved = await update(completionTarget, 'COMPLETADA', summary);
    setSavingCompletion(false);
    if (saved) {
      setCompletionTarget(null);
      setWorkSummary('');
    }
  }

  const visible = filter === 'TODAS' ? appointments : appointments.filter((appointment) => appointment.estado === filter);
  return <AdminShell title="Mis citas" active="/admin/panel-trabajador" roles={['PROFESIONAL', 'TRABAJADOR', 'ESPECIALISTA', 'ADMINISTRADOR']}>
    <RequestMessage error={error} notice={notice} />
    {worker && <section className="admin-panel admin-worker-profile"><span className="site-avatar">{`${worker.usuario?.nombre?.[0] || ''}${worker.usuario?.apellidoP?.[0] || ''}`}</span><div><h2>{personName(worker)}</h2><p>{worker.cargoEspecialidad}</p>{worker.biografia && <small>{worker.biografia}</small>}</div></section>}
    <section className="admin-panel"><div className="admin-section-title"><h2>Mis citas</h2><select aria-label="Filtrar estado" value={filter} onChange={(event) => setFilter(event.target.value)}>{['TODAS', ...states].map((state) => <option key={state} value={state}>{state === 'TODAS' ? 'Todos los estados' : state}</option>)}</select></div><EmptyState loading={loading} error={error} empty={!visible.length}><div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Fecha</th><th>Cliente</th><th>Servicio</th><th>Notas / trabajo realizado</th><th>Estado</th><th>Acciones</th></tr></thead><tbody>{visible.map((appointment) => <tr key={appointment.id}><td>{appointment.fechaHoraInicio?.replace('T', ' ').slice(0, 16)}</td><td>{personName(appointment.cliente)}</td><td>{appointment.servicio?.nombre || '—'}</td><td><div><small>Cliente: {appointment.notasCliente || '—'}</small>{appointment.notasTrabajador && <p><small>Realizado: {appointment.notasTrabajador}</small></p>}</div></td><td><span className={`admin-status ${appointment.estado?.toLowerCase()}`}>{appointment.estado}</span></td><td className="admin-row-actions">{appointment.estado === 'PENDIENTE' && <button type="button" onClick={() => update(appointment, 'CONFIRMADA')}>Confirmar</button>}{appointment.estado === 'CONFIRMADA' && <button type="button" onClick={() => { setError(''); setWorkSummary(''); setCompletionTarget(appointment); }}>Completar</button>}{['PENDIENTE', 'CONFIRMADA'].includes(appointment.estado) && <button type="button" onClick={() => { setError(''); setCancelReason(''); setCancelTarget(appointment); }}>Cancelar</button>}</td></tr>)}</tbody></table></div></EmptyState></section>
    {completionTarget && <div className="admin-modal-backdrop"><section className="admin-modal" role="dialog" aria-modal="true" aria-labelledby="complete-appointment-title"><button type="button" className="admin-modal-close" onClick={() => setCompletionTarget(null)}>Cerrar</button><p className="site-eyebrow">Completar cita {completionTarget.codigoReserva}</p><h2 id="complete-appointment-title">Registrar trabajo realizado</h2><p><strong>Servicio elegido por el cliente:</strong> {completionTarget.servicio?.nombre || 'Servicio no disponible'}</p><p>Describe brevemente la atención. Este detalle quedará guardado en el historial de la cita.</p><form onSubmit={confirmCompletion}><label htmlFor="work-summary">Detalle del trabajo realizado <span aria-hidden="true">*</span></label><textarea id="work-summary" className="admin-note-input" rows={4} required maxLength={1000} value={workSummary} onChange={(event) => setWorkSummary(event.target.value)} placeholder={`Describe la atención de ${completionTarget.servicio?.nombre || 'este servicio'}…`} /><div className="admin-row-actions"><button type="button" onClick={() => setCompletionTarget(null)}>Volver</button><button type="submit" disabled={savingCompletion || !workSummary.trim()}>{savingCompletion ? 'Guardando…' : 'Guardar y completar'}</button></div></form></section></div>}
    {cancelTarget && <div className="admin-modal-backdrop"><section className="admin-modal" role="dialog" aria-modal="true" aria-labelledby="cancel-appointment-title"><button type="button" className="admin-modal-close" onClick={() => setCancelTarget(null)}>Cerrar</button><p className="site-eyebrow">Cancelar cita {cancelTarget.codigoReserva}</p><h2 id="cancel-appointment-title">Indica el motivo</h2><p>El motivo quedará registrado en la cita.</p><form onSubmit={confirmCancellation}><label htmlFor="cancellation-reason">Motivo de cancelación <span aria-hidden="true">*</span></label><textarea id="cancellation-reason" className="admin-note-input" rows={4} required maxLength={500} value={cancelReason} onChange={(event) => setCancelReason(event.target.value)} placeholder="Explica por qué necesitas cancelar esta cita…" /><div className="admin-row-actions"><button type="button" onClick={() => setCancelTarget(null)}>Volver</button><button type="submit" disabled={savingCancellation || !cancelReason.trim()}>{savingCancellation ? 'Cancelando…' : 'Confirmar cancelación'}</button></div></form></section></div>}
  </AdminShell>;
}
