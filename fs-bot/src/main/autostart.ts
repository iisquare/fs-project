import { app } from 'electron'

/**
 * Windows Run 项的名称。不要改用 app.getName() 之类的动态值：
 * 安装、升级和卸载脚本都按这个名称精确清理。
 */
export const AUTOSTART_ITEM_NAME = 'fs-bot'
export const AUTOSTART_ARG = '--autostart'

export interface AutoStartStatus {
  /** 当前环境和平台能否管理系统登录项 */
  supported: boolean
  /** 登录项是否指向当前这个可执行文件与固定参数 */
  registered: boolean
  /** 系统是否真的会在登录时启动它（Windows 可在“启动应用”中单独禁用） */
  enabled: boolean
  /** 给设置页展示的状态说明 */
  reason: string
}

function platformName(): string {
  if (process.platform === 'win32') return 'Windows'
  if (process.platform === 'darwin') return 'macOS'
  return process.platform
}

function readLoginItem(): Electron.LoginItemSettings {
  if (process.platform === 'win32') {
    return app.getLoginItemSettings({
      path: process.execPath
    })
  }
  return app.getLoginItemSettings()
}

function sameExecutable(left: string, right: string): boolean {
  return left.replace(/\//g, '\\').toLowerCase() === right.replace(/\//g, '\\').toLowerCase()
}

/**
 * 开机启动只允许在已打包版本里注册。
 *
 * 开发模式的 process.execPath 是 Electron 运行时，直接写进系统登录项后，
 * 机器重启时既找不到应用的 main 入口，也可能打开一个空白 Electron 窗口。
 */
function unsupportedReason(): string {
  if (!app.isPackaged) {
    return '开发模式不会注册开机启动，请使用安装后的正式版本。'
  }
  return `${platformName()} 暂不支持自动管理开机启动项。`
}

function isSupported(): boolean {
  return app.isPackaged && (process.platform === 'win32' || process.platform === 'darwin')
}

function statusFrom(
  settings: Electron.LoginItemSettings | null,
  supported: boolean
): AutoStartStatus {
  if (!supported || !settings) {
    return {
      supported: false,
      registered: false,
      enabled: false,
      reason: unsupportedReason()
    }
  }

  if (process.platform === 'win32') {
    /**
     * Electron 39 在启动项带参数时，launchItems.args 仍可能回读为空，
     * 因此不能靠 openAtLogin（路径 + 参数完全匹配）判断。
     * 启动项名与可执行文件路径由本应用独占，按这两项匹配最稳定。
     */
    const item = settings.launchItems.find(
      (launchItem) =>
        launchItem.name === AUTOSTART_ITEM_NAME && sameExecutable(launchItem.path, process.execPath)
    )
    const registered = item !== undefined
    const enabled = item?.enabled === true
    return {
      supported: true,
      registered,
      enabled,
      reason: enabled
        ? '系统登录后会启动安装目录中的 fs-bot。'
        : registered
          ? '启动项已写入，但被系统“启动应用”设置禁用，请在 Windows 设置中允许 fs-bot。'
          : '尚未注册系统开机启动项。'
    }
  }

  const registered = settings.openAtLogin === true
  const needsApproval = settings.status === 'requires-approval'
  const enabled = registered && !needsApproval
  return {
    supported: true,
    registered,
    enabled,
    reason: enabled
      ? '系统登录后会启动安装目录中的 fs-bot。'
      : needsApproval
        ? 'macOS 已登记 fs-bot，但仍需在“系统设置 > 通用 > 登录项”中允许。'
        : '尚未允许 fs-bot 在登录时启动。'
  }
}

export function getAutoStartStatus(): AutoStartStatus {
  const supported = isSupported()
  return statusFrom(supported ? readLoginItem() : null, supported)
}

function writeLoginItem(openAtLogin: boolean): void {
  if (process.platform === 'win32') {
    app.setLoginItemSettings({
      openAtLogin,
      enabled: openAtLogin,
      path: process.execPath,
      args: [AUTOSTART_ARG],
      name: AUTOSTART_ITEM_NAME
    })
    return
  }
  app.setLoginItemSettings({ openAtLogin })
}

/**
 * 用户主动切换。写入后回读系统状态，界面拿到的不是“配置里写了什么”，
 * 而是操作系统是否真的注册成功。
 */
export function setAutoStart(openAtLogin: boolean): AutoStartStatus {
  if (!isSupported()) throw new Error(unsupportedReason())

  writeLoginItem(openAtLogin)
  const status = getAutoStartStatus()
  if (openAtLogin && !status.registered) {
    throw new Error('系统未能写入开机启动项，请检查当前用户的系统权限。')
  }
  if (!openAtLogin && status.registered) {
    throw new Error('系统未能移除开机启动项，请检查当前用户的系统权限。')
  }
  return status
}
