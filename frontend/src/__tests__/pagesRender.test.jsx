import { describe, it, expect, vi, beforeEach } from 'vitest';
import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
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
import { SiteFooter, SiteHeader } from '../layouts/SiteShell.jsx';
import { AdminShell } from '../layouts/AdminShell.jsx';

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
    const loginLinks = screen.getAllByRole('link', { name: 'Iniciar sesión' });
    expect(loginLinks).toHaveLength(1);
    expect(loginLinks[0]).toHaveAttribute('href', '/admin/login');
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

  it('debe corregir descripciones de servicios con codificación dañada sin alterar acentos válidos', async () => {
    vi.spyOn(global, 'fetch').mockResolvedValue({
      ok: true,
      status: 200,
      text: async () => JSON.stringify([
        { id: 2, nombre: 'Manicura rusa', descripcion: 'Limpieza profunda de cutÃ­culas y esmaltado de alta duraciÃ³n', duracionMinutos: 60, precio: 22000 },
        { id: 3, nombre: 'Tratamiento Facial Hidratante', descripcion: 'Limpieza profunda, exfoliación y mascarilla regeneradora', duracionMinutos: 60, precio: 28000 },
      ]),
    });

    render(<AuthProvider><ServicesPage /></AuthProvider>);

    expect(await screen.findByText('Limpieza profunda de cutículas y esmaltado de alta duración')).toBeInTheDocument();
    expect(screen.getByText('Limpieza profunda, exfoliación y mascarilla regeneradora')).toBeInTheDocument();
    expect(screen.queryByText(/cutÃ|duraciÃ/)).not.toBeInTheDocument();
  });

  it('debe renderizar TeamPage sin errores', () => {
    expect(() => render(<AuthProvider><TeamPage /></AuthProvider>)).not.toThrow();
  });

  it('debe renderizar BookingPage sin errores', () => {
    expect(() => render(<AuthProvider><BookingPage /></AuthProvider>)).not.toThrow();
  });

  it('no debe permitir continuar con un horario que se cruza con la colación', async () => {
    window.location.search = '?servicioId=2&trabajadorId=1&fecha=2027-01-11&hora=12%3A00';
    vi.spyOn(global, 'fetch').mockImplementation(async (url) => {
      const requestUrl = String(url);
      if (requestUrl.includes('/disponibilidad?')) {
        return {
          ok: true,
          status: 200,
          text: async () => JSON.stringify([{
            horaInicio: '12:00:00',
            horaFin: '13:00:00',
            disponible: false,
            motivoNoDisponible: 'DESCANSO',
          }]),
        };
      }
      if (requestUrl.endsWith('/servicios')) {
        return { ok: true, status: 200, text: async () => JSON.stringify([{ id: 2, nombre: 'Manicura rusa', duracionMinutos: 60, precio: 22000 }]) };
      }
      if (requestUrl.endsWith('/trabajadores')) {
        return { ok: true, status: 200, text: async () => JSON.stringify([{ id: 1, usuario: { nombre: 'Camila', apellidoP: 'Silva' }, servicios: [{ id: 2 }] }]) };
      }
      return { ok: true, status: 200, text: async () => '[]' };
    });

    render(<AuthProvider><BookingPage /></AuthProvider>);

    expect(await screen.findByRole('button', { name: '12:00, Conflicto con colación' })).toBeDisabled();
    expect(screen.getByRole('alert')).toHaveTextContent(
      'El horario de las 12:00 entra en conflicto con la colación: el servicio dura 60 minutos e invade el descanso de la profesional.',
    );
    expect(screen.getByRole('button', { name: 'Continuar' })).toBeDisabled();
  });

  it('debe renderizar AdminAppointmentsPage sin sesión redirigiendo sin colapsar', () => {
    expect(() => render(<AuthProvider><AdminAppointmentsPage /></AuthProvider>)).not.toThrow();
  });

  it('debe mostrar los campos de cliente invitado al agendar desde administración', () => {
    localStorage.setItem('nicolet_jwt_user', JSON.stringify({ id: 1, nombre: 'Admin', rol: 'ADMINISTRADOR' }));
    vi.spyOn(global, 'fetch').mockResolvedValue({
      ok: true,
      status: 200,
      text: async () => '[]',
    });

    render(<AuthProvider><AdminAppointmentsPage /></AuthProvider>);
    fireEvent.change(screen.getByLabelText('Tipo de cliente'), { target: { value: 'guest' } });

    expect(screen.getByLabelText('Nombre del cliente')).toBeRequired();
    expect(screen.getByLabelText('Apellido del cliente')).toBeRequired();
    expect(screen.getByLabelText('Correo (opcional)')).not.toBeRequired();
    expect(screen.getByLabelText('Teléfono (opcional)')).not.toBeRequired();
  });

  it('debe mostrar clientes aunque falle la carga de citas', async () => {
    localStorage.setItem('nicolet_jwt_user', JSON.stringify({ id: 1, nombre: 'Admin', rol: 'ADMINISTRADOR' }));
    vi.spyOn(global, 'fetch').mockImplementation(async (url) => {
      if (String(url).endsWith('/citas')) {
        return { ok: false, status: 500, text: async () => JSON.stringify({ error: 'Error temporal de citas' }) };
      }
      if (String(url).endsWith('/clientes')) {
        return { ok: true, status: 200, text: async () => JSON.stringify([{ id: 17, usuario: { nombre: 'Cliente', apellidoP: 'Existente' } }]) };
      }
      return { ok: true, status: 200, text: async () => '[]' };
    });

    render(<AuthProvider><AdminAppointmentsPage /></AuthProvider>);

    await waitFor(() => expect(screen.getByRole('option', { name: 'Cliente Existente' })).toBeInTheDocument());
    expect(screen.getAllByText(/No se pudieron cargar citas: Error temporal de citas/)).not.toHaveLength(0);
  });

  it('debe mostrar disponibilidad por bloques y bloquear la hora que entra en conflicto con colación en administración', async () => {
    localStorage.setItem('nicolet_jwt_user', JSON.stringify({ id: 1, nombre: 'Admin', rol: 'ADMINISTRADOR' }));
    vi.spyOn(global, 'fetch').mockImplementation(async (url) => {
      const requestUrl = String(url);
      if (requestUrl.includes('/disponibilidad?')) {
        return {
          ok: true,
          status: 200,
          text: async () => JSON.stringify([
            { horaInicio: '11:00:00', horaFin: '12:00:00', disponible: true, motivoNoDisponible: null },
            { horaInicio: '12:00:00', horaFin: '13:00:00', disponible: false, motivoNoDisponible: 'DESCANSO' },
          ]),
        };
      }
      if (requestUrl.endsWith('/trabajadores')) {
        return { ok: true, status: 200, text: async () => JSON.stringify([{ id: 4, usuario: { nombre: 'Camila', apellidoP: 'Silva' } }]) };
      }
      if (requestUrl.endsWith('/servicios')) {
        return { ok: true, status: 200, text: async () => JSON.stringify([{ id: 2, nombre: 'Manicura rusa', duracionMinutos: 60, precio: 22000 }]) };
      }
      return { ok: true, status: 200, text: async () => '[]' };
    });

    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const selectedDate = `${tomorrow.getFullYear()}-${String(tomorrow.getMonth() + 1).padStart(2, '0')}-${String(tomorrow.getDate()).padStart(2, '0')}`;
    render(<AuthProvider><AdminAppointmentsPage /></AuthProvider>);
    await waitFor(() => expect(screen.getByRole('option', { name: 'Camila Silva' })).toBeInTheDocument());
    fireEvent.change(screen.getByLabelText('Profesional'), { target: { value: '4' } });
    fireEvent.change(screen.getByLabelText('Servicio'), { target: { value: '2' } });
    fireEvent.change(screen.getByLabelText('Elegir otra fecha'), { target: { value: selectedDate } });

    expect(await screen.findByRole('button', { name: '12:00, Conflicto con colación' })).toBeDisabled();
    expect(screen.getByRole('status')).toHaveTextContent(
      'Los bloques marcados como “Conflicto con colación” no se pueden reservar: la duración del servicio (60 minutos) invade el descanso de la profesional.',
    );
    expect(screen.getByRole('button', { name: 'Crear cita' })).toBeDisabled();
    fireEvent.click(screen.getByRole('button', { name: '11:00, disponible' }));
    expect(screen.getByRole('button', { name: 'Crear cita' })).toBeEnabled();
  });

  it('debe mostrar y cargar al editar descripciones corregidas en el catálogo de administración', async () => {
    localStorage.setItem('nicolet_jwt_user', JSON.stringify({ id: 1, nombre: 'Admin', rol: 'ADMINISTRADOR' }));
    vi.spyOn(global, 'fetch').mockImplementation(async (url) => {
      if (String(url).endsWith('/servicios')) {
        return {
          ok: true,
          status: 200,
          text: async () => JSON.stringify([{
            id: 2,
            nombre: 'Manicura rusa',
            descripcion: 'Limpieza profunda de cutÃ­culas y esmaltado de alta duraciÃ³n',
            duracionMinutos: 60,
            precio: 22000,
          }]),
        };
      }
      return { ok: true, status: 200, text: async () => '[]' };
    });

    render(<AuthProvider><AdminServicesPage /></AuthProvider>);

    expect(await screen.findByText('Limpieza profunda de cutículas y esmaltado de alta duración')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Editar' }));
    expect(screen.getByLabelText('Descripción')).toHaveValue('Limpieza profunda de cutículas y esmaltado de alta duración');
  });

  it('debe corregir texto con codificación dañada en la biografía de disponibilidad', async () => {
    localStorage.setItem('nicolet_jwt_user', JSON.stringify({ id: 1, nombre: 'Admin', rol: 'ADMINISTRADOR' }));
    vi.spyOn(global, 'fetch').mockImplementation(async (url) => {
      if (String(url).endsWith('/trabajadores')) {
        return {
          ok: true,
          status: 200,
          text: async () => JSON.stringify([{
            id: 8,
            usuario: { nombre: 'Camila', apellidoP: 'Silva' },
            cargoEspecialidad: 'Especialista en Estilismo y Manicura',
            biografia: '5 aÃ±os de experiencia en estÃ©tica integral',
          }]),
        };
      }
      return { ok: true, status: 200, text: async () => '[]' };
    });

    render(<AuthProvider><AdminAvailabilityPage /></AuthProvider>);
    await waitFor(() => expect(screen.getByRole('option', { name: 'Camila Silva' })).toBeInTheDocument());
    fireEvent.change(screen.getByLabelText('Profesional'), { target: { value: '8' } });

    expect(await screen.findByText('5 años de experiencia en estética integral')).toBeInTheDocument();
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
    localStorage.setItem('nicolet_jwt_user', JSON.stringify({ id: 1, nombre: 'Admin', rol: 'ADMINISTRADOR' }));
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

  it('no muestra la página de correos ni consulta la bandeja sin sesión de administrador', () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue({ ok: true, status: 200, text: () => Promise.resolve('{}') });

    const anonimo = render(<AuthProvider><EmailTemplatesPage /></AuthProvider>);
    expect(screen.getByText('Verificando sesión…')).toBeInTheDocument();
    expect(screen.queryByText(/Bandeja de correos despachados/i)).not.toBeInTheDocument();
    expect(window.location.replace).toHaveBeenCalledWith('/admin/login?redirect=%2F');
    anonimo.unmount();

    localStorage.setItem('nicolet_jwt_user', JSON.stringify({ id: 2, nombre: 'Clienta', rol: 'CLIENTE' }));
    render(<AuthProvider><EmailTemplatesPage /></AuthProvider>);
    expect(screen.getByText('Acceso restringido')).toBeInTheDocument();
    expect(screen.queryByText(/Bandeja de correos despachados/i)).not.toBeInTheDocument();

    expect(fetchSpy).not.toHaveBeenCalled();
  });
});
