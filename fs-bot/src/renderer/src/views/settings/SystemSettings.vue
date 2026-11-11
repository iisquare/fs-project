<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useSettingsStore } from '@renderer/stores/settings'
import { useSkillStore } from '@renderer/stores/skill'
import { useUpdateStore } from '@renderer/stores/update'
import { errorMessage } from '@renderer/utils/ErrorUtil'
import { desktopEmergencyShortcutLabel } from '../../../../shared/desktop'

const settings = useSettingsStore()
const skillStore = useSkillStore()
const updateStore = useUpdateStore()
const isMac = window.electron.process.platform === 'darwin'
const emergencyStopShortcut = desktopEmergencyShortcutLabel(window.electron.process.platform)

const apiUrl = ref(String(import.meta.env.VITE_APP_API_URL || ''))
const websiteUrl = ref(String(import.meta.env.VITE_APP_WEBSITE_URL || ''))
const clientVersion = ref('')
const dataVersion = ref<number | null>(null)
const pageLoading = ref(false)
const pageError = ref('')
const desktopPermissionDialog = ref(false)

/** 工作区外访问策略：请求批准 / 帮我批准 / 自动拒绝 / 完全访问权限 */
const policies = [
  {
    value: 'ask',
    label: '请求批准',
    description:
      '模型要读写工作区外的文件时逐次确认；执行命令每次都要确认（命令能改动的范围不限于工作目录），' +
      '拒绝后这一步失败但不中断本轮对话'
  },
  {
    value: 'auto',
    label: '帮我批准',
    description:
      '工作区外的读写与命令执行直接放行、不再询问（命令因此可以改到本机任何地方），适合完全信任当前项目时使用'
  },
  {
    value: 'deny',
    label: '自动拒绝',
    description:
      '工作区外一律拒绝，模型只能在本项目声明的目录内读写文件；命令一条也不执行（命令拦不住落点，索性不放行）'
  },
  {
    value: 'full',
    label: '完全访问权限',
    description: '工作区外与工作区内同等对待，完全放行'
  }
]

/** 桌面任务授权策略：默认每次任务先询问，也可明确设为自动允许 */
const desktopPolicies = [
  {
    value: 'ask',
    label: '请求授权',
    description:
      '每个桌面任务第一次操作前，在对话输入框上方请求批准；允许后当前任务不会为每个点击、输入或截图重复询问'
  },
  {
    value: 'auto',
    label: '自动允许',
    description: `桌面操作开启时直接放行当前任务，不显示任务授权横幅；桌面截图会发送给所选模型，鼠标键盘可直接操作本机。授权仍按任务收回，并保留停止按钮与 ${emergencyStopShortcut} 紧急停止`
  }
]

async function load(): Promise<void> {
  pageLoading.value = true
  pageError.value = ''
  try {
    const results = await Promise.allSettled([
      settings.fetchSettings(),
      skillStore.fetchInstalled()
    ])
    const failure = results.find(
      (item): item is PromiseRejectedResult => 'rejected' === item.status
    )
    if (failure) pageError.value = errorMessage(failure.reason, '系统设置加载失败')
    const versions = await window.ipc.appVersions()
    clientVersion.value = versions.clientVersion
    dataVersion.value = versions.dataVersion
  } catch (error) {
    if (!pageError.value) pageError.value = errorMessage(error, '系统设置加载失败')
  } finally {
    pageLoading.value = false
  }
}

onMounted(() => {
  void load()
})

async function changeAutoStart(value: unknown): Promise<void> {
  try {
    await settings.toggleAutoStart(value === true)
  } catch (error) {
    ElMessage.error(errorMessage(error, '开机启动设置失败'))
  }
}

async function changeDesktopAutomation(value: unknown): Promise<void> {
  try {
    const enabled = value === true
    const status = await settings.setDesktopAutomationEnabled(enabled)
    if (!enabled) {
      desktopPermissionDialog.value = false
      ElMessage.success('桌面操作已关闭')
      return
    }
    if (status.permissionsChecked && !status.permissionsReady) {
      desktopPermissionDialog.value = true
      ElMessage.warning('桌面操作已开启，但系统权限尚未就绪')
      return
    }
    ElMessage.success(
      'auto' === settings.desktopApprovalPolicy
        ? '桌面操作已启用；桌面任务将自动允许'
        : '桌面操作已启用；桌面任务首次操作仍会请求授权'
    )
  } catch (error) {
    ElMessage.error(errorMessage(error, '桌面操作设置失败'))
  }
}

async function openDesktopPermissionSettings(
  key: 'accessibility' | 'screenRecording'
): Promise<void> {
  try {
    await settings.openDesktopPermissionSettings(key)
  } catch (error) {
    ElMessage.error(errorMessage(error, '无法打开系统权限设置'))
  }
}

function permissionStateLabel(state: string, granted: boolean): string {
  if (granted) return '已授权'
  if ('not-determined' === state) return '待授权'
  if ('restricted' === state) return '受系统限制'
  if ('denied' === state) return '已拒绝'
  return '未授权'
}

async function changeDesktopApprovalPolicy(value: unknown): Promise<void> {
  try {
    await settings.setDesktopApprovalPolicy('auto' === value ? 'auto' : 'ask')
    ElMessage.success(
      'auto' === value ? '桌面任务将自动允许，不再弹出授权横幅' : '桌面任务每次将请求授权'
    )
  } catch (error) {
    ElMessage.error(errorMessage(error, '桌面授权策略设置失败'))
  }
}

async function checkUpdate(): Promise<void> {
  const result = await updateStore.check(false)
  if (result && !result.latest) {
    ElMessage.info('服务端暂未发布当前系统的安装包')
  } else if (result && !result.hasUpdate && !result.forceUpgrade) {
    ElMessage.success(`当前已是最新版本 v${result.currentVersion}`)
  }
}
</script>

<template>
  <div v-loading="pageLoading" class="system-settings ui-page">
    <div class="ui-page-header">
      <div class="ui-page-header__copy">
        <h1 class="ui-page-title">系统设置</h1>
        <p class="ui-page-desc">管理客户端启动、升级、服务端连接与本地访问权限。</p>
      </div>
    </div>

    <el-alert
      v-if="pageError"
      class="page-alert"
      type="warning"
      :closable="false"
      show-icon
      :title="pageError"
    >
      <el-button text type="primary" size="small" @click="load">重试</el-button>
    </el-alert>

    <h2 class="section-title">启动与更新</h2>
    <div class="settings-card">
      <div class="setting-item">
        <div class="setting-label">
          <span class="setting-title">开机启动</span>
          <span class="setting-desc">
            {{ settings.autoStartReason || '系统启动时自动运行安装后的 fs-bot' }}
          </span>
        </div>
        <el-switch
          :model-value="settings.autoStart"
          :disabled="!settings.autoStartSupported || settings.autoStartSaving"
          :loading="settings.autoStartSaving"
          @update:model-value="changeAutoStart"
        />
      </div>
      <el-divider />
      <div class="setting-item">
        <div class="setting-label">
          <span class="setting-title">检测更新</span>
          <span class="setting-desc">启动及定时检查新版本并主动提示，下载前仍需确认</span>
        </div>
        <el-switch
          :model-value="settings.autoUpgrade"
          @update:model-value="(val: any) => settings.toggleAutoUpgrade(val)"
        />
      </div>
      <el-divider />
      <div class="setting-item">
        <div class="setting-label">
          <span class="setting-title">更新渠道</span>
          <span class="setting-desc">服务端只返回所选渠道的已启用版本</span>
        </div>
        <el-radio-group
          :model-value="settings.updateChannel"
          @update:model-value="(value: any) => settings.setUpdateChannel(String(value))"
        >
          <el-radio-button value="stable">稳定版</el-radio-button>
          <el-radio-button value="beta">测试版</el-radio-button>
        </el-radio-group>
      </div>
      <el-divider />
      <div class="setting-item">
        <div class="setting-label">
          <span class="setting-title">检查更新</span>
          <span class="setting-desc">{{ updateStore.message }}</span>
        </div>
        <el-button type="primary" plain :loading="updateStore.checking" @click="checkUpdate">
          检查更新
        </el-button>
      </div>
    </div>

    <h2 class="section-title">服务端连接</h2>
    <div class="settings-card">
      <div class="setting-item">
        <div class="setting-label">
          <span class="setting-title">接口地址</span>
          <span class="setting-desc">智能体模块（应用 / 会话 / 技能 / 工具）的接口前缀</span>
        </div>
        <span class="setting-value">{{ apiUrl }}</span>
      </div>
      <el-divider />
      <div class="setting-item">
        <div class="setting-label">
          <span class="setting-title">控制台地址</span>
          <span class="setting-desc">注册、找回密码与后台管理入口</span>
        </div>
        <span class="setting-value">{{ websiteUrl }}</span>
      </div>
    </div>

    <h2 class="section-title">本地目录与权限</h2>
    <div class="settings-card">
      <div class="setting-item">
        <div class="setting-label">
          <span class="setting-title">技能目录</span>
          <span class="setting-desc"
            >服务端下发的技能包解压到这里，已安装 {{ skillStore.installed.length }} 个技能</span
          >
        </div>
        <span class="setting-value">{{ skillStore.root || '未初始化' }}</span>
      </div>
      <el-divider />
      <div class="setting-item">
        <div class="setting-label">
          <span class="setting-title">工作区权限</span>
          <span class="setting-desc"
            >按项目配置目录的读 / 写权限，文件访问全部由主进程按清单放行</span
          >
        </div>
        <el-button text type="primary" @click="$router.push('/project')">前往项目管理</el-button>
      </div>
    </div>

    <h2 class="section-title">本地访问策略</h2>
    <div class="settings-card">
      <div class="setting-item">
        <div class="setting-label">
          <span class="setting-title">桌面操作</span>
          <span class="setting-desc">
            {{
              settings.desktopAutomationReason ||
              (settings.desktopApprovalPolicy === 'auto'
                ? '允许CUA（Computer-Use Agent）桌面子 Agent 查看当前窗口并发起鼠标、键盘操作；当前策略为自动允许'
                : '允许CUA（Computer-Use Agent）桌面子 Agent 查看当前窗口并发起鼠标、键盘操作；每次任务仍需授权')
            }}
          </span>
        </div>
        <el-switch
          :model-value="settings.desktopAutomationEnabled"
          :disabled="!settings.desktopAutomationSupported || settings.desktopAutomationSaving"
          :loading="settings.desktopAutomationSaving"
          @update:model-value="changeDesktopAutomation"
        />
      </div>
      <div
        v-if="
          isMac &&
          settings.desktopAutomationSupported &&
          settings.desktopPermissions.length &&
          settings.desktopPermissionsChecked &&
          settings.desktopPermissionsReady
        "
        class="permission-ready"
      >
        <span class="permission-ready__dot" />
        系统权限已就绪：辅助功能、屏幕录制均已授权
      </div>
      <el-alert
        v-if="
          isMac &&
          settings.desktopAutomationSupported &&
          settings.desktopPermissions.length &&
          settings.desktopPermissionsChecked &&
          !settings.desktopPermissionsReady
        "
        class="desktop-permission-alert"
        type="warning"
        :closable="false"
        show-icon
        :title="
          settings.desktopPermissionsRestartRequired
            ? '屏幕录制权限需要重启 fs-bot 后生效'
            : '桌面操作已开启，但系统权限尚未就绪'
        "
      >
        <div class="permission-list">
          <div v-for="item in settings.desktopPermissions" :key="item.key" class="permission-row">
            <div class="permission-copy">
              <span class="permission-name">{{ item.name }}</span>
              <span class="permission-desc">{{ item.description }}</span>
            </div>
            <el-tag size="small" :type="item.granted ? 'success' : 'warning'" effect="plain">
              {{ permissionStateLabel(item.state, item.granted) }}
            </el-tag>
            <el-button
              v-if="!item.granted"
              text
              type="primary"
              size="small"
              @click="openDesktopPermissionSettings(item.key)"
            >
              去配置
            </el-button>
          </div>
        </div>
        <div class="permission-actions">
          <span v-if="settings.desktopPermissionsRestartRequired">
            授予屏幕录制权限后，请手动完全退出并重新打开
            fs-bot，再开启桌面操作。应用不会自动退出或重启。
          </span>
          <span v-else> 完成辅助功能配置后，请关闭并重新开启「桌面操作」开关以再次检测。 </span>
        </div>
      </el-alert>
    </div>

    <h3 class="subsection-title">桌面授权策略</h3>
    <div class="settings-card">
      <div class="setting-desc policy-desc">
        仅在「桌面操作」开启且系统权限就绪时生效。自动允许只控制任务首次操作是否停下来询问，
        不会改变项目中的 Desktop Agent
        开关，也不会让模型取得文件或命令工具。开启前请确认桌面画面可以发送给所选模型。
      </div>
      <el-radio-group
        class="policy-group"
        :model-value="settings.desktopApprovalPolicy"
        :disabled="
          !settings.desktopAutomationEnabled ||
          (isMac && settings.desktopPermissionsChecked && !settings.desktopPermissionsReady) ||
          settings.desktopApprovalPolicySaving
        "
        @update:model-value="changeDesktopApprovalPolicy"
      >
        <el-radio
          v-for="item in desktopPolicies"
          :key="item.value"
          class="policy-item"
          :value="item.value"
        >
          <span class="policy-label">{{ item.label }}</span>
          <span class="policy-text">{{ item.description }}</span>
        </el-radio>
      </el-radio-group>
    </div>

    <h3 class="subsection-title">工具调用策略</h3>
    <div class="settings-card">
      <div class="setting-desc policy-desc">
        模型调用的本地工具按这里的策略放行：读写文件越过项目声明的目录时过来判一次；
        执行命令每次都要判——命令能落到磁盘的任何地方，工作目录只是它的起点，
        不能因为「在工作区内」就放行。判定与执行都在主进程，界面只负责在你选择「请求批准」时把申请弹出来。
      </div>
      <el-radio-group
        class="policy-group"
        :model-value="settings.workspacePolicy"
        @update:model-value="(value: any) => settings.setWorkspacePolicy(String(value))"
      >
        <el-radio
          v-for="item in policies"
          :key="item.value"
          class="policy-item"
          :value="item.value"
        >
          <span class="policy-label">{{ item.label }}</span>
          <span class="policy-text">{{ item.description }}</span>
        </el-radio>
      </el-radio-group>
    </div>

    <el-dialog
      v-if="isMac"
      v-model="desktopPermissionDialog"
      class="desktop-permission-dialog"
      width="min(600px, calc(100vw - 40px))"
      :close-on-click-modal="false"
      align-center
    >
      <template #header>
        <div class="permission-dialog-header">
          <span class="permission-dialog-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24">
              <path
                d="M12 3 19 6v5.3c0 4.5-2.8 8.1-7 9.7-4.2-1.6-7-5.2-7-9.7V6l7-3Z"
                fill="none"
                stroke="currentColor"
                stroke-width="1.6"
                stroke-linejoin="round"
              />
              <path
                d="m9 12 2 2 4-4"
                fill="none"
                stroke="currentColor"
                stroke-width="1.7"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </span>
          <div>
            <h3>配置桌面操作权限</h3>
            <p>桌面操作已经开启，还需要完成以下 macOS 系统授权。</p>
          </div>
        </div>
      </template>

      <div
        class="permission-summary"
        :class="{ 'is-restart': settings.desktopPermissionsRestartRequired }"
      >
        <span class="permission-summary__dot" />
        <div>
          <strong>
            {{
              settings.desktopPermissionsRestartRequired
                ? '需要重启 fs-bot'
                : `${settings.desktopPermissions.filter((item) => !item.granted).length} 项权限待处理`
            }}
          </strong>
          <span>
            {{
              settings.desktopPermissionsRestartRequired
                ? '屏幕录制权限只在应用重新启动后生效'
                : '应用不会主动修改权限，也不会自动退出'
            }}
          </span>
        </div>
      </div>

      <div class="permission-list permission-list--dialog">
        <div v-for="item in settings.desktopPermissions" :key="item.key" class="permission-row">
          <span class="permission-status-mark" :class="{ 'is-granted': item.granted }">
            <svg v-if="item.granted" viewBox="0 0 16 16" aria-hidden="true">
              <path
                d="m3.8 8.3 2.7 2.6 5.7-5.5"
                fill="none"
                stroke="currentColor"
                stroke-width="1.7"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
            <span v-else />
          </span>
          <div class="permission-copy">
            <span class="permission-name">{{ item.name }}</span>
            <span class="permission-desc">{{ item.description }}</span>
          </div>
          <el-tag size="small" :type="item.granted ? 'success' : 'warning'" effect="plain" round>
            {{ permissionStateLabel(item.state, item.granted) }}
          </el-tag>
          <el-button
            v-if="!item.granted"
            type="primary"
            size="small"
            plain
            @click="openDesktopPermissionSettings(item.key)"
          >
            去授权
          </el-button>
        </div>
      </div>

      <div class="permission-dialog-note">
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
            settings.desktopPermissionsRestartRequired
              ? '完成屏幕录制授权后，请手动完全退出并重新打开 fs-bot，再尝试调用桌面 Agent。'
              : '完成配置后，关闭并重新开启桌面操作开关可再次检测。'
          }}
        </span>
      </div>

      <template #footer>
        <div class="permission-dialog-footer">
          <span>应用会保持运行，不会自动退出或重启。</span>
          <el-button type="primary" @click="desktopPermissionDialog = false"> 稍后配置 </el-button>
        </div>
      </template>
    </el-dialog>

    <h2 class="section-title">版本信息</h2>
    <div class="settings-card">
      <div class="setting-item">
        <div class="setting-label">
          <span class="setting-title">客户端版本</span>
          <span class="setting-desc">当前安装的 fs-bot 应用版本</span>
        </div>
        <span class="setting-value">{{ clientVersion ? `v${clientVersion}` : '读取中...' }}</span>
      </div>
      <el-divider />
      <div class="setting-item">
        <div class="setting-label">
          <span class="setting-title">用户数据版本</span>
          <span class="setting-desc">当前账号本地数据库的 schema 版本</span>
        </div>
        <span class="setting-value">{{ dataVersion === null ? '未连接' : `v${dataVersion}` }}</span>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.page-alert {
  margin-bottom: 16px;
}

.settings-card {
  padding: 14px 22px;
  background: var(--ui-surface);
  border: 1px solid var(--ui-card-border);
  border-radius: var(--ui-card-radius);
  box-shadow: var(--ui-card-shadow);

  :deep(.el-divider--horizontal) {
    margin: 0;
    border-top-color: var(--ui-divider);
  }
}

.section-title {
  margin: 28px 0 12px 2px;
  font-size: 14px;
  font-weight: 600;
  color: var(--ev-c-text-1);

  &:first-of-type {
    margin-top: 0;
  }
}

.subsection-title {
  margin: 24px 0 10px 2px;
  font-size: 13px;
  font-weight: 600;
  color: var(--ev-c-text-2);
}

.setting-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 13px 10px;
  margin: 0 -10px;
  gap: 16px;
  border-radius: 9px;
  transition: background-color 0.2s;

  &:hover {
    background: color-mix(in srgb, var(--ui-surface-hover) 76%, transparent);
  }
}

.setting-label {
  display: flex;
  flex-direction: column;

  .setting-title {
    font-size: 14px;
    font-weight: 500;
    color: var(--ev-c-text-1);
  }

  .setting-desc {
    font-size: 12px;
    line-height: 1.6;
    color: var(--ev-c-text-3);
    margin-top: 4px;
  }
}

.setting-value {
  max-width: 50%;
  font-size: 13px;
  color: var(--ev-c-text-2);
  word-break: break-all;
  text-align: right;
}

.policy-desc {
  margin: 14px 0 14px;
  font-size: 13px;
  line-height: 1.65;
  color: var(--ev-c-text-3);
}

.permission-ready {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 0 10px 12px;
  font-size: 12px;
  color: var(--el-color-success);
}

.permission-ready__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.desktop-permission-alert {
  margin: 2px 10px 14px;
}

.permission-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 10px;
}

.permission-row {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 34px;
}

.permission-copy {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}

.permission-name {
  font-size: 13px;
  color: var(--ev-c-text-1);
}

.permission-desc {
  margin-top: 1px;
  font-size: 12px;
  color: var(--ev-c-text-3);
}

.permission-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid var(--ui-divider);
  font-size: 12px;
  line-height: 1.6;
  color: var(--ev-c-text-3);
}

:global(.desktop-permission-dialog.el-dialog) {
  overflow: hidden;
  border: 1px solid var(--ui-card-border);
  border-radius: 18px;
  box-shadow: 0 24px 80px rgba(15, 23, 42, 0.2);
}

:global(.desktop-permission-dialog .el-dialog__header) {
  padding: 22px 24px 0;
}

:global(.desktop-permission-dialog .el-dialog__body) {
  padding: 18px 24px 8px;
}

:global(.desktop-permission-dialog .el-dialog__footer) {
  padding: 14px 24px 20px;
}

.permission-dialog-header {
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

.permission-dialog-icon {
  display: grid;
  flex: none;
  width: 42px;
  height: 42px;
  place-items: center;
  color: var(--el-color-warning-dark-2);
  background: var(--el-color-warning-light-9);
  border: 1px solid var(--el-color-warning-light-7);
  border-radius: 12px;

  svg {
    width: 24px;
    height: 24px;
  }
}

.permission-summary {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 14px;
  padding: 12px 14px;
  background: var(--el-fill-color-lighter);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 11px;

  &.is-restart {
    background: var(--el-color-warning-light-9);
    border-color: var(--el-color-warning-light-7);
  }

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

  span {
    margin-top: 2px;
    font-size: 12px;
    line-height: 1.5;
    color: var(--ev-c-text-3);
  }
}

.permission-summary__dot {
  flex: none;
  width: 8px;
  height: 8px;
  margin-top: 5px;
  background: var(--el-color-warning);
  border-radius: 50%;
  box-shadow: 0 0 0 4px var(--el-color-warning-light-8);
}

.permission-list--dialog {
  gap: 10px;

  .permission-row {
    min-height: 62px;
    padding: 10px 12px;
    background: var(--ui-surface);
    border: 1px solid var(--ui-card-border);
    border-radius: 11px;
  }

  .permission-name {
    font-size: 14px;
    font-weight: 550;
  }
}

.permission-status-mark {
  display: grid;
  flex: none;
  width: 28px;
  height: 28px;
  place-items: center;
  color: var(--ev-c-text-3);
  background: var(--el-fill-color-light);
  border-radius: 50%;

  &.is-granted {
    color: var(--el-color-success);
    background: var(--el-color-success-light-9);
  }

  svg {
    width: 16px;
    height: 16px;
  }

  span {
    width: 7px;
    height: 7px;
    background: currentColor;
    border-radius: 50%;
  }
}

.permission-dialog-note {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: 14px;
  padding: 11px 13px;
  font-size: 12px;
  line-height: 1.65;
  color: var(--ev-c-text-2);
  background: var(--el-fill-color-lighter);
  border-radius: 9px;

  svg {
    flex: none;
    width: 16px;
    height: 16px;
    margin-top: 1px;
    color: var(--el-color-warning);
  }
}

.permission-dialog-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;

  span {
    font-size: 12px;
    line-height: 1.5;
    color: var(--ev-c-text-3);
    text-align: left;
  }
}

@media (max-width: 520px) {
  .permission-dialog-header {
    align-items: flex-start;
  }

  .permission-list--dialog .permission-row {
    flex-wrap: wrap;
  }

  .permission-list--dialog .permission-copy {
    flex-basis: calc(100% - 42px);
  }

  .permission-dialog-footer {
    align-items: stretch;
    flex-direction: column;

    :deep(.el-button) {
      width: 100%;
      margin-left: 0;
    }
  }
}

.policy-group {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 2px;
}

.policy-item {
  width: 100%;
  height: auto;
  margin-right: 0;
  padding: 10px 12px;
  align-items: flex-start;
  border: 1px solid transparent;
  border-radius: 10px;
  transition:
    background 0.2s,
    border-color 0.2s;

  &:hover {
    background: var(--el-fill-color-lighter);
  }

  &.is-checked {
    background: color-mix(in srgb, var(--el-color-primary) 4%, var(--el-bg-color));
    border-color: color-mix(in srgb, var(--el-color-primary) 14%, var(--el-border-color-lighter));
  }

  :deep(.el-radio__label) {
    display: flex;
    flex-direction: column;
    gap: 2px;
    white-space: normal;
  }

  /* 单选圆点与标题首行共用同一条垂直中线，说明文字继续向下排列。 */
  :deep(.el-radio__input) {
    align-self: flex-start;
    margin-top: 3px;
  }

  .policy-label {
    font-size: 14px;
    line-height: 20px;
    color: var(--ev-c-text-1);
  }

  .policy-text {
    font-size: 12px;
    line-height: 1.7;
    color: var(--ev-c-text-3);
  }
}
</style>
