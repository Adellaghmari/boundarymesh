/// <reference types="vite/client" />

interface ImportMetaEnv {
  /**
   * Public browser configuration only. Never put secrets in VITE_* variables.
   * Empty means same-origin / Vite proxy.
   */
  readonly VITE_API_BASE_URL?: string
  readonly VITE_API_TIMEOUT_MS?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
