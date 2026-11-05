import { useEffect, useState } from 'react';
import { api } from '../api/client.js';
import { SiteFooter, SiteHeader } from './SiteShell.jsx';

const money = (value) => new Intl.NumberFormat('es-CL', {
  style: 'currency', currency: 'CLP', maximumFractionDigits: 0,
}).format(Number(value) || 0);

const INITIAL_DATA = {
  nombreCliente: 'Nicolás',
  apellidoCliente: 'Acuña',
  correoCliente: 'nacuna888@gmail.com',
  nombreTrabajador: 'Felipe Acuña',
  correoTrabajador: 'acunafelipe272@gmail.com',
  cargoEspecialidad: 'Especialista en Coloración y Estilismo',
  nombreServicio: 'Balayage + Hidratación Profunda',
  duracionMinutos: '90',
  precioServicio: '45000',
  codigoReserva: 'RES-2026-A8F4C2',
  fechaCita: '2026-11-06',
  horaInicio: '15:30',
  horaFin: '17:00',
  notasCliente: 'Prefiero tonos cálidos avellana y tengo el cabello sensible.',
  motivoCancelacion: 'Imprevisto médico del especialista; te invitamos a reagendar en horario preferente.',
  ordenCompra: 'OC-20261103-88421',
  codigoAutorizacion: '1213',
  tipoPago: 'Venta Débito (VD)',
  ultimos4Digitos: '6623',
  fechaTransaccion: '03-11-2026 20:45',
};

export const EMAIL_SCREENS = [
  {
    id: 'bienvenida',
    title: '1. Bienvenida',
    recipientRole: 'Cliente',
    trigger: 'Al registrarse una cuenta nueva (AuthService.registrarCliente)',
    getSubject: (d) => `¡Bienvenida a Nicolett Studio Fantasy, ${d.nombreCliente}!`,
    getTo: (d) => `${d.nombreCliente} ${d.apellidoCliente} <${d.correoCliente}>`,
  },
  {
    id: 'pago',
    title: '2. Pago y Reserva',
    recipientRole: 'Cliente',
    trigger: 'Al agendar y pagar la reserva (BookingPage / Webpay)',
    getSubject: (d) => `Comprobante de pago y solicitud de reserva ${d.codigoReserva} | Nicolett Studio Fantasy`,
    getTo: (d) => `${d.nombreCliente} ${d.apellidoCliente} <${d.correoCliente}>`,
  },
  {
    id: 'trabajador-nueva-cita',
    title: '3. Aviso al Trabajador',
    recipientRole: 'Trabajador',
    trigger: 'Cuando un cliente agenda con el profesional (CitaService.agendar)',
    getSubject: (d) => `Nueva cita agendada contigo (${d.codigoReserva}) — ${d.nombreCliente} ${d.apellidoCliente}`,
    getTo: (d) => `${d.nombreTrabajador} <${d.correoTrabajador}>`,
  },
  {
    id: 'cita-confirmada',
    title: '4. Cita Confirmada',
    recipientRole: 'Cliente',
    trigger: 'Cuando el trabajador confirma la cita (PENDIENTE → CONFIRMADA)',
    getSubject: (d) => `¡Tu cita ${d.codigoReserva} fue confirmada por ${d.nombreTrabajador}!`,
    getTo: (d) => `${d.nombreCliente} ${d.apellidoCliente} <${d.correoCliente}>`,
  },
  {
    id: 'cita-cancelada',
    title: '5. Cita Cancelada',
    recipientRole: 'Cliente',
    trigger: 'Cuando el trabajador o administrador cancela la cita con motivo',
    getSubject: (d) => `Aviso de cancelación de tu reserva ${d.codigoReserva} | Nicolett Studio Fantasy`,
    getTo: (d) => `${d.nombreCliente} ${d.apellidoCliente} <${d.correoCliente}>`,
  },
  {
    id: 'recordatorio',
    title: '6. Recordatorio 24h',
    recipientRole: 'Cliente',
    trigger: '24 horas antes de una cita CONFIRMADA',
    getSubject: (d) => `Recordatorio: mañana tienes tu cita en Nicolett Studio Fantasy (${d.horaInicio} hrs)`,
    getTo: (d) => `${d.nombreCliente} ${d.apellidoCliente} <${d.correoCliente}>`,
  },
];

export default function EmailTemplatesPage() {
  const [selectedId, setSelectedId] = useState('pago');
  const [data, setData] = useState(INITIAL_DATA);
  const [editorOpen, setEditorOpen] = useState(false);
  const [smtpOpen, setSmtpOpen] = useState(false);
  const [inbox, setInbox] = useState([]);
  const [smtpConfigured, setSmtpConfigured] = useState(false);
  const [smtpRemitente, setSmtpRemitente] = useState('nacuna888@gmail.com');
  const [smtpPassword, setSmtpPassword] = useState('');
  const [sendingTest, setSendingTest] = useState(false);
  const [savingSmtp, setSavingSmtp] = useState(false);
  const [notice, setNotice] = useState('');
  const [error, setError] = useState('');
  const [selectedSentEmail, setSelectedSentEmail] = useState(null);

  async function refreshInbox() {
    try {
      const response = await api.correosBandeja();
      if (response) {
        setInbox(response.correos || []);
        setSmtpConfigured(Boolean(response.smtpConfigurado));
        if (response.remitente) setSmtpRemitente(response.remitente);
      }
    } catch {
      // En pruebas unitarias sin backend activo no bloquea el renderizado
    }
  }

  useEffect(() => {
    document.title = 'Pantallas de Correo | Nicolett Studio Fantasy';
    refreshInbox();
  }, []);

  const currentScreen = EMAIL_SCREENS.find((item) => item.id === selectedId) || EMAIL_SCREENS[0];

  function updateField(field, value) {
    setData((prev) => ({ ...prev, [field]: value }));
  }

  async function runFlowTest() {
    setSendingTest(true);
    setError('');
    setNotice('');
    try {
      const parts = (data.nombreTrabajador || 'Felipe Acuña').trim().split(/\s+/);
      const result = await api.probarFlujoCorreos({
        correoCliente: data.correoCliente,
        nombreCliente: data.nombreCliente,
        apellidoCliente: data.apellidoCliente,
        correoTrabajador: data.correoTrabajador,
        nombreTrabajador: parts[0] || 'Felipe',
        apellidoTrabajador: parts.slice(1).join(' ') || 'Acuña',
      });
      await refreshInbox();
      setNotice(
        `Flujo ejecutado para reserva ${result.codigoReserva}: se generaron y procesaron 4 correos (Cliente: ${result.correoCliente} · Trabajador: ${result.correoTrabajador}).`,
      );
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setSendingTest(false);
    }
  }

  async function saveSmtpConfig(event) {
    event.preventDefault();
    setSavingSmtp(true);
    setError('');
    setNotice('');
    try {
      const result = await api.configurarSmtp({
        host: 'smtp.gmail.com',
        port: 587,
        remitente: smtpRemitente,
        appPassword: smtpPassword,
      });
      setSmtpConfigured(Boolean(result.smtpConfigurado));
      setNotice(result.mensaje || 'Configuración SMTP actualizada.');
      setSmtpOpen(false);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setSavingSmtp(false);
    }
  }

  return (
    <>
      <SiteHeader />
      <main className="site-section site-page-section">
        <div className="site-section-heading">
          <div>
            <p className="site-eyebrow">Notificaciones por correo electrónico</p>
            <h1>Pantallas de correo · Nicolett Studio Fantasy</h1>
            <p className="site-muted">
              Diseñadas siguiendo la identidad visual de la pantalla de <strong>Bienvenida</strong> para el cliente (<strong>{data.correoCliente}</strong>) y el profesional (<strong>{data.correoTrabajador}</strong>).
            </p>
          </div>
          <div className="site-hero-actions">
            <button
              type="button"
              className="site-button"
              onClick={runFlowTest}
              disabled={sendingTest}
            >
              {sendingTest ? 'Enviando flujo de prueba…' : 'Probar los 4 correos ahora'}
            </button>
            <button
              type="button"
              className="site-button site-button-outline"
              onClick={() => setSmtpOpen((open) => !open)}
            >
              {smtpConfigured ? 'SMTP Gmail Activo' : 'Configurar Gmail SMTP'}
            </button>
            <button
              type="button"
              className="site-button site-button-outline"
              onClick={() => setEditorOpen((open) => !open)}
            >
              {editorOpen ? 'Ocultar datos' : 'Editar datos'}
            </button>
          </div>
        </div>

        {error && <p className="site-form-error" role="alert">{error}</p>}
        {notice && <p className="admin-alert success" role="status">{notice}</p>}

        {/* Configuración opcional de Contraseña de Aplicación de Gmail */}
        {smtpOpen && (
          <section className="site-booking-panel email-variables-panel" aria-label="Configuración SMTP Gmail">
            <h2>Despacho real a Gmail (smtp.gmail.com:587)</h2>
            <p className="site-muted">
              Para entregar correos en las bandejas reales de Gmail, ingresa la <strong>Contraseña de aplicación de Google (16 letras)</strong> de la cuenta remitente. Sin ella, todos los correos del sistema se generan y registran igualmente en la bandeja en vivo de abajo.
            </p>
            <form className="admin-form-grid" onSubmit={saveSmtpConfig}>
              <label>
                Correo Gmail remitente
                <input
                  type="email"
                  required
                  value={smtpRemitente}
                  onChange={(e) => setSmtpRemitente(e.target.value)}
                />
              </label>
              <label>
                Contraseña de aplicación de Google (16 caracteres)
                <input
                  type="password"
                  placeholder="xxxx xxxx xxxx xxxx"
                  value={smtpPassword}
                  onChange={(e) => setSmtpPassword(e.target.value)}
                />
              </label>
              <button className="site-button" type="submit" disabled={savingSmtp}>
                {savingSmtp ? 'Guardando…' : 'Guardar configuración SMTP'}
              </button>
            </form>
          </section>
        )}

        {/* Selector de pantallas de correo */}
        <div className="email-preview-tabs" role="tablist" aria-label="Pantallas de correo">
          {EMAIL_SCREENS.map((screen) => (
            <button
              key={screen.id}
              type="button"
              role="tab"
              aria-selected={selectedId === screen.id}
              className={`email-preview-tab ${selectedId === screen.id ? 'active' : ''}`}
              onClick={() => setSelectedId(screen.id)}
            >
              <strong>{screen.title}</strong>
              <small>Para: {screen.recipientRole}</small>
            </button>
          ))}
        </div>

        {/* Editor opcional de variables */}
        {editorOpen && (
          <section className="site-booking-panel email-variables-panel" aria-label="Variables de plantilla">
            <h2>Datos de prueba en vivo</h2>
            <div className="admin-form-grid">
              <label>Nombre clienta<input value={data.nombreCliente} onChange={(e) => updateField('nombreCliente', e.target.value)} /></label>
              <label>Apellido clienta<input value={data.apellidoCliente} onChange={(e) => updateField('apellidoCliente', e.target.value)} /></label>
              <label>Correo clienta<input value={data.correoCliente} onChange={(e) => updateField('correoCliente', e.target.value)} /></label>
              <label>Especialista<input value={data.nombreTrabajador} onChange={(e) => updateField('nombreTrabajador', e.target.value)} /></label>
              <label>Correo especialista<input value={data.correoTrabajador} onChange={(e) => updateField('correoTrabajador', e.target.value)} /></label>
              <label>Servicio<input value={data.nombreServicio} onChange={(e) => updateField('nombreServicio', e.target.value)} /></label>
              <label>Monto pagado (CLP)<input type="number" value={data.precioServicio} onChange={(e) => updateField('precioServicio', e.target.value)} /></label>
              <label>Código de reserva<input value={data.codigoReserva} onChange={(e) => updateField('codigoReserva', e.target.value)} /></label>
              <label>Fecha cita<input type="date" value={data.fechaCita} onChange={(e) => updateField('fechaCita', e.target.value)} /></label>
              <label>Hora inicio<input type="time" value={data.horaInicio} onChange={(e) => updateField('horaInicio', e.target.value)} /></label>
              <label className="wide">Notas de la clienta<input value={data.notasCliente} onChange={(e) => updateField('notasCliente', e.target.value)} /></label>
              <label className="wide">Motivo de cancelación<input value={data.motivoCancelacion} onChange={(e) => updateField('motivoCancelacion', e.target.value)} /></label>
            </div>
          </section>
        )}

        {/* Cabecera de metadatos del correo (Sobre / Envelope) */}
        <section className="email-envelope-bar" aria-label="Encabezado del correo">
          <div>
            <span>De:</span>
            <strong>Nicolett Studio Fantasy &lt;{smtpRemitente}&gt;</strong>
          </div>
          <div>
            <span>Para:</span>
            <strong>{currentScreen.getTo(data)}</strong>
          </div>
          <div>
            <span>Asunto:</span>
            <strong>{currentScreen.getSubject(data)}</strong>
          </div>
          <div>
            <span>Disparador:</span>
            <span className="site-profile-pill site-profile-pill-gold">{currentScreen.trigger}</span>
          </div>
        </section>

        {/* Contenedor visual del correo siguiendo el diseño de Bienvenida (site-auth-card) */}
        <div className="email-canvas">
          <article className="site-auth-card email-card" aria-label={`Vista previa: ${currentScreen.title}`}>
            {/* Encabezado de marca idéntico al estilo del sitio */}
            <header className="email-card-header">
              <span className="site-brand">
                nicolet<span>.</span>
                <small>
                  {selectedId === 'trabajador-nueva-cita'
                    ? 'STUDIO FANTASY · PORTAL PROFESIONAL'
                    : 'STUDIO FANTASY'}
                </small>
              </span>
            </header>

            {/* 1. PANTALLA DE BIENVENIDA */}
            {selectedId === 'bienvenida' && (
              <div className="email-card-body">
                <p className="site-eyebrow">Cuenta creada con éxito</p>
                <h1>¡Bienvenida, {data.nombreCliente}!</h1>
                <p className="site-muted">
                  ¡Qué gusto tenerte aquí! Tu cuenta en <strong>Nicolett Studio Fantasy</strong> ya está activa. Ahora puedes reservar tus tratamientos favoritos y consultar tus próximas visitas desde tu perfil.
                </p>

                <dl className="email-details-list">
                  <div>
                    <dt>Clienta</dt>
                    <dd>{data.nombreCliente} {data.apellidoCliente}</dd>
                  </div>
                  <div>
                    <dt>Correo registrado</dt>
                    <dd>{data.correoCliente}</dd>
                  </div>
                  <div>
                    <dt>Tipo de cuenta</dt>
                    <dd><span className="site-profile-pill site-profile-pill-gold">Cliente Nicolett</span></dd>
                  </div>
                </dl>

                <div className="site-session-banner">
                  <strong>Tu espacio personal está listo</strong>
                  <span>Agenda en pocos pasos eligiendo tu servicio, especialista, fecha y hora disponible.</span>
                </div>

                <a className="site-button site-submit" href="/reservar">
                  Reservar mi primera hora
                </a>
              </div>
            )}

            {/* 2. PANTALLA DE PAGO Y RESERVA RECIBIDA */}
            {selectedId === 'pago' && (
              <div className="email-card-body">
                <p className="site-eyebrow">Comprobante de pago y reserva recibida</p>
                <h1>¡Gracias por tu pago, {data.nombreCliente}!</h1>
                <p className="site-muted">
                  Hemos recibido tu pago y agendado tu hora. Tu especialista revisará la solicitud y te enviaremos un correo apenas quede <strong>confirmada</strong>.
                </p>

                <p className="site-eyebrow email-sub-eyebrow">Detalle de tu cita</p>
                <dl className="email-details-list">
                  <div>
                    <dt>Código de reserva</dt>
                    <dd>{data.codigoReserva}</dd>
                  </div>
                  <div>
                    <dt>Servicio</dt>
                    <dd>{data.nombreServicio} ({data.duracionMinutos} min)</dd>
                  </div>
                  <div>
                    <dt>Especialista</dt>
                    <dd>{data.nombreTrabajador}</dd>
                  </div>
                  <div>
                    <dt>Fecha y hora</dt>
                    <dd>{data.fechaCita} · {data.horaInicio}</dd>
                  </div>
                  <div>
                    <dt>Estado de la cita</dt>
                    <dd><span className="admin-status pendiente">PENDIENTE DE CONFIRMACIÓN</span></dd>
                  </div>
                </dl>

                <div className="email-voucher-box">
                  <p className="site-eyebrow">Comprobante de transacción (Webpay)</p>
                  <dl className="email-details-list compact">
                    <div>
                      <dt>Comercio</dt>
                      <dd>Nicolett Studio Fantasy</dd>
                    </div>
                    <div>
                      <dt>Orden de compra</dt>
                      <dd>{data.ordenCompra}</dd>
                    </div>
                    <div>
                      <dt>Código de autorización</dt>
                      <dd>{data.codigoAutorizacion}</dd>
                    </div>
                    <div>
                      <dt>Medio de pago / Tarjeta</dt>
                      <dd>{data.tipoPago} · **** {data.ultimos4Digitos}</dd>
                    </div>
                    <div>
                      <dt>Fecha de pago</dt>
                      <dd>{data.fechaTransaccion}</dd>
                    </div>
                  </dl>
                  <div className="site-booking-total email-voucher-total">
                    <span>Monto total pagado</span>
                    <strong>{money(data.precioServicio)}</strong>
                  </div>
                </div>

                <a className="site-button site-submit" href="/mi-perfil">
                  Ver mi reserva en Mi Perfil
                </a>
              </div>
            )}

            {/* 3. PANTALLA DEL TRABAJADOR CUANDO AGENDAN CON ÉL */}
            {selectedId === 'trabajador-nueva-cita' && (
              <div className="email-card-body">
                <p className="site-eyebrow">Nueva solicitud en tu agenda</p>
                <h1>Hola, {data.nombreTrabajador}: agendaron una hora contigo</h1>
                <p className="site-muted">
                  Una clienta acaba de reservar una atención contigo. Revisa los datos de la cita e ingresa a tu portal profesional para <strong>confirmar</strong> o gestionar la hora.
                </p>

                <dl className="email-details-list">
                  <div>
                    <dt>Código de reserva</dt>
                    <dd>{data.codigoReserva}</dd>
                  </div>
                  <div>
                    <dt>Clienta</dt>
                    <dd>{data.nombreCliente} {data.apellidoCliente}</dd>
                  </div>
                  <div>
                    <dt>Correo clienta</dt>
                    <dd>{data.correoCliente}</dd>
                  </div>
                  <div>
                    <dt>Servicio solicitado</dt>
                    <dd>{data.nombreServicio} ({data.duracionMinutos} min)</dd>
                  </div>
                  <div>
                    <dt>Fecha y bloque horario</dt>
                    <dd>{data.fechaCita} · {data.horaInicio} – {data.horaFin}</dd>
                  </div>
                  <div>
                    <dt>Estado actual</dt>
                    <dd><span className="admin-status pendiente">PENDIENTE DE TU CONFIRMACIÓN</span></dd>
                  </div>
                </dl>

                <div className="site-session-banner">
                  <strong>Notas de la clienta para ti:</strong>
                  <span>{data.notasCliente || 'Sin observaciones adicionales.'}</span>
                </div>

                <a className="site-button site-submit" href="/admin/panel-trabajador">
                  Ir a Mi Panel y Confirmar Cita
                </a>
              </div>
            )}

            {/* 4. PANTALLA AL USUARIO CUANDO EL TRABAJADOR CONFIRMA SU CITA */}
            {selectedId === 'cita-confirmada' && (
              <div className="email-card-body">
                <p className="site-eyebrow">Hora asegurada</p>
                <h1>¡Tu cita está confirmada, {data.nombreCliente}!</h1>
                <p className="site-muted">
                  Tu especialista <strong>{data.nombreTrabajador}</strong> ha confirmado tu reserva. Te esperamos en nuestro estudio para brindarte un cuidado pensado especialmente para ti.
                </p>

                <dl className="email-details-list">
                  <div>
                    <dt>Estado de tu cita</dt>
                    <dd><span className="admin-status confirmada">CONFIRMADA</span></dd>
                  </div>
                  <div>
                    <dt>Código de reserva</dt>
                    <dd>{data.codigoReserva}</dd>
                  </div>
                  <div>
                    <dt>Servicio</dt>
                    <dd>{data.nombreServicio} ({data.duracionMinutos} min)</dd>
                  </div>
                  <div>
                    <dt>Especialista</dt>
                    <dd>{data.nombreTrabajador} · {data.cargoEspecialidad}</dd>
                  </div>
                  <div>
                    <dt>Fecha y hora</dt>
                    <dd>{data.fechaCita} · {data.horaInicio}</dd>
                  </div>
                  <div>
                    <dt>Ubicación</dt>
                    <dd>Los Ciruelos, Buin</dd>
                  </div>
                </dl>

                <div className="site-session-banner">
                  <strong>Recomendaciones para tu visita</strong>
                  <span>
                    Te sugerimos llegar 5 minutos antes de tu hora. Si necesitas comunicarte con nosotros, escríbenos al WhatsApp +56 9 4032 2551 indicando tu código {data.codigoReserva}.
                  </span>
                </div>

                <a className="site-button site-submit" href="/mi-perfil">
                  Ver mis reservas en Mi Perfil
                </a>
              </div>
            )}

            {/* 5. PANTALLA DE CITA CANCELADA CON MOTIVO */}
            {selectedId === 'cita-cancelada' && (
              <div className="email-card-body">
                <p className="site-eyebrow">Actualización de tu reserva</p>
                <h1>Tu cita ha sido cancelada, {data.nombreCliente}</h1>
                <p className="site-muted">
                  Te informamos que la reserva <strong>{data.codigoReserva}</strong> fue cancelada. Puedes elegir una nueva fecha y hora disponible cuando quieras desde nuestra plataforma.
                </p>

                <dl className="email-details-list">
                  <div>
                    <dt>Estado</dt>
                    <dd><span className="admin-status cancelada">CANCELADA</span></dd>
                  </div>
                  <div>
                    <dt>Servicio</dt>
                    <dd>{data.nombreServicio}</dd>
                  </div>
                  <div>
                    <dt>Especialista</dt>
                    <dd>{data.nombreTrabajador}</dd>
                  </div>
                  <div>
                    <dt>Fecha original</dt>
                    <dd>{data.fechaCita} · {data.horaInicio}</dd>
                  </div>
                </dl>

                <div className="site-form-error" role="note">
                  <strong>Motivo registrado:</strong> {data.motivoCancelacion}
                </div>

                <a className="site-button site-submit" href="/reservar">
                  Agendar una nueva hora
                </a>
              </div>
            )}

            {/* 6. PANTALLA DE RECORDATORIO 24H ANTES */}
            {selectedId === 'recordatorio' && (
              <div className="email-card-body">
                <p className="site-eyebrow">Recordatorio 24 horas antes</p>
                <h1>¡Te esperamos mañana, {data.nombreCliente}!</h1>
                <p className="site-muted">
                  Te recordamos que mañana tienes una hora agendada y confirmada en <strong>Nicolett Studio Fantasy</strong>.
                </p>

                <dl className="email-details-list">
                  <div>
                    <dt>Código de reserva</dt>
                    <dd>{data.codigoReserva}</dd>
                  </div>
                  <div>
                    <dt>Servicio</dt>
                    <dd>{data.nombreServicio} ({data.duracionMinutos} min)</dd>
                  </div>
                  <div>
                    <dt>Especialista</dt>
                    <dd>{data.nombreTrabajador}</dd>
                  </div>
                  <div>
                    <dt>Fecha y hora</dt>
                    <dd>{data.fechaCita} · {data.horaInicio}</dd>
                  </div>
                  <div>
                    <dt>Dirección</dt>
                    <dd>Los Ciruelos, Buin</dd>
                  </div>
                </dl>

                <a className="site-button site-submit" href="/mi-perfil">
                  Ver mi cita en Mi Perfil
                </a>
              </div>
            )}

            {/* Pie de correo unificado */}
            <footer className="email-card-footer">
              <strong>Nicolett Studio Fantasy</strong> · Un espacio para ti. Un cuidado que se siente.
              <span>Los Ciruelos, Buin · WhatsApp +56 9 4032 2551 · @nicolettstudiofantasy</span>
            </footer>
          </article>
        </div>

        {/* Bandeja de Correos Enviados / Generados por el Backend en Tiempo Real */}
        <section className="site-booking-panel" aria-label="Bandeja de correos enviados">
          <div className="admin-section-title">
            <div>
              <p className="site-eyebrow">Registro en tiempo real del servidor</p>
              <h2>Bandeja de correos despachados ({inbox.length})</h2>
            </div>
            <button type="button" className="site-button site-button-outline" onClick={refreshInbox}>
              Actualizar bandeja
            </button>
          </div>
          {inbox.length ? (
            <div className="admin-table-wrap">
              <table className="admin-table">
                <thead>
                  <tr>
                    <th>Fecha / Hora</th>
                    <th>Pantalla / Tipo</th>
                    <th>Para</th>
                    <th>Asunto</th>
                    <th>Reserva</th>
                    <th>Estado</th>
                    <th>Acción</th>
                  </tr>
                </thead>
                <tbody>
                  {inbox.map((item) => (
                    <tr key={item.id}>
                      <td>{item.fechaHora}</td>
                      <td><strong>{item.tipo}</strong></td>
                      <td>{item.nombreDestinatario} &lt;{item.destinatario}&gt;</td>
                      <td>{item.asunto}</td>
                      <td>{item.codigoReserva || '—'}</td>
                      <td>
                        <span className={`admin-status ${item.estadoEnvio === 'ENVIADO_SMTP' ? 'confirmada' : item.estadoEnvio === 'ERROR_SMTP' ? 'cancelada' : 'pendiente'}`}>
                          {item.estadoEnvio}
                        </span>
                      </td>
                      <td className="admin-row-actions">
                        <button type="button" onClick={() => setSelectedSentEmail(item)}>
                          Ver correo
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <p className="admin-empty">
              Aún no se han despachado correos en esta sesión. Haz clic en «Probar los 4 correos ahora» o realiza una reserva real para verlos aquí.
            </p>
          )}
        </section>

        {selectedSentEmail && (
          <div className="admin-modal-backdrop" onClick={() => setSelectedSentEmail(null)}>
            <section
              className="admin-modal"
              style={{ width: 'min(100%, 660px)', maxHeight: '90vh', overflow: 'auto' }}
              role="dialog"
              aria-modal="true"
              aria-label={`Correo enviado: ${selectedSentEmail.asunto}`}
              onClick={(e) => e.stopPropagation()}
            >
              <button type="button" className="admin-modal-close" onClick={() => setSelectedSentEmail(null)}>
                Cerrar
              </button>
              <p className="site-eyebrow">{selectedSentEmail.tipo} · {selectedSentEmail.fechaHora}</p>
              <h2>{selectedSentEmail.asunto}</h2>
              <p className="site-muted">
                <strong>Para:</strong> {selectedSentEmail.nombreDestinatario} &lt;{selectedSentEmail.destinatario}&gt; · <strong>Detalle:</strong> {selectedSentEmail.detalleEnvio}
              </p>
              <iframe
                title={`Contenido HTML ${selectedSentEmail.id}`}
                srcDoc={selectedSentEmail.html}
                style={{ width: '100%', height: '620px', border: '1px solid #ede4d9', borderRadius: '16px', background: '#fcf9f5' }}
              />
            </section>
          </div>
        )}
      </main>
      <SiteFooter />
    </>
  );
}
