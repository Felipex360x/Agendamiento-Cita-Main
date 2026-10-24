import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { api, apiRequest, ApiError } from '../api/client.js';

describe('Cliente API Frontend: client.js', () => {
  const originalFetch = global.fetch;
  const originalLocalStorage = global.localStorage;

  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  afterEach(() => {
    global.fetch = originalFetch;
  });

  describe('Clase ApiError', () => {
    it('debe instanciar ApiError con mensaje, código de estado y payload', () => {
      const payload = { error: 'No autorizado', codigo: 401 };
      const err = new ApiError('Credenciales inválidas', 401, payload);

      expect(err).toBeInstanceOf(Error);
      expect(err).toBeInstanceOf(ApiError);
      expect(err.name).toBe('ApiError');
      expect(err.message).toBe('Credenciales inválidas');
      expect(err.status).toBe(401);
      expect(err.payload).toEqual(payload);
    });
  });

  describe('Función apiRequest() - Headers y Token JWT', () => {
    it('debe incluir el header Authorization con Bearer token si existe en localStorage', async () => {
      localStorage.setItem('nicolet_jwt_token', 'token-jwt-super-seguro-xyz');

      let capturedHeaders = null;
      global.fetch = vi.fn().mockImplementation((url, options) => {
        capturedHeaders = options.headers;
        return Promise.resolve({
          ok: true,
          status: 200,
          text: () => Promise.resolve(JSON.stringify({ ok: true })),
        });
      });

      await apiRequest('/servicios');

      expect(capturedHeaders.get('Authorization')).toBe('Bearer token-jwt-super-seguro-xyz');
    });

    it('no debe incluir el header Authorization si no hay token en localStorage', async () => {
      let capturedHeaders = null;
      global.fetch = vi.fn().mockImplementation((url, options) => {
        capturedHeaders = options.headers;
        return Promise.resolve({
          ok: true,
          status: 200,
          text: () => Promise.resolve(JSON.stringify([])),
        });
      });

      await apiRequest('/servicios');

      expect(capturedHeaders.get('Authorization')).toBeNull();
    });

    it('debe establecer Content-Type: application/json si hay body y no es FormData', async () => {
      let capturedHeaders = null;
      global.fetch = vi.fn().mockImplementation((url, options) => {
        capturedHeaders = options.headers;
        return Promise.resolve({
          ok: true,
          status: 200,
          text: () => Promise.resolve(JSON.stringify({ id: 1 })),
        });
      });

      await apiRequest('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ correo: 'test@nicolet.cl', password: '123' }),
      });

      expect(capturedHeaders.get('Content-Type')).toBe('application/json');
    });

    it('no debe sobreescribir Content-Type si el body es instancia de FormData', async () => {
      let capturedHeaders = null;
      global.fetch = vi.fn().mockImplementation((url, options) => {
        capturedHeaders = options.headers;
        return Promise.resolve({
          ok: true,
          status: 200,
          text: () => Promise.resolve(JSON.stringify({ ok: true })),
        });
      });

      const formData = new FormData();
      formData.append('archivo', 'dummy');

      await apiRequest('/upload', {
        method: 'POST',
        body: formData,
      });

      expect(capturedHeaders.get('Content-Type')).toBeNull();
    });
  });

  describe('Manejo de Respuestas Exitosas', () => {
    it('debe parsear payloads JSON correctamente', async () => {
      const mockData = [{ id: 1, nombre: 'Corte de Cabello', precio: 25000 }];
      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        status: 200,
        text: () => Promise.resolve(JSON.stringify(mockData)),
      });

      const resultado = await apiRequest('/servicios');
      expect(resultado).toEqual(mockData);
    });

    it('debe retornar payload como texto plano si la respuesta no es JSON', async () => {
      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        status: 200,
        text: () => Promise.resolve('OK PONG'),
      });

      const resultado = await apiRequest('/health');
      expect(resultado).toBe('OK PONG');
    });

    it('debe retornar null si la respuesta tiene cuerpo vacío', async () => {
      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        status: 204,
        text: () => Promise.resolve(''),
      });

      const resultado = await apiRequest('/cita/1', { method: 'DELETE' });
      expect(resultado).toBeNull();
    });
  });

  describe('Manejo de Errores HTTP y Excepciones de Red', () => {
    it('debe disparar evento "nicolet:unauthorized" en ventana ante error HTTP 401', async () => {
      let eventDispatched = false;
      const onUnauthorized = () => { eventDispatched = true; };
      window.addEventListener('nicolet:unauthorized', onUnauthorized);

      global.fetch = vi.fn().mockResolvedValue({
        ok: false,
        status: 401,
        text: () => Promise.resolve(JSON.stringify({ mensaje: 'Token expirado' })),
      });

      await expect(apiRequest('/auth/me')).rejects.toThrow('Token expirado');
      expect(eventDispatched).toBe(true);

      window.removeEventListener('nicolet:unauthorized', onUnauthorized);
    });

    it('debe extraer el mensaje de error de payload.mensaje, payload.message o payload.error', async () => {
      // Caso 1: payload.mensaje
      global.fetch = vi.fn().mockResolvedValueOnce({
        ok: false,
        status: 400,
        text: () => Promise.resolve(JSON.stringify({ mensaje: 'El correo ya está registrado' })),
      });
      await expect(apiRequest('/usuarios')).rejects.toThrow('El correo ya está registrado');

      // Caso 2: payload.message
      global.fetch = vi.fn().mockResolvedValueOnce({
        ok: false,
        status: 404,
        text: () => Promise.resolve(JSON.stringify({ message: 'Recurso no encontrado' })),
      });
      await expect(apiRequest('/servicios/999')).rejects.toThrow('Recurso no encontrado');

      // Caso 3: payload.error
      global.fetch = vi.fn().mockResolvedValueOnce({
        ok: false,
        status: 500,
        text: () => Promise.resolve(JSON.stringify({ error: 'Error interno en base de datos' })),
      });
      await expect(apiRequest('/db-fail')).rejects.toThrow('Error interno en base de datos');

      // Caso 4: fallback a Error HTTP 502
      global.fetch = vi.fn().mockResolvedValueOnce({
        ok: false,
        status: 502,
        text: () => Promise.resolve(''),
      });
      await expect(apiRequest('/bad-gateway')).rejects.toThrow('Error HTTP 502');
    });

    it('debe manejar falla de conexión de red lanzando ApiError con status 0 y mensaje claro', async () => {
      global.fetch = vi.fn().mockRejectedValue(new Error('Failed to fetch'));

      try {
        await apiRequest('/servicios');
        expect.unreachable();
      } catch (err) {
        expect(err).toBeInstanceOf(ApiError);
        expect(err.status).toBe(0);
        expect(err.message).toContain('No se pudo conectar con el backend');
        expect(err.message).toContain('localhost:8080');
      }
    });
  });

  describe('Endpoints expuestos en el objeto exportado "api"', () => {
    it('api.login debe invocar POST /auth/login con correo y password en JSON', async () => {
      let callUrl = '';
      let callOptions = null;

      global.fetch = vi.fn().mockImplementation((url, opts) => {
        callUrl = url;
        callOptions = opts;
        return Promise.resolve({
          ok: true,
          status: 200,
          text: () => Promise.resolve(JSON.stringify({ token: 'jwt-123', id: 1, rol: 'ADMIN' })),
        });
      });

      const res = await api.login('admin@nicolet.cl', 'pass123');
      expect(callUrl).toContain('/auth/login');
      expect(callOptions.method).toBe('POST');
      expect(JSON.parse(callOptions.body)).toEqual({ correo: 'admin@nicolet.cl', password: 'pass123' });
      expect(res.token).toBe('jwt-123');
    });

    it('api.me debe invocar GET /auth/me', async () => {
      let callUrl = '';
      global.fetch = vi.fn().mockImplementation((url) => {
        callUrl = url;
        return Promise.resolve({
          ok: true,
          status: 200,
          text: () => Promise.resolve(JSON.stringify({ id: 1, nombre: 'Admin' })),
        });
      });

      const user = await api.me();
      expect(callUrl).toContain('/auth/me');
      expect(user.nombre).toBe('Admin');
    });

    it('api.servicios debe invocar GET /servicios', async () => {
      let callUrl = '';
      global.fetch = vi.fn().mockImplementation((url) => {
        callUrl = url;
        return Promise.resolve({
          ok: true,
          status: 200,
          text: () => Promise.resolve(JSON.stringify([{ id: 10, nombre: 'Manicure' }])),
        });
      });

      const list = await api.servicios();
      expect(callUrl).toContain('/servicios');
      expect(list).toHaveLength(1);
    });

    it('api.trabajadores debe invocar GET /trabajadores', async () => {
      let callUrl = '';
      global.fetch = vi.fn().mockImplementation((url) => {
        callUrl = url;
        return Promise.resolve({
          ok: true,
          status: 200,
          text: () => Promise.resolve(JSON.stringify([{ id: 2, nombre: 'Camila' }])),
        });
      });

      const list = await api.trabajadores();
      expect(callUrl).toContain('/trabajadores');
      expect(list[0].nombre).toBe('Camila');
    });
  });
});
