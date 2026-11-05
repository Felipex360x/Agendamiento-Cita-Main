import { useEffect, useState } from 'react';
import { api } from '../api/client.js';
import DaySelector, { localDateValue } from '../components/DaySelector.jsx';
import { AdminShell, EmptyState, RequestMessage, personName } from './AdminShell.jsx';

const timeLabel = (time) => (time || '').slice(0, 5);
const unavailableLabel = (reason) => ({ DESCANSO: 'Conflicto con colación', OCUPADO: 'Ocupado', HORA_PASADA: 'No disponible' }[reason] || 'No disponible');

export default function AdminAppointmentsPage() {
  const [appointments, setAppointments] = useState([]);
  const [clients, setClients] = useState([]);
  const [workers, setWorkers] = useState([]);
  const [services, setServices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [slots, setSlots] = useState([]);
  const [slotsLoading, setSlotsLoading] = useState(false);
  const [slotsError, setSlotsError] = useState('');
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [saving, setSaving] = useState(false);
  const [clientMode, setClientMode] = useState('registered');
  const [guestClient, setGuestClient] = useState({ nombre: '', apellidoP: '', correo: '', telefono: '' });
  const [form, setForm] = useState({ clienteId: '', trabajadorId: '', servicioId: '', fecha: '', hora: '', notasCliente: '' });
  const selectedSlotAvailable = !slotsLoading && slots.some(
    (slot) => timeLabel(slot.horaInicio) === form.hora && Boolean(slot.disponible),
  );
  const hasBreakConflict = !slotsLoading && slots.some((slot) => slot.motivoNoDisponible === 'DESCANSO');

  async function reload() {
    setError('');
    const resources = [
      ['citas', api.citas, setAppointments],
      ['clientes', api.clientes, setClients],
      ['profesionales', api.trabajadores, setWorkers],
      ['servicios', api.servicios, setServices],
    ];
    const results = await Promise.allSettled(resources.map(([, load]) => load()));
    const failures = [];

    results.forEach((result, index) => {
      const [resource, , setItems] = resources[index];
      if (result.status === 'fulfilled') {
        setItems(result.value || []);
      } else {
        failures.push(`${resource}: ${result.reason.message}`);
      }
    });

    if (failures.length) setError(`No se pudieron cargar ${failures.join('; ')}`);
    setLoading(false);
  }

  useEffect(() => { reload(); }, []);

  useEffect(() => {
    if (!form.trabajadorId || !form.servicioId || !form.fecha) {
      setSlots([]);
      setSlotsError('');
      setSlotsLoading(false);
      return undefined;
    }

    let active = true;
    setSlots([]);
    setSlotsError('');
    setSlotsLoading(true);
    api.disponibilidad(form.trabajadorId, form.fecha, form.servicioId)
      .then((availableSlots) => { if (active) setSlots(availableSlots || []); })
      .catch((requestError) => { if (active) setSlotsError(requestError.message); })
      .finally(() => { if (active) setSlotsLoading(false); });
    return () => { active = false; };
  }, [form.trabajadorId, form.servicioId, form.fecha]);

  async function createAppointment(event) {
    event.preventDefault();
    if (!selectedSlotAvailable) {
      setSlotsError('El horario seleccionado no está disponible. Elige un bloque disponible para continuar.');
      return;
    }
    setSaving(true);
    setNotice('');
    setError('');
    try {
      const { fecha, hora, clienteId, ...appointmentForm } = form;
      const guestData = clientMode === 'guest' ? {
        clienteNombre: guestClient.nombre.trim(),
        clienteApellidoP: guestClient.apellidoP.trim(),
        clienteCorreo: guestClient.correo.trim() || null,
        clienteTelefono: guestClient.telefono.trim() || null,
      } : { clienteId: Number(clienteId) };
      const payload = { ...appointmentForm, ...guestData, trabajadorId: Number(form.trabajadorId), servicioId: Number(form.servicioId), fechaHoraInicio: `${fecha}T${hora}:00`, estado: 'PENDIENTE' };
      const created = await api.crearCita(payload);
      setAppointments((current) => [created, ...current]);
      if (clientMode === 'guest' && created.cliente) {
        setClients((current) => [created.cliente, ...current.filter((client) => client.id !== created.cliente.id)]);
      }
      setNotice(`Cita ${created.codigoReserva} creada.`);
      setForm({ clienteId: '', trabajadorId: '', servicioId: '', fecha: '', hora: '', notasCliente: '' });
      setSlots([]);
      setClientMode('registered');
      setGuestClient({ nombre: '', apellidoP: '', correo: '', telefono: '' });
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setSaving(false);
    }
  }

  async function changeStatus(appointment, newStatus) {
    setError('');
    setNotice('');
    try {
      const updated = await api.cambiarEstadoCita(appointment.id, newStatus, newStatus === 'CANCELADA' ? 'Cancelada desde gestión' : null);
      setAppointments((current) => current.map((item) => item.id === updated.id ? updated : item));
      setNotice(`Cita ${updated.codigoReserva} actualizada a ${updated.estado}.`);
    } catch (requestError) {
      setError(requestError.message);
    }
  }

  return <AdminShell title="Gestión de citas" active="/admin/citas">
    <RequestMessage error={error} notice={notice} />
    <section className="admin-panel"><h2>Agendar cita</h2><form className="admin-form-grid" onSubmit={createAppointment}>
      <label>Tipo de cliente<select value={clientMode} onChange={(event) => { setClientMode(event.target.value); setForm({ ...form, clienteId: '' }); }}><option value="registered">Cliente registrado</option><option value="guest">Cliente invitado (sin cuenta)</option></select></label>
      {clientMode === 'registered' ? <label>Cliente<select required value={form.clienteId} onChange={(event) => setForm({ ...form, clienteId: event.target.value })}><option value="">Seleccionar cliente</option>{clients.map((client) => <option key={client.id} value={client.id}>{personName(client)}</option>)}</select></label> : <>
        <label>Nombre del cliente<input required maxLength={100} autoComplete="given-name" value={guestClient.nombre} onChange={(event) => setGuestClient({ ...guestClient, nombre: event.target.value })} /></label>
        <label>Apellido del cliente<input required maxLength={100} autoComplete="family-name" value={guestClient.apellidoP} onChange={(event) => setGuestClient({ ...guestClient, apellidoP: event.target.value })} /></label>
        <label>Correo (opcional)<input type="email" maxLength={150} autoComplete="email" value={guestClient.correo} onChange={(event) => setGuestClient({ ...guestClient, correo: event.target.value })} /></label>
        <label>Teléfono (opcional)<input type="tel" maxLength={20} autoComplete="tel" value={guestClient.telefono} onChange={(event) => setGuestClient({ ...guestClient, telefono: event.target.value })} /></label>
      </>}
      <label>Profesional<select required value={form.trabajadorId} onChange={(event) => setForm({ ...form, trabajadorId: event.target.value, hora: '' })}><option value="">Seleccionar profesional</option>{workers.map((item) => <option key={item.id} value={item.id}>{personName(item)}</option>)}</select></label>
      <label>Servicio<select required value={form.servicioId} onChange={(event) => setForm({ ...form, servicioId: event.target.value, hora: '' })}><option value="">Seleccionar servicio</option>{services.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></label>
      <div className="admin-date-selector"><DaySelector label="Fecha" value={form.fecha} onChange={(fecha) => setForm({ ...form, fecha, hora: '' })} minDate={localDateValue()} /></div>
      <div className="wide"><strong>Hora disponible</strong>{!form.trabajadorId || !form.servicioId || !form.fecha ? <p className="site-muted">Selecciona profesional, servicio y fecha para consultar los horarios.</p> : slotsLoading ? <p role="status">Consultando disponibilidad…</p> : slotsError ? <p className="site-form-error" role="alert">{slotsError}</p> : slots.length ? <div className="site-slot-grid">{slots.map((slot) => { const available = Boolean(slot.disponible); const label = timeLabel(slot.horaInicio); return <button type="button" key={slot.horaInicio} disabled={!available} title={available ? 'Disponible' : unavailableLabel(slot.motivoNoDisponible)} aria-label={`${label}, ${available ? 'disponible' : unavailableLabel(slot.motivoNoDisponible)}`} className={`${available ? '' : 'unavailable'} ${label === form.hora ? 'selected' : ''}`} onClick={() => available && setForm({ ...form, hora: label })}>{label}{!available && <small>{unavailableLabel(slot.motivoNoDisponible)}</small>}</button>; })}</div> : <p>No hay horas disponibles para esta fecha. Prueba otro día.</p>}{hasBreakConflict && <p className="site-message" role="status">Los bloques marcados como “Conflicto con colación” no se pueden reservar: la duración del servicio ({services.find((item) => String(item.id) === form.servicioId)?.duracionMinutos} minutos) invade el descanso de la profesional.</p>}</div>
      <label className="wide">Notas<textarea rows={2} value={form.notasCliente} onChange={(event) => setForm({ ...form, notasCliente: event.target.value })} /></label>
      <button className="site-button" type="submit" disabled={saving || loading || slotsLoading || !selectedSlotAvailable}>{saving ? 'Guardando…' : 'Crear cita'}</button>
    </form></section>
    <section className="admin-panel"><div className="admin-section-title"><h2>Reservas registradas</h2><button type="button" className="site-button site-button-outline" onClick={reload}>Actualizar</button></div>
      <EmptyState loading={loading} error={error} empty={!appointments.length}><div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Código</th><th>Cliente</th><th>Profesional</th><th>Servicio</th><th>Fecha</th><th>Estado</th><th>Acciones</th></tr></thead><tbody>{appointments.map((item) => <tr key={item.id}><td>{item.codigoReserva}</td><td>{personName(item.cliente)}</td><td>{personName(item.trabajador)}</td><td>{item.servicio?.nombre || '—'}</td><td>{item.fechaHoraInicio?.replace('T', ' ').slice(0, 16)}</td><td><span className={`admin-status ${item.estado?.toLowerCase()}`}>{item.estado}</span></td><td className="admin-row-actions">{item.estado === 'PENDIENTE' && <button className="admin-action-button admin-action-confirm" type="button" onClick={() => changeStatus(item, 'CONFIRMADA')}>Confirmar</button>}{!['CANCELADA', 'COMPLETADA'].includes(item.estado) && <button className="admin-action-button admin-action-deactivate" type="button" onClick={() => changeStatus(item, 'CANCELADA')}>Cancelar</button>}{item.estado === 'CONFIRMADA' && <button className="admin-action-button admin-action-reactivate" type="button" onClick={() => changeStatus(item, 'COMPLETADA')}>Completar</button>}</td></tr>)}</tbody></table></div></EmptyState>
    </section>
  </AdminShell>;
}
