import { gunzipSync, inflateRawSync } from 'zlib'
import { mkdirSync, writeFileSync } from 'fs'
import { dirname, join, normalize, resolve, sep } from 'path'

/**
 * 归档解包：技能包由服务端下发，常见两种格式（zip / tar.gz）。
 *
 * 这里刻意不引入第三方解压依赖：
 * - zip 只读取中央目录（EOCD）再按 local header 定位数据，压缩方式支持 store(0) 与 deflate(8)；
 * - tar.gz 先 gunzip 再按 512 字节块解析，跳过 pax/长名等扩展头。
 * 两种格式都做「路径逃逸」防护：解出的文件必须落在目标目录内。
 */

export type ArchiveFormat = 'zip' | 'tar.gz' | null

/** 按魔数判断归档格式：zip 以 PK 开头，gzip 以 1f 8b 开头 */
export function detectFormat(buffer: Buffer): ArchiveFormat {
  if (buffer.length >= 4 && buffer[0] === 0x50 && buffer[1] === 0x4b) return 'zip'
  if (buffer.length >= 2 && buffer[0] === 0x1f && buffer[1] === 0x8b) return 'tar.gz'
  return null
}

/** 解包到目标目录，返回写出的文件相对路径清单 */
export function extract(buffer: Buffer, targetDir: string): string[] {
  const format = detectFormat(buffer)
  const root = resolve(targetDir)
  mkdirSync(root, { recursive: true })
  if ('zip' === format) return extractZip(buffer, root)
  if ('tar.gz' === format) return extractTarGz(buffer, root)
  throw new Error('不支持的技能包格式（仅支持 zip / tar.gz）')
}

/** 目标路径必须在根目录内：挡掉 ../ 与绝对路径写入（zip-slip） */
function safePath(root: string, entry: string): string {
  const cleaned = entry.replace(/\\/g, '/').replace(/^\/+/, '')
  const target = resolve(join(root, normalize(cleaned)))
  if (target !== root && !target.startsWith(root + sep)) {
    throw new Error(`技能包包含越界路径：${entry}`)
  }
  return target
}

function writeEntry(root: string, entry: string, data: Buffer, files: string[]): void {
  const target = safePath(root, entry)
  mkdirSync(dirname(target), { recursive: true })
  writeFileSync(target, data)
  files.push(target.substring(root.length + 1).replace(/\\/g, '/'))
}

// ── zip ─────────────────────────────────────────────────────────────

const EOCD_SIGNATURE = 0x06054b50
const CENTRAL_SIGNATURE = 0x02014b50
const LOCAL_SIGNATURE = 0x04034b50

function extractZip(buffer: Buffer, root: string): string[] {
  const eocd = findEocd(buffer)
  if (eocd < 0) throw new Error('zip 文件损坏：未找到中央目录')
  const count = buffer.readUInt16LE(eocd + 10)
  let offset = buffer.readUInt32LE(eocd + 16)
  if (0xffffffff === count || 0xffffffff === offset) {
    throw new Error('暂不支持 zip64 格式的技能包')
  }
  const files: string[] = []
  for (let index = 0; index < count; index++) {
    if (buffer.readUInt32LE(offset) !== CENTRAL_SIGNATURE) {
      throw new Error('zip 文件损坏：中央目录条目异常')
    }
    const method = buffer.readUInt16LE(offset + 10)
    const compressedSize = buffer.readUInt32LE(offset + 20)
    const nameLength = buffer.readUInt16LE(offset + 28)
    const extraLength = buffer.readUInt16LE(offset + 30)
    const commentLength = buffer.readUInt16LE(offset + 32)
    const localOffset = buffer.readUInt32LE(offset + 42)
    const name = buffer.subarray(offset + 46, offset + 46 + nameLength).toString('utf8')
    offset += 46 + nameLength + extraLength + commentLength

    // 目录条目：只建目录，不写文件
    if (name.endsWith('/')) {
      mkdirSync(safePath(root, name), { recursive: true })
      continue
    }
    if (buffer.readUInt32LE(localOffset) !== LOCAL_SIGNATURE) {
      throw new Error(`zip 文件损坏：${name} 的本地头异常`)
    }
    const localNameLength = buffer.readUInt16LE(localOffset + 26)
    const localExtraLength = buffer.readUInt16LE(localOffset + 28)
    const dataStart = localOffset + 30 + localNameLength + localExtraLength
    const raw = buffer.subarray(dataStart, dataStart + compressedSize)
    let data: Buffer
    if (0 === method) {
      data = Buffer.from(raw)
    } else if (8 === method) {
      data = inflateRawSync(raw)
    } else {
      throw new Error(`zip 使用了不支持的压缩方式：${method}`)
    }
    writeEntry(root, name, data, files)
  }
  return files
}

/** 中央目录结束记录在文件尾部，且其后的注释最长 65535 字节 */
function findEocd(buffer: Buffer): number {
  const min = Math.max(0, buffer.length - 65557)
  for (let i = buffer.length - 22; i >= min; i--) {
    if (buffer.readUInt32LE(i) === EOCD_SIGNATURE) return i
  }
  return -1
}

// ── tar.gz ──────────────────────────────────────────────────────────

function extractTarGz(buffer: Buffer, root: string): string[] {
  const tar = gunzipSync(buffer)
  const files: string[] = []
  let offset = 0
  let pendingName = ''
  while (offset + 512 <= tar.length) {
    const header = tar.subarray(offset, offset + 512)
    offset += 512
    if (header.every((byte) => 0 === byte)) break // 结尾的全零块
    const name = readString(header, 0, 100)
    const size = readOctal(header, 124, 12)
    const type = String.fromCharCode(header[156] || 0x30)
    const prefix = readString(header, 345, 155)
    const path = pendingName || (prefix ? `${prefix}/${name}` : name)
    // 数据按 512 对齐
    const dataStart = offset
    const dataEnd = dataStart + size
    offset = dataStart + Math.ceil(size / 512) * 512
    // pax 扩展头与 GNU 长名：内容不落盘，只影响紧随其后的条目
    if ('x' === type || 'g' === type) continue
    if ('L' === type) {
      pendingName = tar.subarray(dataStart, dataEnd).toString('utf8').replace(/\0+$/, '')
      continue
    }
    pendingName = ''
    if ('5' === type) {
      mkdirSync(safePath(root, path), { recursive: true })
      continue
    }
    if ('0' !== type && '\0' !== type && '' !== type) continue // 软/硬链接等一律跳过
    writeEntry(root, path, Buffer.from(tar.subarray(dataStart, dataEnd)), files)
  }
  return files
}

function readString(buffer: Buffer, start: number, length: number): string {
  const end = buffer.indexOf(0, start)
  const stop = end < 0 || end > start + length ? start + length : end
  return buffer.subarray(start, stop).toString('utf8').trim()
}

function readOctal(buffer: Buffer, start: number, length: number): number {
  const text = readString(buffer, start, length).replace(/\0/g, '').trim()
  const value = parseInt(text, 8)
  return Number.isFinite(value) ? value : 0
}
