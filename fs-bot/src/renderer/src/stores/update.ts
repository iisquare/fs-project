import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import ClientUpdateApi from '@renderer/api/ClientUpdateApi'
import Config from '@renderer/core/Config'
import ApiUtil from '@renderer/utils/ApiUtil'
import { errorMessage } from '@renderer/utils/ErrorUtil'
import { useUserStore } from '@renderer/stores/user'

export interface ClientUpdatePackage {
  id: number
  platform: string
  architecture: string
  channel: string
  version: string
  minimumVersion: string
  releaseNotes: string
  fileName: string
  fileSize: number
  sha512: string
  rolloutPercentage: number
  rollback: boolean
  deltaAvailable: boolean
  downloadUrl: string
  createdTime: number
}

export interface ClientUpdateDelta {
  fileName: string
  fileSize: number
  sha512: string
  downloadUrl: string
  manifest: {
    blockSize: number
    blocks: Array<{ checksum: string; size: number }>
  }
}

export interface ClientUpdateCheck {
  platform: string
  architecture: string
  currentVersion: string
  supported: boolean
  forceUpgrade: boolean
  rollback: boolean
  hasUpdate: boolean
  latest: ClientUpdatePackage | null
}

type UpdateStateStatus = Awaited<ReturnType<typeof window.ipc.update.status>>

/**
 * 客户端版本检查与安装。
 *
 * 启动检查在渲染进程首次挂载时执行，与登录态无关；低于服务端最低支持版本时弹窗不可关闭，
 * 普通新版本可以稍后提醒。检测到版本后只展示弹窗，下载必须由用户主动点击。
 */
export const useUpdateStore = defineStore('update', () => {
  const visible = ref(false)
  const checking = ref(false)
  const installing = ref(false)
  const paused = ref(false)
  const cancelled = ref(false)
  const checked = ref(false)
  const ignoring = ref(false)
  const ignoredVersion = ref('')
  const result = ref<ClientUpdateCheck | null>(null)
  const error = ref('')
  const failedInstall = ref<UpdateStateStatus | null>(null)
  const progress = ref(0)
  const received = ref(0)
  const total = ref(0)
  let listened = false

  const forceUpgrade = computed(() => result.value?.forceUpgrade === true)
  const rollback = computed(() => result.value?.rollback === true)
  const hasUpdate = computed(() => result.value?.hasUpdate === true)
  const latest = computed(() => result.value?.latest ?? null)
  const installFailed = computed(() => failedInstall.value !== null)
  const isLatestIgnored = computed(
    () => !!latest.value?.version && sameVersion(latest.value.version, ignoredVersion.value)
  )
  const canIgnore = computed(() => hasUpdate.value && !forceUpgrade.value && !rollback.value)
  const preparingUpdate = computed(
    () => installing.value && total.value === 0 && progress.value === 0
  )
  const installingProgram = computed(() => installing.value && progress.value === 100)
  const message = computed(() => {
    if (checking.value) return '正在检查最新版本...'
    if (installFailed.value) {
      return `上次安装 v${failedInstall.value?.targetVersion} 未完成`
    }
    if (error.value) return error.value
    if (paused.value) return '下载已暂停，可继续下载或终止'
    if (!result.value) return '尚未检查更新'
    if (!result.value.latest) return '服务端暂未发布当前系统的安装包'
    if (rollback.value) return `当前版本已停止使用，需要回滚到 v${latest.value?.version}`
    if (forceUpgrade.value) {
      return `当前版本已停止支持，最低可运行版本为 v${latest.value?.minimumVersion}`
    }
    if (hasUpdate.value) return `发现新版本 v${latest.value?.version}`
    return '当前已是最新版本，且仍在支持范围内'
  })

  function ensureProgressListener(): void {
    if (listened) return
    listened = true
    window.ipc.update.onProgress((value) => {
      progress.value = value.percent
      received.value = value.received
      total.value = value.total
    })
  }

  /** background 为 true 时是启动/定时/网络恢复检查；用户主动检查走 false。 */
  async function check(background = false): Promise<ClientUpdateCheck | null> {
    if (checking.value || installing.value) return null
    checking.value = true
    error.value = ''
    try {
      const versions = await window.ipc.clientMeta()
      const response = await ClientUpdateApi.check({
        currentVersion: versions.clientVersion,
        platform: versions.platform,
        architecture: versions.architecture,
        clientId: versions.clientId,
        channel: versions.channel
      })
      result.value = ApiUtil.data(response) as ClientUpdateCheck
      checked.value = true
      const settings = await Config.getSettings()
      const autoUpgrade = settings.autoUpgrade === true
      ignoredVersion.value = String(settings.ignoredUpdateVersion ?? '')
      const installState = await window.ipc.update.status()
      const targetVersion = result.value.latest?.version ?? ''
      failedInstall.value =
        installState.state === 'install-failed' &&
        (!targetVersion || sameVersion(installState.targetVersion, targetVersion))
          ? installState
          : null
      if (failedInstall.value) {
        error.value =
          `上次安装 v${installState.targetVersion} 未完成，当前仍为 v${installState.currentVersion}。` +
          '请确认后点击“重新安装”手动重试。'
      }
      const ignoreLatest = canIgnore.value && isLatestIgnored.value
      visible.value =
        forceUpgrade.value ||
        rollback.value ||
        (hasUpdate.value && !ignoreLatest && (!background || autoUpgrade))
      return result.value
    } catch (e) {
      checked.value = true
      error.value = errorMessage(e, '检查更新失败')
      if (!background) visible.value = false
      return null
    } finally {
      checking.value = false
    }
  }

  async function checkOnStartup(): Promise<void> {
    if (checked.value) return
    await check(true)
  }

  async function install(): Promise<void> {
    if (installing.value || !latest.value) return
    const target = latest.value
    const user = useUserStore()
    ensureProgressListener()
    installing.value = true
    paused.value = false
    cancelled.value = false
    failedInstall.value = null
    error.value = ''
    progress.value = 0
    received.value = 0
    total.value = 0
    let client: Awaited<ReturnType<typeof window.ipc.clientMeta>> | null = null
    try {
      client = await window.ipc.clientMeta()
      let delta: ClientUpdateDelta['manifest'] | null = null
      if (target.deltaAvailable) {
        try {
          const response = await ClientUpdateApi.delta(target.id)
          delta = (ApiUtil.data(response) as ClientUpdateDelta).manifest
        } catch {
          // 差分清单不可用时直接走整包下载
        }
      }
      void ClientUpdateApi.report({
        clientId: client.clientId,
        platform: client.platform,
        architecture: client.architecture,
        channel: client.channel,
        fromVersion: client.clientVersion,
        targetVersion: target.version,
        event: 'download_started'
      }).catch(() => {})
      await window.ipc.update.downloadAndInstall({
        url: absoluteApiUrl(target.downloadUrl),
        token: String(user.info.token ?? ''),
        fileName: target.fileName,
        fileSize: target.fileSize,
        sha512: target.sha512,
        version: target.version,
        currentVersion: client.clientVersion,
        platform: client.platform,
        architecture: client.architecture,
        delta
      })
    } catch (e) {
      const control = await window.ipc.update.controlStatus()
      if (control.state === 'paused') {
        paused.value = true
        installing.value = false
        progress.value = control.percent
        received.value = control.received
        total.value = control.total
        return
      }
      if (control.state === 'cancelled') {
        paused.value = false
        cancelled.value = true
        installing.value = false
        error.value = ''
        if (!forceUpgrade.value) visible.value = false
        return
      }
      error.value = errorMessage(e, '下载或安装更新失败')
      const installState = await window.ipc.update.status().catch(() => null)
      if (installState?.state === 'install-failed') failedInstall.value = installState
      installing.value = false
      if (client) {
        void ClientUpdateApi.report({
          clientId: client.clientId,
          platform: client.platform,
          architecture: client.architecture,
          channel: client.channel,
          fromVersion: client.clientVersion,
          targetVersion: target.version,
          event: 'update_failed',
          message: error.value
        }).catch(() => {})
      }
    }
  }

  async function pause(): Promise<void> {
    const control = await window.ipc.update.pause()
    if (control.state !== 'paused') return
    paused.value = true
    progress.value = control.percent
    received.value = control.received
    total.value = control.total
  }

  async function cancel(): Promise<void> {
    const control = await window.ipc.update.cancel()
    if (control.state !== 'cancelled') return
    paused.value = false
    cancelled.value = true
    installing.value = false
    error.value = ''
    if (!forceUpgrade.value) visible.value = false
  }

  async function confirmHealthy(): Promise<void> {
    const before = await window.ipc.update.status()
    if (before.state === 'none') {
      failedInstall.value = null
      return
    }
    const after = await window.ipc.update.reportHealthy()
    const client = await window.ipc.clientMeta()
    if (before.state === 'installed-unconfirmed') {
      failedInstall.value = null
      await ClientUpdateApi.report({
        clientId: client.clientId,
        platform: client.platform,
        architecture: client.architecture,
        channel: client.channel,
        fromVersion: before.fromVersion,
        targetVersion: after.targetVersion,
        event: 'install_success'
      }).catch(() => {})
      return
    }
    if (before.state === 'install-failed') {
      await ClientUpdateApi.report({
        clientId: client.clientId,
        platform: client.platform,
        architecture: client.architecture,
        channel: client.channel,
        fromVersion: after.currentVersion,
        targetVersion: after.targetVersion,
        event: 'install_failed',
        message: `安装后仍启动旧版本 ${after.currentVersion}`
      }).catch(() => {})
    }
  }

  function dismiss(): void {
    if (!forceUpgrade.value) visible.value = false
  }

  /** 忽略当前提示的版本：后台检查不再提示或触发下载，发布更高的新版本后恢复检查 */
  async function ignoreCurrentVersion(): Promise<void> {
    const version = latest.value?.version
    if (!version || !canIgnore.value || ignoring.value) return
    ignoring.value = true
    error.value = ''
    try {
      await Config.updateSetting('ignoredUpdateVersion', version)
      ignoredVersion.value = version
      visible.value = false
    } catch (e) {
      error.value = errorMessage(e, '保存忽略版本失败')
    } finally {
      ignoring.value = false
    }
  }

  return {
    visible,
    checking,
    installing,
    paused,
    cancelled,
    checked,
    ignoring,
    ignoredVersion,
    result,
    error,
    progress,
    received,
    total,
    forceUpgrade,
    rollback,
    hasUpdate,
    latest,
    isLatestIgnored,
    canIgnore,
    failedInstall,
    installFailed,
    preparingUpdate,
    installingProgram,
    message,
    check,
    checkOnStartup,
    install,
    pause,
    cancel,
    confirmHealthy,
    dismiss,
    ignoreCurrentVersion
  }
})

function absoluteApiUrl(path: string): string {
  const value = String(path || '')
  if (/^https?:\/\//i.test(value)) return value
  const base = String(import.meta.env.VITE_APP_API_URL || '').replace(/\/+$/, '')
  return base + (value.startsWith('/') ? value : '/' + value)
}

function sameVersion(left: string, right: string): boolean {
  const normalize = (value: string): string => String(value).trim().replace(/^v/i, '')
  return normalize(left) === normalize(right)
}
