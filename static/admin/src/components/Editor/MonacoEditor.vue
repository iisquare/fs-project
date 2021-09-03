<template>
  <div ref="editor" :style="{ height: `${height}px`}"></div>
</template>

<script>
import * as monaco from 'monaco-editor'

export default {
  name: 'MonacoEditor',
  props: {
    value: { type: String, default: '' },
    language: { type: String, default: 'plaintext' },
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
      this.editor = monaco.editor.create(this.$refs.editor, {
        value: this.value,
        language: this.language,
        theme: 'vs-dark'
      })
    }
  },
  mounted () {
    this.$nextTick(() => { this.load() })
  },
  destroyed () {
    this.$emit('input', this.getContent())
    this.editor.getModel().dispose()
  }
}
</script>

<style lang="less">
</style>
