<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import ChatMarkdown from '@renderer/components/Chat/ChatMarkdown.vue'
import { useChatViewStore } from '@renderer/stores/chatView'
import type { MessageExportFormat, MessagePreviewPayload } from '../../../../shared/message-export'

interface ChatMarkdownHandle {
  buildExportHtml: () => Promise<string>
  buildPlainText: () => Promise<string>
}

const route = useRoute()
const windowId = String(route.query.windowId ?? '').trim()
const payload = ref<MessagePreviewPayload | null>(null)
const loading = ref(true)
const exporting = ref(false)
const printing = ref(false)
const copied = ref(false)
const markdownRef = ref<ChatMarkdownHandle | null>(null)
const chatView = useChatViewStore()
const DISPLAY_SETTINGS_KEY = 'fs_message_preview_display'
const DEFAULT_DISPLAY_SETTINGS = {
  zoom: 100,
  fontSize: 15,
  lineHeight: 1.8
}

interface DisplaySettings {
  zoom: number
  fontSize: number
  lineHeight: number
}

function boundedNumber(value: unknown, fallback: number, min: number, max: number): number {
  const number = Number(value)
  return Number.isFinite(number) ? Math.min(max, Math.max(min, number)) : fallback
}

function restoreDisplaySettings(): DisplaySettings {
  try {
    const saved = JSON.parse(localStorage.getItem(DISPLAY_SETTINGS_KEY) ?? '{}') as Partial<
      Record<keyof DisplaySettings, unknown>
    >
    return {
      zoom: boundedNumber(saved.zoom, DEFAULT_DISPLAY_SETTINGS.zoom, 75, 200),
      fontSize: boundedNumber(saved.fontSize, DEFAULT_DISPLAY_SETTINGS.fontSize, 13, 24),
      lineHeight: boundedNumber(saved.lineHeight, DEFAULT_DISPLAY_SETTINGS.lineHeight, 1.4, 2.4)
    }
  } catch {
    return { ...DEFAULT_DISPLAY_SETTINGS }
  }
}

const display = reactive<DisplaySettings>(restoreDisplaySettings())
const zoomContentRef = ref<HTMLElement | null>(null)
const zoomContentHeight = ref(0)
const zoomScale = computed(() => display.zoom / 100)
const zoomViewportStyle = computed(() => ({
  height: `${zoomContentHeight.value * zoomScale.value}px`
}))
const zoomContentStyle = computed(() => ({
  width: `${100 / zoomScale.value}%`,
  transform: `scale(${zoomScale.value})`
}))
let zoomResizeObserver: ResizeObserver | null = null

function syncZoomHeight(): void {
  zoomContentHeight.value = zoomContentRef.value?.offsetHeight ?? 0
}

function observeZoomContent(): void {
  zoomResizeObserver?.disconnect()
  zoomResizeObserver = null
  const content = zoomContentRef.value
  if (!content) return
  zoomResizeObserver = new ResizeObserver(syncZoomHeight)
  zoomResizeObserver.observe(content)
  syncZoomHeight()
}

watch(
  display,
  () => {
    localStorage.setItem(DISPLAY_SETTINGS_KEY, JSON.stringify(display))
  },
  { deep: true }
)

watch(
  payload,
  async () => {
    await nextTick()
    observeZoomContent()
  },
  { flush: 'post' }
)

const title = computed(() => payload.value?.title || '模型回复')
const tableModeLabel = computed(() =>
  'wrap' === chatView.tableMode ? '表格自动换行' : '表格不换行'
)
const tableModeShortLabel = computed(() => ('wrap' === chatView.tableMode ? '换行' : '不换行'))

function errorText(error: unknown): string {
  return error instanceof Error ? error.message : String(error)
}

function filenameStem(): string {
  const now = new Date()
  const part = (value: number): string => String(value).padStart(2, '0')
  return `模型回复-${now.getFullYear()}${part(now.getMonth() + 1)}${part(now.getDate())}-${part(now.getHours())}${part(now.getMinutes())}${part(now.getSeconds())}`
}

async function copyContent(): Promise<void> {
  const content = payload.value?.content ?? ''
  if (!content) return
  try {
    await navigator.clipboard.writeText(content)
    copied.value = true
    window.setTimeout(() => (copied.value = false), 1500)
  } catch {
    ElMessage.error('复制失败')
  }
}

async function printPreview(): Promise<void> {
  if (!payload.value || !markdownRef.value || printing.value) return
  printing.value = true
  try {
    const html = await markdownRef.value.buildExportHtml()
    const result = await window.ipc.messageExport.print({
      title: title.value,
      html
    })
    if (result.printed) ElMessage.success('已提交打印任务')
  } catch (error) {
    ElMessage.error(`打印失败：${errorText(error)}`)
  } finally {
    printing.value = false
  }
}

function setTableDisplayMode(value: string | number | object): void {
  chatView.setTableMode(String(value))
}

function resetDisplaySettings(): void {
  Object.assign(display, DEFAULT_DISPLAY_SETTINGS)
}

function stepDisplaySetting(
  key: keyof DisplaySettings,
  delta: number,
  min: number,
  max: number,
  precision = 0
): void {
  const next = Math.min(max, Math.max(min, Number(display[key]) + delta))
  display[key] = Number(next.toFixed(precision))
}

/** Ctrl + 滚轮：向上放大、向下缩小；普通滚轮仍保持页面滚动。 */
function zoomByWheel(event: WheelEvent): void {
  if (!event.ctrlKey || !event.deltaY || !payload.value) return
  event.preventDefault()
  stepDisplaySetting('zoom', event.deltaY < 0 ? 5 : -5, 75, 200)
}

async function download(format: string | number | object): Promise<void> {
  if (exporting.value || !payload.value || !markdownRef.value) return
  const kind: MessageExportFormat = ['pdf', 'markdown', 'txt'].includes(String(format))
    ? (String(format) as MessageExportFormat)
    : 'docx'
  exporting.value = true
  try {
    const isDocument = 'docx' === kind || 'pdf' === kind
    const content =
      'markdown' === kind
        ? payload.value.content
        : 'txt' === kind
          ? await markdownRef.value.buildPlainText()
          : ''
    const html = isDocument ? await markdownRef.value.buildExportHtml() : ''
    const result = await window.ipc.messageExport.save(
      isDocument
        ? {
            format: kind,
            filename: filenameStem(),
            title: title.value,
            html
          }
        : {
            format: kind,
            filename: filenameStem(),
            title: title.value,
            content
          }
    )
    if (result.path) ElMessage.success(`已导出到 ${result.path}`)
  } catch (error) {
    ElMessage.error(`导出失败：${errorText(error)}`)
  } finally {
    exporting.value = false
  }
}

onMounted(async () => {
  try {
    payload.value = windowId ? await window.ipc.messageWindow.load(windowId) : null
    if (!payload.value) throw new Error('预览内容不存在或窗口已失效')
    document.title = `${title.value} - fs-bot`
    await nextTick()
  } catch (error) {
    ElMessage.error(errorText(error))
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(() => {
  zoomResizeObserver?.disconnect()
  zoomResizeObserver = null
})
</script>

<template>
  <div class="message-preview-window" @wheel.ctrl.prevent="zoomByWheel">
    <header class="preview-toolbar">
      <div class="preview-toolbar__top">
        <div class="preview-heading">
          <span class="preview-mark" aria-hidden="true">
            <svg viewBox="0 0 20 20">
              <path
                d="M5.2 2.8h6.2l3.4 3.4v9.9a1.1 1.1 0 0 1-1.1 1.1H5.2a1.1 1.1 0 0 1-1.1-1.1V3.9a1.1 1.1 0 0 1 1.1-1.1z"
                fill="none"
                stroke="currentColor"
                stroke-width="1.35"
                stroke-linejoin="round"
              />
              <path
                d="M11.2 2.9v3.5h3.5M6.8 9.1h5.3M6.8 12h6.1M6.8 14.8h3.8"
                fill="none"
                stroke="currentColor"
                stroke-width="1.3"
                stroke-linecap="round"
              />
            </svg>
          </span>
          <span class="preview-title">
            <span class="preview-eyebrow">模型回复预览</span>
            <strong>{{ title }}</strong>
          </span>
        </div>
        <div class="preview-actions" role="toolbar" aria-label="回复预览操作">
          <el-button class="preview-control" :disabled="!payload" @click="copyContent">
            <svg v-if="copied" class="control-icon" viewBox="0 0 16 16" aria-hidden="true">
              <path
                d="m3.4 8.2 3 3 6.2-6.4"
                fill="none"
                stroke="currentColor"
                stroke-width="1.5"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
            <svg v-else class="control-icon" viewBox="0 0 16 16" aria-hidden="true">
              <rect
                x="5.8"
                y="5.8"
                width="7.6"
                height="7.6"
                rx="1.6"
                fill="none"
                stroke="currentColor"
                stroke-width="1.2"
              />
              <path
                d="M10.3 3.8H4.8A1.6 1.6 0 0 0 3.2 5.4v5.5"
                fill="none"
                stroke="currentColor"
                stroke-width="1.2"
                stroke-linecap="round"
              />
            </svg>
            <span class="control-label">{{ copied ? '已复制' : '复制' }}</span>
          </el-button>
          <el-button
            class="preview-control"
            :disabled="!payload || printing"
            :loading="printing"
            title="打印"
            @click="printPreview"
          >
            <svg class="control-icon" viewBox="0 0 16 16" aria-hidden="true">
              <path
                d="M4.3 5.3V2.6h7.4v2.7M4.3 11.3H3.1a1.1 1.1 0 0 1-1.1-1.1V6.6a1.1 1.1 0 0 1 1.1-1.1h9.8a1.1 1.1 0 0 1 1.1 1.1v3.6a1.1 1.1 0 0 1-1.1 1.1h-1.2"
                fill="none"
                stroke="currentColor"
                stroke-width="1.2"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
              <rect
                x="4.3"
                y="9.2"
                width="7.4"
                height="4.2"
                rx="0.7"
                fill="none"
                stroke="currentColor"
                stroke-width="1.2"
              />
              <path
                d="M11.8 7h.1"
                stroke="currentColor"
                stroke-width="1.6"
                stroke-linecap="round"
              />
            </svg>
            <span class="control-label">{{ printing ? '准备中' : '打印' }}</span>
          </el-button>
          <el-dropdown trigger="click" :disabled="!payload || exporting" @command="download">
            <el-button class="preview-download" type="primary" :loading="exporting">
              <svg class="control-icon" viewBox="0 0 16 16" aria-hidden="true">
                <path
                  d="M8 2.4v7.2m0 0L5.2 6.8M8 9.6l2.8-2.8M3.2 11.4v1.1a1.1 1.1 0 0 0 1.1 1.1h7.4a1.1 1.1 0 0 0 1.1-1.1v-1.1"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.3"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
              </svg>
              <span>{{ exporting ? '导出中' : '下载' }}</span>
              <svg class="control-caret" viewBox="0 0 12 12" aria-hidden="true">
                <path
                  d="m3.2 4.7 2.8 2.9 2.8-2.9"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.25"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
              </svg>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="pdf">PDF 文档 (.pdf)</el-dropdown-item>
                <el-dropdown-item command="docx">Word 文档 (.docx)</el-dropdown-item>
                <el-dropdown-item command="txt">纯文本 (.txt)</el-dropdown-item>
                <el-dropdown-item command="markdown">Markdown 文档 (.md)</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <div class="preview-formatbar" role="toolbar" aria-label="显示调整">
        <span class="formatbar-caption">显示</span>
        <div class="format-stepper" title="缩放，Ctrl + 滚轮可快速调整">
          <span class="format-stepper__label">缩放</span>
          <button
            type="button"
            class="format-step"
            :disabled="display.zoom <= 75"
            aria-label="缩小内容"
            @click="stepDisplaySetting('zoom', -5, 75, 200)"
          >
            −
          </button>
          <output>{{ display.zoom }}%</output>
          <button
            type="button"
            class="format-step"
            :disabled="display.zoom >= 200"
            aria-label="放大内容"
            @click="stepDisplaySetting('zoom', 5, 75, 200)"
          >
            +
          </button>
        </div>
        <div class="format-stepper">
          <span class="format-stepper__label">字号</span>
          <button
            type="button"
            class="format-step"
            :disabled="display.fontSize <= 13"
            aria-label="减小字号"
            @click="stepDisplaySetting('fontSize', -1, 13, 24)"
          >
            −
          </button>
          <output>{{ display.fontSize }} px</output>
          <button
            type="button"
            class="format-step"
            :disabled="display.fontSize >= 24"
            aria-label="增大字号"
            @click="stepDisplaySetting('fontSize', 1, 13, 24)"
          >
            +
          </button>
        </div>
        <div class="format-stepper">
          <span class="format-stepper__label">行距</span>
          <button
            type="button"
            class="format-step"
            :disabled="display.lineHeight <= 1.4"
            aria-label="减小行距"
            @click="stepDisplaySetting('lineHeight', -0.1, 1.4, 2.4, 1)"
          >
            −
          </button>
          <output>{{ display.lineHeight.toFixed(1) }}</output>
          <button
            type="button"
            class="format-step"
            :disabled="display.lineHeight >= 2.4"
            aria-label="增大行距"
            @click="stepDisplaySetting('lineHeight', 0.1, 1.4, 2.4, 1)"
          >
            +
          </button>
        </div>
        <el-dropdown
          trigger="click"
          popper-class="preview-table-menu"
          @command="setTableDisplayMode"
        >
          <button type="button" class="format-select-button" :title="tableModeLabel">
            <svg class="control-icon" viewBox="0 0 16 16" aria-hidden="true">
              <rect
                x="2.2"
                y="3.2"
                width="11.6"
                height="9.6"
                rx="1"
                fill="none"
                stroke="currentColor"
                stroke-width="1.2"
              />
              <path
                d="M2.2 6.4h11.6M2.2 9.7h11.6M6.2 3.2v9.6M10.2 3.2v9.6"
                fill="none"
                stroke="currentColor"
                stroke-width="1.1"
              />
            </svg>
            <span>{{ tableModeShortLabel }}</span>
            <svg class="control-caret" viewBox="0 0 12 12" aria-hidden="true">
              <path
                d="m3.2 4.7 2.8 2.9 2.8-2.9"
                fill="none"
                stroke="currentColor"
                stroke-width="1.25"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item
                command="wrap"
                role="menuitemradio"
                :aria-checked="'wrap' === chatView.tableMode"
              >
                <span class="table-mode-option">
                  <span class="table-mode-option__copy">
                    <strong>自动换行</strong>
                    <small>按窗口宽度折行显示列内容</small>
                  </span>
                  <svg
                    v-if="'wrap' === chatView.tableMode"
                    class="option-check"
                    viewBox="0 0 16 16"
                    aria-hidden="true"
                  >
                    <path
                      d="m3.4 8.2 3 3 6.2-6.4"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="1.5"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                    />
                  </svg>
                </span>
              </el-dropdown-item>
              <el-dropdown-item
                command="nowrap"
                role="menuitemradio"
                :aria-checked="'nowrap' === chatView.tableMode"
              >
                <span class="table-mode-option">
                  <span class="table-mode-option__copy">
                    <strong>不换行</strong>
                    <small>保持表格布局，横向滚动查看</small>
                  </span>
                  <svg
                    v-if="'nowrap' === chatView.tableMode"
                    class="option-check"
                    viewBox="0 0 16 16"
                    aria-hidden="true"
                  >
                    <path
                      d="m3.4 8.2 3 3 6.2-6.4"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="1.5"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                    />
                  </svg>
                </span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <button
          type="button"
          class="format-reset"
          title="恢复默认显示设置"
          aria-label="恢复默认显示设置"
          @click="resetDisplaySettings"
        >
          <svg viewBox="0 0 16 16" aria-hidden="true">
            <path
              d="M12.6 5.1A5.2 5.2 0 1 0 13 9.3M12.6 5.1V2.7M12.6 5.1H10.2"
              fill="none"
              stroke="currentColor"
              stroke-width="1.25"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
        </button>
      </div>
    </header>

    <el-scrollbar class="preview-scroll">
      <main class="preview-stage">
        <article v-if="payload" class="preview-paper">
          <div class="preview-scale-viewport" :style="zoomViewportStyle">
            <div ref="zoomContentRef" class="preview-scale-content" :style="zoomContentStyle">
              <ChatMarkdown
                ref="markdownRef"
                :content="payload.content"
                :project-id="payload.projectId"
                :conversation-id="payload.conversationId"
                :complete="true"
                :font-size="display.fontSize"
                :line-height="display.lineHeight"
                large
              />
            </div>
          </div>
        </article>
        <div v-else-if="loading" class="preview-loading" aria-label="正在加载回复">
          <span class="skeleton-line skeleton-title" />
          <span class="skeleton-line" />
          <span class="skeleton-line skeleton-medium" />
          <span class="skeleton-block" />
        </div>
        <div v-else class="preview-empty">
          <span class="preview-empty__icon" aria-hidden="true">!</span>
          <strong>无法加载这条回复</strong>
          <p>预览内容可能已失效，请返回对话重新打开。</p>
        </div>
      </main>
    </el-scrollbar>
  </div>
</template>

<style lang="scss" scoped>
.message-preview-window {
  --preview-toolbar-height: 50px;
  display: flex;
  flex-direction: column;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  color: var(--el-text-color-primary);
  background:
    radial-gradient(circle at 12% -8%, rgb(64 158 255 / 10%), transparent 30%),
    linear-gradient(180deg, #f5f7fa 0%, #f0f2f5 100%);
}

.preview-toolbar {
  z-index: 2;
  display: grid;
  grid-template-columns: minmax(0, 1fr) max-content minmax(0, 1fr);
  flex: none;
  align-items: center;
  gap: 12px;
  min-height: var(--preview-toolbar-height);
  padding: 7px clamp(16px, 2vw, 24px);
  overflow-x: auto;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: color-mix(in srgb, var(--el-bg-color) 96%, transparent);
  box-shadow: 0 1px 4px rgb(31 35 41 / 4%);
  backdrop-filter: blur(18px);
  scrollbar-width: none;

  &::-webkit-scrollbar {
    display: none;
  }
}

.preview-toolbar__top {
  display: contents;
}

.preview-heading {
  display: flex;
  flex: none;
  align-items: center;
  min-width: 0;
  order: 1;
  gap: 7px;
  justify-self: start;
}

.preview-mark {
  display: grid;
  flex: none;
  width: 30px;
  height: 30px;
  place-items: center;
  color: var(--el-color-primary);
  background: transparent;

  svg {
    width: 17px;
    height: 17px;
  }
}

.preview-title {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 0;

  .preview-eyebrow {
    display: none;
  }

  strong {
    overflow: hidden;
    max-width: 180px;
    font-size: 13px;
    font-weight: 600;
    line-height: 1.4;
    white-space: nowrap;
    text-overflow: ellipsis;
    color: var(--el-text-color-primary);
  }
}

.preview-actions {
  display: flex;
  flex: none;
  align-items: center;
  order: 3;
  gap: 6px;
  justify-self: end;

  :deep(.el-dropdown),
  :deep(.el-button) {
    margin-left: 0;
  }

  :deep(.el-button) {
    display: inline-flex;
    height: 30px;
    gap: 5px;
    align-items: center;
    justify-content: center;
    padding: 0 9px;
    border-radius: 6px;
    border-color: transparent;
    background-color: transparent;
    font-size: 12.5px;
    font-weight: 500;
    line-height: 1;
  }

  :deep(.el-button > span) {
    display: inline-flex;
    height: 14px;
    align-items: center;
    justify-content: center;
    gap: 6px;
    line-height: 14px;
    white-space: nowrap;
  }

  :deep(.el-button:not(.is-disabled):not(.el-button--primary):hover) {
    border-color: var(--el-color-primary-light-5);
    color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
  }

  :deep(.preview-download.el-button--primary) {
    border-color: var(--el-color-primary);
    color: var(--el-color-white);
    background: var(--el-color-primary);
  }

  :deep(.preview-download.el-button--primary:hover) {
    border-color: var(--el-color-primary-light-3);
    color: var(--el-color-white);
    background: var(--el-color-primary-light-3);
  }
}

.control-icon {
  display: block;
  flex: none;
  width: 14px;
  height: 14px;
  align-self: center;
}

.control-caret {
  display: block;
  flex: none;
  width: 11px;
  height: 11px;
  margin-left: -1px;
  align-self: center;
  opacity: 0.7;
}

.control-label {
  display: inline-flex;
  height: 14px;
  align-items: center;
  line-height: 14px;
}

.preview-download {
  box-shadow: none;

  &:hover {
    box-shadow: none;
  }
}

.preview-formatbar {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  order: 2;
  min-width: max-content;
  gap: 3px;
  padding: 0;
  justify-self: center;

  > * {
    flex: none;
  }
}

.formatbar-caption {
  display: none;
}

.format-stepper {
  display: inline-flex;
  height: 28px;
  align-items: center;
  overflow: hidden;
  border: 0;
  border-radius: 5px;
  background: transparent;

  output {
    display: inline-flex;
    min-width: 42px;
    height: 100%;
    align-items: center;
    justify-content: center;
    font-size: 11px;
    font-variant-numeric: tabular-nums;
    line-height: 14px;
    color: var(--el-text-color-primary);
  }
}

.format-stepper__label {
  display: inline-flex;
  height: 100%;
  align-items: center;
  padding: 0 5px;
  font-size: 11px;
  line-height: 14px;
  color: var(--el-text-color-secondary);
  background: transparent;
}

.format-step {
  display: grid;
  width: 22px;
  height: 100%;
  padding: 0;
  place-items: center;
  border: 0;
  color: var(--el-text-color-secondary);
  background: transparent;
  font: inherit;
  font-size: 14px;
  line-height: 1;
  cursor: pointer;
  transition:
    color 0.15s,
    background-color 0.15s;

  &:hover:not(:disabled) {
    color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
  }

  &:disabled {
    color: var(--el-text-color-disabled);
    cursor: default;
  }
}

.format-select-button {
  display: inline-flex;
  height: 28px;
  align-items: center;
  gap: 5px;
  padding: 0 8px;
  border: 0;
  border-radius: 5px;
  color: var(--el-text-color-regular);
  background: transparent;
  font-size: 11.5px;
  line-height: 14px;
  white-space: nowrap;
  cursor: pointer;
  transition:
    color 0.15s,
    border-color 0.15s,
    background-color 0.15s;

  &:hover {
    color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
  }
}

.format-reset {
  display: grid;
  width: 28px;
  height: 28px;
  padding: 0;
  place-items: center;
  border: 0;
  color: var(--el-text-color-placeholder);
  background: transparent;
  cursor: pointer;

  &:hover {
    color: var(--el-color-primary);
  }

  svg {
    width: 15px;
    height: 15px;
  }
}

.preview-scroll {
  flex: 1 1 auto;
  min-height: 0;
  background: transparent;
}

.preview-stage {
  box-sizing: border-box;
  width: 100%;
  min-height: 100%;
  padding: 24px clamp(18px, 2.5vw, 32px) 40px;
}

.preview-scale-viewport {
  position: relative;
  width: 100%;
  min-height: 1px;
}

.preview-scale-content {
  transform-origin: top left;
  will-change: transform;
}

.preview-paper {
  box-sizing: border-box;
  width: 100%;
  max-width: none;
  min-height: calc(100vh - var(--preview-toolbar-height) - 64px);
  margin: 0;
  padding: clamp(30px, 3.4vw, 52px) clamp(28px, 4.2vw, 64px) 72px;
  border: 1px solid color-mix(in srgb, var(--el-border-color) 38%, transparent);
  border-radius: 18px;
  background: var(--el-bg-color);
  box-shadow:
    0 1px 0 rgb(255 255 255 / 70%),
    0 14px 44px rgb(31 35 41 / 7%);
}

.preview-loading {
  box-sizing: border-box;
  width: 100%;
  min-height: calc(100vh - var(--preview-toolbar-height) - 64px);
  padding: 48px clamp(28px, 4.2vw, 64px);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 18px;
  background: var(--el-bg-color);
}

.skeleton-line,
.skeleton-block {
  display: block;
  border-radius: 6px;
  background: linear-gradient(
    90deg,
    var(--el-fill-color-light) 20%,
    var(--el-fill-color-lighter) 38%,
    var(--el-fill-color-light) 56%
  );
  background-size: 220% 100%;
  animation: preview-loading 1.5s ease-in-out infinite;
}

.skeleton-line {
  width: 100%;
  height: 12px;
  margin-bottom: 13px;
}

.skeleton-title {
  width: min(320px, 46%);
  height: 22px;
  margin-bottom: 28px;
}

.skeleton-medium {
  width: 72%;
}

.skeleton-block {
  width: 100%;
  height: 180px;
  margin-top: 30px;
  border-radius: 10px;
}

.preview-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  box-sizing: border-box;
  width: 100%;
  min-height: 52vh;
  padding: 48px 24px;
  border: 1px dashed var(--el-border-color);
  border-radius: 18px;
  text-align: center;
  background: color-mix(in srgb, var(--el-bg-color) 84%, transparent);

  .preview-empty__icon {
    display: grid;
    width: 42px;
    height: 42px;
    margin-bottom: 14px;
    place-items: center;
    border: 1px solid var(--el-border-color);
    border-radius: 50%;
    color: var(--el-text-color-secondary);
    background: var(--el-fill-color-light);
    font-size: 18px;
    font-weight: 600;
  }

  strong {
    font-size: 15px;
    font-weight: 600;
    color: var(--el-text-color-regular);
  }

  p {
    margin-top: 6px;
    font-size: 13px;
    color: var(--el-text-color-placeholder);
  }
}

:global(.preview-table-menu .el-dropdown-menu) {
  min-width: 246px;
  padding: 6px;
}

:global(.preview-table-menu .el-dropdown-menu__item) {
  min-height: 48px;
  margin: 2px 0;
  padding: 8px 10px;
  border-radius: 8px;
}

.table-mode-option {
  display: flex;
  width: 100%;
  min-width: 0;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.table-mode-option__copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 1px;
  text-align: left;

  strong {
    font-size: 13px;
    font-weight: 500;
    line-height: 1.45;
  }

  small {
    font-size: 11px;
    line-height: 1.45;
    color: var(--el-text-color-placeholder);
  }
}

.option-check {
  flex: none;
  width: 15px;
  height: 15px;
  color: var(--el-color-primary);
}

@keyframes preview-loading {
  0% {
    background-position: 100% 0;
  }

  100% {
    background-position: -120% 0;
  }
}

@media (max-width: 900px) {
  .preview-title strong {
    max-width: 130px;
  }

  .preview-stage {
    padding: 18px 16px 30px;
  }

  .preview-paper,
  .preview-loading {
    padding-right: 30px;
    padding-left: 30px;
    border-radius: 14px;
  }
}

@media (max-width: 720px) {
  .preview-toolbar {
    padding: 7px 12px;
  }

  .preview-title strong {
    max-width: 90px;
  }

  .preview-mark {
    width: 28px;
    height: 28px;
  }

  .preview-stage {
    padding: 12px 10px 24px;
  }

  .preview-paper {
    padding: 26px 20px 48px;
    border-radius: 12px;
  }

  .preview-loading {
    padding: 32px 20px;
    border-radius: 12px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .skeleton-line,
  .skeleton-block {
    animation: none;
  }
}

@media print {
  :global(html),
  :global(body),
  :global(#app),
  :global(.app-content) {
    width: auto !important;
    height: auto !important;
    overflow: visible !important;
    background: #fff !important;
  }

  @page {
    size: A4;
    margin: 16mm 18mm 18mm;
  }

  .message-preview-window {
    display: block !important;
    width: auto !important;
    height: auto !important;
    overflow: visible !important;
    color: #303133 !important;
    background: #fff !important;
  }

  .preview-toolbar {
    display: none !important;
  }

  .preview-scroll {
    height: auto !important;
    overflow: visible !important;
  }

  :deep(.el-scrollbar__wrap) {
    height: auto !important;
    overflow: visible !important;
  }

  :deep(.el-scrollbar__view) {
    display: block !important;
  }

  .preview-stage {
    width: auto !important;
    min-height: 0 !important;
    padding: 0 !important;
  }

  .preview-paper {
    width: 100% !important;
    max-width: none !important;
    min-height: 0 !important;
    margin: 0 !important;
    padding: 0 !important;
    border: 0 !important;
    border-radius: 0 !important;
    box-shadow: none !important;
    background: #fff !important;
  }

  .preview-scale-viewport {
    height: auto !important;
  }

  .preview-scale-content {
    width: 100% !important;
    transform: none !important;
  }
}
</style>
