import { useEffect, useState } from 'react';

export const PUBLIC_PAGES = new Set(['index.html', 'reservar.html', 'servicios.html', 'equipo.html', 'agenda.html', 'dashboard.html', 'clientas.html']);
export const ADMIN_PAGES = new Set(['citas.html', 'disponibilidad.html', 'historial.html', 'login.html', 'panel-trabajador.html', 'pruebas.html', 'reserva-cliente.html', 'servicios.html', 'usuarios.html']);

export function getLegacyPath(pathname) {
  const clean = pathname.replace(/^\/+/, '');
  if (!clean) return 'index.html';
  if (clean === 'app' || clean === 'app/') return 'admin/citas.html';
  if (clean === 'admin' || clean === 'admin/') return 'admin/citas.html';

  // Alias y accesos directos críticos (DEF-01, DEF-03)
  if (clean === 'login' || clean === 'login.html') return 'admin/login.html';
  if (clean === 'reserva-cliente' || clean === 'reserva-cliente.html') return 'admin/reserva-cliente.html';

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

export function assetUrl(value, sourceUrl) {
  if (!value) return value;
  if (typeof value !== 'string') return value;
  const trimmed = value.trim();
  if (/^javascript:/i.test(trimmed)) return '#';
  if (/^(?:[a-z0-9+.-]+:|\/\/|#|data:)/i.test(trimmed)) return trimmed;
  return new URL(trimmed, sourceUrl).href;
}

/**
 * Temporarily hosts the existing screens inside the React application while
 * preserving their DOM behavior. The original markup, CSS and scripts live
 * under frontend/public/legacy and can be converted to React screen by screen.
 */
function LegacyPage() {
  const [error, setError] = useState('');

  useEffect(() => {
    let cancelled = false;
    const scripts = [];
    const styles = [];
    const root = document.getElementById('legacy-root');
    window.VITE_API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';
    const legacyPath = getLegacyPath(window.location.pathname);
    const sourceUrl = new URL(`/legacy/${legacyPath}`, window.location.origin).href;
    const previousTitle = document.title;
    const initialBodyAttrs = Array.from(document.body.attributes).map((a) => ({ name: a.name, value: a.value }));
    document.body.dataset.page = '';

    async function mount() {
      try {
        const response = await fetch(sourceUrl);
        if (!response.ok) throw new Error(`No se pudo cargar ${legacyPath} (HTTP ${response.status})`);
        const markup = await response.text();
        if (cancelled) return;
        const parsed = new DOMParser().parseFromString(markup, 'text/html');
        document.title = parsed.title || 'Nicolet Estudio';
        for (const key of parsed.body.attributes) document.body.setAttribute(key.name, key.value);

        parsed.head.querySelectorAll('link[rel="stylesheet"]').forEach((original) => {
          const link = document.createElement('link');
          link.rel = 'stylesheet';
          link.href = assetUrl(original.getAttribute('href'), sourceUrl);
          document.head.appendChild(link);
          styles.push(link);
        });

        const scriptNodes = [...parsed.body.querySelectorAll('script')];
        scriptNodes.forEach((node) => node.remove());
        parsed.body.querySelectorAll('img[src], source[src], video[poster]').forEach((element) => {
          for (const attribute of ['src', 'poster']) {
            const value = element.getAttribute(attribute);
            if (value) element.setAttribute(attribute, assetUrl(value, sourceUrl));
          }
        });
        root.innerHTML = parsed.body.innerHTML;

        for (const original of scriptNodes) {
          if (cancelled) return;
          await new Promise((resolve, reject) => {
            const script = document.createElement('script');
            if (original.src) {
              script.src = assetUrl(original.getAttribute('src'), sourceUrl);
              script.async = false;
              script.onload = resolve;
              script.onerror = () => reject(new Error(`No se pudo cargar ${original.getAttribute('src')}`));
            } else {
              script.textContent = original.textContent;
              resolve();
            }
            document.body.appendChild(script);
            scripts.push(script);
          });
        }
        // The original scripts register DOMContentLoaded listeners. React mounts
        // after that browser event, so replay it after restoring the legacy DOM.
        document.dispatchEvent(new Event('DOMContentLoaded', { bubbles: true }));
      } catch (loadError) {
        if (!cancelled) setError(loadError.message);
      }
    }

    mount();
    return () => {
      cancelled = true;
      scripts.forEach((script) => script.remove());
      styles.forEach((style) => style.remove());
      root.replaceChildren();

      // Restaurar atributos originales del body para evitar fugas entre navegaciones (DEF-05)
      while (document.body.attributes.length > 0) {
        document.body.removeAttribute(document.body.attributes[0].name);
      }
      for (const attr of initialBodyAttrs) {
        document.body.setAttribute(attr.name, attr.value);
      }
      document.title = previousTitle;
    };
  }, []);

  if (error) return <main style={{ margin: '4rem auto', maxWidth: 700, fontFamily: 'sans-serif' }}><h1>No se pudo abrir esta página</h1><p>{error}</p></main>;
  return <div id="legacy-root" />;
}

export default function App() {
  return <LegacyPage />;
}
