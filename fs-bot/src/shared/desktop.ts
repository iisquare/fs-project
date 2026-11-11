/** 桌面任务首次操作的授权策略：逐任务请求批准 / 直接放行 */
export type DesktopApprovalPolicy = 'ask' | 'auto'

/** 桌面托管期间启用的全局紧急停止组合键，避免占用 Escape 干扰目标应用。 */
export const DESKTOP_EMERGENCY_SHORTCUT = 'CommandOrControl+Alt+Shift+K'

export function desktopEmergencyShortcutLabel(platform: string): string {
  return 'darwin' === platform ? '⌘⌥⇧K' : 'Ctrl+Alt+Shift+K'
}

export type DesktopPermissionKey = 'accessibility' | 'screenRecording'

export type DesktopPermissionState =
  'granted' | 'not-determined' | 'denied' | 'restricted' | 'unknown'

export interface DesktopPermissionStatus {
  key: DesktopPermissionKey
  name: string
  description: string
  state: DesktopPermissionState
  granted: boolean
}

export interface DesktopAutomationSupportStatus {
  supported: boolean
  /** 本次状态是否实际读取过系统权限；启动时的普通状态查询为 false */
  permissionsChecked: boolean
  permissionsReady: boolean
  /** 屏幕录制权限已配置，但需要重启应用后才对当前进程生效 */
  restartRequired: boolean
  permissions: DesktopPermissionStatus[]
  platform: string
  reason: string
}

export interface DesktopAutomationStatus extends DesktopAutomationSupportStatus {
  enabled: boolean
  approvalPolicy: DesktopApprovalPolicy
}

/** 桌面 Agent 因总开关或系统权限不可用而被主进程终止时，推给对应主对话。 */
export interface DesktopAgentUnavailableNotice {
  conversationId: string
  sourceSessionId: string
  reason: string
  permissions: DesktopPermissionStatus[]
  restartRequired: boolean
}
