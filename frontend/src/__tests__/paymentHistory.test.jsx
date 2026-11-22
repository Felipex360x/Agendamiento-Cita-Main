import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor, cleanup, fireEvent, within } from '@testing-library/react';

vi.mock('../api/client.js', () => ({ api: { historialPagos: vi.fn() } }));
const authState = { user: { id: 1, nombre: 'Admin', rol: 'ADMINISTRADOR' }, loading: false, logout: vi.fn() };
vi.mock('../auth/AuthContext.jsx', () => ({ useAuth: () => authState }));

import { api } from '../api/client.js';
import AdminPaymentsPage from '../pages/AdminPaymentsPage.jsx';
import { csvCell, filterPayments, paymentsToCsv, summarizePayments } from '../utils/paymentHistory.js';
import { resolvePage } from '../routes.js';

const pago = (id, estado, monto, extra = {}) => ({
  id, estado, monto, codigoReserva: `RES-2026-${id}`, clienteNombre: `Clienta ${id}`, clienteCorreo: `c${id}@correo.test`,
  servicio: 'Manicura', profesional: 'Camila Silva', ordenCompra: `RES-2026-${id}-1`, estadoCita: 'PENDIENTE',
  creadoEn: '2026-11-22T16:15:02', actualizadoEn: '2026-11-22T16:16:10', fechaCita: '2026-11-30T14:00:00',
  codigoAutorizacion: null, tipoPago: null, ultimos4Digitos: null, ...extra,
});
const LISTA = [
  pago(1, 'APROBADO', 22000, { codigoAutorizacion: '1213', tipoPago: 'Crédito en 1 pago (VN)', ultimos4Digitos: '6623' }),
  pago(2, 'APROBADO', 18000),
  pago(3, 'RECHAZADO', 22000),
  pago(4, 'ANULADO_POR_USUARIO', 22000),
  pago(5, 'EXPIRADO', 22000),
  pago(6, 'INICIADO', 22000),
];

describe('historial de pagos: lógica', () => {
  it('solo los pagos aprobados suman al total cobrado', () => {
    expect(summarizePayments(LISTA)).toEqual({ collected: 40000, approved: 2, rejected: 1, notCompleted: 2, inProgress: 1 });
    expect(summarizePayments([])).toEqual({ collected: 0, approved: 0, rejected: 0, notCompleted: 0, inProgress: 0 });
    expect(summarizePayments(undefined).collected).toBe(0);
  });

  it('busca por reserva, clienta, correo, servicio, profesional u orden', () => {
    expect(filterPayments(LISTA, 'res-2026-3')).toHaveLength(1);
    expect(filterPayments(LISTA, 'C4@CORREO')).toHaveLength(1);
    expect(filterPayments(LISTA, 'camila')).toHaveLength(6);
    expect(filterPayments(LISTA, 'no existe')).toHaveLength(0);
    expect(filterPayments(LISTA, '   ')).toHaveLength(6);
  });

  it('el CSV escapa comillas y neutraliza fórmulas de Excel', () => {
    expect(csvCell('di "hola"')).toBe('"di ""hola"""');
    expect(csvCell('=HYPERLINK("http://x")')).toBe('"\'=HYPERLINK(""http://x"")"');
    expect(csvCell('+56 9 1234')).toBe('"\'+56 9 1234"');
    expect(csvCell(null)).toBe('""');
    const csv = paymentsToCsv([pago(1, 'APROBADO', 22000, { clienteNombre: '=cmd' })]);
    expect(csv.split('\r\n')).toHaveLength(2);
    expect(csv).toContain('"\'=cmd"').toContain('"Pagado"');
  });

  it('la página del historial tiene ruta de administración', () => {
    expect(resolvePage('/admin/pagos')).toBe('admin/pagos.html');
    expect(resolvePage('/pagos')).toBe('admin/pagos.html');
  });
});

describe('página Historial de pagos', () => {
  beforeEach(() => { api.historialPagos.mockReset(); });
  afterEach(cleanup);

  it('muestra el resumen, la tabla y el detalle de un pago aprobado', async () => {
    api.historialPagos.mockResolvedValue({ pagos: LISTA, truncado: false });
    render(<AdminPaymentsPage />);

    await screen.findByText('RES-2026-1');
    const total = screen.getByText('Total cobrado').closest('article');
    expect(total.textContent).toMatch(/40\.000/);
    expect(screen.getByText('Rechazados').closest('article').textContent).toContain('1');
    expect(screen.getByText('No completados').closest('article').textContent).toContain('2');
    expect(screen.getByText(/1 pago\(s\) esperando/)).toBeTruthy();

    fireEvent.click(screen.getAllByRole('button', { name: 'Ver detalle' })[0]);
    const dialog = screen.getByRole('dialog');
    expect(within(dialog).getByText('RES-2026-1')).toBeTruthy();
    expect(dialog.textContent).toContain('1213');
    expect(dialog.textContent).toContain('6623');
  });

  it('un pago rechazado aclara que no hubo cobro y no muestra datos de tarjeta', async () => {
    api.historialPagos.mockResolvedValue({ pagos: [pago(3, 'RECHAZADO', 22000)], truncado: false });
    render(<AdminPaymentsPage />);
    await screen.findByText('RES-2026-3');
    fireEvent.click(screen.getByRole('button', { name: 'Ver detalle' }));
    const dialog = screen.getByRole('dialog');
    expect(dialog.textContent).toContain('no se hizo ningún cargo');
    expect(dialog.textContent).not.toContain('Código de autorización');
  });

  it('al cambiar el estado vuelve a pedir el historial con ese filtro', async () => {
    api.historialPagos.mockResolvedValue({ pagos: LISTA, truncado: false });
    render(<AdminPaymentsPage />);
    await screen.findByText('RES-2026-1');
    fireEvent.change(screen.getByLabelText('Estado del pago'), { target: { value: 'RECHAZADO' } });
    await waitFor(() => expect(api.historialPagos).toHaveBeenLastCalledWith({ estado: 'RECHAZADO', desde: '', hasta: '' }));
  });

  it('avisa si hay más pagos que los mostrados, y si no hay ninguno', async () => {
    api.historialPagos.mockResolvedValue({ pagos: LISTA, truncado: true });
    const { unmount } = render(<AdminPaymentsPage />);
    await screen.findByText(/se ven los 1\.000 más recientes/);
    unmount();

    api.historialPagos.mockResolvedValue({ pagos: [], truncado: false });
    render(<AdminPaymentsPage />);
    await screen.findByText('No hay registros para mostrar.');
  });

  it('si el servidor falla muestra el error y no deja la pantalla cargando', async () => {
    api.historialPagos.mockRejectedValue(new Error('No tienes permiso'));
    render(<AdminPaymentsPage />);
    await screen.findByText('No tienes permiso');
    expect(screen.queryByText('Cargando…')).toBeNull();
  });

  it('un rango de fechas al revés no consulta al servidor', async () => {
    api.historialPagos.mockResolvedValue({ pagos: LISTA, truncado: false });
    render(<AdminPaymentsPage />);
    await screen.findByText('RES-2026-1');
    api.historialPagos.mockClear();
    fireEvent.change(screen.getByLabelText('Desde'), { target: { value: '2026-11-20' } });
    await waitFor(() => expect(api.historialPagos).toHaveBeenCalledTimes(1));
    fireEvent.change(screen.getByLabelText('Hasta'), { target: { value: '2026-11-10' } });
    await screen.findByText(/no puede ser posterior/);
    expect(api.historialPagos).toHaveBeenCalledTimes(1);
  });
});
