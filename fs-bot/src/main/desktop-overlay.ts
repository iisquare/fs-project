import { randomUUID } from 'crypto'
import { join } from 'path'
import { BrowserWindow, screen, type WebContents } from 'electron'
import { desktopEmergencyShortcutLabel } from '../shared/desktop'

const WIDTH = 430
const HEIGHT = 48
const MARGIN_TOP = 18

function overlayHtml(): string {
  const nonce = randomUUID()
  const shortcut = desktopEmergencyShortcutLabel(process.platform)
  return `<!doctype html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <title>__fs_bot_desktop_overlay__</title>
    <meta
      http-equiv="Content-Security-Policy"
      content="default-src 'none'; style-src 'unsafe-inline'; script-src 'nonce-${nonce}'"
    />
    <style>
      * { box-sizing: border-box; }
      html, body {
        width: 100%;
        height: 100%;
        margin: 0;
        overflow: hidden;
        background: transparent;
        pointer-events: none;
        user-select: none;
        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
      }
      .pill {
        display: flex;
        align-items: center;
        gap: 10px;
        height: 42px;
        margin: 3px 12px;
        padding: 0 14px;
        color: #fff;
        background: rgba(18, 24, 38, 0.94);
        border: 1px solid rgba(255, 255, 255, 0.12);
        border-radius: 21px;
        box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
      }
      .pulse {
        position: relative;
        flex: none;
        width: 9px;
        height: 9px;
        border-radius: 50%;
        background: #4ade80;
        box-shadow: 0 0 0 5px rgba(74, 222, 128, 0.14);
        animation: pulse 1.6s ease-in-out infinite;
      }
      .title {
        min-width: 0;
        flex: 1;
        font-size: 13px;
        font-weight: 600;
        white-space: nowrap;
      }
      .stop {
        flex: none;
        display: inline-flex;
        align-items: center;
        gap: 7px;
        height: 30px;
        padding: 0 10px 0 12px;
        color: #ffe4e6;
        background: rgba(225, 29, 72, 0.2);
        border: 1px solid rgba(251, 113, 133, 0.42);
        border-radius: 15px;
        font: inherit;
        font-size: 11px;
        font-weight: 600;
        line-height: 1;
        white-space: nowrap;
        cursor: pointer;
        pointer-events: auto;
        transition:
          background-color 120ms ease,
          border-color 120ms ease,
          transform 80ms ease;
      }
      .stop:hover {
        background: rgba(225, 29, 72, 0.36);
        border-color: rgba(251, 113, 133, 0.72);
      }
      .stop:active { transform: scale(0.97); }
      .stop:focus-visible {
        outline: 2px solid #fda4af;
        outline-offset: 2px;
      }
      .stop:disabled {
        cursor: wait;
        opacity: 0.68;
      }
      .stop kbd {
        min-width: 24px;
        padding: 3px 5px;
        color: rgba(255, 255, 255, 0.78);
        background: rgba(255, 255, 255, 0.1);
        border: 1px solid rgba(255, 255, 255, 0.14);
        border-radius: 5px;
        font: inherit;
        font-size: 10px;
        font-weight: 500;
        text-align: center;
      }
      @keyframes pulse {
        0%, 100% { opacity: 0.65; transform: scale(0.86); }
        50% { opacity: 1; transform: scale(1); }
      }
      @media (prefers-reduced-motion: reduce) {
        .pulse { animation: none; }
        .stop { transition: none; }
      }
    </style>
  </head>
  <body>
    <div class="pill">
      <span class="pulse"></span>
      <span class="title">fs-bot 正在托管此电脑</span>
      <button
        id="stop"
        class="stop"
        type="button"
        title="紧急停止桌面操作（${shortcut}）"
        aria-label="紧急停止桌面操作，快捷键 ${shortcut}"
      >
        <span class="stop-text">紧急停止</span>
        <kbd aria-hidden="true">${shortcut}</kbd>
      </button>
    </div>
    <script nonce="${nonce}">
      (() => {
        const stopButton = document.getElementById('stop')
        const stopText = stopButton && stopButton.querySelector('.stop-text')
        const api = window.desktopOverlay
        let interactive = false
        let stopping = false

        const setInteractive = (next) => {
          if (interactive === next) return
          interactive = next
          api?.setInteractive(next)
        }

        document.addEventListener(
          'mousemove',
          (event) => setInteractive(Boolean(event.target?.closest?.('#stop'))),
          { passive: true }
        )
        stopButton?.addEventListener('pointerenter', () => setInteractive(true))
        stopButton?.addEventListener('pointerleave', () => setInteractive(false))
        document.addEventListener('mouseleave', () => setInteractive(false))
        window.addEventListener('blur', () => setInteractive(false))
        document.addEventListener('visibilitychange', () => {
          if (document.hidden) setInteractive(false)
        })
        api?.onReset(() => {
          stopping = false
          if (stopButton) stopButton.disabled = false
          if (stopText) stopText.textContent = '紧急停止'
          setInteractive(false)
        })

        stopButton?.addEventListener('click', (event) => {
          event.preventDefault()
          event.stopPropagation()
          if (stopping) return
          stopping = true
          stopButton.disabled = true
          if (stopText) stopText.textContent = '正在停止'
          setInteractive(true)
          api?.stop()
        })
      })()
    </script>
  </body>
</html>`
}

/**
 * 桌面操作期间的置顶状态条。
 *
 * 窗口点击穿透、不抢焦点，并使用内容保护从系统截图中排除，避免状态提示本身遮挡模型看到的画面。
 * 桌面 Agent 全局串行，因此整个应用只需要一个状态窗口。
 */
class DesktopOverlay {
  private window: BrowserWindow | null = null
  private stopHandler: (() => void) | null = null

  setStopHandler(handler: () => void): void {
    this.stopHandler = handler
  }

  setInteractive(sender: WebContents, interactive: boolean): void {
    const window = this.window
    if (!window || window.isDestroyed() || window.webContents !== sender) return
    window.setIgnoreMouseEvents(!interactive, { forward: true })
  }

  requestStop(sender: WebContents): void {
    const window = this.window
    if (!window || window.isDestroyed() || window.webContents !== sender) return
    this.stopHandler?.()
  }

  show(): void {
    if (!this.window || this.window.isDestroyed()) {
      this.window = new BrowserWindow({
        title: '__fs_bot_desktop_overlay__',
        width: WIDTH,
        height: HEIGHT,
        frame: false,
        transparent: true,
        resizable: false,
        movable: false,
        minimizable: false,
        maximizable: false,
        closable: false,
        focusable: false,
        skipTaskbar: true,
        alwaysOnTop: true,
        show: false,
        hasShadow: false,
        backgroundColor: '#00000000',
        webPreferences: {
          nodeIntegration: false,
          contextIsolation: true,
          sandbox: true,
          preload: join(__dirname, '../preload/desktop-overlay.js')
        }
      })
      this.window.setIgnoreMouseEvents(true, { forward: true })
      this.window.setAlwaysOnTop(true, 'floating')
      this.window.setContentProtection(true)
      if ('darwin' === process.platform) {
        this.window.setVisibleOnAllWorkspaces(true, {
          visibleOnFullScreen: true,
          skipTransformProcessType: true
        })
      }
      this.window.webContents.setWindowOpenHandler(() => ({ action: 'deny' }))
      this.window.webContents.on('will-navigate', (event) => event.preventDefault())
      void this.window.loadURL(`data:text/html;charset=utf-8,${encodeURIComponent(overlayHtml())}`)
    }
    this.window.setIgnoreMouseEvents(true, { forward: true })
    this.position()
    if (!this.window.isVisible()) this.window.showInactive()
  }

  hide(): void {
    if (!this.window || this.window.isDestroyed()) return
    this.window.setIgnoreMouseEvents(true, { forward: true })
    this.window.webContents.send('desktop-overlay:reset')
    this.window.hide()
  }

  destroy(): void {
    if (this.window && !this.window.isDestroyed()) this.window.destroy()
    this.window = null
  }

  private position(): void {
    if (!this.window || this.window.isDestroyed()) return
    const display = screen.getDisplayNearestPoint(screen.getCursorScreenPoint())
    const area = display.workArea
    this.window.setBounds({
      x: Math.round(area.x + (area.width - WIDTH) / 2),
      y: Math.round(area.y + MARGIN_TOP),
      width: WIDTH,
      height: HEIGHT
    })
  }
}

export const desktopOverlay = new DesktopOverlay()
