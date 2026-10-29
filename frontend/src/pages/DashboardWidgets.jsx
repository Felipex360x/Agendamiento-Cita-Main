import '../dashboard.css';
import { useState } from 'react';
import { parseKey } from './dashboardStats.js';

const money = new Intl.NumberFormat('es-CL', { style: 'currency', currency: 'CLP', maximumFractionDigits: 0 });
export const formatMoney = (value) => money.format(value || 0);
const axisMoney = (value) => {
  if (value >= 1e6) return `$${(value / 1e6).toFixed(1).replace('.', ',').replace(',0', '')} M`;
  if (value >= 1e3) return `$${Math.round(value / 1e3)} mil`;
  return `$${Math.round(value)}`;
};

export const PERIODS = [
  { days: 7, label: '7 días' },
  { days: 30, label: '30 días' },
  { days: 90, label: '90 días' },
  { days: null, label: 'Todo' },
];

export function bucketLabel(key, granularity, long = false) {
  const date = parseKey(key);
  if (granularity === 'month') return date.toLocaleDateString('es-CL', { month: 'short', year: long ? 'numeric' : '2-digit' });
  const text = date.toLocaleDateString('es-CL', { day: 'numeric', month: 'short' });
  return granularity === 'week' ? `${long ? 'Semana del ' : ''}${text}` : text;
}

export function PeriodFilter({ days, onChange }) {
  return <div className="dash-period" role="group" aria-label="Período">
    {PERIODS.map((period) => <button key={period.label} type="button" aria-pressed={period.days === days} className={period.days === days ? 'active' : ''} onClick={() => onChange(period.days)}>{period.label}</button>)}
  </div>;
}

export function Sparkline({ series }) {
  if (!series || series.length < 2 || series.every((value) => value === 0)) return <span className="dash-spark empty" aria-hidden="true" />;
  const max = Math.max(...series);
  const step = 100 / (series.length - 1);
  const points = series.map((value, index) => `${(index * step).toFixed(2)},${(26 - (value / max) * 24).toFixed(2)}`);
  return <svg className="dash-spark" viewBox="0 0 100 28" preserveAspectRatio="none" aria-hidden="true">
    <polygon points={`0,28 ${points.join(' ')} 100,28`} className="dash-spark-area" />
    <polyline points={points.join(' ')} className="dash-spark-line" vectorEffect="non-scaling-stroke" />
  </svg>;
}

export function KpiCard({ label, value, change, unit = '%', worseWhenUp = false, series, hint }) {
  const direction = change == null ? 'none' : change > 0 ? 'up' : change < 0 ? 'down' : 'flat';
  const good = direction === 'flat' || direction === 'none' ? null : (direction === 'up') !== worseWhenUp;
  const tone = good == null ? 'neutral' : good ? 'good' : 'bad';
  const arrow = direction === 'up' ? '▲' : direction === 'down' ? '▼' : '–';
  return <article className="dash-kpi">
    <small>{label}</small>
    <strong>{value}</strong>
    <div className={`dash-delta ${tone}`}>
      {change == null ? <span>Sin período previo</span> : <><span aria-hidden="true">{arrow}</span><span>{change > 0 ? '+' : ''}{String(change).replace('.', ',')}{unit === '%' ? '%' : ' pp'}</span><em>vs. período anterior</em></>}
    </div>
    {hint && <p className="dash-kpi-hint">{hint}</p>}
    <Sparkline series={series} />
  </article>;
}

export function RevenueChart({ timeline, granularity }) {
  const [active, setActive] = useState(null);
  const max = Math.max(1, ...timeline.map((bucket) => bucket.revenue));
  const hasData = timeline.some((bucket) => bucket.revenue > 0);
  const labelEvery = Math.max(1, Math.ceil(timeline.length / 7));
  const ticks = [1, 0.5, 0];
  const activeBucket = active == null ? null : timeline[active];
  return <div className="dash-chart">
    <div className="dash-chart-body">
      <div className="dash-y" aria-hidden="true">{ticks.map((tick) => <span key={tick}>{tick === 0 ? '0' : hasData ? axisMoney(max * tick) : ''}</span>)}</div>
      <div className="dash-plot" onMouseLeave={() => setActive(null)}>
        <div className="dash-grid" aria-hidden="true"><i /><i /><i /></div>
        <div className="dash-bars" role="group" aria-label={`Ingresos por ${granularity === 'day' ? 'día' : granularity === 'week' ? 'semana' : 'mes'}`}>
          {timeline.map((bucket, index) => <button
            key={bucket.key}
            type="button"
            className={`dash-bar-slot${active === index ? ' active' : ''}`}
            onMouseEnter={() => setActive(index)}
            onFocus={() => setActive(index)}
            onBlur={() => setActive(null)}
            aria-label={`${bucketLabel(bucket.key, granularity, true)}: ${formatMoney(bucket.revenue)}, ${bucket.count} citas`}
          ><span className="dash-bar" style={{ height: `${bucket.revenue ? Math.max(3, (bucket.revenue / max) * 100) : 0}%` }} /></button>)}
        </div>
        {activeBucket && <div className="dash-tooltip" style={{ left: `${((active + 0.5) / timeline.length) * 100}%`, transform: `translate(${active / timeline.length > 0.8 ? '-100%' : active / timeline.length < 0.2 ? '0' : '-50%'}, -8px)` }} role="status">
          <strong>{bucketLabel(activeBucket.key, granularity, true)}</strong>
          <span>{formatMoney(activeBucket.revenue)}</span>
          <small>{activeBucket.count} {activeBucket.count === 1 ? 'cita' : 'citas'}</small>
        </div>}
        {!hasData && <p className="dash-chart-empty">Sin ingresos en este período</p>}
        <div className="dash-x" aria-hidden="true">{timeline.map((bucket, index) => <span key={bucket.key}>{index % labelEvery === 0 ? bucketLabel(bucket.key, granularity) : ''}</span>)}</div>
      </div>
    </div>
    <details className="dash-table-view">
      <summary>Ver como tabla</summary>
      <div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Período</th><th>Citas</th><th>Ingresos</th></tr></thead><tbody>{timeline.map((bucket) => <tr key={bucket.key}><td>{bucketLabel(bucket.key, granularity, true)}</td><td>{bucket.count}</td><td>{formatMoney(bucket.revenue)}</td></tr>)}</tbody></table></div>
    </details>
  </div>;
}

export function StatusBreakdown({ statuses, total }) {
  const visible = statuses.filter((status) => status.count > 0);
  if (!total) return <p className="admin-empty">No hay citas en este período.</p>;
  return <div className="dash-status">
    <div className="dash-stack" role="img" aria-label="Distribución de citas por estado">
      {visible.map((status) => <i key={status.key} className={`st-${status.key.toLowerCase()}`} style={{ flexGrow: status.count }} title={`${status.label}: ${status.count}`} />)}
    </div>
    <ul className="dash-legend">
      {statuses.map((status) => <li key={status.key} className={status.count ? '' : 'zero'}>
        <i className={`st-${status.key.toLowerCase()}`} aria-hidden="true" />
        <span>{status.label}</span>
        <strong>{status.count}</strong>
        <em>{total ? Math.round((status.count / total) * 100) : 0}%</em>
      </li>)}
    </ul>
  </div>;
}

export function RankingList({ items, empty = 'Sin datos en este período.', limit = 6 }) {
  const shown = items.slice(0, limit);
  if (!shown.length) return <p className="admin-empty">{empty}</p>;
  const max = Math.max(1, shown[0].count);
  return <ol className="dash-rank">
    {shown.map((item, index) => <li key={item.id}>
      <span className="dash-rank-pos">{index + 1}</span>
      <div className="dash-rank-main">
        <div className="dash-rank-head"><span>{item.name}</span><strong>{item.count} {item.count === 1 ? 'cita' : 'citas'}</strong></div>
        <div className="dash-rank-track"><i style={{ width: `${Math.max(4, (item.count / max) * 100)}%` }} /></div>
        <small>{formatMoney(item.revenue)} en ingresos</small>
      </div>
    </li>)}
  </ol>;
}

export function DashboardSkeleton() {
  return <div className="dash-skeleton" role="status" aria-label="Cargando resumen">
    <div className="dash-kpis">{[0, 1, 2, 3].map((item) => <i key={item} className="sk sk-kpi" />)}</div>
    <i className="sk sk-chart" />
    <div className="dash-grid-2"><i className="sk sk-block" /><i className="sk sk-block" /></div>
  </div>;
}
