// Historial de pagos para administración: etiquetas, resumen, búsqueda y exportación.

export const PAYMENT_STATES = {
  APROBADO: { label: 'Pagado', tone: 'confirmada' },
  RECHAZADO: { label: 'Rechazado', tone: 'cancelada' },
  ANULADO_POR_USUARIO: { label: 'Anulado por la clienta', tone: 'cancelada' },
  EXPIRADO: { label: 'Vencido', tone: 'pendiente' },
  ERROR: { label: 'Error de Webpay', tone: 'cancelada' },
  INICIADO: { label: 'Esperando pago', tone: 'pendiente' },
};

export const paymentStateLabel = (state) => PAYMENT_STATES[state]?.label || state || '—';
export const paymentStateTone = (state) => PAYMENT_STATES[state]?.tone || '';

/** Totales de la lista mostrada. Solo los pagos aprobados cuentan como dinero cobrado. */
export function summarizePayments(payments) {
  const summary = { collected: 0, approved: 0, rejected: 0, notCompleted: 0, inProgress: 0 };
  for (const payment of payments || []) {
    if (payment.estado === 'APROBADO') { summary.approved += 1; summary.collected += Number(payment.monto) || 0; }
    else if (payment.estado === 'RECHAZADO') summary.rejected += 1;
    else if (payment.estado === 'INICIADO') summary.inProgress += 1;
    else summary.notCompleted += 1;
  }
  return summary;
}

/** Busca por código de reserva, clienta, correo, servicio, profesional u orden de compra. */
export function filterPayments(payments, query) {
  const text = String(query || '').trim().toLowerCase();
  if (!text) return payments || [];
  return (payments || []).filter((payment) => [
    payment.codigoReserva, payment.clienteNombre, payment.clienteCorreo, payment.servicio, payment.profesional, payment.ordenCompra,
  ].some((value) => String(value || '').toLowerCase().includes(text)));
}

/** Celda de CSV segura: escapa comillas y evita que Excel ejecute textos que empiezan con = + - @. */
export function csvCell(value) {
  let text = value === null || value === undefined ? '' : String(value);
  if (/^[=+\-@\t\r]/.test(text)) text = `'${text}`;
  return `"${text.replace(/"/g, '""')}"`;
}

export function paymentsToCsv(payments) {
  const header = ['Fecha', 'Reserva', 'Clienta', 'Correo', 'Servicio', 'Profesional', 'Estado', 'Monto', 'Orden de compra', 'Autorización', 'Tipo de pago', 'Tarjeta (últimos 4)'];
  const rows = (payments || []).map((payment) => [
    (payment.creadoEn || '').replace('T', ' ').slice(0, 16), payment.codigoReserva, payment.clienteNombre, payment.clienteCorreo,
    payment.servicio, payment.profesional, paymentStateLabel(payment.estado), payment.monto, payment.ordenCompra,
    payment.codigoAutorizacion, payment.tipoPago, payment.ultimos4Digitos,
  ]);
  return [header, ...rows].map((row) => row.map(csvCell).join(';')).join('\r\n');
}
