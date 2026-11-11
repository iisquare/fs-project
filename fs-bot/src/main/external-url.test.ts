import { describe, expect, it } from 'vitest'
import { externalUrl } from './external-url'

describe('externalUrl', () => {
  it('allows web and mail links', () => {
    expect(externalUrl('https://example.com/path?q=1')).toBe('https://example.com/path?q=1')
    expect(externalUrl('http://example.com')).toBe('http://example.com/')
    expect(externalUrl('mailto:support@example.com')).toBe('mailto:support@example.com')
  })

  it('blocks local, custom and malformed URLs', () => {
    expect(externalUrl('file:///etc/passwd')).toBeNull()
    expect(externalUrl('x-apple.systempreferences:com.apple.preference.security')).toBeNull()
    expect(externalUrl('javascript:alert(1)')).toBeNull()
    expect(externalUrl('not a url')).toBeNull()
    expect(externalUrl(undefined)).toBeNull()
  })
})
