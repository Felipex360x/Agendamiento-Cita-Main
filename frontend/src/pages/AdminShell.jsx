import { useEffect } from 'react';
import { useAuth } from '../auth/AuthContext.jsx';

const adminLinks = [
  ['/admin/citas', 'Citas'],
  ['/agenda', 'Agenda'],
  ['/dashboard', 'Resumen'],
  ['/clientas', 'Clientas'],
  ['/admin/servicios', 'Servicios'],
  ['/admin/disponibilidad', 'Disponibilidad'],
  ['/admin/historial', 'Historial'],
  ['/admin/usuarios', 'Usuarios'],
  ['/admin/panel-trabajador', 'Portal profesional'],
];
const workerLinks = [
  ['/admin/panel-trabajador', 'Mis citas'],
  ['/admin/mi-horario', 'Mi horario'],
  ['/agenda', 'Mi agenda'],
  ['/clientas', 'Mis clientes'],
];

export function AdminShell({ title, children, active, roles = ['ADMINISTRADOR'] }) {
  const { user, loading, logout } = useAuth();
  const role = user?.rol?.toUpperCase() || '';
  const allowed = user && roles.some((candidate) => role.includes(candidate));
  const navigation = role.includes('ADMIN') ? adminLinks : workerLinks;

  useEffect(() => {
    if (!loading && !user) {
      const redirect = `${window.location.pathname}${window.location.search}`;
      window.location.replace(`/admin/login?redirect=${encodeURIComponent(redirect)}`);
    }
  }, [loading, user]);

  if (loading || !user) return <main className="admin-loading" role="status">Verificando sesión…</main>;
  if (!allowed) return <main className="admin-loading"><h1>Acceso restringido</h1><p>Tu cuenta no tiene permiso para abrir este panel.</p><a href="/">Volver al inicio</a></main>;

  return <div className="admin-app"><header className="admin-topbar"><a className="site-brand" href="/">nicolet<span>.</span><small>ESTUDIO</small></a><nav aria-label={role.includes('ADMIN') ? 'Administración' : 'Portal profesional'}>{navigation.map(([href, label]) => <a key={href} className={active === href ? 'active' : ''} href={href}>{label}</a>)}</nav><div className="admin-identity"><span>{user.nombre} · {user.rol}</span><button type="button" onClick={logout}>Salir</button></div></header><main className="admin-content"><div className="admin-page-heading"><p className="site-eyebrow">{role.includes('ADMIN') ? 'Administración' : 'Mi espacio profesional'}</p><h1>{title}</h1></div>{children}</main></div>;
}

export function RequestMessage({ error, notice }) {
  if (error) return <p className="admin-alert error" role="alert">{error}</p>;
  if (notice) return <p className="admin-alert success" role="status">{notice}</p>;
  return null;
}

export function EmptyState({ loading, error, empty, children }) {
  if (loading) return <p className="admin-empty" role="status">Cargando…</p>;
  if (error) return <p className="admin-empty error" role="alert">{error}</p>;
  if (empty) return <p className="admin-empty">No hay registros para mostrar.</p>;
  return children;
}

export function personName(person) {
  return [person?.usuario?.nombre, person?.usuario?.apellidoP].filter(Boolean).join(' ') || '—';
}
