<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useSettingsStore } from '@renderer/stores/settings'
import { useSkillStore } from '@renderer/stores/skill'
import { useUpdateStore } from '@renderer/stores/update'
import { errorMessage } from '@renderer/utils/ErrorUtil'

const settings = useSettingsStore()
const skillStore = useSkillStore()
const updateStore = useUpdateStore()

const apiUrl = ref(String(import.meta.env.VITE_APP_API_URL || ''))
const websiteUrl = ref(String(import.meta.env.VITE_APP_WEBSITE_URL || ''))
const clientVersion = ref('')
const dataVersion = ref<number | null>(null)
const pageLoading = ref(false)
const pageError = ref('')

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
          <span class="setting-title">自动升级</span>
          <span class="setting-desc">有新版本时自动下载并安装</span>
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

/* 访问策略：每个选项一段说明，纵向铺开而不是挤在一行 */
.policy-desc {
  margin-bottom: 14px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ev-c-text-3);
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

  .policy-label {
    font-size: 14px;
    color: var(--ev-c-text-1);
  }

  .policy-text {
    font-size: 12px;
    line-height: 1.7;
    color: var(--ev-c-text-3);
  }
}
</style>
