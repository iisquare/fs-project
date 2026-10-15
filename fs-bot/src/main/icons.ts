import { copyFileSync, existsSync, mkdirSync } from 'fs'
import { basename, extname, join, resolve, sep } from 'path'
import { pathToFileURL } from 'url'
import { randomUUID } from 'crypto'
import { dialog, net, protocol, type BrowserWindow } from 'electron'

/**
 * 本地图标：项目图标选中的本地图片复制到 userdata/icons，界面用 app-icon://<文件名> 引用。
 *
 * 存复制件而不是原路径：原图挪走、改名或换台机器后图标照样显示；
 * 网络地址不经过这里，手填的 http(s) 地址照旧由渲染进程直接加载。
 */
export const ICON_SCHEME = 'app-icon'

/** 允许作为图标的图片类型：文件选择框的过滤条件，也是扩展名兜底时的白名单 */
const ICON_EXTENSIONS = ['png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp', 'ico', 'avif', 'svg']

/** 图标目录：initIcons 之后才有值（安装目录要到 app ready 时才拿到） */
let root = ''

/**
 * 协议必须在 app ready 之前登记成标准协议：
 * 页面在开发模式来自 http://localhost、打包后来自 file://，
 * 自定义协议不登记的话两侧都会把它当不安全来源拦掉（CSP 里同样按协议放行，见 index.html）。
 */
protocol.registerSchemesAsPrivileged([
  { scheme: ICON_SCHEME, privileges: { standard: true, secure: true, supportFetchAPI: true } }
])

/** 图标目录随安装目录走（与 userdata / skills 同级），没有就建一个 */
export function initIcons(installPath: string): void {
  root = join(installPath, 'userdata', 'icons')
  mkdirSync(root, { recursive: true })
}

/**
 * 选择本地图片：复制到图标目录后返回可直接放进 img 的地址。
 * 取消选择返回空串，界面据此保留原有地址。
 */
export async function pickIcon(window: BrowserWindow | null): Promise<string> {
  if (!window) return ''
  const result = await dialog.showOpenDialog(window, {
    title: '选择本地图片',
    filters: [{ name: '图片', extensions: ICON_EXTENSIONS }],
    properties: ['openFile', 'showHiddenFiles']
  })
  if (result.canceled || result.filePaths.length < 1) return ''
  const source = result.filePaths[0]
  const ext = extname(source).toLowerCase()
  // 文件名由主进程生成：小写字母数字与点，标准协议下 hostname 不会被改写
  const name = `${randomUUID()}${ICON_EXTENSIONS.includes(ext.slice(1)) ? ext : '.png'}`
  copyFileSync(source, join(root, name))
  return `${ICON_SCHEME}://${name}`
}

/**
 * 协议请求 → 响应：只认文件名，路径由主进程拼，
 * 渲染进程拿不到图标目录之外的任何文件（文件名里带 .. 或分隔符时按越界处理）。
 */
export function serveIcon(url: string): Response | Promise<Response> {
  if (!root) return new Response(null, { status: 404 })
  const name = basename(iconFileName(url))
  if (!name) return new Response(null, { status: 404 })
  const folder = resolve(root)
  const file = resolve(join(folder, name))
  if (!file.startsWith(folder + sep) || !existsSync(file)) {
    return new Response(null, { status: 404 })
  }
  // 交给 net.fetch 按扩展名给出响应类型，图片直接能在 img 里显示
  return net.fetch(pathToFileURL(file).toString())
}

/** 文件名取自地址：标准协议的 app-icon://<文件名> 落在 hostname 上，解析不出来就当没这一步 */
function iconFileName(url: string): string {
  try {
    const parsed = new URL(url)
    return parsed.hostname || parsed.pathname
  } catch {
    return ''
  }
}
