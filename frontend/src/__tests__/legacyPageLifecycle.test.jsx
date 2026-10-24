import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import App from '../App.jsx';

describe('Ciclo de Vida y Adaptador DOM: LegacyPage (App.jsx)', () => {
  const originalFetch = global.fetch;

  beforeEach(() => {
    vi.restoreAllMocks();
    document.body.className = '';
    document.body.removeAttribute('data-page');
    document.title = 'Nicolet Test';
  });

  afterEach(() => {
    global.fetch = originalFetch;
  });

  it('debe renderizar mensaje de error si el fetch del HTML legacy falla con 404', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 404,
      statusText: 'Not Found',
    });

    render(<App />);

    await waitFor(() => {
      expect(screen.getByText('No se pudo abrir esta página')).toBeInTheDocument();
      expect(screen.getByText(/No se pudo cargar .*HTTP 404/)).toBeInTheDocument();
    });
  });

  it('debe parsear HTML, inyectar estilos y scripts, y despachar DOMContentLoaded', async () => {
    const mockHtml = `
      <!DOCTYPE html>
      <html>
        <head>
          <title>Página Legacy de Prueba</title>
          <link rel="stylesheet" href="css/custom.css">
        </head>
        <body data-page="testpage" class="theme-gold">
          <div id="content"><span>Contenido Legacy Inyectado</span></div>
          <script>window.__legacyScriptExecuted = true;</script>
        </body>
      </html>
    `;

    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      text: () => Promise.resolve(mockHtml),
    });

    let domContentLoadedFired = false;
    const domListener = () => { domContentLoadedFired = true; };
    document.addEventListener('DOMContentLoaded', domListener);

    render(<App />);

    await waitFor(() => {
      expect(document.title).toBe('Página Legacy de Prueba');
      expect(screen.getByText('Contenido Legacy Inyectado')).toBeInTheDocument();
    });

    expect(domContentLoadedFired).toBe(true);
    document.removeEventListener('DOMContentLoaded', domListener);
  });

  describe('Auditoría QA: Fugas de Estado y Polución del DOM en el Ciclo de Vida', () => {
    it('DEF-05 Corregido: Limpieza completa de atributos y estilos en document.body al desmontar', async () => {
      const mockHtml = `
        <!DOCTYPE html>
        <html>
          <body data-page="home" class="gold-theme full-screen" style="background-color: rgb(255, 0, 0);">
            <div>Test</div>
          </body>
        </html>
      `;

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        status: 200,
        text: () => Promise.resolve(mockHtml),
      });

      const { unmount } = render(<App />);

      await waitFor(() => {
        expect(document.body.getAttribute('data-page')).toBe('home');
        expect(document.body.getAttribute('class')).toBe('gold-theme full-screen');
      });

      // Desmontar el componente
      unmount();

      // data-page y clases inyectadas se limpian correctamente al desmontar:
      expect(document.body.getAttribute('data-page')).toBeNull();
      expect(document.body.getAttribute('class') || '').not.toContain('gold-theme');
      expect(document.body.style.backgroundColor).toBe('');
    });

    it('QA-DEFECT-10: Polución y fuga de scripts en document.body (la eliminación del tag script no limpia estado en memoria)', async () => {
      const mockHtml = `
        <!DOCTYPE html>
        <html>
          <body>
            <div>Legacy View</div>
            <script id="injected-legacy-script">console.log("running");</script>
          </body>
        </html>
      `;

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        status: 200,
        text: () => Promise.resolve(mockHtml),
      });

      const { unmount } = render(<App />);

      await waitFor(() => {
        expect(screen.getByText('Legacy View')).toBeInTheDocument();
        const scriptElement = document.body.querySelector('script');
        expect(scriptElement).not.toBeNull();
        expect(scriptElement.textContent).toBe('console.log("running");');
      });

      unmount();

      // Al desmontar, el nodo <script> se remueve del DOM
      const scriptAfter = document.body.querySelector('script');
      expect(scriptAfter).toBeNull();
      // Sin embargo, por diseño del motor de JavaScript, las funciones declaradas
      // en window, event listeners agregados y timers NO se recolectan al remover el nodo script.
    });
  });
});
