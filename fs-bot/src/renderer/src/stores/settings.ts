import { ref } from 'vue'
import { defineStore } from 'pinia'
import Config from '@renderer/core/Config'
import { clearTools } from '@renderer/core/ToolCache'
import type {
  DesktopApprovalPolicy,
  DesktopAutomationStatus,
  DesktopPermissionKey,
  DesktopPermissionStatus
} from '../../../shared/desktop'

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
  const desktopAutomationEnabled = ref(false)
  const desktopAutomationSupported = ref(false)
  const desktopAutomationReason = ref('')
  const desktopPermissionsChecked = ref(false)
  const desktopPermissionsReady = ref(false)
  const desktopPermissionsRestartRequired = ref(false)
  const desktopPermissions = ref<DesktopPermissionStatus[]>([])
  const desktopAutomationSaving = ref(false)
  const desktopApprovalPolicy = ref<DesktopApprovalPolicy>('ask')
  const desktopApprovalPolicySaving = ref(false)
  const loading = ref(false)

  function applyDesktopStatus(status: DesktopAutomationStatus): void {
    desktopAutomationEnabled.value = status.enabled === true
    desktopAutomationSupported.value = status.supported === true
    desktopAutomationReason.value = String(status.reason ?? '')
    desktopPermissionsChecked.value = status.permissionsChecked === true
    desktopPermissionsReady.value = status.permissionsReady === true
    desktopPermissionsRestartRequired.value = status.restartRequired === true
    desktopPermissions.value = Array.isArray(status.permissions) ? status.permissions : []
    desktopApprovalPolicy.value = status.approvalPolicy === 'auto' ? 'auto' : 'ask'
  }

  async function fetchSettings(): Promise<void> {
    loading.value = true
    try {
      const [settings, autoStartStatus, desktopStatus] = await Promise.all([
        Config.getSettings(),
        window.ipc.autoStart.get(),
        window.ipc.automation.desktopStatus()
      ])
      autoStart.value = autoStartStatus.enabled
      autoStartSupported.value = autoStartStatus.supported
      autoStartReason.value = autoStartStatus.reason
      autoUpgrade.value = settings.autoUpgrade === true
      updateChannel.value = String(settings.updateChannel ?? 'stable')
      workspacePolicy.value = String(settings.workspacePolicy ?? 'ask')
      applyDesktopStatus(desktopStatus)
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

  async function setDesktopAutomationEnabled(enabled: boolean): Promise<DesktopAutomationStatus> {
    desktopAutomationSaving.value = true
    try {
      const status = await window.ipc.automation.setDesktopEnabled(enabled)
      applyDesktopStatus(status)
      // 工具集按项目缓存；开关与权限状态变化后清空，下一次发送重新装配桌面委派入口。
      clearTools()
      return status
    } finally {
      desktopAutomationSaving.value = false
    }
  }

  /** 只刷新平台支持与本地开关；不会读取或触发系统权限检测 */
  async function refreshDesktopStatus(): Promise<void> {
    applyDesktopStatus(await window.ipc.automation.desktopStatus())
  }

  async function openDesktopPermissionSettings(key: DesktopPermissionKey): Promise<void> {
    await window.ipc.automation.openDesktopPermissionSettings(key)
  }

  async function setDesktopApprovalPolicy(policy: DesktopApprovalPolicy): Promise<void> {
    desktopApprovalPolicySaving.value = true
    try {
      desktopApprovalPolicy.value = await window.ipc.automation.setDesktopApprovalPolicy(policy)
    } finally {
      desktopApprovalPolicySaving.value = false
    }
  }

  return {
    autoStart,
    autoStartSupported,
    autoStartReason,
    autoStartSaving,
    autoUpgrade,
    updateChannel,
    workspacePolicy,
    desktopAutomationEnabled,
    desktopAutomationSupported,
    desktopAutomationReason,
    desktopPermissionsChecked,
    desktopPermissionsReady,
    desktopPermissionsRestartRequired,
    desktopPermissions,
    desktopAutomationSaving,
    desktopApprovalPolicy,
    desktopApprovalPolicySaving,
    loading,
    fetchSettings,
    toggleAutoStart,
    toggleAutoUpgrade,
    setUpdateChannel,
    setWorkspacePolicy,
    setDesktopAutomationEnabled,
    refreshDesktopStatus,
    openDesktopPermissionSettings,
    setDesktopApprovalPolicy
  }
})
