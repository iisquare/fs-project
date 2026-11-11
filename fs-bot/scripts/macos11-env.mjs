#!/usr/bin/env node

import { createHash } from 'node:crypto'
import {
  accessSync,
  constants,
  existsSync,
  lstatSync,
  mkdirSync,
  readFileSync,
  readdirSync,
  readlinkSync,
  renameSync,
  rmSync,
  symlinkSync,
  unlinkSync,
  writeFileSync
} from 'node:fs'
import { homedir } from 'node:os'
import { delimiter, dirname, isAbsolute, join, resolve } from 'node:path'
import { spawn, spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const SCRIPT_PATH = fileURLToPath(import.meta.url)
const ROOT_DIR = resolve(dirname(SCRIPT_PATH), '..')
const CACHE_BASE = join(
  process.env.XDG_CACHE_HOME || join(homedir(), 'Library', 'Caches'),
  'fs-bot',
  'macos11'
)
const LOCK_DIR = join(CACHE_BASE, 'active.lock')
const BACKUP_NODE_MODULES = join(ROOT_DIR, 'dist', '.node_modules-mainline-macos11')
const COMPAT = {
  electron: '37.10.3',
  vite: '~7.2.7',
  esbuild: '0.25.12'
}

let activeChild = null
let lockHeld = false
let modulesActivated = false
let envDir = ''

function usage() {
  console.log(`Usage:
  node scripts/macos11-env.mjs dev       Launch the development app
  node scripts/macos11-env.mjs start     Preview an existing production build
  node scripts/macos11-env.mjs build     Build a macOS 11 DMG and ZIP
  node scripts/macos11-env.mjs shell     Open a shell using the compatibility environment
  node scripts/macos11-env.mjs install   Prepare dependencies without launching
  node scripts/macos11-env.mjs status    Show the environment state
  node scripts/macos11-env.mjs restore   Recover mainline node_modules after an interruption
  node scripts/macos11-env.mjs clean     Remove the isolated compatibility environment

The mainline package.json, lockfile, pnpm-workspace.yaml, postinstall script,
and electron-builder.yml are never modified. The current package.json and
lockfile are read every time the script runs.

Optional environment variables:
  FS_BOT_NODE_VERSION       Node major version to use (default: 22)
  FS_BOT_PNPM_VERSION       pnpm version to use (default: 11.4.0)
  FS_BOT_MAC11_ARCH         Build architecture: x64 or arm64`)
}

function info(message) {
  console.log(`[macos11] ${message}`)
}

function die(message) {
  throw new Error(message)
}

function pathExists(target) {
  try {
    lstatSync(target)
    return true
  } catch {
    return false
  }
}

function findExecutable(name, searchPath = process.env.PATH || '') {
  if (isAbsolute(name)) {
    return isExecutable(name) ? name : null
  }

  for (const directory of searchPath.split(delimiter)) {
    if (!directory) continue
    const candidate = join(directory, name)
    if (isExecutable(candidate)) return candidate
  }
  return null
}

function isExecutable(target) {
  try {
    accessSync(target, constants.X_OK)
    return true
  } catch {
    return false
  }
}

function majorVersion(value) {
  return String(value).split('.')[0]
}

function findRequestedNode(wantedMajor) {
  const candidates = [
    join(homedir(), '.nvm', 'versions', 'node', `v${wantedMajor}`, 'bin', 'node'),
    join('/opt/homebrew/opt', `node@${wantedMajor}`, 'bin', 'node'),
    join('/usr/local/opt', `node@${wantedMajor}`, 'bin', 'node')
  ]

  const nvmRoot = join(homedir(), '.nvm', 'versions', 'node')
  if (existsSync(nvmRoot)) {
    const versions = readdirSync(nvmRoot)
      .filter((name) => name.startsWith(`v${wantedMajor}.`))
      .sort((left, right) => right.localeCompare(left, undefined, { numeric: true }))
    candidates.unshift(...versions.map((name) => join(nvmRoot, name, 'bin', 'node')))
  }

  const direct = findExecutable(`node@${wantedMajor}`)
  if (direct) candidates.push(direct)
  return candidates.find(isExecutable) || null
}

function ensureRequestedNode() {
  const wantedMajor = process.env.FS_BOT_NODE_VERSION || '22'
  const currentMajor = majorVersion(process.versions.node)
  if (currentMajor === wantedMajor) return

  if (process.env.FS_BOT_NODE_REEXEC !== '1') {
    const executable = findRequestedNode(wantedMajor)
    if (executable) {
      info(`Restarting with Node.js ${wantedMajor} (${executable})`)
      const result = spawnSync(executable, [SCRIPT_PATH, ...process.argv.slice(2)], {
        stdio: 'inherit',
        env: {
          ...process.env,
          FS_BOT_NODE_REEXEC: '1'
        }
      })
      process.exit(result.status ?? 1)
    }

    const fnm = findExecutable('fnm')
    if (fnm) {
      const probe = spawnSync(
        fnm,
        ['exec', `--using=${wantedMajor}`, 'node', '-p', 'process.version'],
        { encoding: 'utf8' }
      )
      if (probe.status === 0) {
        info(`Restarting with Node.js ${wantedMajor} through fnm`)
        const result = spawnSync(
          fnm,
          ['exec', `--using=${wantedMajor}`, 'node', SCRIPT_PATH, ...process.argv.slice(2)],
          {
            stdio: 'inherit',
            env: {
              ...process.env,
              FS_BOT_NODE_REEXEC: '1'
            }
          }
        )
        process.exit(result.status ?? 1)
      }
    }
  }

  die(
    `Node.js ${wantedMajor}.x is required for macOS 11 ` +
      `(detected ${process.version}); install it with nvm, fnm, or Homebrew`
  )
}

function prependNodePath() {
  const nodeDirectory = dirname(process.execPath)
  process.env.PATH = `${nodeDirectory}${delimiter}${process.env.PATH || ''}`
  process.env.NODE = process.execPath
  process.env.npm_node_execpath = process.execPath
}

function ensureHost() {
  if (process.platform !== 'darwin') {
    die('this environment script must run on macOS')
  }

  const result = spawnSync('sw_vers', ['-productVersion'], {
    encoding: 'utf8'
  })
  if (result.status !== 0) die('unable to read the macOS version')

  const version = result.stdout.trim()
  if (Number(majorVersion(version)) < 11) {
    die(`macOS 11 or newer is required (detected ${version})`)
  }
}

function ensureXcode() {
  const xcodeSelect = findExecutable('xcode-select')
  if (!xcodeSelect) {
    die('Xcode Command Line Tools are required; run: xcode-select --install')
  }

  const result = spawnSync(xcodeSelect, ['-p'], { stdio: 'ignore' })
  if (result.status !== 0) {
    die('Xcode Command Line Tools are not configured; run: xcode-select --install')
  }
}

function ensurePnpm() {
  let pnpm = findExecutable('pnpm')
  if (!pnpm) {
    const corepack = findExecutable('corepack')
    if (!corepack) {
      die('pnpm is missing; install Node.js 22 with Corepack enabled')
    }

    const version = process.env.FS_BOT_PNPM_VERSION || '11.4.0'
    info(`Activating pnpm ${version} with Corepack`)
    const result = spawnSync(corepack, ['prepare', `pnpm@${version}`, '--activate'], {
      stdio: 'inherit'
    })
    if (result.status !== 0) die('pnpm activation failed')
    pnpm = findExecutable('pnpm')
    if (!pnpm) die('pnpm activation failed')
  }

  const result = spawnSync(pnpm, ['--version'], { encoding: 'utf8' })
  const version = result.status === 0 ? result.stdout.trim() : ''
  if (!version || Number(majorVersion(version)) < 10) {
    die(`pnpm 10 or newer is required (detected ${version || 'unknown'})`)
  }
  info(`Using pnpm ${version}`)
}

function acquireLock() {
  mkdirSync(CACHE_BASE, { recursive: true })

  try {
    mkdirSync(LOCK_DIR)
    lockHeld = true
  } catch (error) {
    if (error.code !== 'EEXIST') throw error

    const pidFile = join(LOCK_DIR, 'pid')
    const owner = existsSync(pidFile) ? Number(readFileSync(pidFile, 'utf8').trim()) : 0

    if (owner && isProcessRunning(owner)) {
      die(`another macOS 11 environment process is active (pid ${owner})`)
    }

    rmSync(LOCK_DIR, { recursive: true, force: true })
    mkdirSync(LOCK_DIR)
    lockHeld = true
  }

  writeFileSync(join(LOCK_DIR, 'pid'), `${process.pid}\n`)
}

function isProcessRunning(pid) {
  try {
    process.kill(pid, 0)
    return true
  } catch (error) {
    return error.code === 'EPERM'
  }
}

function releaseLock() {
  if (!lockHeld) return
  rmSync(LOCK_DIR, { recursive: true, force: true })
  lockHeld = false
}

function currentArchitecture(applyBuildOverride = false) {
  const arch = process.arch
  if (arch !== 'x64' && arch !== 'arm64') {
    die(`unsupported Node.js architecture: ${arch}`)
  }

  if (!applyBuildOverride) return arch

  const selected = process.env.FS_BOT_MAC11_ARCH || arch
  if (selected !== 'x64' && selected !== 'arm64') {
    die('FS_BOT_MAC11_ARCH must be x64 or arm64')
  }
  if (selected !== arch) {
    die(
      `build architecture ${selected} does not match Node.js architecture ` +
        `${arch}; use a matching Node.js installation`
    )
  }
  return arch
}

function writeEnvironmentFiles(arch) {
  envDir = join(CACHE_BASE, `node${process.env.FS_BOT_NODE_VERSION || '22'}-${arch}`)
  mkdirSync(envDir, { recursive: true })

  const mainline = JSON.parse(readFileSync(join(ROOT_DIR, 'package.json'), 'utf8'))
  const manifest = structuredClone(mainline)
  manifest.dependencies = manifest.dependencies || {}
  manifest.devDependencies = manifest.devDependencies || {}
  manifest.scripts = manifest.scripts || {}

  // Electron 38 dropped macOS 11. The esbuild override is written to the
  // isolated workspace because its newer macOS binaries require macOS 12.
  // Vite stays on the verified 7.2.x line; all other versions follow mainline.
  manifest.devDependencies.electron = COMPAT.electron
  manifest.devDependencies.vite = COMPAT.vite
  manifest.devDependencies.esbuild = COMPAT.esbuild
  delete manifest.scripts.postinstall

  writeFileSync(join(envDir, 'package.json'), `${JSON.stringify(manifest, null, 2)}\n`)

  writeFileSync(
    join(envDir, 'pnpm-workspace.yaml'),
    `packages:
  - .

allowBuilds:
  '@parcel/watcher': true
  better-sqlite3-multiple-ciphers: true
  electron: true
  electron-winstaller: true
  esbuild: true
  koffi: false

onlyBuiltDependencies:
  - '@parcel/watcher'
  - better-sqlite3-multiple-ciphers
  - electron
  - electron-winstaller
  - esbuild

overrides:
  esbuild: ${COMPAT.esbuild}
  '@nut-tree-fork/node-mac-permissions': npm:@computer-use/node-mac-permissions@2.2.2
  follow-redirects: 1.16.2
`
  )

  rmSync(join(envDir, 'scripts', 'postinstall.mjs'), { force: true })

  const builderConfig = {
    appId: 'com.iisquare.fs.bot',
    productName: manifest.productName || manifest.name || 'fs-bot',
    electronVersion: COMPAT.electron,
    directories: {
      buildResources: join(ROOT_DIR, 'build'),
      output: join(ROOT_DIR, 'dist', 'macos11')
    },
    files: [
      {
        from: join(ROOT_DIR, 'out'),
        to: 'out',
        filter: ['**/*']
      },
      {
        from: join(ROOT_DIR, 'resources'),
        to: 'resources',
        filter: ['**/*']
      },
      'package.json'
    ],
    asarUnpack: [
      'resources/**',
      'node_modules/better-sqlite3-multiple-ciphers/**',
      'node_modules/@nut-tree-fork/**',
      'node_modules/@computer-use/**'
    ],
    mac: {
      category: 'public.app-category.productivity',
      target: [
        { target: 'dmg', arch: [arch] },
        { target: 'zip', arch: [arch] }
      ],
      minimumSystemVersion: '11.0',
      artifactName: '${productName}-${version}-mac11-${arch}.${ext}',
      entitlements: join(ROOT_DIR, 'build', 'entitlements.mac.plist'),
      entitlementsInherit: join(ROOT_DIR, 'build', 'entitlements.mac.plist'),
      hardenedRuntime: false,
      identity: '-',
      notarize: false,
      extendInfo: {
        NSCameraUsageDescription: 'Application requests access to the device camera.',
        NSMicrophoneUsageDescription: 'Application requests access to the microphone.',
        NSDocumentsFolderUsageDescription: 'Application requests access to Documents.',
        NSDownloadsFolderUsageDescription: 'Application requests access to Downloads.'
      }
    },
    dmg: {
      artifactName: '${productName}-${version}-mac11-${arch}.${ext}'
    },
    npmRebuild: false,
    electronDownload: {
      mirror: 'https://npmmirror.com/mirrors/electron/'
    }
  }

  writeFileSync(
    join(envDir, 'electron-builder.mac11.json'),
    `${JSON.stringify(builderConfig, null, 2)}\n`
  )
}

function environmentHash() {
  const hash = createHash('sha256')
  for (const file of [join(envDir, 'package.json'), join(envDir, 'pnpm-workspace.yaml')]) {
    hash.update(readFileSync(file))
  }

  const mainlineLock = join(ROOT_DIR, 'pnpm-lock.yaml')
  if (existsSync(mainlineLock)) hash.update(readFileSync(mainlineLock))
  return hash.digest('hex')
}

function installedPackageVersion(packageName) {
  const manifest = join(envDir, 'node_modules', packageName, 'package.json')
  if (!existsSync(manifest)) return null
  try {
    return JSON.parse(readFileSync(manifest, 'utf8')).version || null
  } catch {
    return null
  }
}

function installedNutMacPermissionsVersion() {
  const pnpmDir = join(envDir, 'node_modules', '.pnpm')
  let entries = []
  try {
    entries = readdirSync(pnpmDir, { withFileTypes: true })
  } catch {
    return null
  }
  const match = entries.find(
    (entry) => entry.isDirectory() && entry.name.startsWith('@computer-use+node-mac-permissions@')
  )
  if (!match) return null
  const packageDir = join(
    pnpmDir,
    match.name,
    'node_modules',
    '@computer-use',
    'node-mac-permissions'
  )
  if (!existsSync(join(packageDir, 'build', 'Release', 'permissions.node'))) return null
  return match.name.slice('@computer-use+node-mac-permissions@'.length)
}

function packageBinEntry(baseDir, packageName, binName) {
  const packageDir = join(baseDir, 'node_modules', packageName)
  const manifest = JSON.parse(readFileSync(join(packageDir, 'package.json'), 'utf8'))
  const relativeEntry = typeof manifest.bin === 'string' ? manifest.bin : manifest.bin?.[binName]
  if (!relativeEntry) {
    die(`${packageName} does not expose the ${binName} command`)
  }

  const entry = join(packageDir, relativeEntry)
  if (!existsSync(entry)) {
    die(`${packageName} command entry is missing: ${entry}`)
  }
  return entry
}

function compatibilityRuntimeIsReady() {
  const installedVite = installedPackageVersion('vite')
  const installedPermissions = installedNutMacPermissionsVersion()
  const versionsMatch =
    installedPackageVersion('electron') === COMPAT.electron &&
    installedVite?.startsWith('7.2.') &&
    installedPackageVersion('esbuild') === COMPAT.esbuild &&
    ('darwin' !== process.platform || installedPermissions === '2.2.2')

  return (
    versionsMatch &&
    isExecutable(join(envDir, 'node_modules', '.bin', 'electron-vite')) &&
    isExecutable(join(envDir, 'node_modules', '.bin', 'electron-builder')) &&
    existsSync(join(envDir, 'node_modules', 'electron', 'dist', 'Electron.app'))
  )
}

function dependencyCacheIsReady(hash) {
  const stateFile = join(envDir, '.manifest-hash')
  return (
    existsSync(stateFile) &&
    readFileSync(stateFile, 'utf8').trim() === hash &&
    compatibilityRuntimeIsReady()
  )
}

function ensureDependencies() {
  const hash = environmentHash()
  if (dependencyCacheIsReady(hash)) {
    info('Using cached compatibility dependencies')
    return
  }

  const pnpm = findExecutable('pnpm')
  if (!pnpm) die('pnpm is not available')

  info('Installing macOS 11 compatibility dependencies')
  const stateFile = join(envDir, '.manifest-hash')
  const previousHash = existsSync(stateFile) ? readFileSync(stateFile, 'utf8').trim() : ''
  const args = ['install', '--no-frozen-lockfile']
  if (previousHash === hash && !compatibilityRuntimeIsReady()) {
    info('Repairing an incomplete compatibility dependency cache')
    args.push('--force')
  }

  const result = spawnSync(pnpm, args, {
    cwd: envDir,
    stdio: 'inherit',
    env: {
      ...process.env,
      ELECTRON_MIRROR: process.env.ELECTRON_MIRROR || 'https://npmmirror.com/mirrors/electron/',
      ELECTRON_BUILDER_BINARIES_MIRROR:
        process.env.ELECTRON_BUILDER_BINARIES_MIRROR ||
        'https://npmmirror.com/mirrors/electron-builder-binaries/',
      npm_config_userconfig: join(ROOT_DIR, '.npmrc')
    }
  })

  if (result.status !== 0) {
    die('compatibility dependency installation failed')
  }

  info('Rebuilding native dependencies for Electron')
  const electronBuilderEntry = packageBinEntry(envDir, 'electron-builder', 'electron-builder')
  const rebuild = spawnSync(process.execPath, [electronBuilderEntry, 'install-app-deps'], {
    cwd: envDir,
    stdio: 'inherit',
    env: {
      ...process.env,
      ELECTRON_MIRROR: process.env.ELECTRON_MIRROR || 'https://npmmirror.com/mirrors/electron/',
      ELECTRON_BUILDER_BINARIES_MIRROR:
        process.env.ELECTRON_BUILDER_BINARIES_MIRROR ||
        'https://npmmirror.com/mirrors/electron-builder-binaries/'
    }
  })
  if (rebuild.status !== 0) {
    die('native dependency rebuild for Electron failed')
  }
  if (!compatibilityRuntimeIsReady()) {
    const detected = {
      electron: installedPackageVersion('electron'),
      vite: installedPackageVersion('vite'),
      esbuild: installedPackageVersion('esbuild'),
      'node-mac-permissions': installedNutMacPermissionsVersion()
    }
    die(
      'the compatibility dependency installation is incomplete or has ' +
        `unexpected versions: ${JSON.stringify(detected)}`
    )
  }

  writeFileSync(stateFile, `${hash}\n`)
}

function layoutState() {
  const rootNodeModules = join(ROOT_DIR, 'node_modules')
  let target = null
  if (pathExists(rootNodeModules)) {
    const stat = lstatSync(rootNodeModules)
    if (stat.isSymbolicLink()) target = readlinkSync(rootNodeModules)
  }

  return {
    rootNodeModules,
    target,
    rootExists: pathExists(rootNodeModules),
    backupExists: pathExists(BACKUP_NODE_MODULES)
  }
}

function activateNodeModules() {
  mkdirSync(join(ROOT_DIR, 'dist'), { recursive: true })
  const compatibilityModules = join(envDir, 'node_modules')
  let state = layoutState()

  if (state.backupExists) {
    if (state.rootExists && state.target !== compatibilityModules) {
      die('both node_modules and the mainline backup exist; run the restore command')
    }
    if (!state.rootExists) {
      renameSync(BACKUP_NODE_MODULES, state.rootNodeModules)
      state = layoutState()
    }
  }

  if (state.rootExists) {
    if (state.target && state.target !== compatibilityModules) {
      die(`node_modules is an unexpected symlink: ${state.target}`)
    }
    if (!state.target) {
      renameSync(state.rootNodeModules, BACKUP_NODE_MODULES)
    }
  }

  if (!pathExists(state.rootNodeModules)) {
    symlinkSync(compatibilityModules, state.rootNodeModules, 'dir')
  }
  modulesActivated = true
}

function restoreNodeModules() {
  const rootNodeModules = join(ROOT_DIR, 'node_modules')
  if (pathExists(rootNodeModules)) {
    const stat = lstatSync(rootNodeModules)
    if (stat.isSymbolicLink()) {
      const target = readlinkSync(rootNodeModules)
      if (target.startsWith(`${CACHE_BASE}/`)) {
        unlinkSync(rootNodeModules)
      }
    }
  }

  if (pathExists(BACKUP_NODE_MODULES)) {
    if (pathExists(rootNodeModules)) {
      info(`Mainline node_modules is present; backup remains at ${BACKUP_NODE_MODULES}`)
    } else {
      renameSync(BACKUP_NODE_MODULES, rootNodeModules)
      info('Restored mainline node_modules')
    }
  }
  modulesActivated = false
}

function compatibilityEnvironment() {
  return {
    ...process.env,
    PATH: `${join(ROOT_DIR, 'node_modules', '.bin')}${delimiter}${process.env.PATH || ''}`,
    ELECTRON_MIRROR: process.env.ELECTRON_MIRROR || 'https://npmmirror.com/mirrors/electron/',
    ELECTRON_BUILDER_BINARIES_MIRROR:
      process.env.ELECTRON_BUILDER_BINARIES_MIRROR ||
      'https://npmmirror.com/mirrors/electron-builder-binaries/',
    FS_BOT_MAC11_ACTIVE: '1'
  }
}

function runForeground(executable, args, cwd, env = compatibilityEnvironment()) {
  return new Promise((resolvePromise, rejectPromise) => {
    const child = spawn(executable, args, {
      cwd,
      env,
      stdio: 'inherit'
    })
    let settled = false
    activeChild = child
    child.once('error', (error) => {
      if (settled) return
      settled = true
      activeChild = null
      rejectPromise(error)
    })
    child.once('exit', (code, signal) => {
      if (settled) return
      settled = true
      activeChild = null
      if (code === 0) {
        resolvePromise()
        return
      }
      rejectPromise(
        new Error(
          signal
            ? `${executable} terminated by ${signal}`
            : `${executable} exited with code ${code}`
        )
      )
    })
  })
}

async function runDev(args) {
  activateNodeModules()
  info(`Starting development mode with Electron ${COMPAT.electron}`)
  await runForeground(
    process.execPath,
    [packageBinEntry(ROOT_DIR, 'electron-vite', 'electron-vite'), 'dev', ...args],
    ROOT_DIR
  )
}

async function runStart(args) {
  activateNodeModules()
  info(`Previewing the production build with Electron ${COMPAT.electron}`)
  await runForeground(
    process.execPath,
    [packageBinEntry(ROOT_DIR, 'electron-vite', 'electron-vite'), 'preview', ...args],
    ROOT_DIR
  )
}

async function runBuild(arch) {
  activateNodeModules()

  info('Running type checks')
  await runForeground(
    process.execPath,
    [
      packageBinEntry(ROOT_DIR, 'typescript', 'tsc'),
      '--noEmit',
      '-p',
      'tsconfig.node.json',
      '--composite',
      'false'
    ],
    ROOT_DIR
  )
  await runForeground(
    process.execPath,
    [
      packageBinEntry(ROOT_DIR, 'vue-tsc', 'vue-tsc'),
      '--noEmit',
      '-p',
      'tsconfig.web.json',
      '--composite',
      'false'
    ],
    ROOT_DIR
  )

  info('Building application bundle')
  await runForeground(
    process.execPath,
    [packageBinEntry(ROOT_DIR, 'electron-vite', 'electron-vite'), 'build'],
    ROOT_DIR
  )

  info(`Packaging macOS 11 artifacts for ${arch}`)
  await runForeground(
    process.execPath,
    [
      packageBinEntry(envDir, 'electron-builder', 'electron-builder'),
      '--mac',
      `--${arch}`,
      '--config',
      join(envDir, 'electron-builder.mac11.json')
    ],
    envDir
  )

  info(`Artifacts written to ${join(ROOT_DIR, 'dist', 'macos11')}`)
}

async function runShell() {
  activateNodeModules()
  info('Entering compatibility shell; the environment is restored after exit')
  console.log(`Direct commands:
  electron-vite dev
  electron-vite build

To package a DMG, exit this shell and run:
  node scripts/macos11-env.mjs build

Type exit to restore the mainline environment.`)

  await runForeground(process.env.SHELL || '/bin/zsh', [], ROOT_DIR)
}

function showStatus() {
  const state = layoutState()
  let modulesState = 'mainline'
  if (state.target && state.target.startsWith(`${CACHE_BASE}/`)) {
    modulesState = `compatibility (${state.target})`
  } else if (state.backupExists) {
    modulesState = state.rootExists
      ? 'conflict; mainline and backup both exist'
      : 'interrupted; run restore'
  }

  console.log(`root: ${ROOT_DIR}`)
  console.log(`cache: ${CACHE_BASE}`)
  console.log(`node: ${process.version} (${process.arch})`)
  console.log(`compat Electron: ${COMPAT.electron}`)
  console.log(`compat Vite: ${COMPAT.vite}`)
  console.log(`compat esbuild: ${COMPAT.esbuild}`)
  console.log(`node_modules: ${modulesState}`)
}

function cleanEnvironment() {
  const state = layoutState()
  if (state.target && state.target.startsWith(`${CACHE_BASE}/`)) {
    die('compatibility node_modules is active; exit that environment first')
  }
  if (state.backupExists) {
    die('the mainline node_modules backup exists; run restore first')
  }

  const expected = join(
    process.env.XDG_CACHE_HOME || join(homedir(), 'Library', 'Caches'),
    'fs-bot',
    'macos11'
  )
  if (CACHE_BASE !== expected) {
    die(`refusing to clean an unexpected path: ${CACHE_BASE}`)
  }

  rmSync(CACHE_BASE, { recursive: true, force: true })
  info(`Removed cache: ${CACHE_BASE}`)
}

function cleanup() {
  try {
    if (modulesActivated) restoreNodeModules()
    releaseLock()
  } catch (error) {
    console.error(`[macos11] cleanup failed: ${error.message}`)
  }
}

function installSignalHandlers() {
  for (const [signal, code] of [
    ['SIGINT', 130],
    ['SIGTERM', 143],
    ['SIGHUP', 129]
  ]) {
    process.once(signal, () => {
      if (activeChild && activeChild.exitCode === null) {
        activeChild.kill(signal)
      }
      cleanup()
      process.exit(code)
    })
  }
  process.once('exit', cleanup)
}

async function main() {
  const command = process.argv[2] || 'help'
  const args = process.argv.slice(3)

  if (command === 'help' || command === '-h' || command === '--help') {
    usage()
    return
  }
  if (command === 'status') {
    showStatus()
    return
  }
  if (command === 'clean') {
    if (args.length > 0) die('clean does not accept extra arguments')
    acquireLock()
    installSignalHandlers()
    cleanEnvironment()
    return
  }
  if (command === 'restore') {
    if (args.length > 0) die('restore does not accept extra arguments')
    acquireLock()
    installSignalHandlers()
    restoreNodeModules()
    return
  }
  if (!['dev', 'start', 'build', 'shell', 'install'].includes(command)) {
    usage()
    die(`unknown command: ${command}`)
  }
  if (command !== 'dev' && command !== 'start' && args.length > 0) {
    die(`${command} does not accept extra arguments`)
  }
  if (command === 'start' && !existsSync(join(ROOT_DIR, 'out', 'main', 'index.js'))) {
    die('production build is missing; run the build command first')
  }

  ensureHost()
  ensureRequestedNode()
  prependNodePath()
  info(`Using Node.js ${process.version} (${process.execPath})`)
  ensureXcode()
  ensurePnpm()
  installSignalHandlers()
  acquireLock()

  const arch = currentArchitecture(command === 'build')
  writeEnvironmentFiles(arch)
  ensureDependencies()
  info(`node-mac-permissions: ${installedNutMacPermissionsVersion()}`)

  if (command === 'install') {
    info('Compatibility environment is ready')
    return
  }
  if (command === 'dev') {
    await runDev(args)
    return
  }
  if (command === 'start') {
    await runStart(args)
    return
  }
  if (command === 'build') {
    await runBuild(arch)
    return
  }
  if (command === 'shell') {
    await runShell()
  }
}

function isDirectInvocation() {
  return Boolean(process.argv[1]) && resolve(process.argv[1]) === SCRIPT_PATH
}

export { COMPAT, writeEnvironmentFiles }

if (isDirectInvocation()) {
  main()
    .catch((error) => {
      console.error(`error: ${error.message}`)
      process.exitCode = 1
    })
    .finally(cleanup)
}
