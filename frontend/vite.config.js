import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

const proxy = {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true,
    xfwd: true, // envía la IP real del cliente (sirve para el límite de intentos)
  },
  // Solo se reenvía la ruta que revisa el estado de Spring.
  '/actuator/health': {
    target: 'http://localhost:8080',
    changeOrigin: true,
  },
  '/doc': {
    target: 'http://localhost:8080',
    changeOrigin: true,
  },
};

export default defineConfig({
  plugins: [react()],
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/test/setup.js',
  },
  // Desarrollo local. No se publica por el túnel: el servidor de desarrollo sirve el código fuente.
  server: {
    host: 'localhost',
    port: 5173,
    proxy,
  },
  // Sirve la versión compilada (dist/). Es lo que se publica por Cloudflare Tunnel en la demo.
  preview: {
    host: 'localhost',
    port: 5173,
    allowedHosts: [
      '.trycloudflare.com',
      ...(process.env.TUNNEL_HOSTNAME ? [process.env.TUNNEL_HOSTNAME] : []),
    ],
    proxy,
  },
  build: {
    outDir: 'dist',
  },
});
