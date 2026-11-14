<script setup lang="ts">
import { computed, onMounted, onUnmounted, provide, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import locale from 'element-plus/dist/locale/zh-cn.mjs'
import { useUserStore } from '@renderer/stores/user'
import { useProjectStore } from '@renderer/stores/project'
import { useConversationStore } from '@renderer/stores/conversation'
import { useChatStore } from '@renderer/stores/chat'
import { useScheduleStore } from '@renderer/stores/schedule'
import { useAgentStore } from '@renderer/stores/agents'
import { useApprovalStore } from '@renderer/stores/approval'
import { useSettingsStore } from '@renderer/stores/settings'
import { useLoadingStore } from '@renderer/stores/loading'
import { useUpdateStore } from '@renderer/stores/update'
import { activeAgentParentConversations, cancelAgentRuns } from '@renderer/core/AgentRuntime'
import UserApi from '@renderer/api/UserApi'
import { page } from '@renderer/router/config'
import AppLoading from '@renderer/components/Layout/AppLoading.vue'
import UpdateDialog from '@renderer/components/Update/UpdateDialog.vue'
import { CLIENT_UPGRADE_WINDOW_EVENT } from '../../shared/client'
import type { DesktopAgentUnavailableNotice } from '../../shared/desktop'

const user = useUserStore()
const router = useRouter()
const appLoading = useLoadingStore()
const update = useUpdateStore()
const settings = useSettingsStore()
let updateTimer: number | null = null
let stopPrepareQuit: (() => void) | null = null
let stopClientUpgradeRequired: (() => void) | null = null
let stopDesktopUnavailable: (() => void) | null = null
const desktopUnavailableDialog = ref(false)
const desktopUnavailableNotice = ref<DesktopAgentUnavailableNotice | null>(null)
const contentBlocked = computed(() => appLoading.visible || (update.forceUpgrade && update.visible))
const standaloneDetail =
  window.location.hash.startsWith('#/agent-detail') ||
  window.location.hash.startsWith('#/message-preview')

/** 已经准备过的身份（用户 + 令牌）：同一个身份只准备一次 */
let bootstrappedFor = ''

async function logout(): Promise<void> {
  await UserApi.logout().finally(async () => {
    // 退出前断开所有流并停掉调度，避免携带上一个账号的会话继续跑
    await window.ipc.agent.closeAll()
    await window.ipc.messageWindow.closeAll()
    await useChatStore().reset()
    useScheduleStore().stop()
    useConversationStore().reset()
    useAgentStore().reset()
    user.reset()
    bootstrappedFor = ''
    router.push(page.login)
  })
}

/**
 * 登录后一次性准备：项目、会话列表与定时调度。
 *
 * 只在用户库就绪（user.ready）之后发起：主进程打开用户库是异步的，
 * 早于它就绪的读库调用会直接失败，页面也就拿不到任何数据。
 */
async function bootstrap(): Promise<void> {
  if (!user.ready || !user.isLogined()) return
  const identity = `${user.info.id}:${user.info.token}`
  if (bootstrappedFor === identity) return
  bootstrappedFor = identity
  try {
    const projectStore = useProjectStore()
    const scheduleStore = useScheduleStore()
    await useAgentStore().initialize()
    await projectStore.fetchProjects()
    await useConversationStore().fetchConversations()
    await scheduleStore.fetchTasks()
    await scheduleStore.start()
  } catch (error) {
    // 失败不记为已完成：状态再变化时还能重试
    bootstrappedFor = ''
    console.error('[App] Failed to bootstrap:', error)
  }
}

onMounted(() => {
  if (standaloneDetail) return
  stopPrepareQuit = window.ipc.onPrepareQuit(() => {
    void (async () => {
      try {
        await useChatStore().flushBeforeExit()
      } catch (error) {
        console.error('[App] Failed to flush chat checkpoints before quit:', error)
      } finally {
        await window.ipc.quitReady().catch((error) => {
          console.error('[App] Failed to acknowledge quit:', error)
        })
      }
    })()
  })
  // 登录态与用户库由路由守卫准备好，这里只做登录后的数据准备
  // 工作区外的访问申请：队列挂在 approval store 上，横幅由对话页或布局展示
  useApprovalStore().listen()
  stopDesktopUnavailable = window.ipc.automation.onDesktopUnavailable(desktopUnavailable)
  void bootstrap()
  // 延迟确认新版本界面已经完成启动；若进程或渲染器随后崩溃，待更新标记会保留
  window.setTimeout(() => {
    void update.confirmHealthy()
  }, 3000)
  stopClientUpgradeRequired = window.ipc.onClientUpgradeRequired(upgradeRequired)
  window.addEventListener(CLIENT_UPGRADE_WINDOW_EVENT, upgradeRequired)
  window.addEventListener('online', upgradeRequired)
  updateTimer = window.setInterval(
    () => {
      void update.check(true)
    },
    4 * 60 * 60 * 1000
  )
})

function upgradeRequired(): void {
  void update.check(true)
}

function desktopUnavailable(notice: DesktopAgentUnavailableNotice): void {
  // 先同步中止对应主对话与全部子 Agent，工具失败后模型也不能继续推进。
  const conversationId = String(notice?.conversationId ?? '')
  cancelAgentRuns(conversationId, '', true)
  useChatStore().stop(conversationId)
  if (notice?.permissions.length) {
    settings.desktopAutomationReason = String(notice.reason ?? '')
    settings.desktopPermissionsChecked = true
    settings.desktopPermissionsReady = false
    settings.desktopPermissionsRestartRequired = notice.restartRequired === true
    settings.desktopPermissions = notice.permissions
  }
  if (desktopUnavailableDialog.value) return
  const reason = String(notice?.reason ?? '')
    .replace(/[。；;，,\s]*Desktop Agent 对话已自动终止[。；;，,\s]*$/i, '')
    .trim()
  desktopUnavailableNotice.value = {
    ...notice,
    reason: reason || '桌面操作当前不可用。'
  }
  desktopUnavailableDialog.value = true
}

function openDesktopSettings(): void {
  desktopUnavailableDialog.value = false
  void router.push(page.settings)
}

function desktopUnavailableClosed(): void {
  desktopUnavailableNotice.value = null
  void window.ipc.automation.releaseDesktopUnavailable().catch(() => undefined)
}

onUnmounted(() => {
  stopPrepareQuit?.()
  stopPrepareQuit = null
  stopClientUpgradeRequired?.()
  stopClientUpgradeRequired = null
  stopDesktopUnavailable?.()
  stopDesktopUnavailable = null
  if (desktopUnavailableDialog.value) {
    void window.ipc.automation.releaseDesktopUnavailable().catch(() => undefined)
  }
  window.removeEventListener(CLIENT_UPGRADE_WINDOW_EVENT, upgradeRequired)
  window.removeEventListener('online', upgradeRequired)
  if (updateTimer !== null) window.clearInterval(updateTimer)
})

// 登录 / 还原完成（ready 翻到 true）再准备一次；同一身份不会重复准备
watch(
  () => user.ready,
  () => {
    if (!standaloneDetail) void bootstrap()
  }
)

// 总开关关闭时，不等待下一个 desktop_op：直接终止所有正在运行的 Desktop Agent 主对话。
watch(
  () => settings.desktopAutomationEnabled,
  (enabled, previous) => {
    if (enabled || previous !== true) return
    for (const conversationId of activeAgentParentConversations('desktop')) {
      desktopUnavailable({
        conversationId,
        sourceSessionId: '',
        reason: '桌面操作已关闭，Desktop Agent 对话已自动终止。请到系统设置中重新开启后再试。',
        permissions: [],
        restartRequired: false
      })
    }
  }
)

provide('logout', logout)
</script>

<template>
  <el-config-provider :locale="locale">
    <div
      class="app-content"
      :inert="contentBlocked"
      :aria-hidden="contentBlocked ? 'true' : undefined"
    >
      <router-view />
    </div>
    <AppLoading />
    <UpdateDialog />
    <el-dialog
      v-model="desktopUnavailableDialog"
      class="desktop-stop-dialog"
      width="min(560px, calc(100vw - 40px))"
      :close-on-click-modal="false"
      align-center
      @closed="desktopUnavailableClosed"
    >
      <template #header>
        <div class="desktop-stop-header">
          <span class="desktop-stop-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24">
              <rect
                x="3.5"
                y="4.5"
                width="17"
                height="12"
                rx="2"
                fill="none"
                stroke="currentColor"
                stroke-width="1.6"
              />
              <path
                d="M8.5 20h7M12 16.5V20"
                fill="none"
                stroke="currentColor"
                stroke-width="1.6"
                stroke-linecap="round"
              />
              <path
                d="m9.3 8.1 5.4 5.4M14.7 8.1l-5.4 5.4"
                fill="none"
                stroke="currentColor"
                stroke-width="1.7"
                stroke-linecap="round"
              />
            </svg>
          </span>
          <div>
            <h3>Desktop Agent 已终止</h3>
            <p>为保护当前任务，桌面操作没有继续执行。</p>
          </div>
        </div>
      </template>

      <div class="desktop-stop-state">
        <span class="desktop-stop-state__dot" />
        <div>
          <strong>对话已自动终止</strong>
          <p>{{ desktopUnavailableNotice?.reason }}</p>
        </div>
      </div>

      <div v-if="desktopUnavailableNotice?.permissions.length" class="desktop-stop-permissions">
        <div class="desktop-stop-section-title">权限状态</div>
        <div
          v-for="permission in desktopUnavailableNotice.permissions"
          :key="permission.key"
          class="desktop-stop-permission"
        >
          <span class="desktop-stop-permission__mark" :class="{ 'is-granted': permission.granted }">
            {{ permission.granted ? '✓' : '!' }}
          </span>
          <span class="desktop-stop-permission__name">{{ permission.name }}</span>
          <el-tag
            size="small"
            :type="permission.granted ? 'success' : 'warning'"
            effect="plain"
            round
          >
            {{ permission.granted ? '已授权' : '未就绪' }}
          </el-tag>
        </div>
      </div>

      <div class="desktop-stop-tip">
        <svg viewBox="0 0 16 16" aria-hidden="true">
          <circle cx="8" cy="8" r="6.2" fill="none" stroke="currentColor" stroke-width="1.2" />
          <path
            d="M8 7.1v4M8 4.8v.2"
            stroke="currentColor"
            stroke-width="1.4"
            stroke-linecap="round"
          />
        </svg>
        <span>
          {{
            desktopUnavailableNotice?.restartRequired
              ? '屏幕录制权限需要重启 fs-bot 后才会对当前进程生效。'
              : '应用和本次对话记录都已保留，不会退出客户端。'
          }}
        </span>
      </div>

      <template #footer>
        <div class="desktop-stop-footer">
          <el-button @click="desktopUnavailableDialog = false">稍后处理</el-button>
          <el-button type="primary" @click="openDesktopSettings">前往系统设置</el-button>
        </div>
      </template>
    </el-dialog>
  </el-config-provider>
</template>

<style lang="scss" scoped>
.app-content {
  width: 100%;
  height: 100%;
}

:global(.desktop-stop-dialog.el-dialog) {
  overflow: hidden;
  border: 1px solid var(--ui-card-border);
  border-radius: 18px;
  box-shadow: 0 24px 80px rgba(15, 23, 42, 0.22);
}

:global(.desktop-stop-dialog .el-dialog__header) {
  padding: 22px 24px 0;
}

:global(.desktop-stop-dialog .el-dialog__body) {
  padding: 18px 24px 8px;
}

:global(.desktop-stop-dialog .el-dialog__footer) {
  padding: 12px 24px 20px;
}

.desktop-stop-header {
  display: flex;
  align-items: center;
  gap: 14px;
  text-align: left;

  h3 {
    margin: 0;
    font-size: 18px;
    font-weight: 650;
    line-height: 1.3;
    color: var(--ev-c-text-1);
  }

  p {
    margin: 4px 0 0;
    font-size: 13px;
    line-height: 1.5;
    color: var(--ev-c-text-3);
  }
}

.desktop-stop-icon {
  display: grid;
  flex: none;
  width: 44px;
  height: 44px;
  place-items: center;
  color: var(--el-color-danger);
  background: var(--el-color-danger-light-9);
  border: 1px solid var(--el-color-danger-light-7);
  border-radius: 13px;

  svg {
    width: 25px;
    height: 25px;
  }
}

.desktop-stop-state {
  display: flex;
  align-items: flex-start;
  gap: 11px;
  padding: 13px 14px;
  background: var(--el-color-warning-light-9);
  border: 1px solid var(--el-color-warning-light-7);
  border-radius: 11px;

  div {
    display: flex;
    flex-direction: column;
    min-width: 0;
  }

  strong {
    font-size: 13px;
    font-weight: 600;
    color: var(--ev-c-text-1);
  }

  p {
    margin: 3px 0 0;
    font-size: 12px;
    line-height: 1.65;
    color: var(--ev-c-text-2);
  }
}

.desktop-stop-state__dot {
  flex: none;
  width: 8px;
  height: 8px;
  margin-top: 5px;
  background: var(--el-color-warning);
  border-radius: 50%;
  box-shadow: 0 0 0 4px var(--el-color-warning-light-8);
}

.desktop-stop-permissions {
  margin-top: 14px;
  padding: 12px 14px;
  background: var(--ui-surface);
  border: 1px solid var(--ui-card-border);
  border-radius: 11px;
}

.desktop-stop-section-title {
  margin-bottom: 7px;
  font-size: 12px;
  font-weight: 600;
  color: var(--ev-c-text-3);
}

.desktop-stop-permission {
  display: flex;
  align-items: center;
  gap: 9px;
  min-height: 32px;
}

.desktop-stop-permission__mark {
  display: grid;
  width: 20px;
  height: 20px;
  place-items: center;
  font-size: 12px;
  font-weight: 700;
  color: var(--el-color-warning);
  background: var(--el-color-warning-light-9);
  border-radius: 50%;

  &.is-granted {
    color: var(--el-color-success);
    background: var(--el-color-success-light-9);
  }
}

.desktop-stop-permission__name {
  flex: 1;
  font-size: 13px;
  color: var(--ev-c-text-1);
}

.desktop-stop-tip {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: 14px;
  font-size: 12px;
  line-height: 1.65;
  color: var(--ev-c-text-3);

  svg {
    flex: none;
    width: 16px;
    height: 16px;
    margin-top: 1px;
    color: var(--el-color-warning);
  }
}

.desktop-stop-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

@media (max-width: 520px) {
  .desktop-stop-header {
    align-items: flex-start;
  }

  .desktop-stop-footer {
    :deep(.el-button) {
      flex: 1;
      margin-left: 0;
    }
  }
}
</style>
