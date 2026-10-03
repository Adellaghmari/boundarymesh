import { createApp } from 'vue'
import './styles/_base.scss'
import CloudConsoleView from './views/CloudConsoleView.vue'

// Separate hosted document for /operator. Lab routes are not imported here.
createApp(CloudConsoleView).mount('#app')
