import { useEffect, useState } from 'react';
import { HomePage, LoginPage, ServicesPage, TeamPage } from './pages/PublicPages.jsx';
import BookingPage from './pages/BookingPage.jsx';
import AdminAppointmentsPage from './pages/AdminAppointmentsPage.jsx';
import AdminServicesPage from './pages/AdminServicesPage.jsx';
import AdminUsersPage from './pages/AdminUsersPage.jsx';
import AdminAvailabilityPage from './pages/AdminAvailabilityPage.jsx';
import AdminHistoryPage from './pages/AdminHistoryPage.jsx';
import WorkerPortalPage from './pages/WorkerPortalPage.jsx';
import WorkerSchedulePage from './pages/WorkerSchedulePage.jsx';
import AdminDiagnosticsPage from './pages/AdminDiagnosticsPage.jsx';
import { StaffAgendaPage, StaffClientsPage, StaffDashboardPage } from './pages/StaffPages.jsx';
import ClientProfilePage from './pages/ClientProfilePage.jsx';

export const PUBLIC_PAGES = new Set(['index.html', 'reservar.html', 'servicios.html', 'equipo.html', 'agenda.html', 'dashboard.html', 'clientas.html', 'mi-perfil.html']);
export const ADMIN_PAGES = new Set(['citas.html', 'disponibilidad.html', 'historial.html', 'login.html', 'mi-horario.html', 'panel-trabajador.html', 'pruebas.html', 'reserva-cliente.html', 'servicios.html', 'usuarios.html']);

export function getLegacyPath(pathname) {
  const clean = pathname.replace(/^\/+/, '');
  if (!clean) return 'index.html';
  if (clean === 'app' || clean === 'app/') return 'admin/citas.html';
  if (clean === 'admin' || clean === 'admin/') return 'admin/citas.html';

  // Alias y accesos directos críticos (DEF-01, DEF-03)
  if (clean === 'login' || clean === 'login.html') return 'admin/login.html';
  if (clean === 'reserva-cliente' || clean === 'reserva-cliente.html') return 'admin/reserva-cliente.html';

  if (clean.startsWith('admin/')) {
    let page = clean.slice('admin/'.length).split('/')[0];
    if (page && !page.includes('.')) page = `${page}.html`;
    return ADMIN_PAGES.has(page) ? `admin/${page}` : 'admin/citas.html';
  }

  let page = clean.split('/')[0];
  if (page && !page.includes('.')) {
    if (PUBLIC_PAGES.has(`${page}.html`)) return `${page}.html`;
    if (ADMIN_PAGES.has(`${page}.html`)) return `admin/${page}.html`;
  }
  return PUBLIC_PAGES.has(page) ? page : 'index.html';
}

export function assetUrl(value, sourceUrl) {
  if (!value) return value;
  if (typeof value !== 'string') return value;
  const trimmed = value.trim();
  if (/^javascript:/i.test(trimmed)) return '#';
  if (/^(?:[a-z0-9+.-]+:|\/\/|#|data:)/i.test(trimmed)) return trimmed;
  return new URL(trimmed, sourceUrl).href;
}

/**
 * Retained for compatibility while direct legacy asset URLs are phased out.
 * Application routes below now render React pages directly.
 */
export function LegacyPage() {
  return <main className="site-page-section"><h1>Esta vista antigua ya no está disponible</h1><a href="/">Volver al inicio</a></main>;
}

export default function App() {
  const page = getLegacyPath(window.location.pathname);

  if (page === 'index.html') return <HomePage />;
  if (page === 'servicios.html') return <ServicesPage />;
  if (page === 'equipo.html') return <TeamPage />;
  if (page === 'mi-perfil.html') return <ClientProfilePage />;
  if (page === 'agenda.html') return <StaffAgendaPage />;
  if (page === 'dashboard.html') return <StaffDashboardPage />;
  if (page === 'clientas.html') return <StaffClientsPage />;
  if (page === 'reservar.html' || page === 'admin/reserva-cliente.html') return <BookingPage />;
  if (page === 'admin/login.html') return <LoginPage />;
  if (page === 'admin/citas.html') return <AdminAppointmentsPage />;
  if (page === 'admin/servicios.html') return <AdminServicesPage />;
  if (page === 'admin/usuarios.html') return <AdminUsersPage />;
  if (page === 'admin/disponibilidad.html') return <AdminAvailabilityPage />;
  if (page === 'admin/historial.html') return <AdminHistoryPage />;
  if (page === 'admin/panel-trabajador.html') return <WorkerPortalPage />;
  if (page === 'admin/mi-horario.html') return <WorkerSchedulePage />;
  if (page === 'admin/pruebas.html') return <AdminDiagnosticsPage />;
  return <main className="site-page-section"><h1>Página no encontrada</h1><a href="/">Volver al inicio</a></main>;
}
