import { defineConfig, devices } from '@playwright/test'

const port = 4174

export default defineConfig({
  testDir: './e2e',
  testMatch: 'public-recruiter-flow.spec.ts',
  timeout: 60_000,
  expect: { timeout: 15_000 },
  forbidOnly: true,
  retries: 0,
  workers: 1,
  reporter: 'list',
  use: {
    baseURL: `http://127.0.0.1:${port}`,
    trace: 'on-first-retry',
    headless: true,
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: {
    command:
      process.platform === 'win32'
        ? `set VITE_APP_MODE=cloud&& npm run build-only && npm run preview -- --host 127.0.0.1 --port ${port} --strictPort`
        : `VITE_APP_MODE=cloud npm run build-only && npm run preview -- --host 127.0.0.1 --port ${port} --strictPort`,
    url: `http://127.0.0.1:${port}`,
    timeout: 180_000,
    reuseExistingServer: false,
    stdout: 'pipe',
    stderr: 'pipe',
  },
})
