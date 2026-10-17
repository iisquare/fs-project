/**
 * 对话正文里的本机图片解析
 *
 * 正文只带路径（`![说明](local:相对路径)`），展示地址由主进程按项目工作区权限读取后签发：
 * - 权限判定、工作区外访问策略与普通文件读取同一套（`workspace:image` → WorkspaceManager.image）；
 * - 地址是 `chat-image://<随机标识>`，渲染进程拿不到绝对路径，也拼不出别的文件；
 * - 每条正文各自解析（调用方按路径去重），不做跨消息缓存，授权变更后历史消息里的图片同步不可见。
 */
const LocalImageUtil = {
  /**
   * 解析一张本机图片
   * @returns 可展示地址；无权限、文件不存在、格式不支持或请求失败时返回空串（调用方回落默认图）
   */
  async resolve(projectId: string, path: string, conversationId = ''): Promise<string> {
    const target = String(path ?? '').trim()
    if (!target) return ''
    try {
      const result = await window.ipc.workspace.image(
        String(projectId ?? ''),
        target,
        String(conversationId ?? '')
      )
      return String(result?.url ?? '')
    } catch {
      // 渲染期的后台请求：失败不弹通知，统一按「未取得地址」回落默认图
      return ''
    }
  }
}

export default LocalImageUtil
