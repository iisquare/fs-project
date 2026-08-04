<script setup lang="ts">
/**
 * 代码编辑器 - 基于 CodeMirror 的代码编辑组件，支持语法高亮、代码折叠、搜索替换、自动提示等功能。
 *
 * @v-model  {String}          编辑器内容（双向绑定主值）
 * @prop     {String}           mode         - 语法模式，默认 'null'，可选 'javascript', 'sql' 等
 * @prop     {Number}           height       - 编辑器高度(px)，默认 500
 * @prop     {String}           theme        - 主题，默认 'base16-light'，可选 'ayu-dark'
 * @prop     {Boolean}          foldGutter   - 是否显示代码折叠，默认 false
 * @prop     {Boolean}          lineNumbers  - 是否显示行号，默认 true
 * @prop     {Boolean}          lineWrapping - 是否自动换行，默认 true
 * @prop     {Boolean}          resizable    - 是否允许拖拽调整高度，默认 false
 * @prop     {HintItem[]}       hints        - 自定义自动提示列表
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

import 'codemirror/addon/fold/foldgutter.css'
import 'codemirror/addon/fold/foldcode.js'
import 'codemirror/addon/fold/foldgutter.js'
import 'codemirror/addon/fold/brace-fold.js'
import 'codemirror/addon/fold/comment-fold.js'

import 'codemirror/mode/javascript/javascript'
import 'codemirror/mode/sql/sql'
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'

const model: any = defineModel()
const {
  mode = 'null',
  height = 500,
  theme = 'base16-light',
  foldGutter = false,
  lineNumbers = true,
  lineWrapping = true,
  resizable = false,
  hints = [],
} = defineProps({
  mode: { type: String, required: false },
  height: { type: Number, required: false },
  theme: { type: String, required: false },
  foldGutter: { type: Boolean, required: false },
  lineNumbers: { type: Boolean, required: false },
  lineWrapping: { type: Boolean, required: false },
  resizable: { type: Boolean, required: false },
  hints: { type: Array<Object>, required: false },
})

watch(model, (val) => {
  if (editor && editor.getValue() !== val) {
    editor.setValue(val || '')
  }
})
const editorRef = ref()
let editor: any = null
const currentHeight = ref(height)
let isResizing = false
let startY = 0
let startHeight = 0

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
const handleHint = () => {
  const cursor = editor.getCursor()
  const line = editor.getLine(cursor.line)
  let word = ''
  for (let index = cursor.ch - 1; index >= 0; index--) {
    const char = line.charAt(index)
    if (!new RegExp('[\\w\\d_\\-\\.`]').test(char)) break
    word = char + word
  }
  let list: any = []
  if (word.length > 0) {
    list = hints.filter((item: any) => item.displayText.toUpperCase().indexOf(word.toUpperCase()) >= 0)
  }
  list = list.map((item: any) => Object.assign(item, { render: hintRender }))
  const token = editor.getTokenAt(cursor)
  return {
    list: list,
    from: { ch: cursor.ch - word.length, line: cursor.line },
    to: { ch: token.end, line: cursor.line }
  }
}
const hintRender = (elt: any, data: any, cur: any) => {
  const wrapper = document.createElement('div')
  wrapper.innerHTML = cur.displayText
  elt.appendChild(wrapper)
}
const load = () => {
  editor = CodeMirror(editorRef.value, {
    value: model.value || '',
    mode: mode,
    theme: theme,
    foldGutter: foldGutter,
    lineNumbers: lineNumbers,
    lineWrapping: lineWrapping,
    hintOptions: {
      completeSingle: false,
      hint: handleHint
    },
    gutters: ['CodeMirror-linenumbers', 'CodeMirror-foldgutter'],
  })
  editor.setSize('auto', currentHeight.value + 'px')
  editor.on('change', () => {
    model.value = getContent()
  })
  editor.on('inputRead', (cm: any) => cm.showHint())
  refresh()
}
onMounted(() => {
  nextTick(() => {
    load()
  })
})
onUnmounted(() => {
  model.value = getContent()
  document.removeEventListener('mousemove', onResizeMouseMove)
  document.removeEventListener('mouseup', onResizeMouseUp)
})
defineExpose({ getContent, setContent })
</script>

<template>
  <div ref="editorRef" class="fs-code-editor" :class="{ 'fs-code-editor--resizable': resizable }">
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

  &__resize-handle {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    height: 8px;
    cursor: ns-resize;
    background: transparent;
    z-index: 10;

    &:hover,
    &:active {
      background: var(--el-color-primary, #409eff);
      opacity: 0.3;
    }
  }

  :deep(.CodeMirror) {
    height: auto;
  }
}
</style>
