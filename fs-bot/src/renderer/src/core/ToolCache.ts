import type { Project } from '@renderer/db/ProjectDb'

/**
 * 工具集共享缓存：键只由项目工具配置决定，不包含会话标识。
 *
 * 正常打开会话与多轮对话直接复用缓存；新建会话调用 refreshTools 使对应配置失效。
 * 缓存与刷新逻辑独立于此模块，避免会话 store 为了失效缓存而依赖完整的 Toolbox。
 */
interface ToolBuild<T> {
  toolset: T
  complete: boolean
}

interface PendingBuild<T> {
  promise: Promise<ToolBuild<T>>
  revision: number
}

const cache = new Map<string, unknown>()
const pendingBuilds = new Map<string, PendingBuild<unknown>>()
const revisions = new Map<string, number>()

/** 共享工具集的配置键：工具引用或记忆开关变化时视为新的一份配置 */
export function toolConfigKey(project: Project | null): string {
  const refs = (project?.tools ?? []).filter((item) => false !== item?.enabled)
  return `${JSON.stringify(refs)}|memory:${true === project?.memoryEnabled ? 1 : 0}`
}

/**
 * 取共享工具集：命中直接返回；未命中时合并同配置的并发组装。
 * 服务端请求失败的结果可供当前轮次使用，但不会进入共享缓存。
 */
export async function sharedToolset<T>(
  project: Project | null,
  build: () => Promise<ToolBuild<T>>
): Promise<T> {
  const key = toolConfigKey(project)
  const cached = cache.get(key)
  if (cached !== undefined) return cached as T

  let pending = pendingBuilds.get(key) as PendingBuild<T> | undefined
  if (!pending) {
    const promise = build()
    pending = { promise, revision: revisions.get(key) ?? 0 }
    pendingBuilds.set(key, pending)
    const clear = (): void => {
      if (pendingBuilds.get(key) === pending) pendingBuilds.delete(key)
    }
    void promise.then(clear, clear)
  }

  const built = await pending.promise
  // 等待期间若新建会话刷新过这一份配置，旧请求不能覆盖刷新后的结果
  if (built.complete && (revisions.get(key) ?? 0) === pending.revision) {
    cache.set(key, built.toolset)
  }
  return built.toolset
}

/** 新建会话时刷新该项目的共享工具集；下一次组装会重新获取工具定义与统一说明 */
export function refreshTools(project: Project | null): void {
  const key = toolConfigKey(project)
  cache.delete(key)
  pendingBuilds.delete(key)
  revisions.set(key, (revisions.get(key) ?? 0) + 1)
}

/** 登录身份变化时清空所有账号相关工具定义，避免跨账号复用共享缓存 */
export function clearTools(): void {
  cache.clear()
  for (const key of pendingBuilds.keys()) {
    revisions.set(key, (revisions.get(key) ?? 0) + 1)
  }
  pendingBuilds.clear()
}
