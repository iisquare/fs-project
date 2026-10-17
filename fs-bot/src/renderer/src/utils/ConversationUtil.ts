import { ElMessageBox } from 'element-plus'

/**
 * 删除会话前的确认。
 * 生成中删除会停止这一轮（含本地命令与待确认的访问申请），并丢弃本轮结果；
 * 文案把后果说清楚，确认按钮也直接写成「停止并删除」。
 */
export async function confirmRemoveConversation(running: boolean): Promise<boolean> {
  try {
    await ElMessageBox.confirm(
      running
        ? '这段对话正在生成。删除会先停止本轮生成与正在执行的本地任务，并丢弃本轮结果；删除后无法恢复。'
        : '删除后无法恢复，确定删除这段对话吗？',
      running ? '停止并删除对话' : '删除对话',
      {
        type: 'warning',
        confirmButtonText: running ? '停止并删除' : '删除',
        cancelButtonText: '取消'
      }
    )
    return true
  } catch {
    return false
  }
}
