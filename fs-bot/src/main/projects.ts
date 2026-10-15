import { BrowserWindow, dialog } from 'electron'
import { readFileSync, writeFileSync } from 'fs'
import type { ProjectExportRequest, ProjectImportResult } from './database/types'

/**
 * 项目配置文件的导入 / 导出：原生对话框选路径，读写都在主进程完成。
 *
 * 渲染进程给的是「已经序列化好的文件内容」（导出）或直接拿回文件原文（导入），
 * 主进程不解释配置结构——格式由渲染进程的 ProjectUtil 定义并校验，
 * 这样导出的文件既能被本客户端导入，也能被管理端导出的文件覆盖同一份格式。
 */

/** 文件类型过滤：配置是 JSON，同时放开所有文件，避免用户存成别的后缀时选不到 */
const FILE_FILTERS = [
  { name: '项目配置 (JSON)', extensions: ['json'] },
  { name: '所有文件', extensions: ['*'] }
]

/** 导出：弹保存对话框并把内容写入所选文件；用户取消时返回空路径 */
export async function exportProject(
  window: BrowserWindow | null,
  request: ProjectExportRequest
): Promise<{ path: string }> {
  if (!window) throw new Error('窗口不可用，无法打开保存对话框')
  const filename = String(request?.filename ?? '').trim() || 'project.json'
  const content = String(request?.content ?? '')
  const result = await dialog.showSaveDialog(window, {
    title: '导出项目配置',
    defaultPath: filename,
    filters: FILE_FILTERS
  })
  if (result.canceled || !result.filePath) return { path: '' }
  writeFileSync(result.filePath, content, 'utf8')
  return { path: result.filePath }
}

/** 导入：弹选择对话框并读回文件原文，格式校验交给渲染进程（错误信息更好定位） */
export async function importProject(window: BrowserWindow | null): Promise<ProjectImportResult> {
  if (!window) throw new Error('窗口不可用，无法打开选择对话框')
  const result = await dialog.showOpenDialog(window, {
    title: '导入项目配置',
    properties: ['openFile'],
    filters: FILE_FILTERS
  })
  if (result.canceled || result.filePaths.length < 1) return { path: '', content: '' }
  const path = result.filePaths[0]
  return { path, content: readFileSync(path, 'utf8') }
}
