<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import LayoutIcon from '@/components/Layout/LayoutIcon.vue'

const model: any = defineModel({ default: () => [] })
const props = withDefaults(defineProps<{
  sources?: any[],
  targets?: any[],
  loading?: boolean,
  message?: string,
}>(), {
  sources: () => [],
  targets: () => [],
  loading: false,
  message: '',
})
const emit = defineEmits<{
  refresh: []
}>()

const dialogVisible = ref(false)
const selectedSource = ref<any>(null)
const canvasRef = ref<HTMLElement>()
const linePaths = ref<any[]>([])
const canvasSize = ref({ width: 900, height: 300 })
const dragLine = ref<any>(null)
let resizeObserver: ResizeObserver | null = null
let dragOrigin: { field: any, x: number, y: number, canvas: DOMRect } | null = null

const typeOptions = [
  { value: '', label: '保持原类型' },
  { value: 'java.lang.String', label: 'String' },
  { value: 'java.lang.Integer', label: 'Integer' },
  { value: 'java.lang.Long', label: 'Long' },
  { value: 'java.lang.Double', label: 'Double' },
  { value: 'java.lang.Boolean', label: 'Boolean' },
  { value: 'java.sql.Timestamp', label: 'Timestamp' },
  { value: 'java.sql.Date', label: 'Date' },
]

const rows = computed<any[]>(() => Array.isArray(model.value) ? model.value : [])
const sourceOrigins = computed(() =>
  props.sources.map((field: any) => field.origin).filter((name: string, index: number, list: string[]) =>
    name && list.indexOf(name) === index))
const targetOrigins = computed(() =>
  props.targets.map((field: any) => field.origin).filter((name: string, index: number, list: string[]) =>
    name && list.indexOf(name) === index))

const typeLabel = (type: string) => {
  const labels: Record<string, string> = {
    'java.lang.String': 'String',
    'java.lang.Integer': 'Integer',
    'java.lang.Long': 'Long',
    'java.lang.Double': 'Double',
    'java.lang.Boolean': 'Boolean',
    'java.sql.Timestamp': 'Timestamp',
    'java.sql.Date': 'Date',
  }
  return labels[type] || type || '保持原类型'
}

const targetMapping = (field: any) =>
  rows.value.find((row: any) => row.target === field.name)

const update = (index: number, key: string, value: string) => {
  const next = rows.value.map((row: any) => ({ ...row }))
  next[index] = { ...next[index], [key]: value }
  model.value = next
}

const sourceIndex = (name: string) =>
  props.sources.findIndex((field: any) => field.name === name)
const targetIndex = (name: string) =>
  props.targets.findIndex((field: any) => field.name === name)

const mappingIssue = (row: any) => {
  if (!row.source || !row.target) return '映射未完整配置'
  const sourceMissing = props.sources.length > 0 && sourceIndex(row.source) < 0
  const targetMissing = props.targets.length > 0 && targetIndex(row.target) < 0
  if (sourceMissing && targetMissing) return '源字段和目标字段均不存在'
  if (sourceMissing) return '源字段不存在'
  if (targetMissing) return '目标字段不存在'
  return ''
}

const validMappings = computed(() => rows.value.filter((row: any) => !mappingIssue(row)))
const invalidCount = computed(() => rows.value.length - validMappings.value.length)
const mappingCount = computed(() => validMappings.value.length)

const connectFields = (source: any, target: any) => {
  const index = rows.value.findIndex((row: any) => row.target === target.name)
  const mapping = {
    target: target.name,
    source: source.name,
    clsType: target.clsType || source.clsType || '',
  }
  if (index >= 0) {
    const next = rows.value.map((row: any) => ({ ...row }))
    next[index] = mapping
    model.value = next
  } else {
    model.value = rows.value.concat(mapping)
  }
  selectedSource.value = null
}

const selectSource = (field: any) => {
  selectedSource.value = selectedSource.value?.name === field.name ? null : field
}

const selectTarget = (field: any) => {
  if (!selectedSource.value) return
  connectFields(selectedSource.value, field)
}

const updateSource = (index: number, value: string) => {
  const source = props.sources.find((field: any) => field.name === value)
  const next = rows.value.map((row: any) => ({ ...row }))
  next[index] = {
    ...next[index],
    source: value,
    clsType: next[index].clsType || source?.clsType || '',
  }
  model.value = next
}

const updateTarget = (index: number, value: string) => {
  const target = props.targets.find((field: any) => field.name === value)
  const next = rows.value.map((row: any) => ({ ...row }))
  next[index] = {
    ...next[index],
    target: value,
    clsType: next[index].clsType || target?.clsType || '',
  }
  model.value = next
}

const removeTargetMapping = (field: any) => {
  model.value = rows.value.filter((row: any) => row.target !== field.name)
}

const removeMapping = (index: number) => {
  model.value = rows.value.filter((_: any, current: number) => current !== index)
}

const addMapping = () => {
  model.value = rows.value.concat({ source: '', target: '', clsType: '' })
}

const clearMappings = () => {
  model.value = []
  selectedSource.value = null
}

const matchByName = () => {
  const result = rows.value.map((row: any) => ({ ...row }))
  props.targets.forEach((target: any) => {
    const source = props.sources.find((field: any) => field.name === target.name)
    if (!source || result.some((row: any) => row.target === target.name)) return
    result.push({ target: target.name, source: source.name, clsType: target.clsType || source.clsType || '' })
  })
  model.value = result
}

const refreshLines = async () => {
  await nextTick()
  const canvas = canvasRef.value
  if (!canvas) {
    linePaths.value = []
    return
  }
  const canvasRect = canvas.getBoundingClientRect()
  canvasSize.value = {
    width: canvas.clientWidth,
    height: canvas.scrollHeight,
  }
  const paths: any[] = []
  rows.value.forEach((row: any) => {
    const source = sourceIndex(row.source)
    const target = targetIndex(row.target)
    if (source < 0 || target < 0) return
    const start = canvas.querySelector(`[data-source-index="${source}"]`) as HTMLElement
    const end = canvas.querySelector(`[data-target-index="${target}"]`) as HTMLElement
    if (!start || !end) return
    const startRect = start.getBoundingClientRect()
    const endRect = end.getBoundingClientRect()
    const startX = startRect.right - canvasRect.left
    const startY = startRect.top + startRect.height / 2 - canvasRect.top
    const endX = endRect.left - canvasRect.left
    const endY = endRect.top + endRect.height / 2 - canvasRect.top
    const offset = Math.max(30, (endX - startX) * 0.45)
    paths.push({
      target: row.target,
      d: `M ${startX} ${startY} C ${startX + offset} ${startY}, ${endX - offset} ${endY}, ${endX} ${endY}`,
    })
  })
  linePaths.value = paths
}

const sourceAnchorStart = (event: PointerEvent, field: any) => {
  event.preventDefault()
  event.stopPropagation()
  const target = event.currentTarget as HTMLElement
  const canvas = canvasRef.value
  if (!canvas) return
  const canvasRect = canvas.getBoundingClientRect()
  const rect = target.getBoundingClientRect()
  selectedSource.value = field
  dragOrigin = {
    field,
    x: rect.right - canvasRect.left,
    y: rect.top + rect.height / 2 - canvasRect.top,
    canvas: canvasRect,
  }
  dragLine.value = {
    d: `M ${dragOrigin.x} ${dragOrigin.y} C ${dragOrigin.x + 35} ${dragOrigin.y}, ${dragOrigin.x + 35} ${dragOrigin.y}, ${dragOrigin.x} ${dragOrigin.y}`,
  }
  window.addEventListener('pointermove', sourceAnchorMove)
  window.addEventListener('pointerup', sourceAnchorEnd, { once: true })
}

const sourceAnchorMove = (event: PointerEvent) => {
  if (!dragOrigin) return
  const endX = event.clientX - dragOrigin.canvas.left
  const endY = event.clientY - dragOrigin.canvas.top
  const offset = Math.max(30, Math.abs(endX - dragOrigin.x) * 0.45)
  dragLine.value = {
    d: `M ${dragOrigin.x} ${dragOrigin.y} C ${dragOrigin.x + offset} ${dragOrigin.y}, ${endX - offset} ${endY}, ${endX} ${endY}`,
  }
}

const sourceAnchorEnd = (event: PointerEvent) => {
  window.removeEventListener('pointermove', sourceAnchorMove)
  if (dragOrigin) {
    const element = document.elementFromPoint(event.clientX, event.clientY) as HTMLElement | null
    const anchor = element?.closest('[data-target-index]') as HTMLElement | null
    const index = Number(anchor?.dataset.targetIndex)
    const field = props.targets[index]
    if (field) connectFields(dragOrigin.field, field)
  }
  dragOrigin = null
  dragLine.value = null
}

const clearAnchorDrag = () => {
  window.removeEventListener('pointermove', sourceAnchorMove)
  window.removeEventListener('pointerup', sourceAnchorEnd)
  dragOrigin = null
  dragLine.value = null
}

const openEditor = async () => {
  dialogVisible.value = true
  await nextTick()
  refreshLines()
}

const observeCanvas = () => {
  resizeObserver?.disconnect()
  if (!canvasRef.value) return
  resizeObserver = new ResizeObserver(() => refreshLines())
  resizeObserver.observe(canvasRef.value)
}

watch(dialogVisible, async (visible) => {
  if (visible) {
    await nextTick()
    observeCanvas()
    refreshLines()
  } else {
    resizeObserver?.disconnect()
    resizeObserver = null
    selectedSource.value = null
    clearAnchorDrag()
  }
})

watch(
  () => [props.sources, props.targets, model.value],
  () => { if (dialogVisible.value) refreshLines() },
  { deep: true },
)

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  clearAnchorDrag()
})
</script>

<template>
  <div class="field-map-editor">
    <div class="field-map-editor__result-title">
      <div>
        <strong>字段映射</strong>
        <span>{{ mappingCount }} 条有效<template v-if="invalidCount"> · {{ invalidCount }} 条异常</template></span>
      </div>
      <el-button link type="primary" @click="openEditor">修改映射</el-button>
    </div>

    <div class="field-map-editor__result">
      <div class="field-map-editor__result-head">
        <span>上游字段</span>
        <span></span>
        <span>输出字段</span>
        <span>转换类型</span>
      </div>
      <div
        v-for="(row, index) in rows"
        :key="index"
        class="field-map-editor__result-row"
        :class="{ 'is-invalid': !!mappingIssue(row) }"
      >
        <span :title="row.source">{{ row.source || '未选择' }}</span>
        <span class="field-map-editor__result-arrow">→</span>
        <span :title="row.target">{{ row.target || '未选择' }}</span>
        <span :class="{ 'is-error': !!mappingIssue(row) }">
          {{ mappingIssue(row) || typeLabel(row.clsType) }}
        </span>
      </div>
      <el-empty v-if="!rows.length" description="尚未配置字段映射" :image-size="40" />
    </div>

    <el-dialog
      v-model="dialogVisible"
      class="field-map-dialog"
      title="修改字段映射"
      width="min(1050px, 96vw)"
      top="4vh"
      append-to-body
      destroy-on-close
    >
      <el-scrollbar class="field-map-editor__dialog-scroll" max-height="76vh">
      <el-alert
        v-if="message"
        class="field-map-editor__message"
        type="warning"
        :closable="false"
        show-icon
        :title="message"
      />

      <div ref="canvasRef" class="field-map-editor__canvas">
        <div class="field-map-editor__table-column">
          <header>
            <div>
              <strong>源字段</strong>
              <em>{{ sources.length }}</em>
              <small class="origin">{{ sourceOrigins.join('、') || '未连接上游' }}</small>
            </div>
          </header>
          <div class="field-map-editor__field-head">
            <span>字段</span>
            <span>类型</span>
          </div>
          <el-scrollbar class="field-map-editor__field-scroll" max-height="300px" @scroll="refreshLines">
            <div
              v-for="(field, index) in sources"
              :key="field.name"
              class="field-map-editor__field-row"
              :class="{ 'is-selected': selectedSource?.name === field.name, 'is-mapped': rows.some((row: any) => row.source === field.name) }"
            >
              <span class="name">{{ field.name }}</span>
              <span class="type">{{ field.type }}</span>
              <i
                :data-source-index="index"
                class="field-map-editor__anchor is-source"
                :class="{ 'is-active': rows.some((row: any) => row.source === field.name) }"
                @pointerdown="sourceAnchorStart($event, field)"
                @click.stop="selectSource(field)"
              ></i>
            </div>
            <p v-if="!sources.length">暂无源字段</p>
          </el-scrollbar>
        </div>

        <div class="field-map-editor__table-column">
          <header>
            <div>
              <strong>目标字段</strong>
              <em>{{ targets.length }}</em>
              <small class="origin">{{ targetOrigins.join('、') || '未连接下游' }}</small>
            </div>
          </header>
          <div class="field-map-editor__field-head">
            <span>字段</span>
            <span>类型</span>
          </div>
          <el-scrollbar class="field-map-editor__field-scroll" max-height="300px" @scroll="refreshLines">
            <div
              v-for="(field, index) in targets"
              :key="field.name"
              class="field-map-editor__field-row is-target"
              :class="{ 'is-mapped': !!targetMapping(field) }"
            >
              <i
                :data-target-index="index"
                class="field-map-editor__anchor is-target"
                :class="{ 'is-active': !!targetMapping(field) }"
                @click.stop="selectTarget(field)"
              ></i>
              <span class="name">{{ field.name }}</span>
              <span class="type">{{ field.type }}</span>
              <button
                v-if="targetMapping(field)"
                type="button"
                class="field-map-editor__remove"
                title="删除映射"
                @click.stop="removeTargetMapping(field)"
              >
                <layout-icon name="Close" />
              </button>
            </div>
            <p v-if="!targets.length">暂无目标字段</p>
          </el-scrollbar>
        </div>
        <svg
          class="field-map-editor__lines"
          :width="canvasSize.width"
          :height="canvasSize.height"
          :viewBox="`0 0 ${canvasSize.width} ${canvasSize.height}`"
          aria-hidden="true"
        >
          <defs>
            <marker
              id="field-map-arrow"
              markerWidth="8"
              markerHeight="8"
              refX="7"
              refY="4"
              orient="auto"
            >
              <path d="M0,0 L8,4 L0,8 Z" class="field-map-editor__arrow" />
            </marker>
          </defs>
          <path
            v-for="line in linePaths"
            :key="line.target"
            :d="line.d"
            class="field-map-editor__line"
            marker-end="url(#field-map-arrow)"
          />
          <path
            v-if="dragLine"
            :d="dragLine.d"
            class="field-map-editor__line is-dragging"
            marker-end="url(#field-map-arrow)"
          />
        </svg>
      </div>

      <p v-if="selectedSource" class="field-map-editor__connect-tip">
        已选择源字段 <strong>{{ selectedSource.name }}</strong>，请点击目标字段完成连接。
      </p>

      <div class="field-map-editor__manual-title">
        <div>
          <strong>映射关系</strong>
          <span>{{ mappingCount }} 条有效<template v-if="invalidCount"> · {{ invalidCount }} 条异常</template></span>
        </div>
        <el-space :size="6" wrap>
          <el-button size="small" :loading="loading" @click="emit('refresh')">读取字段</el-button>
          <el-button
            v-if="sources.length && targets.length"
            size="small"
            @click="matchByName"
          >同名匹配</el-button>
          <el-button size="small" type="primary" plain @click="addMapping">
            <layout-icon name="Plus" />
            添加映射
          </el-button>
          <el-popconfirm
            title="确认清空全部字段映射？"
            width="180"
            confirm-button-text="清空"
            cancel-button-text="取消"
            @confirm="clearMappings"
          >
            <template #reference>
              <el-button size="small" type="danger" plain :disabled="!rows.length">
                清空
              </el-button>
            </template>
          </el-popconfirm>
        </el-space>
      </div>
      <el-alert
        v-if="invalidCount"
        class="field-map-editor__mapping-warning"
        type="warning"
        :closable="false"
        show-icon
        :title="`有 ${invalidCount} 条映射引用了当前源/目标表中不存在的字段，请重新选择或删除。`"
      />
      <div class="field-map-editor__manual">
        <div class="field-map-editor__manual-head">
          <span>源字段</span>
          <span></span>
          <span>目标字段</span>
          <span>转换类型</span>
          <span></span>
        </div>
        <div
          v-for="(row, index) in rows"
          :key="index"
          class="field-map-editor__manual-row"
          :class="{ 'is-invalid': !!mappingIssue(row) }"
        >
          <el-select
            :model-value="row.source"
            filterable
            allow-create
            placeholder="选择源字段"
            @update:model-value="(value: string) => updateSource(index, value)"
          >
            <el-option
              v-for="field in sources"
              :key="field.name"
              :label="`${field.name} · ${field.type}`"
              :value="field.name"
            />
          </el-select>
          <span class="arrow">→</span>
          <el-select
            :model-value="row.target"
            filterable
            allow-create
            placeholder="选择目标字段"
            @update:model-value="(value: string) => updateTarget(index, value)"
          >
            <el-option
              v-for="field in targets"
              :key="field.name"
              :label="`${field.name} · ${field.type}`"
              :value="field.name"
            />
          </el-select>
          <el-select
            :model-value="row.clsType || ''"
            placeholder="保持原类型"
            @update:model-value="(value: string) => update(index, 'clsType', value)"
          >
            <el-option
              v-for="item in typeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          <el-button link type="danger" @click="removeMapping(index)">
            <layout-icon name="Delete" />
          </el-button>
        </div>
        <el-empty v-if="!rows.length" description="尚未配置映射" :image-size="40" />
      </div>
      </el-scrollbar>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.field-map-editor {
  width: 100%;

  &__result-title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    margin-bottom: 6px;

    > div {
      min-width: 0;
      display: flex;
      align-items: center;
      gap: 7px;
    }

    strong {
      color: var(--el-text-color-primary);
      font-size: 13px;
    }

    span {
      padding: 1px 6px;
      border-radius: 9px;
      color: var(--el-text-color-secondary);
      background: var(--el-fill-color-light);
      font-size: 10px;
    }
  }

  &__result {
    overflow: hidden;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 7px;
    background: var(--el-bg-color);
    box-shadow: 0 1px 2px rgb(15 23 42 / 3%);
  }

  &__result-head,
  &__result-row {
    display: grid;
    grid-template-columns: minmax(0, 1fr) 18px minmax(0, 1fr) 92px;
    gap: 5px;
    align-items: center;
  }

  &__result-head {
    min-height: 32px;
    padding: 0 9px;
    color: var(--el-text-color-secondary);
    background: var(--el-fill-color-extra-light);
    font-size: 10px;
    font-weight: 600;
  }

  &__result-row {
    min-height: 34px;
    padding: 0 9px;
    border-top: 1px solid var(--el-border-color-extra-light);
    color: var(--el-text-color-regular);
    font-size: 11px;
    transition: background-color 0.15s;

    &:hover {
      background: var(--el-color-primary-light-9);
    }

    &.is-invalid {
      background: var(--el-color-danger-light-9);

      &:hover {
        background: var(--el-color-danger-light-8);
      }
    }

    span {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  &__result-arrow {
    color: var(--el-color-primary);
    text-align: center;
  }

  &__result-row .is-error {
    color: var(--el-color-danger);
  }

  &__message {
    margin-bottom: 8px;

    :deep(.el-alert__title) {
      font-size: 11px;
      line-height: 1.5;
    }
  }

  &__canvas {
    position: relative;
    min-height: 130px;
    display: grid;
    grid-template-columns: minmax(0, 1fr) 108px minmax(0, 1fr);
    gap: 0;
    padding: 6px;
    box-sizing: border-box;
    border-radius: 8px;
    background:
      radial-gradient(circle at center, var(--el-fill-color-light) 0, transparent 62%),
      var(--fs-layout-background-color, #f7fafc);
  }

  &__table-column {
    position: relative;
    z-index: 2;
    min-width: 0;
    overflow: visible;
    align-self: start;
    padding: 0;
    border: 1px solid var(--el-border-color);
    border-radius: 7px;
    background: var(--el-bg-color);
    box-shadow: 0 3px 10px rgb(15 23 42 / 5%);

    &:first-child {
      grid-column: 1;

      header {
        background: var(--el-color-primary-light-9);
      }
    }

    &:nth-child(2) {
      grid-column: 3;

      header {
        background: var(--el-color-success-light-9);
      }
    }

    header {
      height: 34px;
      display: flex;
      align-items: center;
      padding: 0 8px;
      color: var(--el-text-color-primary);
      font-size: 12px;
      background: var(--el-fill-color-extra-light);
      border-bottom: 1px solid var(--el-border-color-lighter);
      border-radius: 6px 6px 0 0;

      > div {
        min-width: 0;
        width: 100%;
        display: flex;
        align-items: center;
        gap: 6px;

        em {
          min-width: 18px;
          padding: 1px 5px;
          border-radius: 9px;
          color: var(--el-color-primary);
          background: var(--el-color-primary-light-9);
          font-size: 10px;
          font-style: normal;
          text-align: center;
        }

        .origin {
          min-width: 0;
          margin-left: auto;
          overflow: hidden;
          color: var(--el-text-color-placeholder);
          font-size: 9px;
          font-weight: normal;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }
    }

    > p {
      margin: 12px 6px;
      color: var(--el-text-color-placeholder);
      font-size: 11px;
    }
  }

  &__field-head {
    min-height: 22px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 8px;
    color: var(--el-text-color-placeholder);
    background: var(--el-fill-color-lighter);
    font-size: 10px;
  }

  &__field-scroll {
    width: 100%;
    max-height: 300px;
    scrollbar-gutter: stable;

    :deep(.el-scrollbar__wrap) {
      overflow-x: hidden;
    }

    :deep(.el-scrollbar__view) {
      min-width: 0;
    }
  }

  &__field-row {
    position: relative;
    height: 30px;
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 0 18px 0 8px;
    box-sizing: border-box;
    border-bottom: 1px solid var(--el-border-color-extra-light);
    color: var(--el-text-color-regular);
    font-size: 11px;
    cursor: default;
    user-select: none;
    transition: background-color 0.15s, color 0.15s;

    &.is-target {
      padding-right: 8px;
      padding-left: 20px;
    }

    &:hover,
    &.is-selected {
      color: var(--el-color-primary);
      background: var(--el-color-primary-light-9);
    }

    &.is-mapped {
      color: var(--el-color-primary-dark-2);
      background-image: linear-gradient(90deg, var(--el-color-primary-light-9), transparent 55%);
    }

    &.is-target.is-mapped {
      color: var(--el-color-success-dark-2);
      background-image: linear-gradient(90deg, var(--el-color-success-light-9), transparent 55%);
    }

    .name {
      min-width: 0;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .type {
      margin-left: auto;
      max-width: 42%;
      overflow: hidden;
      color: var(--el-text-color-placeholder);
      font-size: 10px;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  &__anchor {
    position: absolute;
    top: 50%;
    z-index: 3;
    width: 12px;
    height: 12px;
    box-sizing: border-box;
    border: 2px solid var(--el-text-color-placeholder);
    border-radius: 50%;
    background: var(--el-bg-color);
    box-shadow: 0 0 0 2px var(--el-bg-color);
    transform: translateY(-50%);

    &.is-source {
      right: 3px;
      cursor: crosshair;
      touch-action: none;
    }

    &.is-target {
      left: 3px;
      cursor: crosshair;
      border-color: var(--el-color-success);

      &.is-active {
        border-color: var(--el-color-success);
        background: var(--el-color-success);
      }
    }

    &.is-active {
      border-color: var(--el-color-primary);
      background: var(--el-color-primary);
    }
  }

  &__remove {
    flex: none;
    width: 18px;
    height: 18px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: 0;
    border: 0;
    color: var(--el-text-color-placeholder);
    background: transparent;
    cursor: pointer;

    &:hover {
      color: var(--el-color-danger);
    }
  }

  &__lines {
    position: absolute;
    inset: 0;
    z-index: 5;
    pointer-events: none;
    overflow: visible;
  }

  &__line {
    fill: none;
    stroke: var(--el-color-primary);
    stroke-width: 2;
    opacity: 0.9;
    filter: drop-shadow(0 1px 1px rgb(64 158 255 / 18%));
    vector-effect: non-scaling-stroke;

    &.is-dragging {
      stroke-dasharray: 5 4;
      opacity: 0.65;
    }
  }

  &__arrow {
    fill: var(--el-color-primary);
  }

  &__connect-tip {
    margin: 5px 0 0;
    padding: 5px 7px;
    border-radius: 4px;
    color: var(--el-color-primary-dark-2);
    background: var(--el-color-primary-light-9);
    font-size: 11px;
    line-height: 1.5;
  }

  &__manual-title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin: 6px 0 4px;
    color: var(--el-text-color-primary);

    > div {
      display: flex;
      align-items: center;
      gap: 7px;
    }

    strong {
      font-size: 13px;
    }

    > div > span {
      color: var(--el-text-color-secondary);
      font-size: 10px;
    }
  }

  &__manual {
    overflow: hidden;
    padding: 0;
    border: 1px solid var(--el-border-color);
    border-radius: 7px;
    background: var(--el-bg-color);
  }

  &__mapping-warning {
    margin: 0 0 4px;

    :deep(.el-alert__title) {
      font-size: 11px;
      line-height: 1.5;
    }
  }

  &__manual-head,
  &__manual-row {
    display: grid;
    grid-template-columns: minmax(120px, 1fr) 18px minmax(120px, 1fr) 118px 22px;
    gap: 6px;
    align-items: center;
  }

  &__manual-head {
    min-height: 28px;
    margin-bottom: 0;
    padding: 0 9px;
    box-sizing: border-box;
    color: var(--el-text-color-secondary);
    background: var(--el-fill-color-extra-light);
    font-size: 10px;
    font-weight: 600;
    border-bottom: 1px solid var(--el-border-color-lighter);
  }

  &__manual-row {
    margin-bottom: 0;
    padding: 5px 7px;
    border-bottom: 1px solid var(--el-border-color-extra-light);

    &:last-of-type {
      border-bottom: 0;
    }

    &.is-invalid {
      background: var(--el-color-danger-light-9);
      box-shadow: inset 3px 0 0 var(--el-color-danger-light-5);
    }

    .arrow {
      color: var(--el-color-primary);
      font-size: 14px;
      text-align: center;
    }
  }
}
</style>

<style lang="scss">
.field-map-dialog .el-dialog__body {
  max-height: none;
  overflow: hidden;
  padding-top: 12px;
  padding-bottom: 12px;
}
</style>
