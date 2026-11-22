// Utilidades del pago en línea con Webpay Plus.

/** Texto corto con la situación del pago de una cita (vacío si no aplica, p. ej. cita cancelada). */
export function paymentLabel(appointment) {
  if (!appointment || appointment.estado === 'CANCELADA') return '';
  return ({
    PENDIENTE_PAGO: 'Esperando pago',
    PAGADO: 'Pagado en línea',
    PAGO_EN_LOCAL: 'Pago en el estudio',
  })[appointment.estadoPago] || '';
}

/** Solo se acepta enviar al cliente a una dirección https de Transbank. */
export function isWebpayUrl(value) {
  try {
    const url = new URL(value);
    return url.protocol === 'https:' && (url.hostname === 'transbank.cl' || url.hostname.endsWith('.transbank.cl'));
  } catch {
    return false;
  }
}

/** Lleva al navegador al formulario de Webpay: debe ser un POST con el campo `token_ws`. */
export function redirectToWebpay(url, token, doc = document) {
  if (!isWebpayUrl(url) || !token) throw new Error('La dirección de pago recibida no es válida.');
  const form = doc.createElement('form');
  form.method = 'POST';
  form.action = url;
  const field = doc.createElement('input');
  field.type = 'hidden';
  field.name = 'token_ws';
  field.value = token;
  form.appendChild(field);
  doc.body.appendChild(form);
  form.submit();
}
