import { useAuth } from '../auth/AuthContext.jsx';

export function SiteHeader({ active }) {
  const { user, logout } = useAuth();
  const role = user?.rol?.toUpperCase() || '';
  const isClient = role.includes('CLIENTE');
  const isWorker = ['PROFESIONAL', 'TRABAJADOR', 'ESPECIALISTA'].some((candidate) => role.includes(candidate));
  const isAdmin = role.includes('ADMIN');
  return (
    <header className="site-topbar">
      <a className="site-brand" href="/">nicolet<span>.</span><small>STUDIO FANTASY</small></a>
      <nav aria-label="Navegación principal">
        <a className={active === 'inicio' ? 'active' : ''} href="/">Inicio</a>
        <a className={active === 'servicios' ? 'active' : ''} href="/servicios">Servicios</a>
        <a className={active === 'equipo' ? 'active' : ''} href="/equipo">Nuestro equipo</a>
        {isClient && <a className={active === 'perfil' ? 'active' : ''} href="/mi-perfil">Mi perfil</a>}
      </nav>
      <div className="site-actions">
        {user ? <><span className="site-user">Hola, {user.nombre}</span>{isWorker && <a className="site-button site-button-outline" href="/admin/panel-trabajador">Mi panel</a>}{isAdmin && <a className="site-button site-button-outline" href="/admin/citas">Administración</a>}<button type="button" className="site-link-button" onClick={logout}>Salir</button></> : <a className="site-button site-button-outline site-login-link" href="/admin/login">Iniciar sesión</a>}
        {(!user || isClient) && <a className="site-button" href="/reservar">Reservar hora</a>}
      </div>
    </header>
  );
}

export function SiteFooter() {
  return <footer className="site-footer"><a className="site-brand" href="/">nicolet<span>.</span></a><span>Un espacio para ti. Un cuidado que se siente.</span><span>© {new Date().getFullYear()} Nicolett Studio Fantasy · Buin, Chile</span></footer>;
}
