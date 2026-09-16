import { createApp } from 'vue'
import './styles/_base.scss'

// Build-time split keeps local lab routes and their chunks out of the hosted bundle.
if (import.meta.env.VITE_APP_MODE === 'cloud') {
  void import('./views/CloudConsoleView.vue').then(({ default: CloudConsoleView }) => {
    createApp(CloudConsoleView).mount('#app')
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
