<script setup lang="ts">
import { onBeforeUnmount, ref } from 'vue'
import { renderAsync } from 'docx-preview'

const props = withDefaults(defineProps<{
  height?: string
}>(), {
  height: '100%',
})

const emit = defineEmits<{
  rendered: []
}>()

const container = ref<HTMLDivElement>()
let renderSerial = 0
let destroyed = false

const render = async (content: ArrayBuffer) => {
  const serial = ++renderSerial
  const element = container.value
  if (!element) return
  element.innerHTML = ''
  await renderAsync(new Blob([content]), element, undefined, {
    inWrapper: true,
    hideWrapperOnPrint: true,
    breakPages: true,
    ignoreWidth: false,
    ignoreHeight: false,
    ignoreFonts: false,
    ignoreLastRenderedPageBreak: true,
    renderHeaders: true,
    renderFooters: true,
    renderFootnotes: true,
    renderEndnotes: true,
    renderComments: false,
    renderChanges: false,
    useBase64URL: true,
    className: 'fs-docx-page',
  })
  if (!destroyed && serial === renderSerial) emit('rendered')
}

const clear = () => {
  renderSerial++
  if (container.value) container.value.innerHTML = ''
}

const scrollToOutline = (name: string, repeatIndex = 0) => {
  const paragraphs = Array.from(container.value?.querySelectorAll('p') || [])
  let current = 0
  for (const paragraph of paragraphs) {
    if ((paragraph.textContent || '').trim() !== name.trim()) continue
    if (current !== repeatIndex) {
      current++
      continue
    }
    paragraph.scrollIntoView({ behavior: 'smooth', block: 'center' })
    paragraph.classList.add('docx-outline-target')
    window.setTimeout(() => paragraph.classList.remove('docx-outline-target'), 1800)
    return
  }
}

const exportPdf = async (fileName: string) => {
  const sections = Array.from(container.value?.querySelectorAll<HTMLElement>('.fs-docx-page-wrapper > section') || [])
  if (!sections.length) throw new Error('当前没有可导出的文档页面')
  const [{ default: html2canvas }, { jsPDF }] = await Promise.all([
    import('html2canvas'),
    import('jspdf'),
  ])
  let pdf: any = null
  for (let index = 0; index < sections.length; index++) {
    const section = sections[index]
    const rect = section.getBoundingClientRect()
    const scale = Math.min(2, 1800 / Math.max(rect.width, 1))
    const canvas = await html2canvas(section, {
      scale,
      useCORS: true,
      backgroundColor: '#ffffff',
      logging: false,
      width: rect.width,
      height: rect.height,
    })
    const widthPt = rect.width * 0.75
    const heightPt = rect.height * 0.75
    const orientation = widthPt > heightPt ? 'landscape' : 'portrait'
    if (!pdf) {
      pdf = new jsPDF({
        orientation,
        unit: 'pt',
        format: [widthPt, heightPt],
        compress: true,
      })
    } else {
      pdf.addPage([widthPt, heightPt], orientation)
    }
    pdf.addImage(canvas.toDataURL('image/jpeg', 0.95), 'JPEG', 0, 0, widthPt, heightPt, undefined, 'FAST')
  }
  pdf?.save(fileName)
}

onBeforeUnmount(() => {
  destroyed = true
  clear()
})

defineExpose({ render, clear, scrollToOutline, exportPdf })
</script>

<template>
  <div class="docx-preview" :style="{ height }">
    <el-scrollbar class="docx-preview__scrollbar">
      <div ref="container" class="docx-preview__content"></div>
    </el-scrollbar>
  </div>
</template>

<style scoped lang="scss">
.docx-preview {
  overflow: hidden;
  background: #eef1f5;
  &__scrollbar {
    height: 100%;
  }
  &__content {
    min-height: 100%;
  }
  :deep(.fs-docx-page-wrapper) {
    align-items: center;
    padding: 24px 24px 28px;
    background: transparent;
  }
  :deep(.fs-docx-page-wrapper > section) {
    margin: 0 auto 24px;
    box-shadow: 0 2px 14px rgb(58 66 76 / 18%);
  }
  :deep(.docx-outline-target) {
    background: var(--el-color-primary-light-8);
    transition: background-color .25s ease;
  }
}

@media print {
  .docx-preview {
    overflow: visible;
    background: #fff;
  }
  .docx-preview__scrollbar,
  .docx-preview__scrollbar :deep(.el-scrollbar__wrap) {
    height: auto;
    overflow: visible;
  }
  .docx-preview__content {
    min-height: 0;
  }
  :deep(.fs-docx-page-wrapper) {
    padding: 0;
    background: #fff;
  }
  :deep(.fs-docx-page-wrapper > section) {
    margin: 0;
    box-shadow: none;
  }
  :deep(.fs-docx-page-wrapper > section:not(:last-child)) {
    break-after: page;
  }
  :deep(.fs-docx-page-wrapper > section:last-child) {
    break-after: auto;
  }
}
</style>
