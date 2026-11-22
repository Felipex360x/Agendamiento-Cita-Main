import { useEffect, useMemo, useState } from 'react';
import { api } from '../api/client.js';
import { AdminShell, EmptyState, RequestMessage } from '../layouts/AdminShell.jsx';
import {
  PAYMENT_STATES, filterPayments, paymentStateLabel, paymentStateTone, paymentsToCsv, summarizePayments,
} from '../utils/paymentHistory.js';

const money = (value) => new Intl.NumberFormat('es-CL', { style: 'currency', currency: 'CLP', maximumFractionDigits: 0 }).format(Number(value) || 0);
const dateTime = (value) => (value ? value.replace('T', ' ').slice(0, 16) : '—');

export default function AdminPaymentsPage() {
  const [payments, setPayments] = useState([]);
  const [truncated, setTruncated] = useState(false);
  const [state, setState] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selected, setSelected] = useState(null);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    document.title = 'Historial de pagos | Nicolett Studio Fantasy';
  }, []);

  useEffect(() => {
    if (from && to && from > to) { setError('La fecha "Desde" no puede ser posterior a "Hasta".'); setPayments([]); setLoading(false); return undefined; }
    let active = true;
    setLoading(true);
    setError('');
    api.historialPagos({ estado: state, desde: from, hasta: to })
      .then((response) => { if (active) { setPayments(response?.pagos || []); setTruncated(Boolean(response?.truncado)); } })
      .catch((requestError) => { if (active) { setPayments([]); setError(requestError.message); } })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [state, from, to, reloadKey]);

  const visible = useMemo(() => filterPayments(payments, query), [payments, query]);
  const totals = useMemo(() => summarizePayments(visible), [visible]);

  function downloadCsv() {
    const blob = new Blob([`\uFEFF${paymentsToCsv(visible)}`], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `historial-pagos-${new Date().toISOString().slice(0, 10)}.csv`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
  }

  return <AdminShell title="Historial de pagos" active="/admin/pagos">
    <RequestMessage error={error} />
    <section className="admin-panel admin-form-grid">
      <label>Estado del pago<select value={state} onChange={(event) => setState(event.target.value)}><option value="">Todos</option>{Object.entries(PAYMENT_STATES).map(([value, item]) => <option key={value} value={value}>{item.label}</option>)}</select></label>
      <label>Desde<input type="date" value={from} onChange={(event) => setFrom(event.target.value)} /></label>
      <label>Hasta<input type="date" value={to} onChange={(event) => setTo(event.target.value)} /></label>
      <label>Buscar<input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Reserva, clienta, correo, servicio u orden" /></label>
    </section>
    <div className="admin-metrics admin-metrics-4">
      <article title="Suma de los pagos aprobados por Webpay"><small>Total cobrado</small><strong>{money(totals.collected)}</strong></article>
      <article><small>Pagos aprobados</small><strong>{totals.approved}</strong></article>
      <article><small>Rechazados</small><strong>{totals.rejected}</strong></article>
      <article title="Anulados por la clienta, vencidos o con error de Webpay"><small>No completados</small><strong>{totals.notCompleted}</strong></article>
    </div>
    <section className="admin-panel">
      <div className="admin-panel-heading">
        <h2>Pagos</h2>
        <div className="admin-panel-actions">
          <button type="button" className="site-button site-button-outline" onClick={() => setReloadKey((key) => key + 1)} disabled={loading}>Actualizar</button>
          <button type="button" className="site-button site-button-outline" onClick={downloadCsv} disabled={!visible.length}>Descargar CSV</button>
        </div>
      </div>
      {truncated && <p className="admin-alert" role="status">Hay más pagos que los mostrados: se ven los 1.000 más recientes. Acota las fechas para ver el resto.</p>}
      {totals.inProgress > 0 && <p className="site-muted">{totals.inProgress} pago(s) esperando que la clienta termine en Webpay; no se suman al total cobrado.</p>}
      <EmptyState loading={loading} error="" empty={!visible.length}>
        <div className="admin-table-wrap"><table className="admin-table">
          <thead><tr><th>Fecha</th><th>Reserva</th><th>Clienta</th><th>Servicio</th><th>Estado</th><th>Monto</th><th></th></tr></thead>
          <tbody>{visible.map((payment) => <tr key={payment.id}>
            <td>{dateTime(payment.creadoEn)}</td>
            <td><strong>{payment.codigoReserva}</strong></td>
            <td>{payment.clienteNombre}<br /><small className="site-muted">{payment.clienteCorreo}</small></td>
            <td>{payment.servicio}<br /><small className="site-muted">{payment.profesional}</small></td>
            <td><span className={`admin-status ${paymentStateTone(payment.estado)}`}>{paymentStateLabel(payment.estado)}</span></td>
            <td>{money(payment.monto)}</td>
            <td className="admin-row-actions"><button type="button" className="site-text-button" onClick={() => setSelected(payment)}>Ver detalle</button></td>
          </tr>)}</tbody>
        </table></div>
      </EmptyState>
    </section>
    {selected && <div className="admin-modal-backdrop" role="presentation" onClick={() => setSelected(null)}>
      <section className="admin-modal" role="dialog" aria-modal="true" aria-labelledby="payment-modal-title" onClick={(event) => event.stopPropagation()}>
        <button type="button" className="admin-modal-close" onClick={() => setSelected(null)}>Cerrar</button>
        <p className="site-eyebrow">Detalle del pago</p>
        <h2 id="payment-modal-title">{selected.codigoReserva}</h2>
        <p><span className={`admin-status ${paymentStateTone(selected.estado)}`}>{paymentStateLabel(selected.estado)}</span> · {money(selected.monto)}</p>
        <p><strong>Clienta:</strong> {selected.clienteNombre} ({selected.clienteCorreo})</p>
        <p><strong>Servicio:</strong> {selected.servicio} con {selected.profesional}</p>
        <p><strong>Fecha de la cita:</strong> {dateTime(selected.fechaCita)} · estado de la cita: {selected.estadoCita}</p>
        <p><strong>Pago iniciado:</strong> {dateTime(selected.creadoEn)}{selected.actualizadoEn && <> · <strong>Resuelto:</strong> {dateTime(selected.actualizadoEn)}</>}</p>
        <p><strong>Orden de compra:</strong> {selected.ordenCompra}</p>
        {selected.estado === 'APROBADO'
          ? <><p><strong>Código de autorización:</strong> {selected.codigoAutorizacion || '—'}</p><p><strong>Medio de pago:</strong> {selected.tipoPago || '—'} · **** {selected.ultimos4Digitos || '----'}</p></>
          : <p className="site-muted">Sin cobro: no se hizo ningún cargo a la tarjeta.</p>}
      </section>
    </div>}
  </AdminShell>;
}
