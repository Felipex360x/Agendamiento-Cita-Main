import { useEffect, useState } from 'react';
import { api } from '../api/client.js';
import DaySelector, { localDateValue } from '../components/DaySelector.jsx';
import { AdminShell, EmptyState, RequestMessage, personName } from './AdminShell.jsx';

const localTime = () => {
  const date = new Date();
  return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
};

export default function AdminAppointmentsPage() {
  const [appointments, setAppointments] = useState([]);
  const [clients, setClients] = useState([]);
  const [workers, setWorkers] = useState([]);
  const [services, setServices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState({ clienteId: '', trabajadorId: '', servicioId: '', fecha: '', hora: '', notasCliente: '' });

  async function reload() {
    setError('');
    try {
      const [allAppointments, allClients, allWorkers, allServices] = await Promise.all([
        api.citas(), api.clientes(), api.trabajadores(), api.servicios(),
      ]);
      setAppointments(allAppointments || []);
      setClients(allClients || []);
      setWorkers(allWorkers || []);
      setServices(allServices || []);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { reload(); }, []);

  async function createAppointment(event) {
    event.preventDefault();
    setSaving(true);
    setNotice('');
    setError('');
    try {
      const { fecha, hora, ...appointmentForm } = form;
      const payload = { ...appointmentForm, clienteId: Number(form.clienteId), trabajadorId: Number(form.trabajadorId), servicioId: Number(form.servicioId), fechaHoraInicio: `${fecha}T${hora}:00`, estado: 'PENDIENTE' };
      const created = await api.crearCita(payload);
      setAppointments((current) => [created, ...current]);
      setNotice(`Cita ${created.codigoReserva} creada.`);
      setForm({ clienteId: '', trabajadorId: '', servicioId: '', fecha: '', hora: '', notasCliente: '' });
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
      <label>Cliente<select required value={form.clienteId} onChange={(event) => setForm({ ...form, clienteId: event.target.value })}><option value="">Seleccionar cliente</option>{clients.map((client) => <option key={client.id} value={client.id}>{personName(client)}</option>)}</select></label>
      <label>Profesional<select required value={form.trabajadorId} onChange={(event) => setForm({ ...form, trabajadorId: event.target.value })}><option value="">Seleccionar profesional</option>{workers.map((item) => <option key={item.id} value={item.id}>{personName(item)}</option>)}</select></label>
      <label>Servicio<select required value={form.servicioId} onChange={(event) => setForm({ ...form, servicioId: event.target.value })}><option value="">Seleccionar servicio</option>{services.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></label>
      <DaySelector label="Fecha" value={form.fecha} onChange={(fecha) => setForm({ ...form, fecha, hora: '' })} minDate={localDateValue()} />
      <label>Hora<input required type="time" min={form.fecha === localDateValue() ? localTime() : undefined} value={form.hora} onChange={(event) => setForm({ ...form, hora: event.target.value })} /></label>
      <label className="wide">Notas<textarea rows={2} value={form.notasCliente} onChange={(event) => setForm({ ...form, notasCliente: event.target.value })} /></label>
      <button className="site-button" type="submit" disabled={saving || loading}>{saving ? 'Guardando…' : 'Crear cita'}</button>
    </form></section>
    <section className="admin-panel"><div className="admin-section-title"><h2>Reservas registradas</h2><button type="button" className="site-button site-button-outline" onClick={reload}>Actualizar</button></div>
      <EmptyState loading={loading} error={error} empty={!appointments.length}><div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Código</th><th>Cliente</th><th>Profesional</th><th>Servicio</th><th>Fecha</th><th>Estado</th><th>Acciones</th></tr></thead><tbody>{appointments.map((item) => <tr key={item.id}><td>{item.codigoReserva}</td><td>{personName(item.cliente)}</td><td>{personName(item.trabajador)}</td><td>{item.servicio?.nombre || '—'}</td><td>{item.fechaHoraInicio?.replace('T', ' ').slice(0, 16)}</td><td><span className={`admin-status ${item.estado?.toLowerCase()}`}>{item.estado}</span></td><td className="admin-row-actions">{item.estado === 'PENDIENTE' && <button type="button" onClick={() => changeStatus(item, 'CONFIRMADA')}>Confirmar</button>}{!['CANCELADA', 'COMPLETADA'].includes(item.estado) && <button type="button" onClick={() => changeStatus(item, 'CANCELADA')}>Cancelar</button>}{item.estado === 'CONFIRMADA' && <button type="button" onClick={() => changeStatus(item, 'COMPLETADA')}>Completar</button>}</td></tr>)}</tbody></table></div></EmptyState>
    </section>
  </AdminShell>;
}
