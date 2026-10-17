/**
 * 项目环境变量：项目编辑器「环境变量」那一格里的键值对。
 *
 * 它们的命令执行作用只在本机——执行本机命令（`run_command`）时带进子进程的环境，
 * 于是同一个命令在不同项目里能落到不同的工具链上（典型的：一个项目指 JDK 8、另一个指 JDK 17）。
 *
 * 给模型的本机说明里，非密钥值会按项目下发，标了「密钥」的项只有变量名
 * （见 `WorkspaceManager.describe`）；分享到服务端时标了「密钥」的行整行留下
 * （见 `utils/ProjectUtil.ts` 的 `publishConfig`）。
 * 这一份只做纯计算：解析库里的 JSON、把项目声明与当前进程环境合成子进程环境。
 */

/** 变量名规则：字母或下划线开头，后面是字母 / 数字 / 下划线（与 shell 的写法一致） */
const NAME_PATTERN = /^[A-Za-z_][A-Za-z0-9_]*$/

/** 命令输出里密钥值的替身 */
const SECRET_MASK = '***'

/** 项目里声明的一个环境变量：secret 只表示「界面掩码显示 + 分享时留下本机」 */
export interface ProjectEnvVar {
  key: string
  value: string
  secret: boolean
}

/**
 * 解析库里 `projects.env_vars` 那一列（JSON 数组）：结构不对的项丢掉。
 * 读不出来（空串、坏掉的 JSON）就当没配——这些变量是附加项，不挡项目本身可用。
 */
export function parseProjectEnvVars(raw: unknown): ProjectEnvVar[] {
  if (typeof raw !== 'string' || !raw) return []
  let list: unknown
  try {
    list = JSON.parse(raw)
  } catch {
    return []
  }
  if (!Array.isArray(list)) return []
  return list.map(one).filter((item): item is ProjectEnvVar => null !== item)
}

/** 单项归一：键不合规（空、带特殊字符）的整项丢掉，值一律按文本收敛 */
function one(item: unknown): ProjectEnvVar | null {
  if (!item || 'object' !== typeof item) return null
  const source = item as Record<string, unknown>
  const key = String(source['key'] ?? '').trim()
  if (!NAME_PATTERN.test(key)) return null
  return { key, value: String(source['value'] ?? ''), secret: true === source['secret'] }
}

/** 变量名是否同名：Windows 的环境变量不区分大小写（Path / PATH 是同一个） */
function sameName(key: string, other: string): boolean {
  return 'win32' === process.platform ? key.toUpperCase() === other.toUpperCase() : key === other
}

/** PATH 的分隔符：Windows 用分号，其余平台用冒号 */
const PATH_SEP = 'win32' === process.platform ? ';' : ':'

/**
 * 合成子进程环境：项目里的变量按名覆盖进程环境变量（同名先摘掉旧写法，避免一个环境块里
 * 出现两条 Path）。
 *
 * 两条例外与约定：
 * - `PATH` 不覆盖，而是把项目里的值**前置**到系统 PATH 之前——把目标 JDK 的 bin 目录写进去
 *   就能切换 `java`，不必把系统 PATH 抄一遍（抄漏了会连基本命令都跑不起来）；
 * - 值原样使用，不做变量展开（写成 `%USERPROFILE%` 这种引用不会被解析）。
 */
export function mergeProjectEnv(base: NodeJS.ProcessEnv, vars: ProjectEnvVar[]): NodeJS.ProcessEnv {
  const env: NodeJS.ProcessEnv = { ...base }
  const prepend: string[] = []
  for (const item of vars) {
    if (sameName('PATH', item.key)) {
      if (item.value) prepend.push(item.value)
      continue
    }
    const existing = Object.keys(env).find((key) => key !== item.key && sameName(key, item.key))
    if (existing) delete env[existing]
    env[item.key] = item.value
  }
  if (prepend.length) {
    // 沿用进程里 PATH 原本的大小写写法：Windows 上是 `Path`，多写一条同名键更麻烦
    const key = Object.keys(env).find((name) => sameName(name, 'PATH')) ?? 'PATH'
    const current = String(env[key] ?? '')
    env[key] = [...prepend, current].filter(Boolean).join(PATH_SEP)
  }
  return env
}

/**
 * 命令输出脱敏：标了「密钥」的值在回到调用方之前换成 `***`。
 *
 * 挡的是「值自己跑出来」这类泄露——命令回显了整个环境、脚本把配置打进了日志、
 * 或者模型直接 `echo %TOKEN%`。按值做精确子串替换（长值先替，免得短值把长值切碎），
 * 不猜编码、不做正则：**拦不住刻意编码后的外传**（base64 / certutil 之类），
 * 那是「有心拿走」，不是这一步能解决的；密钥该用在哪就交给脚本，别让模型沾手。
 */
export function redactSecretValues(text: string, vars: ProjectEnvVar[]): string {
  let out = text
  const values = vars
    .filter((item) => item.secret && item.value)
    .map((item) => item.value)
    .sort((a, b) => b.length - a.length)
  for (const value of values) out = out.split(value).join(SECRET_MASK)
  return out
}
