import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import React from 'react';
import { render, screen, act, waitFor } from '@testing-library/react';
import { AuthProvider, useAuth } from '../auth/AuthContext.jsx';
import { api } from '../api/client.js';
import fs from 'fs';
import path from 'path';

// Componente auxiliar de prueba para consumir el hook useAuth
function DummyConsumer() {
  const { user, loading, login, logout } = useAuth();
  return (
    <div>
      <span data-testid="loading">{loading ? 'true' : 'false'}</span>
      <span data-testid="user">{user ? user.nombre : 'anonymous'}</span>
      <span data-testid="rol">{user ? user.rol : 'none'}</span>
      <button data-testid="btn-login" onClick={() => login('admin@nicolet.cl', 'pass123')}>Login</button>
      <button data-testid="btn-logout" onClick={logout}>Logout</button>
    </div>
  );
}

describe('Módulo de Autenticación React: AuthContext.jsx', () => {
  const TOKEN_KEY = 'nicolet_jwt_token';
  const USER_KEY = 'nicolet_jwt_user';

  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  describe('Control de Invocación del Hook useAuth()', () => {
    it('debe lanzar un error descriptivo si se invoca fuera de AuthProvider', () => {
      // Suprimir error en consola de React para test de error boundary esperado
      const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {});

      expect(() => {
        render(<DummyConsumer />);
      }).toThrow('useAuth debe usarse dentro de AuthProvider');

      consoleError.mockRestore();
    });
  });

  describe('Flujos de Estado de AuthProvider', () => {
    it('debe inicializarse como usuario anónimo si no hay sesión previa en localStorage', () => {
      render(
        <AuthProvider>
          <DummyConsumer />
        </AuthProvider>
      );

      expect(screen.getByTestId('loading').textContent).toBe('false');
      expect(screen.getByTestId('user').textContent).toBe('anonymous');
      expect(screen.getByTestId('rol').textContent).toBe('none');
    });

    it('debe restaurar usuario persistido en localStorage al montar', () => {
      const stored = { id: 10, nombre: 'Valentina', apellidoP: 'Soto', rol: 'PROFESIONAL' };
      localStorage.setItem(USER_KEY, JSON.stringify(stored));

      render(
        <AuthProvider>
          <DummyConsumer />
        </AuthProvider>
      );

      expect(screen.getByTestId('user').textContent).toBe('Valentina');
      expect(screen.getByTestId('rol').textContent).toBe('PROFESIONAL');
    });

    it('debe validar la sesión con api.me() al iniciar si existe token', async () => {
      localStorage.setItem(TOKEN_KEY, 'valid-token-abc');
      const apiUser = { id: 5, nombre: 'Isidora', apellidoP: 'Muñoz', rol: 'ADMIN' };
      vi.spyOn(api, 'me').mockResolvedValue(apiUser);

      render(
        <AuthProvider>
          <DummyConsumer />
        </AuthProvider>
      );

      // Estado inicial con token: loading es true
      expect(screen.getByTestId('loading').textContent).toBe('true');

      await waitFor(() => {
        expect(screen.getByTestId('loading').textContent).toBe('false');
      });

      expect(screen.getByTestId('user').textContent).toBe('Isidora');
      expect(screen.getByTestId('rol').textContent).toBe('ADMIN');
      expect(JSON.parse(localStorage.getItem(USER_KEY))).toEqual(apiUser);
    });

    it('debe hacer logout si api.me() falla con sesión inválida al montar', async () => {
      localStorage.setItem(TOKEN_KEY, 'expired-token');
      localStorage.setItem(USER_KEY, JSON.stringify({ nombre: 'Antiguo' }));
      vi.spyOn(api, 'me').mockRejectedValue(new Error('Sesión expirada'));

      render(
        <AuthProvider>
          <DummyConsumer />
        </AuthProvider>
      );

      await waitFor(() => {
        expect(screen.getByTestId('loading').textContent).toBe('false');
      });

      expect(screen.getByTestId('user').textContent).toBe('anonymous');
      expect(localStorage.getItem(TOKEN_KEY)).toBeNull();
      expect(localStorage.getItem(USER_KEY)).toBeNull();
    });

    it('debe procesar login exitoso guardando token y actualizando estado', async () => {
      const loginPayload = {
        token: 'new-jwt-token-777',
        id: 1,
        nombre: 'Camila',
        apellidoP: 'Rojas',
        correo: 'camila@nicolet.cl',
        rol: 'PROFESIONAL',
      };
      vi.spyOn(api, 'login').mockResolvedValue(loginPayload);

      render(
        <AuthProvider>
          <DummyConsumer />
        </AuthProvider>
      );

      await act(async () => {
        screen.getByTestId('btn-login').click();
      });

      expect(screen.getByTestId('user').textContent).toBe('Camila');
      expect(screen.getByTestId('rol').textContent).toBe('PROFESIONAL');
      expect(localStorage.getItem(TOKEN_KEY)).toBe('new-jwt-token-777');
      expect(JSON.parse(localStorage.getItem(USER_KEY))).toEqual({
        id: 1,
        nombre: 'Camila',
        apellidoP: 'Rojas',
        correo: 'camila@nicolet.cl',
        rol: 'PROFESIONAL',
      });
    });

    it('debe procesar logout manual limpiando credenciales en localStorage', async () => {
      localStorage.setItem(TOKEN_KEY, 'token-to-delete');
      localStorage.setItem(USER_KEY, JSON.stringify({ nombre: 'Activo' }));

      render(
        <AuthProvider>
          <DummyConsumer />
        </AuthProvider>
      );

      await act(async () => {
        screen.getByTestId('btn-logout').click();
      });

      expect(screen.getByTestId('user').textContent).toBe('anonymous');
      expect(localStorage.getItem(TOKEN_KEY)).toBeNull();
      expect(localStorage.getItem(USER_KEY)).toBeNull();
    });

    it('debe cerrar sesión automáticamente al recibir el evento global "nicolet:unauthorized"', async () => {
      localStorage.setItem(TOKEN_KEY, 'stale-token');
      localStorage.setItem(USER_KEY, JSON.stringify({ nombre: 'Usuario Temporal' }));

      render(
        <AuthProvider>
          <DummyConsumer />
        </AuthProvider>
      );

      expect(screen.getByTestId('user').textContent).toBe('Usuario Temporal');

      await act(async () => {
        window.dispatchEvent(new Event('nicolet:unauthorized'));
      });

      expect(screen.getByTestId('user').textContent).toBe('anonymous');
      expect(localStorage.getItem(TOKEN_KEY)).toBeNull();
      expect(localStorage.getItem(USER_KEY)).toBeNull();
    });
  });

  describe('Auditoría QA: Detección de Código Desconectado / Huérfano (Architecture Gap)', () => {
    it('DEF-04 Corregido: AuthProvider está integrado y envuelve App en frontend/src/main.jsx', () => {
      const mainPath = path.resolve(__dirname, '../main.jsx');
      const mainSource = fs.readFileSync(mainPath, 'utf8');

      // Verificamos que AuthProvider es importado y renderizado correctamente en main.jsx
      const importsAuthProvider = mainSource.includes('AuthProvider');
      const rendersAuthProvider = /<AuthProvider[\s\S]*<\/AuthProvider>|<AuthProvider\s*\/>/.test(mainSource);

      expect(importsAuthProvider).toBe(true);
      expect(rendersAuthProvider).toBe(true);
    });

    it('QA-DEFECT-06: AuthProvider o useAuth NO están integrados en frontend/src/App.jsx', () => {
      const appPath = path.resolve(__dirname, '../App.jsx');
      const appSource = fs.readFileSync(appPath, 'utf8');

      const usesAuthInApp = appSource.includes('useAuth') || appSource.includes('AuthProvider');
      expect(usesAuthInApp).toBe(false);
    });

    it('QA-DEFECT-07: LegacyPage no comparte estado de sesión reactiva con AuthContext', () => {
      // LegacyPage inyecta HTML y scripts de forma imperativa sin consultar el contexto React
      const appPath = path.resolve(__dirname, '../App.jsx');
      const appSource = fs.readFileSync(appPath, 'utf8');

      expect(appSource).not.toContain('AuthContext');
      expect(appSource).not.toContain('client.js');
    });
  });
});
