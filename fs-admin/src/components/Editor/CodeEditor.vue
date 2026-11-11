<script setup lang="ts">
/**
 * 代码编辑器 - 基于 CodeMirror 的代码编辑组件，支持语法高亮、代码折叠、搜索替换、自动提示等功能。
 *
 * @v-model  {String}          编辑器内容（双向绑定主值）
 * @prop     {String}           mode         - 语法模式，默认 'null'，可选 'javascript', 'application/json', 'sql' 等
 * @prop     {Number}           height       - 编辑器高度(px)，默认 500
 * @prop     {String}           theme        - 主题，默认 'base16-light'，可选 'ayu-dark'
 * @prop     {Boolean}          foldGutter   - 是否显示代码折叠，默认 false
 * @prop     {Boolean}          lineNumbers  - 是否显示行号，默认 true
 * @prop     {Boolean}          lineWrapping - 是否自动换行，默认 true
 * @prop     {Boolean}          readonly     - 是否只读查看，默认 false（仍允许聚焦、选择和复制）
 * @prop     {Boolean}          resizable    - 是否允许拖拽调整高度，默认 false
 * @prop     {Boolean}          fill         - 是否铺满父容器高度，默认 false
 * @prop     {Boolean}          bordered     - 是否显示输入框式边框，默认 true；
 *                                              铺满容器的场景（如 SQL 编辑器）传 false，去掉边框与圆角
 * @prop     {String}           background   - 编辑器底色，默认白色（与面板里的输入框一致）；
 *                                              传颜色值（如 #f5f5f5）或 transparent / inherit 可覆盖
 * @prop     {Number}           fontSize     - 编辑器字号(px)，默认 0 表示继承外层（属性面板基准字号）
 * @prop     {String}           fontFamily   - 编辑器字体，默认 monospace（代码场景）；
 *                                              编辑散文类内容（提示词、回复内容）时传界面字体更易读
 * @prop     {Number}           lineHeight   - 行高倍数，默认 0 表示沿用主题默认（代码场景紧凑）；
 *                                              散文类内容传 1.5 之类更松快
 * @prop     {String}           placeholder  - 空白占位提示文字
 * @prop     {HintItem[]}       hints        - 自定义自动提示列表
 * @prop     {String}           trigger      - 唤起提示的触发字符，如 '/'；输入该字符时抛出 trigger 事件，由调用方决定提示形态
 * @prop     {Object}           keys         - 追加按键处理，形如 `{ Up: () => true }`：
 *                                             返回 true 表示已处理（阻止默认行为），返回 false 交回编辑器默认行为；
 *                                             输入法合成（如中文输入）期间一律不拦截
 * @emits    trigger - 输入触发字符时抛出，调用方可据此在光标处展开提示面板
 *
 * 提示项结构 (HintItem):
 *   { className: string, displayText: string, text: string }
 *   className    - li 元素的 CSS 类名
 *   displayText  - 联想列表展示内容
 *   text         - 选中后实际插入的文本
 *
 * @example
 * <!-- 基础用法 -->
 * <code-editor v-model="code" mode="javascript" />
 *
 * <!-- 带自定义提示 -->
 * <code-editor v-model="sql" mode="sql" :hints="sqlHints" />
 *
 * <!-- 只读 JSON 查看器（支持语法高亮与折叠） -->
 * <code-editor :model-value="jsonText" mode="application/json" readonly fold-gutter />
 *
 * <!-- 变量占位符以标签展示（内容仍是 {{#名称#}} 文本） -->
 * <code-editor ref="editorRef" v-model="text" trigger="/" @trigger="openPanel" />
 * editorRef.value.renderTokens(/\{\{#([^#{}]+)#\}\}/g, 'cm-token', (match) => '展示名称')
 */
import 'codemirror/lib/codemirror.css'
import 'codemirror/theme/ayu-dark.css'
import 'codemirror/theme/base16-light.css'
import CodeMirror from 'codemirror'

import 'codemirror/addon/scroll/annotatescrollbar.js'
import 'codemirror/addon/search/matchesonscrollbar.js'
import 'codemirror/addon/search/match-highlighter.js'
import 'codemirror/addon/search/jump-to-line.js'

import 'codemirror/addon/dialog/dialog.js'
import 'codemirror/addon/dialog/dialog.css'
import 'codemirror/addon/search/searchcursor.js'
import 'codemirror/addon/search/search.js'

import 'codemirror/addon/hint/show-hint.css'
import 'codemirror/addon/hint/show-hint.js'

import 'codemirror/addon/display/placeholder.js'

import 'codemirror/addon/fold/foldgutter.css'
import 'codemirror/addon/fold/foldcode.js'
import 'codemirror/addon/fold/foldgutter.js'
import 'codemirror/addon/fold/brace-fold.js'
import 'codemirror/addon/fold/comment-fold.js'

import 'codemirror/mode/javascript/javascript'
import 'codemirror/mode/sql/sql'
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'

// JSON/JavaScript 模式默认没有 fold helper，显式注册括号折叠器。
if ((CodeMirror as any).fold?.brace) {
  CodeMirror.registerHelper('fold', 'json', (CodeMirror as any).fold.brace)
  CodeMirror.registerHelper('fold', 'javascript', (CodeMirror as any).fold.brace)
}

const model: any = defineModel()
const emit = defineEmits(['trigger'])
const {
  mode = 'null',
  height = 500,
  theme = 'base16-light',
  foldGutter = false,
  lineNumbers = true,
  lineWrapping = true,
  readonly = false,
  resizable = false,
  fill = false,
  bordered = true,
  background = '',
  fontSize = 0,
  fontFamily = '',
  lineHeight = 0,
  placeholder = '',
  hints = [],
  trigger = '',
  keys = {},
} = defineProps({
  mode: { type: String, required: false },
  height: { type: Number, required: false },
  theme: { type: String, required: false },
  foldGutter: { type: Boolean, required: false },
  lineNumbers: { type: Boolean, required: false },
  lineWrapping: { type: Boolean, required: false },
  readonly: { type: Boolean, required: false },
  resizable: { type: Boolean, required: false },
  fill: { type: Boolean, required: false },
  bordered: { type: Boolean, required: false },
  background: { type: String, required: false },
  fontSize: { type: Number, required: false },
  fontFamily: { type: String, required: false },
  lineHeight: { type: Number, required: false },
  placeholder: { type: String, required: false },
  hints: { type: Array<Object>, required: false },
  trigger: { type: String, required: false },
  keys: { type: Object, required: false },
})

watch(model, (val) => {
  if (editor && editor.getValue() !== val) {
    editor.setValue(val || '')
  }
})
const editorRef = ref()
let editor: any = null
let foldMarkerClickHandler: any = null
// 监听容器尺寸变化，容器在抽屉/标签页过渡动画结束后尺寸才稳定，
// CodeMirror 不会自动感知尺寸变化，需触发 refresh() 重绘，否则内容不显示
let resizeObserver: any = null
const currentHeight = ref(height)
/** 编辑器字体：默认等宽（代码场景），变量编辑器等散文场景由调用方传入界面字体 */
const editorFont = computed(() => fontFamily || 'monospace')
/** 行高：未传时沿用主题默认，传入倍数（如 1.5）用于散文类内容 */
const editorLineHeight = computed(() => (lineHeight ? String(lineHeight) : 'normal'))
/** 编辑器底色：默认白面（与面板里的输入框一致），可由调用方覆盖或设为 transparent 融进容器 */
const editorBackground = computed(() => background || 'var(--fs-panel-surface)')
let isResizing = false
let startY = 0
let startHeight = 0
// 标签化展示的匹配规则与生成的标记，匹配内容仍保留在文本中，仅改变展示
let currentTokenPattern: RegExp | null = null
let currentTokenClass = 'cm-token'
let currentTokenLabel: any = null
let markers: any[] = []
let applyingTokens = false
// 标签内的小图标：一个带孔的标签形状，用于区分变量占位符与普通文本
const TOKEN_ICON = [
  '<svg viewBox="0 0 16 16" width="12" height="12" aria-hidden="true">',
  '<path d="M2.6 4.6a2 2 0 0 1 2-2h4.9a2 2 0 0 1 1.5.7l2.2 2.5a2 2 0 0 1 0 2.4l-2.2 2.5a2 2 0 0 1-1.5.7H4.6a2 2 0 0 1-2-2z"',
  ' fill="none" stroke="currentColor" stroke-width="1.3" stroke-linejoin="round" />',
  '<circle cx="5.5" cy="7.5" r="1.05" fill="currentColor" />',
  '</svg>',
].join('')

const clearTokens = () => {
  markers.forEach((marker: any) => marker.clear())
  markers = []
}
/**
 * 把匹配内容渲染为标签：正则的首个捕获组作为标签文案，未包含捕获组时展示整段匹配内容
 */
const applyTokens = () => {
  if (!editor || applyingTokens) return
  applyingTokens = true
  try {
    clearTokens()
    if (!currentTokenPattern) return
    const flags = currentTokenPattern.flags.indexOf('g') >= 0 ? currentTokenPattern.flags : currentTokenPattern.flags + 'g'
    const pattern = new RegExp(currentTokenPattern.source, flags)
    const content = editor.getValue()
    let match: any = pattern.exec(content)
    while (match) {
      if (!match[0]) break
      const chip = document.createElement('span')
      chip.className = `${currentTokenClass} CodeMirror-widget`
      chip.title = match[0]
      const icon = document.createElement('span')
      icon.className = `${currentTokenClass}__icon`
      icon.innerHTML = TOKEN_ICON
      const label = document.createElement('span')
      label.className = `${currentTokenClass}__text`
      label.textContent = currentTokenLabel ? currentTokenLabel(match) : (match[1] ?? match[0])
      chip.appendChild(icon)
      chip.appendChild(label)
      const from = editor.posFromIndex(match.index)
      const to = editor.posFromIndex(match.index + match[0].length)
      markers.push(editor.markText(from, to, { replacedWith: chip, atomic: true }))
      match = pattern.exec(content)
    }
  } catch (e) {
    // 标签渲染失败不影响编辑，保留原文本文本
    clearTokens()
  } finally {
    applyingTokens = false
  }
}
/**
 * 设置标签化展示规则，传 null 清除
 * @param {RegExp}   pattern   匹配规则，首个捕获组为标签标识
 * @param {String}   className 标签的 CSS 类名
 * @param {Function} label     标签文案，入参为匹配结果，缺省取首个捕获组
 */
const renderTokens = (pattern: RegExp | null = null, className = 'cm-token', label: any = null) => {
  currentTokenPattern = pattern
  currentTokenClass = className
  currentTokenLabel = label
  applyTokens()
}

const onResizeMouseDown = (e: MouseEvent) => {
  isResizing = true
  startY = e.clientY
  startHeight = currentHeight.value
  document.addEventListener('mousemove', onResizeMouseMove)
  document.addEventListener('mouseup', onResizeMouseUp)
  e.preventDefault()
}

const onResizeMouseMove = (e: MouseEvent) => {
  if (!isResizing) return
  const delta = e.clientY - startY
  currentHeight.value = Math.max(100, startHeight + delta)
  editor?.setSize('auto', currentHeight.value + 'px')
}

const onResizeMouseUp = () => {
  isResizing = false
  document.removeEventListener('mousemove', onResizeMouseMove)
  document.removeEventListener('mouseup', onResizeMouseUp)
}

const setContent = (content: any) => {
  editor?.setValue(content)
}
const getContent = () => {
  return editor?.getValue()
}
const refresh = () => {
  window.setTimeout(() => editor?.refresh(), 100)
}
const replaceSelection = (text: any) => {
  editor?.replaceSelection(text)
  editor?.focus()
}
const getSelection = () => {
  return editor?.getSelection() || ''
}
const getCursor = () => {
  return editor?.getCursor() ?? null
}
const getLine = (line: number) => {
  return editor?.getLine(line) ?? ''
}
/** 光标位置（窗口坐标），用于把提示面板定位到光标下方 */
const cursorCoords = () => {
  if (!editor) return null
  const coords = editor.cursorCoords(true, 'window')
  return { left: coords.left, top: coords.top, bottom: coords.bottom }
}
const replaceRange = (text: string, from: any, to: any) => {
  editor?.replaceRange(text, from, to)
}
const focus = () => {
  editor?.focus()
}
const foldAll = () => {
  editor?.execCommand('foldAll')
}
const unfoldAll = () => {
  editor?.execCommand('unfoldAll')
}

// 输入法合成状态：合成期间不拦截按键，保证中文等输入法正常使用
let composing = false
const onCompositionStart = () => { composing = true }
const onCompositionEnd = () => { composing = false }
/**
 * 组装 extraKeys：外部通过 keys 传入 `{ Up: () => boolean }`，
 * 返回 true 表示已处理（阻止编辑器默认行为），返回 false 交回编辑器默认行为
 */
const buildExtraKeys = () => {
  const result: any = {}
  Object.keys(keys ?? {}).forEach((name: string) => {
    const handler: any = (keys as any)[name]
    if ('function' !== typeof handler) return
    result[name] = () => {
      if (composing) return CodeMirror.Pass
      return false === handler() ? CodeMirror.Pass : undefined
    }
  })
  return result
}
const filterHints = (word: string) => {
  const matched = word
    ? hints.filter((item: any) => String(item.displayText).toUpperCase().indexOf(word.toUpperCase()) >= 0)
    : hints
  return matched.map((item: any) => Object.assign({}, item, { render: hintRender }))
}
const handleHint = () => {
  const cursor = editor.getCursor()
  const line = editor.getLine(cursor.line)
  let word = ''
  for (let index = cursor.ch - 1; index >= 0; index--) {
    const char = line.charAt(index)
    if (/[\s,()=;'"<>+*\/]/.test(char)) break
    word = char + word
  }
  const token = editor.getTokenAt(cursor)
  return {
    list: word.length > 0 ? filterHints(word) : [],
    from: { ch: cursor.ch - word.length, line: cursor.line },
    to: { ch: token.end, line: cursor.line }
  }
}
const hintRender = (elt: any, data: any, cur: any) => {
  const wrapper = document.createElement('div')
  wrapper.textContent = cur.displayText
  elt.appendChild(wrapper)
}
const bindFoldMarkerClick = () => {
  if (!editor || !foldGutter || foldMarkerClickHandler) return
  const scroller = editor.getScrollerElement()
  foldMarkerClickHandler = (event: MouseEvent) => {
    if (event.button !== 0) return
    const target = event.target as HTMLElement | null
    const marker = target?.closest('.CodeMirror-foldgutter-open, .CodeMirror-foldgutter-folded')
    if (!marker) return
    event.preventDefault()
    event.stopImmediatePropagation()
    const line = editor.lineAtHeight(event.clientY, 'window')
    editor.foldCode(CodeMirror.Pos(line, 0), { rangeFinder: CodeMirror.fold.brace })
  }
  scroller.addEventListener('mousedown', foldMarkerClickHandler, true)
}
const load = () => {
  // 只有真正需要时才保留 gutter，避免 lineNumbers 关闭时左侧留一条空灰条
  const gutters: string[] = []
  if (lineNumbers) gutters.push('CodeMirror-linenumbers')
  if (foldGutter) gutters.push('CodeMirror-foldgutter')
  editor = CodeMirror(editorRef.value, {
    value: model.value || '',
    mode: mode,
    theme: theme,
    foldGutter: foldGutter ? { rangeFinder: CodeMirror.fold.brace } : false,
    lineNumbers: lineNumbers,
    lineWrapping: lineWrapping,
    readOnly: readonly,
    placeholder: placeholder,
    hintOptions: {
      completeSingle: false,
      hint: handleHint
    },
    gutters: gutters,
  })
  bindFoldMarkerClick()
  editor.setSize('auto', fill ? '100%' : currentHeight.value + 'px')
  editor.setOption('extraKeys', buildExtraKeys())
  const inputField: any = editor.getInputField()
  inputField?.addEventListener('compositionstart', onCompositionStart)
  inputField?.addEventListener('compositionend', onCompositionEnd)
  editor.on('change', (cm: any, change: any) => {
    model.value = getContent()
    // 文本变化后重建变量等标签标记，保证新增/删除的占位符同步展示
    applyTokens()
    // 输入触发字符时抛出事件：change 时机晚于 inputRead，文本已落地，调用方可据此展开自定义提示面板
    const inserted = String(change?.text?.[0] ?? '')
    if (trigger && '+input' === change?.origin && inserted.indexOf(trigger) >= 0) {
      emit('trigger', { char: trigger })
    }
  })
  editor.on('inputRead', (cm: any) => cm.showHint())
  applyTokens()
  refresh()
}
onMounted(() => {
  nextTick(() => {
    load()
    if (editorRef.value) {
      resizeObserver = new ResizeObserver(() => refresh())
      resizeObserver.observe(editorRef.value)
    }
  })
})
onUnmounted(() => {
  if (!readonly) model.value = getContent()
  clearTokens()
  const inputField: any = editor?.getInputField?.()
  inputField?.removeEventListener('compositionstart', onCompositionStart)
  inputField?.removeEventListener('compositionend', onCompositionEnd)
  resizeObserver?.disconnect()
  if (foldMarkerClickHandler) {
    editor?.getScrollerElement?.()?.removeEventListener('mousedown', foldMarkerClickHandler, true)
    foldMarkerClickHandler = null
  }
  document.removeEventListener('mousemove', onResizeMouseMove)
  document.removeEventListener('mouseup', onResizeMouseUp)
})
defineExpose({
  getContent, setContent, replaceSelection, getSelection, renderTokens,
  getCursor, getLine, cursorCoords, replaceRange, focus, foldAll, unfoldAll,
})
</script>

<template>
  <div
    ref="editorRef"
    class="fs-code-editor"
    :class="[
      {
        'fs-code-editor--resizable': resizable,
        'fs-code-editor--fill': fill,
        'fs-code-editor--bordered': bordered,
      },
    ]"
    :style="fontSize ? { fontSize: fontSize + 'px' } : undefined">
    <div v-if="resizable" class="fs-code-editor__resize-handle" @mousedown="onResizeMouseDown" />
  </div>
</template>

<style lang="scss" scoped>
.fs-code-editor {
  width: 100%;
  line-height: normal;

  &--resizable {
    position: relative;
    padding-bottom: 8px;
  }

  &--fill {
    height: 100%;

    :deep(.CodeMirror) {
      height: 100%;
    }
  }

  &__resize-handle {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    height: 8px;
    cursor: ns-resize;
    background: transparent;
    z-index: 10;

    /* 可见的抓握提示：整条 8px 仍是拖拽热区，只在中间画一小节提示条 */
    &::after {
      content: '';
      position: absolute;
      top: 50%;
      left: 50%;
      width: 34px;
      height: 4px;
      border-radius: 2px;
      background: var(--el-border-color);
      transform: translate(-50%, -50%);
      transition: background-color 0.2s, width 0.2s;
    }
    &:hover::after,
    &:active::after {
      width: 48px;
      background: var(--el-color-primary);
    }
  }

  /**
   * 编辑器外观对齐面板里的输入框：白面 + 内边距 + 界面字体。
   * 主题（base16-light）自带的 #f5f5f5 灰底与页面里的灰块同族，这里统一成输入框的白面。
   */
  :deep(.CodeMirror) {
    height: auto;
    box-sizing: border-box;
    padding: 0;
    background: v-bind(editorBackground);
    color: var(--el-text-color-primary);
    font-family: v-bind(editorFont);
    line-height: v-bind(editorLineHeight);
    transition: border-color 0.2s, box-shadow 0.2s;
  }
  /* 默认带边框：1px 描边 + 圆角，聚焦时主色描边与浅色外圈 */
  &--bordered :deep(.CodeMirror) {
    border: solid 1px var(--el-border-color);
    border-radius: 4px;
    &:hover {
      border-color: var(--el-border-color-dark);
    }
    &.CodeMirror-focused {
      border-color: var(--el-color-primary);
      box-shadow: 0 0 0 2px var(--el-color-primary-light-9);
    }
  }
  /* 不要边框时（如 SQL 编辑器铺满容器）连聚焦描边也一并去掉：
     这类编辑器本身就是整块编辑区，光标即焦点提示，不再画蓝色边框 */
  :deep(.CodeMirror-lines) {
    /* 文字不再贴边，留出与输入框一致的内边距 */
    padding: 8px 10px;
  }
  :deep(.CodeMirror-gutters) {
    border-right: none;
    background: transparent;
  }
  :deep(.CodeMirror-foldgutter) {
    width: 12px;
  }
  :deep(.CodeMirror-foldgutter-open),
  :deep(.CodeMirror-foldgutter-folded) {
    cursor: pointer;
    pointer-events: auto;
  }
  /* 行号列默认 min-width: 20px，只有个位数行号时左侧会空出一大块；改为按行号内容自适应 */
  :deep(.CodeMirror-linenumber) {
    min-width: 0;
  }
  /* 编辑器内部滚动条：细样式，与面板滚动条一致 */
  :deep(.CodeMirror-vscrollbar),
  :deep(.CodeMirror-hscrollbar) {
    &::-webkit-scrollbar {
      width: 8px;
      height: 8px;
    }
    &::-webkit-scrollbar-thumb {
      border-radius: 4px;
      background: var(--el-border-color);
      &:hover {
        background: var(--el-border-color-dark);
      }
    }
    &::-webkit-scrollbar-track {
      background: transparent;
    }
  }

  :deep(.CodeMirror-placeholder) {
    color: var(--el-text-color-placeholder);
  }

  /* 变量等占位符标签：底层文本仍是 {{#节点标识.变量名#}}，展示为中文名称小胶囊 */
  :deep(.cm-token) {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    height: 18px;
    padding: 0 5px;
    border-radius: 4px;
    border: solid 1px var(--el-color-primary-light-7);
    background: var(--el-color-primary-light-9);
    color: var(--el-color-primary);
    font-size: 12px;
    font-weight: 500;
    line-height: 1;
    vertical-align: middle;
    transition: background-color 0.2s, border-color 0.2s;
    &:hover {
      border-color: var(--el-color-primary-light-5);
      background: var(--el-color-primary-light-8);
    }
    .cm-token__icon {
      display: inline-flex;
      flex: none;
      opacity: 0.85;
    }
    .cm-token__text {
      max-width: 180px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}
</style>
