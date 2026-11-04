import { describe, expect, it } from 'vitest'
import { normalizeWebUrl, safeWebPageUrl } from './web-url'

describe('normalizeWebUrl', () => {
  it('adds https for ordinary domains', () => {
    expect(normalizeWebUrl('example.com/path')).toBe('https://example.com/path')
  })

  it('uses http for loopback development servers', () => {
    expect(normalizeWebUrl('localhost:5173/app')).toBe('http://localhost:5173/app')
  })

  it('treats a host port as a host, not as a protocol', () => {
    expect(normalizeWebUrl('example.com:8443/path')).toBe('https://example.com:8443/path')
  })

  it.each([
    'file:///C:/Windows/win.ini',
    'data:text/plain,secret',
    'javascript:alert(1)',
    'ftp://example.com/file'
  ])('rejects non-web protocol %s', (url) => {
    expect(() => normalizeWebUrl(url)).toThrow('只允许打开 http / https')
  })
})

describe('safeWebPageUrl', () => {
  it('allows web pages and the initial blank page', () => {
    expect(safeWebPageUrl('about:blank')).toBe(true)
    expect(safeWebPageUrl('https://example.com')).toBe(true)
    expect(safeWebPageUrl('http://localhost:3000')).toBe(true)
  })

  it.each(['file:///C:/secret.txt', 'data:text/html,secret', 'javascript:alert(1)'])(
    'rejects unsafe final page %s',
    (url) => {
      expect(safeWebPageUrl(url)).toBe(false)
    }
  )
})
