import { ref } from 'vue'
import { defineStore } from 'pinia'
import Config from '@renderer/core/Config'

export const useSettingsStore = defineStore('settings', () => {
  const autoStart = ref(false)
  const autoUpgrade = ref(false)
  /** 工作区外文件的访问策略：ask 请求批准、auto 帮我批准、deny 自动拒绝、full 完全访问权限 */
  const workspacePolicy = ref('ask')
  const loading = ref(false)

  async function fetchSettings() {
    loading.value = true
    try {
      const settings = await Config.getSettings()
      autoStart.value = settings.autoStart === true
      autoUpgrade.value = settings.autoUpgrade === true
      workspacePolicy.value = String(settings.workspacePolicy ?? 'ask')
    } finally {
      loading.value = false
    }
  }

  async function toggleAutoStart(val: boolean) {
    await Config.updateSetting('autoStart', val)
    autoStart.value = val
  }

  async function toggleAutoUpgrade(val: boolean) {
    await Config.updateSetting('autoUpgrade', val)
    autoUpgrade.value = val
  }

  /** 切换访问策略：策略由主进程执行与持久化，这里只发起并回读实际生效的取值 */
  async function setWorkspacePolicy(val: string) {
    workspacePolicy.value = await window.ipc.workspace.setPolicy(val as any)
  }

  return {
    autoStart,
    autoUpgrade,
    workspacePolicy,
    loading,
    fetchSettings,
    toggleAutoStart,
    toggleAutoUpgrade,
    setWorkspacePolicy
  }
})
