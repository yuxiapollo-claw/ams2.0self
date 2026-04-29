import { configDefaults, defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      }
    }
  },
  test: {
    environment: 'jsdom',
    include: ['src/tests/**/*.spec.ts'],
    exclude: [...configDefaults.exclude, 'tests/e2e/**']
  }
})
