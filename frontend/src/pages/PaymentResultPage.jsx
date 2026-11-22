import { useEffect, useMemo, useState } from 'react';
import { api } from '../api/client.js';
import { SiteFooter, SiteHeader } from '../layouts/SiteShell.jsx';

const money = (value) => new Intl.NumberFormat('es-CL', {
  style: 'currency', currency: 'CLP', maximumFractionDigits: 0,
}).format(Number(value) || 0);

const MESSAGES = {
  aprobado: {
    eyebrow: 'Pago recibido',
    title: '¡Pago exitoso! Tu hora quedó reservada.',
    text: 'Te enviamos el comprobante por correo. Tu especialista ya fue avisado y confirmará tu cita desde su panel.',
    ok: true,
  },
  rechazado: {
    eyebrow: 'Pago rechazado',
    title: 'No pudimos procesar tu pago.',
    text: 'Tu banco o Webpay rechazó la transacción y no se hizo ningún cobro. La hora se liberó: puedes volver a reservar e intentar con otra tarjeta.',
  },
  anulado: {
    eyebrow: 'Pago anulado',
    title: 'Anulaste el pago.',
    text: 'No se hizo ningún cobro y la hora se liberó. Si quieres, puedes volver a reservar.',
  },
  expirado: {
    eyebrow: 'Tiempo agotado',
    title: 'Se acabó el tiempo para pagar.',
    text: 'No se hizo ningún cobro y la hora se liberó para otras personas. Puedes volver a reservar.',
  },
  error: {
    eyebrow: 'No pudimos confirmar el pago',
    title: 'Algo salió mal con el pago.',
    text: 'No se confirmó el cobro y la hora se liberó. Si ves un cargo en tu tarjeta, escríbenos por WhatsApp indicando tu código de reserva.',
  },
};

function workerName(worker) {
  return [worker?.usuario?.nombre, worker?.usuario?.apellidoP].filter(Boolean).join(' ') || 'Especialista';
}

export default function PaymentResultPage() {
  const params = useMemo(() => new URLSearchParams(window.location.search), []);
  const status = MESSAGES[params.get('estado')] ? params.get('estado') : 'error';
  const code = params.get('codigo');
  const message = MESSAGES[status];
  const [appointment, setAppointment] = useState(null);

  useEffect(() => {
    document.title = 'Resultado del pago | Nicolett Studio Fantasy';
    if (status !== 'aprobado' || !code) return undefined;
    let active = true;
    // El detalle solo se muestra si hay una sesión iniciada; si no, basta con el código.
    api.citaPorCodigo(code).then((data) => { if (active) setAppointment(data); }).catch(() => {});
    return () => { active = false; };
  }, [status, code]);

  return <><SiteHeader active="reserva" /><main className="site-section site-booking">
    <section className={`site-booking-panel site-booking-confirmation site-payment-${message.ok ? 'ok' : 'fail'}`}>
      <p className="site-eyebrow">{message.eyebrow}</p>
      <h1 role="status">{message.title}</h1>
      <p>{message.text}</p>
      {(code || appointment) && <dl>
        {code && <div><dt>Código de reserva</dt><dd>{code}</dd></div>}
        {appointment && <>
          <div><dt>Servicio</dt><dd>{appointment.servicio?.nombre}</dd></div>
          <div><dt>Especialista</dt><dd>{workerName(appointment.trabajador)}</dd></div>
          <div><dt>Fecha y hora</dt><dd>{appointment.fechaHoraInicio?.replace('T', ' · ').slice(0, 18)}</dd></div>
          <div><dt>Monto pagado</dt><dd>{money(appointment.precioFinal)}</dd></div>
        </>}
      </dl>}
      <div className="site-booking-actions">
        {message.ok
          ? <a className="site-button" href="/mi-perfil">Ver mis citas</a>
          : <a className="site-button" href="/reservar">Volver a reservar</a>}
        <a className="site-button site-button-outline" href="/">Ir al inicio</a>
      </div>
    </section>
  </main><SiteFooter /></>;
}
