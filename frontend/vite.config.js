import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  // Do not expose stale legacy HTML/JS from public/; the application is React-only.
  publicDir: false,
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/test/setup.js',
  },
  server: {
    host: 'localhost',
    port: 5173,
    allowedHosts: [
      '.trycloudflare.com',
      ...(process.env.TUNNEL_HOSTNAME ? [process.env.TUNNEL_HOSTNAME] : []),
    ],
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        xfwd: true, // añade X-Forwarded-For con la IP del cliente directo (necesario para el límite de intentos)
      },
      // Solo la sonda de salud de Spring (no se exponen otros endpoints de actuator).
      '/actuator/health': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/doc': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: 'dist',
  },
});
