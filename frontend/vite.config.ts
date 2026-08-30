import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

export default defineConfig({
  base: process.env.VITE_BASE_PATH ?? '/',
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    proxy: {
      // Port 8080 is occupied by Oracle TNS Listener on this development PC.
      // Keep the local RaahMediQ backend on 8081 so API calls never reach the unrelated service.
      '/api': process.env.VITE_BACKEND_PROXY ?? 'http://127.0.0.1:8081',
      '/actuator': process.env.VITE_BACKEND_PROXY ?? 'http://127.0.0.1:8081',
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: './src/test-setup.ts',
  },
})
