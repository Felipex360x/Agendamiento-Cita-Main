import { describe, expect, it } from 'vitest';
import { computeDashboard } from '../pages/dashboardStats.js';

const TODAY = new Date(2026, 9, 25, 12); // 25-oct-2026

const worker = (id, nombre) => ({ id, usuario: { nombre, apellidoP: 'X' } });
const service = (id, nombre) => ({ id, nombre });
let seq = 0;
const appt = (day, estado, { price = 10000, client = 1, w = worker(1, 'Ana'), s = service(1, 'Corte'), time = '10:00' } = {}) => ({
  id: ++seq,
  fechaHoraInicio: `${day}T${time}:00`,
  estado,
  precioFinal: price,
  cliente: { id: client },
  trabajador: w,
  servicio: s,
});

describe('computeDashboard', () => {
  it('cuenta ingresos solo de citas COMPLETADAS dentro del período', () => {
    const data = [
      appt('2026-10-20', 'COMPLETADA', { price: 20000 }),
      appt('2026-10-21', 'CONFIRMADA', { price: 99999 }),
      appt('2026-09-01', 'COMPLETADA', { price: 50000 }), // fuera de 30 días
    ];
    const stats = computeDashboard(data, { days: 30, today: TODAY });
    expect(stats.kpis.revenue.value).toBe(20000);
    expect(stats.kpis.appointments.value).toBe(2);
  });

  it('calcula la variación contra el período anterior', () => {
    const data = [
      appt('2026-10-20', 'COMPLETADA', { price: 30000 }),
      appt('2026-09-20', 'COMPLETADA', { price: 20000 }), // período anterior (31-60 días atrás)
    ];
    const stats = computeDashboard(data, { days: 30, today: TODAY });
    expect(stats.kpis.revenue.previous).toBe(20000);
    expect(stats.kpis.revenue.change).toBe(50);
  });

  it('devuelve change null cuando no hay base de comparación', () => {
    const stats = computeDashboard([appt('2026-10-20', 'COMPLETADA')], { days: 30, today: TODAY });
    expect(stats.kpis.revenue.change).toBeNull();
  });

  it('con days=null abarca todo el historial y no compara', () => {
    const data = [appt('2025-01-10', 'COMPLETADA', { price: 1000 }), appt('2026-10-20', 'COMPLETADA', { price: 2000 })];
    const stats = computeDashboard(data, { days: null, today: TODAY });
    expect(stats.kpis.revenue.value).toBe(3000);
    expect(stats.kpis.revenue.change).toBeNull();
  });

  it('calcula clientas únicas atendidas y la tasa de cancelación', () => {
    const data = [
      appt('2026-10-20', 'COMPLETADA', { client: 1 }),
      appt('2026-10-21', 'COMPLETADA', { client: 1 }),
      appt('2026-10-22', 'COMPLETADA', { client: 2 }),
      appt('2026-10-23', 'CANCELADA', { client: 3 }),
    ];
    const stats = computeDashboard(data, { days: 30, today: TODAY });
    expect(stats.kpis.clients.value).toBe(2);
    expect(stats.kpis.cancelRate.value).toBe(25);
  });

  it('tasa de cancelación es 0 sin citas', () => {
    expect(computeDashboard([], { days: 30, today: TODAY }).kpis.cancelRate.value).toBe(0);
  });

  it('agrupa por día con huecos en cero para períodos cortos', () => {
    const data = [appt('2026-10-24', 'COMPLETADA', { price: 5000 }), appt('2026-10-24', 'COMPLETADA', { price: 7000 })];
    const stats = computeDashboard(data, { days: 7, today: TODAY });
    expect(stats.timeline).toHaveLength(7);
    expect(stats.timeline.at(-1).key).toBe('2026-10-25');
    const day = stats.timeline.find((bucket) => bucket.key === '2026-10-24');
    expect(day).toMatchObject({ revenue: 12000, count: 2 });
    expect(stats.timeline[0]).toMatchObject({ revenue: 0, count: 0 });
  });

  it('agrupa por semana en períodos largos', () => {
    const stats = computeDashboard([appt('2026-10-20', 'COMPLETADA')], { days: 90, today: TODAY });
    expect(stats.granularity).toBe('week');
    expect(stats.timeline.length).toBeLessThanOrEqual(14);
  });

  it('distribuye por estado, incluyendo todos los estados', () => {
    const data = [appt('2026-10-20', 'PENDIENTE'), appt('2026-10-20', 'NO_ASISTIO'), appt('2026-10-20', 'EN_ATENCION'), appt('2026-10-20', 'CONFIRMADA')];
    const stats = computeDashboard(data, { days: 30, today: TODAY });
    const byKey = Object.fromEntries(stats.statuses.map((status) => [status.key, status.count]));
    expect(byKey).toMatchObject({ PENDIENTE: 1, CONFIRMADA: 1, EN_ATENCION: 1, NO_ASISTIO: 1, COMPLETADA: 0, CANCELADA: 0 });
  });

  it('arma rankings por profesional y servicio ordenados por citas', () => {
    const ana = worker(1, 'Ana');
    const bea = worker(2, 'Bea');
    const data = [
      appt('2026-10-20', 'COMPLETADA', { w: bea, price: 8000, s: service(2, 'Manicure') }),
      appt('2026-10-21', 'COMPLETADA', { w: bea, price: 8000, s: service(2, 'Manicure') }),
      appt('2026-10-22', 'COMPLETADA', { w: ana, price: 5000 }),
      appt('2026-10-23', 'CANCELADA', { w: ana, price: 5000 }),
    ];
    const stats = computeDashboard(data, { days: 30, today: TODAY });
    expect(stats.byWorker[0]).toMatchObject({ name: 'Bea X', count: 2, revenue: 16000 });
    expect(stats.byWorker[1]).toMatchObject({ name: 'Ana X', count: 1, revenue: 5000 });
    expect(stats.byService[0]).toMatchObject({ name: 'Manicure', count: 2 });
  });

  it('lista las citas de hoy y mañana sin canceladas, ordenadas por hora', () => {
    const data = [
      appt('2026-10-26', 'CONFIRMADA', { time: '09:00' }),
      appt('2026-10-25', 'PENDIENTE', { time: '15:00' }),
      appt('2026-10-25', 'CONFIRMADA', { time: '11:00' }),
      appt('2026-10-25', 'CANCELADA', { time: '12:00' }),
      appt('2026-10-27', 'CONFIRMADA', { time: '12:00' }),
    ];
    const upcoming = computeDashboard(data, { days: 30, today: TODAY }).upcoming;
    expect(upcoming.map((item) => item.fechaHoraInicio.slice(0, 16))).toEqual(['2026-10-25T11:00', '2026-10-25T15:00', '2026-10-26T09:00']);
  });

  it('tolera datos incompletos sin lanzar errores', () => {
    const data = [{ id: 1, fechaHoraInicio: '2026-10-20T10:00:00', estado: 'COMPLETADA' }, { id: 2 }];
    expect(() => computeDashboard(data, { days: 30, today: TODAY })).not.toThrow();
  });

  it('mantiene un rango válido si todo el historial corresponde a reservas futuras', () => {
    const stats = computeDashboard([appt('2026-10-26', 'CONFIRMADA')], { days: null, today: TODAY });
    expect(stats.range).toMatchObject({ start: '2026-10-25', end: '2026-10-26' });
    expect(stats.timeline).toHaveLength(2);
    expect(stats.timeline[1].count).toBe(1);
  });

  it('excluye citas finalizadas de la agenda de hoy y mañana', () => {
    const data = ['COMPLETADA', 'CANCELADA', 'NO_ASISTIO', 'EN_ATENCION', 'CONFIRMADA', 'PENDIENTE']
      .map((status) => appt('2026-10-25', status));
    expect(computeDashboard(data, { today: TODAY }).upcoming.map((item) => item.estado))
      .toEqual(['EN_ATENCION', 'CONFIRMADA', 'PENDIENTE']);
  });
});
