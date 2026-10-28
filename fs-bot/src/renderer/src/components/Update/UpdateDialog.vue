<script setup lang="ts">
import { computed } from 'vue'
import { useUpdateStore } from '@renderer/stores/update'

const update = useUpdateStore()
const visible = computed({
  get: () => update.visible,
  set: (value: boolean) => {
    if (value || !update.forceUpgrade) update.visible = value
  }
})

const latest = computed(() => update.latest)

function formatSize(bytes: number): string {
  if (!Number.isFinite(bytes) || bytes <= 0) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function quitApplication(): void {
  void window.ipc.quit()
}
</script>

<template>
  <el-dialog
    v-model="visible"
    width="min(520px, calc(100vw - 48px))"
    :show-close="!update.forceUpgrade && !update.installing"
    :close-on-click-modal="!update.forceUpgrade && !update.installing"
    :close-on-press-escape="!update.forceUpgrade && !update.installing"
    :z-index="10000"
    append-to-body
    align-center
    class="update-dialog"
  >
    <template #header>
      <div class="update-heading">
        <el-tag :type="update.forceUpgrade ? 'danger' : 'primary'" effect="dark">
          {{ update.rollback ? '版本回滚' : update.forceUpgrade ? '必须升级' : '版本更新' }}
        </el-tag>
        <span>{{
          update.rollback
            ? `当前版本已停止使用，回滚到 v${latest?.version}`
            : update.forceUpgrade
              ? '当前版本已停止支持'
              : `发现新版本 v${latest?.version}`
        }}</span>
      </div>
    </template>

    <div v-if="latest" class="update-body">
      <div class="version-line">
        <span>当前版本 v{{ update.result?.currentVersion }}</span>
        <span>→</span>
        <strong>最新版本 v{{ latest.version }}</strong>
      </div>
      <el-alert
        v-if="update.rollback"
        type="error"
        :closable="false"
        show-icon
        :title="`将回滚到 v${latest.version}，完成回滚后才能继续使用。`"
      />
      <el-alert
        v-else-if="update.forceUpgrade"
        type="error"
        :closable="false"
        show-icon
        :title="`最低支持版本为 v${latest.minimumVersion}，完成升级后才能继续使用。`"
      />
      <el-alert
        v-else
        type="info"
        :closable="false"
        show-icon
        :title="`最低支持版本为 v${latest.minimumVersion}，当前版本仍在支持范围内。`"
      />
      <div class="release">
        <div class="release-title">发布日志</div>
        <pre class="release-notes">{{ latest.releaseNotes || '本次更新暂无发布日志' }}</pre>
      </div>
      <el-progress
        v-if="update.installing || update.paused"
        :percentage="update.progress"
        :indeterminate="update.installing && !update.total"
        :status="update.installing && update.progress === 100 ? 'success' : undefined"
      />
      <div v-if="update.installing || update.paused" class="progress-text">
        {{
          update.paused
            ? `下载已暂停，已下载 ${formatSize(update.received)} / ${formatSize(update.total)}`
            : update.total > 0
              ? `${formatSize(update.received)} / ${formatSize(update.total)}`
              : '正在准备下载...'
        }}
        <span v-if="update.installing && update.progress === 100">
          ，SHA-512 校验通过，正在启动安装程序
        </span>
      </div>
      <el-alert
        v-if="update.error"
        type="error"
        :closable="false"
        show-icon
        :title="update.error"
      />
    </div>

    <template #footer>
      <template v-if="update.installing && !update.paused">
        <el-button @click="update.pause()">暂停下载</el-button>
        <el-button type="danger" plain @click="update.cancel()">终止下载</el-button>
      </template>
      <template v-else-if="update.paused">
        <el-button type="danger" plain @click="update.cancel()">终止下载</el-button>
        <el-button type="primary" @click="update.install()">继续下载</el-button>
      </template>
      <template v-else>
        <el-button v-if="!update.forceUpgrade" @click="update.dismiss()">稍后提醒</el-button>
        <el-button v-else @click="quitApplication">退出应用</el-button>
        <el-button type="primary" @click="update.install()">
          {{ update.cancelled ? '重新下载' : update.error ? '重试下载' : '下载并安装' }}
        </el-button>
      </template>
    </template>
  </el-dialog>
</template>

<style lang="scss" scoped>
.update-heading {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 17px;
  font-weight: 600;
  color: var(--ev-c-text-1);
}

:global(.update-dialog) {
  max-height: calc(100vh - 48px);
  margin: 24px auto;
}

:global(.update-dialog .el-dialog__body) {
  max-height: calc(100vh - 190px);
  overflow-y: auto;
}

.update-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.version-line {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--ev-c-text-2);

  strong {
    color: var(--ev-c-text-1);
  }
}

.release {
  padding: 12px 14px;
  border: 1px solid var(--ui-card-border);
  border-radius: 8px;
  background: var(--ui-surface-hover);
}

.release-title {
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--ev-c-text-1);
}

.release-notes {
  max-height: 220px;
  margin: 0;
  overflow: auto;
  font: inherit;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  color: var(--ev-c-text-2);
}

.progress-text {
  font-size: 12px;
  color: var(--ev-c-text-3);
}
</style>
