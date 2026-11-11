import { describe, expect, it } from 'vitest'
import { formatAsciiLogArguments, LocalizedError, toReadableAsciiLogText } from './logging'

function isAsciiLogOutput(text: string): boolean {
  return [...text].every((character) => {
    const codePoint = character.codePointAt(0) ?? 0
    return (
      (codePoint >= 0x20 && codePoint <= 0x7e) ||
      codePoint === 0x09 ||
      codePoint === 0x0a ||
      codePoint === 0x0d ||
      codePoint === 0x1b
    )
  })
}

describe('main process logging', () => {
  it('keeps ASCII and ANSI intact while replacing untranslated localized text', () => {
    const input = '\x1b[31m当前目标不是具体窗口\x1b[0m\nready'
    const output = toReadableAsciiLogText(input)

    expect(output).toBe('\x1b[31m[localized text omitted from console; see UI]\x1b[0m\nready')
    expect(output).not.toContain('\\u')
    expect(isAsciiLogOutput(output)).toBe(true)
  })

  it('collapses adjacent localized characters into one readable marker', () => {
    expect(toReadableAsciiLogText('ok 😀中文 done')).toBe(
      'ok [localized text omitted from console; see UI] done'
    )
  })

  it('preserves LF and CRLF log line endings across platforms', () => {
    expect(toReadableAsciiLogText('first\nsecond\r\nthird')).toBe('first\nsecond\r\nthird')
  })

  it('uses locale-independent formatting without adding terminal colors', () => {
    const output = formatAsciiLogArguments([
      '%s: %o',
      '当前任务未获授权',
      { action: 'snapshot', attempt: 2 }
    ])

    expect(output).toContain('[localized text omitted from console; see UI]')
    expect(output).toContain("{ action: 'snapshot', attempt: 2 }")
    expect(output).not.toContain('\x1b[')
    expect(output).not.toContain('\\u')
    expect(isAsciiLogOutput(output)).toBe(true)
  })

  it('logs a readable English diagnostic for localized errors', () => {
    const error = new LocalizedError(
      'DESKTOP_AUTH_DENIED',
      '当前桌面任务未获授权',
      'Desktop automation authorization was denied; stop the task without retrying'
    )
    const output = formatAsciiLogArguments([
      "Error occurred in handler for 'automation:desktop':",
      error
    ])

    expect(output).toContain("Error occurred in handler for 'automation:desktop':")
    expect(output).toContain(
      'ClientError: Desktop automation authorization was denied; stop the task without retrying [DESKTOP_AUTH_DENIED]'
    )
    expect(output).toContain('logging.test.ts')
    expect(output).not.toContain('当前')
    expect(output).not.toContain('\\u')
    expect(isAsciiLogOutput(output)).toBe(true)
    expect(error.message).toBe('当前桌面任务未获授权')
  })

  it('uses a readable fallback for an untranslated localized error', () => {
    const output = formatAsciiLogArguments([new Error('未知中文错误')])

    expect(output).toContain('Error: [localized text omitted from console; see UI]')
    expect(output).toContain('logging.test.ts')
    expect(output).not.toContain('\\u')
    expect(isAsciiLogOutput(output)).toBe(true)
  })
})
