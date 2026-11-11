/** 客户端身份：业务请求头、版本策略、灰度分桶和更新检查共用同一份口径。 */
export interface ClientMeta {
  clientVersion: string
  platform: string
  architecture: string
  clientId: string
  channel: string
}

/** 服务端版本闸门返回的业务码。 */
export const CLIENT_UPGRADE_CODE = 90426

/** 渲染进程收到版本升级要求时使用的窗口事件。 */
export const CLIENT_UPGRADE_WINDOW_EVENT = 'fs-client-upgrade-required'

/** 主进程通知渲染进程升级的 IPC 通道。 */
export const CLIENT_UPGRADE_REQUIRED_CHANNEL = 'app:client-upgrade-required'

/**
 * 服务端识别客户端版本所用的五个请求头。
 * 由一个入口生成，避免 Axios、SSE 和主进程下载各拼一份后逐渐漏字段。
 */
export function clientRequestHeaders(client: ClientMeta): Record<string, string> {
  return {
    'x-client-version': String(client.clientVersion ?? ''),
    'x-client-platform': String(client.platform ?? ''),
    'x-client-arch': String(client.architecture ?? ''),
    'x-client-id': String(client.clientId ?? ''),
    'x-client-channel': String(client.channel ?? '')
  }
}

/** 从标准 `{ code, message, data }` 响应中读取业务码。 */
export function clientResultCode(value: unknown): number | null {
  if (!value || typeof value !== 'object' || !('code' in value)) return null
  const code = Number((value as { code?: unknown }).code)
  return Number.isFinite(code) ? code : null
}

/** HTTP 426 或其它响应只要带 90426，都按客户端必须升级处理。 */
export function isClientUpgradeResult(value: unknown): boolean {
  return CLIENT_UPGRADE_CODE === clientResultCode(value)
}

/** 426 是版本闸门的协议状态；即使代理改写了错误体，也应进入升级流程。 */
export function isClientUpgradeResponse(status: number, value: unknown): boolean {
  return 426 === status || isClientUpgradeResult(value)
}

/** 主进程业务请求遇到版本闸门时抛出，IPC 层据此通知渲染进程。 */
export class ClientUpgradeRequiredError extends Error {
  readonly code = CLIENT_UPGRADE_CODE

  constructor(
    message: string,
    readonly status = 426
  ) {
    super(message)
    this.name = 'ClientUpgradeRequiredError'
  }
}

export function isClientUpgradeRequired(error: unknown): error is ClientUpgradeRequiredError {
  return (
    error instanceof ClientUpgradeRequiredError ||
    (!!error &&
      typeof error === 'object' &&
      CLIENT_UPGRADE_CODE === Number((error as { code?: unknown }).code))
  )
}
