<script setup lang="ts">
import Vditor from 'vditor'
import 'vditor/dist/index.css'
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const model: any = defineModel()
const {
  readonly = false,
  height = 400,
  placeholder = '',
} = defineProps({
  readonly: { type: Boolean, required: false },
  height: { type: Number, required: false },
  placeholder: { type: String, required: false },
})

const editorRef = ref<HTMLDivElement>()
const previewRef = ref<HTMLDivElement>()
let vditor: Vditor | null = null

const getContent = () => {
  return vditor?.getValue() ?? model.value ?? ''
}

const setContent = (content: string) => {
  vditor?.setValue(content ?? '')
}

const loadEditor = () => {
  if (!editorRef.value) return
  vditor = new Vditor(editorRef.value, {
    height: height + 'px',
    mode: 'wysiwyg',
    lang: 'zh_CN',
    value: model.value ?? '',
    placeholder: placeholder,
    cache: { enable: false },
    toolbar: [
      'headings', 'bold', 'italic', 'strike', '|',
      'list', 'ordered-list', 'check', '|',
      'quote', 'code', 'inline-code', 'table', 'line', '|',
      'link', '|',
      'undo', 'redo', '|',
      'edit-mode', 'outline', 'fullscreen',
    ],
    input(value: string) {
      model.value = value
    },
    after() {
      if (model.value) {
        vditor!.setValue(model.value)
      }
    },
  })
}

const loadPreview = () => {
  if (!previewRef.value) return
  const content = model.value ?? ''
  if (!content) {
    previewRef.value.innerHTML = '<span style="color: var(--el-text-color-placeholder)">暂无内容</span>'
    return
  }
  Vditor.preview(previewRef.value, content, {
    mode: 'light',
    hljs: { lineNumber: true },
  })
}

watch(() => model.value, () => {
  if (readonly) {
    nextTick(() => loadPreview())
  }
})

onMounted(() => {
  nextTick(() => {
    if (readonly) {
      loadPreview()
    } else {
      loadEditor()
    }
  })
})

onBeforeUnmount(() => {
  if (vditor) {
    model.value = vditor.getValue()
    vditor.destroy()
    vditor = null
  }
})

defineExpose({ getContent, setContent })
</script>

<template>
  <div class="fs-markdown-editor">
    <div ref="previewRef" class="fs-markdown-preview vditor-reset" v-if="readonly"></div>
    <div ref="editorRef" v-else></div>
  </div>
</template>

<style lang="scss" scoped>
.fs-markdown-editor {
  width: 100%;
  line-height: normal;
}
.fs-markdown-preview {
  padding: 8px 0;
  word-break: break-word;
  :deep(p:last-child) {
    margin-bottom: 0;
  }
}
</style>
