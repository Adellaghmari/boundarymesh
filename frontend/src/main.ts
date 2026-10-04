import { createApp } from 'vue'
import './styles/_base.scss'
import { selectPublicEntry } from './publicEntry'

// Hosted builds set VITE_APP_MODE=cloud. That bundle is the public portfolio.
// Local `npm run dev` leaves the variable unset and keeps the lab console.
if (import.meta.env.VITE_APP_MODE === 'cloud') {
  const entry = selectPublicEntry(window.location.pathname)
  // Keep each import() in its own statement. A shared ternary preloads only one stylesheet.
  const mount = async () => {
    if (entry === 'explore') {
      const { default: View } = await import('./views/PublicExplorerView.vue')
      createApp(View).mount('#app')
      return
    }
    if (entry === 'landing') {
      const { default: View } = await import('./views/PublicLandingView.vue')
      createApp(View).mount('#app')
      return
    }
    const { default: View } = await import('./views/PublicMissingView.vue')
    createApp(View).mount('#app')
  }
  void mount()
} else {
  void Promise.all([import('./App.vue'), import('./router'), import('pinia')]).then(
    ([{ default: App }, { default: router }, { createPinia }]) => {
      const app = createApp(App)
      app.use(createPinia())
      app.use(router)
      app.mount('#app')
    },
  )
}
