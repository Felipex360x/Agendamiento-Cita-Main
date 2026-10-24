import { describe, it, expect } from 'vitest';
import { assetUrl } from '../App.jsx';

describe('Adaptador Legacy: assetUrl(value, sourceUrl)', () => {
  const sourceUrl = 'http://localhost:5173/legacy/admin/citas.html';
  const rootSourceUrl = 'http://localhost:5173/legacy/index.html';

  describe('Passthrough de URLs absolutas, protocolos y esquemas especiales', () => {
    it('debe retornar inalteradas las URLs con protocolo http:// y https://', () => {
      expect(assetUrl('http://cdn.ejemplo.com/estilos.css', sourceUrl)).toBe('http://cdn.ejemplo.com/estilos.css');
      expect(assetUrl('https://fonts.googleapis.com/css2', sourceUrl)).toBe('https://fonts.googleapis.com/css2');
    });

    it('debe retornar inalteradas las URLs relativas al protocolo (//)', () => {
      expect(assetUrl('//cdn.jsdelivr.net/npm/chart.js', sourceUrl)).toBe('//cdn.jsdelivr.net/npm/chart.js');
    });

    it('debe retornar inalterados los Data URIs (data:)', () => {
      const dataUri = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY44YAAAAASUVORK5CYII=';
      expect(assetUrl(dataUri, sourceUrl)).toBe(dataUri);
    });

    it('debe retornar inalterados los anclajes (#)', () => {
      expect(assetUrl('#seccion-1', sourceUrl)).toBe('#seccion-1');
      expect(assetUrl('#modal', sourceUrl)).toBe('#modal');
    });

    it('debe manejar valores falsy o nulos retornando el mismo valor', () => {
      expect(assetUrl('', sourceUrl)).toBe('');
      expect(assetUrl(null, sourceUrl)).toBe(null);
      expect(assetUrl(undefined, sourceUrl)).toBe(undefined);
    });
  });

  describe('Resolución de Rutas Relativas contra sourceUrl', () => {
    it('debe resolver assets relativos locales en el mismo subdirectorio', () => {
      const resolvedCss = assetUrl('css/admin.css', sourceUrl);
      expect(resolvedCss).toBe('http://localhost:5173/legacy/admin/css/admin.css');

      const resolvedJs = assetUrl('js/auth.js', sourceUrl);
      expect(resolvedJs).toBe('http://localhost:5173/legacy/admin/js/auth.js');
    });

    it('debe resolver assets desde la raíz legacy (sourceUrl de index.html)', () => {
      const resolvedCss = assetUrl('css/styles.css', rootSourceUrl);
      expect(resolvedCss).toBe('http://localhost:5173/legacy/css/styles.css');

      const resolvedImg = assetUrl('img/salon-interior.jpg', rootSourceUrl);
      expect(resolvedImg).toBe('http://localhost:5173/legacy/img/salon-interior.jpg');
    });

    it('debe resolver referencias relativas padre "../"', () => {
      const resolvedParent = assetUrl('../css/styles.css', sourceUrl);
      expect(resolvedParent).toBe('http://localhost:5173/legacy/css/styles.css');
    });

    it('debe resolver rutas con barra absoluta "/img/hair.jpg" respecto al origen', () => {
      const resolvedRoot = assetUrl('/img/hair.jpg', sourceUrl);
      expect(resolvedRoot).toBe('http://localhost:5173/img/hair.jpg');
    });
  });

  describe('Auditoría QA: Riesgos de resolución de assets', () => {
    it('QA-RISK-01: Rutas con barra absoluta "/" pierden el prefijo "/legacy/"', () => {
      // Si un archivo HTML legacy tuviera un asset con href="/css/styles.css",
      // new URL('/css/styles.css', 'http://localhost:5173/legacy/admin/citas.html')
      // resuelve a 'http://localhost:5173/css/styles.css' en vez de '/legacy/css/styles.css'.
      const resolved = assetUrl('/css/styles.css', sourceUrl);
      expect(resolved).toBe('http://localhost:5173/css/styles.css');
      expect(resolved).not.toContain('/legacy/');
    });
  });
});
