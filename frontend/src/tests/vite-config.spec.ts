/** @vitest-environment node */

import { describe, expect, it } from 'vitest'

import config from '../../vite.config'

describe('vitest discovery config', () => {
  it('limits unit tests to the src/tests directory and keeps e2e out of vitest', () => {
    expect(config.test?.include).toEqual(['src/tests/**/*.spec.ts'])
    expect(config.test?.exclude).toEqual(
      expect.arrayContaining(['**/node_modules/**', 'tests/e2e/**'])
    )
  })

  it('proxies api requests to the local backend during direct-run smoke tests', () => {
    expect(config.server?.proxy).toEqual({
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      }
    })
  })
})
