import { ref } from 'vue'
import { defineStore } from 'pinia'
import { useSettingsStore } from './settings'

/**
 * 工作区外的访问申请：主进程判定不了时把申请推上来等答复，在这里排队。
 *
 * 队列挂在 store 上而不是某个组件里：申请可能由后台会话或定时任务触发，
 * 队列在 store 上，界面把横幅摆在「那段对话」里就没问题。
 * 判定与执行始终在主进程，这里只负责把人拉进回路。
 *
 * **各对话相互独立**：申请带着发起它的 `conversationId` 上来，只交给那段对话处理
 * （别的对话看不到、也答不了），一段对话的审批不会串到另一段；
 * 正在等答复的那段对话，横幅摆在其输入框上方（见 ApprovalBanner），
 * 会话列表上另给一个「待确认」点（见 SidebarContent），切到别的页面也不至于漏掉。
 */

/** 一次访问申请：读文件 / 写文件 / 执行命令，命令会带上原文 */
export interface ApprovalRequest {
  id: string
  projectId: string
  path: string
  need: 'read' | 'write' | 'command'
  command?: string
  /** 发起这次访问的会话：申请只归它处理（主进程从工具调用那里带上来） */
  conversationId: string
}

/** 主进程推上来的申请：正常路径一定带会话标识，这里按可选收，缺了就走下面的告警 */
type IncomingApproval = Omit<ApprovalRequest, 'conversationId'> & { conversationId?: string }

export const useApprovalStore = defineStore('approval', () => {
  /** 待答复的申请：**逐条**确认，一段对话一次只显示最前一条 */
  const queue = ref<ApprovalRequest[]>([])
  /** 正在提交的那条申请：只让被点的那一组的按钮转圈，别的对话互不影响 */
  const submittingId = ref('')

  /** 监听主进程推上来的申请：应用启动时注册一次，重复调用不会叠加监听 */
  function listen(): void {
    if (listening) return
    listening = true
    window.ipc.workspace.onApprovalRequest((request: IncomingApproval) => {
      const conversationId = String(request.conversationId ?? '')
      // 正常路径一定带会话标识（工具调用那边一路传下来）；万一没有，这条申请无处安放，
      // 记一行日志便于排查，但不塞给别的对话——各对话相互独立是硬口径。
      if (!conversationId) console.warn('[Approval] request without conversationId:', request.id)
      queue.value.push({ ...request, conversationId })
    })
    // 主进程等到超时、按拒绝处理后会把申请作废：撤掉本地的这一条，别留着一条点不动的申请
    // （否则横幅和会话列表上的「待确认」点会一直挂着）
    window.ipc.workspace.onApprovalClosed((request: { id: string }) => {
      queue.value = queue.value.filter((item) => item.id !== request.id)
    })
  }

  /** 这段对话要答复的那条申请：只认属于自己的，别人的看不见 */
  function currentOf(conversationId: string): ApprovalRequest | null {
    const id = String(conversationId ?? '')
    if (!id) return null
    return queue.value.find((item) => item.conversationId === id) ?? null
  }

  /** 这段对话下还等着几条：侧边栏据此打「待确认」的点 */
  function pendingOf(conversationId: string): number {
    const id = String(conversationId ?? '')
    if (!id) return 0
    return queue.value.filter((item) => item.conversationId === id).length
  }

  /**
   * 答复一条申请：允许本次 / 始终允许 / 拒绝。
   * `always` 只在允许时有意义：主进程会把访问策略切成「帮我批准」（后续不再逐个询问），
   * 这里把界面上的策略显示同步过来，免得横幅已经不再弹了、底部还写着「请求批准」。
   */
  async function answer(
    request: ApprovalRequest | null,
    allow: boolean,
    always = false
  ): Promise<void> {
    if (!request || submittingId.value) return
    submittingId.value = request.id
    try {
      await window.ipc.workspace.approve({ id: request.id, allow, always: allow && always })
      if (allow && always) useSettingsStore().workspacePolicy = 'auto'
    } finally {
      submittingId.value = ''
      queue.value = queue.value.filter((item) => item.id !== request.id)
    }
  }

  /** 这条申请是不是正在提交：横幅据此只给被点的那一组按钮转圈 */
  function isSubmitting(request: ApprovalRequest | null): boolean {
    return !!request && submittingId.value === request.id
  }

  return { queue, submittingId, listen, currentOf, pendingOf, isSubmitting, answer }
})

/** 监听只注册一次：App 每次挂载都调 listen，也不该叠加监听器 */
let listening = false
