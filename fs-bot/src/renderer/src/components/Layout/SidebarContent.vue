<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { page } from '@renderer/router/config'
import { useConversationStore } from '@renderer/stores/conversation'
import { useProjectStore } from '@renderer/stores/project'
import { useChatStore } from '@renderer/stores/chat'
import { useApprovalStore } from '@renderer/stores/approval'
import { groupByTime, GROUP_LABELS } from '@renderer/utils/TimeUtil'
import { errorMessage } from '@renderer/utils/ErrorUtil'
import { confirmRemoveConversation } from '@renderer/utils/ConversationUtil'
import SidebarConversationItem from './SidebarConversationItem.vue'

const router = useRouter()
const conversationStore = useConversationStore()
const projectStore = useProjectStore()
const chatStore = useChatStore()
const approvalStore = useApprovalStore()
const loading = ref(false)
const loadError = ref('')

async function load(): Promise<void> {
  loading.value = true
  loadError.value = ''
  try {
    const results = await Promise.allSettled([
      projectStore.fetchProjects(),
      conversationStore.fetchConversations()
    ])
    const failure = results.find(
      (item): item is PromiseRejectedResult => 'rejected' === item.status
    )
    if (failure) loadError.value = errorMessage(failure.reason, '会话列表加载失败')
  } catch (error) {
    loadError.value = errorMessage(error, '会话列表加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void load()
})

const grouped = computed(() => {
  return groupByTime(conversationStore.visible)
})

const groupKeys = ['today', 'yesterday', 'thisWeek', 'earlier'] as const

function projectName(projectId: string): string {
  return projectStore.find(projectId)?.name ?? ''
}

function isRunning(id: string): boolean {
  return chatStore.find(id)?.running === true
}

/** 这段对话有没有申请在等：横幅跟着对话走，别的页面靠这里不让申请被漏掉 */
function isWaiting(id: string): boolean {
  return approvalStore.pendingOf(id) > 0
}

async function handleSelect(id: string) {
  try {
    await conversationStore.select(id)
    router.push(page.home)
  } catch (error) {
    loadError.value = errorMessage(error, '会话内容加载失败')
  }
}

async function handleDelete(id: string): Promise<void> {
  const running = chatStore.find(id)?.running === true
  if (!(await confirmRemoveConversation(running))) return
  try {
    await conversationStore.remove(id)
  } catch (error) {
    loadError.value = errorMessage(error, '删除会话失败')
  }
}

/** 重命名：标题由用户定，写进 store 与会话表，之后的轮次不再自动改标题 */
async function handleRename(id: string, title: string): Promise<void> {
  await conversationStore.rename(id, title)
}
</script>

<template>
  <el-scrollbar class="sidebar-content" v-loading="loading">
    <div class="project-filter">
      <el-select
        v-model="conversationStore.filterProjectId"
        size="small"
        placeholder="全部项目"
        clearable
      >
        <el-option
          v-for="item in projectStore.projects"
          :key="item.id"
          :label="item.name"
          :value="item.id"
        />
      </el-select>
    </div>

    <div class="mode-default">
      <div v-if="loadError" class="group-empty load-error">
        <div>{{ loadError }}</div>
        <el-button text type="primary" size="small" @click="load">重试</el-button>
      </div>
      <template v-for="key in groupKeys" :key="key">
        <div v-if="grouped[key].length > 0" class="history-group">
          <div class="group-title">{{ GROUP_LABELS[key] }}</div>
          <SidebarConversationItem
            v-for="conv in grouped[key]"
            :key="conv.id"
            :conversation="conv"
            :active="conversationStore.currentId === conv.id"
            :project-name="projectName(conv.projectId)"
            :running="isRunning(conv.id)"
            :waiting="isWaiting(conv.id)"
            @select="handleSelect($event)"
            @remove="handleDelete($event)"
            @rename="handleRename"
          />
        </div>
      </template>
      <div
        v-if="!loadError && conversationStore.visible.length === 0"
        class="group-empty"
      >
        暂无对话记录
      </div>
    </div>
  </el-scrollbar>
</template>

<style lang="scss" scoped>
.sidebar-content {
  flex: 1;
  min-height: 0;
}

.mode-default {
  padding: 8px 0;
}

.project-filter {
  padding: 8px 12px 4px;
}

.history-group {
  .group-title {
    padding: 8px 16px;
    font-size: 12px;
    color: var(--ev-c-text-3);
  }
}

.group-empty {
  padding: 32px 16px;
  text-align: center;
  font-size: 13px;
  color: var(--ev-c-text-3);
}

.load-error {
  color: var(--el-color-warning);
}
</style>
