import { fileURLToPath, URL } from 'node:url'

import vue from '@vitejs/plugin-vue'
import { defineConfig, type Plugin } from 'vite'
import vueDevTools from 'vite-plugin-vue-devtools'

function rewriteOperatorEntry(): Plugin {
  const rewrite = (req: { url?: string }, _res: unknown, next: () => void) => {
    const url = req.url ?? ''
    const path = url.split('?')[0] ?? ''
    if (path === '/operator' || path.startsWith('/operator/')) {
      const query = url.includes('?') ? url.slice(url.indexOf('?')) : ''
      req.url = `/operator.html${query}`
    }
    next()
  }
  const redirectExplore = (
    req: { url?: string },
    res: { statusCode: number; setHeader: (name: string, value: string) => void; end: () => void },
    next: () => void,
  ) => {
    const path = (req.url ?? '').split('?')[0] ?? ''
    if (path === '/explore' || path.startsWith('/explore/')) {
      res.statusCode = 301
      res.setHeader('Location', '/')
      res.end()
      return
    }
    next()
  }
  return {
    name: 'boundarymesh-operator-entry',
    configureServer(server) {
      server.middlewares.use(rewrite)
    },
    configurePreviewServer(server) {
      server.middlewares.use(redirectExplore)
      server.middlewares.use(rewrite)
    },
  }
}

const proxy = {
  '/api': { target: 'http://127.0.0.1:8080', changeOrigin: true },
  '/v3': { target: 'http://127.0.0.1:8080', changeOrigin: true },
  '/oauth2': { target: 'http://127.0.0.1:8080', changeOrigin: true },
  '/login': { target: 'http://127.0.0.1:8080', changeOrigin: true },
  '/logout': { target: 'http://127.0.0.1:8080', changeOrigin: true },
}

export default defineConfig({
  plugins: [vue(), rewriteOperatorEntry(), ...(process.env.VITEST ? [] : [vueDevTools()])],
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
  build: {
    rollupOptions: {
      input: {
        main: fileURLToPath(new URL('./index.html', import.meta.url)),
        operator: fileURLToPath(new URL('./operator.html', import.meta.url)),
      },
    },
  },
  css: {
    preprocessorOptions: {
      scss: {
        quietDeps: true,
      },
    },
  },
})
