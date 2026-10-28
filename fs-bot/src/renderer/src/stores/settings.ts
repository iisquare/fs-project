import { ref } from 'vue'
import { defineStore } from 'pinia'
import Config from '@renderer/core/Config'

type WorkspacePolicy = 'ask' | 'auto' | 'deny' | 'full'

export const useSettingsStore = defineStore('settings', () => {
  const autoStart = ref(false)
  const autoStartSupported = ref(false)
  const autoStartReason = ref('')
  const autoStartSaving = ref(false)
  const autoUpgrade = ref(false)
  const updateChannel = ref('stable')
  /** 工作区外文件的访问策略：ask 请求批准、auto 帮我批准、deny 自动拒绝、full 完全访问权限 */
  const workspacePolicy = ref('ask')
  const loading = ref(false)

  async function fetchSettings(): Promise<void> {
    loading.value = true
    try {
      const [settings, autoStartStatus] = await Promise.all([
        Config.getSettings(),
        window.ipc.autoStart.get()
      ])
      autoStart.value = autoStartStatus.enabled
      autoStartSupported.value = autoStartStatus.supported
      autoStartReason.value = autoStartStatus.reason
      autoUpgrade.value = settings.autoUpgrade === true
      updateChannel.value = String(settings.updateChannel ?? 'stable')
      workspacePolicy.value = String(settings.workspacePolicy ?? 'ask')
    } finally {
      loading.value = false
    }
  }

  async function toggleAutoStart(val: boolean): Promise<void> {
    autoStartSaving.value = true
    try {
      const status = await window.ipc.autoStart.set(val)
      autoStart.value = status.enabled
      autoStartSupported.value = status.supported
      autoStartReason.value = status.reason
    } finally {
      autoStartSaving.value = false
    }
  }

  async function toggleAutoUpgrade(val: boolean): Promise<void> {
    await Config.updateSetting('autoUpgrade', val)
    autoUpgrade.value = val
  }

  async function setUpdateChannel(val: string): Promise<void> {
    await Config.updateSetting('updateChannel', val)
    updateChannel.value = val
  }

  /** 切换访问策略：策略由主进程执行与持久化，这里只发起并回读实际生效的取值 */
  async function setWorkspacePolicy(val: string): Promise<void> {
    workspacePolicy.value = await window.ipc.workspace.setPolicy(val as WorkspacePolicy)
  }

  return {
    autoStart,
    autoStartSupported,
    autoStartReason,
    autoStartSaving,
    autoUpgrade,
    updateChannel,
    workspacePolicy,
    loading,
    fetchSettings,
    toggleAutoStart,
    toggleAutoUpgrade,
    setUpdateChannel,
    setWorkspacePolicy
  }
})
