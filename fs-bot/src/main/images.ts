import { randomUUID } from 'crypto'
import { existsSync, statSync } from 'fs'
import { extname } from 'path'
import { pathToFileURL } from 'url'
import { net, protocol } from 'electron'

/**
 * 对话正文里的本机图片：模型用 `![说明](local:相对路径)` 引用本机文件，
 * 主进程按项目工作区权限读取后签发一个 `chat-image://<随机标识>` 地址，界面只认这个地址。
 *
 * 地址里不带路径：渲染进程拿不到「按任意路径读文件」的能力，协议处理时只按标识从内存映射回文件；
 * 标识不可预测、只在本次运行内有效，重启后历史消息重新解析时会换一批新地址。
 *
 * 只开放栅格图（不含 svg）：SVG 可以带脚本，虽然 img 里不会执行，但没有展示 SVG 的需求时
 * 少开一类内容更稳妥。单张大小上限与 workspace 的读取上限分开，图片展示按自己的口径。
 */
export const CHAT_IMAGE_SCHEME = 'chat-image'

/** 单张图片展示上限：再大就让模型改用缩略图或缩小后再展示，避免把整张原图塞进渲染进程 */
export const MAX_CHAT_IMAGE_SIZE = 20 * 1024 * 1024

/** 允许展示的图片类型：后缀 → contentType */
const IMAGE_TYPES: Record<string, string> = {
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.gif': 'image/gif',
  '.webp': 'image/webp',
  '.bmp': 'image/bmp',
  '.avif': 'image/avif'
}

/** 内存里的「标识 → 文件」映射：数量有上限，满了淘汰最早签发的一条（访问时刷新顺序） */
const files = new Map<string, { path: string; type: string }>()
const MAX_FILES = 2048

/** 协议必须在 app ready 之前登记成标准协议，否则页面侧的 CSP 与 img 加载都会把它当不安全来源 */
protocol.registerSchemesAsPrivileged([
  {
    scheme: CHAT_IMAGE_SCHEME,
    privileges: { standard: true, secure: true, supportFetchAPI: true, stream: true }
  }
])

/** 文件后缀对应的 contentType：不在白名单里返回空串（调用方据此拒绝） */
export function chatImageType(path: string): string {
  return IMAGE_TYPES[extname(String(path ?? '')).toLowerCase()] ?? ''
}

/**
 * 签发展示地址：调用方必须已经完成工作区鉴权与大小 / 类型检查。
 * 同一个文件每次渲染各签发一个标识，互不影响；地址只用于展示，不写库。
 */
export function issueChatImage(path: string, type: string): string {
  if (files.size >= MAX_FILES) {
    const oldest = files.keys().next().value
    if (oldest) files.delete(oldest)
  }
  const id = randomUUID()
  files.set(id, { path, type })
  return `${CHAT_IMAGE_SCHEME}://${id}`
}

/**
 * 协议请求 → 响应：标识取自地址的 hostname，只认内存里签发过的那些。
 * 文件在展示期间被删改时按 404 处理，界面由图片加载失败兜底到默认图。
 */
export async function serveChatImage(url: string): Promise<Response> {
  const id = imageId(url)
  const entry = id ? files.get(id) : undefined
  if (!entry || !existsSync(entry.path)) return new Response(null, { status: 404 })
  try {
    const stat = statSync(entry.path)
    if (!stat.isFile() || stat.size > MAX_CHAT_IMAGE_SIZE) {
      return new Response(null, { status: 404 })
    }
  } catch {
    return new Response(null, { status: 404 })
  }
  // 刷新访问顺序：最近看过的图片留在映射里，历史消息翻来翻去不重复签发
  files.delete(id)
  files.set(id, entry)
  // 交给 net.fetch 读本地文件并给出响应类型（与图标协议同一套做法）
  const response = await net.fetch(pathToFileURL(entry.path).toString())
  if (!response.ok || !response.body) return response
  // 显式给出 contentType：少量平台 / 格式下 file 响应未必带对类型；
  // 本地文件可能被覆盖，禁止中间层缓存住旧内容
  return new Response(response.body, {
    status: response.status,
    headers: { 'Content-Type': entry.type, 'Cache-Control': 'no-store' }
  })
}

/** 标识取自地址：标准协议的 hostname 就是签发时放进地址的那一段 */
function imageId(url: string): string {
  try {
    return new URL(url).hostname
  } catch {
    return ''
  }
}
