import {
  existsSync,
  mkdirSync,
  readdirSync,
  readFileSync,
  rmSync,
  statSync,
  writeFileSync
} from 'fs'
import { get as httpGet } from 'http'
import { get as httpsGet } from 'https'
import { join, resolve, sep } from 'path'
import { extract } from './archive'

/**
 * 本地技能目录：技能包由服务端下发（/agent/skillVersion/package），下载后解压到 <运行目录>/skills，
 * 每个技能一个文件夹，并存一份 meta.json 记录来源版本，供页面回显「已安装 / 有可用版本」。
 *
 * 目录结构：
 *   skills/<技能名>/...           技能包内容（SKILL.md 等）
 *   skills/<技能名>/meta.json     { skillId, skillName, versionId, versionName, installedAt }
 */

export interface SkillMeta {
  skillId: number
  skillName: string
  versionId: number
  versionName: string
  installedAt: number
}

export interface InstalledSkill {
  name: string
  path: string
  fileCount: number
  size: number
  meta: SkillMeta | null
  /** 技能摘要：SKILL.md 里 YAML 的 description，没有就取第一段正文（列表里用来说明这个技能干什么） */
  summary: string
}

/** 技能包内的一个文件：相对技能目录的路径（用 / 分隔）与大小 */
export interface SkillFileEntry {
  path: string
  size: number
}

/** 技能包内文件的读取结果：超长按上限截断（truncated 为真表示还有没给出的内容） */
export interface SkillFileContent {
  name: string
  path: string
  size: number
  content: string
  truncated: boolean
}

export interface SkillInstallRequest {
  baseUrl: string
  token: string
  skillId: number
  skillName: string
  versionId: number
  versionName: string
}

const META_FILE = 'meta.json'
/** 技能说明文档：@ 引用技能时把它作为上下文交给模型（与 Claude Skills 同一套约定） */
const SKILL_FILE = 'SKILL.md'
const MAX_SKILL_FILE = 256 * 1024
/** 技能包清单与单个文件内容的上限：够模型读懂技能，又不至于把一轮请求撑爆 */
const MAX_SKILL_FILES = 500
const MAX_SKILL_CONTENT = 256 * 1024
/** 技能摘要的取材上限：只看 SKILL.md 开头这一段，够认出这个技能是干什么的 */
const MAX_SUMMARY_SOURCE = 4096
const MAX_SUMMARY = 160
const MAX_REDIRECT = 5
const DOWNLOAD_TIMEOUT = 60000

export class SkillManager {
  private static instance: SkillManager

  private root = ''

  static getInstance(): SkillManager {
    if (!SkillManager.instance) {
      SkillManager.instance = new SkillManager()
    }
    return SkillManager.instance
  }

  get path(): string {
    return this.root
  }

  init(runtimeDir: string): void {
    this.root = join(runtimeDir, 'skills')
    mkdirSync(this.root, { recursive: true })
    console.log('[Skill] Skills directory:', this.root)
  }

  /** 已安装技能清单：目录名即技能名，meta.json 缺失（手工放入）时 meta 为 null */
  list(): InstalledSkill[] {
    if (!this.root || !existsSync(this.root)) return []
    const result: InstalledSkill[] = []
    for (const entry of readdirSync(this.root, { withFileTypes: true })) {
      if (!entry.isDirectory()) continue
      const dir = join(this.root, entry.name)
      const stat = this.measure(dir)
      result.push({
        name: entry.name,
        path: dir,
        fileCount: stat.count,
        size: stat.size,
        meta: this.readMeta(dir),
        summary: this.summary(dir)
      })
    }
    return result.sort((a, b) => a.name.localeCompare(b.name))
  }

  /** 从服务端下载技能包并按技能名解压覆盖安装 */
  async install(request: SkillInstallRequest): Promise<InstalledSkill> {
    this.ensure()
    const name = resolveName(request.skillName)
    const target = this.target(name)
    const url = buildUrl(request.baseUrl, `/agent/skillVersion/package?id=${request.versionId}`)
    const buffer = await download(url, request.token)
    if (target !== resolve(this.root)) {
      rmSync(target, { recursive: true, force: true })
    }
    mkdirSync(target, { recursive: true })
    extract(buffer, target)
    const meta: SkillMeta = {
      skillId: request.skillId,
      skillName: request.skillName,
      versionId: request.versionId,
      versionName: request.versionName,
      installedAt: Date.now()
    }
    writeFileSync(join(target, META_FILE), JSON.stringify(meta, null, 2), 'utf8')
    const stat = this.measure(target)
    return {
      name,
      path: target,
      fileCount: stat.count,
      size: stat.size,
      meta,
      summary: this.summary(target)
    }
  }

  remove(name: string): void {
    this.ensure()
    const target = this.target(name)
    if (target === resolve(this.root)) throw new Error('拒绝对技能根目录执行删除')
    if (!existsSync(target)) return
    rmSync(target, { recursive: true, force: true })
  }

  /** 读取技能的说明文档：技能目录下的 SKILL.md */
  read(name: string): { name: string; path: string; content: string } {
    this.ensure()
    const dir = this.target(name)
    const file = join(dir, SKILL_FILE)
    if (!existsSync(file)) throw new Error(`技能未安装或缺少 ${SKILL_FILE}：${name}`)
    if (statSync(file).size > MAX_SKILL_FILE) {
      throw new Error(`技能说明超过上限（256KB）：${name}`)
    }
    return { name, path: dir, content: readFileSync(file, 'utf8') }
  }

  /** 技能包内的文件清单（不含目录）：模型按它挑要读的说明、脚本与参考文档 */
  files(name: string): { name: string; path: string; files: SkillFileEntry[] } {
    this.ensure()
    const dir = this.target(name)
    if (!existsSync(dir)) throw new Error(`技能未安装：${name}`)
    const files: SkillFileEntry[] = []
    const walk = (current: string, prefix: string): void => {
      for (const entry of readdirSync(current, { withFileTypes: true })) {
        if (files.length >= MAX_SKILL_FILES) return
        const relative = prefix ? `${prefix}/${entry.name}` : entry.name
        const full = join(current, entry.name)
        if (entry.isDirectory()) {
          walk(full, relative)
          continue
        }
        files.push({ path: relative, size: statSync(full).size })
      }
    }
    walk(dir, '')
    return { name, path: dir, files }
  }

  /**
   * 读技能包内的一个文件：省略 path 时读 SKILL.md（技能的主入口）。
   * 技能包内容按不可信输入处理——相对路径解析后必须仍落在这个技能目录里；
   * 非文本文件（图片等资源）直接拒绝，超长按上限截断并把 truncated 置真。
   */
  readFile(name: string, relative = ''): SkillFileContent {
    this.ensure()
    const dir = this.target(name)
    if (!existsSync(dir)) throw new Error(`技能未安装：${name}`)
    const label = String(relative || '').trim() || SKILL_FILE
    const file = resolve(dir, label)
    if (file !== resolve(dir) && !file.startsWith(resolve(dir) + sep)) {
      throw new Error(`非法的文件路径：${relative}`)
    }
    if (!existsSync(file) || !statSync(file).isFile()) {
      throw new Error(`技能「${name}」里没有这个文件：${label}`)
    }
    const size = statSync(file).size
    const buffer = readFileSync(file)
    if (isBinary(buffer)) throw new Error(`不是文本文件，无法读取：${label}`)
    const text = buffer.toString('utf8')
    const truncated = text.length > MAX_SKILL_CONTENT
    return {
      name,
      path: file,
      size,
      content: truncated ? `${text.slice(0, MAX_SKILL_CONTENT)}\n…（已截断）` : text,
      truncated
    }
  }

  private ensure(): void {
    if (!this.root) throw new Error('技能目录尚未初始化')
  }

  /** 目标路径必须落在 skills 目录内：技能名来自服务端，同样按不可信输入处理 */
  private target(name: string): string {
    const target = resolve(join(this.root, name))
    if (!target.startsWith(resolve(this.root) + sep)) {
      throw new Error(`非法的技能名称：${name}`)
    }
    return target
  }

  private readMeta(dir: string): SkillMeta | null {
    const file = join(dir, META_FILE)
    if (!existsSync(file)) return null
    try {
      return JSON.parse(readFileSync(file, 'utf8')) as SkillMeta
    } catch {
      return null
    }
  }

  /** 技能摘要：读 SKILL.md 开头一段，取 frontmatter 的 description（没有就取第一段正文） */
  private summary(dir: string): string {
    const file = join(dir, SKILL_FILE)
    if (!existsSync(file)) return ''
    try {
      return skillSummary(readFileSync(file, 'utf8').slice(0, MAX_SUMMARY_SOURCE))
    } catch {
      return ''
    }
  }

  private measure(dir: string): { count: number; size: number } {
    let count = 0
    let size = 0
    const walk = (current: string): void => {
      for (const entry of readdirSync(current, { withFileTypes: true })) {
        const path = join(current, entry.name)
        if (entry.isDirectory()) {
          walk(path)
          continue
        }
        count++
        size += statSync(path).size
      }
    }
    walk(dir)
    return { count, size }
  }
}

/** 技能名做最小可用化处理：去掉路径分隔符与首尾空白，避免越界目录名 */
function resolveName(name: string): string {
  const cleaned = String(name || '')
    .replace(/[\\/]/g, '-')
    .trim()
  if (!cleaned) throw new Error('技能名称为空')
  return cleaned
}

/** 粗判二进制：前 8KB 里出现 NUL 就按非文本处理（技能包里可能带图片等资源） */
function isBinary(buffer: Buffer): boolean {
  return buffer.subarray(0, 8192).includes(0)
}

/**
 * 技能摘要：SKILL.md 的 frontmatter 里 `description: …` 优先（技能包的通用约定），
 * 没有就取第一段正文（跳过标题、分隔线与代码块）——列表里一行就能看出技能是干什么的。
 */
function skillSummary(text: string): string {
  const rows = String(text ?? '').split(/\r?\n/)
  const described = frontmatterDescription(rows)
  return clip(described || firstProse(rows.slice(frontmatterEnd(rows))))
}

function frontmatterDescription(rows: string[]): string {
  if ('---' !== String(rows[0] ?? '').trim()) return ''
  for (let index = 1; index < rows.length; index++) {
    const line = String(rows[index] ?? '').trim()
    if ('---' === line) return ''
    const matched = /^description\s*:\s*(.*)$/i.exec(line)
    if (!matched) continue
    const value = matched[1].trim().replace(/^['"]|['"]$/g, '')
    // 折叠 / 块写法（|、>、>-）：值写在下一层缩进里，把这几个行接起来
    return value && !/^[|>][-+]?$/.test(value) ? value : blockValue(rows, index + 1)
  }
  return ''
}

/** frontmatter 的结束位置（下一行的下标）：没有 frontmatter 时给 0 */
function frontmatterEnd(rows: string[]): number {
  if ('---' !== String(rows[0] ?? '').trim()) return 0
  for (let index = 1; index < rows.length; index++) {
    if ('---' === String(rows[index] ?? '').trim()) return index + 1
  }
  return rows.length
}

/** 块写法的取值：接住后面缩进的行（空行当断句，遇到顶格内容或分隔线就结束） */
function blockValue(rows: string[], start: number): string {
  const parts: string[] = []
  for (let index = start; index < rows.length; index++) {
    const raw = String(rows[index] ?? '')
    const line = raw.trim()
    if ('---' === line) break
    if (!line) {
      if (parts.length) parts.push('')
      continue
    }
    if (!/^\s/.test(raw)) break
    parts.push(line)
  }
  return parts.join(' ').trim()
}

function firstProse(rows: string[]): string {
  let inFence = false
  for (const row of rows) {
    const line = String(row ?? '').trim()
    if (line.startsWith('```')) {
      inFence = !inFence
      continue
    }
    if (inFence || !line || line.startsWith('#') || line.startsWith('---')) continue
    return line
  }
  return ''
}

/** 摘要压成一行并限长：列表里每行都占上下文，摘要只用来「认出是哪个技能」 */
function clip(text: string): string {
  const value = String(text ?? '')
    .replace(/\s+/g, ' ')
    .trim()
  return value.length > MAX_SUMMARY ? `${value.slice(0, MAX_SUMMARY)}…` : value
}

function buildUrl(baseUrl: string, path: string): string {
  const base = String(baseUrl || '').replace(/\/+$/, '')
  if (!base) throw new Error('接口地址未配置')
  return base + path
}

/** 下载文件：手动跟随重定向（服务端返回文件服务地址），每次请求都带上登录态 */
function download(url: string, token: string, redirect = 0): Promise<Buffer> {
  return new Promise((resolvePromise, reject) => {
    const request = new URL(url)
    const client = 'https:' === request.protocol ? httpsGet : httpGet
    const req = client(
      url,
      {
        headers: {
          Accept: 'application/octet-stream, application/zip, */*',
          ...(token ? { Authorization: 'Bearer ' + token } : {})
        },
        timeout: DOWNLOAD_TIMEOUT
      },
      (res) => {
        const status = res.statusCode ?? 0
        if (status >= 300 && status < 400 && res.headers.location) {
          res.resume()
          if (redirect >= MAX_REDIRECT) {
            reject(new Error('下载技能包失败：重定向次数过多'))
            return
          }
          const next = new URL(res.headers.location, url).toString()
          download(next, token, redirect + 1).then(resolvePromise, reject)
          return
        }
        if (status < 200 || status >= 300) {
          res.resume()
          reject(new Error(`下载技能包失败：HTTP ${status}`))
          return
        }
        const chunks: Buffer[] = []
        res.on('data', (chunk: Buffer) => chunks.push(chunk))
        res.on('end', () => resolvePromise(Buffer.concat(chunks)))
        res.on('error', reject)
      }
    )
    req.on('timeout', () => {
      req.destroy(new Error('下载技能包超时'))
    })
    req.on('error', reject)
  })
}
