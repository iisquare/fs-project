import { randomUUID } from 'crypto'
import { existsSync, mkdirSync, readFileSync, renameSync, writeFileSync } from 'fs'
import { dirname, join } from 'path'
import { parse, stringify } from 'smol-toml'

/**
 * 系统级配置（原 system.db 中的 system_config 表，改为 <运行目录>/userdata/config.toml 存储；
 * 运行目录见 main/index.ts：开发模式是项目根目录，生产环境是用户主目录下的 .fs-bot）
 *
 * 文件结构（TOML，可手工修改，应用写入时按规范格式重写）：
 * loginHistory = [ "admin" ]
 *
 * [settings]
 * autoUpgrade = true
 * workspacePolicy = "ask"
 */

/** 设置项取值：与 TOML 原生标量一致 */
type SettingValue = string | number | boolean

const CONFIG_FILE = 'config.toml'

const FILE_HEADER = `# fs-bot 系统配置（应用自动维护，手工修改后重启应用生效）
`

/**
 * 已登记的设置项：用默认值确定类型，读取时按该类型归一化
 * 默认值类型就是写入文件时的类型（false 对应 TOML 布尔值）
 */
const SETTINGS_SCHEMA: Record<string, SettingValue> = {
  autoUpgrade: true,
  /** 本机安装标识：灰度发布分桶与更新统计，不随服务端账号变化 */
  clientId: '',
  /** 更新渠道：服务端只返回同渠道的已启用版本 */
  updateChannel: 'stable',
  /** 工作区外文件的访问策略，见 WorkspaceManager */
  workspacePolicy: 'ask'
}

interface ConfigData {
  settings: Record<string, SettingValue>
  loginHistory: string[]
}

export class ConfigStore {
  private static instance: ConfigStore

  private filePath = ''
  private data: ConfigData = this.normalize(null)

  static getInstance(): ConfigStore {
    if (!ConfigStore.instance) {
      ConfigStore.instance = new ConfigStore()
    }
    return ConfigStore.instance
  }

  get path(): string {
    return this.filePath
  }

  /**
   * 初始化配置文件。
   * 文件缺失或损坏时按默认值写入；正常文件只读取不重写，避免破坏手工排版与注释。
   */
  init(runtimeDir: string): void {
    const dataDir = join(runtimeDir, 'userdata')
    mkdirSync(dataDir, { recursive: true })
    this.filePath = join(dataDir, CONFIG_FILE)

    if (!existsSync(this.filePath)) {
      this.data = this.normalize(null)
      this.write()
    } else {
      const text = readFileSync(this.filePath, 'utf8')
      try {
        this.data = this.normalize(parse(text))
      } catch (e) {
        // 文件被手工改坏时保留原文件，改用默认值继续运行
        const backupPath = this.filePath + '.bak'
        try {
          renameSync(this.filePath, backupPath)
          console.warn(`[Config] Invalid config file, backup to ${backupPath}:`, e)
        } catch (err) {
          console.warn('[Config] Failed to read config file, fallback to defaults:', err)
        }
        this.data = this.normalize(null)
        this.write()
      }
    }
    // 开机启动由操作系统登录项管理，不再保存一份可能与系统状态漂移的配置。
    this.removeSetting('autoStart')
    if (!this.data.settings.clientId) {
      this.data.settings.clientId = randomUUID()
      this.write()
    }
    console.log('[Config] Config file ready:', this.filePath)
  }

  /** 归一化文件内容：登记的设置项按默认值类型转换，登录历史仅保留字符串项 */
  private normalize(raw: Record<string, unknown> | null): ConfigData {
    const settings: Record<string, SettingValue> = { ...SETTINGS_SCHEMA }
    const rawSettings = (raw?.['settings'] ?? {}) as Record<string, unknown>
    for (const [key, value] of Object.entries(rawSettings)) {
      const normalized = this.parseSetting(key, value)
      if (normalized !== undefined) settings[key] = normalized
    }
    const rawHistory = raw?.['loginHistory']
    return {
      settings,
      loginHistory: Array.isArray(rawHistory)
        ? rawHistory.filter((serial): serial is string => typeof serial === 'string')
        : []
    }
  }

  /**
   * 转换单个设置项。
   * 已登记项按默认值类型转换（兼容 "false" 这类字符串写法）；未登记项原样保留标量值。
   */
  private parseSetting(key: string, value: unknown): SettingValue | undefined {
    if (value === null || typeof value === 'object' || typeof value === 'undefined')
      return undefined
    if (typeof value !== 'string' && typeof value !== 'number' && typeof value !== 'boolean') {
      return undefined
    }
    const expect = SETTINGS_SCHEMA[key]
    if (typeof expect === 'boolean') {
      if (typeof value === 'boolean') return value
      const text = String(value).trim().toLowerCase()
      if (text === 'true') return true
      if (text === 'false') return false
      return expect
    }
    if (typeof expect === 'number') {
      const num = Number(value)
      return Number.isFinite(num) ? num : expect
    }
    if (typeof expect === 'string') return String(value)
    return value
  }

  private serialize(): string {
    return FILE_HEADER + stringify(this.data)
  }

  /** 先写临时文件再改名，避免写入过程中断导致配置损坏 */
  private write(): void {
    if (!this.filePath) return
    mkdirSync(dirname(this.filePath), { recursive: true })
    const tmpPath = this.filePath + '.tmp'
    writeFileSync(tmpPath, this.serialize(), 'utf8')
    renameSync(tmpPath, this.filePath)
  }

  private ctx(): ConfigData {
    if (!this.filePath) throw new Error('Config file not initialized')
    return this.data
  }

  getSettings(): Record<string, SettingValue> {
    return { ...this.ctx().settings }
  }

  updateSetting(key: string, value: SettingValue): void {
    const normalized = this.parseSetting(key, value)
    if (normalized === undefined) return
    this.ctx().settings[key] = normalized
    this.write()
  }

  private removeSetting(key: string): boolean {
    if (!(key in this.ctx().settings)) return false
    delete this.ctx().settings[key]
    this.write()
    return true
  }

  /** 最近使用的账号排在最前 */
  listLoginHistory(): string[] {
    return [...this.ctx().loginHistory]
  }

  saveLoginHistory(serial: string): string[] {
    const data = this.ctx()
    data.loginHistory = [serial, ...data.loginHistory.filter((item) => item !== serial)]
    this.write()
    return this.listLoginHistory()
  }

  removeLoginHistory(serial: string): string[] {
    const data = this.ctx()
    data.loginHistory = data.loginHistory.filter((item) => item !== serial)
    this.write()
    return this.listLoginHistory()
  }
}
