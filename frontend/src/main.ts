import { createApp } from 'vue'
import './styles/_base.scss'

// Hosted builds set VITE_APP_MODE=cloud. That bundle is the public portfolio page.
// Local `npm run dev` leaves the variable unset and keeps the lab console.
if (import.meta.env.VITE_APP_MODE === 'cloud') {
  void import('./views/PublicLandingView.vue').then(({ default: PublicLandingView }) => {
    createApp(PublicLandingView).mount('#app')
  })
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
