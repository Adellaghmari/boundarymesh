import { fileURLToPath, URL } from 'node:url'

import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'
import vueDevTools from 'vite-plugin-vue-devtools'

const proxy = {
  '/api': { target: 'http://127.0.0.1:8080', changeOrigin: true },
  '/v3': { target: 'http://127.0.0.1:8080', changeOrigin: true },
  '/oauth2': { target: 'http://127.0.0.1:8080', changeOrigin: true },
  '/login': { target: 'http://127.0.0.1:8080', changeOrigin: true },
  '/logout': { target: 'http://127.0.0.1:8080', changeOrigin: true },
}

export default defineConfig({
  plugins: [vue(), ...(process.env.VITEST ? [] : [vueDevTools()])],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    host: '127.0.0.1',
    port: 5173,
    strictPort: true,
    proxy,
  },
  preview: {
    host: '127.0.0.1',
    port: 4173,
    strictPort: true,
    proxy,
  },
  css: {
    preprocessorOptions: {
      scss: {
        quietDeps: true,
      },
    },
  },
})
