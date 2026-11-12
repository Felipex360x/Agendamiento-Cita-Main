import { describe, it, expect, vi, afterEach } from 'vitest';
import { apiRequest, parseApiError } from '../api/client.js';

const respuesta = (status, body) => ({ ok: false, status, text: () => Promise.resolve(typeof body === 'string' ? body : JSON.stringify(body)) });

describe('E22: contrato de errores de la API', () => {
  const originalFetch = global.fetch;
  afterEach(() => { global.fetch = originalFetch; });

  it('un 400 de validación con "errores" muestra los mensajes por campo, no "Error HTTP 400"', async () => {
    global.fetch = vi.fn().mockResolvedValue(respuesta(400, {
      status: 400, codigo: 'VALIDACION', mensaje: 'Formato de correo electrónico inválido La contraseña debe tener entre 8 y 72 caracteres',
      errores: { correo: 'Formato de correo electrónico inválido', password: 'La contraseña debe tener entre 8 y 72 caracteres' },
    }));
    const error = await apiRequest('/auth/register', { method: 'POST', body: '{}' }).catch((e) => e);
    expect(error.status).toBe(400);
    expect(error.message).not.toMatch(/Error HTTP/);
    expect(error.message).toContain('Formato de correo electrónico inválido');
    expect(error.fieldErrors).toEqual({
      correo: 'Formato de correo electrónico inválido',
      password: 'La contraseña debe tener entre 8 y 72 caracteres',
    });
  });

  it('acepta el formato plano anterior {campo: mensaje} y antepone la etiqueta del campo', () => {
    const { message, fieldErrors } = parseApiError({ correo: 'Ingresa un correo válido', telefono: 'Máximo 20' }, 400);
    expect(message).toBe('Correo: Ingresa un correo válido Teléfono: Máximo 20');
    expect(fieldErrors).toEqual({ correo: 'Ingresa un correo válido', telefono: 'Máximo 20' });
  });

  it('un 409 de conflicto muestra el mensaje del servidor', async () => {
    global.fetch = vi.fn().mockResolvedValue(respuesta(409, { status: 409, codigo: 'CONFLICTO', mensaje: 'El correo electrónico ya se encuentra registrado.', error: 'x' }));
    await expect(apiRequest('/auth/register')).rejects.toThrow('El correo electrónico ya se encuentra registrado.');
  });

  it('mantiene los formatos {message} y {error} y el respaldo genérico', () => {
    expect(parseApiError({ message: 'Recurso no encontrado' }, 404).message).toBe('Recurso no encontrado');
    expect(parseApiError({ error: 'Falla' }, 500).message).toBe('Falla');
    expect(parseApiError(null, 502).message).toBe('Error HTTP 502');
    expect(parseApiError('<html>502 Bad Gateway</html>', 502).message).toBe('Error HTTP 502');
  });

  it('un error de negocio sin errores por campo no inventa campos', () => {
    const { message, fieldErrors } = parseApiError({ status: 400, codigo: 'SOLICITUD_INVALIDA', mensaje: 'Debes indicar el motivo', error: 'Debes indicar el motivo' }, 400);
    expect(message).toBe('Debes indicar el motivo');
    expect(fieldErrors).toEqual({});
  });
});
