import { describe, expect, it } from 'vitest'
import { normalizeDesktopApprovalPolicy } from './desktop-policy'

describe('normalizeDesktopApprovalPolicy', () => {
  it('only enables automatic desktop approval for the explicit auto value', () => {
    expect(normalizeDesktopApprovalPolicy('auto')).toBe('auto')
  })

  it('falls back to requesting approval for missing or invalid values', () => {
    expect(normalizeDesktopApprovalPolicy('ask')).toBe('ask')
    expect(normalizeDesktopApprovalPolicy('full')).toBe('ask')
    expect(normalizeDesktopApprovalPolicy(undefined)).toBe('ask')
  })
})
