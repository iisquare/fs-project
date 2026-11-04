<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import type { UploadFile, UploadInstance } from 'element-plus'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import * as ElementPlusIcons from '@element-plus/icons-vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import ApiUtil from '@/utils/ApiUtil'
import DocxUtil from '@/utils/DocxUtil'
import DocxLayoutUtil from '@/utils/DocxLayoutUtil'
import FormUtil from '@/utils/FormUtil'
import PrintApi from '@/api/oa/PrintApi'
import FormFrameApi from '@/api/oa/FormFrameApi'
import CanvasDocumentEditor from '@/views/oa/components/CanvasDocumentEditor.vue'
import LayoutBack from '@/components/Layout/LayoutBack.vue'

interface FieldNode {
  field: string
  id?: string
  label: string
  path?: string
  component?: string
  type: string
  list: boolean
  required?: boolean
  multiple?: boolean
  placeholder?: string
  optionCount?: number
  general?: boolean
  children?: FieldNode[]
  depth?: number
  listParent?: string
}

interface PlaceholderNode {
  name: string
  raw: string
  type: string
  count?: number
  field?: FieldNode
  issue?: string
}

const PLACEHOLDER_TYPE_LABELS: Record<string, string> = {
  value: '单值',
  list: '列表开始',
  'list-end': '列表结束',
  row: '表格行字段',
  special: '特殊标签',
  current: '当前项',
}

const route = useRoute()
const router = useRouter()
const editorRef = ref<InstanceType<typeof CanvasDocumentEditor>>()
const loading = ref(false)
const saving = ref(false)
const exporting = ref(false)
const info = ref<any>({
  id: Number(route.query.id || 0),
  name: '',
  frameId: null,
  sort: 0,
  status: 1,
  description: '',
  hasTemplate: false,
  templateName: '',
  templateSize: 0,
})
const fields = ref<FieldNode[]>([])
const placeholders = ref<any[]>([])
const selectedFile = ref<File>()
const uploadRef = ref<UploadInstance>()
const editorDirty = ref(false)
const sideTab = ref('settings')
const outline = ref<any[]>([])
const fieldKeyword = ref('')
const placeholderKeyword = ref('')
const placeholderFilter = ref('all')
const activePlaceholderRaw = ref('')
const placeholderMatchInfo = ref<{ index: number, count: number } | null>(null)
const outlineVisible = ref(true)
const sideVisible = ref(true)
const helpVisible = ref(false)
const metadataBaseline = ref('')
let fieldsRequestSerial = 0

const USAGE_EXAMPLES = [
  {
    title: '单值变量',
    syntax: '{{申请人}}',
    description: '插入一个表单字段。变量名使用右侧“字段”列表中的名称，不建议直接填写表单控件的内部字段 ID。',
  },
  {
    title: '正文列表',
    syntax: '{{?采购明细}}\n物品：{{物品名称}}，数量：{{数量}}\n{{/采购明细}}',
    description: '列表开始和结束标签必须成对出现；中间每一行会按子表单记录重复生成。',
  },
  {
    title: '表格循环',
    syntax: '{{采购明细}}\n[物品名称]    [数量]',
    description: '把 {{采购明细}} 放在循环行上方单独一行，在需要重复的表格行中使用方括号字段。',
  },
  {
    title: '系统字段',
    syntax: '{{_id}}\n{{createdTimeText}}\n{{updatedTimeText}}',
    description: '可直接使用记录主键和格式化后的创建、修改时间；其他系统字段可从字段列表中搜索并插入。',
  },
  {
    title: '嵌套路径',
    syntax: '{{customer.address}}',
    description: '当业务数据包含嵌套对象时，可使用英文句点访问路径。普通 OA 表单优先使用字段列表中的名称。',
  },
]

const metadataSnapshot = () => JSON.stringify({
  name: info.value.name || '',
  frameId: info.value.frameId ?? null,
  sort: Number(info.value.sort || 0),
  status: Number(info.value.status || 0),
  description: info.value.description || '',
})

const hasUnsavedChanges = computed(() => {
  if (!info.value.id) {
    return editorDirty.value
      || Boolean(selectedFile.value)
      || Boolean(String(info.value.name || '').trim())
      || Boolean(info.value.frameId)
      || Boolean(String(info.value.description || '').trim())
  }
  return editorDirty.value || metadataSnapshot() !== metadataBaseline.value
})

const syncMetadataBaseline = () => {
  metadataBaseline.value = metadataSnapshot()
}

const PLACEHOLDER_FILTERS = [
  { label: '全部', value: 'all' },
  { label: '异常', value: 'issue' },
  { label: '单值', value: 'value' },
  { label: '列表', value: 'list' },
  { label: '行字段', value: 'row' },
]

const flatten = (nodes: FieldNode[], depth = 0, listParent = ''): FieldNode[] => {
  const result: FieldNode[] = []
  for (const node of nodes) {
    const item = Object.assign({}, node, { depth, listParent })
    result.push(item)
    if (node.children?.length) {
      result.push(...flatten(node.children, depth + 1, node.list ? node.field : listParent))
    }
  }
  return result
}

const fieldRows = computed(() => {
  const keyword = fieldKeyword.value.trim().toLowerCase()
  const rows = flatten(fields.value)
  if (!keyword) return rows
  return rows.filter(item => [
    item.label,
    item.field,
    item.path,
    item.component,
    item.placeholder,
    item.id,
  ].some(value => String(value || '').toLowerCase().includes(keyword)))
})

const placeholderRows = computed<PlaceholderNode[]>(() => {
  const allFields = flatten(fields.value)
  const balance = new Map<string, { start: number, end: number }>()
  for (const item of placeholders.value) {
    if (item.type !== 'list' && item.type !== 'list-end') continue
    const current = balance.get(item.name) || { start: 0, end: 0 }
    if (item.type === 'list') current.start += Number(item.count || 1)
    if (item.type === 'list-end') current.end += Number(item.count || 1)
    balance.set(item.name, current)
  }
  return placeholders.value.map(item => {
    const candidates = allFields.filter(field => field.field === item.name)
    const field = item.type === 'row'
      ? candidates.find(candidate => candidate.listParent) || candidates[0]
      : candidates.find(candidate => !candidate.listParent) || candidates[0]
    let issue = ''
    if (item.type === 'list' || item.type === 'list-end') {
      const current = balance.get(item.name)
      if (current && current.start !== current.end) {
        issue = `列表开始 ${current.start} 个，结束 ${current.end} 个`
      }
    }
    if (!field && item.type !== 'special' && item.type !== 'current') {
      issue = issue || '未找到对应表单字段'
    }
    if (item.type === 'row' && field && !field.listParent) {
      issue = issue || '该行字段不在子表单内'
    }
    return {
      ...item,
      count: Number(item.count || 1),
      field,
      issue,
    }
  })
})

const placeholderIssueCount = computed(() => {
  return placeholderRows.value.filter(item => item.issue).length
})

const filteredPlaceholderRows = computed(() => {
  const keyword = placeholderKeyword.value.trim().toLowerCase()
  return placeholderRows.value.filter(item => {
    if (placeholderFilter.value === 'issue' && !item.issue) return false
    if (placeholderFilter.value === 'value' && item.type !== 'value') return false
    if (placeholderFilter.value === 'list' && !['list', 'list-end'].includes(item.type)) return false
    if (placeholderFilter.value === 'row' && item.type !== 'row') return false
    if (!keyword) return true
    return [
      item.name,
      item.raw,
      item.field?.component,
      item.field?.path,
      item.issue,
    ].some(value => String(value || '').toLowerCase().includes(keyword))
  })
})

const parsePlaceholders = (content: string) => {
  if (!content) return []
  try {
    const result = JSON.parse(content)
    return Array.isArray(result) ? result : []
  } catch (e) {
    return []
  }
}

const loadFields = (frameId = info.value.frameId) => {
  const serial = ++fieldsRequestSerial
  PrintApi.fields({ id: info.value.id, frameId }).then((result: any) => {
    if (serial === fieldsRequestSerial) fields.value = ApiUtil.data(result) || []
  }).catch(() => {})
}

const handleFrameChange = (frameId: number | null) => {
  fieldKeyword.value = ''
  if (!frameId) {
    fieldsRequestSerial++
    fields.value = []
    return
  }
  loadFields(frameId)
}

const resetPlaceholderNavigation = () => {
  activePlaceholderRaw.value = ''
  placeholderMatchInfo.value = null
  editorRef.value?.findText('')
}

const loadDocument = () => {
  if (!info.value.hasTemplate) return Promise.resolve()
  return PrintApi.loadDocument({ id: info.value.id }).then(async (result: any) => {
    const data = ApiUtil.data(result)
    const arrayBuffer = DocxUtil.base64ToArrayBuffer(data.content)
    const state = await DocxLayoutUtil.extractEditorState(arrayBuffer)
    await (state?.data?.main
      ? editorRef.value?.loadEditorState(state as any, arrayBuffer)
      : editorRef.value?.importDocx(arrayBuffer))
    editorDirty.value = false
  })
}

const handleBeforeUnload = (event: BeforeUnloadEvent) => {
  if (!hasUnsavedChanges.value) return
  event.preventDefault()
  event.returnValue = ''
}

const handleSaveShortcut = (event: KeyboardEvent) => {
  if (!(event.ctrlKey || event.metaKey) || event.key.toLowerCase() !== 's') return
  event.preventDefault()
  saveDocument()
}

onBeforeRouteLeave(async () => {
  if (!hasUnsavedChanges.value) return true
  try {
    await ElMessageBox.confirm(
      '当前模板还有未保存的修改，离开后这些修改将丢失。',
      '离开打印模板设计',
      {
        confirmButtonText: '放弃修改',
        cancelButtonText: '继续编辑',
        type: 'warning',
      },
    )
    return true
  } catch {
    return false
  }
})

onMounted(async () => {
  window.addEventListener('beforeunload', handleBeforeUnload)
  window.addEventListener('keydown', handleSaveShortcut)
  if (window.innerWidth < 1280) outlineVisible.value = false
  if (!info.value.id) {
    syncMetadataBaseline()
    return
  }
  loading.value = true
  try {
    const result: any = await PrintApi.info({ id: info.value.id })
    Object.assign(info.value, ApiUtil.data(result))
    placeholders.value = parsePlaceholders(info.value.content)
    syncMetadataBaseline()
    loadFields()
    await loadDocument()
  } catch (e: any) {
    ElNotification({
      title: '模板加载失败',
      message: e?.message || '打印模板不存在或已被删除',
      type: 'error',
    })
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
  window.removeEventListener('keydown', handleSaveShortcut)
})

const handleFileChange = async (uploadFile: UploadFile) => {
  const file = uploadFile.raw
  if (!file) return
  const resetUploadSelection = () => {
    const input = (uploadRef.value?.$el as HTMLElement | undefined)?.querySelector('input')
    if (input) input.value = ''
  }
  if (!file.name.toLowerCase().endsWith('.docx')) {
    ElMessage.warning('仅支持 .docx 格式的 Word 文档')
    resetUploadSelection()
    return
  }
  if (file.size > 20 * 1024 * 1024) {
    ElMessage.warning('Word 模板不能超过 20MB')
    resetUploadSelection()
    return
  }
  if (editorDirty.value) {
    try {
      await ElMessageBox.confirm(
        '载入新的 Word 模板会覆盖当前尚未保存的文档内容。',
        '替换当前模板',
        {
          confirmButtonText: '继续替换',
          cancelButtonText: '取消',
          type: 'warning',
        },
      )
    } catch {
      resetUploadSelection()
      return
    }
  }
  try {
    await editorRef.value?.importDocx(await file.arrayBuffer())
    selectedFile.value = file
    editorDirty.value = true
    placeholders.value = []
    resetPlaceholderNavigation()
    resetUploadSelection()
    ElMessage.success('Word 文档已载入，可继续在线编辑')
  } catch (e: any) {
    resetUploadSelection()
    ElNotification({ title: 'Word 导入失败', message: e?.message || '文档格式或内容无法解析', type: 'error' })
  }
}

const handleUploadClick = () => {
  const element = uploadRef.value?.$el as HTMLElement | undefined
  element?.querySelector('input')?.click()
}

const saveDocument = async (): Promise<boolean> => {
  if (saving.value) return false
  if (!info.value.name?.trim()) {
    ElMessage.warning('请先填写模板名称')
    return false
  }
  info.value.name = info.value.name.trim()
  info.value.sort = Number(info.value.sort || 0)
  info.value.status = Number(info.value.status || 1)
  saving.value = true
  try {
    if (!info.value.id && !info.value.hasTemplate && !editorDirty.value) {
      ElMessage.warning('请先上传 Word 模板或直接编辑文档内容')
      return false
    }
    const sourceName = selectedFile.value?.name || info.value.templateName || `${info.value.name}.docx`
    const documentBlob = await editorRef.value!.exportDocx(sourceName)
    const result: any = await PrintApi.uploadDocument({
      id: info.value.id,
      name: info.value.name,
      frameId: info.value.frameId ?? '',
      sort: info.value.sort || 0,
      status: info.value.status || 1,
      description: info.value.description || '',
      file: new File([documentBlob], sourceName, {
        type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      }),
    }, { success: false })
    const savedInfo = ApiUtil.data(result)
    if (!savedInfo?.id) throw new Error('模板记录创建失败')
    const isNew = !info.value.id
    Object.assign(info.value, savedInfo)
    selectedFile.value = undefined
    editorDirty.value = false
    syncMetadataBaseline()
    if (isNew) {
      await router.replace({ path: '/oa/print/model', query: { id: info.value.id } })
    }
    placeholders.value = parsePlaceholders(info.value.content)
    resetPlaceholderNavigation()
    ElMessage.success('Word 模板已保存')
    return true
  } catch (e: any) {
    ElNotification({ title: '模板保存失败', message: e?.message || '请稍后重试', type: 'error' })
    return false
  } finally {
    saving.value = false
  }
}

const exportDocument = async () => {
  if (exporting.value) return
  exporting.value = true
  try {
    const blob = await editorRef.value!.exportDocx(`${info.value.name || '打印模板'}.docx`)
    DocxUtil.download(blob, `${info.value.name || '打印模板'}.docx`)
  } catch (e: any) {
    ElNotification({ title: '导出失败', message: e?.message || '请稍后重试', type: 'error' })
  } finally {
    exporting.value = false
  }
}

const printDocument = () => {
  editorRef.value?.print()
}

const insertValue = (item: FieldNode) => {
  editorRef.value?.insertText(`{{${item.field}}}`)
}

const insertListStart = (item: FieldNode) => {
  editorRef.value?.insertText(`{{?${item.field}}}`)
}

const insertListEnd = (item: FieldNode) => {
  editorRef.value?.insertText(`{{/${item.field}}}`)
}

const insertRowValue = (item: FieldNode) => {
  editorRef.value?.insertText(`[${item.field}]`)
}

const insertListVariable = (command: string, item: FieldNode) => {
  if (command === 'table') insertValue(item)
  if (command === 'start') insertListStart(item)
  if (command === 'end') insertListEnd(item)
}

const openPreview = async () => {
  const location = router.resolve({
    path: '/oa/print/pdf',
    query: { id: info.value.id, sample: '1' },
  })
  const previewWindow = window.open('', '_blank')
  if (!previewWindow) {
    ElMessage.warning('浏览器阻止了新窗口，请允许弹出窗口后重试')
    return
  }
  previewWindow.document.title = '正在准备打印预览'
  previewWindow.document.body.style.cssText = 'margin:0;min-height:100vh;display:grid;place-items:center;font:14px/1.6 system-ui,sans-serif;color:#606266;background:#f5f7fa'
  previewWindow.document.body.textContent = '正在保存模板并准备预览…'
  if (!await saveDocument()) {
    previewWindow.close()
    return
  }
  previewWindow.location.replace(location.href)
}

const handleCatalog = (items: any[]) => {
  outline.value = items || []
}

const goToOutline = (item: any) => {
  editorRef.value?.goToOutline(item.id)
}

const locatePlaceholder = async (item: PlaceholderNode) => {
  activePlaceholderRaw.value = item.raw
  placeholderMatchInfo.value = await editorRef.value?.findText(item.raw) || null
  if (!placeholderMatchInfo.value?.count) {
    ElMessage.warning('未在文档中定位到该占位符')
  }
}

const navigatePlaceholder = async (direction: 'previous' | 'next') => {
  if (!activePlaceholderRaw.value) return
  placeholderMatchInfo.value = await editorRef.value?.navigateSearch(direction) || null
}

const copyPlaceholder = (item: PlaceholderNode) => {
  FormUtil.copyToClipboard(item.raw)
}

const insertPlaceholder = (item: PlaceholderNode) => {
  editorRef.value?.insertText(item.raw)
}

const copyUsageExample = (value: string) => {
  FormUtil.copyToClipboard(value)
}

const inspectPlaceholders = () => {
  helpVisible.value = false
  sideVisible.value = true
  sideTab.value = 'placeholders'
}

const toggleSettings = () => {
  if (sideVisible.value && sideTab.value === 'settings') {
    sideVisible.value = false
    return
  }
  sideVisible.value = true
  sideTab.value = 'settings'
}

const handleMoreCommand = (command: string) => {
  if (command === 'upload') handleUploadClick()
  if (command === 'help') helpVisible.value = true
  if (command === 'download') exportDocument()
  if (command === 'print') printDocument()
}

</script>

<template>
  <div class="print-model">
    <header class="print-model__header">
      <div class="print-model__header-main">
        <LayoutBack to="/oa/print/list" title="返回列表" icon="ArrowLeft" />
        <div class="print-model__heading">
          <div class="print-model__title-group">
            <div class="print-model__title-line">
              <span class="print-model__title">{{ info.name || '打印模板设计' }}</span>
              <el-tag v-if="hasUnsavedChanges" type="warning" size="small" effect="plain">未保存</el-tag>
              <el-tag v-if="Number(info.status) === 2" type="info" size="small" effect="plain">已禁用</el-tag>
            </div>
            <div class="print-model__meta">
              {{ info.templateName || '尚未上传 Word 模板' }}
              <template v-if="info.templateSize"> · {{ (info.templateSize / 1024).toFixed(1) }} KB</template>
            </div>
          </div>
        </div>
      </div>
      <div class="print-model__header-actions">
        <el-button-group class="print-model__view-toggles">
          <el-button
            :icon="ElementPlusIcons.List"
            :class="{ 'is-active': outlineVisible }"
            :title="outlineVisible ? '隐藏文档大纲' : '显示文档大纲'"
            aria-label="切换文档大纲"
            @click="outlineVisible = !outlineVisible"
          />
          <el-button
            :icon="ElementPlusIcons.Setting"
            :class="{ 'is-active': sideVisible && sideTab === 'settings' }"
            :title="sideVisible && sideTab === 'settings' ? '关闭基础信息' : '打开基础信息'"
            :aria-label="sideVisible && sideTab === 'settings' ? '关闭基础信息' : '打开基础信息'"
            @click="toggleSettings"
          />
        </el-button-group>
        <el-button :icon="ElementPlusIcons.View" @click="openPreview">预览</el-button>
        <el-button type="primary" :icon="ElementPlusIcons.DocumentChecked" :loading="saving" @click="saveDocument">保存模板</el-button>
        <el-dropdown
          trigger="click"
          @command="handleMoreCommand"
        >
          <el-button>
            更多
            <el-icon class="el-icon--right"><ElementPlusIcons.ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="upload" :icon="ElementPlusIcons.Upload">
                上传 Word
              </el-dropdown-item>
              <el-dropdown-item command="help" :icon="ElementPlusIcons.QuestionFilled">
                变量用法
              </el-dropdown-item>
              <el-dropdown-item command="download" :icon="ElementPlusIcons.Download" :disabled="exporting" divided>
                下载 Word
              </el-dropdown-item>
              <el-dropdown-item command="print" :icon="ElementPlusIcons.Printer">
                直接打印
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <el-splitter class="print-model__body">
      <el-splitter-panel v-if="outlineVisible" size="220px" min="180px" max="300px" collapsible>
        <aside class="print-model__outline-panel">
          <div class="print-model__panel-title">
            <span>文档大纲</span>
            <el-tag size="small" effect="plain">{{ outline.length }}</el-tag>
          </div>
          <el-scrollbar class="print-model__outline-scroll">
            <el-tree
              v-if="outline.length"
              :data="outline"
              node-key="id"
              :props="{ label: 'name', children: 'subCatalog' }"
              :expand-on-click-node="false"
              default-expand-all
              class="outline-tree"
              @node-click="goToOutline"
            >
              <template #default="{ data }">
                <span class="outline-tree__item">
                  <span class="outline-tree__name">{{ data.name }}</span>
                  <span class="outline-tree__page">P{{ data.pageNo }}</span>
                </span>
              </template>
            </el-tree>
            <div v-else class="print-model__outline-empty">
              <span class="print-model__outline-empty-icon">
                <el-icon><ElementPlusIcons.Tickets /></el-icon>
              </span>
              <strong>暂无文档大纲</strong>
              <p>在 Word 中应用“标题 1 / 2 / 3”样式后，这里会自动生成层级导航。</p>
            </div>
          </el-scrollbar>
          <div class="print-model__summary">
            <div class="print-model__summary-row">
              <span>模板文件</span>
              <el-tag :type="info.hasTemplate ? 'success' : 'info'" size="small" effect="plain">
                {{ info.hasTemplate ? '已上传' : '未上传' }}
              </el-tag>
            </div>
            <div class="print-model__summary-row">
              <span>变量占位符</span>
              <strong>{{ placeholders.length }}</strong>
            </div>
          </div>
        </aside>
      </el-splitter-panel>

      <el-splitter-panel min="360px">
        <section class="print-model__editor" v-loading="loading">
          <el-upload ref="uploadRef" :show-file-list="false" accept=".docx" :auto-upload="false" :on-change="handleFileChange">
            <span class="hidden-upload" />
          </el-upload>
          <CanvasDocumentEditor
            ref="editorRef"
            height="100%"
            @upload="handleUploadClick"
            @change="editorDirty = true"
            @catalog="handleCatalog"
            @save="saveDocument"
            @export="exportDocument"
          />
        </section>
      </el-splitter-panel>

      <el-splitter-panel v-if="sideVisible" size="340px" min="300px" max="400px" collapsible>
        <aside class="print-model__side">
          <el-tabs v-model="sideTab" stretch class="print-model__tabs">
            <el-tab-pane label="基础信息" name="settings">
              <div class="print-model__pane">
                <el-scrollbar class="print-model__pane-scroll">
                  <section class="print-model__settings">
                    <el-form label-position="top" class="print-model__form">
                      <el-form-item label="模板名称" required>
                        <el-input v-model="info.name" clearable placeholder="例如：采购申请单" />
                      </el-form-item>
                      <el-form-item label="关联数据表单">
                        <form-select
                          v-model="info.frameId"
                          :callback="FormFrameApi.list"
                          clearable
                          placeholder="选择后可套用托管数据"
                          @change="handleFrameChange"
                        />
                      </el-form-item>
                      <div class="print-model__form-row">
                        <el-form-item label="发布状态">
                          <el-radio-group v-model="info.status">
                            <el-radio-button :value="1">启用</el-radio-button>
                            <el-radio-button :value="2">禁用</el-radio-button>
                          </el-radio-group>
                        </el-form-item>
                        <el-form-item label="展示顺序">
                          <el-input-number v-model="info.sort" :min="0" :max="127" controls-position="right" />
                        </el-form-item>
                      </div>
                      <el-form-item label="模板说明">
                        <el-input
                          v-model="info.description"
                          type="textarea"
                          :rows="3"
                          resize="none"
                          placeholder="说明模板用途、适用场景或注意事项"
                        />
                      </el-form-item>
                    </el-form>
                  </section>
                </el-scrollbar>
              </div>
            </el-tab-pane>
            <el-tab-pane name="fields">
              <template #label>
                <span class="print-model__tab-label">
                  字段
                  <span class="print-model__tab-count">{{ fieldRows.length }}</span>
                </span>
              </template>
              <div class="print-model__pane">
                <div class="print-model__pane-toolbar">
                  <el-input
                    v-model="fieldKeyword"
                    clearable
                    placeholder="搜索字段名称、路径或组件"
                  >
                    <template #prefix>
                      <el-icon><ElementPlusIcons.Search /></el-icon>
                    </template>
                  </el-input>
                </div>
                <el-scrollbar class="print-model__pane-scroll">
                  <div class="print-model__pane-content">
                    <div class="print-model__fields" v-if="fieldRows.length" @mousedown.prevent>
                      <div
                        v-for="item in fieldRows"
                        :key="item.path || `${item.listParent}-${item.field}`"
                        class="field-item"
                        :style="{ paddingLeft: `${item.depth ? item.depth * 14 : 8}px` }"
                      >
                        <div class="field-item__main" @click="!item.list && insertValue(item)">
                          <div class="field-item__title">
                            <span class="field-item__label">{{ item.label }}</span>
                            <span class="field-item__aside">
                              <span class="field-item__component">{{ item.component || item.type }}</span>
                              <el-tag v-if="item.required" size="small" type="danger" effect="plain">必填</el-tag>
                              <el-tag v-if="item.multiple && !item.list" size="small" type="warning" effect="plain">多选</el-tag>
                            </span>
                          </div>
                          <div class="field-item__meta">
                            <template v-if="item.list">
                              <code v-text="`正文：{{?${item.field}}} … {{/${item.field}}}`"></code>
                              <code v-text="`表格：{{${item.field}}} + 行字段`"></code>
                            </template>
                            <code
                              v-else-if="item.listParent"
                              v-text="`正文 {{${item.field}}} · 表格 [${item.field}]`"
                            ></code>
                            <code v-else v-text="`{{${item.field}}}`"></code>
                            <span v-if="item.path && item.path !== item.label">{{ item.path }}</span>
                            <span v-if="item.field !== item.label">变量：{{ item.field }}</span>
                            <span v-if="item.placeholder">提示：{{ item.placeholder }}</span>
                            <span v-if="item.optionCount">选项 {{ item.optionCount }} 个</span>
                          </div>
                        </div>
                        <el-dropdown
                          v-if="item.list"
                          trigger="click"
                          @command="command => insertListVariable(command, item)"
                          @click.stop
                        >
                          <el-button link type="primary" size="small">
                            插入列表
                            <el-icon class="el-icon--right"><ElementPlusIcons.ArrowDown /></el-icon>
                          </el-button>
                          <template #dropdown>
                            <el-dropdown-menu>
                              <el-dropdown-item command="table">Word 表格循环变量</el-dropdown-item>
                              <el-dropdown-item command="start" divided>正文列表开始</el-dropdown-item>
                              <el-dropdown-item command="end">正文列表结束</el-dropdown-item>
                            </el-dropdown-menu>
                          </template>
                        </el-dropdown>
                        <el-tooltip
                          v-else-if="item.listParent"
                          content="插入当前 Word 表格行的数据字段"
                          placement="left"
                        >
                          <el-button link type="success" size="small" @click.stop="insertRowValue(item)">表格行字段</el-button>
                        </el-tooltip>
                      </div>
                    </div>
                    <div v-else class="print-model__pane-empty">
                      <span class="print-model__pane-empty-icon">
                        <el-icon><ElementPlusIcons.Document /></el-icon>
                      </span>
                      <strong>{{ fieldKeyword ? '没有匹配的字段' : '尚未关联数据表单' }}</strong>
                      <p>
                        {{ fieldKeyword
                          ? '尝试搜索字段名称、组件类型或数据路径。'
                          : '关联表单后，可在这里查看并插入全部业务字段。' }}
                      </p>
                      <el-button
                        v-if="!fieldKeyword && !info.frameId"
                        type="primary"
                        plain
                        size="small"
                        @click="toggleSettings"
                      >
                        前往关联表单
                      </el-button>
                    </div>
                  </div>
                </el-scrollbar>
              </div>
            </el-tab-pane>
            <el-tab-pane name="placeholders">
              <template #label>
                <span class="print-model__tab-label">
                  占位符
                  <span class="print-model__tab-count" :class="{ 'is-warning': placeholderIssueCount }">
                    {{ placeholders.length }}
                  </span>
                </span>
              </template>
              <div class="print-model__pane">
                <div class="print-model__pane-toolbar">
                  <div class="placeholder-tools">
                    <el-input
                      v-model="placeholderKeyword"
                      clearable
                      placeholder="搜索变量、字段或路径"
                    >
                      <template #prefix>
                        <el-icon><ElementPlusIcons.Search /></el-icon>
                      </template>
                    </el-input>
                    <el-segmented
                      v-model="placeholderFilter"
                      :options="PLACEHOLDER_FILTERS"
                      size="small"
                      class="placeholder-tools__filter"
                    />
                  </div>
                  <div v-if="activePlaceholderRaw" class="placeholder-match">
                    <span class="placeholder-match__name">{{ activePlaceholderRaw }}</span>
                    <span v-if="placeholderMatchInfo?.count">
                      第 {{ placeholderMatchInfo.index + 1 }} / {{ placeholderMatchInfo.count }} 处
                    </span>
                    <el-button
                      text
                      size="small"
                      :icon="ElementPlusIcons.ArrowUp"
                      title="上一个匹配"
                      @click="navigatePlaceholder('previous')"
                    />
                    <el-button
                      text
                      size="small"
                      :icon="ElementPlusIcons.ArrowDown"
                      title="下一个匹配"
                      @click="navigatePlaceholder('next')"
                    />
                  </div>
                </div>
                <el-scrollbar class="print-model__pane-scroll">
                  <div class="print-model__pane-content">
                    <div class="placeholder-list" v-if="filteredPlaceholderRows.length">
                      <div
                        v-for="item in filteredPlaceholderRows"
                        :key="item.raw"
                        class="placeholder-item"
                        :class="{
                          'is-active': activePlaceholderRaw === item.raw,
                          'is-invalid': !!item.issue,
                        }"
                        @click="locatePlaceholder(item)"
                      >
                        <div class="placeholder-item__main">
                          <div class="placeholder-item__head">
                            <code>{{ item.raw }}</code>
                            <span class="placeholder-item__type">{{ PLACEHOLDER_TYPE_LABELS[item.type] || item.type }}</span>
                            <span class="placeholder-item__count">×{{ item.count }}</span>
                          </div>
                          <div v-if="item.field" class="placeholder-item__meta">
                            <span>{{ item.field.component }}</span>
                            <span>{{ item.field.path }}</span>
                          </div>
                          <div v-if="item.issue" class="placeholder-item__issue">{{ item.issue }}</div>
                        </div>
                        <el-space :size="0" class="placeholder-item__actions" @click.stop>
                          <el-tooltip content="插入到当前光标" placement="top">
                            <el-button text size="small" :icon="ElementPlusIcons.Plus" @click="insertPlaceholder(item)" />
                          </el-tooltip>
                          <el-tooltip content="复制变量" placement="top">
                            <el-button text size="small" :icon="ElementPlusIcons.CopyDocument" @click="copyPlaceholder(item)" />
                          </el-tooltip>
                        </el-space>
                      </div>
                    </div>
                    <div v-else class="print-model__pane-empty">
                      <span class="print-model__pane-empty-icon">
                        <el-icon><ElementPlusIcons.MagicStick /></el-icon>
                      </span>
                      <strong>{{ placeholders.length ? '没有匹配的占位符' : '暂无占位符' }}</strong>
                      <p>
                        {{ placeholders.length
                          ? '调整搜索关键词或切换筛选条件后重试。'
                          : '保存 Word 后，系统会自动识别变量并在这里检查异常。' }}
                      </p>
                      <el-button
                        v-if="!placeholders.length"
                        type="primary"
                        plain
                        size="small"
                        @click="helpVisible = true"
                      >
                        查看变量用法
                      </el-button>
                    </div>
                  </div>
                </el-scrollbar>
              </div>
            </el-tab-pane>
          </el-tabs>
        </aside>
      </el-splitter-panel>
    </el-splitter>

    <el-drawer
      v-model="helpVisible"
      title="打印模板变量使用说明"
      size="min(560px, 92vw)"
      class="print-model__usage-drawer"
    >
      <el-scrollbar class="usage-guide-scroll">
        <div class="usage-guide">
          <el-alert
            title="推荐直接点击右侧“字段”列表插入变量"
            type="info"
            :closable="false"
            show-icon
            description="保存 Word 后，系统会自动识别并检查占位符。请使用英文半角花括号和方括号。"
          />
          <article v-for="(example, index) in USAGE_EXAMPLES" :key="example.title" class="usage-guide__item">
            <div class="usage-guide__head">
              <span class="usage-guide__index">{{ index + 1 }}</span>
              <strong>{{ example.title }}</strong>
              <el-button
                link
                type="primary"
                :icon="ElementPlusIcons.CopyDocument"
                @click="copyUsageExample(example.syntax)"
              >
                复制示例
              </el-button>
            </div>
            <pre class="usage-guide__syntax">{{ example.syntax }}</pre>
            <p>{{ example.description }}</p>
          </article>
          <el-alert
            title="常见错误"
            type="warning"
            :closable="false"
            show-icon
            description="列表开始和结束标签未成对、表格行字段误用双花括号、变量名与字段列表不一致，都会导致预览提示异常或内容未替换。"
          />
          <div class="usage-guide__footer">
            <span>保存后可在“占位符”页签查看识别结果和异常项。</span>
            <el-button type="primary" :icon="ElementPlusIcons.Search" @click="inspectPlaceholders">
              去检查占位符
            </el-button>
          </div>
        </div>
      </el-scrollbar>
    </el-drawer>
  </div>
</template>

<style scoped lang="scss">
.print-model {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
  background: var(--el-bg-color-page);
  &__header {
    display: flex;
    flex: 0 0 auto;
    min-height: 64px;
    padding: 10px 16px;
    gap: 12px;
    flex-wrap: wrap;
    @include flex-between();
    background: var(--el-bg-color);
    border-bottom: 1px solid var(--el-border-color-lighter);
    box-shadow: 0 1px 4px rgb(28 37 51 / 6%);
  }
  &__header-main,
  &__header-actions {
    display: flex;
    align-items: center;
    gap: 10px;
  }
  &__header-main {
    min-width: 0;
    flex: 1 1 360px;
  }
  &__header-actions {
    flex: 0 1 auto;
    flex-wrap: wrap;
    gap: 8px;
    justify-content: flex-end;
  }
  &__view-toggles {
    :deep(.el-button) {
      color: var(--el-text-color-secondary);
      background: var(--el-bg-color);
    }
    :deep(.el-button.is-active) {
      color: var(--el-color-primary);
      background: var(--el-color-primary-light-9);
    }
  }
  &__heading {
    min-width: 0;
  }
  &__title-group {
    min-width: 0;
  }
  &__title-line {
    display: flex;
    min-width: 0;
    align-items: center;
    gap: 8px;
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
    font-size: 12px;
    line-height: 18px;
    color: var(--el-text-color-secondary);
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
    min-height: 0;
    overflow: hidden;
    background: var(--el-fill-color-lighter);
  }
  &__settings {
    padding: 20px 18px 24px;
  }
  &__form {
    padding: 0;
    :deep(.el-form-item) {
      margin-bottom: 16px;
    }
    :deep(.el-form-item:last-child) {
      margin-bottom: 0;
    }
    :deep(.el-form-item__label) {
      padding-bottom: 6px;
      color: var(--el-text-color-regular);
      font-size: 13px;
      line-height: 20px;
    }
  }
  &__form-row {
    display: grid;
    grid-template-columns: minmax(0, 1fr) 116px;
    gap: 12px;
    margin-bottom: 16px;
    :deep(.el-input-number) {
      width: 100%;
    }
    :deep(.el-radio-group) {
      display: flex;
      width: 100%;
    }
    :deep(.el-radio-button) {
      flex: 1 1 0;
    }
    :deep(.el-radio-button__inner) {
      width: 100%;
    }
    :deep(.el-form-item) {
      margin-bottom: 0;
    }
  }
  &__tabs {
    display: flex;
    width: 100%;
    height: 100%;
    min-height: 0;
    flex-direction: column;
    margin: 0;
    padding: 0;
    background: var(--el-fill-color-lighter);
    :deep(.el-tabs__header) {
      flex: 0 0 48px;
      margin: 0;
      padding: 0 8px;
      background: var(--el-bg-color);
    }
    :deep(.el-tabs__item) {
      font-size: 12px;
      font-weight: 500;
    }
    :deep(.el-tabs__content) {
      flex: 1 1 0;
      min-height: 0;
      overflow: hidden;
    }
    :deep(.el-tab-pane) {
      height: 100%;
    }
  }
  &__tab-label {
    display: inline-flex;
    align-items: center;
    gap: 5px;
  }
  &__tab-count {
    display: inline-grid;
    min-width: 20px;
    height: 18px;
    padding: 0 5px;
    place-items: center;
    color: var(--el-text-color-secondary);
    font-size: 11px;
    font-weight: 500;
    line-height: 18px;
    background: var(--el-fill-color);
    border-radius: 9px;
    &.is-warning {
      color: var(--el-color-warning);
      background: var(--el-color-warning-light-9);
    }
  }
  &__pane {
    display: flex;
    width: 100%;
    height: 100%;
    min-height: 0;
    flex-direction: column;
    background: var(--el-bg-color);
  }
  &__pane-toolbar {
    flex: 0 0 auto;
    padding: 12px 14px 10px;
  }
  &__pane-scroll {
    flex: 1 1 0;
    min-height: 0;
    height: auto;
  }
  &__pane-content {
    padding: 4px 8px 18px;
  }
  &__pane-empty {
    display: flex;
    min-height: 240px;
    padding: 28px 18px;
    align-items: center;
    justify-content: center;
    flex-direction: column;
    text-align: center;
    strong {
      margin-top: 10px;
      color: var(--el-text-color-primary);
      font-size: 13px;
      font-weight: 600;
    }
    p {
      max-width: 240px;
      margin: 6px 0 14px;
      color: var(--el-text-color-secondary);
      font-size: 12px;
      line-height: 1.7;
    }
  }
  &__pane-empty-icon {
    display: grid;
    width: 42px;
    height: 42px;
    place-items: center;
    color: var(--el-color-primary);
    font-size: 20px;
    background: var(--el-color-primary-light-9);
    border-radius: 50%;
  }
  &__fields {
    padding: 0 0 12px;
  }
  &__editor {
    display: flex;
    width: 100%;
    min-width: 0;
    min-height: 0;
    height: 100%;
    overflow: hidden;
  }
  &__outline-panel {
    display: flex;
    flex-direction: column;
    width: 100%;
    height: 100%;
    overflow: hidden;
    background: var(--el-bg-color);
  }
  &__panel-title {
    display: flex;
    flex: 0 0 48px;
    align-items: center;
    justify-content: space-between;
    padding: 0 14px;
    font-size: 13px;
    font-weight: 600;
    border-bottom: 1px solid var(--el-border-color-lighter);
  }
  &__outline-scroll {
    flex: 1 1 auto;
    min-height: 0;
  }
  &__outline-empty {
    display: flex;
    min-height: 250px;
    padding: 32px 18px;
    align-items: center;
    justify-content: center;
    flex-direction: column;
    text-align: center;
    strong {
      margin-top: 10px;
      color: var(--el-text-color-primary);
      font-size: 13px;
      font-weight: 600;
    }
    p {
      margin: 6px 0 0;
      color: var(--el-text-color-secondary);
      font-size: 12px;
      line-height: 1.7;
    }
  }
  &__outline-empty-icon {
    display: grid;
    width: 42px;
    height: 42px;
    place-items: center;
    color: var(--el-color-primary);
    font-size: 20px;
    background: var(--el-color-primary-light-9);
    border-radius: 50%;
  }
  &__summary {
    flex: 0 0 auto;
    padding: 12px 14px;
    border-top: 1px solid var(--el-border-color-lighter);
    background: var(--el-fill-color-lighter);
  }
  &__summary-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    min-height: 28px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
    strong {
      font-weight: 500;
      color: var(--el-text-color-regular);
    }
  }
}
.field-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  min-height: 48px;
  margin: 2px 6px;
  padding: 5px 6px 5px 0;
  border-radius: 4px;
  cursor: pointer;
  &:hover {
    background: var(--el-fill-color-light);
  }
  &__main {
    display: flex;
    min-width: 0;
    flex: 1 1 auto;
    flex-direction: column;
    align-items: stretch;
    gap: 3px;
    padding: 2px 0;
  }
  &__title {
    display: flex;
    min-width: 0;
    align-items: center;
    justify-content: space-between;
    gap: 4px;
  }
  &__label {
    flex: 1 1 auto;
    min-width: 0;
    max-width: 150px;
    overflow: hidden;
    font-size: 13px;
    font-weight: 500;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  &__component {
    flex: none;
    color: var(--el-text-color-placeholder);
    font-size: 12px;
    line-height: 18px;
    white-space: nowrap;
  }
  &__aside {
    display: flex;
    flex: none;
    align-items: center;
    justify-content: flex-end;
    gap: 4px;
  }
  &__meta {
    display: flex;
    min-width: 0;
    flex-wrap: wrap;
    gap: 3px 8px;
    color: var(--el-text-color-secondary);
    font-size: 12px;
    line-height: 18px;
    code {
      max-width: 100%;
      overflow: hidden;
      padding: 0 4px;
      color: var(--el-color-primary);
      font-size: 11px;
      background: var(--el-color-primary-light-9);
      border-radius: 3px;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}
.placeholder-tools {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 8px;
  &__filter {
    display: flex;
    width: 100%;
    :deep(.el-segmented__item) {
      flex: 1 1 0;
      min-width: 0;
    }
  }
}
.placeholder-match {
  display: flex;
  min-height: 30px;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  padding: 4px 4px 4px 8px;
  color: var(--el-text-color-secondary);
  font-size: 11px;
  background: var(--el-color-primary-light-9);
  border-radius: 4px;
  &__name {
    min-width: 0;
    flex: 1 1 auto;
    overflow: hidden;
    color: var(--el-color-primary);
    font-family: monospace;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
.placeholder-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 0 0 12px;
}
.placeholder-item {
  display: flex;
  min-width: 0;
  align-items: flex-start;
  gap: 4px;
  padding: 7px 6px 7px 8px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 5px;
  cursor: pointer;
  transition: border-color 0.15s, background-color 0.15s;
  &:hover {
    background: var(--el-fill-color-light);
    border-color: var(--el-border-color);
  }
  &.is-active {
    background: var(--el-color-primary-light-9);
    border-color: var(--el-color-primary-light-5);
  }
  &.is-invalid {
    border-color: var(--el-color-danger-light-7);
  }
  &__main {
    display: flex;
    min-width: 0;
    flex: 1 1 auto;
    flex-direction: column;
    gap: 3px;
  }
  &__head {
    display: flex;
    min-width: 0;
    align-items: center;
    flex-wrap: wrap;
    gap: 5px;
  }
  code {
    max-width: 100%;
    overflow: hidden;
    color: var(--el-text-color-primary);
    font-size: 12px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  &__type,
  &__count {
    flex: none;
    color: var(--el-text-color-placeholder);
    font-size: 11px;
  }
  &__meta {
    display: flex;
    min-width: 0;
    flex-wrap: wrap;
    gap: 2px 8px;
    color: var(--el-text-color-secondary);
    font-size: 11px;
    line-height: 16px;
  }
  &__issue {
    color: var(--el-color-danger);
    font-size: 11px;
    line-height: 16px;
  }
  &__actions {
    flex: none;
    margin-top: -3px;
    :deep(.el-button) {
      width: 24px;
      height: 24px;
      margin: 0;
      padding: 0;
    }
  }
}
.hidden-upload {
  display: none;
}
.outline-tree {
  padding: 2px 0 12px;
  background: transparent;
  :deep(.el-tree-node__content) {
    height: 32px;
    border-radius: 4px;
  }
  &__item {
    display: flex;
    flex: 1;
    min-width: 0;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    padding-right: 6px;
  }
  &__name {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  &__page {
    flex: none;
    color: var(--el-text-color-secondary);
    font-size: 11px;
  }
}

:global(.print-model__usage-drawer .el-drawer__body) {
  padding: 0;
  overflow: hidden;
}
.usage-guide-scroll {
  height: 100%;
}
.usage-guide {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 16px 20px 24px;
  color: var(--el-text-color-regular);
  &__item {
    padding: 14px;
    background: var(--el-fill-color-lighter);
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 8px;
  }
  &__head {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 10px;
    strong {
      flex: 1 1 auto;
      color: var(--el-text-color-primary);
      font-size: 14px;
    }
  }
  &__index {
    display: inline-grid;
    width: 22px;
    height: 22px;
    place-items: center;
    color: var(--el-color-primary);
    font-size: 12px;
    font-weight: 600;
    background: var(--el-color-primary-light-9);
    border-radius: 50%;
  }
  &__syntax {
    margin: 0 0 10px;
    padding: 10px 12px;
    overflow: hidden;
    color: var(--el-text-color-primary);
    font-family: Consolas, Monaco, 'Courier New', monospace;
    font-size: 12px;
    line-height: 1.65;
    white-space: pre-wrap;
    word-break: break-word;
    background: var(--el-bg-color);
    border: 1px solid var(--el-border-color);
    border-radius: 6px;
  }
  p {
    margin: 0;
    color: var(--el-text-color-secondary);
    font-size: 12px;
    line-height: 1.7;
  }
  &__footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    padding-top: 4px;
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }
}

@media (max-width: 900px) {
  .print-model {
    &__header {
      align-items: flex-start;
      padding: 10px 12px;
    }
    &__header-main {
      flex: 1 1 100%;
    }
    &__header-actions {
      flex: 1 1 100%;
      width: 100%;
      justify-content: flex-end;
    }
  }
}

@media (max-width: 960px) {
  .print-model {
    &__form-row {
      grid-template-columns: 1fr;
      gap: 0;
    }
  }
  .usage-guide__footer {
    align-items: flex-start;
    flex-direction: column;
  }
}

</style>
