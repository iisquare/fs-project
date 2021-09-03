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

export default {
  name: 'CodeEditor',
  props: {
    value: { type: String, default: '' },
    mode: { type: String, default: 'null' },
    height: { type: Number, default: 500 }
  },
  data () {
    return {
      editor: null
    }
  },
  methods: {
    setContent (content) {
      this.editor.setValue(content)
    },
    getContent () {
      return this.editor.getValue()
    },
    load () {
      this.editor = CodeMirror(this.$refs.editor, {
        value: this.value,
        mode: this.mode,
        theme: 'ayu-dark',
        lineNumbers: true
      })
      this.editor.setSize('auto', this.height + 'px')
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
