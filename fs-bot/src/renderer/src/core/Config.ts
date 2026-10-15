/** 设置项取值：与主进程 config.toml 的原生标量一致 */
export type SettingValue = string | number | boolean

/**
 * 系统级配置读写（userdata/config.toml，由主进程维护）
 */
export default {
  async getSettings(): Promise<Record<string, SettingValue>> {
    return window.ipc.config.getSettings()
  },

  async updateSetting(key: string, value: SettingValue) {
    return window.ipc.config.updateSetting(key, value)
  },

  async listLoginHistory() {
    return window.ipc.config.listLoginHistory()
  },

  async saveLoginHistory(serial: string) {
    return window.ipc.config.saveLoginHistory(serial)
  },

  async removeLoginHistory(serial: string) {
    return window.ipc.config.removeLoginHistory(serial)
  }
}
