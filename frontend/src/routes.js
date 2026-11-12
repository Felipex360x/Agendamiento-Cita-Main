// Lista de páginas del sitio: la usan la aplicación principal y el inicio de sesión.
export const PUBLIC_PAGES = new Set(['index.html', 'reservar.html', 'servicios.html', 'equipo.html', 'agenda.html', 'dashboard.html', 'clientas.html', 'mi-perfil.html']);
export const ADMIN_PAGES = new Set(['citas.html', 'disponibilidad.html', 'historial.html', 'login.html', 'mi-horario.html', 'panel-trabajador.html', 'pruebas.html', 'reserva-cliente.html', 'servicios.html', 'usuarios.html', 'correos.html']);

export function resolvePage(pathname) {
  const clean = pathname.replace(/^\/+/, '');
  if (!clean) return 'index.html';
  if (clean === 'app' || clean === 'app/') return 'admin/citas.html';
  if (clean === 'admin' || clean === 'admin/') return 'admin/citas.html';

  // Nombres cortos y accesos rápidos
  if (clean === 'login' || clean === 'login.html') return 'admin/login.html';
  if (clean === 'reserva-cliente' || clean === 'reserva-cliente.html') return 'admin/reserva-cliente.html';
  if (clean === 'correos' || clean === 'correos.html') return 'admin/correos.html';

  if (clean.startsWith('admin/')) {
    let page = clean.slice('admin/'.length).split('/')[0];
    if (page && !page.includes('.')) page = `${page}.html`;
    return ADMIN_PAGES.has(page) ? `admin/${page}` : 'admin/citas.html';
  }

  let page = clean.split('/')[0];
  if (page && !page.includes('.')) {
    if (PUBLIC_PAGES.has(`${page}.html`)) return `${page}.html`;
    if (ADMIN_PAGES.has(`${page}.html`)) return `admin/${page}.html`;
  }
  return PUBLIC_PAGES.has(page) ? page : 'index.html';
}

/**
 * Busca una ruta solo si pertenece a una página existente (a diferencia de resolvePage, que abre
 * una página por defecto). Devuelve el nombre de la página ('index.html', 'admin/usuarios.html', …) o null.
 */
export function resolveKnownPage(pathname) {
  const clean = String(pathname || '').replace(/^\/+/, '').replace(/\/+$/, '');
  if (!clean) return 'index.html';
  if (clean === 'app' || clean === 'admin') return 'admin/citas.html';
  if (clean === 'login' || clean === 'login.html') return 'admin/login.html';
  if (clean === 'reserva-cliente' || clean === 'reserva-cliente.html') return 'admin/reserva-cliente.html';
  if (clean === 'correos' || clean === 'correos.html') return 'admin/correos.html';
  const withHtml = (name) => (name.includes('.') ? name : `${name}.html`);
  if (clean.startsWith('admin/')) {
    const page = withHtml(clean.slice('admin/'.length));
    return !page.includes('/') && ADMIN_PAGES.has(page) ? `admin/${page}` : null;
  }
  if (clean.includes('/')) return null;
  const page = withHtml(clean);
  if (PUBLIC_PAGES.has(page)) return page;
  return ADMIN_PAGES.has(page) ? `admin/${page}` : null;
}

/**
 * Revisa que la ruta de `?redirect=` sea segura y dentro del mismo sitio. Acepta rutas cortas
 * (/admin/usuarios) y con .html, mantiene los parámetros, y rechaza enlaces a otros sitios,
 * páginas que no existen y el propio inicio de sesión (para no quedar en bucle).
 */
export function getSafeRedirect(requested, origin, fallback = null) {
  if (typeof requested !== 'string') return fallback;
  const value = requested.trim();
  // eslint-disable-next-line no-control-regex
  if (!value.startsWith('/') || value.startsWith('//') || /[\\\u0000-\u001f]/.test(value)) return fallback;
  let target;
  try { target = new URL(value, origin); } catch { return fallback; }
  if (target.origin !== origin) return fallback;
  const page = resolveKnownPage(target.pathname);
  if (!page || page === 'admin/login.html') return fallback;
  return `${target.pathname}${target.search}${target.hash}`;
}
