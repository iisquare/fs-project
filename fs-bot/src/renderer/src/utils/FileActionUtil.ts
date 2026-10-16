import { ElMessage, ElMessageBox } from 'element-plus'

/**
 * 对话里的文件动作：打开与「在文件夹中显示」都交给主进程执行（系统默认程序 / 文件管理器），
 * 渲染进程只负责发起，并把结论讲清楚。
 *
 * 两种不成功要分开处理：路径不存在这类直接报错；可执行 / 脚本文件是主进程按安全默认拒绝的
 * （打开即运行），这里弹一次确认再带着 force 重来——对话里的路径可能来自模型输出，
 * 不能点一下就执行。
 *
 * `localPath` 用在同一件事的入口上：正文里的 Markdown 链接如果是本机路径，
 * 就不该继续走「外部浏览器打开」那条路（见 components/Chat/ChatMarkdown.vue）。
 */
const FileActionUtil = {
  /** 用系统默认程序打开本机文件 / 目录（目录交给文件管理器） */
  async open(projectId: string, path: string): Promise<void> {
    const target = String(path ?? '').trim()
    if (!target) return
    const project = String(projectId ?? '')
    let result = await window.ipc.workspace.open(project, target)
    if (!result.ok && result.unsafe) {
      try {
        await ElMessageBox.confirm(
          `${result.path}\n\n这是可执行 / 脚本文件，用系统默认程序打开会真的运行它。确认打开吗？`,
          '确认打开',
          { type: 'warning', confirmButtonText: '仍然打开', cancelButtonText: '取消' }
        )
      } catch {
        return
      }
      result = await window.ipc.workspace.open(project, target, true)
    }
    if (!result.ok) ElMessage.error(result.message || `打开失败：${target}`)
  },

  /** 在系统文件管理器中定位这个文件 / 目录（可执行文件与已删改的文件都还能这么找） */
  async reveal(projectId: string, path: string): Promise<void> {
    const target = String(path ?? '').trim()
    if (!target) return
    const result = await window.ipc.workspace.reveal(String(projectId ?? ''), target)
    if (!result.ok) ElMessage.error(result.message || `无法定位：${target}`)
  },

  /**
   * 链接地址 → 本机路径：本机路径返回原样的路径，外部地址返回空串（调用方按原来的口径处理）。
   *
   * 认三种写法：`file:///D:/a/b.md`、Windows 盘符与 UNC 共享（`D:\a\b.md`、`\\server\share\a.md`）、
   * 其余不带协议的（`src/App.vue`，按工作区目录解析）。带协议的一律不算本机路径——http(s)、mailto
   * 与知识库图片的 kb: 都走各自那条路。
   */
  localPath(href: string): string {
    const value = String(href ?? '').trim()
    if (!value || value.startsWith('#')) return ''
    // 盘符与 UNC 长得也像「协议 + 冒号」，先单独认下来，否则会被下面的协议判断筛掉
    if (/^[a-z]:[\\/]/i.test(value) || value.startsWith('\\\\')) return value
    if ('file:' === value.slice(0, 5).toLowerCase()) {
      try {
        // file:///D:/a/b.md → /D:/a/b.md：去掉开头那个斜杠才是 Windows 认得的形式
        const path = decodeURIComponent(new URL(value).pathname)
        return /^\/[a-z]:/i.test(path) ? path.slice(1) : path
      } catch {
        return ''
      }
    }
    return /^[a-z][a-z0-9+.-]*:/i.test(value) ? '' : value
  }
}

export default FileActionUtil
