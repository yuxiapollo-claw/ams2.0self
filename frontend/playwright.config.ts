import { defineConfig } from '@playwright/test'

const e2ePort = 4173

export default defineConfig({
  testDir: './tests/e2e',
  use: {
    baseURL: `http://127.0.0.1:${e2ePort}`
  },
  webServer: {
    command: `npm run dev -- --host 127.0.0.1 --port ${e2ePort}`,
    url: `http://127.0.0.1:${e2ePort}/login`,
    reuseExistingServer: false,
    timeout: 120000
  }
})
