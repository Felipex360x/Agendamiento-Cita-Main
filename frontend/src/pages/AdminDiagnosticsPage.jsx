import { useState } from 'react';
import { api } from '../api/client.js';
import { AdminShell, RequestMessage } from './AdminShell.jsx';

const checks = [
  ['Servicios', () => api.servicios()],
  ['Profesionales', () => api.trabajadores()],
  ['Citas', () => api.citas()],
  ['Estado Spring', async () => {
    const response = await fetch('/actuator/health');
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    return response.json();
  }],
];

export default function AdminDiagnosticsPage() {
  const [results, setResults] = useState({});
  const [running, setRunning] = useState(false);
  const [error, setError] = useState('');

  async function runChecks() {
    setRunning(true);
    setError('');
    const next = {};
    for (const [name, request] of checks) {
      try {
        const value = await request();
        next[name] = { ok: true, detail: Array.isArray(value) ? `${value.length} registros` : value.status || 'Disponible' };
      } catch (requestError) {
        next[name] = { ok: false, detail: requestError.message };
      }
      setResults({ ...next });
    }
    setRunning(false);
  }

  return <AdminShell title="Diagnóstico de servicios" active="/admin/pruebas">
    <RequestMessage error={error} />
    <section className="admin-panel"><p>Comprueba la conexión con los endpoints principales y el estado de Spring Boot.</p><button className="site-button" type="button" onClick={runChecks} disabled={running}>{running ? 'Consultando…' : 'Ejecutar diagnóstico'}</button><div className="admin-check-list">{checks.map(([name]) => <article key={name} className={results[name]?.ok ? 'success' : results[name] ? 'error' : ''}><strong>{name}</strong><span>{results[name] ? results[name].detail : 'Pendiente'}</span></article>)}</div></section>
  </AdminShell>;
}
