<script setup lang="ts">
/**
 * 聊天附件列表：气泡、对话历史排查区与运行日志详情共用。
 * 文件信息来自文件服务（id / name / type / suffix / size），点击后获取签名地址并在新页面打开。
 */
import { ElMessage } from 'element-plus'
import { Box, Close, Document, Grid, Loading, Paperclip, Picture } from '@element-plus/icons-vue'
import FileApi from '@/api/server/FileApi'
import ApiUtil from '@/utils/ApiUtil'

const props = defineProps<{
  files?: any[]
  title?: string
  removable?: boolean
}>()
const emit = defineEmits<{
  remove: [file: any, index: number]
}>()

const attachmentName = (file: any) => String(file?.name || file?.id || '附件')
const attachmentSuffix = (file: any) => {
  const source = String(file?.suffix || file?.name || '')
  const suffix = source.indexOf('.') >= 0 ? source.split('.').pop() : source
  return String(suffix || '').toUpperCase()
}
const attachmentKind = (file: any) => {
  const type = String(file?.type || '').toLowerCase()
  const suffix = attachmentSuffix(file).toLowerCase()
  if (type.startsWith('image/') || ['png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp'].includes(suffix)) return 'image'
  if ('pdf' === suffix) return 'pdf'
  if (['doc', 'docx', 'rtf'].includes(suffix)) return 'word'
  if (['xls', 'xlsx', 'csv'].includes(suffix)) return 'sheet'
  if (['zip', 'rar', '7z', 'tar', 'gz'].includes(suffix)) return 'archive'
  if (['txt', 'md', 'json', 'xml', 'yaml', 'yml'].includes(suffix)) return 'text'
  return 'file'
}
const attachmentIcon = (file: any) => {
  const kind = attachmentKind(file)
  if ('image' === kind) return Picture
  if ('sheet' === kind) return Grid
  if ('archive' === kind) return Box
  return Document
}
const attachmentSize = (value: any) => {
  const size = Number(value ?? 0)
  if (!Number.isFinite(size) || size <= 0) return ''
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}
const attachmentMeta = (file: any) => {
  if ('uploading' === file?.status) return '正在上传…'
  if ('failed' === file?.status) return '上传失败'
  const suffix = attachmentSuffix(file)
  const meta = suffix.length <= 10 ? suffix : ''
  return [meta, attachmentSize(file?.size)].filter((text: string) => text).join(' · ')
}
const removableFile = (file: any) => props.removable && 'uploading' !== file?.status
const actionableFile = (file: any) => 'uploading' !== file?.status && 'failed' !== file?.status

/**
 * 先同步占住一个标签页，签名地址返回后替换地址，
 * 避免异步请求完成后 window.open 被浏览器当作弹窗拦截。
 */
const openAttachment = (file: any) => {
  if (!actionableFile(file)) return
  const direct = String(file?.url || file?.downloadUrl || '').trim()
  const target = window.open('', '_blank')
  if (target) {
    target.opener = null
    target.document.title = `正在打开 ${attachmentName(file)}…`
  }
  const openUrl = (url: string) => {
    if (target) target.location.replace(url)
    else window.open(url, '_blank')
  }
  if (direct) {
    openUrl(direct)
    return
  }
  if (!file?.id) {
    target?.close()
    ElMessage.warning('该附件缺少文件标识，暂时无法下载')
    return
  }
  FileApi.download(file.id, { success: false, warning: false }).then((result: any) => {
    const url = String(ApiUtil.data(result) ?? '').trim()
    if (!url) throw ApiUtil.result(1404, '获取附件下载地址失败')
    openUrl(url)
  }).catch((error: any) => {
    target?.close()
    ElMessage.error(ApiUtil.message(error) || '附件下载失败')
  })
}
</script>

<template>
  <div class="chat-attachments" v-if="props.files?.length">
    <div class="chat-attachments-title" v-if="props.title">
      <el-icon><Paperclip /></el-icon>
      <span>{{ props.title }}</span>
      <span class="chat-attachments-count">{{ props.files?.length }}</span>
    </div>
    <div
      class="chat-attachment"
      :class="[`is-${attachmentKind(file)}`, { 'is-uploading': 'uploading' === file?.status }]"
      :key="file.id ?? file.key ?? fileIndex"
      v-for="(file, fileIndex) in props.files">
      <button
        class="chat-attachment-main"
        type="button"
        :disabled="!actionableFile(file)"
        :title="actionableFile(file) ? `在新页面打开 ${attachmentName(file)}` : attachmentMeta(file)"
        @click="openAttachment(file)">
        <span class="chat-attachment-icon">
          <el-icon v-if="'uploading' === file?.status" class="is-loading"><Loading /></el-icon>
          <el-icon v-else><component :is="attachmentIcon(file)" /></el-icon>
        </span>
        <span class="chat-attachment-copy">
          <span class="chat-attachment-name">{{ attachmentName(file) }}</span>
          <span class="chat-attachment-meta" v-if="attachmentMeta(file)">{{ attachmentMeta(file) }}</span>
        </span>
      </button>
      <button
        class="chat-attachment-remove"
        type="button"
        title="移除附件"
        v-if="removableFile(file)"
        @click.stop="emit('remove', file, fileIndex)">
        <el-icon><Close /></el-icon>
      </button>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.chat-attachments {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
}
.chat-attachments-title {
  display: flex;
  align-items: center;
  gap: 5px;
  width: 100%;
  margin-bottom: -1px;
  font-size: 11px;
  line-height: 18px;
  color: var(--el-text-color-placeholder);
  .chat-attachments-count {
    min-width: 18px;
    padding: 0 5px;
    border-radius: 9px;
    text-align: center;
    color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
  }
}
.chat-attachment {
  display: flex;
  align-items: center;
  min-width: 0;
  max-width: 100%;
  min-height: 48px;
  border: solid 1px var(--el-color-primary-light-7);
  border-radius: 9px;
  color: var(--el-text-color-regular);
  background: var(--el-bg-color);
  box-shadow: 0 1px 2px rgb(0 0 0 / 3%);
  transition: border-color 0.2s, box-shadow 0.2s, transform 0.2s;
  &:hover,
  &:focus-within {
    border-color: var(--el-color-primary);
    box-shadow: 0 3px 10px rgb(0 0 0 / 7%);
    transform: translateY(-1px);
  }
  &.is-uploading {
    border-style: dashed;
    box-shadow: none;
    transform: none;
  }
}
.chat-attachment-main {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 0;
  max-width: 100%;
  padding: 7px 9px;
  border: 0;
  color: inherit;
  background: transparent;
  font: inherit;
  text-align: left;
  cursor: pointer;
  &:focus-visible {
    outline: none;
  }
  &:disabled {
    cursor: default;
  }
}
.chat-attachment-icon {
  flex: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 31px;
  height: 31px;
  border-radius: 8px;
  font-size: 17px;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  .is-image & {
    color: #3b82b5;
    background: #eaf5fb;
  }
  .is-pdf & {
    color: #d85d5d;
    background: #fdf0f0;
  }
  .is-word & {
    color: #4c7bd9;
    background: #eef3fd;
  }
  .is-sheet & {
    color: #3e9466;
    background: #edf8f1;
  }
  .is-archive & {
    color: #b1813f;
    background: #fbf4e8;
  }
  .is-loading {
    animation: attachment-spin 1s linear infinite;
  }
}
.chat-attachment-copy {
  display: flex;
  flex-direction: column;
  min-width: 0;
  line-height: 1.35;
}
.chat-attachment-name {
  min-width: 0;
  max-width: 270px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12.5px;
  font-weight: 500;
  color: var(--el-text-color-primary);
}
.chat-attachment-meta {
  margin-top: 3px;
  font-size: 11px;
  color: var(--el-text-color-placeholder);
}
.chat-attachment-remove {
  flex: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  margin: 0 5px 0 0;
  padding: 0;
  border: 0;
  border-radius: 5px;
  color: var(--el-text-color-placeholder);
  background: transparent;
  cursor: pointer;
  &:hover {
    color: var(--el-color-danger);
    background: var(--el-color-danger-light-9);
  }
}
@keyframes attachment-spin {
  to {
    transform: rotate(360deg);
  }
}
@media (max-width: 520px) {
  .chat-attachment {
    max-width: 100%;
  }
  .chat-attachment-name {
    max-width: 190px;
  }
}
</style>
