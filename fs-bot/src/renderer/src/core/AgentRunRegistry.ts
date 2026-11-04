interface ParentRunState {
  token: string
  canceled: boolean
}

const parentRuns = new Map<string, ParentRunState>()

/**
 * 每轮主会话开始时生成新令牌。令牌不跨轮复用，因此旧轮次在异步初始化后迟到时，
 * 会发现当前令牌已经变化并按已停止处理。
 */
export function beginAgentParentRun(parentConversationId: string): string {
  const id = String(parentConversationId ?? '').trim()
  if (!id) return ''
  const token = crypto.randomUUID()
  parentRuns.set(id, { token, canceled: false })
  return token
}

export function cancelAgentParentRun(parentConversationId: string, token = ''): void {
  const current = parentRuns.get(String(parentConversationId ?? '').trim())
  if (current && (!token || current.token === token)) current.canceled = true
}

export function isAgentParentRunCanceled(parentConversationId: string, token: string): boolean {
  const current = parentRuns.get(String(parentConversationId ?? '').trim())
  return !token || !current || current.token !== token || current.canceled
}

export function resetAgentParentRuns(): void {
  for (const state of parentRuns.values()) state.canceled = true
  parentRuns.clear()
}
