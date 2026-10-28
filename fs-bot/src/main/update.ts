import { app } from 'electron'
import { spawn } from 'child_process'
import { createHash } from 'crypto'
import {
  chmodSync,
  closeSync,
  createReadStream,
  createWriteStream,
  existsSync,
  mkdirSync,
  openSync,
  readdirSync,
  readFileSync,
  readSync,
  renameSync,
  rmSync,
  statSync,
  writeSync,
  writeFileSync
} from 'fs'
import { get as httpGet, type ClientRequest, type IncomingMessage } from 'http'
import { get as httpsGet } from 'https'
import { basename, join } from 'path'
import { Transform } from 'stream'
import { pipeline } from 'stream/promises'

export interface UpdateInstallRequest {
  url: string
  token: string
  fileName: string
  fileSize: number
  sha512: string
  version: string
  currentVersion: string
  platform: string
  architecture: string
  delta?: DeltaManifest | null
}

export interface UpdateProgress {
  received: number
  total: number
  percent: number
}

export interface UpdateInstallResult {
  path: string
  sha512: string
}

export interface UpdateStateStatus {
  state: 'none' | 'installed-unconfirmed' | 'install-failed'
  currentVersion: string
  targetVersion: string
  fromVersion: string
  startedAt: number
}

export interface UpdateControlStatus {
  state: 'idle' | 'downloading' | 'paused' | 'cancelled' | 'installing' | 'error'
  received: number
  total: number
  percent: number
  message: string
}

interface PendingUpdate {
  targetVersion: string
  fromVersion: string
  startedAt: number
  installerFile: string
  cacheDir: string
}

interface DeltaBlock {
  checksum: string
  size: number
}

export interface DeltaManifest {
  blockSize: number
  blocks: DeltaBlock[]
}

interface CachedPackageMeta {
  version: string
  platform: string
  architecture: string
  fileName: string
  sha512: string
}

interface CachedPackage extends CachedPackageMeta {
  file: string
}

const MAX_REDIRECT = 5
const IDLE_TIMEOUT = 60 * 1000
const UPDATE_STATE_FILE = 'client-update-state.json'

let installing = false
let updateStatePath = ''
let updateCacheDir = ''
let controlState: UpdateControlStatus['state'] = 'idle'
let controlReceived = 0
let controlTotal = 0
let controlPercent = 0
let controlMessage = ''
let pauseRequested = false
let cancelRequested = false
let activeRequest: ClientRequest | null = null

class UpdateInterruptedError extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'UpdateInterruptedError'
  }
}

/** 初始化跨版本保留的更新状态文件；运行目录在更新前后保持不变。 */
export function initUpdateState(runtimeDir: string): void {
  const directory = join(runtimeDir, 'userdata')
  mkdirSync(directory, { recursive: true })
  updateStatePath = join(directory, UPDATE_STATE_FILE)
  updateCacheDir = join(directory, 'updates')
  mkdirSync(updateCacheDir, { recursive: true })
}

/** 当前启动版本与待更新目标的对应关系，用于识别“仍启动旧版”的安装失败。 */
export function updateState(): UpdateStateStatus {
  const currentVersion = app.getVersion()
  const pending = readPending()
  if (!pending) {
    return {
      state: 'none',
      currentVersion,
      targetVersion: '',
      fromVersion: '',
      startedAt: 0
    }
  }
  const rollbackTarget =
    !!pending.fromVersion && compareVersion(pending.fromVersion, pending.targetVersion) > 0
  const reached = rollbackTarget
    ? compareVersion(currentVersion, pending.targetVersion) === 0
    : compareVersion(currentVersion, pending.targetVersion) >= 0
  return {
    state: reached ? 'installed-unconfirmed' : 'install-failed',
    currentVersion,
    targetVersion: pending.targetVersion,
    fromVersion: pending.fromVersion,
    startedAt: pending.startedAt
  }
}

/**
 * 新版本完成渲染进程启动后调用。只有当前版本达到或超过目标版本才清除待更新状态；
 * 旧版本重新启动时保留标记，由下次版本检查识别并重新升级。
 */
export function reportUpdateHealthy(): UpdateStateStatus {
  const status = updateState()
  if (status.state === 'installed-unconfirmed') {
    const pending = readPending()
    if (pending) pruneUpdateCache(pending.cacheDir)
    clearPending()
  }
  return status
}

export function downloadControlStatus(): UpdateControlStatus {
  return {
    state: controlState,
    received: controlReceived,
    total: controlTotal,
    percent: controlPercent,
    message: controlMessage
  }
}

export function pauseUpdateDownload(): UpdateControlStatus {
  if (controlState !== 'downloading') return downloadControlStatus()
  pauseRequested = true
  controlState = 'paused'
  controlMessage = '下载已暂停'
  activeRequest?.destroy(new UpdateInterruptedError('下载已暂停'))
  return downloadControlStatus()
}

export function cancelUpdateDownload(): UpdateControlStatus {
  if (controlState === 'idle' || controlState === 'installing') return downloadControlStatus()
  cancelRequested = true
  pauseRequested = false
  controlState = 'cancelled'
  controlMessage = '下载已终止'
  activeRequest?.destroy(new UpdateInterruptedError('下载已终止'))
  return downloadControlStatus()
}

function throwIfInterrupted(): void {
  if (pauseRequested) throw new UpdateInterruptedError('下载已暂停')
  if (cancelRequested) throw new UpdateInterruptedError('下载已终止')
}

/**
 * 客户端安装包更新。
 *
 * 服务端给出安装包地址、大小与上传时计算的 SHA-512。这里下载到独立临时目录，
 * 边写边算 SHA-512；大小或摘要任一不一致都会删除文件并中止安装。
 */
export async function downloadAndInstallUpdate(
  request: UpdateInstallRequest,
  onProgress: (progress: UpdateProgress) => void
): Promise<UpdateInstallResult> {
  if (installing) throw new Error('安装包正在下载或安装，请稍候')
  const url = String(request?.url ?? '').trim()
  assertSecureUpdateUrl(url)
  const token = String(request?.token ?? '')
  const expectedSha512 = String(request?.sha512 ?? '')
    .trim()
    .toLowerCase()
  if (!/^[a-f0-9]{128}$/.test(expectedSha512)) {
    throw new Error('服务端未提供有效的安装包 SHA-512')
  }
  const expectedSize = Number(request?.fileSize ?? 0)
  if (!Number.isSafeInteger(expectedSize) || expectedSize <= 0) {
    throw new Error('服务端提供的安装包大小异常')
  }
  const fileName = safeFileName(request?.fileName, request?.version)
  if (!installerAllowed(fileName)) throw new Error('安装包格式与当前系统不匹配')

  installing = true
  pauseRequested = false
  cancelRequested = false
  controlState = 'downloading'
  controlReceived = 0
  controlTotal = expectedSize
  controlPercent = 0
  controlMessage = '正在准备下载'
  const progress = (value: UpdateProgress): void => {
    if (controlState === 'downloading') {
      controlReceived = value.received
      controlTotal = value.total
      controlPercent = value.percent
      controlMessage = '正在下载安装包'
    }
    onProgress(value)
  }
  if (!updateCacheDir) throw new Error('更新缓存目录未初始化')
  const directory = join(updateCacheDir, expectedSha512.substring(0, 32))
  mkdirSync(directory, { recursive: true })
  const target = join(directory, fileName)
  const partFile = target + '.part'
  try {
    let actualSha512 = ''
    if (existsSync(target) && statSync(target).size === expectedSize) {
      actualSha512 = await hashFileSha512(target)
      if (actualSha512 !== expectedSha512) {
        rmSync(target, { force: true })
        actualSha512 = ''
      }
    }
    if (!actualSha512) {
      let differential = false
      if (request.delta && (!existsSync(partFile) || statSync(partFile).size === 0)) {
        const previous = findCachedPackage(
          request.currentVersion,
          request.platform,
          request.architecture,
          directory
        )
        if (previous) {
          try {
            await reconstructDelta(
              url,
              token,
              previous.file,
              partFile,
              request.delta,
              expectedSize,
              progress
            )
            differential = true
          } catch (error) {
            if (error instanceof UpdateInterruptedError || pauseRequested || cancelRequested) {
              throw error
            }
            console.warn('[Update] Differential download failed, fallback to full package:', error)
            rmSync(partFile, { force: true })
          }
        }
      }
      if (!differential) {
        await downloadResume(url, token, partFile, expectedSize, progress)
      }
      const partSize = existsSync(partFile) ? statSync(partFile).size : 0
      if (partSize !== expectedSize) {
        throw new Error(`安装包下载不完整：期望 ${expectedSize} 字节，实际 ${partSize} 字节`)
      }
      actualSha512 = await hashFileSha512(partFile)
      throwIfInterrupted()
      if (actualSha512 !== expectedSha512) {
        rmSync(partFile, { force: true })
        throw new Error(`安装包 SHA-512 校验失败：期望 ${expectedSha512}，实际 ${actualSha512}`)
      }
      rmSync(target, { force: true })
      renameSync(partFile, target)
    }
    if (actualSha512 !== expectedSha512) {
      throw new Error(`安装包 SHA-512 校验失败：期望 ${expectedSha512}，实际 ${actualSha512}`)
    }
    throwIfInterrupted()
    writeCachedPackage(directory, {
      version: String(request.version),
      platform: String(request.platform),
      architecture: String(request.architecture),
      fileName,
      sha512: actualSha512
    })
    progress({ received: expectedSize, total: expectedSize, percent: 100 })
    throwIfInterrupted()
    writePending({
      targetVersion: String(request.version),
      fromVersion: String(request.currentVersion),
      startedAt: Date.now(),
      installerFile: fileName,
      cacheDir: directory
    })
    controlState = 'installing'
    controlPercent = 100
    controlMessage = '正在启动安装程序'
    try {
      await launchInstaller(target, directory)
    } catch (error) {
      clearPending()
      throw error
    }
    setTimeout(() => app.quit(), 500)
    return { path: target, sha512: actualSha512 }
  } catch (error) {
    if (!pauseRequested && !cancelRequested) {
      controlState = 'error'
      controlMessage = error instanceof Error ? error.message : String(error)
    }
    throw error
  } finally {
    installing = false
  }
}

interface CacheOperation {
  kind: 'copy' | 'download'
  start: number
  end: number
  oldStart?: number
}

async function downloadResume(
  url: string,
  token: string,
  target: string,
  expectedSize: number,
  onProgress: (progress: UpdateProgress) => void
): Promise<void> {
  throwIfInterrupted()
  let start = existsSync(target) ? statSync(target).size : 0
  if (start > expectedSize) {
    rmSync(target, { force: true })
    start = 0
  }
  if (start === expectedSize) {
    onProgress({ received: expectedSize, total: expectedSize, percent: 99 })
    return
  }
  throwIfInterrupted()
  const stream = await requestStream(url, token, start)
  if (stream.start === 0 && start > 0) {
    start = 0
    rmSync(target, { force: true })
  }
  const contentLength = Number(stream.response.headers['content-length'] ?? 0)
  const remaining = expectedSize - start
  if (contentLength > 0 && contentLength !== remaining) {
    stream.response.destroy()
    throw new Error(`安装包剩余大小不一致：期望 ${remaining}，响应头为 ${contentLength}`)
  }
  let received = start
  const meter = new Transform({
    transform(chunk: Buffer, _encoding, callback) {
      received += chunk.length
      if (received > expectedSize) {
        callback(new Error('实际下载的安装包超过服务端记录的大小'))
        return
      }
      onProgress({
        received,
        total: expectedSize,
        percent: Math.min(99, Math.round((received / expectedSize) * 100))
      })
      callback(null, chunk)
    }
  })
  await pipeline(
    stream.response,
    meter,
    createWriteStream(target, { flags: start > 0 ? 'a' : 'w' })
  )
}

async function reconstructDelta(
  url: string,
  token: string,
  oldFile: string,
  target: string,
  manifest: DeltaManifest,
  expectedSize: number,
  onProgress: (progress: UpdateProgress) => void
): Promise<void> {
  validateDeltaManifest(manifest, expectedSize)
  const oldBlocks = buildLocalBlockMap(oldFile, manifest.blockSize)
  const operations = buildDeltaOperations(manifest, oldBlocks)
  const downloadSize = operations
    .filter((operation) => operation.kind === 'download')
    .reduce((total, operation) => total + operation.end - operation.start, 0)
  const oldFd = openSync(oldFile, 'r')
  const outFd = openSync(target, 'w')
  let downloaded = 0
  try {
    for (const operation of operations) {
      throwIfInterrupted()
      const length = operation.end - operation.start
      if (operation.kind === 'copy') {
        copyRange(oldFd, outFd, operation.oldStart ?? 0, length)
        continue
      }
      const stream = await requestStream(url, token, operation.start, operation.end - 1)
      if (!stream.partial || stream.start !== operation.start) {
        stream.response.destroy()
        throw new Error('服务器未按请求返回差分 Range')
      }
      const contentLength = Number(stream.response.headers['content-length'] ?? 0)
      if (contentLength > 0 && contentLength !== length) {
        stream.response.destroy()
        throw new Error('差分 Range 响应长度不一致')
      }
      await new Promise<void>((resolve, reject) => {
        stream.response.on('data', (chunk: Buffer) => {
          try {
            writeAllSync(outFd, chunk)
            downloaded += chunk.length
            onProgress({
              received: downloaded,
              total: downloadSize,
              percent:
                downloadSize > 0 ? Math.min(99, Math.round((downloaded / downloadSize) * 100)) : 99
            })
          } catch (error) {
            stream.response.destroy()
            reject(error)
          }
        })
        stream.response.on('error', reject)
        stream.response.on('aborted', () => reject(new Error('差分下载响应被中止')))
        stream.response.on('end', resolve)
      })
    }
  } finally {
    closeSync(oldFd)
    closeSync(outFd)
  }
  if (statSync(target).size !== expectedSize) {
    throw new Error('差分重建后的安装包大小不一致')
  }
}

function validateDeltaManifest(manifest: DeltaManifest, expectedSize: number): void {
  const blockSize = Number(manifest.blockSize)
  if (!Number.isSafeInteger(blockSize) || blockSize <= 0 || !Array.isArray(manifest.blocks)) {
    throw new Error('差分清单格式异常')
  }
  let total = 0
  for (const block of manifest.blocks) {
    const size = Number(block.size)
    if (
      !/^[a-f0-9]{64}$/i.test(String(block.checksum)) ||
      !Number.isSafeInteger(size) ||
      size <= 0 ||
      size > blockSize
    ) {
      throw new Error('差分清单块信息异常')
    }
    total += size
  }
  if (total !== expectedSize) throw new Error('差分清单总大小与安装包不一致')
}

function buildLocalBlockMap(file: string, blockSize: number): Map<string, number> {
  const result = new Map<string, number>()
  const fd = openSync(file, 'r')
  const buffer = Buffer.allocUnsafe(blockSize)
  let offset = 0
  try {
    while (true) {
      let filled = 0
      while (filled < blockSize) {
        const length = readSync(fd, buffer, filled, blockSize - filled, offset + filled)
        if (!length) break
        filled += length
      }
      if (!filled) break
      const checksum = createHash('sha256').update(buffer.subarray(0, filled)).digest('hex')
      if (!result.has(checksum)) result.set(checksum, offset)
      offset += filled
    }
  } finally {
    closeSync(fd)
  }
  return result
}

function buildDeltaOperations(
  manifest: DeltaManifest,
  oldBlocks: Map<string, number>
): CacheOperation[] {
  const operations: CacheOperation[] = []
  let offset = 0
  for (const block of manifest.blocks) {
    const oldStart = oldBlocks.get(block.checksum)
    const last = operations[operations.length - 1]
    if (oldStart === undefined) {
      if (last?.kind === 'download' && last.end === offset) {
        last.end += block.size
      } else {
        operations.push({ kind: 'download', start: offset, end: offset + block.size })
      }
    } else if (
      last?.kind === 'copy' &&
      last.end === offset &&
      (last.oldStart ?? 0) + (last.end - last.start) === oldStart
    ) {
      last.end += block.size
    } else {
      operations.push({
        kind: 'copy',
        start: offset,
        end: offset + block.size,
        oldStart
      })
    }
    offset += block.size
  }
  return operations
}

function copyRange(oldFd: number, outFd: number, start: number, length: number): void {
  const buffer = Buffer.allocUnsafe(1024 * 1024)
  let copied = 0
  while (copied < length) {
    const expected = Math.min(buffer.length, length - copied)
    const count = readSync(oldFd, buffer, 0, expected, start + copied)
    if (count <= 0) throw new Error('旧安装包读取不完整，无法继续差分更新')
    writeAllSync(outFd, buffer.subarray(0, count))
    copied += count
  }
}

function writeAllSync(fd: number, data: Buffer): void {
  let written = 0
  while (written < data.length) {
    const count = writeSync(fd, data, written, data.length - written)
    if (count <= 0) throw new Error('写入安装包缓存失败')
    written += count
  }
}

function findCachedPackage(
  currentVersion: string,
  platform: string,
  architecture: string,
  exclude: string
): CachedPackage | null {
  if (!existsSync(updateCacheDir)) return null
  for (const name of readdirSync(updateCacheDir, { withFileTypes: true })) {
    if (!name.isDirectory()) continue
    const directory = join(updateCacheDir, name.name)
    if (directory === exclude) continue
    const metaFile = join(directory, 'package.json')
    if (!existsSync(metaFile)) continue
    try {
      const meta = JSON.parse(readFileSync(metaFile, 'utf8')) as CachedPackageMeta
      if (
        meta.version !== currentVersion ||
        meta.platform !== platform ||
        meta.architecture !== architecture
      ) {
        continue
      }
      const file = join(directory, meta.fileName)
      if (existsSync(file)) return { ...meta, file }
    } catch {
      // 损坏的缓存忽略，后续回退整包下载
    }
  }
  return null
}

function writeCachedPackage(directory: string, value: CachedPackageMeta): void {
  const file = join(directory, 'package.json')
  writeFileSync(file + '.tmp', JSON.stringify(value, null, 2), 'utf8')
  renameSync(file + '.tmp', file)
}

function pruneUpdateCache(keepDirectory: string): void {
  if (!keepDirectory || !updateCacheDir || !existsSync(updateCacheDir)) return
  for (const entry of readdirSync(updateCacheDir, { withFileTypes: true })) {
    if (!entry.isDirectory()) continue
    const directory = join(updateCacheDir, entry.name)
    if (directory !== keepDirectory) rmSync(directory, { recursive: true, force: true })
  }
}

function hashFileSha512(file: string): Promise<string> {
  return new Promise((resolve, reject) => {
    const hash = createHash('sha512')
    createReadStream(file)
      .on('error', reject)
      .on('data', (chunk) => hash.update(chunk))
      .on('end', () => resolve(hash.digest('hex')))
  })
}

async function launchInstaller(installerPath: string, workDir: string): Promise<void> {
  switch (process.platform) {
    case 'win32':
      if (!installerPath.toLowerCase().endsWith('-setup.exe')) {
        throw new Error('Windows 仅支持 electron-builder NSIS Setup 安装包自动更新')
      }
      // electron-builder NSIS：静默安装、应用退出后替换文件，并由安装器重新启动应用
      await spawnDetached(installerPath, ['--updated', '/S', '--force-run'], workDir)
      return
    case 'darwin':
      await installMacDmg(installerPath, workDir)
      return
    case 'linux':
      await installLinuxAppImage(installerPath, workDir)
      return
    default:
      throw new Error('当前系统不支持自动替换安装')
  }
}

async function installMacDmg(installerPath: string, workDir: string): Promise<void> {
  const target = currentMacBundle()
  const mountPoint = join(workDir, 'mount')
  const scriptPath = join(workDir, 'install-macos.sh')
  writeFileSync(
    scriptPath,
    `#!/bin/sh
set -eu
APP_PID="$1"
DMG="$2"
TARGET="$3"
MOUNT="$4"
STATE="$5"
STAGING="\${TARGET}.updating.$$"
BACKUP="\${TARGET}.backup.$$"
cleanup() {
  hdiutil detach "$MOUNT" -quiet >/dev/null 2>&1 || true
  if [ ! -d "$TARGET" ] && [ -d "$BACKUP" ]; then
    mv "$BACKUP" "$TARGET"
  fi
  open "$TARGET" >/dev/null 2>&1 || true
}
trap cleanup EXIT
while kill -0 "$APP_PID" >/dev/null 2>&1; do sleep 1; done
mkdir -p "$MOUNT"
hdiutil attach "$DMG" -nobrowse -readonly -mountpoint "$MOUNT" >/dev/null
SOURCE="$(find "$MOUNT" -maxdepth 1 -name '*.app' -type d | head -n 1)"
[ -n "$SOURCE" ]
rm -rf "$STAGING"
ditto "$SOURCE" "$STAGING"
mv "$TARGET" "$BACKUP"
if mv "$STAGING" "$TARGET"; then
  rm -rf "$BACKUP"
else
  mv "$BACKUP" "$TARGET"
  exit 1
fi
open "$TARGET" >/dev/null
attempt=0
while [ -e "$STATE" ] && [ "$attempt" -lt 30 ]; do
  sleep 1
  attempt=$((attempt + 1))
done
if [ -e "$STATE" ]; then
  pkill -f "$TARGET/Contents/MacOS/" >/dev/null 2>&1 || true
  rm -rf "$TARGET"
  mv "$BACKUP" "$TARGET"
  exit 1
fi
rm -rf "$BACKUP"
`,
    'utf8'
  )
  chmodSync(scriptPath, 0o755)
  await spawnDetached(
    '/bin/sh',
    [scriptPath, String(process.pid), installerPath, target, mountPoint, updateStatePath],
    workDir
  )
}

async function installLinuxAppImage(installerPath: string, workDir: string): Promise<void> {
  const target = String(process.env.APPIMAGE ?? '')
  if (!target || !target.startsWith('/')) {
    throw new Error('当前运行方式不是 AppImage，无法自动替换应用文件')
  }
  chmodSync(installerPath, 0o755)
  const scriptPath = join(workDir, 'install-linux.sh')
  writeFileSync(
    scriptPath,
    `#!/bin/sh
set -eu
APP_PID="$1"
SOURCE="$2"
TARGET="$3"
STATE="$4"
STAGING="\${TARGET}.updating.$$"
BACKUP="\${TARGET}.backup.$$"
NEW_PID=""
SHOULD_LAUNCH=1
cleanup() {
  if [ ! -f "$TARGET" ] && [ -f "$BACKUP" ]; then
    mv -f "$BACKUP" "$TARGET"
  fi
  if [ "$SHOULD_LAUNCH" -eq 1 ] && [ -x "$TARGET" ]; then
    nohup "$TARGET" >/dev/null 2>&1 &
  fi
}
trap cleanup EXIT
while kill -0 "$APP_PID" >/dev/null 2>&1; do sleep 1; done
cp "$SOURCE" "$STAGING"
chmod 755 "$STAGING"
mv -f "$TARGET" "$BACKUP"
mv -f "$STAGING" "$TARGET"
nohup "$TARGET" >/dev/null 2>&1 &
NEW_PID=$!"
SHOULD_LAUNCH=0
attempt=0
while [ -e "$STATE" ] && [ "$attempt" -lt 30 ]; do
  sleep 1
  attempt=$((attempt + 1))
done
if [ -e "$STATE" ]; then
  kill "$NEW_PID" >/dev/null 2>&1 || true
  wait "$NEW_PID" >/dev/null 2>&1 || true
  mv -f "$BACKUP" "$TARGET"
  SHOULD_LAUNCH=1
  exit 1
fi
rm -f "$BACKUP"
`,
    'utf8'
  )
  chmodSync(scriptPath, 0o755)
  await spawnDetached(
    '/bin/sh',
    [scriptPath, String(process.pid), installerPath, target, updateStatePath],
    workDir
  )
}

function currentMacBundle(): string {
  const appPath = app.getAppPath()
  const marker = appPath.indexOf('.app/')
  if (marker < 0) throw new Error('无法定位当前 macOS 应用包')
  return appPath.substring(0, marker + 4)
}

function spawnDetached(command: string, args: string[], cwd: string): Promise<void> {
  return new Promise((resolve, reject) => {
    const child = spawn(command, args, {
      cwd,
      detached: true,
      stdio: 'ignore',
      windowsHide: true
    })
    child.once('error', reject)
    child.once('spawn', () => {
      child.unref()
      resolve()
    })
  })
}

function readPending(): PendingUpdate | null {
  if (!updateStatePath || !existsSync(updateStatePath)) return null
  try {
    const value = JSON.parse(readFileSync(updateStatePath, 'utf8')) as PendingUpdate
    if (!value?.targetVersion || !Number.isFinite(Number(value.startedAt))) return null
    return {
      ...value,
      fromVersion: String(value.fromVersion ?? ''),
      cacheDir: String(value.cacheDir ?? '')
    }
  } catch {
    clearPending()
    return null
  }
}

function writePending(value: PendingUpdate): void {
  if (!updateStatePath) throw new Error('更新状态未初始化')
  const tempPath = updateStatePath + '.tmp'
  writeFileSync(tempPath, JSON.stringify(value, null, 2), 'utf8')
  renameSync(tempPath, updateStatePath)
}

function clearPending(): void {
  if (!updateStatePath) return
  rmSync(updateStatePath, { force: true })
  rmSync(updateStatePath + '.tmp', { force: true })
}

/** 仅用于安装后版本确认，沿用与后台一致的语义化版本比较口径。 */
function compareVersion(left: string, right: string): number {
  const parse = (value: string): { numbers: string[]; pre: string } => {
    const source = String(value).trim().replace(/^v/i, '').split('+', 1)[0]
    const index = source.indexOf('-')
    return {
      numbers: (index < 0 ? source : source.substring(0, index)).split('.'),
      pre: index < 0 ? '' : source.substring(index + 1)
    }
  }
  const a = parse(left)
  const b = parse(right)
  const count = Math.max(a.numbers.length, b.numbers.length)
  for (let index = 0; index < count; index++) {
    const x = (a.numbers[index] ?? '0').replace(/^0+(?!$)/, '')
    const y = (b.numbers[index] ?? '0').replace(/^0+(?!$)/, '')
    if (x.length !== y.length) return x.length - y.length
    if (x !== y) return x < y ? -1 : 1
  }
  if (a.pre === b.pre) return 0
  if (!a.pre) return 1
  if (!b.pre) return -1
  return a.pre < b.pre ? -1 : 1
}

interface UpdateStream {
  response: IncomingMessage
  start: number
  partial: boolean
}

function requestStream(
  url: string,
  token: string,
  start = 0,
  endInclusive?: number,
  redirect = 0
): Promise<UpdateStream> {
  return new Promise((resolve, reject) => {
    const target = assertSecureUpdateUrl(url)
    const client = 'https:' === target.protocol ? httpsGet : httpGet
    const headers: Record<string, string> = {
      Accept: 'application/octet-stream, application/x-msdownload, */*',
      ...(token ? { Authorization: 'Bearer ' + token } : {})
    }
    if (start > 0 || endInclusive !== undefined) {
      headers.Range = `bytes=${start}-${endInclusive === undefined ? '' : endInclusive}`
    }
    const request = client(
      target,
      {
        headers,
        timeout: IDLE_TIMEOUT
      },
      (response) => {
        const status = response.statusCode ?? 0
        if (status >= 300 && status < 400 && response.headers.location) {
          response.resume()
          if (redirect >= MAX_REDIRECT) {
            reject(new Error('下载安装包失败：重定向次数过多'))
            return
          }
          const next = new URL(response.headers.location, target).toString()
          const nextToken = new URL(next).origin === target.origin ? token : ''
          requestStream(next, nextToken, start, endInclusive, redirect + 1).then(resolve, reject)
          return
        }
        if (status < 200 || status >= 300) {
          response.resume()
          reject(new Error(`下载安装包失败：HTTP ${status}`))
          return
        }
        const partial = 206 === status
        if (partial) {
          const range = /^bytes\s+(\d+)-(\d+)\//i.exec(
            String(response.headers['content-range'] ?? '')
          )
          if (!range || Number(range[1]) !== start) {
            response.destroy()
            reject(new Error('服务器返回的 Range 起点与请求不一致'))
            return
          }
        }
        resolve({ response, start: partial ? start : 0, partial })
      }
    )
    activeRequest = request
    request.on('close', () => {
      if (activeRequest === request) activeRequest = null
    })
    request.on('timeout', () => request.destroy(new Error('下载安装包超时')))
    request.on('error', reject)
  })
}

/** 生产环境更新包必须走 HTTPS；开发环境保留 HTTP 以便本地联调。 */
function assertSecureUpdateUrl(value: string): URL {
  let target: URL
  try {
    target = new URL(value)
  } catch {
    throw new Error('更新地址格式异常')
  }
  if (target.protocol !== 'https:' && target.protocol !== 'http:') {
    throw new Error('更新地址协议异常')
  }
  const loopback = ['localhost', '127.0.0.1', '::1'].includes(target.hostname)
  if (app.isPackaged && target.protocol !== 'https:' && !loopback) {
    throw new Error('生产环境更新地址必须使用 HTTPS')
  }
  return target
}

function safeFileName(fileName: unknown, version: unknown): string {
  const source = basename(String(fileName ?? '').trim())
  const safe = Array.from(source)
    .map((character) => {
      return character.charCodeAt(0) < 32 || '<>:"/\\|?*'.includes(character) ? '_' : character
    })
    .join('')
    .trim()
  if (safe && safe !== '.' && safe !== '..') return safe
  const suffix =
    {
      win32: '.exe',
      darwin: '.dmg',
      linux: '.AppImage'
    }[process.platform] ?? '.bin'
  return `fs-bot-${String(version ?? 'update').replace(/[^0-9A-Za-z._-]/g, '_')}${suffix}`
}

function installerAllowed(fileName: string): boolean {
  const lower = fileName.toLowerCase()
  switch (process.platform) {
    case 'win32':
      return lower.endsWith('-setup.exe')
    case 'darwin':
      return lower.endsWith('.dmg')
    case 'linux':
      return lower.endsWith('.appimage')
    default:
      return false
  }
}
