<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useSettingsStore } from '@renderer/stores/settings'
import { useSkillStore } from '@renderer/stores/skill'

const settings = useSettingsStore()
const skillStore = useSkillStore()

const apiUrl = ref(String(import.meta.env.VITE_APP_API_URL || ''))
const websiteUrl = ref(String(import.meta.env.VITE_APP_WEBSITE_URL || ''))

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

onMounted(async () => {
  await settings.fetchSettings()
  await skillStore.fetchInstalled()
})
</script>

<template>
  <div class="system-settings">
    <h2>系统设置</h2>
    <div class="settings-card">
      <div class="setting-item">
        <div class="setting-label">
          <span class="setting-title">开机启动</span>
          <span class="setting-desc">系统启动时自动运行 fs-bot</span>
        </div>
        <el-switch
          :model-value="settings.autoStart"
          @update:model-value="(val: any) => settings.toggleAutoStart(val)"
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
    </div>

    <h2 class="section-title">服务端</h2>
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

    <h2 class="section-title">本地目录</h2>
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
        <el-radio class="policy-item" v-for="item in policies" :key="item.value" :value="item.value">
          <span class="policy-label">{{ item.label }}</span>
          <span class="policy-text">{{ item.description }}</span>
        </el-radio>
      </el-radio-group>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.system-settings {
  padding: 40px;

  h2 {
    font-size: 20px;
    color: var(--ev-c-text-1);
    margin-bottom: 24px;
  }
}

.settings-card {
  background: var(--el-bg-color);
  border-radius: 8px;
  padding: 24px;
  border: 1px solid var(--el-border-color-light);
}

.section-title {
  margin: 28px 0 16px;
  font-size: 16px;
  color: var(--ev-c-text-1);
}

.setting-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  gap: 16px;
}

.setting-label {
  display: flex;
  flex-direction: column;

  .setting-title {
    font-size: 15px;
    color: var(--ev-c-text-1);
  }

  .setting-desc {
    font-size: 13px;
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
  margin-bottom: 12px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ev-c-text-3);
}

.policy-group {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 4px;
}

.policy-item {
  height: auto;
  padding: 8px 0;
  align-items: flex-start;

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
