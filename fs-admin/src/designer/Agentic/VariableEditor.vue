<script setup lang="ts">
/**
 * 变量编辑器 - 用于回复内容等可插入变量的文本：基于代码编辑器，
 * 实际值占位符为 `{{#节点标识.变量英文名称#}}`（如 `{{#n1.query#}}`），编辑器中展示为
 * `节点名称.变量中文名称` 的名称标签；插入变量按钮与输入 `/` 都唤起同一个变量面板。
 * 通过 expose 提供 insert（按变量引用插入占位符）与 copy（复制当前内容）供标题栏按钮调用。
 *
 * @v-model {String}  文本内容
 * @prop {*}      instance    - 画布实例（X6Container 暴露的 flow）
 * @prop {*}      activeItem  - 当前激活的节点，用于排除自身
 * @prop {Number} height      - 编辑器高度(px)
 * @prop {Number} fontSize    - 编辑器字号(px)，默认 12，比属性面板正文更紧凑
 * @prop {Boolean} resizable  - 是否允许拖拽底部调整高度，默认 true
 * @prop {String} mode        - 编辑器语法模式
 * @prop {String} placeholder - 空白占位提示文字
 */
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import FormUtil from '@/utils/FormUtil'
import VariablePicker from './VariablePicker.vue'
import { parseTriggerWord, variableGroups, variableTokens } from './variable'

const model: any = defineModel<string>()
const {
  instance,
  activeItem = {},
  height = 200,
  fontSize = 12,
  resizable = true,
  mode = 'null',
  placeholder = '',
  lineNumbers = false,
} = defineProps<{
  instance?: any,
  activeItem?: any,
  height?: number,
  fontSize?: number,
  resizable?: boolean,
  mode?: string,
  placeholder?: string,
  lineNumbers?: boolean,
}>()

const editorRef = ref()
const rootRef = ref()
const panelRef = ref()
const pickerRef = ref()
const groups = computed(() => variableGroups(instance, activeItem))
const tokens = computed(() => variableTokens(groups.value))

// 实际值占位符：{{#节点标识.变量英文名称#}}，如 {{#n1.query#}}
const tokenPattern = /\{\{#([^#{}]+)#\}\}/g
// 标签文案：节点名称.变量中文名称，变量不存在时回落到占位符原文
const tokenLabel = (match: any) => tokens.value[match[1]]?.label ?? match[1]
// 输入 / 唤起的变量面板：定位到光标下方，面板形态与「插入变量」按钮一致
const panelVisible = ref(false)
const panelStyle = ref<any>({})
const keyword = ref('')
const range = ref<any>(null)

const closePanel = () => {
  panelVisible.value = false
  range.value = null
  keyword.value = ''
}

/** 依据编辑器光标前的文本同步查询词与替换范围，触发字符被删除或换行时关闭面板 */
const syncPanel = () => {
  const cursor = editorRef.value?.getCursor?.()
  const line = cursor ? editorRef.value?.getLine?.(cursor.line) : ''
  const parsed = cursor && undefined !== line ? parseTriggerWord(line, cursor.ch) : null
  if (!parsed) {
    closePanel()
    return
  }
  keyword.value = parsed.word
  range.value = {
    from: { line: cursor.line, ch: parsed.from },
    to: { line: cursor.line, ch: cursor.ch },
  }
}

const openPanel = () => {
  const coords = editorRef.value?.cursorCoords?.()
  if (!coords) return
  // 面板宽度 280px，右侧留出边距避免超出视口
  panelStyle.value = {
    left: `${Math.max(8, Math.min(coords.left, window.innerWidth - 296))}px`,
    top: `${coords.bottom + 4}px`,
  }
  panelVisible.value = true
  syncPanel()
}

/** 面板选中变量：替换触发词（含触发字符）为占位符 */
const handlePick = (reference: string) => {
  const text = tokens.value[reference]?.token
  const target = range.value
  if (!text || !target) return
  closePanel()
  editorRef.value?.replaceRange?.(text, target.from, target.to)
  editorRef.value?.focus?.()
}

/**
 * 面板弹出时接管编辑器的上下键与回车：上下在列表中选择、回车插入、Esc 关闭；
 * 面板未弹出时返回 false，交回编辑器默认行为（上下移动光标、回车换行）。
 * 焦点始终留在编辑器，中文等输入法可正常输入，输入内容会实时过滤面板。
 */
const keys = {
  Up: () => {
    if (!panelVisible.value) return false
    pickerRef.value?.move(-1)
    return true
  },
  Down: () => {
    if (!panelVisible.value) return false
    pickerRef.value?.move(1)
    return true
  },
  Enter: () => {
    if (!panelVisible.value) return false
    pickerRef.value?.pickActive()
    return true
  },
  Esc: () => {
    if (!panelVisible.value) return false
    closePanel()
    editorRef.value?.focus?.()
    return true
  },
}

const handleDocumentMouseDown = (event: MouseEvent) => {
  if (!panelVisible.value) return
  const target = event.target as Node
  if (rootRef.value?.contains?.(target)) return
  if (panelRef.value?.contains?.(target)) return
  closePanel()
}

const handleDocumentKeydown = (event: KeyboardEvent) => {
  if (!panelVisible.value) return
  if ('Escape' === event.key) closePanel()
}

onMounted(() => {
  editorRef.value?.renderTokens(tokenPattern, 'cm-token', tokenLabel)
  document.addEventListener('mousedown', handleDocumentMouseDown)
  document.addEventListener('keydown', handleDocumentKeydown)
})
onBeforeUnmount(() => {
  document.removeEventListener('mousedown', handleDocumentMouseDown)
  document.removeEventListener('keydown', handleDocumentKeydown)
})

// 继续输入时同步过滤词与替换范围
watch(model, () => {
  if (panelVisible.value) syncPanel()
})

/**
 * 插入变量占位符（在编辑器光标处插入）
 * @param {String} reference 变量引用，如 `节点ID.变量名` 或 `sys.变量名`
 */
const insert = (reference: string) => {
  const text = tokens.value[reference]?.token
  if (!text) return
  const editor: any = editorRef.value
  if (editor?.replaceSelection) {
    editor.replaceSelection(text)
    return
  }
  model.value = `${model.value ?? ''}${text}`
}

/** 复制当前内容 */
const copy = () => {
  const text = String(model.value ?? '')
  if (!text) {
    ElMessage.warning('内容为空，无可复制内容')
    return
  }
  FormUtil.copyToClipboard(text).then(() => {
    ElMessage.success('已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败，请手动选择复制')
  })
}

defineExpose({ insert, copy })
</script>

<template>
  <div ref="rootRef" class="variable-editor">
    <code-editor
      ref="editorRef"
      v-model="model"
      :mode="mode"
      :height="height"
      :font-size="fontSize"
      :resizable="resizable"
      :line-numbers="lineNumbers"
      :placeholder="placeholder"
      trigger="/"
      :keys="keys"
      @trigger="openPanel" />
    <teleport to="body">
      <div
        ref="panelRef"
        class="variable-editor__panel"
        :style="panelStyle"
        v-if="panelVisible"
        @mousedown.prevent>
        <VariablePicker
          ref="pickerRef"
          v-model:keyword="keyword"
          :instance="instance"
          :active-item="activeItem"
          :searchable="false"
          @select="handlePick" />
      </div>
    </teleport>
  </div>
</template>

<style lang="scss" scoped>
.variable-editor {
  width: 100%;
}
.variable-editor__panel {
  position: fixed;
  z-index: 3000;
  width: 280px;
  padding: 8px;
  border-radius: 6px;
  border: solid 1px var(--el-border-color-lighter);
  background: var(--el-bg-color-overlay);
  box-shadow: var(--el-box-shadow-light);
  outline: none;
}
</style>
