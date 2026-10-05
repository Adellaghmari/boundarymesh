import { functions } from '@vue/devtools-core'
import { createRpcServer, devtools, setDevToolsEnv, toggleComponentInspectorEnabled } from '@vue/devtools-kit'

declare global {
  interface Window {
    __VUE_DEVTOOLS_CLIENT_URL__?: string
  }
}
import overlayCssUrl from 'vite-plugin-vue-devtools/src/overlay/devtools-overlay.css?url'
import overlayScriptUrl from 'vite-plugin-vue-devtools/src/overlay/devtools-overlay.mjs?url'

/**
 * Mounts the real Vue DevTools client against this public synthetic application.
 * Vite Inspect, the editor launcher, and the component crosshair stay disabled.
 */
export function installPublicDevtools(): void {
  const clientUrl = new URL(`${import.meta.env.BASE_URL}__devtools__/`, window.location.href).href
  window.__VUE_DEVTOOLS_CLIENT_URL__ = clientUrl
  setDevToolsEnv({ vitePluginDetected: false })
  toggleComponentInspectorEnabled(false)
  devtools.init()
  createRpcServer(functions, { preset: 'iframe' })
  createRpcServer(functions, { preset: 'broadcast' })

  const style = document.createElement('style')
  style.textContent = '.vue-devtools__inspector-button{display:none !important}'
  document.head.appendChild(style)

  const link = document.createElement('link')
  link.rel = 'stylesheet'
  link.href = overlayCssUrl
  document.head.appendChild(link)

  const script = document.createElement('script')
  script.type = 'module'
  script.src = overlayScriptUrl
  document.body.appendChild(script)
}
