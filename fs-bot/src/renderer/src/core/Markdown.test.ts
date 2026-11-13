import { describe, expect, it, vi } from 'vitest'

vi.mock('dompurify', () => ({
  default: {
    addHook: vi.fn(),
    sanitize: (html: string) => html
  }
}))

import { localImagePaths, resolveMarkdownImages } from './Markdown'

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
    const source = resolveMarkdownImages(`![我负责的15条](${path})`, {
      localImage: (value) => `chat-image://${encodeURIComponent(value)}`
    })
    expect(source).toContain(`chat-image://${encodeURIComponent(path)}`)
  })

  it('rewrites image destinations in place for Vditor without touching code blocks', () => {
    const path = String.raw`C:\Users\Lenovo\.fs-bot\my screenshots\result(1).png`
    const source = `![截图](<${path}>)\n\n\`\`\`md\n![示例](local:a.png)\n\`\`\``
    expect(
      resolveMarkdownImages(source, {
        localImage: () => 'chat-image://resolved'
      })
    ).toBe('![截图](chat-image://resolved)\n\n```md\n![示例](local:a.png)\n```')
  })

  it('rewrites knowledge images and preserves the optional title', () => {
    expect(
      resolveMarkdownImages('![图](kb:image_1 "标题")', {
        knowledgeImage: (id) => `https://example.test/${id}?a=1&b=2`
      })
    ).toBe('![图](https://example.test/image_1?a=1&b=2 "标题")')
  })
})
