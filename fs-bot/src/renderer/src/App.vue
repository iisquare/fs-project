<script setup lang="ts">
import { onMounted, provide, watch } from 'vue'
import { useRouter } from 'vue-router'
import locale from 'element-plus/dist/locale/zh-cn.mjs'
import { useUserStore } from '@renderer/stores/user'
import { useProjectStore } from '@renderer/stores/project'
import { useConversationStore } from '@renderer/stores/conversation'
import { useChatStore } from '@renderer/stores/chat'
import { useScheduleStore } from '@renderer/stores/schedule'
import { useApprovalStore } from '@renderer/stores/approval'
import UserApi from '@renderer/api/UserApi'
import { page } from '@renderer/router/config'

const user = useUserStore()
const router = useRouter()

/** 已经准备过的身份（用户 + 令牌）：同一个身份只准备一次 */
let bootstrappedFor = ''

async function logout() {
  await UserApi.logout().finally(() => {
    // 退出前断开所有流并停掉调度，避免携带上一个账号的会话继续跑
    useChatStore().reset()
    useScheduleStore().stop()
    useConversationStore().reset()
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
async function bootstrap() {
  if (!user.ready || !user.isLogined()) return
  const identity = `${user.info.id}:${user.info.token}`
  if (bootstrappedFor === identity) return
  bootstrappedFor = identity
  try {
    const projectStore = useProjectStore()
    const scheduleStore = useScheduleStore()
    await projectStore.fetchProjects()
    await useConversationStore().fetchConversations()
    await scheduleStore.fetchTasks()
    scheduleStore.start()
  } catch (error) {
    // 失败不记为已完成：状态再变化时还能重试
    bootstrappedFor = ''
    console.error('[App] Failed to bootstrap:', error)
  }
}

onMounted(() => {
  // 登录态与用户库已在 main.ts 里准备好，这里只做登录后的数据准备
  // 工作区外的访问申请：队列挂在 approval store 上，横幅由对话页或布局展示
  useApprovalStore().listen()
  void bootstrap()
})

// 登录 / 还原完成（ready 翻到 true）再准备一次；同一身份不会重复准备
watch(
  () => user.ready,
  () => {
    void bootstrap()
  }
)

provide('logout', logout)
</script>

<template>
  <el-config-provider :locale="locale">
    <router-view />
  </el-config-provider>
</template>
