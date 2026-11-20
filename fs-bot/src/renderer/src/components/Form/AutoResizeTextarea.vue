<script setup lang="ts">
/**
 * Element Plus 文本框的兼容封装。
 *
 * 当前版本在中文输入法组词期间会跳过 input / modelValue 更新，导致 autosize
 * 停在组词前的行数；这里只在组词期间按原生 textarea 的实际高度补一次撑开，
 * 组词结束后仍由 el-input 自己接管。Element Plus 修复该问题后，使用端可以直接
 * 换回 el-input，或删除这一层而不影响外部接口。
 */
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

defineOptions({ inheritAttrs: false })

type TextareaResize = 'none' | 'both' | 'horizontal' | 'vertical'

interface ElementInputExpose {
  textarea?: HTMLTextAreaElement
  focus?: () => void
}

const model = defineModel<string>({ default: '' })
const props = withDefaults(
  defineProps<{
    minRows?: number
    maxRows?: number
    resize?: TextareaResize
    disabled?: boolean
    placeholder?: string
  }>(),
  {
    minRows: 1,
    maxRows: 8,
    resize: 'none',
    disabled: false,
    placeholder: ''
  }
)
const emit = defineEmits<{
  resize: [height: number]
}>()

const inputRef = ref<ElementInputExpose | null>(null)
const rowRange = computed(() => {
  const minRows = Math.max(1, Math.floor(props.minRows))
  return {
    minRows,
    maxRows: Math.max(minRows, Math.floor(props.maxRows))
  }
})
const autosize = computed(() => rowRange.value)
let compositionResizeFrame = 0

function textarea(): HTMLTextAreaElement | null {
  return inputRef.value?.textarea ?? null
}

function scheduleCompositionResize(): void {
  if (compositionResizeFrame) cancelAnimationFrame(compositionResizeFrame)
  compositionResizeFrame = requestAnimationFrame(() => {
    compositionResizeFrame = 0
    const element = textarea()
    if (!element) return
    const style = window.getComputedStyle(element)
    const lineHeight = Number.parseFloat(style.lineHeight)
    const padding = Number.parseFloat(style.paddingTop) + Number.parseFloat(style.paddingBottom)
    if (!Number.isFinite(lineHeight) || !Number.isFinite(padding)) return
    const maxHeight = lineHeight * rowRange.value.maxRows + padding
    const height = Math.min(element.scrollHeight, maxHeight)
    element.style.height = `${height}px`
    element.style.overflowY = 'hidden'
    // 刚撑到完整行高时，浏览器会把原来的内部滚动位置收回。
    if (element.scrollHeight <= height) element.scrollTop = 0
    emit('resize', height)
  })
}

function onTextareaInput(event: Event): void {
  if ((event as InputEvent).isComposing) scheduleCompositionResize()
}

onMounted(() => {
  const element = textarea()
  element?.addEventListener('compositionupdate', scheduleCompositionResize)
  element?.addEventListener('input', onTextareaInput)
})

onBeforeUnmount(() => {
  const element = textarea()
  element?.removeEventListener('compositionupdate', scheduleCompositionResize)
  element?.removeEventListener('input', onTextareaInput)
  if (compositionResizeFrame) cancelAnimationFrame(compositionResizeFrame)
})

defineExpose({
  textarea,
  focus: () => inputRef.value?.focus?.()
})
</script>

<template>
  <el-input
    ref="inputRef"
    v-bind="$attrs"
    v-model="model"
    class="auto-resize-textarea"
    type="textarea"
    :autosize="autosize"
    :resize="resize"
    :disabled="disabled"
    :placeholder="placeholder"
  />
</template>
