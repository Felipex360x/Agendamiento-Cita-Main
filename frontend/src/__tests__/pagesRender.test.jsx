import { describe, it, expect, vi, beforeEach } from 'vitest';
import React from 'react';
import { fireEvent, render, screen, within } from '@testing-library/react';
import { AuthProvider } from '../auth/AuthContext.jsx';
import App from '../App.jsx';
import WorkerPortalPage from '../pages/WorkerPortalPage.jsx';
import { StaffAgendaPage, StaffClientsPage, StaffDashboardPage } from '../pages/StaffPages.jsx';
import AdminAppointmentsPage from '../pages/AdminAppointmentsPage.jsx';
import AdminServicesPage from '../pages/AdminServicesPage.jsx';
import AdminUsersPage from '../pages/AdminUsersPage.jsx';
import AdminAvailabilityPage from '../pages/AdminAvailabilityPage.jsx';
import AdminHistoryPage from '../pages/AdminHistoryPage.jsx';
import AdminDiagnosticsPage from '../pages/AdminDiagnosticsPage.jsx';
import BookingPage from '../pages/BookingPage.jsx';
import ClientProfilePage from '../pages/ClientProfilePage.jsx';
import EmailTemplatesPage from '../pages/EmailTemplatesPage.jsx';
import { HomePage, LoginPage, ServicesPage, TeamPage } from '../pages/PublicPages.jsx';
import { SiteFooter, SiteHeader } from '../pages/SiteShell.jsx';
import { AdminShell } from '../pages/AdminShell.jsx';

describe('Renderizado de Páginas y Enrutamiento React', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
    delete window.location;
    window.location = {
      pathname: '/',
      search: '',
      origin: 'http://localhost:3000',
      replace: vi.fn(),
      assign: vi.fn(),
    };
  });

  it('debe renderizar HomePage sin errores', () => {
    expect(() => render(<AuthProvider><HomePage /></AuthProvider>)).not.toThrow();
  });

  it('debe abrir y cerrar el menú de navegación móvil', () => {
    render(<AuthProvider><SiteHeader /></AuthProvider>);

    const toggle = screen.getByRole('button', { name: 'Abrir menú' });
    expect(toggle).toHaveAttribute('aria-expanded', 'false');

    fireEvent.click(toggle);
    expect(screen.getByRole('button', { name: 'Cerrar menú' })).toHaveAttribute('aria-expanded', 'true');
    expect(within(screen.getByRole('navigation', { name: 'Navegación principal' })).getByRole('link', { name: 'Iniciar sesión' }))
      .toHaveAttribute('href', '/admin/login');
    fireEvent.click(screen.getByRole('link', { name: 'Servicios' }));
    expect(screen.getByRole('button', { name: 'Abrir menú' })).toHaveAttribute('aria-expanded', 'false');
  });

  it('debe mostrar la información de contacto en el pie de página', () => {
    render(<SiteFooter />);

    expect(screen.getByRole('link', { name: 'Los Ciruelos, Buin' })).toHaveAttribute(
      'href',
      'https://www.google.com/maps/search/?api=1&query=Los+Ciruelos%2C+Buin',
    );
    expect(screen.getByRole('link', { name: '@nicolettstudiofantasy' })).toHaveAttribute(
      'href',
      'https://www.instagram.com/nicolettstudiofantasy/',
    );
    expect(screen.getByRole('link', { name: '+56 9 4032 2551' })).toHaveAttribute(
      'href',
      'https://wa.me/56940322551',
    );
  });

  it('debe abrir y cerrar el menú de administración', () => {
    localStorage.setItem('nicolet_jwt_user', JSON.stringify({ nombre: 'Valentina', rol: 'ADMINISTRADOR' }));
    render(<AuthProvider><AdminShell title="Gestión de citas" active="/admin/citas" /></AuthProvider>);

    const toggle = screen.getByRole('button', { name: 'Abrir menú' });
    expect(toggle).toHaveAttribute('aria-expanded', 'false');

    fireEvent.click(toggle);
    expect(screen.getByRole('button', { name: 'Cerrar menú' })).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByRole('navigation', { name: 'Administración' })).toHaveClass('is-open');
    fireEvent.click(screen.getByRole('link', { name: 'Disponibilidad' }));
    expect(screen.getByRole('button', { name: 'Abrir menú' })).toHaveAttribute('aria-expanded', 'false');
  });

  it('debe renderizar LoginPage sin errores', () => {
    expect(() => render(<AuthProvider><LoginPage /></AuthProvider>)).not.toThrow();
  });

  it('debe renderizar ServicesPage sin errores', () => {
    expect(() => render(<AuthProvider><ServicesPage /></AuthProvider>)).not.toThrow();
  });

  it('debe renderizar TeamPage sin errores', () => {
    expect(() => render(<AuthProvider><TeamPage /></AuthProvider>)).not.toThrow();
  });

  it('debe renderizar BookingPage sin errores', () => {
    expect(() => render(<AuthProvider><BookingPage /></AuthProvider>)).not.toThrow();
  });

  it('debe renderizar AdminAppointmentsPage sin sesión redirigiendo sin colapsar', () => {
    expect(() => render(<AuthProvider><AdminAppointmentsPage /></AuthProvider>)).not.toThrow();
  });

  it('debe renderizar StaffAgendaPage sin sesión sin lanzar TypeError de null user', () => {
    expect(() => render(<AuthProvider><StaffAgendaPage /></AuthProvider>)).not.toThrow();
  });

  it('debe renderizar WorkerPortalPage sin sesión sin lanzar TypeError de null user', () => {
    expect(() => render(<AuthProvider><WorkerPortalPage /></AuthProvider>)).not.toThrow();
  });

  it('debe renderizar StaffDashboardPage y StaffClientsPage sin colapsar', () => {
    expect(() => render(<AuthProvider><StaffDashboardPage /></AuthProvider>)).not.toThrow();
    expect(() => render(<AuthProvider><StaffClientsPage /></AuthProvider>)).not.toThrow();
  });

  it('debe renderizar paneles de administración sin colapsar', () => {
    expect(() => render(<AuthProvider><AdminServicesPage /></AuthProvider>)).not.toThrow();
    expect(() => render(<AuthProvider><AdminUsersPage /></AuthProvider>)).not.toThrow();
    expect(() => render(<AuthProvider><AdminAvailabilityPage /></AuthProvider>)).not.toThrow();
    expect(() => render(<AuthProvider><AdminHistoryPage /></AuthProvider>)).not.toThrow();
    expect(() => render(<AuthProvider><AdminDiagnosticsPage /></AuthProvider>)).not.toThrow();
  });

  it('debe renderizar ClientProfilePage sin sesión sin colapsar', () => {
    expect(() => render(<AuthProvider><ClientProfilePage /></AuthProvider>)).not.toThrow();
  });

  it('debe despachar las distintas rutas en App sin errores', () => {
    window.location.pathname = '/';
    expect(() => render(<AuthProvider><App /></AuthProvider>)).not.toThrow();

    window.location.pathname = '/servicios';
    expect(() => render(<AuthProvider><App /></AuthProvider>)).not.toThrow();

    window.location.pathname = '/equipo';
    expect(() => render(<AuthProvider><App /></AuthProvider>)).not.toThrow();

    window.location.pathname = '/reservar';
    expect(() => render(<AuthProvider><App /></AuthProvider>)).not.toThrow();

    window.location.pathname = '/mi-perfil';
    expect(() => render(<AuthProvider><App /></AuthProvider>)).not.toThrow();

    window.location.pathname = '/agenda';
    expect(() => render(<AuthProvider><App /></AuthProvider>)).not.toThrow();

    window.location.pathname = '/dashboard';
    expect(() => render(<AuthProvider><App /></AuthProvider>)).not.toThrow();

    window.location.pathname = '/clientas';
    expect(() => render(<AuthProvider><App /></AuthProvider>)).not.toThrow();

    window.location.pathname = '/admin/citas';
    expect(() => render(<AuthProvider><App /></AuthProvider>)).not.toThrow();

    window.location.pathname = '/admin/panel-trabajador';
    expect(() => render(<AuthProvider><App /></AuthProvider>)).not.toThrow();

    window.location.pathname = '/correos';
    expect(() => render(<AuthProvider><App /></AuthProvider>)).not.toThrow();
  });

  it('debe renderizar EmailTemplatesPage y permitir alternar entre las pantallas de correo (Bienvenida, Pago, Trabajador y Confirmada)', () => {
    render(<AuthProvider><EmailTemplatesPage /></AuthProvider>);

    // Por defecto muestra la pantalla 2 (Pago y Reserva)
    expect(screen.getByText(/Comprobante de pago y reserva recibida/i)).toBeInTheDocument();
    expect(screen.getByText(/Comprobante de transacción \(Webpay\)/i)).toBeInTheDocument();

    // Pantalla 1: Bienvenida
    fireEvent.click(screen.getByRole('tab', { name: /1\. Bienvenida/i }));
    expect(screen.getByText(/Cuenta creada con éxito/i)).toBeInTheDocument();

    // Pantalla 3: Aviso al Trabajador
    fireEvent.click(screen.getByRole('tab', { name: /3\. Aviso al Trabajador/i }));
    expect(screen.getByText(/agendaron una hora contigo/i)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Ir a Mi Panel y Confirmar Cita/i })).toHaveAttribute('href', '/admin/panel-trabajador');

    // Pantalla 4: Cita Confirmada
    fireEvent.click(screen.getByRole('tab', { name: /4\. Cita Confirmada/i }));
    expect(screen.getByText(/¡Tu cita está confirmada/i)).toBeInTheDocument();

    // Pantalla 5: Cita Cancelada
    fireEvent.click(screen.getByRole('tab', { name: /5\. Cita Cancelada/i }));
    expect(screen.getByText(/Tu cita ha sido cancelada/i)).toBeInTheDocument();

    // Pantalla 6: Recordatorio 24h
    fireEvent.click(screen.getByRole('tab', { name: /6\. Recordatorio 24h/i }));
    expect(screen.getByText(/Recordatorio 24 horas antes/i)).toBeInTheDocument();
  });
});
