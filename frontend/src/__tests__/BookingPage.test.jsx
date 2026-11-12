import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, cleanup, fireEvent } from '@testing-library/react';
import { businessToday } from '../utils/dates.js';

vi.mock('../api/client.js', () => ({
  api: {
    servicios: vi.fn(), trabajadores: vi.fn(), disponibilidad: vi.fn(),
    clientePorUsuario: vi.fn(), crearCita: vi.fn(),
  },
}));
const authState = { user: null, register: vi.fn(), logout: vi.fn() };
vi.mock('../auth/AuthContext.jsx', () => ({ useAuth: () => authState }));

import { api } from '../api/client.js';
import BookingPage from '../pages/BookingPage.jsx';

const addDays = (iso, days) => {
  const date = new Date(`${iso}T12:00:00Z`);
  date.setUTCDate(date.getUTCDate() + days);
  return date.toISOString().slice(0, 10);
};
const FUTURE = addDays(businessToday(), 3);
const PAST = addDays(businessToday(), -2);

const SERVICES = [
  { id: 1, nombre: 'Corte', duracionMinutos: 45, precio: 18000 },
  { id: 2, nombre: 'Masaje', duracionMinutos: 60, precio: 32000 },
];
const WORKERS = [
  { id: 10, usuario: { nombre: 'Camila', apellidoP: 'Silva' }, cargoEspecialidad: 'Estilista', servicios: [{ id: 1 }] },
  { id: 11, usuario: { nombre: 'Daniela', apellidoP: 'Muñoz' }, cargoEspecialidad: 'Masajista', servicios: [{ id: 2 }] },
];
const slot = (hora, disponible = true) => ({ horaInicio: `${hora}:00`, horaFin: '11:00:00', disponible, motivoNoDisponible: disponible ? null : 'OCUPADO' });

function open(search) {
  window.history.pushState({}, '', `/reservar.html${search}`);
  return render(<BookingPage />);
}

describe('BookingPage: servicio-profesional, enlaces parciales y borrador (E09, E18, E19)', () => {
  beforeEach(() => {
    cleanup();
    vi.clearAllMocks();
    authState.user = null;
    api.servicios.mockResolvedValue(SERVICES);
    api.trabajadores.mockResolvedValue(WORKERS);
    api.disponibilidad.mockResolvedValue([slot('09:00'), slot('10:00'), slot('11:00', false)]);
  });

  it('E09: tras elegir el servicio solo se ofrecen los profesionales que lo atienden', async () => {
    open('?servicioId=1');
    expect(await screen.findByText('2. Elige a tu especialista')).toBeInTheDocument();
    expect(screen.getByText('Camila Silva')).toBeInTheDocument();
    expect(screen.queryByText('Daniela Muñoz')).not.toBeInTheDocument();
  });

  it('E19: un enlace con profesional pero sin servicio abre el paso 1 y no consulta disponibilidad', async () => {
    open('?trabajadorId=10');
    expect(await screen.findByText('1. Elige tu servicio')).toBeInTheDocument();
    expect(api.disponibilidad).not.toHaveBeenCalled();
    expect(screen.queryByText(/No hay horas disponibles/)).not.toBeInTheDocument();
  });

  it('E19: servicio inexistente o profesional que no ofrece el servicio → primer paso incompleto', async () => {
    open('?servicioId=999&trabajadorId=10');
    expect(await screen.findByText('1. Elige tu servicio')).toBeInTheDocument();
    cleanup();
    open('?servicioId=1&trabajadorId=11'); // Daniela no ofrece Corte
    expect(await screen.findByText('2. Elige a tu especialista')).toBeInTheDocument();
    expect(api.disponibilidad).not.toHaveBeenCalled();
  });

  it('E19: una fecha pasada o malformada abre el paso 3 sin fecha elegida ni consulta', async () => {
    open(`?servicioId=1&trabajadorId=10&fecha=${PAST}&hora=10:00`);
    expect(await screen.findByText('3. Selecciona fecha y hora')).toBeInTheDocument();
    expect(api.disponibilidad).not.toHaveBeenCalled();
    expect(screen.getByRole('button', { name: 'Continuar' })).toBeDisabled();
    cleanup();
    open('?servicioId=1&trabajadorId=10&fecha=no-es-fecha');
    expect(await screen.findByText('3. Selecciona fecha y hora')).toBeInTheDocument();
    expect(api.disponibilidad).not.toHaveBeenCalled();
  });

  it('E18: al volver del login se revalida la disponibilidad y se conserva el horario si sigue libre', async () => {
    open(`?servicioId=1&trabajadorId=10&fecha=${FUTURE}&hora=10:00`);
    await waitFor(() => expect(api.disponibilidad).toHaveBeenCalledWith('10', FUTURE, '1'));
    const elegido = await screen.findByRole('button', { name: '10:00, disponible' });
    expect(elegido.className).toContain('selected');
    expect(screen.getByRole('button', { name: 'Continuar' })).toBeEnabled();
    expect(api.crearCita).not.toHaveBeenCalled(); // nunca se confirma automáticamente
  });

  it('E18: si el horario guardado ya no está disponible se descarta con un aviso', async () => {
    api.disponibilidad.mockResolvedValue([slot('09:00'), slot('10:00', false)]);
    open(`?servicioId=1&trabajadorId=10&fecha=${FUTURE}&hora=10:00`);
    expect(await screen.findByRole('alert')).toHaveTextContent('ya no está disponible');
    expect(screen.getByRole('button', { name: 'Continuar' })).toBeDisabled();
    expect(api.crearCita).not.toHaveBeenCalled();
  });

  it('E18: el enlace "Inicia sesión" conserva servicio, profesional, fecha y hora', async () => {
    open(`?servicioId=1&trabajadorId=10&fecha=${FUTURE}&hora=10:00`);
    fireEvent.click(await screen.findByRole('button', { name: 'Continuar' }));
    const link = await screen.findByRole('link', { name: 'Inicia sesión' });
    const redirect = new URL(link.getAttribute('href'), window.location.origin).searchParams.get('redirect');
    const target = new URL(redirect, window.location.origin);
    expect(target.pathname).toBe('/reservar.html');
    expect(Object.fromEntries(target.searchParams)).toEqual({ servicioId: '1', trabajadorId: '10', fecha: FUTURE, hora: '10:00' });
  });

  it('un conflicto al confirmar (horario tomado) se muestra al usuario y no crea reserva', async () => {
    authState.user = { id: 5, nombre: 'Ana', correo: 'ana@x.cl', rol: 'CLIENTE' };
    api.clientePorUsuario.mockResolvedValue({ id: 77 });
    api.crearCita.mockRejectedValue(new Error('El profesional ya tiene una cita agendada en el horario solicitado'));
    const { container } = open(`?servicioId=1&trabajadorId=10&fecha=${FUTURE}&hora=10:00`);
    fireEvent.click(await screen.findByRole('button', { name: 'Continuar' }));
    await screen.findByText('4. Confirma tus datos');
    fireEvent.submit(container.querySelector('form'));
    expect(await screen.findByRole('alert')).toHaveTextContent('ya tiene una cita agendada');
    expect(screen.queryByText('Reserva recibida')).not.toBeInTheDocument();
  });
});
