/**
 * 链接 / 图片地址 → 本机路径：本机路径返回原样的路径，外部地址返回空串。
 *
 * 认三种写法：`file:///D:/a/b.md`、Windows 盘符与 UNC 共享（`D:\a\b.md`、`\\server\share\a.md`）、
 * 其余不带协议的（`src/App.vue`，按工作区目录解析）。带协议的一律不算本机路径——http(s)、mailto
 * 与知识库图片的 kb: 都走各自那条路。
 *
 * 文件链接（`FileActionUtil.localPath`）与正文图片（`core/Markdown.ts` 的本机图片识别）共用这一处，
 * 免得两边的路径口径各算一遍、慢慢对不上。
 */
export function localPath(href: string): string {
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
