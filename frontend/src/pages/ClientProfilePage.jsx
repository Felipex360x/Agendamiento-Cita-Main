import { useEffect, useState } from 'react';
import { api } from '../api/client.js';
import { useAuth } from '../auth/AuthContext.jsx';
import { SiteFooter, SiteHeader } from './SiteShell.jsx';

const blankProfile = { nombre: '', apellidoP: '', correo: '', telefono: '', rutDni: '', fechaNacimiento: '', notasPreferencias: '' };

function profileForm(client) {
  return {
    nombre: client.usuario?.nombre || '',
    apellidoP: client.usuario?.apellidoP || '',
    correo: client.usuario?.correo || '',
    telefono: client.telefono || '',
    rutDni: client.rutDni || '',
    fechaNacimiento: client.fechaNacimiento || '',
    notasPreferencias: client.notasPreferencias || '',
  };
}

export default function ClientProfilePage() {
  const { user, loading: authLoading, updateProfile, logout } = useAuth();
  const [client, setClient] = useState(null);
  const [appointments, setAppointments] = useState([]);
  const [form, setForm] = useState(blankProfile);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [emailChanged, setEmailChanged] = useState(false);
  const isClient = user?.rol?.toUpperCase().includes('CLIENTE');
  const userInitials = `${user?.nombre?.[0] || ''}${user?.apellidoP?.[0] || ''}`.toUpperCase() || 'NS';

  useEffect(() => {
    document.title = 'Mi perfil | Nicolett Studio Fantasy';
    if (!user || !isClient) { setLoading(false); return undefined; }
    let active = true;
    api.clientePorUsuario(user.id)
      .then(async (profile) => {
        if (!active) return;
        setClient(profile);
        setForm(profileForm(profile));
        const history = await api.citasDeCliente(profile.id);
        if (active) setAppointments(history || []);
      })
      .catch((requestError) => { if (active) setError(requestError.message); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [user?.id, isClient]);

  async function save(event) {
    event.preventDefault();
    setSaving(true);
    setError('');
    setNotice('');
    try {
      const updated = await api.actualizarPerfilCliente(client.id, { ...form, fechaNacimiento: form.fechaNacimiento || null });
      setClient(updated);
      setForm(profileForm(updated));
      const emailChanged = updated.usuario.correo.toLowerCase() !== user.correo.toLowerCase();
      updateProfile({ nombre: updated.usuario.nombre, apellidoP: updated.usuario.apellidoP, correo: updated.usuario.correo });
      if (emailChanged) {
        setEmailChanged(true);
        logout();
      } else {
        setNotice('Tus datos se guardaron correctamente.');
      }
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setSaving(false);
    }
  }

  if (authLoading) return <main className="admin-loading" role="status">Verificando sesión…</main>;
  if (!user) return <><SiteHeader /><main className="site-section site-profile-page"><section className="site-message"><h1>{emailChanged ? 'Correo actualizado' : 'Inicia sesión para ver tu perfil'}</h1><p>{emailChanged ? 'Por seguridad, inicia sesión nuevamente con tu correo actualizado.' : 'Desde tu perfil podrás actualizar tus datos y revisar tus reservas.'}</p><a className="site-button" href="/admin/login?redirect=%2Fmi-perfil.html">Iniciar sesión</a></section></main><SiteFooter /></>;
  if (!isClient) return <><SiteHeader /><main className="site-section site-profile-page"><section className="site-message"><h1>Este espacio es para clientes</h1><p>Ingresa al panel correspondiente a tu tipo de usuario.</p><a className="site-button" href={user.rol?.toUpperCase().includes('ADMIN') ? '/admin/citas' : '/admin/panel-trabajador'}>Ir a mi espacio</a></section></main><SiteFooter /></>;

  return <><SiteHeader active="perfil" /><main className="site-section site-profile-page">
    <div className="site-profile-header-banner">
      <div className="site-avatar site-profile-avatar" aria-hidden="true">{userInitials}</div>
      <div className="site-profile-header-text">
        <p className="site-eyebrow">Tu espacio</p>
        <h1>Mi perfil</h1>
        <div className="site-profile-meta-pills">
          <span className="site-profile-pill"><strong>{user.nombre} {user.apellidoP || ''}</strong></span>
          <span className="site-profile-pill site-profile-pill-muted">{user.correo}</span>
          <span className="site-profile-pill site-profile-pill-gold">Cliente Nicolett</span>
        </div>
        <p className="site-muted">Actualiza tus datos y consulta las reservas asociadas a tu cuenta.</p>
      </div>
    </div>
    {error && <p className="site-form-error" role="alert">{error}</p>}
    {notice && <p className="admin-alert success" role="status">{notice}</p>}
    {loading ? <p className="site-message" role="status">Cargando tu perfil…</p> : <div className="site-profile-layout">
      <section className="site-profile-card"><h2>Mis datos</h2><form className="site-profile-form" onSubmit={save}>
        <label>Nombre<input required value={form.nombre} onChange={(event) => setForm({ ...form, nombre: event.target.value })} /></label>
        <label>Apellido<input required value={form.apellidoP} onChange={(event) => setForm({ ...form, apellidoP: event.target.value })} /></label>
        <label>Correo electrónico<input required type="email" value={form.correo} onChange={(event) => setForm({ ...form, correo: event.target.value })} /></label>
        <label>Teléfono<input type="tel" value={form.telefono} onChange={(event) => setForm({ ...form, telefono: event.target.value })} /></label>
        <label>RUT / DNI<input value={form.rutDni} onChange={(event) => setForm({ ...form, rutDni: event.target.value })} /></label>
        <label>Fecha de nacimiento<input type="date" value={form.fechaNacimiento} onChange={(event) => setForm({ ...form, fechaNacimiento: event.target.value })} /></label>
        <label className="wide">Preferencias o información para tus atenciones<textarea rows={3} value={form.notasPreferencias} onChange={(event) => setForm({ ...form, notasPreferencias: event.target.value })} /></label>
        <button className="site-button" type="submit" disabled={saving || loading}>{saving ? 'Guardando…' : 'Guardar cambios'}</button>
      </form></section>
      <section className="site-profile-card"><div className="site-profile-bookings-heading"><div><h2>Mis reservas</h2><p>Consulta las citas asociadas a tu cuenta.</p></div><a className="site-button" href="/reservar">Reservar hora</a></div>
        {appointments.length ? <div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Fecha</th><th>Servicio</th><th>Profesional</th><th>Estado</th><th>Código</th></tr></thead><tbody>{appointments.map((appointment) => <tr key={appointment.id}><td>{appointment.fechaHoraInicio?.replace('T', ' ').slice(0, 16)}</td><td>{appointment.servicio?.nombre || '—'}</td><td>{[appointment.trabajador?.usuario?.nombre, appointment.trabajador?.usuario?.apellidoP].filter(Boolean).join(' ') || '—'}</td><td><span className={`admin-status ${appointment.estado?.toLowerCase()}`}>{appointment.estado}</span></td><td>{appointment.codigoReserva}</td></tr>)}</tbody></table></div> : <p className="admin-empty">Todavía no tienes reservas.</p>}
      </section>
    </div>}
  </main><SiteFooter /></>;
}
