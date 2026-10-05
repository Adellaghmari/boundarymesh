import { createApp } from 'vue'
import './styles/_base.scss'
import { selectPublicEntry } from './publicEntry'

// Hosted builds set VITE_APP_MODE=cloud and mount the same console as local development.
// The operator document is a separate entry and is not loaded here.
// Keep App, router, and pinia as separate import() calls.
async function mountProduct(): Promise<void> {
  const [{ default: App }, { default: router }, { createPinia, setActivePinia }] = await Promise.all([
    import('./App.vue'),
    import('./router'),
    import('pinia'),
  ])
  if (import.meta.env.VITE_APP_MODE === 'cloud') {
    const { installPublicDevtools } = await import('./devtools/installPublicDevtools')
    installPublicDevtools()
  }
  const app = createApp(App)
  const pinia = createPinia()
  setActivePinia(pinia)
  app.use(pinia)
  app.use(router)
  if (import.meta.env.VITE_APP_MODE === 'cloud') {
    const { seedPublicDemo } = await import('./demo/seedPublicDemo')
    await seedPublicDemo()
  }
  app.mount('#app')
}

if (import.meta.env.VITE_APP_MODE === 'cloud') {
  const entry = selectPublicEntry(window.location.pathname)
  if (entry === 'redirect-home') {
    window.location.replace('/')
  } else {
    void mountProduct()
  }
} else {
  void mountProduct()
}
