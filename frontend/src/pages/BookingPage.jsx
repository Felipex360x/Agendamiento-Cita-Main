import { useEffect, useMemo, useState } from 'react';
import { api } from '../api/client.js';
import { useAuth } from '../auth/AuthContext.jsx';
import DaySelector from '../components/DaySelector.jsx';
import { SiteFooter, SiteHeader } from './SiteShell.jsx';

const money = (value) => new Intl.NumberFormat('es-CL', {
  style: 'currency', currency: 'CLP', maximumFractionDigits: 0,
}).format(Number(value) || 0);

const localDate = () => {
  const date = new Date();
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
};

const timeLabel = (time) => (time || '').slice(0, 5);
const unavailableLabel = (reason) => ({ DESCANSO: 'Colación', OCUPADO: 'Ocupado', HORA_PASADA: 'No disponible' }[reason] || 'No disponible');

function workerName(worker) {
  return [worker?.usuario?.nombre, worker?.usuario?.apellidoP].filter(Boolean).join(' ') || 'Especialista';
}

export default function BookingPage() {
  const { user, register, logout } = useAuth();
  const isClient = user?.rol?.toUpperCase().includes('CLIENTE');
  const params = useMemo(() => new URLSearchParams(window.location.search), []);
  const [services, setServices] = useState([]);
  const [workers, setWorkers] = useState([]);
  const [serviceId, setServiceId] = useState(params.get('servicioId') || '');
  const [workerId, setWorkerId] = useState(params.get('trabajadorId') || '');
  const [date, setDate] = useState(params.get('fecha') || '');
  const [time, setTime] = useState(params.get('hora') || '');
  const [slots, setSlots] = useState([]);
  const [catalogLoading, setCatalogLoading] = useState(true);
  const [slotsLoading, setSlotsLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [booking, setBooking] = useState(null);
  const [notes, setNotes] = useState('');

  const service = services.find((item) => String(item.id) === String(serviceId));
  const worker = workers.find((item) => String(item.id) === String(workerId));
  const steps = ['Servicio', 'Especialista', 'Fecha y hora', 'Tus datos'];
  const [step, setStep] = useState(workerId ? 3 : serviceId ? 2 : 1);
  const loginHref = `/admin/login?redirect=${encodeURIComponent(`/reservar.html${serviceId ? `?servicioId=${serviceId}` : ''}`)}`;

  useEffect(() => {
    document.title = 'Reserva tu hora | Nicolett Studio Fantasy';
    let active = true;
    Promise.all([api.servicios(), api.trabajadores()])
      .then(([availableServices, availableWorkers]) => {
        if (!active) return;
        setServices(availableServices || []);
        setWorkers(availableWorkers || []);
      })
      .catch((requestError) => { if (active) setError(requestError.message); })
      .finally(() => { if (active) setCatalogLoading(false); });
    return () => { active = false; };
  }, []);

  useEffect(() => {
    if (!serviceId || !workerId || !date) {
      setSlots([]);
      return undefined;
    }
    let active = true;
    setSlotsLoading(true);
    setSlots([]);
    setError('');
    api.disponibilidad(workerId, date, serviceId)
      .then((availableSlots) => { if (active) setSlots(availableSlots || []); })
      .catch((requestError) => { if (active) setError(requestError.message); })
      .finally(() => { if (active) setSlotsLoading(false); });
    return () => { active = false; };
  }, [serviceId, workerId, date]);

  function chooseService(id) {
    setServiceId(String(id));
    setWorkerId('');
    setDate('');
    setTime('');
    setStep(2);
    setError('');
  }

  function chooseWorker(id) {
    setWorkerId(String(id));
    setTime('');
    setStep(3);
    setError('');
  }

  async function confirmBooking(event) {
    event.preventDefault();
    setBusy(true);
    setError('');
    const data = new FormData(event.currentTarget);
    try {
      let customerUser = user;
      if (!customerUser) {
        customerUser = await register(
          data.get('nombre'), data.get('apellidoP'), data.get('correo'), data.get('password'), data.get('telefono'),
        );
      }
      const customer = await api.clientePorUsuario(customerUser.id);
      const created = await api.crearCita({
        clienteId: customer.id,
        trabajadorId: Number(workerId),
        servicioId: Number(serviceId),
        fechaHoraInicio: `${date}T${time}:00`,
        notasCliente: notes.trim() || null,
        estado: 'PENDIENTE',
      });
      setBooking(created);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setBusy(false);
    }
  }

  function restart() {
    setBooking(null);
    setStep(1);
    setServiceId('');
    setWorkerId('');
    setDate('');
    setTime('');
    setNotes('');
  }

  return <><SiteHeader active="reserva" /><main className="site-section site-booking">
    <p className="site-eyebrow">Reserva tu momento</p>
    <h1>Agenda tu próxima visita</h1>
    {!booking && <div className="site-booking-steps" aria-label="Pasos de reserva">{steps.map((label, index) => <button type="button" key={label} className={step === index + 1 ? 'active' : step > index + 1 ? 'complete' : ''} onClick={() => { if (index + 1 < step) setStep(index + 1); }} disabled={index + 1 >= step}><span>{index + 1}</span>{label}</button>)}</div>}
    {error && <p className="site-form-error" role="alert">{error}</p>}
    {catalogLoading && <p className="site-message" role="status">Cargando servicios y especialistas…</p>}
    {!catalogLoading && !booking && <div className="site-booking-layout"><section className="site-booking-main">
      {step === 1 && <section className="site-booking-panel"><h2>1. Elige tu servicio</h2><div className="site-booking-options">{services.map((item) => <button key={item.id} type="button" className={`site-booking-option ${String(item.id) === String(serviceId) ? 'selected' : ''}`} onClick={() => chooseService(item.id)}><span><strong>{item.nombre}</strong><small>{item.duracionMinutos} min</small></span><strong>{money(item.precio)}</strong></button>)}</div></section>}
      {step === 2 && <section className="site-booking-panel"><h2>2. Elige a tu especialista</h2><div className="site-booking-options">{workers.map((item) => <button key={item.id} type="button" className={`site-booking-option ${String(item.id) === String(workerId) ? 'selected' : ''}`} onClick={() => chooseWorker(item.id)}><span><strong>{workerName(item)}</strong><small>{item.cargoEspecialidad || 'Especialista'}</small></span><span aria-hidden="true">→</span></button>)}</div><button type="button" className="site-text-button" onClick={() => setStep(1)}>Cambiar servicio</button></section>}
      {step === 3 && <section className="site-booking-panel"><h2>3. Selecciona fecha y hora</h2><p className="site-muted">{service?.nombre} · {workerName(worker)}</p><DaySelector label="Elige un día" value={date} onChange={(selectedDate) => { setDate(selectedDate); setTime(''); }} minDate={localDate()} />{date && <div className="site-slot-grid">{slotsLoading ? <p role="status">Consultando disponibilidad…</p> : slots.length ? slots.map((slot) => { const available = Boolean(slot.disponible); const label = timeLabel(slot.horaInicio); return <button type="button" key={slot.horaInicio} disabled={!available} title={available ? 'Disponible' : unavailableLabel(slot.motivoNoDisponible)} aria-label={`${label}, ${available ? 'disponible' : unavailableLabel(slot.motivoNoDisponible)}`} className={`${available ? '' : 'unavailable'} ${label === time ? 'selected' : ''}`} onClick={() => available && setTime(label)}>{label}{!available && <small>{unavailableLabel(slot.motivoNoDisponible)}</small>}</button>; }) : !error && <p>No hay horas disponibles para esta fecha. Prueba otro día.</p>}</div>}<div className="site-booking-actions"><button type="button" className="site-button site-button-outline" onClick={() => setStep(2)}>Atrás</button><button type="button" className="site-button" disabled={!date || !time} onClick={() => setStep(4)}>Continuar</button></div></section>}
      {step === 4 && <section className="site-booking-panel"><h2>4. Confirma tus datos</h2>{user && !isClient ? <div className="site-message"><p>La sesión activa no tiene un perfil de cliente para reservar.</p><button type="button" className="site-text-button" onClick={logout}>Cerrar sesión y continuar como cliente</button></div> : <form onSubmit={confirmBooking}>{!user && <><p className="site-muted">Crea tu cuenta para guardar la reserva y administrar tus citas. ¿Ya tienes una cuenta? <a href={loginHref}>Inicia sesión</a>.</p><label>Nombre<input name="nombre" autoComplete="given-name" required /></label><label>Apellido paterno<input name="apellidoP" autoComplete="family-name" required /></label><label>Correo<input name="correo" type="email" autoComplete="email" required /></label><label>Contraseña<input name="password" type="password" minLength={6} maxLength={72} autoComplete="new-password" required /></label><label>Teléfono (opcional)<input name="telefono" type="tel" autoComplete="tel" /></label></>}{user && <p className="site-muted">Reserva para {user.nombre} ({user.correo})</p>}<label>Notas para tu especialista<textarea value={notes} onChange={(event) => setNotes(event.target.value)} rows={3} /></label><div className="site-booking-actions"><button type="button" className="site-button site-button-outline" onClick={() => setStep(3)}>Atrás</button><button type="submit" className="site-button" disabled={busy}>{busy ? 'Agendando…' : 'Confirmar reserva'}</button></div></form>}</section>}
    </section><aside className="site-booking-summary"><p className="site-eyebrow">Resumen</p><h2>{service?.nombre || 'Elige un servicio'}</h2><p>{worker ? workerName(worker) : 'Especialista por elegir'}</p><p>{date || 'Fecha por elegir'} {time && `· ${time}`}</p>{service && <div className="site-booking-total"><span>{service.duracionMinutos} minutos</span><strong>{money(service.precio)}</strong></div>}</aside></div>}
    {booking && <section className="site-booking-panel site-booking-confirmation"><p className="site-eyebrow">Reserva recibida</p><h2>¡Gracias! Tu cita está agendada.</h2><p>El estado actual es <strong>{booking.estado}</strong>. El profesional podrá confirmarla desde su panel.</p><dl><div><dt>Código de reserva</dt><dd>{booking.codigoReserva}</dd></div><div><dt>Servicio</dt><dd>{service?.nombre}</dd></div><div><dt>Especialista</dt><dd>{workerName(worker)}</dd></div><div><dt>Fecha y hora</dt><dd>{date} · {time}</dd></div></dl><button type="button" className="site-button" onClick={restart}>Agendar otra cita</button></section>}
  </main><SiteFooter /></>;
}
