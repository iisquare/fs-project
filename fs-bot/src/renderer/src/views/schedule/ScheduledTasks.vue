<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useProjectStore } from '@renderer/stores/project'
import { nextRun, useScheduleStore } from '@renderer/stores/schedule'
import { errorMessage } from '@renderer/utils/ErrorUtil'
import type { ScheduleTask } from '@renderer/db/TaskDb'

const projectStore = useProjectStore()
const scheduleStore = useScheduleStore()

const mode = ref<'list' | 'create' | 'edit'>('list')
const editingId = ref('')
const form = ref(emptyForm())
const pageLoading = ref(false)
const pageError = ref('')

function emptyForm() {
  return {
    name: '',
    projectId: '',
    cron: '0 9 * * *',
    prompt: '',
    enabled: true
  }
}

async function loadPage(): Promise<void> {
  pageLoading.value = true
  pageError.value = ''
  try {
    const results = await Promise.allSettled([
      projectStore.fetchProjects(),
      scheduleStore.fetchTasks()
    ])
    const failure = results.find(
      (item): item is PromiseRejectedResult => 'rejected' === item.status
    )
    if (failure) pageError.value = errorMessage(failure.reason, '定时任务加载失败')
  } catch (error) {
    pageError.value = errorMessage(error, '定时任务加载失败')
  } finally {
    pageLoading.value = false
  }
}

onMounted(() => {
  void loadPage()
})

const nextText = computed(() => {
  const next = nextRun(form.value.cron)
  if (!next) return '表达式无效或一年内不会触发'
  return next.toLocaleString('zh-CN')
})

function openCreate() {
  editingId.value = ''
  form.value = {
    ...emptyForm(),
    // 没有「当前项目」了：默认取列表第一个，任务自己记住绑定哪个项目
    projectId: projectStore.projects[0]?.id || ''
  }
  mode.value = 'create'
}

function openEdit(task: ScheduleTask) {
  editingId.value = task.id
  form.value = {
    name: task.name,
    projectId: task.projectId,
    cron: task.cron,
    prompt: task.prompt,
    enabled: task.enabled
  }
  mode.value = 'edit'
}

/** 列表与表单是两个独立视图：表单占满内容区，不和列表分栏 */
function backToList() {
  mode.value = 'list'
  editingId.value = ''
}

async function submit() {
  if (!form.value.name.trim()) {
    ElMessage.warning('请输入任务名称')
    return
  }
  if (!form.value.projectId) {
    ElMessage.warning('请选择项目')
    return
  }
  if (!nextRun(form.value.cron)) {
    ElMessage.warning('cron 表达式无效（5 段：分 时 日 月 周）')
    return
  }
  try {
    if (editingId.value) {
      await scheduleStore.update(editingId.value, form.value)
    } else {
      await scheduleStore.create(form.value)
    }
    backToList()
  } catch (error) {
    ElMessage.error(errorMessage(error, '保存定时任务失败'))
  }
}

async function runNow(task: ScheduleTask) {
  try {
    const ok = await scheduleStore.run(task)
    if (ok) ElMessage.success('已发起对话，可在会话列表查看运行结果')
    else ElMessage.error('任务未执行：项目不存在或已删除')
  } catch (error) {
    ElMessage.error(errorMessage(error, '运行定时任务失败'))
  }
}

async function remove(task: ScheduleTask) {
  try {
    await ElMessageBox.confirm(`确定删除任务「${task.name}」？`, '确认删除', { type: 'warning' })
  } catch {
    return
  }
  try {
    await scheduleStore.remove(task.id)
  } catch (error) {
    ElMessage.error(errorMessage(error, '删除定时任务失败'))
  }
}

async function toggleTask(task: ScheduleTask): Promise<void> {
  try {
    await scheduleStore.toggle(task)
  } catch (error) {
    ElMessage.error(errorMessage(error, '切换任务状态失败'))
  }
}

function projectName(projectId: string): string {
  return projectStore.find(projectId)?.name ?? '（项目已删除）'
}

function nextOf(task: ScheduleTask): string {
  if (!task.enabled) return '-'
  const next = nextRun(task.cron)
  return next ? next.toLocaleString('zh-CN') : '表达式无效'
}

function statusType(status: string): 'success' | 'danger' | 'info' {
  if (status === 'failed') return 'danger'
  if (status === 'triggered') return 'success'
  return 'info'
}

function statusText(status: string): string {
  if (status === 'failed') return '失败'
  if (status === 'triggered') return '已触发'
  return '未执行'
}
</script>

<template>
  <!-- 表单视图是卡片外壳（头部自带返回栏），顶部留白比列表页再紧一档 -->
  <div class="scheduled-tasks ui-page" :class="{ 'ui-page--editor': mode !== 'list' }">
    <!-- 只有列表视图有页头；表单视图只留一条返回栏 -->
    <div v-if="mode === 'list'" class="ui-page-header">
      <div class="ui-page-header__copy">
        <h1 class="ui-page-title">定时任务</h1>
        <p class="ui-page-desc">
          按 cron 表达式自动以指定项目发起对话；调度在客户端运行，应用保持运行且已登录即会执行。
        </p>
      </div>
      <div class="ui-page-actions">
        <el-button type="primary" @click="openCreate">创建任务</el-button>
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
      <el-button text type="primary" size="small" @click="loadPage">重试</el-button>
    </el-alert>

    <!-- 列表视图：行式排布，窗口变窄时自动换行，不出现横向滚动条 -->
    <div v-if="mode === 'list'" v-loading="scheduleStore.loading || pageLoading" class="task-list">
      <el-empty v-if="scheduleStore.tasks.length === 0" description="暂无定时任务" />
      <div v-for="task in scheduleStore.tasks" :key="task.id" class="task-row">
        <div class="task-main">
          <div class="task-head">
            <span class="task-name">{{ task.name }}</span>
            <el-tag size="small" :type="statusType(task.lastStatus)">
              {{ statusText(task.lastStatus) }}
            </el-tag>
            <span v-if="task.lastRunAt" class="task-last">{{ task.lastRunAt }}</span>
          </div>
          <p class="task-prompt">{{ task.prompt }}</p>
          <div class="task-meta">
            <span>项目：{{ projectName(task.projectId) }}</span>
            <code>{{ task.cron }}</code>
            <span>下次执行：{{ nextOf(task) }}</span>
          </div>
        </div>
        <div class="task-side">
          <el-switch
            :model-value="task.enabled"
            :active-text="task.enabled ? '启用' : '停用'"
            @update:model-value="toggleTask(task)"
          />
          <div class="task-actions">
            <el-button text type="primary" @click="runNow(task)">运行一次</el-button>
            <el-button text @click="openEdit(task)">编辑</el-button>
            <el-button text type="danger" @click="remove(task)">删除</el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 表单视图 -->
    <section v-else class="editor-pane ui-editor">
      <div class="editor-bar ui-editor-bar">
        <el-button text class="ui-back-button" @click="backToList">← 返回任务列表</el-button>
      </div>
      <el-form :model="form" label-width="96px" class="task-form">
        <el-form-item label="任务名称" required>
          <el-input v-model="form.name" placeholder="例如：每日简报" />
        </el-form-item>
        <el-form-item label="项目" required>
          <el-select v-model="form.projectId" placeholder="选择项目" style="width: 100%">
            <el-option
              v-for="item in projectStore.projects"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="cron" required>
          <el-input v-model="form.cron" placeholder="分 时 日 月 周" />
          <div class="field-tip">下次执行：{{ nextText }}</div>
        </el-form-item>
        <el-form-item label="提示词" required>
          <el-input
            v-model="form.prompt"
            type="textarea"
            :rows="4"
            placeholder="触发时发送给应用的内容"
          />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>

        <div class="editor-footer">
          <el-button @click="backToList">取消</el-button>
          <el-button type="primary" @click="submit">保存</el-button>
        </div>
      </el-form>
    </section>
  </div>
</template>

<style lang="scss" scoped>
.page-alert {
  margin-bottom: 16px;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.task-row {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  flex-wrap: wrap;
  padding: 18px 20px;
  background: var(--ui-surface);
  border: 1px solid var(--ui-card-border);
  border-radius: var(--ui-card-radius);
  box-shadow: var(--ui-card-shadow);
  transition:
    border-color 0.2s,
    background-color 0.2s,
    box-shadow 0.2s;

  &:hover {
    background: var(--ui-surface-hover);
    border-color: color-mix(in srgb, var(--el-color-primary) 18%, var(--ui-card-border));
    box-shadow: var(--ui-card-shadow-hover);
  }
}

.task-main {
  flex: 1;
  min-width: 260px;
}

.task-head {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.task-name {
  min-width: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--ev-c-text-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-last {
  font-size: 12px;
  color: var(--ev-c-text-3);
  white-space: nowrap;
}

.task-prompt {
  margin: 6px 0 0;
  font-size: 13px;
  line-height: 1.6;
  color: var(--ev-c-text-3);
  word-break: break-word;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.task-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 8px;
  font-size: 12px;
  color: var(--ev-c-text-3);

  code {
    padding: 1px 6px;
    border-radius: 4px;
    font-size: 12px;
    background: var(--el-fill-color-light);
  }
}

.task-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  flex-shrink: 0;
  padding-left: 16px;
  border-left: 1px solid var(--el-border-color-lighter);
}

.task-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.field-tip {
  margin-top: 4px;
  font-size: 12px;
  color: var(--ev-c-text-3);
}

.editor-pane {
  min-height: 360px;
}

.task-form {
  padding: 24px 24px 0;
}

.editor-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 18px;
  margin: 4px -24px 0;
  background: color-mix(in srgb, var(--ui-surface) 92%, transparent);
  border: 0;
  border-top: 1px solid var(--ui-divider);
  border-radius: 0 0 var(--ui-card-radius) var(--ui-card-radius);
  box-shadow: none;
}

@media (max-width: 620px) {
  .task-side {
    align-items: flex-start;
    width: 100%;
    padding-top: 12px;
    padding-left: 0;
    border-top: 1px solid var(--el-border-color-lighter);
    border-left: 0;
  }
}
</style>
