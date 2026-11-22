import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor, cleanup } from '@testing-library/react';

vi.mock('../api/client.js', () => ({ api: { citaPorCodigo: vi.fn() } }));
vi.mock('../auth/AuthContext.jsx', () => ({ useAuth: () => ({ user: null, logout: vi.fn() }) }));

import { api } from '../api/client.js';
import PaymentResultPage from '../pages/PaymentResultPage.jsx';
import { isWebpayUrl, paymentLabel, redirectToWebpay } from '../utils/payments.js';
import { resolvePage } from '../routes.js';

describe('utilidades de pago', () => {
  it('solo acepta direcciones https de Transbank', () => {
    expect(isWebpayUrl('https://webpay3gint.transbank.cl/webpayserver/initTransaction')).toBe(true);
    expect(isWebpayUrl('https://webpay3g.transbank.cl/webpayserver/initTransaction')).toBe(true);
    expect(isWebpayUrl('http://webpay3g.transbank.cl/x')).toBe(false);
    expect(isWebpayUrl('https://transbank.cl.evil.com/x')).toBe(false);
    expect(isWebpayUrl('https://eviltransbank.cl/x')).toBe(false);
    expect(isWebpayUrl('javascript:alert(1)')).toBe(false);
    expect(isWebpayUrl('')).toBe(false);
  });

  it('redirectToWebpay envía un POST con token_ws y rechaza direcciones ajenas', () => {
    const submit = vi.fn();
    const doc = document.implementation.createHTMLDocument('x');
    const original = doc.createElement.bind(doc);
    doc.createElement = (tag) => { const el = original(tag); if (tag === 'form') el.submit = submit; return el; };

    redirectToWebpay('https://webpay3gint.transbank.cl/webpayserver/initTransaction', 'tok-9', doc);

    const form = doc.querySelector('form');
    expect(form.method).toBe('post');
    expect(form.action).toBe('https://webpay3gint.transbank.cl/webpayserver/initTransaction');
    expect(form.querySelector('input[name="token_ws"]').value).toBe('tok-9');
    expect(submit).toHaveBeenCalledTimes(1);
    expect(() => redirectToWebpay('https://evil.example/pay', 'tok', doc)).toThrow();
    expect(() => redirectToWebpay('https://webpay3gint.transbank.cl/x', '', doc)).toThrow();
  });

  it('paymentLabel describe el pago y se oculta en citas canceladas', () => {
    expect(paymentLabel({ estado: 'PENDIENTE', estadoPago: 'PENDIENTE_PAGO' })).toBe('Esperando pago');
    expect(paymentLabel({ estado: 'PENDIENTE', estadoPago: 'PAGADO' })).toBe('Pagado en línea');
    expect(paymentLabel({ estado: 'PENDIENTE', estadoPago: 'PAGO_EN_LOCAL' })).toBe('Pago en el estudio');
    expect(paymentLabel({ estado: 'CANCELADA', estadoPago: 'PENDIENTE_PAGO' })).toBe('');
    expect(paymentLabel(null)).toBe('');
  });

  it('la página de resultado del pago tiene ruta pública', () => {
    expect(resolvePage('/pago-resultado.html')).toBe('pago-resultado.html');
    expect(resolvePage('/pago-resultado')).toBe('pago-resultado.html');
  });
});

describe('PaymentResultPage', () => {
  beforeEach(() => { cleanup(); vi.clearAllMocks(); });
  afterEach(() => window.history.pushState({}, '', '/'));

  function open(search) {
    window.history.pushState({}, '', `/pago-resultado.html${search}`);
    return render(<PaymentResultPage />);
  }

  it('pago aprobado: confirma, muestra el código y el detalle de la cita', async () => {
    api.citaPorCodigo.mockResolvedValue({
      servicio: { nombre: 'Corte' }, trabajador: { usuario: { nombre: 'Camila', apellidoP: 'Silva' } },
      fechaHoraInicio: '2026-12-01T10:00:00', precioFinal: 18000,
    });
    open('?estado=aprobado&codigo=RES-2026-ABC123');

    expect(screen.getByRole('status')).toHaveTextContent('Pago exitoso');
    expect(screen.getByText('RES-2026-ABC123')).toBeInTheDocument();
    expect(await screen.findByText('Camila Silva')).toBeInTheDocument();
    expect(api.citaPorCodigo).toHaveBeenCalledWith('RES-2026-ABC123');
    expect(screen.getByRole('link', { name: 'Ver mis citas' })).toBeInTheDocument();
  });

  it.each([
    ['rechazado', 'No pudimos procesar tu pago'],
    ['anulado', 'Anulaste el pago'],
    ['expirado', 'Se acabó el tiempo'],
    ['error', 'Algo salió mal'],
  ])('estado %s: explica que no hubo cobro y ofrece volver a reservar', async (estado, titulo) => {
    open(`?estado=${estado}&codigo=RES-2026-ABC123`);
    expect(screen.getByRole('status')).toHaveTextContent(titulo);
    expect(screen.getByRole('link', { name: 'Volver a reservar' })).toBeInTheDocument();
    await waitFor(() => expect(api.citaPorCodigo).not.toHaveBeenCalled());
  });

  it('un estado desconocido o ausente se trata como error', () => {
    open('?estado=hackeado');
    expect(screen.getByRole('status')).toHaveTextContent('Algo salió mal');
    cleanup();
    open('');
    expect(screen.getByRole('status')).toHaveTextContent('Algo salió mal');
  });

  it('si no hay sesión, la consulta de la cita falla en silencio y se sigue mostrando el código', async () => {
    api.citaPorCodigo.mockRejectedValue(new Error('401'));
    open('?estado=aprobado&codigo=RES-2026-ABC123');
    await waitFor(() => expect(api.citaPorCodigo).toHaveBeenCalled());
    expect(screen.getByText('RES-2026-ABC123')).toBeInTheDocument();
  });
});
