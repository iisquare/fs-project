import { formatWithOptions } from 'util'

type ConsoleMethod = 'log' | 'info' | 'warn' | 'error' | 'debug'

const CONSOLE_METHODS: ConsoleMethod[] = ['log', 'info', 'warn', 'error', 'debug']
const LOCALIZED_TEXT_PLACEHOLDER = '[localized text omitted from console; see UI]'

let installed = false

/**
 * An error whose UI message stays localized while its console representation stays readable.
 *
 * IPC always receives `message`. Main-process logs use `logMessage` and `code` instead, so a GBK
 * log collector never has to decode localized text.
 */
export class LocalizedError extends Error {
  readonly code: string
  readonly logMessage: string

  constructor(code: string, message: string, logMessage: string) {
    super(message)
    this.name = 'ClientError'
    this.code = code
    this.logMessage = logMessage
  }
}

function isLocalizedError(error: Error): error is LocalizedError {
  return (
    error instanceof LocalizedError &&
    /^[A-Z0-9_]+$/.test(error.code) &&
    /^[\x20-\x7e]+$/.test(error.logMessage)
  )
}

function formatErrorForLog(error: Error): Error | string {
  if (!isLocalizedError(error)) return error
  const stackLines = (error.stack ?? '').split(/\r?\n/)
  const header = `${error.name}: ${error.logMessage} [${error.code}]`
  return stackLines.length > 1 ? `${header}\n${stackLines.slice(1).join('\n')}` : header
}

/**
 * Keep process logs readable and inside ASCII.
 *
 * Localized text that lacks an English logging counterpart is replaced with one explicit marker.
 * Reversible `\uXXXX` output is intentionally not used: unreadable escapes are not useful logs.
 */
export function toReadableAsciiLogText(text: string): string {
  let output = ''
  let omittedLocalizedText = false
  for (const character of text) {
    const codePoint = character.codePointAt(0) ?? 0
    const printableAscii = codePoint >= 0x20 && codePoint <= 0x7e
    const controlSequence =
      codePoint === 0x09 || codePoint === 0x0a || codePoint === 0x0d || codePoint === 0x1b
    if (printableAscii || controlSequence) {
      output += character
      omittedLocalizedText = false
    } else if (!omittedLocalizedText) {
      output += LOCALIZED_TEXT_PLACEHOLDER
      omittedLocalizedText = true
    }
  }
  return output
}

export function formatAsciiLogArguments(args: readonly unknown[]): string {
  // Keep formatting deterministic across Windows, macOS, Linux, CI streams and redirected files.
  // Existing application-provided ANSI sequences are preserved by toReadableAsciiLogText.
  const readableArgs = args.map((argument) =>
    argument instanceof Error ? formatErrorForLog(argument) : argument
  )
  return toReadableAsciiLogText(formatWithOptions({ colors: false }, ...readableArgs))
}

/**
 * Install once, before any IPC handlers are registered.
 *
 * Console methods are the final JavaScript boundary used by both application code and Electron's
 * default IPC error reporter. Formatting here also preserves error stacks and ANSI colors while
 * replacing untranslated localized text with one explicit, readable marker.
 */
export function installAsciiConsoleLogging(): void {
  if (installed) return
  installed = true

  for (const method of CONSOLE_METHODS) {
    const original = console[method].bind(console)
    console[method] = (...args: unknown[]): void => {
      original(formatAsciiLogArguments(args))
    }
  }
}
