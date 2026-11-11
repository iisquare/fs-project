import { describe, expect, it } from 'vitest'
import { DESKTOP_EMERGENCY_SHORTCUT, desktopEmergencyShortcutLabel } from './desktop'

describe('desktop emergency shortcut', () => {
  it('does not reserve Escape globally', () => {
    expect(DESKTOP_EMERGENCY_SHORTCUT).toBe('CommandOrControl+Alt+Shift+K')
    expect(DESKTOP_EMERGENCY_SHORTCUT).not.toMatch(/Escape|Esc/i)
  })

  it('shows the native modifier names on each desktop platform', () => {
    expect(desktopEmergencyShortcutLabel('win32')).toBe('Ctrl+Alt+Shift+K')
    expect(desktopEmergencyShortcutLabel('linux')).toBe('Ctrl+Alt+Shift+K')
    expect(desktopEmergencyShortcutLabel('darwin')).toBe('⌘⌥⇧K')
  })
})
