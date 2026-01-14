<script setup lang="ts">
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
  hints = [],
  volatile,
} = defineProps({
  mode: { type: String, required: false },
  height: { type: Number, required: false },
  theme: { type: String, required: false },
  foldGutter: { type: Boolean, required: false },
  lineNumbers: { type: Boolean, required: false },
  lineWrapping: { type: Boolean, required: false },
  hints: { type: Array<Object>, required: false }, // { className: 'li的类名', displayText: '联想展示内容', text: '实际插入内容' }
  volatile: { required: false },
})

watch(() => volatile, () => {
  setContent(model.value)
})
const editorRef = ref()
let editor: any = null
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
    value: model.value,
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
  editor.setSize('auto', height + 'px')
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
})
defineExpose({ getContent, setContent })
</script>

<template>
  <div ref="editorRef" class="fs-code-editor"></div>
</template>

<style lang="scss" scoped>
.fs-code-editor {
  width: 100%;
  line-height: normal;
}
</style>
