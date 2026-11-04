<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import Editor, {
  EditorMode,
  ListStyle,
  ListType,
  PageMode,
  RowFlex,
  TextDecorationStyle,
  TitleLevel,
} from '@hufe921/canvas-editor'
import type { IEditorResult, IRangeStyle } from '@hufe921/canvas-editor'
import docxPlugin from '@hufe921/canvas-editor-plugin-docx'
import * as ElementPlusIcons from '@element-plus/icons-vue'
import DocxLayoutUtil from '@/utils/DocxLayoutUtil'

const props = withDefaults(defineProps<{
  editable?: boolean
  showActions?: boolean
  height?: string
}>(), {
  editable: true,
  showActions: true,
  height: 'calc(100vh - 158px)',
})

const emit = defineEmits<{
  change: []
  catalog: [items: any[]]
  upload: []
  save: []
  export: []
}>()

const container = ref<HTMLDivElement>()
const imageInputRef = ref<HTMLInputElement>()
const searchInputRef = ref<any>()
let editor: Editor | null = null
let readyResolvers: Array<() => void> = []
let unready = false
let catalogTimer: number | undefined
let lastRange: any = null
let resizeObserver: ResizeObserver | null = null
let fitFrame: number | undefined
let sourceArrayBuffer: ArrayBuffer | null = null

const PAGE_WIDTH = 820
const CANVAS_HORIZONTAL_PADDING = 48

const FONT_OPTIONS = [
  { label: '宋体', value: '宋体' },
  { label: '微软雅黑', value: 'Microsoft YaHei' },
  { label: '黑体', value: 'SimHei' },
  { label: '楷体', value: 'KaiTi' },
  { label: '仿宋', value: 'FangSong' },
  { label: 'Arial', value: 'Arial' },
  { label: 'Times New Roman', value: 'Times New Roman' },
]
const SIZE_OPTIONS = [
  { label: '初号', value: 56 },
  { label: '小初', value: 48 },
  { label: '一号', value: 34 },
  { label: '小一', value: 32 },
  { label: '二号', value: 29 },
  { label: '小二', value: 24 },
  { label: '三号', value: 21 },
  { label: '小三', value: 20 },
  { label: '四号', value: 18 },
  { label: '小四', value: 16 },
  { label: '五号', value: 14 },
  { label: '小五', value: 12 },
  { label: '六号', value: 10 },
  { label: '小六', value: 8 },
]
const TITLE_OPTIONS = [
  { label: '正文', value: 'body' },
  { label: '标题 1', value: TitleLevel.FIRST },
  { label: '标题 2', value: TitleLevel.SECOND },
  { label: '标题 3', value: TitleLevel.THIRD },
  { label: '标题 4', value: TitleLevel.FOURTH },
  { label: '标题 5', value: TitleLevel.FIFTH },
  { label: '标题 6', value: TitleLevel.SIXTH },
]
const LINE_HEIGHT_OPTIONS = [1, 1.25, 1.5, 1.75, 2, 2.5, 3]
const ALIGNMENT_OPTIONS = [
  { label: '左对齐', value: RowFlex.LEFT },
  { label: '居中', value: RowFlex.CENTER },
  { label: '右对齐', value: RowFlex.RIGHT },
  { label: '两端对齐', value: RowFlex.JUSTIFY },
]
const TEXT_COLORS = ['#303133', '#c00000', '#ed7d31', '#ffc000', '#70ad47', '#4472c4', '#7030a0', '#ffffff']
const BACKGROUND_COLORS = ['#fff2cc', '#fce4d6', '#e2f0d9', '#deebf7', '#e4dfec', '#f8cbad', '#d9ead3', '#f2f2f2']
const rangeStyle = ref<Partial<IRangeStyle>>({})
const textColorVisible = ref(false)
const backgroundColorVisible = ref(false)
const searchVisible = ref(false)
const searchKeyword = ref('')
const replaceKeyword = ref('')
const searchInfo = ref<{ index: number, count: number } | null>(null)

const currentFontLabel = computed(() => {
  const current = rangeStyle.value.font || '宋体'
  return FONT_OPTIONS.find(option => option.value === current)?.label || current
})
const currentSizeLabel = computed(() => {
  const current = rangeStyle.value.size || 16
  return SIZE_OPTIONS.find(option => option.value === current)?.label || `${current}px`
})
const currentTitleLabel = computed(() => {
  const current = rangeStyle.value.level || 'body'
  return TITLE_OPTIONS.find(option => option.value === current)?.label || '正文'
})
const currentAlignment = computed(() => rangeStyle.value.rowFlex || RowFlex.LEFT)
const getAlignmentLines = (value: RowFlex) => {
  const widths = value === RowFlex.LEFT
    ? [12, 9, 12, 7]
    : value === RowFlex.CENTER
      ? [12, 8, 12, 6]
      : value === RowFlex.RIGHT
        ? [12, 9, 12, 7]
        : [12, 12, 12, 12]
  return widths.map((width, index) => {
    const x1 = value === RowFlex.CENTER
      ? (18 - width) / 2
      : value === RowFlex.RIGHT
        ? 15 - width
        : 3
    return { x1, x2: x1 + width, y: 3 + index * 4 }
  })
}
const currentAlignmentLines = computed(() => getAlignmentLines(currentAlignment.value))
const currentTextColor = computed(() => rangeStyle.value.color || '#303133')
const currentBackgroundColor = computed(() => rangeStyle.value.highlight || '#fff2cc')
const searchResultText = computed(() => {
  if (!searchInfo.value || !searchInfo.value.count) return '0 / 0'
  return `${searchInfo.value.index + 1} / ${searchInfo.value.count}`
})

const fitPageToCanvas = () => {
  if (fitFrame !== undefined) window.cancelAnimationFrame(fitFrame)
  fitFrame = window.requestAnimationFrame(() => {
    if (!editor || !container.value) return
    const canvas = container.value.closest<HTMLElement>('.document-canvas')
    if (!canvas) return
    const availableWidth = canvas.clientWidth - CANVAS_HORIZONTAL_PADDING
    if (availableWidth <= 0) return
    const pageWidth = editor.command.getOptions().width || PAGE_WIDTH
    const scale = Math.min(1, availableWidth / pageWidth)
    editor.command.executePageScale(Number(scale.toFixed(3)))
  })
}

const getEditor = () => {
  if (editor) return editor
  if (unready) throw new Error('文档编辑器已销毁')
  return new Promise<Editor>((resolve) => {
    readyResolvers.push(() => resolve(editor as Editor))
  })
}

const run = async (command: (instance: Editor) => void) => {
  const instance = await getEditor()
  command(instance)
}

const runAtSelection = async (command: (instance: Editor) => void) => {
  const instance = await getEditor()
  if (lastRange) restoreRange(instance, lastRange)
  command(instance)
  lastRange = instance.command.getRange()
}

const refreshCatalog = () => {
  window.clearTimeout(catalogTimer)
  catalogTimer = window.setTimeout(async () => {
    if (!editor) return
    try {
      emit('catalog', await editor.command.getCatalog() || [])
    } catch (e) {
      emit('catalog', [])
    }
  }, 180)
}

const importDocx = async (arrayBuffer: ArrayBuffer) => {
  const instance = await getEditor()
  sourceArrayBuffer = arrayBuffer.slice(0)
  await (instance.command as any).executeImportDocx({
    arrayBuffer,
    isAppend: false,
  })
  // 部分外部 Word 会在导入时带入连续模式，这里统一恢复分页展示。
  instance.command.executePageMode(PageMode.PAGING)
  instance.command.executeForceUpdate()
  fitPageToCanvas()
  refreshCatalog()
}

const loadEditorState = async (state: IEditorResult, source?: ArrayBuffer) => {
  const instance = await getEditor()
  if (source) sourceArrayBuffer = source.slice(0)
  if (state.options) instance.command.executeUpdateOptions(state.options)
  instance.command.executeSetValue(state.data, { isSetCursor: false })
  instance.command.executePageMode(PageMode.PAGING)
  instance.command.executeForceUpdate()
  fitPageToCanvas()
  refreshCatalog()
}

const getEditorState = async (): Promise<IEditorResult> => {
  const instance = await getEditor()
  return instance.command.getValue()
}

const exportDocx = async (fileName = 'document.docx'): Promise<Blob> => {
  const instance = await getEditor()
  // 仅在明确点击下载按钮时保存文件，保存/预览流程只生成 Blob。
  const originalClick = HTMLAnchorElement.prototype.click
  HTMLAnchorElement.prototype.click = function () {
    if (this.download && this.href.startsWith('blob:')) return
    return originalClick.call(this)
  }
  let documentBlob: Blob
  try {
    documentBlob = await (instance.command as any).executeExportDocx({ fileName, download: false })
  } finally {
    HTMLAnchorElement.prototype.click = originalClick
  }
  return DocxLayoutUtil.preserveEditorLayout(
    documentBlob,
    sourceArrayBuffer || undefined,
    instance.command.getValue(),
  )
}

const print = async () => {
  const instance = await getEditor()
  await instance.command.executePrint()
}

const setFont = (font: string) => runAtSelection(instance => instance.command.executeFont(font))
const setSize = (size: number) => runAtSelection(instance => instance.command.executeSize(size))
const setLineHeight = (rowMargin: number) => runAtSelection(instance => instance.command.executeRowMargin(rowMargin))
const setUnderlineStyle = (style: TextDecorationStyle) => {
  runAtSelection(instance => instance.command.executeUnderline({ style }))
}
const setTextColor = (color: string | null) => runAtSelection(instance => instance.command.executeColor(color))
const setTextBackground = (color: string | null) => runAtSelection(instance => instance.command.executeHighlight(color))
const chooseTextColor = (color: string) => {
  setTextColor(color)
  textColorVisible.value = false
}
const chooseBackgroundColor = (color: string | null) => {
  setTextBackground(color)
  backgroundColorVisible.value = false
}
const handleCustomTextColor = (event: Event) => {
  chooseTextColor((event.target as HTMLInputElement).value)
}
const handleCustomBackgroundColor = (event: Event) => {
  chooseBackgroundColor((event.target as HTMLInputElement).value)
}
const clearFormatting = () => runAtSelection(instance => instance.command.executeFormat())
const togglePainter = () => runAtSelection(instance => instance.command.executePainter({ isDblclick: false }))

const setTitleByValue = (value: string) => {
  setTitle(value === 'body' ? null : value as TitleLevel)
}

const setListByValue = (value: string) => {
  if (!value) {
    setList(null)
    return
  }
  const [type, style] = value.split(':') as [ListType, ListStyle]
  runAtSelection(instance => instance.command.executeList(type, style))
}

const insertTable = (row = 3, col = 3) => {
  runAtSelection(instance => instance.command.executeInsertTable(row, col))
}

const insertPageBreak = () => {
  runAtSelection(instance => instance.command.executePageBreak())
}

const insertSeparator = (style: string) => {
  const dashArrayMap: Record<string, number[]> = {
    solid: [0, 0],
    dashed: [4, 4],
    dotted: [1, 1],
    dashDot: [7, 3, 3, 3],
  }
  runAtSelection(instance => instance.command.executeSeparator(dashArrayMap[style] || [0, 0]))
}

const insertDate = () => {
  const now = new Date()
  insertText(`${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日`)
}

const handleInsertCommand = (command: string) => {
  if (command.startsWith('table:')) {
    const [, row, col] = command.split(':').map(Number)
    insertTable(row, col)
    return
  }
  if (command.startsWith('separator:')) {
    insertSeparator(command.split(':')[1])
    return
  }
  if (command === 'image') openImagePicker()
  if (command === 'hyperlink') insertHyperlink()
  if (command === 'pageBreak') insertPageBreak()
  if (command === 'date') insertDate()
}

const openImagePicker = () => {
  imageInputRef.value?.click()
}

const handleImageChange = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  try {
    const value = await new Promise<string>((resolve, reject) => {
      const reader = new FileReader()
      reader.onload = () => resolve(String(reader.result || ''))
      reader.onerror = () => reject(reader.error || new Error('图片读取失败'))
      reader.readAsDataURL(file)
    })
    const image = await new Promise<HTMLImageElement>((resolve, reject) => {
      const element = new Image()
      element.onload = () => resolve(element)
      element.onerror = () => reject(new Error('图片解析失败'))
      element.src = value
    })
    const instance = await getEditor()
    const pageWidth = instance.command.getOptions().width || PAGE_WIDTH
    const maxWidth = Math.min(520, pageWidth - 216)
    const ratio = Math.min(1, maxWidth / image.naturalWidth)
    if (lastRange) restoreRange(instance, lastRange)
    instance.command.executeImage({
      value,
      width: Math.round(image.naturalWidth * ratio),
      height: Math.round(image.naturalHeight * ratio),
    })
    lastRange = instance.command.getRange()
  } catch (error: any) {
    ElMessage.error(error?.message || '图片插入失败')
  }
}

const restoreRange = (instance: Editor, range: any) => {
  if (!range) return
  instance.command.executeFocus({ range, isMoveCursorToVisible: true })
  instance.command.executeSetRange(
    range.startIndex,
    range.endIndex,
    range.tableId,
    range.startTdIndex,
    range.endTdIndex,
    range.startTrIndex,
    range.endTrIndex,
  )
}

const insertHyperlink = async () => {
  const instance = await getEditor()
  const selectedText = instance.command.getRangeText().trim()
  const range = lastRange ? { ...lastRange } : instance.command.getRange()
  try {
    const result = await ElMessageBox.prompt('请输入链接地址', '插入超链接', {
      confirmButtonText: '插入',
      cancelButtonText: '取消',
      inputPlaceholder: 'https://example.com',
      inputValidator: value => Boolean(value?.trim()) || '链接地址不能为空',
    })
    const url = result.value.trim()
    if (!url) return
    const label = selectedText || url
    restoreRange(instance, range)
    instance.command.executeHyperlink({
      valueList: Array.from(label).map(value => ({ value })),
      url,
    })
    lastRange = instance.command.getRange()
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.message || '超链接插入失败')
    }
  }
}

const updateSearchInfo = () => {
  searchInfo.value = editor?.command.getSearchNavigateInfo() || null
}

const focusSearchInput = () => {
  nextTick(() => searchInputRef.value?.focus())
}

const searchDocument = async () => {
  if (!searchKeyword.value) {
    await run(instance => instance.command.executeSearch(null))
    searchInfo.value = null
    return
  }
  await run(instance => instance.command.executeSearch(searchKeyword.value, {
    isIgnoreCase: true,
    isRegEnable: false,
  }))
  updateSearchInfo()
}

const searchPrevious = async () => {
  if (!searchKeyword.value) return
  if (!searchInfo.value) {
    await searchDocument()
    return
  }
  await run(instance => instance.command.executeSearchNavigatePre())
  updateSearchInfo()
}

const searchNext = async () => {
  if (!searchKeyword.value) return
  if (!searchInfo.value) {
    await searchDocument()
    return
  }
  await run(instance => instance.command.executeSearchNavigateNext())
  updateSearchInfo()
}

const replaceCurrent = async () => {
  if (!searchKeyword.value || !searchInfo.value) return
  await run(instance => instance.command.executeReplace(replaceKeyword.value))
  await searchDocument()
}

const closeSearch = () => {
  run(instance => instance.command.executeSearch(null))
  searchInfo.value = null
}

const findText = async (text: string) => {
  if (!text) {
    searchKeyword.value = ''
    closeSearch()
    return null
  }
  searchKeyword.value = text
  await run(instance => instance.command.executeSearch(text, {
    isIgnoreCase: true,
    isRegEnable: false,
  }))
  updateSearchInfo()
  if (searchInfo.value && searchInfo.value.index < 0) {
    await run(instance => instance.command.executeSearchNavigateNext())
    updateSearchInfo()
  }
  return searchInfo.value ? { ...searchInfo.value } : null
}

const navigateSearch = async (direction: 'previous' | 'next') => {
  if (!searchInfo.value) return null
  await run(instance => direction === 'previous'
    ? instance.command.executeSearchNavigatePre()
    : instance.command.executeSearchNavigateNext())
  updateSearchInfo()
  return searchInfo.value ? { ...searchInfo.value } : null
}

const insertText = (text: string) => {
  run(instance => {
    const range = lastRange || instance.command.getRange()
    restoreRange(instance, range)
    instance.command.executeInsertElementList([{ value: text }])
    lastRange = instance.command.getRange()
  })
}

const goToOutline = async (titleId: string) => {
  const instance = await getEditor()
  instance.command.executeLocationCatalog(titleId)
}

const setTitle = (level: TitleLevel | null) => runAtSelection(instance => instance.command.executeTitle(level))
const setList = (type: ListType | null) => runAtSelection(instance => instance.command.executeList(type))
const setAlign = (value: RowFlex) => runAtSelection(instance => instance.command.executeRowFlex(value))

onMounted(() => {
  editor = new Editor(container.value as HTMLDivElement, {
    main: [
      { value: '欢迎使用在线打印模板编辑器' },
    ],
  }, {
    mode: props.editable ? EditorMode.EDIT : EditorMode.READONLY,
    scrollContainerSelector: '.document-canvas__scrollbar .el-scrollbar__wrap',
    pageMode: PageMode.PAGING,
    pageGap: 24,
    width: PAGE_WIDTH,
    height: 1120,
    margins: [96, 108, 96, 108],
    defaultFont: '宋体',
    defaultSize: 16,
    pageBreak: {
      fontSize: 0,
      lineDash: [0, 100000],
    },
  })
  editor.use(docxPlugin)
  editor.listener.contentChange = () => {
    emit('change')
    refreshCatalog()
  }
  const rememberRange = (range?: any) => {
    if (!editor) return
    const active = document.activeElement
    if (!container.value?.contains(active)) return
    const current = range || editor.command.getRange()
    if (current && Number.isFinite(current.startIndex) && Number.isFinite(current.endIndex)) {
      lastRange = { ...current }
    }
  }
  editor.eventBus.on('rangeChange', (range: any) => rememberRange(range))
  editor.eventBus.on('mouseup', () => {
    window.setTimeout(() => {
      if (editor && container.value?.contains(document.activeElement)) {
        const current = editor.command.getRange()
        if (Number.isFinite(current.startIndex) && Number.isFinite(current.endIndex)) {
          lastRange = { ...current }
        }
      }
    })
  })
  // 初次点击正文时立即记录光标，避免后续侧栏操作丢失位置。
  const initialRangeHandler = () => {
    if (!lastRange && editor) lastRange = editor.command.getRange()
  }
  const oldRangeChange = editor.listener.rangeStyleChange
  editor.listener.rangeStyleChange = payload => {
    rangeStyle.value = { ...payload }
    if (!lastRange && editor) lastRange = editor.command.getRange()
    oldRangeChange?.(payload)
  }
  editor.eventBus.on('click', () => {
    if (editor && container.value?.contains(document.activeElement)) {
      const current = editor.command.getRange()
      if (Number.isFinite(current.startIndex) && Number.isFinite(current.endIndex)) {
        lastRange = { ...current }
      }
    }
  })
  const resolvers = readyResolvers
  readyResolvers = []
  resolvers.forEach(resolve => resolve())
  refreshCatalog()
  resizeObserver = new ResizeObserver(fitPageToCanvas)
  const canvas = container.value?.closest<HTMLElement>('.document-canvas')
  if (canvas) resizeObserver.observe(canvas)
  fitPageToCanvas()
})

onBeforeUnmount(() => {
  unready = true
  window.clearTimeout(catalogTimer)
  if (fitFrame !== undefined) window.cancelAnimationFrame(fitFrame)
  resizeObserver?.disconnect()
  resizeObserver = null
  editor?.destroy()
  editor = null
})

defineExpose({
  importDocx,
  loadEditorState,
  getEditorState,
  exportDocx,
  print,
  insertText,
  insertTable,
  insertPageBreak,
  goToOutline,
  findText,
  navigateSearch,
  refreshCatalog,
})
</script>

<template>
  <section class="document-editor">
    <div class="document-toolbar">
      <el-scrollbar class="document-toolbar__scrollbar">
        <div class="document-toolbar__content" @mousedown.prevent>
        <div class="document-toolbar__group">
          <el-button
            text
            size="small"
            :icon="ElementPlusIcons.RefreshLeft"
            title="撤销"
            @click="run(instance => instance.command.executeUndo())"
          />
          <el-button
            text
            size="small"
            :icon="ElementPlusIcons.RefreshRight"
            title="重做"
            @click="run(instance => instance.command.executeRedo())"
          />
          <el-button
            text
            size="small"
            :icon="ElementPlusIcons.Brush"
            title="格式刷（单击使用一次）"
            :class="{ 'is-active': rangeStyle.painter }"
            @click="togglePainter"
          />
          <el-button
            text
            size="small"
            :icon="ElementPlusIcons.MagicStick"
            title="清除格式"
            @click="clearFormatting"
          />
        </div>
        <el-divider direction="vertical" />

        <div class="document-toolbar__group">
          <el-dropdown trigger="click" @command="setFont">
            <button type="button" class="document-toolbar__select" :title="`字体：${currentFontLabel}`">
              <span>{{ currentFontLabel }}</span>
              <el-icon><ElementPlusIcons.ArrowDown /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="font in FONT_OPTIONS" :key="font.value" :command="font.value">
                  <span :style="{ fontFamily: font.value }">{{ font.label }}</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-dropdown trigger="click" @command="setSize">
            <button type="button" class="document-toolbar__select document-toolbar__select--size" :title="`字号：${currentSizeLabel}`">
              <span>{{ currentSizeLabel }}</span>
              <el-icon><ElementPlusIcons.ArrowDown /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="size in SIZE_OPTIONS" :key="size.value" :command="size.value">
                  {{ size.label }}（{{ size.value }}）
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button text size="small" title="增大字号" @click="runAtSelection(instance => instance.command.executeSizeAdd())">A<sup>+</sup></el-button>
          <el-button text size="small" title="减小字号" @click="runAtSelection(instance => instance.command.executeSizeMinus())">A<sup>-</sup></el-button>
          <el-button
            text
            size="small"
            title="加粗"
            :class="{ 'is-active': rangeStyle.bold }"
            @click="runAtSelection(instance => instance.command.executeBold())"
          ><b>B</b></el-button>
          <el-button
            text
            size="small"
            title="斜体"
            :class="{ 'is-active': rangeStyle.italic }"
            @click="runAtSelection(instance => instance.command.executeItalic())"
          ><i>I</i></el-button>
          <el-dropdown trigger="click" @command="setUnderlineStyle">
            <button
              type="button"
              class="document-toolbar__plain"
              :class="{ 'is-active': rangeStyle.underline }"
              title="下划线样式"
            >
              <u>U</u>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item :command="TextDecorationStyle.SOLID">
                  <span class="document-toolbar__menu-item">
                    <span class="document-toolbar__underline-sample">U</span>单实线
                  </span>
                </el-dropdown-item>
                <el-dropdown-item :command="TextDecorationStyle.DOUBLE">
                  <span class="document-toolbar__menu-item">
                    <span class="document-toolbar__underline-sample document-toolbar__underline-sample--double">U</span>双实线
                  </span>
                </el-dropdown-item>
                <el-dropdown-item :command="TextDecorationStyle.DASHED">
                  <span class="document-toolbar__menu-item">
                    <span class="document-toolbar__underline-sample document-toolbar__underline-sample--dashed">U</span>虚线
                  </span>
                </el-dropdown-item>
                <el-dropdown-item :command="TextDecorationStyle.DOTTED">
                  <span class="document-toolbar__menu-item">
                    <span class="document-toolbar__underline-sample document-toolbar__underline-sample--dotted">U</span>点线
                  </span>
                </el-dropdown-item>
                <el-dropdown-item :command="TextDecorationStyle.WAVY">
                  <span class="document-toolbar__menu-item">
                    <span class="document-toolbar__underline-sample document-toolbar__underline-sample--wavy">U</span>波浪线
                  </span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button
            text
            size="small"
            title="删除线"
            :class="{ 'is-active': rangeStyle.strikeout }"
            @click="runAtSelection(instance => instance.command.executeStrikeout())"
          ><s>S</s></el-button>
          <el-button text size="small" title="上标" @click="runAtSelection(instance => instance.command.executeSuperscript())">x<sup>2</sup></el-button>
          <el-button text size="small" title="下标" @click="runAtSelection(instance => instance.command.executeSubscript())">x<sub>2</sub></el-button>
          <el-popover
            v-model:visible="textColorVisible"
            trigger="click"
            placement="bottom"
            :width="204"
            @show="backgroundColorVisible = false"
          >
            <template #reference>
              <el-button text size="small" class="document-toolbar__color-button" title="字体颜色">
                <span class="document-toolbar__color-letter">A</span>
                <span class="document-toolbar__color-strip" :style="{ backgroundColor: currentTextColor }" />
              </el-button>
            </template>
            <div class="document-color-panel">
              <button
                v-for="color in TEXT_COLORS"
                :key="color"
                type="button"
                class="document-color-panel__swatch"
                :class="{ 'is-active': currentTextColor.toUpperCase() === color.toUpperCase() }"
                :style="{ backgroundColor: color }"
                :title="color"
                @click="chooseTextColor(color)"
              />
              <label class="document-color-panel__custom" title="自定义颜色">
                <span>＋</span>
                <input type="color" :value="currentTextColor" @input="handleCustomTextColor">
              </label>
              <el-button class="document-color-panel__clear" link type="primary" @click="chooseTextColor('#303133')">恢复默认</el-button>
            </div>
          </el-popover>
          <el-popover
            v-model:visible="backgroundColorVisible"
            trigger="click"
            placement="bottom"
            :width="204"
            @show="textColorVisible = false"
          >
            <template #reference>
              <el-button text size="small" class="document-toolbar__color-button" title="背景颜色">
                <el-icon class="document-toolbar__background-icon"><ElementPlusIcons.BrushFilled /></el-icon>
                <span class="document-toolbar__color-strip" :style="{ backgroundColor: currentBackgroundColor }" />
              </el-button>
            </template>
            <div class="document-color-panel">
              <button
                v-for="color in BACKGROUND_COLORS"
                :key="color"
                type="button"
                class="document-color-panel__swatch"
                :class="{ 'is-active': currentBackgroundColor.toUpperCase() === color.toUpperCase() }"
                :style="{ backgroundColor: color }"
                :title="color"
                @click="chooseBackgroundColor(color)"
              />
              <label class="document-color-panel__custom" title="自定义背景颜色">
                <span>＋</span>
                <input type="color" :value="currentBackgroundColor" @input="handleCustomBackgroundColor">
              </label>
              <el-button class="document-color-panel__clear" link type="primary" @click="chooseBackgroundColor(null)">清除背景</el-button>
            </div>
          </el-popover>
        </div>
        <el-divider direction="vertical" />

        <div class="document-toolbar__group">
          <el-dropdown trigger="click" @command="setTitleByValue">
            <button type="button" class="document-toolbar__select document-toolbar__select--title" :title="`段落样式：${currentTitleLabel}`">
              <el-icon><ElementPlusIcons.Document /></el-icon>
              <span>{{ currentTitleLabel }}</span>
              <el-icon><ElementPlusIcons.ArrowDown /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="item in TITLE_OPTIONS" :key="item.value" :command="item.value">
                  {{ item.label }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-dropdown trigger="click" @command="setAlign">
            <button type="button" class="document-toolbar__plain" title="段落对齐">
              <svg class="document-toolbar__alignment-icon" viewBox="0 0 18 18" aria-hidden="true">
                <line
                  v-for="line in currentAlignmentLines"
                  :key="line.y"
                  :x1="line.x1"
                  :x2="line.x2"
                  :y1="line.y"
                  :y2="line.y"
                />
              </svg>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="item in ALIGNMENT_OPTIONS" :key="item.value" :command="item.value">
                  <span class="document-toolbar__alignment-option">
                    <svg class="document-toolbar__alignment-icon" viewBox="0 0 18 18" aria-hidden="true">
                      <line
                        v-for="line in getAlignmentLines(item.value)"
                        :key="line.y"
                        :x1="line.x1"
                        :x2="line.x2"
                        :y1="line.y"
                        :y2="line.y"
                      />
                    </svg>
                    {{ item.label }}
                  </span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-dropdown trigger="click" @command="setLineHeight">
            <button type="button" class="document-toolbar__plain" title="行间距">
              <el-icon><ElementPlusIcons.Sort /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="height in LINE_HEIGHT_OPTIONS" :key="height" :command="height">
                  {{ height }} 倍行距
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-dropdown trigger="click" @command="setListByValue">
            <button type="button" class="document-toolbar__plain" title="列表样式">
              <el-icon><ElementPlusIcons.List /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="">
                  <span class="document-toolbar__menu-item"><span class="document-toolbar__list-marker">×</span>取消列表</span>
                </el-dropdown-item>
                <el-dropdown-item :command="`${ListType.UL}:${ListStyle.DISC}`">
                  <span class="document-toolbar__menu-item"><span class="document-toolbar__list-marker">●</span>实心圆点列表</span>
                </el-dropdown-item>
                <el-dropdown-item :command="`${ListType.UL}:${ListStyle.CIRCLE}`">
                  <span class="document-toolbar__menu-item"><span class="document-toolbar__list-marker">○</span>空心圆点列表</span>
                </el-dropdown-item>
                <el-dropdown-item :command="`${ListType.UL}:${ListStyle.SQUARE}`">
                  <span class="document-toolbar__menu-item"><span class="document-toolbar__list-marker">■</span>方块列表</span>
                </el-dropdown-item>
                <el-dropdown-item :command="`${ListType.UL}:${ListStyle.CHECKBOX}`">
                  <span class="document-toolbar__menu-item"><span class="document-toolbar__list-marker">☑</span>复选框列表</span>
                </el-dropdown-item>
                <el-dropdown-item :command="`${ListType.OL}:${ListStyle.DECIMAL}`">
                  <span class="document-toolbar__menu-item"><span class="document-toolbar__list-marker document-toolbar__list-marker--ordered">1.</span>有序列表</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        <el-divider direction="vertical" />

        <div class="document-toolbar__group">
          <el-dropdown trigger="click" @command="handleInsertCommand">
            <button type="button" class="document-toolbar__plain" title="插入内容">
              <el-icon><ElementPlusIcons.Plus /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="size in [2, 3, 4, 5]" :key="size" :command="`table:${size}:${size}`">
                  {{ size }} × {{ size }} 表格
                </el-dropdown-item>
                <el-dropdown-item divided command="image">
                  <span class="document-toolbar__menu-item"><el-icon><ElementPlusIcons.Picture /></el-icon>图片</span>
                </el-dropdown-item>
                <el-dropdown-item command="hyperlink">
                  <span class="document-toolbar__menu-item"><el-icon><ElementPlusIcons.Link /></el-icon>超链接</span>
                </el-dropdown-item>
                <el-dropdown-item divided command="separator:solid">
                  实线分隔线
                </el-dropdown-item>
                <el-dropdown-item command="separator:dashed">
                  虚线分隔线
                </el-dropdown-item>
                <el-dropdown-item command="separator:dotted">
                  点线分隔线
                </el-dropdown-item>
                <el-dropdown-item command="separator:dashDot">
                  点划线分隔线
                </el-dropdown-item>
                <el-dropdown-item divided command="pageBreak">
                  <span class="document-toolbar__menu-item"><el-icon><ElementPlusIcons.Files /></el-icon>分页符</span>
                </el-dropdown-item>
                <el-dropdown-item command="date">
                  <span class="document-toolbar__menu-item"><el-icon><ElementPlusIcons.Clock /></el-icon>当前日期</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        <el-divider direction="vertical" />

        <div class="document-toolbar__group">
          <el-popover
            v-model:visible="searchVisible"
            trigger="click"
            placement="bottom-end"
            :width="340"
            @after-enter="focusSearchInput"
            @hide="closeSearch"
          >
            <template #reference>
              <el-button text size="small" :icon="ElementPlusIcons.Search" title="查找与替换" />
            </template>
            <div class="document-search" @mousedown.stop>
              <div class="document-search__row">
                <el-input
                  ref="searchInputRef"
                  v-model="searchKeyword"
                  size="small"
                  clearable
                  placeholder="查找内容"
                  @keyup.enter="searchDocument"
                  @clear="searchDocument"
                />
                <span class="document-search__count">{{ searchResultText }}</span>
                <el-button text size="small" :icon="ElementPlusIcons.ArrowUp" title="上一个" @click="searchPrevious" />
                <el-button text size="small" :icon="ElementPlusIcons.ArrowDown" title="下一个" @click="searchNext" />
              </div>
              <div class="document-search__row">
                <el-input
                  v-model="replaceKeyword"
                  size="small"
                  placeholder="替换为"
                  @keyup.enter="replaceCurrent"
                />
                <el-button size="small" type="primary" :disabled="!searchInfo" @click="replaceCurrent">替换</el-button>
              </div>
            </div>
          </el-popover>
          <el-button text size="small" :icon="ElementPlusIcons.Printer" title="打印" @click="print" />
        </div>
        </div>
      </el-scrollbar>
      <input
        ref="imageInputRef"
        class="document-toolbar__file"
        type="file"
        accept="image/png,image/jpeg,image/gif,image/webp"
        @change="handleImageChange"
      />
    </div>
    <div class="document-canvas" :style="{ height }">
      <el-scrollbar class="document-canvas__scrollbar">
        <div class="document-canvas__stage">
          <div ref="container" class="document-canvas__inner"></div>
        </div>
      </el-scrollbar>
    </div>
  </section>
</template>

<style scoped lang="scss">
$toolbar-control-size: 28px;

.document-editor {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  width: 100%;
  min-width: 0;
  height: 100%;
  background: #eef1f5;
}
.document-toolbar {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  height: 48px;
  padding: 0 4px;
  overflow: hidden;
  background: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.document-toolbar__scrollbar {
  width: 100%;
  height: 100%;
  :deep(.el-scrollbar__wrap) {
    height: 100%;
  }
  :deep(.el-scrollbar__view) {
    display: flex;
    height: 100%;
    align-items: center;
  }
}
.document-toolbar__content {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  justify-content: center;
  width: max-content;
  min-width: 100%;
  height: 100%;
  gap: 1px;
}
.document-toolbar__group {
  display: flex;
  align-items: center;
  gap: 1px;
}
.document-toolbar {
  :deep(.el-button) {
    box-sizing: border-box;
    flex: none;
    width: $toolbar-control-size;
    height: $toolbar-control-size;
    margin: 0 !important;
    padding: 0 !important;
    border: 0 !important;
  }
  :deep(.el-button:hover) {
    background: var(--el-fill-color-light);
  }
  :deep(.el-button.is-active),
  .document-toolbar__plain.is-active {
    color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
  }
  :deep(.el-divider--vertical) {
    height: 18px;
    margin: 0 2px;
  }
}
.document-toolbar__select,
.document-toolbar__plain {
  display: inline-flex;
  box-sizing: border-box;
  flex: none;
  height: $toolbar-control-size;
  align-items: center;
  justify-content: center;
  gap: 3px;
  padding: 0 6px;
  color: var(--el-text-color-regular);
  font: inherit;
  font-size: 12px;
  background: transparent;
  border: 0;
  border-radius: 4px;
  cursor: pointer;
  &:hover {
    color: var(--el-color-primary);
    background: var(--el-fill-color-light);
  }
  .el-icon {
    font-size: 11px;
  }
}
.document-toolbar__select {
  min-width: 68px;
  justify-content: space-between;
  &--size {
    min-width: 54px;
  }
  &--title {
    min-width: 82px;
  }
}
.document-toolbar__plain {
  width: $toolbar-control-size;
  min-width: $toolbar-control-size;
  padding: 0;
}
.document-toolbar__color-button {
  position: relative;
  display: inline-flex;
  box-sizing: border-box;
  flex: 0 0 $toolbar-control-size;
  width: $toolbar-control-size;
  height: $toolbar-control-size;
  align-items: center;
  justify-content: center;
  padding: 0;
  color: var(--el-text-color-regular);
  background: transparent;
  border: 0;
  border-radius: 4px;
  cursor: pointer;
  line-height: 1;
  &:hover {
    background: var(--el-fill-color-light);
  }
}
.document-toolbar__color-letter {
  height: 19px;
  font-family: Arial, sans-serif;
  font-size: 16px;
  font-weight: 600;
  line-height: 1;
}
.document-toolbar__background-icon {
  height: 18px;
  font-size: 16px;
}
.document-toolbar__color-strip {
  position: absolute;
  right: 5px;
  bottom: 4px;
  left: 5px;
  height: 3px;
  border-radius: 1px;
  box-shadow: inset 0 0 0 1px rgb(0 0 0 / 8%);
}
.document-color-panel {
  display: grid;
  grid-template-columns: repeat(5, 30px);
  gap: 7px;
  align-items: center;
}
.document-color-panel__swatch,
.document-color-panel__custom {
  position: relative;
  display: flex;
  box-sizing: border-box;
  width: 30px;
  height: 30px;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  cursor: pointer;
  &:hover {
    border-color: var(--el-color-primary);
  }
  &.is-active {
    box-shadow: 0 0 0 2px var(--el-color-primary-light-5);
    border-color: var(--el-color-primary);
  }
}
.document-color-panel__custom {
  color: var(--el-text-color-secondary);
  font-size: 18px;
  input {
    position: absolute;
    width: 0;
    height: 0;
    opacity: 0;
    pointer-events: none;
  }
}
.document-color-panel__clear {
  grid-column: span 4;
  justify-self: start;
  padding: 0;
}
.document-toolbar__alignment-icon {
  width: 18px;
  height: 18px;
  stroke: currentColor;
  stroke-width: 1.5;
  stroke-linecap: round;
}
.document-toolbar__alignment-option {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.document-toolbar__menu-item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 132px;
  .el-icon {
    width: 16px;
    font-size: 15px;
  }
}
.document-toolbar__underline-sample {
  display: inline-flex;
  width: 16px;
  justify-content: center;
  font-family: serif;
  line-height: 18px;
  text-decoration-line: underline;
  &--double {
    text-decoration-style: double;
  }
  &--dashed {
    text-decoration-style: dashed;
  }
  &--dotted {
    text-decoration-style: dotted;
  }
  &--wavy {
    text-decoration-style: wavy;
  }
}
.document-toolbar__list-marker {
  display: inline-flex;
  width: 16px;
  justify-content: center;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  &--ordered {
    font-size: 12px;
  }
}
.document-toolbar__file {
  display: none;
}
.document-search {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.document-search__row {
  display: flex;
  align-items: center;
  gap: 6px;
  .el-input {
    flex: 1 1 auto;
  }
}
.document-search__count {
  flex: none;
  min-width: 40px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  text-align: center;
}
.document-canvas {
  position: relative;
  box-sizing: border-box;
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
}
.document-canvas__scrollbar {
  height: 100%;
}
.document-canvas__stage {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  box-sizing: border-box;
  width: 100%;
  min-width: 100%;
  min-height: 100%;
  padding: 24px;
}
.document-canvas__inner {
  flex: none;
  min-height: 0;
}
</style>
