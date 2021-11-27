<template>
  <div ref="editor" class="fs-code-editor"></div>
</template>

<script>
import 'codemirror/lib/codemirror.css'
import 'codemirror/theme/ayu-dark.css'
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

import 'codemirror/mode/javascript/javascript'
import 'codemirror/mode/sql/sql'

export default {
  name: 'CodeEditor',
  props: {
    value: { type: String, default: '' },
    mode: { type: String, default: 'null' },
    height: { type: Number, default: 500 },
    theme: { type: String, default: 'ayu-dark' },
    lineNumbers: { type: Boolean, default: true },
    lineWrapping: { type: Boolean, default: true },
    hints: { type: Array, default: () => [] }, // { className: 'li的类名', displayText: '联想展示内容', text: '实际插入内容' }
    volatile: { type: [Object, String, Number], default: null }
  },
  data () {
    return {
      editor: null
    }
  },
  watch: {
    volatile () {
      this.setContent(this.value)
    }
  },
  methods: {
    setContent (content) {
      this.editor.setValue(content)
      this.refresh()
    },
    getContent () {
      return this.editor.getValue()
    },
    refresh () {
      window.setTimeout(() => this.editor.refresh(), 100)
    },
    load () {
      this.editor = CodeMirror(this.$refs.editor, {
        value: this.value,
        mode: this.mode,
        theme: this.theme,
        lineNumbers: this.lineNumbers,
        lineWrapping: this.lineWrapping,
        hintOptions: {
          completeSingle: false,
          hint: this.handleHint
        }
      })
      this.editor.setSize('auto', this.height + 'px')
      this.editor.on('change', () => {
        this.$emit('input', this.getContent())
      })
      this.editor.on('inputRead', cm => cm.showHint())
      this.refresh()
    },
    handleHint () {
      const cursor = this.editor.getCursor()
      const line = this.editor.getLine(cursor.line)
      let word = ''
      for (let index = cursor.ch - 1; index >= 0; index--) {
        const char = line.charAt(index)
        if (!new RegExp('[\\w\\d_\\-\\.`]').test(char)) break
        word = char + word
      }
      let list = []
      if (word.length > 0) {
        list = this.hints.filter(item => item.displayText.toUpperCase().indexOf(word.toUpperCase()) >= 0)
      }
      list = list.map(item => Object.assign(item, { render: this.hintRender }))
      const token = this.editor.getTokenAt(cursor)
      return {
        list: list,
        from: { ch: cursor.ch - word.length, line: cursor.line },
        to: { ch: token.end, line: cursor.line }
      }
    },
    hintRender (elt, data, cur) {
      const wrapper = document.createElement('div')
      wrapper.innerHTML = cur.displayText
      elt.appendChild(wrapper)
    }
  },
  mounted () {
    this.$nextTick(() => { this.load() })
  },
  destroyed () {
    this.$emit('input', this.getContent())
  }
}
</script>

<style lang="less">
.fs-code-editor {
  line-height: normal;
}
</style>
<style>
.CodeMirror-hints {
  z-index: 1000;
}
</style>
