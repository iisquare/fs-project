/** 桌面任务首次操作前的授权方式：逐任务请求批准 / 直接放行当前任务 */
export type DesktopApprovalPolicy = 'ask' | 'auto'

/** 配置文件和 IPC 输入都可能来自手工编辑；除明确的 auto 外一律按最保守的 ask 处理 */
export function normalizeDesktopApprovalPolicy(value: unknown): DesktopApprovalPolicy {
  return 'auto' === value ? 'auto' : 'ask'
}
