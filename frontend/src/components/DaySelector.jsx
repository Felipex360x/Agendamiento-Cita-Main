import { useMemo, useState } from 'react';

export function localDateValue() {
  const date = new Date();
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

function dateKey(date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

function pageForDate(value, firstDate) {
  if (!value) return 0;
  const first = Date.parse(`${firstDate}T00:00:00Z`);
  const selected = Date.parse(`${value}T00:00:00Z`);
  if (Number.isNaN(selected)) return 0;
  return Math.max(0, Math.floor((selected - first) / 86400000 / 7));
}

export default function DaySelector({ value, onChange, minDate = localDateValue(), label = 'Fecha', required = true }) {
  const [weekOffset, setWeekOffset] = useState(() => pageForDate(value, minDate));
  const days = useMemo(() => {
    const firstDay = new Date(`${minDate}T12:00:00`);
    firstDay.setDate(firstDay.getDate() + weekOffset * 7);
    return Array.from({ length: 7 }, (_, index) => {
      const date = new Date(firstDay);
      date.setDate(firstDay.getDate() + index);
      return {
        value: dateKey(date),
        weekday: new Intl.DateTimeFormat('es-CL', { weekday: 'short' }).format(date).replace('.', ''),
        day: date.getDate(),
        month: new Intl.DateTimeFormat('es-CL', { month: 'short' }).format(date).replace('.', ''),
        label: new Intl.DateTimeFormat('es-CL', { weekday: 'long', day: 'numeric', month: 'long' }).format(date),
      };
    });
  }, [minDate, weekOffset]);
  const rangeLabel = `${new Intl.DateTimeFormat('es-CL', { day: 'numeric', month: 'short' }).format(new Date(`${days[0].value}T12:00:00`))} – ${new Intl.DateTimeFormat('es-CL', { day: 'numeric', month: 'short', year: 'numeric' }).format(new Date(`${days[6].value}T12:00:00`))}`;

  function changeWeek(amount) {
    setWeekOffset((offset) => Math.max(0, offset + amount));
    onChange('');
  }

  function chooseCalendarDate(event) {
    const selectedDate = event.target.value;
    onChange(selectedDate);
    setWeekOffset(pageForDate(selectedDate, minDate));
  }

  return <div className="site-day-picker">
    <span className="site-day-picker-label">{label}</span>
    <div className="site-day-picker-heading">
      <button type="button" aria-label="Ver semana anterior" disabled={weekOffset === 0} onClick={() => changeWeek(-1)}>‹</button>
      <strong aria-live="polite">{rangeLabel}</strong>
      <button type="button" aria-label="Ver semana siguiente" onClick={() => changeWeek(1)}>›</button>
    </div>
    <div className="site-day-options" role="group" aria-label={`Selecciona un día para ${label.toLowerCase()}`}>
      {days.map((day) => <button type="button" key={day.value} aria-label={day.label} aria-pressed={value === day.value} className={value === day.value ? 'selected' : ''} onClick={() => onChange(day.value)}>
        <small>{day.weekday}</small><strong>{day.day}</strong><span>{day.month}</span>
      </button>)}
    </div>
    <label className="site-calendar-input">Elegir otra fecha<input type="date" min={minDate} value={value} onChange={chooseCalendarDate} required={required} /></label>
  </div>;
}
