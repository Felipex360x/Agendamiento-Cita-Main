import { describe, it, expect } from 'vitest';
import { getSafeRedirect, resolveKnownPage } from '../routes.js';

const ORIGIN = 'http://localhost:5173';
const ok = (value) => getSafeRedirect(value, ORIGIN, '/fallback');

describe('E17: redirección segura del login', () => {
  it('conserva destinos amigables válidos (AdminShell genera /admin/usuarios)', () => {
    expect(ok('/admin/usuarios')).toBe('/admin/usuarios');
    expect(ok('/admin/citas')).toBe('/admin/citas');
    expect(ok('/mi-perfil')).toBe('/mi-perfil');
    expect(ok('/agenda')).toBe('/agenda');
  });

  it('conserva variantes .html, query y hash', () => {
    expect(ok('/admin/usuarios.html')).toBe('/admin/usuarios.html');
    expect(ok('/reservar.html?servicioId=3&trabajadorId=2#paso')).toBe('/reservar.html?servicioId=3&trabajadorId=2#paso');
    expect(ok('/reservar?servicioId=3')).toBe('/reservar?servicioId=3');
  });

  it('páginas administrativas sin prefijo /admin siguen resolviendo', () => {
    expect(ok('/historial')).toBe('/historial');
    expect(resolveKnownPage('/historial')).toBe('admin/historial.html');
  });

  it('rechaza destinos externos y esquemas peligrosos', () => {
    for (const malo of [
      'https://evil.example/admin/usuarios', 'http://localhost:5173.evil.example/x', '//evil.example/admin',
      '/\\evil.example', '\\\\evil.example', 'javascript:alert(1)', 'data:text/html,hola', 'ftp://x/y',
      '/admin/usuarios\n//evil.example', '', '   ', null, undefined, 42,
    ]) {
      expect(ok(malo), String(malo)).toBe('/fallback');
    }
  });

  it('rechaza páginas desconocidas, rutas anidadas y el propio login (evita bucles)', () => {
    for (const malo of ['/no-existe', '/admin/no-existe', '/admin/usuarios/extra', '/mi-perfil/otro', '/admin/login', '/login', '/admin/login.html']) {
      expect(ok(malo), malo).toBe('/fallback');
    }
  });

  it('el fallback es el que se pasa (o null por defecto)', () => {
    expect(getSafeRedirect('https://evil.example', ORIGIN)).toBeNull();
    expect(getSafeRedirect('//evil.example', ORIGIN, '/admin/citas')).toBe('/admin/citas');
  });
});
