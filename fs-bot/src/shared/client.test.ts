import { describe, expect, it } from 'vitest'
import {
  CLIENT_UPGRADE_CODE,
  ClientUpgradeRequiredError,
  clientRequestHeaders,
  clientResultCode,
  isClientUpgradeRequired,
  isClientUpgradeResponse,
  isClientUpgradeResult
} from './client'

describe('client request contract', () => {
  it('keeps all mandatory client headers in one place', () => {
    expect(
      clientRequestHeaders({
        clientVersion: '1.2.3',
        platform: 'win32',
        architecture: 'x64',
        clientId: 'client-1',
        channel: 'stable'
      })
    ).toEqual({
      'x-client-version': '1.2.3',
      'x-client-platform': 'win32',
      'x-client-arch': 'x64',
      'x-client-id': 'client-1',
      'x-client-channel': 'stable'
    })
  })

  it('recognizes upgrade codes from structured responses', () => {
    expect(clientResultCode({ code: '90426' })).toBe(CLIENT_UPGRADE_CODE)
    expect(isClientUpgradeResult({ code: CLIENT_UPGRADE_CODE, message: 'upgrade' })).toBe(true)
    expect(isClientUpgradeResult({ code: 0 })).toBe(false)
    expect(isClientUpgradeResult(null)).toBe(false)
    expect(isClientUpgradeResponse(426, null)).toBe(true)
    expect(isClientUpgradeResponse(200, { code: CLIENT_UPGRADE_CODE })).toBe(true)
    expect(isClientUpgradeResponse(500, { code: 0 })).toBe(false)
  })

  it('recognizes the upgrade error across the IPC boundary', () => {
    const error = new ClientUpgradeRequiredError('upgrade', 426)
    expect(isClientUpgradeRequired(error)).toBe(true)
    expect(isClientUpgradeRequired({ code: CLIENT_UPGRADE_CODE })).toBe(true)
    expect(isClientUpgradeRequired(new Error('network'))).toBe(false)
  })
})
