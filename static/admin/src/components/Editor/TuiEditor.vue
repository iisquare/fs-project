<template>
  <div ref="editor" class="fs-tui-editor"></div>
</template>

<script>
import TuiEditor from '@toast-ui/editor'
import '@toast-ui/editor/dist/toastui-editor.css'
import '@toast-ui/editor/dist/i18n/zh-cn.js'

import 'prismjs/themes/prism.css'
import '@toast-ui/editor-plugin-code-syntax-highlight/dist/toastui-editor-plugin-code-syntax-highlight.css'
import Prism from 'prismjs'
import CodeSyntaxHighlight from '@toast-ui/editor-plugin-code-syntax-highlight'

import '@toast-ui/editor-plugin-table-merged-cell/dist/toastui-editor-plugin-table-merged-cell.css'
import TableMergedCell from '@toast-ui/editor-plugin-table-merged-cell'

export default {
  name: 'TuiEditor',
  props: {
    value: { type: String, default: '' },
    height: { type: Number, default: 500 }
  },
  data () {
    return {
      editor: null
    }
  },
  methods: {
    setContent (content) {
      this.editor.setMarkdown(content)
    },
    getContent () {
      return this.editor.getMarkdown()
    },
    getHtml () {
      return this.editor.getHtml()
    },
    load () {
      this.editor = new TuiEditor({
        el: this.$refs.editor,
        height: `${this.height}px`,
        initialEditType: 'wysiwyg',
        previewStyle: 'vertical',
        initialValue: this.value,
        language: 'zh-CN',
        plugins: [[CodeSyntaxHighlight, { highlighter: Prism }], TableMergedCell]
      })
    }
  },
  mounted () {
    this.$nextTick(() => { this.load() })
  },
  destroyed () {
    this.$emit('input', this.getContent())
    this.editor.destroy()
  }
}
</script>

<style lang="less">
.fs-tui-editor {
  line-height: normal;
}
</style>
