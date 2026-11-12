import { useEffect, useRef, useState } from 'react';
import { api } from '../api/client.js';
import DaySelector, { localDateValue } from '../components/DaySelector.jsx';
import displayText from '../utils/displayText.js';
import { AdminShell, EmptyState, RequestMessage, personName } from '../layouts/AdminShell.jsx';

const unavailableLabel = (reason) => ({ DESCANSO: 'Colación', OCUPADO: 'Ocupado', HORA_PASADA: 'No disponible' }[reason] || reason || 'Ocupado');

export default function AdminAvailabilityPage() {
  const [workers, setWorkers] = useState([]);
  const [services, setServices] = useState([]);
  const [workerId, setWorkerId] = useState('');
  const [serviceId, setServiceId] = useState('');
  const [date, setDate] = useState(localDateValue());
  // Guarda el resultado de la búsqueda actual. Se borra al cambiar los filtros.
  const [result, setResult] = useState(null);
  const requestSeq = useRef(0);
  const [catalogLoading, setCatalogLoading] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    Promise.all([api.trabajadores(), api.servicios()])
      .then(([allWorkers, allServices]) => { setWorkers(allWorkers || []); setServices(allServices || []); })
      .catch((requestError) => setError(requestError.message))
      .finally(() => setCatalogLoading(false));
  }, []);

  function resetResults() {
    requestSeq.current += 1; // ignora respuestas de búsquedas anteriores que aún no terminan
    setResult(null);
    setError('');
    setLoading(false);
  }

  async function search(event) {
    event.preventDefault();
    const query = { workerId, serviceId, date };
    const seq = ++requestSeq.current;
    setLoading(true);
    setError('');
    setResult(null);
    try {
      const slots = await api.disponibilidad(query.workerId, query.date, query.serviceId || undefined);
      if (seq === requestSeq.current) setResult({ ...query, slots: slots || [] });
    } catch (requestError) {
      if (seq === requestSeq.current) setError(requestError.message);
    } finally {
      if (seq === requestSeq.current) setLoading(false);
    }
  }

  const selectedWorker = workers.find((item) => String(item.id) === String(workerId));
  // Solo muestra los servicios que realiza el profesional elegido.
  const availableServices = selectedWorker
    ? services.filter((item) => (selectedWorker.servicios || []).some((own) => own.id === item.id))
    : services;

  function selectWorker(value) {
    setWorkerId(value);
    const next = workers.find((item) => String(item.id) === String(value));
    if (serviceId && next && !(next.servicios || []).some((own) => String(own.id) === String(serviceId))) setServiceId('');
    resetResults();
  }

  function selectService(value) { setServiceId(value); resetResults(); }
  function selectDate(selectedDate) { setDate(selectedDate); resetResults(); }

  const worker = workers.find((item) => String(item.id) === workerId);
  return <AdminShell title="Disponibilidad" active="/admin/disponibilidad">
    <RequestMessage error={error} />
    <section className="admin-panel"><h2>Consultar agenda</h2><form className="admin-form-grid" onSubmit={search}>
      <label>Profesional<select required value={workerId} onChange={(event) => selectWorker(event.target.value)}><option value="">Seleccionar</option>{workers.map((item) => <option key={item.id} value={item.id}>{personName(item)}</option>)}</select></label>
      <div className="admin-date-selector"><DaySelector label="Fecha" value={date} onChange={selectDate} minDate={localDateValue()} /></div>
      <label>Servicio<select value={serviceId} onChange={(event) => selectService(event.target.value)}><option value="">Duración predeterminada</option>{availableServices.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></label>
      <button className="site-button" type="submit" disabled={catalogLoading || loading}>{loading ? 'Consultando…' : 'Ver disponibilidad'}</button>
    </form></section>
    {worker && <section className="admin-panel"><p className="site-eyebrow">{personName(worker)}</p><h2>{displayText(worker.cargoEspecialidad) || 'Profesional'}</h2>{worker.biografia && <p>{displayText(worker.biografia)}</p>}</section>}
    {(loading || error || result) && <section className="admin-panel"><h2>Bloques del {result?.date || date}</h2><EmptyState loading={loading} error={error} empty={!result?.slots.length}><div className="site-slot-grid">{(result?.slots || []).map((slot) => <div key={slot.horaInicio} className={`admin-slot ${slot.disponible ? 'available' : ''}`}><strong>{String(slot.horaInicio).slice(0, 5)}–{String(slot.horaFin).slice(0, 5)}</strong><small>{slot.disponible ? 'Disponible' : unavailableLabel(slot.motivoNoDisponible)}</small></div>)}</div><a className="site-button" href={`/reservar?trabajadorId=${result?.workerId}&fecha=${result?.date}${result?.serviceId ? `&servicioId=${result.serviceId}` : ''}`}>Reservar este día</a></EmptyState></section>}
  </AdminShell>;
}
