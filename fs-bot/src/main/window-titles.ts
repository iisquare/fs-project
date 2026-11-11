type KoffiModule = typeof import('koffi').default

export interface ReadWindowsWindowTitlesOptions {
  platform?: NodeJS.Platform
}

interface WindowsTitleApi {
  koffi: KoffiModule
  getWindowTextLength: (handle: number) => number
  getWindowText: (handle: number, buffer: unknown, maxLength: number) => number
}

const UNREADABLE_TITLE = '\ufffd'
let windowsTitleApi: Promise<WindowsTitleApi> | null = null

function validHandle(value: unknown): number | null {
  const handle = Number(value)
  return Number.isSafeInteger(handle) && handle > 0 ? handle : null
}

async function loadWindowsTitleApi(): Promise<WindowsTitleApi> {
  if (windowsTitleApi) return windowsTitleApi

  windowsTitleApi = (async () => {
    // Koffi ships N-API prebuilds for Windows x64/arm64. Keep it lazy so macOS and
    // Linux never load the Windows-only user32 bindings.
    const imported = await import('koffi')
    const koffi: KoffiModule = imported.default
    const user32 = koffi.load('user32.dll')
    return {
      koffi,
      getWindowTextLength: user32.func(
        'int __stdcall GetWindowTextLengthW(uintptr_t hWnd)'
      ) as WindowsTitleApi['getWindowTextLength'],
      getWindowText: user32.func(
        'int __stdcall GetWindowTextW(uintptr_t hWnd, _Out_ uint16_t *text, int maxCount)'
      ) as WindowsTitleApi['getWindowText']
    }
  })().catch((error) => {
    windowsTitleApi = null
    throw error
  })
  return windowsTitleApi
}

function decodeUtf16(units: Uint16Array): string {
  if (!units.length) return ''
  return Buffer.from(units.buffer, units.byteOffset, units.byteLength).toString('utf16le')
}

export async function collectWindowTitles(
  handles: readonly unknown[],
  readTitle: (handle: number) => string
): Promise<Map<number, string>> {
  const ids = [...new Set(handles.map(validHandle).filter((handle): handle is number => !!handle))]
  const titles = new Map<number, string>()
  for (const handle of ids) {
    try {
      const title = readTitle(handle)
      if (title) titles.set(handle, title)
    } catch {
      // A window can disappear between enumeration and reading its title.
    }
  }
  return titles
}

/**
 * libnut 2.7.5 在 Windows 上通过 ANSI GetWindowText 读取标题，非 ASCII 字符会变成
 * U+FFFD 或其它不可逆乱码。这里按窗口句柄直接调用 Win32 Unicode API，其它平台
 * 保持原生实现不变。
 */
export async function readWindowsWindowTitles(
  handles: readonly unknown[],
  options: ReadWindowsWindowTitlesOptions = {}
): Promise<Map<number, string>> {
  const platform = options.platform ?? process.platform
  if ('win32' !== platform) return new Map()

  const { koffi, getWindowTextLength, getWindowText } = await loadWindowsTitleApi()
  return collectWindowTitles(handles, (handle) => {
    // A window may disappear between libnut enumeration and this read.
    const length = getWindowTextLength(handle)
    if (length <= 0) return ''

    const buffer = koffi.alloc('uint16_t', length + 1)
    try {
      const copied = getWindowText(handle, buffer, length + 1)
      if (copied <= 0) return ''
      const units = koffi.decode(buffer, 'uint16_t', Math.min(copied, length)) as Uint16Array
      return decodeUtf16(units)
    } finally {
      koffi.free(buffer)
    }
  })
}

/** 无 Unicode 标题可用时，避免把含替换字符的乱码继续交给模型。 */
export function usableWindowTitle(title: string): string {
  return title.includes(UNREADABLE_TITLE) ? '（窗口标题不可读取）' : title
}
