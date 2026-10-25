import { useEffect, useState } from 'react';
import { useAuth } from '../auth/AuthContext.jsx';
import { api } from '../api/client.js';
import { SiteFooter, SiteHeader } from './SiteShell.jsx';
import hairImage from '../assets/hair.jpg';
import nailsImage from '../assets/nails.jpg';
import salonImage from '../assets/salon-interior.jpg';

const money = (value) => new Intl.NumberFormat('es-CL', {
  style: 'currency', currency: 'CLP', maximumFractionDigits: 0,
}).format(Number(value) || 0);

function useCatalog(resource) {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    const load = resource === 'workers' ? api.trabajadores : api.servicios;
    load()
      .then((result) => { if (active) setItems(result || []); })
      .catch((requestError) => { if (active) setError(requestError.message); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [resource]);

  return { items, loading, error };
}

function ServiceCards({ services }) {
  return (
    <div className="site-card-grid">
      {services.map((service) => {
        const name = service.nombre || 'Servicio';
        const image = /manic|uñ/i.test(name) ? nailsImage : /facial|piel/i.test(name) ? salonImage : hairImage;
        return (
          <article className="site-service-card" key={service.id}>
            <img src={image} alt="" loading="lazy" />
            <div className="site-service-copy">
              <p className="site-eyebrow">{service.duracionMinutos || '—'} minutos</p>
              <h2>{name}</h2>
              <p>{service.descripcion || 'Un cuidado pensado para ti.'}</p>
              <div className="site-service-meta"><strong>{money(service.precio)}</strong><a href={`/reservar?servicioId=${service.id}`}>Reservar →</a></div>
            </div>
          </article>
        );
      })}
    </div>
  );
}

function CatalogState({ loading, error, empty, children }) {
  if (loading) return <p className="site-message" role="status">Cargando catálogo…</p>;
  if (error) return <p className="site-message site-error" role="alert">{error}. Inténtalo nuevamente en unos momentos.</p>;
  if (empty) return <p className="site-message">Aún no hay elementos disponibles.</p>;
  return children;
}

export function HomePage() {
  const { items, loading, error } = useCatalog('services');
  useEffect(() => { document.title = 'Nicolett Studio Fantasy'; }, []);

  return <><SiteHeader active="inicio" /><main>
    <section className="site-hero">
      <img src={salonImage} alt="Interior de Nicolett Studio Fantasy" />
      <div className="site-hero-copy"><p className="site-eyebrow">Belleza a tu ritmo y sin barreras</p><h1>Nicolett Studio Fantasy, <em>tu esencia.</em></h1><p>Más que un salón, un espacio para reconectar contigo. Realzamos tu belleza con dedicación, estilo y un cuidado que se siente.</p><div className="site-hero-actions"><a className="site-button" href="/reservar">Reserva tu momento</a><a className="site-button site-button-outline" href="/servicios">Explorar servicios</a></div></div>
    </section>
    <section className="site-benefits"><div><strong>Belleza a tu medida</strong><span>Cada detalle pensado para ti</span></div><div><strong>Manos expertas, cuidado real</strong><span>Especialistas que escuchan y acompañan</span></div><div><strong>Tu hora, asegurada</strong><span>Reserva en unos pocos pasos</span></div></section>
    <section className="site-section"><div className="site-section-heading"><div><p className="site-eyebrow">Nuestros servicios</p><h2>Elige cómo quieres brillar</h2></div><a href="/servicios">Ver todos →</a></div><CatalogState loading={loading} error={error} empty={!items.length}><ServiceCards services={items.slice(0, 3)} /></CatalogState></section>
  </main><SiteFooter /></>;
}

export function ServicesPage() {
  const { items, loading, error } = useCatalog('services');
  useEffect(() => { document.title = 'Servicios | Nicolett Studio Fantasy'; }, []);

  return <><SiteHeader active="servicios" /><main className="site-section site-page-section"><p className="site-eyebrow">Servicios</p><h1>Un cuidado para cada versión de ti</h1><CatalogState loading={loading} error={error} empty={!items.length}><ServiceCards services={items} /></CatalogState></main><SiteFooter /></>;
}

export function TeamPage() {
  const { items, loading, error } = useCatalog('workers');
  useEffect(() => { document.title = 'Nuestro equipo | Nicolett Studio Fantasy'; }, []);

  return <><SiteHeader active="equipo" /><main className="site-section site-page-section"><p className="site-eyebrow">Nuestro equipo</p><h1>Manos expertas, cuidado real</h1><CatalogState loading={loading} error={error} empty={!items.length}><div className="site-team-grid">{items.map((worker) => {
    const first = worker.usuario?.nombre || 'Especialista';
    const last = worker.usuario?.apellidoP || '';
    const initials = `${first[0] || ''}${last[0] || ''}`;
    return <article className="site-team-card" key={worker.id}><span className="site-avatar" aria-hidden="true">{initials}</span><div><h2>{first} {last}</h2><p>{worker.cargoEspecialidad || 'Especialista'}</p></div></article>;
  })}</div></CatalogState></main><SiteFooter /></>;
}

function defaultRouteForRole(role) {
  const normalizedRole = role?.toUpperCase() || '';
  if (normalizedRole.includes('CLIENTE')) return '/mi-perfil';
  if (normalizedRole.includes('ADMIN')) return '/admin/citas';
  if (['PROFESIONAL', 'TRABAJADOR', 'ESPECIALISTA'].some((candidate) => normalizedRole.includes(candidate))) {
    return '/admin/panel-trabajador';
  }
  return '/';
}

function getSafeRedirect(fallback = null) {
  const requested = new URLSearchParams(window.location.search).get('redirect');
  if (!requested || /^(?:[a-z0-9+.-]+:|\/\/)/i.test(requested.trim())) return fallback;
  const target = new URL(requested, window.location.origin);
  const pathname = target.pathname;
  const suffix = `${target.search}${target.hash}`;
  const publicPages = new Set(['index.html', 'reservar.html', 'servicios.html', 'equipo.html', 'agenda.html', 'dashboard.html', 'clientas.html', 'mi-perfil.html']);
  const adminPages = new Set(['citas.html', 'disponibilidad.html', 'historial.html', 'login.html', 'mi-horario.html', 'panel-trabajador.html', 'pruebas.html', 'reserva-cliente.html', 'servicios.html', 'usuarios.html']);
  const clean = pathname.replace(/^\/+/, '').replace(/\/$/, '');
  if (clean.startsWith('admin/')) {
    return adminPages.has(clean.slice('admin/'.length)) ? `/${clean}${suffix}` : fallback;
  }
  if (publicPages.has(clean)) return `/${clean}${suffix}`;
  return adminPages.has(clean) ? `/admin/${clean}${suffix}` : fallback;
}

export function LoginPage() {
  const { user, login, register, logout } = useAuth();
  const [tab, setTab] = useState('login');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const requestedRedirect = getSafeRedirect();
  const sessionDestination = requestedRedirect || defaultRouteForRole(user?.rol);
  useEffect(() => { document.title = 'Bienvenido | Nicolett Studio Fantasy'; }, []);

  async function submit(event) {
    event.preventDefault();
    setError('');
    setBusy(true);
    const data = new FormData(event.currentTarget);
    try {
      if (tab === 'login') {
        const profile = await login(data.get('correo'), data.get('password'));
        window.location.assign(requestedRedirect || defaultRouteForRole(profile.rol));
      } else {
        await register(data.get('nombre'), data.get('apellidoP'), data.get('correo'), data.get('password'), data.get('telefono'));
        window.location.assign('/reservar');
      }
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setBusy(false);
    }
  }

  return <><SiteHeader /><main className="site-auth-page"><section className="site-auth-card"><h1>Bienvenido</h1><p className="site-muted">¡Qué gusto tenerte aquí! Inicia sesión o crea una cuenta para continuar.</p>
    {user && <div className="site-session-banner">Sesión activa<strong>{user.nombre}</strong><span>{user.correo}</span><span>Tipo de usuario: {user.rol}</span><a href={sessionDestination}>Ir a mi espacio</a><button className="site-link-button" onClick={logout}>Cerrar sesión</button></div>}
    <div className="site-tabs" role="tablist"><button type="button" className={tab === 'login' ? 'active' : ''} onClick={() => { setTab('login'); setError(''); }}>Iniciar sesión</button><button type="button" className={tab === 'register' ? 'active' : ''} onClick={() => { setTab('register'); setError(''); }}>Crear cuenta</button></div>
    <form id="react-login-form" onSubmit={submit}>
      {tab === 'register' && <><label>Nombre<input name="nombre" autoComplete="given-name" required /></label><label>Apellido paterno<input name="apellidoP" autoComplete="family-name" required /></label></>}
      <label>Correo electrónico<input name="correo" type="email" autoComplete="email" required /></label>
      <label>Contraseña<input name="password" type="password" autoComplete={tab === 'login' ? 'current-password' : 'new-password'} minLength={tab === 'register' ? 6 : undefined} maxLength={72} required /></label>
      {tab === 'register' && <label>Teléfono (opcional)<input name="telefono" type="tel" autoComplete="tel" /></label>}
      {error && <p className="site-form-error" role="alert">{error}</p>}
      <button className="site-button site-submit" type="submit" disabled={busy}>{busy ? 'Procesando…' : tab === 'login' ? 'Ingresar' : 'Crear cuenta y reservar'}</button>
    </form>
  </section></main><SiteFooter /></>;
}
