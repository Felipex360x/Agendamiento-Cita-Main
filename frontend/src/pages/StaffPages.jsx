import { useEffect, useMemo, useState } from 'react';
import { api } from '../api/client.js';
import { useAuth } from '../auth/AuthContext.jsx';
import { AdminShell, EmptyState, RequestMessage, personName } from './AdminShell.jsx';
import { computeDashboard } from './dashboardStats.js';
import { DashboardSkeleton, KpiCard, PeriodFilter, RankingList, RevenueChart, StatusBreakdown, formatMoney } from './DashboardWidgets.jsx';

const asDate = (value) => value ? new Date(`${value.slice(0, 10)}T12:00:00`) : null;
const dateKey = (value) => value instanceof Date ? `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, '0')}-${String(value.getDate()).padStart(2, '0')}` : '';
const localNotesKey = (userId, id) => `nicolet_client_notes_${userId}_${id}`;

function mondayOf(date) {
  const monday = new Date(date.getFullYear(), date.getMonth(), date.getDate(), 12);
  monday.setDate(monday.getDate() - ((monday.getDay() + 6) % 7));
  return monday;
}

export function StaffAgendaPage() {
  const { user } = useAuth();
  const [appointments, setAppointments] = useState([]);
  const [workers, setWorkers] = useState([]);
  const [workerId, setWorkerId] = useState('');
  const [weekOffset, setWeekOffset] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const administrator = user?.rol?.toUpperCase().includes('ADMIN');
  const monday = mondayOf(new Date());
  monday.setDate(monday.getDate() + weekOffset * 7);

  useEffect(() => {
    if (!user) return;
    let active = true;
    const loadAppointments = administrator
      ? api.trabajadores().then(async (list) => {
        if (active) setWorkers(list || []);
        return api.citas();
      })
      : api.trabajadorPorUsuario(user.id).then((ownWorker) => {
        if (active) { setWorkers([ownWorker]); setWorkerId(String(ownWorker.id)); }
        return api.citasDeTrabajador(ownWorker.id);
      });
    loadAppointments.then((list) => { if (active) setAppointments(list || []); })
      .catch((requestError) => { if (active) setError(requestError.message); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [user?.id, administrator]);

  const days = Array.from({ length: 5 }, (_, index) => { const day = new Date(monday); day.setDate(monday.getDate() + index); return day; });
  const visible = appointments.filter((appointment) => {
    const day = appointment.fechaHoraInicio?.slice(0, 10);
    const inWeek = days.some((value) => dateKey(value) === day);
    return inWeek && (!administrator || !workerId || String(appointment.trabajador?.id) === workerId);
  }).sort((a, b) => a.fechaHoraInicio.localeCompare(b.fechaHoraInicio));

  return <AdminShell title="Agenda semanal" active="/agenda" roles={['ADMINISTRADOR', 'PROFESIONAL', 'TRABAJADOR', 'ESPECIALISTA']}>
    <RequestMessage error={error} />
    <section className="admin-panel admin-week-controls"><button type="button" onClick={() => setWeekOffset((offset) => offset - 1)}>← Semana anterior</button><strong>{days[0].toLocaleDateString('es-CL', { day: 'numeric', month: 'short' })} – {days[4].toLocaleDateString('es-CL', { day: 'numeric', month: 'long', year: 'numeric' })}</strong><button type="button" onClick={() => setWeekOffset((offset) => offset + 1)}>Semana siguiente →</button>{administrator && <select aria-label="Filtrar profesional" value={workerId} onChange={(event) => setWorkerId(event.target.value)}><option value="">Todos los profesionales</option>{workers.map((worker) => <option key={worker.id} value={worker.id}>{personName(worker)}</option>)}</select>}</section>
    <section className="admin-panel"><h2>Reservas de la semana</h2><EmptyState loading={loading} error={error} empty={!visible.length}><div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Fecha</th><th>Hora</th><th>Cliente</th><th>Servicio</th><th>Profesional</th><th>Estado</th></tr></thead><tbody>{visible.map((appointment) => <tr key={appointment.id}><td>{asDate(appointment.fechaHoraInicio)?.toLocaleDateString('es-CL', { weekday: 'short', day: 'numeric', month: 'short' })}</td><td>{appointment.fechaHoraInicio?.slice(11, 16)}</td><td>{personName(appointment.cliente)}</td><td>{appointment.servicio?.nombre}</td><td>{personName(appointment.trabajador)}</td><td><span className={`admin-status ${appointment.estado?.toLowerCase()}`}>{appointment.estado}</span></td></tr>)}</tbody></table></div></EmptyState></section>
  </AdminShell>;
}

export function StaffDashboardPage() {
  const { user } = useAuth();
  const [appointments, setAppointments] = useState([]);
  const [days, setDays] = useState(30);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  useEffect(() => {
    if (!user?.rol?.toUpperCase().includes('ADMIN')) { setLoading(false); return undefined; }
    let active = true;
    setLoading(true);
    setError('');
    api.citas()
      .then((list) => { if (active) setAppointments(list || []); })
      .catch((requestError) => { if (active) setError(requestError.message); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [user?.id, user?.rol]);

  const stats = useMemo(() => computeDashboard(appointments, { days }), [appointments, days]);
  const { kpis } = stats;
  const periodText = days ? `Últimos ${days} días` : 'Todo el historial';
  const recent = useMemo(() => appointments.slice().sort((a, b) => (b.fechaHoraInicio || '').localeCompare(a.fechaHoraInicio || '')).slice(0, 8), [appointments]);

  return <AdminShell title="Resumen del estudio" active="/dashboard" roles={['ADMINISTRADOR']}>
    <RequestMessage error={error} />
    {loading ? <DashboardSkeleton /> : !error && <div className="dash">
      <div className="dash-toolbar">
        <p>{periodText} · {asDate(stats.range.start)?.toLocaleDateString('es-CL', { day: 'numeric', month: 'short', year: 'numeric' })} – {asDate(stats.range.end)?.toLocaleDateString('es-CL', { day: 'numeric', month: 'short', year: 'numeric' })}</p>
        <PeriodFilter days={days} onChange={setDays} />
      </div>
      <div className="dash-kpis">
        <KpiCard label="Ingresos" value={formatMoney(kpis.revenue.value)} change={kpis.revenue.change} series={kpis.revenue.series} hint="Solo citas completadas" />
        <KpiCard label="Citas" value={kpis.appointments.value} change={kpis.appointments.change} series={kpis.appointments.series} hint="Todas las agendadas en el período" />
        <KpiCard label="Clientas atendidas" value={kpis.clients.value} change={kpis.clients.change} series={kpis.clients.series} hint="Clientas únicas con cita completada" />
        <KpiCard label="Tasa de cancelación" value={`${String(kpis.cancelRate.value).replace('.', ',')}%`} change={kpis.cancelRate.change} unit="pp" worseWhenUp series={kpis.cancelRate.series} hint="Canceladas sobre el total de citas" />
      </div>
      <section className="admin-panel dash-panel">
        <header className="dash-panel-head"><h2>Ingresos</h2><span>por {stats.granularity === 'day' ? 'día' : stats.granularity === 'week' ? 'semana' : 'mes'}</span></header>
        <RevenueChart timeline={stats.timeline} granularity={stats.granularity} />
      </section>
      <div className="dash-grid-2">
        <section className="admin-panel dash-panel"><header className="dash-panel-head"><h2>Estado de las citas</h2><span>{stats.total} en total</span></header><StatusBreakdown statuses={stats.statuses} total={stats.total} /></section>
        <section className="admin-panel dash-panel">
          <header className="dash-panel-head"><h2>Hoy y mañana</h2><span>{stats.upcoming.length} {stats.upcoming.length === 1 ? 'cita' : 'citas'}</span></header>
          {stats.upcoming.length ? <ul className="dash-agenda">{stats.upcoming.map((appointment) => <li key={appointment.id}>
            <time dateTime={appointment.fechaHoraInicio}><strong>{appointment.fechaHoraInicio.slice(11, 16)}</strong><small>{appointment.fechaHoraInicio.slice(0, 10) === dateKey(new Date()) ? 'Hoy' : 'Mañana'}</small></time>
            <div><span>{personName(appointment.cliente)}</span><small>{appointment.servicio?.nombre || '—'} · {personName(appointment.trabajador)}</small></div>
            <span className={`admin-status ${appointment.estado?.toLowerCase()}`}>{appointment.estado?.replace('_', ' ')}</span>
          </li>)}</ul> : <p className="admin-empty">No hay citas para hoy ni mañana.</p>}
        </section>
      </div>
      <div className="dash-grid-2">
        <section className="admin-panel dash-panel"><header className="dash-panel-head"><h2>Profesionales</h2><span>citas e ingresos</span></header><RankingList items={stats.byWorker} /></section>
        <section className="admin-panel dash-panel"><header className="dash-panel-head"><h2>Servicios más elegidos</h2><span>citas e ingresos</span></header><RankingList items={stats.byService} /></section>
      </div>
      <section className="admin-panel dash-panel">
        <header className="dash-panel-head"><h2>Últimas reservas</h2><a href="/admin/citas">Ver todas →</a></header>
        {recent.length ? <div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Clienta</th><th>Servicio</th><th>Profesional</th><th>Fecha</th><th>Estado</th></tr></thead><tbody>{recent.map((appointment) => <tr key={appointment.id}><td>{personName(appointment.cliente)}</td><td>{appointment.servicio?.nombre}</td><td>{personName(appointment.trabajador)}</td><td>{appointment.fechaHoraInicio?.replace('T', ' ').slice(0, 16)}</td><td><span className={`admin-status ${appointment.estado?.toLowerCase()}`}>{appointment.estado?.replace('_', ' ')}</span></td></tr>)}</tbody></table></div> : <p className="admin-empty">Aún no hay reservas registradas.</p>}
      </section>
    </div>}
  </AdminShell>;
}

export function StaffClientsPage() {
  const { user } = useAuth();
  const administrator = user?.rol?.toUpperCase().includes('ADMIN');
  const [clients, setClients] = useState([]);
  const [selectedId, setSelectedId] = useState('');
  const [appointments, setAppointments] = useState([]);
  const [workerAppointments, setWorkerAppointments] = useState([]);
  const [query, setQuery] = useState('');
  const [note, setNote] = useState('');
  const [notes, setNotes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [error, setError] = useState('');
  useEffect(() => {
    if (!user) return undefined;
    let active = true;
    const loadClients = administrator
      ? api.clientes().then((list) => { if (active) setClients(list || []); })
      : api.trabajadorPorUsuario(user.id)
        .then((worker) => api.citasDeTrabajador(worker.id))
        .then((list) => {
          if (!active) return;
          const ownAppointments = list || [];
          setWorkerAppointments(ownAppointments);
          const uniqueClients = new Map();
          ownAppointments.forEach((appointment) => {
            if (appointment.cliente) uniqueClients.set(String(appointment.cliente.id), appointment.cliente);
          });
          setClients([...uniqueClients.values()]);
        });
    loadClients.catch((requestError) => { if (active) setError(requestError.message); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [administrator, user?.id]);
  useEffect(() => {
    if (!selectedId) return;
    if (!administrator) {
      setAppointments(workerAppointments.filter((appointment) => String(appointment.cliente?.id) === selectedId));
      setHistoryLoading(false);
      try { setNotes(JSON.parse(localStorage.getItem(localNotesKey(user.id, selectedId)) || '[]')); } catch { setNotes([]); }
      return undefined;
    }
    let active = true;
    setHistoryLoading(true);
    api.citasDeCliente(selectedId).then((list) => { if (active) setAppointments(list || []); }).catch((requestError) => { if (active) setError(requestError.message); }).finally(() => { if (active) setHistoryLoading(false); });
    try { setNotes(JSON.parse(localStorage.getItem(localNotesKey(user.id, selectedId)) || '[]')); } catch { setNotes([]); }
    return () => { active = false; };
  }, [selectedId, administrator, workerAppointments]);

  const selected = clients.find((client) => String(client.id) === selectedId);
  const filtered = useMemo(() => clients.filter((client) => personName(client).toLowerCase().includes(query.toLowerCase())), [clients, query]);

  function saveNote() {
    const text = note.trim();
    if (!text || !selectedId) return;
    const next = [{ id: Date.now(), text, date: new Date().toLocaleString('es-CL') }, ...notes];
    setNotes(next);
    localStorage.setItem(localNotesKey(user.id, selectedId), JSON.stringify(next));
    setNote('');
  }

  return <AdminShell title={administrator ? 'Historial de clientas' : 'Mis clientes'} active="/clientas" roles={['ADMINISTRADOR', 'PROFESIONAL', 'TRABAJADOR', 'ESPECIALISTA']}>
    <RequestMessage error={error} />
    <div className="admin-client-layout"><section className="admin-panel"><h2>Clientas</h2><input aria-label="Buscar clienta" placeholder="Buscar…" value={query} onChange={(event) => setQuery(event.target.value)} /><EmptyState loading={loading} error={error} empty={!filtered.length}><div className="admin-client-list">{filtered.map((client) => <button type="button" key={client.id} className={String(client.id) === selectedId ? 'active' : ''} onClick={() => setSelectedId(String(client.id))}>{personName(client)}<small>{client.usuario?.correo}</small></button>)}</div></EmptyState></section>
      <section className="admin-panel">{selected ? <><p className="site-eyebrow">Ficha de clienta</p><h2>{personName(selected)}</h2><p>{selected.usuario?.correo} · {selected.telefono || 'Sin teléfono'}</p><h3>Historial de atenciones</h3><EmptyState loading={historyLoading} error={error} empty={!appointments.length}><div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Fecha</th><th>Servicio</th><th>Profesional</th><th>Estado</th></tr></thead><tbody>{appointments.map((appointment) => <tr key={appointment.id}><td>{appointment.fechaHoraInicio?.replace('T', ' ').slice(0, 16)}</td><td>{appointment.servicio?.nombre}</td><td>{personName(appointment.trabajador)}</td><td><span className={`admin-status ${appointment.estado?.toLowerCase()}`}>{appointment.estado}</span></td></tr>)}</tbody></table></div></EmptyState><h3>Observaciones</h3><textarea className="admin-note-input" rows={3} placeholder="Escribe una observación de atención…" value={note} onChange={(event) => setNote(event.target.value)} /><button className="site-button" type="button" onClick={saveNote} disabled={!note.trim()}>Guardar observación</button><div className="admin-note-list">{notes.map((item) => <article key={item.id}><small>{item.date}</small><p>{item.text}</p></article>)}</div></> : <p className="admin-empty">Elige una clienta para ver sus visitas y notas.</p>}</section></div>
  </AdminShell>;
}
