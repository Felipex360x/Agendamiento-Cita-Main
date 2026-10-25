import { useEffect, useState } from 'react';
import { api } from '../api/client.js';
import DaySelector, { localDateValue } from '../components/DaySelector.jsx';
import { AdminShell, EmptyState, RequestMessage, personName } from './AdminShell.jsx';

const unavailableLabel = (reason) => ({ DESCANSO: 'Colación', OCUPADO: 'Ocupado', HORA_PASADA: 'No disponible' }[reason] || reason || 'Ocupado');

export default function AdminAvailabilityPage() {
  const [workers, setWorkers] = useState([]);
  const [services, setServices] = useState([]);
  const [workerId, setWorkerId] = useState('');
  const [serviceId, setServiceId] = useState('');
  const [date, setDate] = useState(localDateValue());
  const [slots, setSlots] = useState([]);
  const [catalogLoading, setCatalogLoading] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [searched, setSearched] = useState(false);

  useEffect(() => {
    Promise.all([api.trabajadores(), api.servicios()])
      .then(([allWorkers, allServices]) => { setWorkers(allWorkers || []); setServices(allServices || []); })
      .catch((requestError) => setError(requestError.message))
      .finally(() => setCatalogLoading(false));
  }, []);

  async function search(event) {
    event.preventDefault();
    setLoading(true);
    setError('');
    setSearched(true);
    try { setSlots(await api.disponibilidad(workerId, date, serviceId || undefined)); }
    catch (requestError) { setError(requestError.message); setSlots([]); }
    finally { setLoading(false); }
  }

  function selectDate(selectedDate) {
    setDate(selectedDate);
    setSlots([]);
    setSearched(false);
  }

  const worker = workers.find((item) => String(item.id) === workerId);
  return <AdminShell title="Disponibilidad" active="/admin/disponibilidad">
    <RequestMessage error={error} />
    <section className="admin-panel"><h2>Consultar agenda</h2><form className="admin-form-grid" onSubmit={search}>
      <label>Profesional<select required value={workerId} onChange={(event) => setWorkerId(event.target.value)}><option value="">Seleccionar</option>{workers.map((item) => <option key={item.id} value={item.id}>{personName(item)}</option>)}</select></label>
      <DaySelector label="Fecha" value={date} onChange={selectDate} minDate={localDateValue()} />
      <label>Servicio<select value={serviceId} onChange={(event) => setServiceId(event.target.value)}><option value="">Duración predeterminada</option>{services.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></label>
      <button className="site-button" type="submit" disabled={catalogLoading || loading}>{loading ? 'Consultando…' : 'Ver disponibilidad'}</button>
    </form></section>
    {worker && <section className="admin-panel"><p className="site-eyebrow">{personName(worker)}</p><h2>{worker.cargoEspecialidad || 'Profesional'}</h2>{worker.biografia && <p>{worker.biografia}</p>}</section>}
    {searched && <section className="admin-panel"><h2>Bloques del {date}</h2><EmptyState loading={loading} error={error} empty={!slots.length}><div className="site-slot-grid">{slots.map((slot) => <div key={slot.horaInicio} className={`admin-slot ${slot.disponible ? 'available' : ''}`}><strong>{String(slot.horaInicio).slice(0, 5)}–{String(slot.horaFin).slice(0, 5)}</strong><small>{slot.disponible ? 'Disponible' : unavailableLabel(slot.motivoNoDisponible)}</small></div>)}</div><a className="site-button" href={`/reservar?trabajadorId=${workerId}&fecha=${date}${serviceId ? `&servicioId=${serviceId}` : ''}`}>Reservar este día</a></EmptyState></section>}
  </AdminShell>;
}
