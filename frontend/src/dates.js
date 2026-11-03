// Contrato de fechas: LocalDate/LocalDateTime de la API son hora de pared del negocio, en la zona
// VITE_BUSINESS_TIMEZONE (por defecto America/Santiago; debe coincidir con app.timezone del backend).
// «Hoy» y «hora actual» se calculan en esa zona, no en la del navegador.
export const BUSINESS_TIMEZONE = import.meta.env?.VITE_BUSINESS_TIMEZONE || 'America/Santiago';

export function businessParts(now = new Date(), timeZone = BUSINESS_TIMEZONE) {
  const parts = Object.fromEntries(
    new Intl.DateTimeFormat('en-CA', {
      timeZone, year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hourCycle: 'h23',
    }).formatToParts(now).map((part) => [part.type, part.value]),
  );
  return { date: `${parts.year}-${parts.month}-${parts.day}`, time: `${parts.hour}:${parts.minute}` };
}

export const businessToday = (now) => businessParts(now).date;
export const businessNowTime = (now) => businessParts(now).time;
