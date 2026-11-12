import { describe, it, expect } from 'vitest';
import { businessParts } from '../utils/dates.js';

describe('dates.js: «hoy» y hora en la zona del negocio (America/Santiago)', () => {
  it('no depende de la zona del navegador: medianoche UTC aún es el día anterior en Chile', () => {
    // 2026-11-03T02:30Z = 2 de noviembre 23:30 en Santiago (UTC-3, horario de verano)
    expect(businessParts(new Date('2026-11-03T02:30:00Z'), 'America/Santiago')).toEqual({ date: '2026-11-02', time: '23:30' });
    expect(businessParts(new Date('2026-11-03T03:00:00Z'), 'America/Santiago')).toEqual({ date: '2026-11-03', time: '00:00' });
  });

  it('cambio a horario de verano (6 sep 2026: 24:00 → 01:00)', () => {
    expect(businessParts(new Date('2026-09-06T03:30:00Z'), 'America/Santiago')).toEqual({ date: '2026-09-05', time: '23:30' });
    expect(businessParts(new Date('2026-09-06T04:30:00Z'), 'America/Santiago')).toEqual({ date: '2026-09-06', time: '01:30' });
  });

  it('cambio a horario de invierno (4 abr 2026: 24:00 → 23:00)', () => {
    expect(businessParts(new Date('2026-04-05T02:30:00Z'), 'America/Santiago')).toEqual({ date: '2026-04-04', time: '23:30' });
    expect(businessParts(new Date('2026-04-05T03:30:00Z'), 'America/Santiago')).toEqual({ date: '2026-04-04', time: '23:30' });
  });
});
