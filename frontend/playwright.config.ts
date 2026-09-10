import process from 'node:process'
import { defineConfig, devices } from '@playwright/test'

const isCi = !!process.env.CI
const frontendPort = isCi ? 4173 : 5173

export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  expect: { timeout: 15_000 },
  forbidOnly: isCi,
  retries: isCi ? 2 : 0,
  workers: 1,
  reporter: 'list',
  use: {
    baseURL: `http://127.0.0.1:${frontendPort}`,
    trace: 'on-first-retry',
    headless: true,
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: [
    {
      command: 'powershell -NoProfile -ExecutionPolicy Bypass -File ../scripts/start-backend.ps1',
      url: 'http://127.0.0.1:8080/api/v1/health',
      timeout: 300_000,
      reuseExistingServer: !isCi,
      stdout: 'pipe',
      stderr: 'pipe',
    },
    {
      command: isCi ? 'npm run build-only && npm run preview' : 'npm run dev',
      url: `http://127.0.0.1:${frontendPort}`,
      timeout: 180_000,
      reuseExistingServer: !isCi,
      stdout: 'pipe',
      stderr: 'pipe',
    },
  ],
})
