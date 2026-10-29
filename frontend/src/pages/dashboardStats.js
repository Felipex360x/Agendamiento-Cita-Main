export const STATUS_ORDER = [
  ['PENDIENTE', 'Pendiente'],
  ['CONFIRMADA', 'Confirmada'],
  ['EN_ATENCION', 'En atención'],
  ['COMPLETADA', 'Completada'],
  ['CANCELADA', 'Cancelada'],
  ['NO_ASISTIO', 'No asistió'],
];

const INACTIVE = new Set(['CANCELADA', 'NO_ASISTIO']);

export const dayKey = (date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
export const parseKey = (key) => new Date(`${key}T12:00:00`);

function addDays(key, amount) {
  const date = parseKey(key);
  date.setDate(date.getDate() + amount);
  return dayKey(date);
}

function diffDays(fromKey, toKey) {
  return Math.round((parseKey(toKey) - parseKey(fromKey)) / 86400000);
}

function mondayKey(key) {
  const date = parseKey(key);
  date.setDate(date.getDate() - ((date.getDay() + 6) % 7));
  return dayKey(date);
}

const bucketKeyFor = (key, granularity) => {
  if (granularity === 'week') return mondayKey(key);
  if (granularity === 'month') return `${key.slice(0, 7)}-01`;
  return key;
};

const personLabel = (person) => [person?.usuario?.nombre, person?.usuario?.apellidoP].filter(Boolean).join(' ') || 'Sin asignar';
const priceOf = (appointment) => Number(appointment.precioFinal || 0);
const sum = (list, pick) => list.reduce((total, item) => total + pick(item), 0);

function periodTotals(list) {
  const completed = list.filter((item) => item.estado === 'COMPLETADA');
  return {
    revenue: sum(completed, priceOf),
    appointments: list.length,
    clients: new Set(completed.map((item) => item.cliente?.id).filter((id) => id != null)).size,
    cancelRate: list.length ? Math.round((list.filter((item) => item.estado === 'CANCELADA').length / list.length) * 1000) / 10 : 0,
  };
}

const percentChange = (value, previous) => (previous > 0 ? Math.round(((value - previous) / previous) * 100) : null);

function rank(list, idOf, nameOf) {
  const groups = new Map();
  list.forEach((item) => {
    const id = idOf(item);
    if (id == null || INACTIVE.has(item.estado)) return;
    const group = groups.get(id) || { id, name: nameOf(item), count: 0, revenue: 0 };
    group.count += 1;
    if (item.estado === 'COMPLETADA') group.revenue += priceOf(item);
    groups.set(id, group);
  });
  return [...groups.values()].sort((a, b) => b.count - a.count || b.revenue - a.revenue || a.name.localeCompare(b.name, 'es'));
}

/**
 * Resume las citas para el dashboard. `days` es el tamaño del período en días
 * (null = todo el historial). Los ingresos cuentan solo citas COMPLETADAS.
 */
export function computeDashboard(appointments, { days = 30, today = new Date() } = {}) {
  const valid = (appointments || []).filter((item) => item?.fechaHoraInicio);
  const withKey = valid.map((item) => ({ ...item, _day: item.fechaHoraInicio.slice(0, 10) }));
  const todayKey = dayKey(today);

  let start;
  let end;
  if (days) {
    end = todayKey;
    start = addDays(end, -(days - 1));
  } else {
    const keys = withKey.map((item) => item._day).sort();
    start = keys.length && keys[0] < todayKey ? keys[0] : todayKey;
    end = keys.length && keys.at(-1) > todayKey ? keys.at(-1) : todayKey;
  }
  const inRange = (item, from, to) => item._day >= from && item._day <= to;
  const current = withKey.filter((item) => inRange(item, start, end));
  const previousList = days ? withKey.filter((item) => inRange(item, addDays(start, -days), addDays(start, -1))) : [];

  const span = diffDays(start, end) + 1;
  const granularity = span <= 31 ? 'day' : span <= 120 ? 'week' : 'month';

  const buckets = new Map();
  for (let offset = 0; offset < span; offset += 1) {
    const key = bucketKeyFor(addDays(start, offset), granularity);
    if (!buckets.has(key)) buckets.set(key, { key, revenue: 0, count: 0, cancelled: 0, clientIds: new Set() });
  }
  current.forEach((item) => {
    const bucket = buckets.get(bucketKeyFor(item._day, granularity));
    if (!bucket) return;
    bucket.count += 1;
    if (item.estado === 'CANCELADA') bucket.cancelled += 1;
    if (item.estado === 'COMPLETADA') {
      bucket.revenue += priceOf(item);
      if (item.cliente?.id != null) bucket.clientIds.add(item.cliente.id);
    }
  });
  const timeline = [...buckets.values()].map(({ clientIds, ...bucket }) => ({ ...bucket, clients: clientIds.size }));

  const totals = periodTotals(current);
  const before = periodTotals(previousList);
  const hasBefore = previousList.length > 0;
  const kpi = (name, series, change) => ({ value: totals[name], previous: before[name], change, series });
  const kpis = {
    revenue: kpi('revenue', timeline.map((b) => b.revenue), percentChange(totals.revenue, before.revenue)),
    appointments: kpi('appointments', timeline.map((b) => b.count), percentChange(totals.appointments, before.appointments)),
    clients: kpi('clients', timeline.map((b) => b.clients), percentChange(totals.clients, before.clients)),
    cancelRate: kpi('cancelRate', timeline.map((b) => (b.count ? Math.round((b.cancelled / b.count) * 100) : 0)), hasBefore ? Math.round((totals.cancelRate - before.cancelRate) * 10) / 10 : null),
  };

  const statuses = STATUS_ORDER.map(([key, label]) => ({ key, label, count: current.filter((item) => item.estado === key).length }));

  const tomorrowKey = addDays(todayKey, 1);
  const upcoming = withKey
    .filter((item) => (item._day === todayKey || item._day === tomorrowKey) && ['PENDIENTE', 'CONFIRMADA', 'EN_ATENCION'].includes(item.estado))
    .sort((a, b) => a.fechaHoraInicio.localeCompare(b.fechaHoraInicio))
    .map(({ _day, ...item }) => item);

  return {
    range: { start, end, days },
    granularity,
    kpis,
    timeline,
    statuses,
    byWorker: rank(current, (item) => item.trabajador?.id, (item) => personLabel(item.trabajador)),
    byService: rank(current, (item) => item.servicio?.id, (item) => item.servicio?.nombre || 'Sin servicio'),
    upcoming,
    total: current.length,
  };
}
