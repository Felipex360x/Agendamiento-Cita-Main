import { describe, it, expect } from 'vitest';
import { getLegacyPath, PUBLIC_PAGES, ADMIN_PAGES } from '../App.jsx';

describe('Router Legacy: getLegacyPath(pathname)', () => {
  describe('Rutas Públicas Raíz y Fallback', () => {
    it('debe resolver la raíz "/" hacia "index.html"', () => {
      expect(getLegacyPath('/')).toBe('index.html');
    });

    it('debe resolver string vacío "" hacia "index.html"', () => {
      expect(getLegacyPath('')).toBe('index.html');
    });

    it('debe resolver múltiples barras iniciales "///" hacia "index.html"', () => {
      expect(getLegacyPath('///')).toBe('index.html');
    });

    it('debe resolver páginas públicas declaradas con extensión .html', () => {
      expect(getLegacyPath('/index.html')).toBe('index.html');
      expect(getLegacyPath('/reservar.html')).toBe('reservar.html');
      expect(getLegacyPath('/servicios.html')).toBe('servicios.html');
      expect(getLegacyPath('/equipo.html')).toBe('equipo.html');
      expect(getLegacyPath('/agenda.html')).toBe('agenda.html');
      expect(getLegacyPath('/dashboard.html')).toBe('dashboard.html');
      expect(getLegacyPath('/clientas.html')).toBe('clientas.html');
    });

    it('debe resolver rutas públicas sin barra inicial', () => {
      expect(getLegacyPath('reservar.html')).toBe('reservar.html');
      expect(getLegacyPath('servicios.html')).toBe('servicios.html');
    });

    it('debe hacer fallback a "index.html" para rutas públicas inexistentes', () => {
      expect(getLegacyPath('/pagina-que-no-existe')).toBe('index.html');
      expect(getLegacyPath('/contacto.html')).toBe('index.html');
      expect(getLegacyPath('/promociones')).toBe('index.html');
    });
  });

  describe('Alias y Prefijos Administrativos', () => {
    it('debe resolver el alias "/app" y "/app/" hacia "admin/citas.html"', () => {
      expect(getLegacyPath('/app')).toBe('admin/citas.html');
      expect(getLegacyPath('/app/')).toBe('admin/citas.html');
      expect(getLegacyPath('app')).toBe('admin/citas.html');
      expect(getLegacyPath('app/')).toBe('admin/citas.html');
    });

    it('debe resolver el alias "/admin" y "/admin/" hacia "admin/citas.html"', () => {
      expect(getLegacyPath('/admin')).toBe('admin/citas.html');
      expect(getLegacyPath('admin')).toBe('admin/citas.html');
      expect(getLegacyPath('/admin/')).toBe('admin/citas.html');
    });

    it('debe resolver páginas administrativas válidas bajo "/admin/*"', () => {
      expect(getLegacyPath('/admin/citas.html')).toBe('admin/citas.html');
      expect(getLegacyPath('/admin/disponibilidad.html')).toBe('admin/disponibilidad.html');
      expect(getLegacyPath('/admin/historial.html')).toBe('admin/historial.html');
      expect(getLegacyPath('/admin/login.html')).toBe('admin/login.html');
      expect(getLegacyPath('/admin/panel-trabajador.html')).toBe('admin/panel-trabajador.html');
      expect(getLegacyPath('/admin/pruebas.html')).toBe('admin/pruebas.html');
      expect(getLegacyPath('/admin/reserva-cliente.html')).toBe('admin/reserva-cliente.html');
      expect(getLegacyPath('/admin/servicios.html')).toBe('admin/servicios.html');
      expect(getLegacyPath('/admin/usuarios.html')).toBe('admin/usuarios.html');
    });

    it('debe hacer fallback a "admin/citas.html" para rutas desconocidas bajo "/admin/*"', () => {
      expect(getLegacyPath('/admin/no-existe.html')).toBe('admin/citas.html');
      expect(getLegacyPath('/admin/configuracion')).toBe('admin/citas.html');
      expect(getLegacyPath('/admin/inventario.html')).toBe('admin/citas.html');
    });
  });

  describe('Pruebas de Regresión y Corrección de Rutas (Bugs Corregidos)', () => {
    it('DEF-01 Corregido: /login.html y /login directos sin prefijo /admin resuelven a admin/login.html', () => {
      expect(getLegacyPath('/login.html')).toBe('admin/login.html');
      expect(getLegacyPath('/login')).toBe('admin/login.html');
      expect(getLegacyPath('login.html')).toBe('admin/login.html');
      expect(getLegacyPath('login')).toBe('admin/login.html');
    });

    it('DEF-03 Corregido: /reserva-cliente.html y /reserva-cliente resuelven a admin/reserva-cliente.html', () => {
      expect(getLegacyPath('/reserva-cliente.html')).toBe('admin/reserva-cliente.html');
      expect(getLegacyPath('/reserva-cliente')).toBe('admin/reserva-cliente.html');
      expect(getLegacyPath('reserva-cliente.html')).toBe('admin/reserva-cliente.html');
    });

    it('DEF-07 Corregido: Rutas amigables sin .html resuelven a su respectivo archivo .html', () => {
      expect(getLegacyPath('/reservar')).toBe('reservar.html');
      expect(getLegacyPath('/servicios')).toBe('servicios.html');
      expect(getLegacyPath('/equipo')).toBe('equipo.html');
      expect(getLegacyPath('/agenda')).toBe('agenda.html');
      expect(getLegacyPath('/dashboard')).toBe('dashboard.html');
      expect(getLegacyPath('/clientas')).toBe('clientas.html');
    });

    it('DEF-07 Corregido: Rutas administrativas amigables sin .html resuelven a admin/*.html', () => {
      expect(getLegacyPath('/admin/login')).toBe('admin/login.html');
      expect(getLegacyPath('/admin/citas')).toBe('admin/citas.html');
      expect(getLegacyPath('/admin/servicios')).toBe('admin/servicios.html');
      expect(getLegacyPath('/admin/disponibilidad')).toBe('admin/disponibilidad.html');
      expect(getLegacyPath('/admin/historial')).toBe('admin/historial.html');
      expect(getLegacyPath('/admin/panel-trabajador')).toBe('admin/panel-trabajador.html');
      expect(getLegacyPath('/admin/usuarios')).toBe('admin/usuarios.html');
    });
  });
});
