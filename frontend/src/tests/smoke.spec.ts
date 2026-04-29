import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { beforeEach, describe, expect, it } from 'vitest'

describe('app bootstrap', () => {
  beforeEach(() => {
    document.body.innerHTML = ''
  })

  it('bootstraps from the scaffold html entrypoint', async () => {
    const html = readFileSync(resolve(__dirname, '../../index.html'), 'utf8')
    expect(html).toMatch(/<script\s+type="module"\s+src="\/src\/main\.ts"><\/script>/)
    document.body.innerHTML = html.match(/<body[^>]*>([\s\S]*)<\/body>/i)?.[1] ?? ''

    await import('../main')

    expect(document.querySelector('#app')).not.toBeNull()
    expect(document.querySelector('#ams-root')?.textContent).toContain('AMS2.0')
  })
})
