import { describe, expect, it, vi } from 'vitest'

vi.mock('dompurify', () => ({
  default: {
    addHook: vi.fn(),
    sanitize: (html: string) => html
  }
}))

import { localImagePaths, markdownParts } from './Markdown'

describe('local Markdown images', () => {
  it('keeps Windows backslashes that Markdown would otherwise treat as escapes', () => {
    const path = String.raw`C:\Users\Lenovo\.fs-bot\userdata\automation\temp\screenshots\browser.png`
    expect(localImagePaths(`![我负责的15条](${path})`)).toEqual([path])
  })

  it('handles angle-bracket paths containing spaces and balanced parentheses', () => {
    const path = String.raw`C:\Users\Lenovo\.fs-bot\my screenshots\result(1).png`
    expect(localImagePaths(`![截图](<${path}>)`)).toEqual([path])
  })

  it('ignores image-shaped text inside code blocks', () => {
    const text = '```md\n![截图](C:\\Users\\Lenovo\\.fs-bot\\a.png)\n```'
    expect(localImagePaths(text)).toEqual([])
  })

  it('passes the exact Windows path to the image resolver', () => {
    const path = String.raw`C:\Users\Lenovo\.fs-bot\userdata\automation\temp\screenshots\browser.png`
    const parts = markdownParts(`![我负责的15条](${path})`, {
      localImage: (value) => `chat-image://${encodeURIComponent(value)}`
    })
    expect(parts[0]?.type).toBe('text')
    if (parts[0]?.type === 'text') {
      expect(parts[0].html).toContain(`src="chat-image://${encodeURIComponent(path)}"`)
    }
  })
})
