import { useState } from 'react';
import { useAuth } from '../auth/AuthContext.jsx';

export function SiteHeader({ active }) {
  const [menuOpen, setMenuOpen] = useState(false);
  const { user, logout } = useAuth();
  const role = user?.rol?.toUpperCase() || '';
  const isClient = role.includes('CLIENTE');
  const isWorker = ['PROFESIONAL', 'TRABAJADOR', 'ESPECIALISTA'].some((candidate) => role.includes(candidate));
  const isAdmin = role.includes('ADMIN');
  return (
    <header className="site-topbar">
      <a className="site-brand" href="/">nicolet<small>STUDIO FANTASY</small></a>
      <button
        className={`site-menu-toggle${menuOpen ? ' is-open' : ''}`}
        type="button"
        aria-label={menuOpen ? 'Cerrar menú' : 'Abrir menú'}
        aria-expanded={menuOpen}
        aria-controls="site-navigation"
        onClick={() => setMenuOpen((open) => !open)}
      >
        <span />
        <span />
        <span />
      </button>
      <nav id="site-navigation" className={menuOpen ? 'is-open' : ''} aria-label="Navegación principal">
        <a className={active === 'inicio' ? 'active' : ''} href="/" onClick={() => setMenuOpen(false)}>Inicio</a>
        <a className={active === 'servicios' ? 'active' : ''} href="/servicios" onClick={() => setMenuOpen(false)}>Servicios</a>
        <a className={active === 'equipo' ? 'active' : ''} href="/equipo" onClick={() => setMenuOpen(false)}>Nuestro equipo</a>
        {isClient && <a className={active === 'perfil' ? 'active' : ''} href="/mi-perfil" onClick={() => setMenuOpen(false)}>Mi perfil</a>}
        {!user && <a className="site-mobile-login site-button site-button-outline" href="/admin/login" onClick={() => setMenuOpen(false)}>Iniciar sesión</a>}
      </nav>
      <div className="site-actions">
        {user ? <><span className="site-user">Hola, {user.nombre}</span>{isWorker && <a className="site-button site-button-outline" href="/admin/panel-trabajador">Mi panel</a>}{isAdmin && <a className="site-button site-button-outline" href="/admin/citas">Administración</a>}<button type="button" className="site-link-button" onClick={logout}>Salir</button></> : <a className="site-button site-button-outline site-login-link" href="/admin/login">Iniciar sesión</a>}
        {(!user || isClient) && <a className="site-button" href="/reservar">Reservar hora</a>}
      </div>
    </header>
  );
}

export function SiteFooter() {
  return <footer className="site-footer">
    <div className="site-footer-brand">
      <a className="site-brand" href="/">nicolet<span>.</span></a>
      <span>Un espacio para ti. Un cuidado que se siente.</span>
    </div>
    <address className="site-footer-contact">
      <div><strong>Ubicación</strong><a href="https://www.google.com/maps/search/?api=1&query=Los+Ciruelos%2C+Buin" target="_blank" rel="noreferrer">Los Ciruelos, Buin</a></div>
      <div><strong>Instagram</strong><a href="https://www.instagram.com/nicolettstudiofantasy/" target="_blank" rel="noreferrer">@nicolettstudiofantasy</a></div>
      <div><strong>WhatsApp</strong><a href="https://wa.me/56940322551" target="_blank" rel="noreferrer">+56 9 4032 2551</a></div>
    </address>
    <span className="site-footer-copyright">© {new Date().getFullYear()} Nicolett Studio Fantasy · Buin, Chile</span>
  </footer>;
}
