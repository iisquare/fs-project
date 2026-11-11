import { beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({
  mouse: {
    config: { autoDelayMs: 0 },
    releaseButton: vi.fn(async () => undefined),
    pressButton: vi.fn(async () => undefined),
    move: vi.fn(async () => undefined)
  }
}))

vi.mock('electron', () => ({
  desktopCapturer: { getSources: vi.fn() },
  nativeImage: { createFromPath: vi.fn() },
  screen: { getDisplayNearestPoint: vi.fn(), getDisplayMatching: vi.fn() },
  shell: { openExternal: vi.fn() },
  systemPreferences: {
    isTrustedAccessibilityClient: vi.fn(),
    getMediaAccessStatus: vi.fn()
  },
  net: {},
  protocol: { registerSchemesAsPrivileged: vi.fn() }
}))

vi.mock('@nut-tree-fork/nut-js', () => ({
  Button: { LEFT: 0, MIDDLE: 2, RIGHT: 1 },
  FileType: { PNG: 'png' },
  Key: {
    Escape: 0,
    Enter: 103,
    LeftControl: 80,
    LeftWin: 106,
    LeftCmd: 107,
    LeftAlt: 82,
    LeftShift: 104,
    Space: 116,
    Multiply: 48,
    Add: 70,
    Subtract: 49,
    Divide: 47,
    Decimal: 123,
    R: 13,
    A: 72
  },
  Region: class {
    left = 0
    top = 0
    width = 0
    height = 0
  },
  getActiveWindow: vi.fn(),
  getWindows: vi.fn(),
  keyboard: { config: { autoDelayMs: 0 } },
  mouse: mocks.mouse,
  screen: { captureRegion: vi.fn() },
  sleep: vi.fn(async () => undefined)
}))

import {
  DesktopAutomation,
  desktopCaptureErrorMessage,
  isDesktopPermissionFailure,
  keyCombination
} from './desktop-automation'

describe('DesktopAutomation capture diagnostics', () => {
  it('describes Windows capture failures without macOS permission instructions', () => {
    const failure = desktopCaptureErrorMessage('win32')

    expect(failure.message).toContain('Windows')
    expect(failure.message).toContain('屏幕已解锁')
    expect(failure.message).not.toMatch(/macOS|屏幕录制权限/i)
    expect(failure.log).toContain('Windows')
  })

  it('keeps macOS screen-recording guidance on macOS', () => {
    const failure = desktopCaptureErrorMessage('darwin')

    expect(failure.message).toContain('macOS 屏幕录制权限')
    expect(failure.log).toContain('screen-recording permission')
  })

  it('does not mention macOS for Linux capture failures', () => {
    const failure = desktopCaptureErrorMessage('linux')

    expect(failure.message).not.toMatch(/macOS|屏幕录制权限/i)
    expect(failure.log).not.toMatch(/macOS|screen-recording permission/i)
  })

  it('does not treat Windows capture errors as missing macOS permissions', () => {
    const error = new Error('desktop capture failed: check macOS screen recording permission')

    expect(isDesktopPermissionFailure(error, 'win32')).toBe(false)
    expect(isDesktopPermissionFailure(error, 'darwin')).toBe(true)
    expect(isDesktopPermissionFailure(error, 'linux')).toBe(false)
  })
})

describe('DesktopAutomation key parsing', () => {
  it('accepts Escape even though its native key code is zero', () => {
    expect(keyCombination('Escape', 'win32')).toEqual([0])
    expect(keyCombination('Esc', 'darwin')).toEqual([0])
  })

  it('accepts common Windows key names used by the agent', () => {
    expect(keyCombination('Win+R', 'win32')).toEqual([106, 13])
    expect(keyCombination('Windows+r', 'win32')).toEqual([106, 13])
    expect(keyCombination('Ctrl+Esc', 'win32')).toEqual([80, 0])
  })

  it('maps primary modifiers correctly on every desktop platform', () => {
    expect(keyCombination('Cmd+Space', 'darwin')).toEqual([107, 116])
    expect(keyCombination('Win+R', 'darwin')).toEqual([107, 13])
    expect(keyCombination('Meta+R', 'win32')).toEqual([106, 13])
    expect(keyCombination('Super+R', 'linux')).toEqual([106, 13])
  })

  it('accepts calculator symbols', () => {
    expect(keyCombination('*', 'win32')).toEqual([48])
    expect(keyCombination('+', 'darwin')).toEqual([70])
  })
})

describe('DesktopAutomation shutdown', () => {
  beforeEach(() => {
    mocks.mouse.releaseButton.mockClear()
  })

  it('does not synthesize mouse events when closing an idle desktop session', async () => {
    const automation = new DesktopAutomation()

    await automation.run({ action: 'close' })

    expect(mocks.mouse.releaseButton).not.toHaveBeenCalled()
  })

  it('does not synthesize mouse events when cancelling an unknown session', () => {
    const automation = new DesktopAutomation()

    automation.cancel('missing-session')

    expect(mocks.mouse.releaseButton).not.toHaveBeenCalled()
  })

  it('does not synthesize mouse events when no button is held during emergency stop', () => {
    const automation = new DesktopAutomation()

    automation.emergencyStop()

    expect(mocks.mouse.releaseButton).not.toHaveBeenCalled()
  })
})
