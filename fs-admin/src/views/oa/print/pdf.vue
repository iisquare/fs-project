<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElNotification } from 'element-plus'
import * as ElementPlusIcons from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import ApiUtil from '@/utils/ApiUtil'
import DateUtil from '@/utils/DateUtil'
import DocxUtil from '@/utils/DocxUtil'
import PrintApi from '@/api/oa/PrintApi'
import FormDataApi from '@/api/oa/FormDataApi'
import DocxPreview from '@/views/oa/components/DocxPreview.vue'
import LayoutBack from '@/components/Layout/LayoutBack.vue'

interface PrintFieldNode {
  id?: string
  field: string
  label: string
  path?: string
  component?: string
  type: string
  list: boolean
  required?: boolean
  general?: boolean
  children?: PrintFieldNode[]
}

const route = useRoute()
const router = useRouter()
const previewRef = ref<InstanceType<typeof DocxPreview>>()
const loading = ref(false)
const rendering = ref(false)
const exporting = ref(false)
const exportingPdf = ref(false)
const info = ref<any>({ id: 0, name: '', frameId: null })
const records = ref<any[]>([])
const dataId = ref(String(route.query.dataId || ''))
const rendered = ref<any>()
const placeholders = ref<any[]>([])
const unresolved = ref<string[]>([])
const outline = ref<any[]>([])
const printFields = ref<PrintFieldNode[]>([])
const recordFilterKeyword = ref('')
const recordSelectLoading = ref(false)
const recordPage = ref(1)
const recordPageSize = 20
const recordTotal = ref(0)
const selectedRecord = ref<any>()
const outlineVisible = ref(false)
const renderError = ref('')
let renderRequestSerial = 0

const outlineTree = computed(() => {
  const roots: any[] = []
  const stack: any[] = []
  for (const [index, item] of outline.value.entries()) {
    const node = { ...item, key: `${item.name}-${item.repeatIndex || 0}-${index}`, subCatalog: [] }
    const level = Number(item.level || 1)
    while (stack.length && Number(stack[stack.length - 1].level || 1) >= level) stack.pop()
    if (stack.length) stack[stack.length - 1].subCatalog.push(node)
    else roots.push(node)
    stack.push(node)
  }
  return roots
})

const backTo = computed(() => {
  const returnTo = String(route.query.returnTo || '')
  if (returnTo) return returnTo
  return info.value.id
    ? { path: '/oa/print/model', query: { id: info.value.id } }
    : { path: '/oa/print/list' }
})

const loadRecordById = async () => {
  if (!dataId.value) return
  try {
    const result: any = await FormDataApi.list({
      _id: dataId.value,
      frameId: info.value.frameId,
      page: 1,
      pageSize: 1,
      sort: 'createdTime.desc',
    })
    const record = ApiUtil.data(result)?.rows?.[0]
    if (record) selectedRecord.value = record
  } catch {
    selectedRecord.value = undefined
  }
}

const loadRecords = async (page = 1) => {
  if (!info.value.frameId) {
    records.value = []
    recordTotal.value = 0
    return
  }
  recordSelectLoading.value = true
  try {
    const result: any = await FormDataApi.list({
      frameId: info.value.frameId,
      page,
      pageSize: recordPageSize,
      sort: 'createdTime.desc',
    })
    const data = ApiUtil.data(result)
    records.value = data?.rows || []
    recordPage.value = Number(data?.page || page)
    recordTotal.value = Number(data?.total || records.value.length)
    if (dataId.value) {
      const current = records.value.find(record => record._id === dataId.value)
      if (current) selectedRecord.value = current
      else if (!selectedRecord.value || selectedRecord.value._id !== dataId.value) {
        await loadRecordById()
      }
    }
  } catch {
    records.value = []
    recordTotal.value = 0
  } finally {
    recordSelectLoading.value = false
  }
}

const changeRecordPage = (offset: number) => {
  const next = recordPage.value + offset
  if (next < 1 || next * recordPageSize - recordPageSize >= recordTotal.value) return
  loadRecords(next)
}

const loadPrintFields = () => {
  if (!info.value.id) return Promise.resolve()
  return PrintApi.fields({
    id: info.value.id,
    frameId: info.value.frameId,
  }).then((result: any) => {
    printFields.value = ApiUtil.data(result) || []
  }).catch(() => {})
}

const flattenFields = (nodes: PrintFieldNode[], depth = 0): Array<PrintFieldNode & { depth: number }> => {
  const result: Array<PrintFieldNode & { depth: number }> = []
  for (const node of nodes) {
    result.push({ ...node, depth })
    if (node.children?.length) result.push(...flattenFields(node.children, depth + 1))
  }
  return result
}

const summaryFields = computed(() => {
  return flattenFields(printFields.value)
    .filter(item => item.depth === 0 && !item.list && !item.general && item.type !== 'password')
    .sort((left, right) => Number(Boolean(right.required)) - Number(Boolean(left.required)))
})

const formatFieldValue = (value: any): string => {
  if (value === null || value === undefined || value === '') return ''
  if (Array.isArray(value)) return value.map(item => formatFieldValue(item)).filter(Boolean).join('、')
  if (typeof value === 'boolean') return value ? '是' : '否'
  if (typeof value === 'object') return Object.values(value).map(item => formatFieldValue(item)).filter(Boolean).join('、')
  const text = String(value)
  return text.length > 36 ? `${text.slice(0, 36)}…` : text
}

const recordSummary = (record: any) => {
  const content = record?.content || {}
  return summaryFields.value
    .map(item => ({
      label: item.label,
      value: formatFieldValue(content[item.id || item.field] ?? content[item.field]),
    }))
    .filter(item => item.value)
    .slice(0, 4)
}

const recordTitle = (record: any) => {
  const first = recordSummary(record)[0]
  return first ? `${first.label}：${first.value}` : `记录 ${String(record?._id || '').slice(-8)}`
}

const recordTimeline = (record: any) => {
  const createdBy = record?.bpmStartUserIdName || record?.createdUidName || ''
  const updatedBy = record?.updatedUidName || ''
  return [
    createdBy ? `发起/创建：${createdBy}` : '',
    record?.createdTime ? `创建：${DateUtil.format(record.createdTime)}` : '',
    updatedBy ? `更新：${updatedBy}` : '',
    record?.updatedTime ? `修改：${DateUtil.format(record.updatedTime)}` : '',
  ].filter(Boolean)
}

const recordSearchText = (record: any) => {
  return [
    record?._id,
    record?.bpmStartUserIdName,
    record?.createdUidName,
    record?.updatedUidName,
    record?.bpmWorkflowIdName,
    record?.bpmInstanceId,
    DateUtil.format(record?.createdTime),
    DateUtil.format(record?.updatedTime),
    ...recordSummary(record).map(item => `${item.label} ${item.value}`),
  ].join(' ').toLowerCase()
}

const visibleRecords = computed(() => {
  const keyword = recordFilterKeyword.value.trim().toLowerCase()
  if (!keyword) return records.value
  return records.value.filter(record => recordSearchText(record).includes(keyword))
})

const recordOptions = computed(() => {
  if (!selectedRecord.value) return visibleRecords.value
  if (visibleRecords.value.some(record => record._id === selectedRecord.value._id)) {
    return visibleRecords.value
  }
  return [selectedRecord.value, ...visibleRecords.value]
})

const canGoPreviousRecordPage = computed(() => recordPage.value > 1)
const canGoNextRecordPage = computed(() => recordPage.value * recordPageSize < recordTotal.value)

const selectedRecordTitle = computed(() => {
  if (!selectedRecord.value) return ''
  return recordTitle(selectedRecord.value)
})

const filterRecords = (keyword: string) => {
  recordFilterKeyword.value = keyword || ''
}

const handleSelectVisible = (visible: boolean) => {
  if (!visible) {
    recordFilterKeyword.value = ''
    return
  }
  loadRecords(recordPage.value)
}

const renderTemplate = async (sample = !dataId.value) => {
  if (!info.value.id) return
  const serial = ++renderRequestSerial
  rendering.value = true
  renderError.value = ''
  try {
    const result: any = await PrintApi.render({
      id: info.value.id,
      dataId: dataId.value || undefined,
      sample,
    })
    if (serial !== renderRequestSerial) return
    const nextRendered = ApiUtil.data(result)
    await previewRef.value?.render(DocxUtil.base64ToArrayBuffer(nextRendered.content))
    if (serial !== renderRequestSerial) return
    rendered.value = nextRendered
    placeholders.value = rendered.value?.placeholders || []
    outline.value = rendered.value?.outline || []
    unresolved.value = rendered.value?.unresolved || []
    if (unresolved.value.length) {
      ElNotification({
        title: '存在未匹配的占位符',
        message: unresolved.value.join('、'),
        type: 'warning',
        duration: 6000,
      })
    }
  } catch (e: any) {
    if (serial !== renderRequestSerial) return
    rendered.value = undefined
    placeholders.value = []
    outline.value = []
    unresolved.value = []
    renderError.value = e?.message || '请检查模板或表单数据'
    previewRef.value?.clear()
    ElNotification({ title: '模板渲染失败', message: e?.message || '请检查模板或表单数据', type: 'error' })
  } finally {
    if (serial === renderRequestSerial) rendering.value = false
  }
}

onMounted(async () => {
  const id = Number(route.query.id || 0)
  if (!id) {
    ElNotification({ title: '无法预览', message: '缺少打印模板标识', type: 'warning' })
    return
  }
  loading.value = true
  try {
    const result: any = await PrintApi.info({ id })
    info.value = ApiUtil.data(result)
    await loadPrintFields()
    await loadRecords(1)
    if (dataId.value && !selectedRecord.value) await loadRecordById()
    await renderTemplate(!dataId.value)
  } catch (e: any) {
    ElNotification({ title: '加载失败', message: e?.message || '打印模板不存在', type: 'error' })
  } finally {
    loading.value = false
  }
})

const handleDataChange = async (value?: string) => {
  dataId.value = value || ''
  recordFilterKeyword.value = ''
  selectedRecord.value = records.value.find(record => record._id === dataId.value)
    || (selectedRecord.value?._id === dataId.value ? selectedRecord.value : undefined)
  if (dataId.value && !selectedRecord.value) await loadRecordById()
  return renderTemplate(!value)
}

const exportResult = () => {
  if (!rendered.value?.content) return
  exporting.value = true
  try {
    const blob = DocxUtil.base64ToBlob(rendered.value.content, rendered.value.mime)
    DocxUtil.download(blob, rendered.value.fileName || `${info.value.name || '打印结果'}.docx`)
  } finally {
    exporting.value = false
  }
}

const printResult = () => {
  window.print()
}

const editTemplate = () => {
  router.push({ path: '/oa/print/model', query: { id: info.value.id } })
}

const goToOutline = (item: any) => {
  previewRef.value?.scrollToOutline(item.name, Number(item.repeatIndex || 0))
}

const exportPdf = async () => {
  if (exportingPdf.value || !rendered.value) return
  exportingPdf.value = true
  try {
    const fileName = (rendered.value.fileName || `${info.value.name || '打印结果'}.docx`).replace(/\.docx$/i, '.pdf')
    await previewRef.value?.exportPdf(fileName)
  } catch (e: any) {
    ElNotification({ title: 'PDF 导出失败', message: e?.message || '请稍后重试', type: 'error' })
  } finally {
    exportingPdf.value = false
  }
}

</script>

<template>
  <div class="print-preview">
    <header class="print-preview__header">
      <div class="print-preview__lead">
        <LayoutBack :to="backTo" title="返回" icon="ArrowLeft" />
        <div class="print-preview__heading">
          <div class="print-preview__title">{{ info.name || '打印预览' }}</div>
          <div class="print-preview__meta">
            <span v-if="Number(info.status) === 2" class="is-warning">模板已禁用，仅示例预览</span>
            <span v-else-if="renderError" class="is-error">预览失败</span>
            <span v-else-if="rendering">正在生成预览…</span>
            <span v-else-if="selectedRecord">已套用：{{ selectedRecordTitle }}</span>
            <span v-else>当前为示例数据预览</span>
          </div>
        </div>
      </div>
      <div class="print-preview__selection">
        <el-select
          v-if="info.frameId"
          v-model="dataId"
          clearable
          filterable
          :disabled="Number(info.status) === 2"
          remote
          :remote-method="filterRecords"
          :loading="recordSelectLoading"
          placeholder="选择表单数据（可选）"
          popper-class="print-data-select-popper"
          class="print-preview__data-select"
          @change="handleDataChange"
          @visible-change="handleSelectVisible"
        >
          <el-option
            v-for="record in recordOptions"
            :key="record._id"
            :value="record._id"
            :label="`${recordTitle(record)} · ${DateUtil.format(record.createdTime)}`"
          >
            <div class="print-preview__data-option">
              <div class="print-preview__data-option-head">
                <span class="print-preview__data-option-title">{{ recordTitle(record) }}</span>
                <span class="print-preview__data-option-id">{{ record._id }}</span>
              </div>
              <div v-if="recordSummary(record).length" class="print-preview__data-option-summary">
                <span v-for="item in recordSummary(record)" :key="item.label">
                  <b>{{ item.label }}</b>{{ item.value }}
                </span>
              </div>
              <div class="print-preview__data-option-meta">
                <span v-for="text in recordTimeline(record)" :key="text">{{ text }}</span>
                <span v-if="record.bpmWorkflowIdName">流程：{{ record.bpmWorkflowIdName }}</span>
              </div>
            </div>
          </el-option>
          <template #footer>
            <div class="print-preview__record-pager" @mousedown.stop>
              <span>共 {{ recordTotal }} 条 · 当前页内检索</span>
              <el-space :size="4">
                <el-button
                  text
                  size="small"
                  :disabled="!canGoPreviousRecordPage || recordSelectLoading"
                  @click.stop="changeRecordPage(-1)"
                >
                  上一页
                </el-button>
                <span>{{ recordPage }}</span>
                <el-button
                  text
                  size="small"
                  :disabled="!canGoNextRecordPage || recordSelectLoading"
                  @click.stop="changeRecordPage(1)"
                >
                  下一页
                </el-button>
              </el-space>
            </div>
          </template>
        </el-select>
        <el-button :icon="ElementPlusIcons.Refresh" :loading="rendering" @click="renderTemplate(!dataId)">
          刷新
        </el-button>
        <el-tooltip :content="outlineVisible ? '隐藏文档大纲' : '显示文档大纲'" placement="bottom">
          <el-button
            :type="outlineVisible ? 'primary' : 'default'"
            :icon="ElementPlusIcons.List"
            plain
            aria-label="切换文档大纲"
            @click="outlineVisible = !outlineVisible"
          />
        </el-tooltip>
      </div>
      <div class="print-preview__actions">
        <el-button :icon="ElementPlusIcons.Edit" @click="editTemplate">编辑模板</el-button>
        <el-button
          type="primary"
          :icon="ElementPlusIcons.Printer"
          :disabled="!rendered || rendering"
          @click="printResult"
        >
          打印
        </el-button>
        <el-dropdown trigger="click" :disabled="!rendered || rendering">
          <el-button :disabled="!rendered || rendering">
            下载
            <el-icon class="el-icon--right"><ElementPlusIcons.ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item :icon="ElementPlusIcons.Download" :disabled="exporting" @click="exportResult">
                下载 Word
              </el-dropdown-item>
              <el-dropdown-item :icon="ElementPlusIcons.Document" :disabled="exportingPdf" @click="exportPdf">
                下载 PDF
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <el-splitter class="print-preview__body">
      <el-splitter-panel
        v-if="outlineVisible"
        class="print-preview__outline-panel"
        size="240px"
        min="180px"
        max="340px"
        collapsible
      >
        <aside class="print-preview__side">
          <div class="print-preview__side-title">
            <span>文档大纲</span>
            <el-tag size="small" effect="plain">{{ outline.length }}</el-tag>
          </div>
          <el-scrollbar class="print-preview__outline">
            <el-tree
              v-if="outlineTree.length"
              :data="outlineTree"
              node-key="key"
              :props="{ label: 'name', children: 'subCatalog' }"
              :expand-on-click-node="false"
              default-expand-all
              class="outline-tree"
              @node-click="goToOutline"
            >
              <template #default="{ data }">
                <span class="outline-tree__name">{{ data.name }}</span>
              </template>
            </el-tree>
            <el-empty v-else description="文档中未检测到标题" :image-size="52" />
          </el-scrollbar>
          <div class="print-preview__status">
            <el-alert
              v-if="unresolved.length"
              type="warning"
              :closable="false"
              show-icon
              :title="`${unresolved.length} 个占位符未匹配：${unresolved.join('、')}`"
            />
            <div v-else-if="placeholders.length" class="print-preview__placeholder-count">
              已识别 {{ placeholders.length }} 个占位符
            </div>
          </div>
        </aside>
      </el-splitter-panel>
      <el-splitter-panel class="print-preview__content-panel" min="360px">
        <section
          class="print-preview__content"
          v-loading="loading || rendering"
          element-loading-text="正在生成打印预览…"
        >
          <div v-if="renderError && !rendering" class="print-preview__error">
            <el-empty :description="renderError" :image-size="72">
              <el-button type="primary" :icon="ElementPlusIcons.Refresh" @click="renderTemplate(!dataId)">
                重新生成
              </el-button>
            </el-empty>
          </div>
          <DocxPreview ref="previewRef" height="100%" />
        </section>
      </el-splitter-panel>
    </el-splitter>
  </div>
</template>

<style scoped lang="scss">
.print-preview {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
  background: var(--el-fill-color-light);
  &__header {
    display: flex;
    flex: 0 0 auto;
    min-height: 64px;
    padding: 10px 16px;
    gap: 12px 16px;
    flex-wrap: wrap;
    align-items: center;
    justify-content: space-between;
    background: var(--el-bg-color);
    border-bottom: 1px solid var(--el-border-color-lighter);
    box-shadow: 0 1px 4px rgb(28 37 51 / 6%);
  }
  &__lead,
  &__selection,
  &__actions {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  &__lead {
    min-width: 0;
    flex: 1 1 300px;
  }
  &__heading {
    min-width: 0;
  }
  &__selection {
    min-width: 0;
    flex: 1 1 500px;
    justify-content: center;
  }
  &__actions {
    flex: 0 1 auto;
    justify-content: flex-end;
  }
  &__title {
    overflow: hidden;
    font-size: 16px;
    font-weight: 600;
    line-height: 22px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  &__meta {
    overflow: hidden;
    font-size: 12px;
    line-height: 18px;
    color: var(--el-text-color-secondary);
    text-overflow: ellipsis;
    white-space: nowrap;
    .is-error {
      color: var(--el-color-danger);
    }
    .is-warning {
      color: var(--el-color-warning);
    }
  }
  &__body {
    flex: 1 1 auto;
    min-height: 0;
    height: 100%;
    :deep(.el-splitter-bar__dragger:hover:before) {
      width: 3px;
      background: var(--el-color-primary);
    }
  }
  &__side {
    display: flex;
    width: 100%;
    height: 100%;
    flex-direction: column;
    min-height: 0;
    overflow: hidden;
    background: var(--el-bg-color);
  }
  &__side-title {
    display: flex;
    flex: 0 0 44px;
    align-items: center;
    justify-content: space-between;
    padding: 0 14px;
    font-size: 13px;
    font-weight: 600;
    border-bottom: 1px solid var(--el-border-color-lighter);
  }
  &__outline {
    flex: 1 1 auto;
    min-height: 0;
  }
  &__status {
    flex: 0 0 auto;
    padding: 10px 12px;
    border-top: 1px solid var(--el-border-color-lighter);
  }
  &__placeholder-count {
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
  &__data-select {
    width: min(440px, 44vw);
    min-width: 260px;
  }
  &__record-pager {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 6px 12px;
    color: var(--el-text-color-secondary);
    font-size: 11px;
  }
  &__data-option {
    display: flex;
    flex-direction: column;
    gap: 4px;
    padding: 5px 0;
  }
  &__data-option-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
  }
  &__data-option-title {
    min-width: 0;
    overflow: hidden;
    color: var(--el-text-color-primary);
    font-size: 13px;
    font-weight: 600;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  &__data-option-id {
    flex: none;
    color: var(--el-text-color-placeholder);
    font-family: monospace;
    font-size: 10px;
  }
  &__data-option-summary {
    display: flex;
    flex-wrap: wrap;
    gap: 3px 10px;
    color: var(--el-text-color-regular);
    font-size: 12px;
    b {
      margin-right: 4px;
      color: var(--el-color-primary);
      font-weight: 500;
    }
  }
  &__data-option-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 2px 10px;
    color: var(--el-text-color-secondary);
    font-size: 11px;
    line-height: 16px;
  }
  &__content {
    position: relative;
    width: 100%;
    height: 100%;
    min-height: 0;
    overflow: hidden;
  }
  &__error {
    position: absolute;
    z-index: 3;
    inset: 0;
    display: grid;
    place-items: center;
    padding: 24px;
    background: var(--el-bg-color);
  }
}
.outline-tree {
  padding: 8px 6px;
  background: transparent;
  :deep(.el-tree-node__content) {
    height: 32px;
    border-radius: 4px;
  }
  &__name {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
:global(.print-data-select-popper .el-select-dropdown__item) {
  height: auto;
  min-height: 52px;
  padding: 7px 12px;
  line-height: normal;
}
:global(.print-data-select-popper .el-select-dropdown__item.is-selected) {
  color: inherit;
}
@media (max-width: 1180px) {
  .print-preview {
    &__lead {
      flex-basis: calc(50% - 8px);
    }
    &__actions {
      flex: 1 1 auto;
    }
    &__selection {
      order: 3;
      flex-basis: 100%;
      justify-content: flex-start;
    }
    &__data-select {
      width: min(520px, 100%);
    }
  }
}
@media (max-width: 720px) {
  .print-preview {
    &__header {
      padding: 10px 12px;
    }
    &__lead {
      flex-basis: 100%;
    }
    &__actions {
      flex-basis: 100%;
      justify-content: flex-end;
    }
    &__selection {
      justify-content: flex-end;
    }
    &__data-select {
      min-width: 0;
      flex: 1 1 auto;
    }
  }
}
@media print {
  .print-preview {
    height: auto;
    overflow: visible;
    background: #fff;
    &__header,
    &__outline-panel,
    :deep(.el-splitter-bar) {
      display: none !important;
    }
    &__body,
    &__content-panel,
    &__content {
      display: block;
      height: auto;
      overflow: visible;
    }
  }
}
</style>
