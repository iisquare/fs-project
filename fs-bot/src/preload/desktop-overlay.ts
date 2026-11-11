import { contextBridge, ipcRenderer } from 'electron'

/**
 * 桌面状态条只暴露按钮命中切换、紧急停止和隐藏后的界面重置。
 * 不把 ipcRenderer 或其他应用能力带进这个独立窗口。
 */
contextBridge.exposeInMainWorld('desktopOverlay', {
  setInteractive: (interactive: boolean): void => {
    ipcRenderer.send('desktop-overlay:set-interactive', interactive === true)
  },
  stop: (): void => {
    ipcRenderer.send('desktop-overlay:stop')
  },
  onReset: (callback: () => void): void => {
    ipcRenderer.on('desktop-overlay:reset', () => callback())
  }
})
