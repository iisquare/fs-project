<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import AgentDb, {
  type AgentInteractionTask,
  type AgentSession,
  type AgentStatus
} from '@renderer/db/AgentDb'
import type { AgentRunView } from '@renderer/stores/agents'
import AgentSessionDetail from '@renderer/components/Chat/AgentSessionDetail.vue'

const route = useRoute()
const sessionId = String(route.query.sessionId ?? '').trim()
const session = ref<AgentSession | null>(null)
const interaction = ref<AgentInteractionTask | null>(null)
const visible = ref(true)
let unsubscribe: (() => void) | null = null

const agentStatus = computed<AgentStatus>(
  () => interaction.value?.status ?? session.value?.status ?? 'running'
)
const run = computed<AgentRunView | null>(() => {
  const current = session.value
  if (!current) return null
  return {
    sessionId: current.id,
    parentConversationId: current.parentConversationId,
    agent: current.agent,
    objective: interaction.value?.objective || current.title,
    taskKey: current.taskKey,
    sessionMode: current.sessionMode,
    reused: current.reused,
    status: agentStatus.value,
    round: interaction.value?.rounds.length ?? 0,
    tool: '',
    message: current.error || current.summary,
    startedAt: interaction.value?.startedAt || current.createdAt,
    lastActivityAt: interaction.value?.updatedAt || current.updatedAt,
    stalled: false,
    traceCount: current.trace.length,
    interaction: interaction.value,
    managed: false
  }
})

function isInteraction(value: unknown): value is AgentInteractionTask {
  return (
    !!value &&
    'object' === typeof value &&
    'string' === typeof (value as { id?: unknown }).id &&
    Array.isArray((value as { rounds?: unknown }).rounds)
  )
}

async function load(): Promise<void> {
  if (!sessionId) return
  session.value = await AgentDb.get(sessionId)
}

async function close(): Promise<void> {
  await window.ipc.agent.closeSelf()
}

onMounted(() => {
  void load()
  unsubscribe = window.ipc.agent.onUpdate((update) => {
    if (update.sessionId !== sessionId || !isInteraction(update.interaction)) return
    interaction.value = update.interaction
    document.title = `${
      interaction.value.objective || session.value?.title || '子 Agent'
    } - 执行详情`
  })
})

onBeforeUnmount(() => {
  unsubscribe?.()
})
</script>

<template>
  <div class="agent-detail-window">
    <AgentSessionDetail
      v-model="visible"
      :session-id="sessionId"
      :parent-conversation-id="session?.parentConversationId ?? ''"
      :project-id="session?.projectId ?? ''"
      :run="run"
      standalone
      @update:model-value="!$event && close()"
    />
  </div>
</template>

<style scoped>
.agent-detail-window {
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background: var(--el-bg-color);
}
</style>
