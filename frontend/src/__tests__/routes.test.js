import { describe, it, expect } from 'vitest';
import { resolvePage, PUBLIC_PAGES, ADMIN_PAGES } from '../routes.js';

describe('Enrutador: resolvePage(pathname)', () => {
  describe('Rutas Públicas Raíz y Fallback', () => {
    it('debe resolver la raíz "/" hacia "index.html"', () => {
      expect(resolvePage('/')).toBe('index.html');
    });

    it('debe resolver string vacío "" hacia "index.html"', () => {
      expect(resolvePage('')).toBe('index.html');
    });

    it('debe resolver múltiples barras iniciales "///" hacia "index.html"', () => {
      expect(resolvePage('///')).toBe('index.html');
    });

    it('debe resolver páginas públicas declaradas con extensión .html', () => {
      expect(resolvePage('/index.html')).toBe('index.html');
      expect(resolvePage('/reservar.html')).toBe('reservar.html');
      expect(resolvePage('/servicios.html')).toBe('servicios.html');
      expect(resolvePage('/equipo.html')).toBe('equipo.html');
      expect(resolvePage('/agenda.html')).toBe('agenda.html');
      expect(resolvePage('/dashboard.html')).toBe('dashboard.html');
      expect(resolvePage('/clientas.html')).toBe('clientas.html');
    });

    it('debe resolver rutas públicas sin barra inicial', () => {
      expect(resolvePage('reservar.html')).toBe('reservar.html');
      expect(resolvePage('servicios.html')).toBe('servicios.html');
    });

    it('debe hacer fallback a "index.html" para rutas públicas inexistentes', () => {
      expect(resolvePage('/pagina-que-no-existe')).toBe('index.html');
      expect(resolvePage('/contacto.html')).toBe('index.html');
      expect(resolvePage('/promociones')).toBe('index.html');
    });
  });

  describe('Alias y Prefijos Administrativos', () => {
    it('debe resolver el alias "/app" y "/app/" hacia "admin/citas.html"', () => {
      expect(resolvePage('/app')).toBe('admin/citas.html');
      expect(resolvePage('/app/')).toBe('admin/citas.html');
      expect(resolvePage('app')).toBe('admin/citas.html');
      expect(resolvePage('app/')).toBe('admin/citas.html');
    });

    it('debe resolver el alias "/admin" y "/admin/" hacia "admin/citas.html"', () => {
      expect(resolvePage('/admin')).toBe('admin/citas.html');
      expect(resolvePage('admin')).toBe('admin/citas.html');
      expect(resolvePage('/admin/')).toBe('admin/citas.html');
    });

    it('debe resolver páginas administrativas válidas bajo "/admin/*"', () => {
      expect(resolvePage('/admin/citas.html')).toBe('admin/citas.html');
      expect(resolvePage('/admin/disponibilidad.html')).toBe('admin/disponibilidad.html');
      expect(resolvePage('/admin/historial.html')).toBe('admin/historial.html');
      expect(resolvePage('/admin/login.html')).toBe('admin/login.html');
      expect(resolvePage('/admin/panel-trabajador.html')).toBe('admin/panel-trabajador.html');
      expect(resolvePage('/admin/pruebas.html')).toBe('admin/pruebas.html');
      expect(resolvePage('/admin/reserva-cliente.html')).toBe('admin/reserva-cliente.html');
      expect(resolvePage('/admin/servicios.html')).toBe('admin/servicios.html');
      expect(resolvePage('/admin/usuarios.html')).toBe('admin/usuarios.html');
    });

    it('debe hacer fallback a "admin/citas.html" para rutas desconocidas bajo "/admin/*"', () => {
      expect(resolvePage('/admin/no-existe.html')).toBe('admin/citas.html');
      expect(resolvePage('/admin/configuracion')).toBe('admin/citas.html');
      expect(resolvePage('/admin/inventario.html')).toBe('admin/citas.html');
    });
  });

  describe('Pruebas de Regresión y Corrección de Rutas (Bugs Corregidos)', () => {
    it('DEF-01 Corregido: /login.html y /login directos sin prefijo /admin resuelven a admin/login.html', () => {
      expect(resolvePage('/login.html')).toBe('admin/login.html');
      expect(resolvePage('/login')).toBe('admin/login.html');
      expect(resolvePage('login.html')).toBe('admin/login.html');
      expect(resolvePage('login')).toBe('admin/login.html');
    });

    it('DEF-03 Corregido: /reserva-cliente.html y /reserva-cliente resuelven a admin/reserva-cliente.html', () => {
      expect(resolvePage('/reserva-cliente.html')).toBe('admin/reserva-cliente.html');
      expect(resolvePage('/reserva-cliente')).toBe('admin/reserva-cliente.html');
      expect(resolvePage('reserva-cliente.html')).toBe('admin/reserva-cliente.html');
    });

    it('DEF-07 Corregido: Rutas amigables sin .html resuelven a su respectivo archivo .html', () => {
      expect(resolvePage('/reservar')).toBe('reservar.html');
      expect(resolvePage('/servicios')).toBe('servicios.html');
      expect(resolvePage('/equipo')).toBe('equipo.html');
      expect(resolvePage('/agenda')).toBe('agenda.html');
      expect(resolvePage('/dashboard')).toBe('dashboard.html');
      expect(resolvePage('/clientas')).toBe('clientas.html');
    });

    it('DEF-07 Corregido: Rutas administrativas amigables sin .html resuelven a admin/*.html', () => {
      expect(resolvePage('/admin/login')).toBe('admin/login.html');
      expect(resolvePage('/admin/citas')).toBe('admin/citas.html');
      expect(resolvePage('/admin/servicios')).toBe('admin/servicios.html');
      expect(resolvePage('/admin/disponibilidad')).toBe('admin/disponibilidad.html');
      expect(resolvePage('/admin/historial')).toBe('admin/historial.html');
      expect(resolvePage('/admin/panel-trabajador')).toBe('admin/panel-trabajador.html');
      expect(resolvePage('/admin/usuarios')).toBe('admin/usuarios.html');
    });
  });
});
