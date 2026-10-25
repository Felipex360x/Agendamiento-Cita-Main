import { useEffect, useState } from 'react';
import { api } from '../api/client.js';
import { useAuth } from '../auth/AuthContext.jsx';
import { AdminShell, RequestMessage, personName } from './AdminShell.jsx';

const weekdays = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];

function timeValue(value) { return value ? String(value).slice(0, 5) : ''; }

function normalizeSchedule(schedule) {
  return (schedule || []).map((day) => ({
    ...day,
    horaInicio: timeValue(day.horaInicio),
    horaFin: timeValue(day.horaFin),
    horaInicioDescanso: timeValue(day.horaInicioDescanso),
    horaFinDescanso: timeValue(day.horaFinDescanso),
  }));
}

export default function WorkerSchedulePage() {
  const { user } = useAuth();
  const [worker, setWorker] = useState(null);
  const [schedule, setSchedule] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  useEffect(() => {
    if (!user) return undefined;
    let active = true;
    api.trabajadorPorUsuario(user.id)
      .then(async (profile) => {
        if (!active) return;
        setWorker(profile);
        const ownSchedule = await api.horarioTrabajador(profile.id);
        if (active) setSchedule(normalizeSchedule(ownSchedule));
      })
      .catch((requestError) => { if (active) setError(requestError.message); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [user?.id]);

  function updateDay(day, changes) {
    setSchedule((current) => current.map((item) => item.diaSemana === day ? { ...item, ...changes } : item));
  }

  async function save(event) {
    event.preventDefault();
    if (!worker) return;
    setSaving(true);
    setError('');
    setNotice('');
    try {
      const updated = await api.actualizarHorarioTrabajador(worker.id, schedule.map((day) => ({
        ...day,
        horaInicioDescanso: day.horaInicioDescanso || null,
        horaFinDescanso: day.horaFinDescanso || null,
      })));
      setSchedule(normalizeSchedule(updated));
      setNotice('Tu horario quedó actualizado. Los clientes verán los bloques de colación como no disponibles.');
    } catch (requestError) { setError(requestError.message); }
    finally { setSaving(false); }
  }

  return <AdminShell title="Mi horario" active="/admin/mi-horario" roles={['PROFESIONAL', 'TRABAJADOR', 'ESPECIALISTA']}>
    <RequestMessage error={error} notice={notice} />
    {worker && <section className="admin-panel admin-worker-profile"><span className="site-avatar">{`${worker.usuario?.nombre?.[0] || ''}${worker.usuario?.apellidoP?.[0] || ''}`}</span><div><h2>{personName(worker)}</h2><p>{worker.cargoEspecialidad}</p></div></section>}
    <section className="admin-panel">
      <h2>Horario de atención semanal</h2>
      <p className="admin-schedule-help">Activa los días en que atiendes y configura tu jornada. Si bloqueas una colación, esos horarios aparecerán en gris y no se podrán reservar.</p>
      {loading ? <p className="admin-empty" role="status">Cargando tu horario…</p> : <form onSubmit={save}>
        <div className="admin-schedule-list">{schedule.map((day) => {
          const breakEnabled = Boolean(day.horaInicioDescanso && day.horaFinDescanso);
          return <fieldset className="admin-schedule-day" key={day.diaSemana}>
            <legend><label className="admin-schedule-toggle"><input type="checkbox" checked={Boolean(day.activo)} onChange={(event) => updateDay(day.diaSemana, { activo: event.target.checked })} /><strong>{weekdays[day.diaSemana - 1]}</strong></label></legend>
            <label>Desde<input type="time" required value={day.horaInicio} onChange={(event) => updateDay(day.diaSemana, { horaInicio: event.target.value })} /></label>
            <label>Hasta<input type="time" required value={day.horaFin} onChange={(event) => updateDay(day.diaSemana, { horaFin: event.target.value })} /></label>
            <label className="admin-schedule-toggle"><input type="checkbox" checked={breakEnabled} onChange={(event) => updateDay(day.diaSemana, event.target.checked ? { horaInicioDescanso: '13:00', horaFinDescanso: '14:00' } : { horaInicioDescanso: '', horaFinDescanso: '' })} />Bloquear colación</label>
            {breakEnabled && <><label>Colación desde<input type="time" required value={day.horaInicioDescanso} onChange={(event) => updateDay(day.diaSemana, { horaInicioDescanso: event.target.value })} /></label><label>Colación hasta<input type="time" required value={day.horaFinDescanso} onChange={(event) => updateDay(day.diaSemana, { horaFinDescanso: event.target.value })} /></label></>}
          </fieldset>;
        })}</div>
        <button className="site-button" type="submit" disabled={saving || schedule.length !== 7}>{saving ? 'Guardando horario…' : 'Guardar mi horario'}</button>
      </form>}
    </section>
  </AdminShell>;
}
