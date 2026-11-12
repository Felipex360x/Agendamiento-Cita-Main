import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, cleanup, fireEvent, within, act } from '@testing-library/react';
import { businessToday } from '../utils/dates.js';

vi.mock('../api/client.js', () => ({
  api: {
    trabajadores: vi.fn(), servicios: vi.fn(), citas: vi.fn(), citasDeTrabajador: vi.fn(), trabajadorPorUsuario: vi.fn(),
    disponibilidad: vi.fn(),
  },
}));
const authState = { user: { id: 1, nombre: 'Admin', rol: 'ADMINISTRADOR' }, loading: false, logout: vi.fn() };
vi.mock('../auth/AuthContext.jsx', () => ({ useAuth: () => authState }));

import { api } from '../api/client.js';
import AdminHistoryPage from '../pages/AdminHistoryPage.jsx';
import AdminAvailabilityPage from '../pages/AdminAvailabilityPage.jsx';
import { StaffAgendaPage } from '../pages/StaffPages.jsx';

const WORKERS = [
  { id: 10, usuario: { nombre: 'Camila', apellidoP: 'Silva' }, servicios: [{ id: 1 }, { id: 2 }] },
  { id: 11, usuario: { nombre: 'Daniela', apellidoP: 'Muñoz' }, servicios: [{ id: 2 }] },
];
const SERVICES = [{ id: 1, nombre: 'Corte' }, { id: 2, nombre: 'Masaje' }];

const addDays = (iso, days) => {
  const date = new Date(`${iso}T12:00:00Z`);
  date.setUTCDate(date.getUTCDate() + days);
  return date.toISOString().slice(0, 10);
};
const mondayOfWeek = () => {
  const today = businessToday();
  const weekday = (new Date(`${today}T12:00:00Z`).getUTCDay() + 6) % 7; // 0 = lunes
  return addDays(today, -weekday);
};
const cita = (id, estado, precio, fecha = `${businessToday()}T10:00:00`, extra = {}) => ({
  id, estado, precioFinal: precio, fechaHoraInicio: fecha, codigoReserva: `RES-${id}`,
  cliente: { id: 100 + id, usuario: { nombre: `Cliente${id}`, apellidoP: 'X' } },
  trabajador: { id: 10, usuario: { nombre: 'Camila', apellidoP: 'Silva' } },
  servicio: { nombre: 'Corte' }, ...extra,
});
const deferred = () => { let resolve, reject; const promise = new Promise((a, b) => { resolve = a; reject = b; }); return { promise, resolve, reject }; };

beforeEach(() => {
  cleanup();
  vi.clearAllMocks();
  authState.user = { id: 1, nombre: 'Admin', rol: 'ADMINISTRADOR' };
  api.trabajadores.mockResolvedValue(WORKERS);
  api.servicios.mockResolvedValue(SERVICES);
});

describe('E15: Agenda semanal con fines de semana', () => {
  it('muestra reservas de sábado y domingo, no solo de lunes a viernes', async () => {
    const lunes = mondayOfWeek();
    api.citas.mockResolvedValue([
      cita(1, 'CONFIRMADA', 10000, `${addDays(lunes, 2)}T10:00:00`),
      cita(2, 'CONFIRMADA', 10000, `${addDays(lunes, 5)}T11:00:00`, { cliente: { id: 7, usuario: { nombre: 'Sabado', apellidoP: 'Cliente' } } }),
      cita(3, 'PENDIENTE', 10000, `${addDays(lunes, 6)}T12:00:00`, { cliente: { id: 8, usuario: { nombre: 'Domingo', apellidoP: 'Cliente' } } }),
      cita(4, 'PENDIENTE', 10000, `${addDays(lunes, 7)}T12:00:00`, { cliente: { id: 9, usuario: { nombre: 'Siguiente', apellidoP: 'Semana' } } }),
    ]);
    render(<StaffAgendaPage />);
    expect(await screen.findByText('Sabado Cliente')).toBeInTheDocument();
    expect(screen.getByText('Domingo Cliente')).toBeInTheDocument();
    expect(screen.getByText('Cliente1 X')).toBeInTheDocument();
    expect(screen.queryByText('Siguiente Semana')).not.toBeInTheDocument(); // otra semana
  });
});

describe('E20: ingresos y valor reservado con una sola definición', () => {
  it('canceladas, pendientes y no asistidas no cuentan como ingreso; solo las completadas', async () => {
    api.citasDeTrabajador.mockResolvedValue([
      cita(1, 'CANCELADA', 20000), cita(2, 'PENDIENTE', 10000), cita(3, 'CONFIRMADA', 5000),
      cita(4, 'COMPLETADA', 30000), cita(5, 'NO_ASISTIO', 7000),
    ]);
    render(<AdminHistoryPage />);
    fireEvent.change(await screen.findByLabelText('Profesional'), { target: { value: '10' } });
    const ingresos = (await screen.findByText('Ingresos (citas completadas)')).closest('article');
    expect(within(ingresos).getByText(/30\.000/)).toBeInTheDocument();
    const reservado = screen.getByText('Valor reservado').closest('article');
    expect(within(reservado).getByText(/15\.000/)).toBeInTheDocument(); // 10.000 pendiente + 5.000 confirmada
  });

  it('una cancelada de $20.000 sin atenciones completadas no genera ingresos', async () => {
    api.citasDeTrabajador.mockResolvedValue([cita(1, 'CANCELADA', 20000)]);
    render(<AdminHistoryPage />);
    fireEvent.change(await screen.findByLabelText('Profesional'), { target: { value: '10' } });
    const ingresos = (await screen.findByText('Ingresos (citas completadas)')).closest('article');
    expect(within(ingresos).getByText(/\$\s?0$/)).toBeInTheDocument();
  });
});

describe('E21: historial — error→éxito y cambios rápidos de profesional', () => {
  it('un error previo desaparece al cargar correctamente otro profesional', async () => {
    api.citasDeTrabajador.mockRejectedValueOnce(new Error('Falla temporal')).mockResolvedValueOnce([cita(1, 'COMPLETADA', 1000)]);
    render(<AdminHistoryPage />);
    const select = await screen.findByLabelText('Profesional');
    fireEvent.change(select, { target: { value: '10' } });
    expect((await screen.findAllByRole('alert'))[0]).toHaveTextContent('Falla temporal');
    fireEvent.change(select, { target: { value: '11' } });
    await waitFor(() => expect(screen.queryByRole('alert')).not.toBeInTheDocument());
    expect(await screen.findByText('Cliente1 X')).toBeInTheDocument();
  });

  it('una respuesta lenta del profesional anterior no pisa al vigente, y los resultados viejos se limpian', async () => {
    const lento = deferred();
    api.citasDeTrabajador.mockImplementation((id) => (String(id) === '10' ? lento.promise : Promise.resolve([cita(2, 'COMPLETADA', 2000)])));
    render(<AdminHistoryPage />);
    const select = await screen.findByLabelText('Profesional');
    fireEvent.change(select, { target: { value: '10' } });
    fireEvent.change(select, { target: { value: '11' } });
    expect(await screen.findByText('Cliente2 X')).toBeInTheDocument();
    await act(async () => { lento.resolve([cita(1, 'COMPLETADA', 1000)]); });
    expect(screen.queryByText('Cliente1 X')).not.toBeInTheDocument();
    expect(screen.getByText('Cliente2 X')).toBeInTheDocument();
  });
});

describe('E19/E21: disponibilidad del administrador', () => {
  const slots = [{ horaInicio: '09:00:00', horaFin: '09:45:00', disponible: true }];
  const buscar = async () => {
    fireEvent.change(await screen.findByLabelText('Profesional'), { target: { value: '10' } });
    fireEvent.click(screen.getByRole('button', { name: 'Ver disponibilidad' }));
  };

  it('el enlace de reserva conserva exactamente los filtros de la consulta mostrada', async () => {
    api.disponibilidad.mockResolvedValue(slots);
    render(<AdminAvailabilityPage />);
    fireEvent.change(await screen.findByLabelText('Servicio'), { target: { value: '1' } });
    await buscar();
    const link = await screen.findByRole('link', { name: 'Reservar este día' });
    const href = new URL(link.getAttribute('href'), window.location.origin);
    expect(href.searchParams.get('trabajadorId')).toBe('10');
    expect(href.searchParams.get('servicioId')).toBe('1');
    expect(href.searchParams.get('fecha')).toBe(businessToday());
  });

  it('sin servicio el enlace no inventa servicioId (BookingPage abre el primer paso incompleto)', async () => {
    api.disponibilidad.mockResolvedValue(slots);
    render(<AdminAvailabilityPage />);
    await buscar();
    const link = await screen.findByRole('link', { name: 'Reservar este día' });
    expect(new URL(link.getAttribute('href'), window.location.origin).searchParams.has('servicioId')).toBe(false);
  });

  it('cambiar de profesional, servicio o fecha invalida los bloques mostrados y el enlace', async () => {
    api.disponibilidad.mockResolvedValue(slots);
    render(<AdminAvailabilityPage />);
    await buscar();
    expect(await screen.findByRole('link', { name: 'Reservar este día' })).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('Servicio'), { target: { value: '2' } });
    expect(screen.queryByRole('link', { name: 'Reservar este día' })).not.toBeInTheDocument();
    expect(screen.queryByText('09:00–09:45')).not.toBeInTheDocument();
  });

  it('una respuesta anterior que llega tarde se descarta si cambiaron los filtros', async () => {
    const lenta = deferred();
    api.disponibilidad.mockReturnValueOnce(lenta.promise);
    render(<AdminAvailabilityPage />);
    await buscar();
    fireEvent.change(screen.getByLabelText('Servicio'), { target: { value: '2' } }); // cambia el criterio
    await act(async () => { lenta.resolve(slots); });
    expect(screen.queryByText('09:00–09:45')).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Reservar este día' })).not.toBeInTheDocument();
  });

  it('un error se limpia al reintentar con éxito', async () => {
    api.disponibilidad.mockRejectedValueOnce(new Error('El profesional no ofrece el servicio solicitado')).mockResolvedValueOnce(slots);
    render(<AdminAvailabilityPage />);
    await buscar();
    expect((await screen.findAllByRole('alert'))[0]).toHaveTextContent('no ofrece el servicio');
    fireEvent.click(screen.getByRole('button', { name: 'Ver disponibilidad' }));
    await screen.findByRole('link', { name: 'Reservar este día' });
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('solo se listan los servicios que atiende el profesional elegido (E09)', async () => {
    render(<AdminAvailabilityPage />);
    fireEvent.change(await screen.findByLabelText('Profesional'), { target: { value: '11' } }); // solo Masaje
    const opciones = within(screen.getByLabelText('Servicio')).getAllByRole('option').map((o) => o.textContent);
    expect(opciones).toEqual(['Duración predeterminada', 'Masaje']);
  });
});
