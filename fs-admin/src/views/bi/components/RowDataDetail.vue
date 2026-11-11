<script setup lang="ts">
/**
 * BI 行数据详情抽屉，统一数据集查询与 SQL 查询的结果展示。
 * 支持字段明细、JSON 树、CSV 三种视图，并可切换结果行、复制当前视图或单个字段值。
 */
import { computed, ref } from 'vue'
import {
  ArrowLeft,
  ArrowRight,
  DArrowLeft,
  DArrowRight,
  Grid,
} from '@element-plus/icons-vue'
import ButtonCopy from '@/components/Button/ButtonCopy.vue'
import CodeEditor from '@/components/Editor/CodeEditor.vue'
import JsonTreeView from '@/views/bi/components/JsonTreeView.vue'

interface RowColumn {
  prop: string
  label?: string
  type?: string
}

type DetailFormat = 'simple' | 'fields' | 'json' | 'csv'

const props = defineProps<{
  modelValue: boolean
  rows: Record<string, any>[]
  rowIndex: number
  columns: RowColumn[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  changeRow: [value: number]
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const format = ref<DetailFormat>('simple')
const jsonMode = ref<'tree' | 'source'>('tree')
const jsonTreeRef = ref<{ expandAll: () => void, collapseAll: () => void }>()
const codeEditorRef = ref<{ foldAll: () => void, unfoldAll: () => void }>()
const formatOptions = [
  { label: '名称/值', value: 'simple' },
  { label: '字段详情', value: 'fields' },
  { label: 'JSON', value: 'json' },
  { label: 'CSV', value: 'csv' },
]
const jsonModeOptions = [
  { label: '树形', value: 'tree' },
  { label: '源码', value: 'source' },
]

const fieldColumns = computed(() => props.columns
  .filter(column => column && column.prop)
  .map(column => ({
    ...column,
    label: column.label || column.prop,
  })))

const currentRow = computed(() => props.rows?.[props.rowIndex] ?? null)
const recordPosition = computed(() => props.rows?.length
  ? `${Math.max(props.rowIndex + 1, 1)} / ${props.rows.length}`
  : '0 / 0')
const canGoPrevious = computed(() => props.rowIndex > 0)
const canGoNext = computed(() => props.rowIndex >= 0 && props.rowIndex < props.rows.length - 1)

const columnValue = (column: any) => currentRow.value?.[column.prop]

const safeStringify = (value: any, pretty = false) => {
  try {
    return JSON.stringify(value, null, pretty ? 2 : 0)
  } catch {
    return String(value)
  }
}

const valueText = (value: any) => {
  if (value === null || value === undefined) return 'NULL'
  if (value === '') return '空字符串'
  if (typeof value === 'object') return safeStringify(value, true)
  return String(value)
}

const copyValue = (value: any) => {
  if (value === null || value === undefined) return 'NULL'
  if (typeof value === 'object') return safeStringify(value)
  return String(value)
}

const cellKind = (value: any) => {
  if (value === null || value === undefined) return 'null'
  if (value === '') return 'empty-string'
  if (typeof value === 'object') return 'object'
  return 'value'
}

const orderedRow = computed(() => {
  return Object.fromEntries(fieldColumns.value.map(column => [column.prop, columnValue(column)]))
})

const jsonText = computed(() => safeStringify(orderedRow.value, true))

const csvCell = (value: any) => {
  let text = ''
  if (value !== null && value !== undefined) {
    text = typeof value === 'object' ? safeStringify(value) : String(value)
  }
  return `"${text.replace(/"/g, '""')}"`
}

const csvText = computed(() => {
  const header = fieldColumns.value.map(column => csvCell(column.label)).join(',')
  const value = fieldColumns.value.map(column => csvCell(columnValue(column))).join(',')
  return `${header}\n${value}`
})

const simpleFieldsText = computed(() => {
  const header = ['字段', '值'].join('\t')
  const rows = fieldColumns.value.map(column => [
    column.label,
    valueText(columnValue(column)),
  ].join('\t'))
  return [header, ...rows].join('\n')
})

const fieldsText = computed(() => {
  const header = ['字段', '类型', '值'].join('\t')
  const rows = fieldColumns.value.map(column => [
    column.label,
    column.type || '',
    valueText(columnValue(column)),
  ].join('\t'))
  return [header, ...rows].join('\n')
})

const copyText = computed(() => {
  if (format.value === 'json') return jsonText.value
  if (format.value === 'csv') return csvText.value
  if (format.value === 'fields') return fieldsText.value
  return simpleFieldsText.value
})

const formatLabel = computed(() => formatOptions.find(item => item.value === format.value)?.label || '')
const copyButtonText = computed(() => ['JSON', 'CSV'].includes(formatLabel.value)
  ? `复制 ${formatLabel.value}`
  : `复制${formatLabel.value}`)

const navigateTo = (index: number) => {
  if (!props.rows.length) return
  const nextIndex = Math.max(0, Math.min(index, props.rows.length - 1))
  if (nextIndex !== props.rowIndex) emit('changeRow', nextIndex)
}

</script>

<template>
  <el-drawer
    v-model="visible"
    class="row-data-detail"
    size="60%"
    resizable
    append-to-body
    destroy-on-close
  >
    <template #header>
      <div class="row-data-detail__title">
        <span class="row-data-detail__title-icon">
          <el-icon><Grid /></el-icon>
        </span>
        <div class="row-data-detail__title-copy">
          <strong>行数据详情</strong>
          <span>{{ fieldColumns.length }} 个字段</span>
        </div>
      </div>
    </template>

    <div class="row-data-detail__body">
      <div class="row-data-detail__toolbar">
        <el-segmented
          v-model="format"
          class="row-data-detail__view-switch"
          :options="formatOptions"
          size="small"
        />
        <div class="row-data-detail__toolbar-actions">
          <template v-if="format === 'json'">
            <el-segmented v-model="jsonMode" :options="jsonModeOptions" size="small" />
            <template v-if="jsonMode === 'tree'">
              <el-button text size="small" @click="jsonTreeRef?.expandAll()">展开全部</el-button>
              <el-button text size="small" @click="jsonTreeRef?.collapseAll()">收起全部</el-button>
            </template>
            <template v-else>
              <el-button text size="small" @click="codeEditorRef?.unfoldAll()">展开全部</el-button>
              <el-button text size="small" @click="codeEditorRef?.foldAll()">折叠全部</el-button>
            </template>
          </template>
          <ButtonCopy
            class="row-data-detail__copy"
            :content="copyText"
            :text="copyButtonText"
            :title="`复制当前${formatLabel}内容`"
          />
        </div>
      </div>

      <div class="row-data-detail__navigation">
        <span>当前第 <strong>{{ recordPosition }}</strong> 条</span>
        <el-button-group size="small">
          <el-button
            :icon="DArrowLeft"
            :disabled="!canGoPrevious"
            @click="navigateTo(0)"
          >首条</el-button>
          <el-button
            :icon="ArrowLeft"
            :disabled="!canGoPrevious"
            @click="navigateTo(rowIndex - 1)"
          >上一条</el-button>
          <el-button
            :icon="ArrowRight"
            :disabled="!canGoNext"
            @click="navigateTo(rowIndex + 1)"
          >下一条</el-button>
          <el-button
            :icon="DArrowRight"
            :disabled="!canGoNext"
            @click="navigateTo(rows.length - 1)"
          >末条</el-button>
        </el-button-group>
      </div>

      <el-empty v-if="!fieldColumns.length" class="row-data-detail__empty" description="暂无可展示的字段" />

      <div v-else-if="format === 'simple'" class="row-data-detail__fields">
        <el-table :data="fieldColumns" border size="small" table-layout="auto">
          <el-table-column label="字段" min-width="180">
            <template #default="{ row: column }">
              <span class="row-data-detail__field-name">{{ column.label }}</span>
            </template>
          </el-table-column>
          <el-table-column label="值" min-width="320">
            <template #default="{ row: column }">
              <div class="row-data-detail__field-value">
                <span
                  class="row-data-detail__value"
                  :class="`is-${cellKind(columnValue(column))}`"
                >{{ valueText(columnValue(column)) }}</span>
                <ButtonCopy
                  class="row-data-detail__field-copy"
                  :content="copyValue(columnValue(column))"
                  :title="`复制字段「${column.label}」的值`"
                />
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div v-else-if="format === 'fields'" class="row-data-detail__fields">
        <el-table :data="fieldColumns" border size="small" table-layout="auto">
          <el-table-column label="字段" min-width="180">
            <template #default="{ row: column }">
              <span class="row-data-detail__field-name">{{ column.label }}</span>
            </template>
          </el-table-column>
          <el-table-column label="类型" width="150">
            <template #default="{ row: column }">
              <span class="row-data-detail__field-type">{{ column.type || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="值" min-width="320">
            <template #default="{ row: column }">
              <span
                class="row-data-detail__value"
                :class="`is-${cellKind(columnValue(column))}`"
              >{{ valueText(columnValue(column)) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="64" align="center">
            <template #default="{ row: column }">
              <ButtonCopy
                :content="copyValue(columnValue(column))"
                :title="`复制字段「${column.label}」的值`"
              />
            </template>
          </el-table-column>
        </el-table>
      </div>

      <template v-else-if="format === 'json'">
        <JsonTreeView
          v-if="jsonMode === 'tree'"
          ref="jsonTreeRef"
          class="row-data-detail__json-tree"
          :data="orderedRow"
        />
        <div v-else class="row-data-detail__json-source">
          <CodeEditor
            ref="codeEditorRef"
            :model-value="jsonText"
            mode="application/json"
            fold-gutter
            fill
            bordered
            :line-wrapping="false"
          />
        </div>
      </template>
      <pre v-else class="row-data-detail__code">{{ csvText }}</pre>
    </div>
  </el-drawer>
</template>

<style lang="scss">
.row-data-detail {
  min-width: min(420px, 96vw);

  .el-drawer__header {
    margin-bottom: 0;
    padding-bottom: 16px;
    border-bottom: 1px solid var(--el-border-color-light);
  }

  .el-drawer__body {
    min-height: 0;
    padding: 0;
  }
}

.row-data-detail__title {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 10px;
}

.row-data-detail__title-icon {
  display: inline-flex;
  width: 32px;
  height: 32px;
  flex: none;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--el-color-primary-light-7);
  border-radius: 8px;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);

  .el-icon {
    font-size: 16px;
  }
}

.row-data-detail__title-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;

  strong {
    color: var(--el-text-color-primary);
    font-size: 16px;
    font-weight: 600;
    line-height: 20px;
  }

  span {
    color: var(--el-text-color-placeholder);
    font-size: 11px;
    font-weight: 400;
    line-height: 15px;
  }
}

.row-data-detail__body {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  background: var(--el-fill-color-blank);
}

.row-data-detail__toolbar {
  display: flex;
  flex: none;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 50px;
  padding: 9px 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: var(--el-bg-color);
}

.row-data-detail__view-switch {
  display: inline-flex;
  height: 28px;
  max-width: 100%;
  align-items: center;
  font-size: 12px;

  .el-segmented__item-label {
    line-height: 20px;
  }
}

.row-data-detail .row-data-detail__toolbar-actions {
  display: flex;
  min-width: 0;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-left: auto;

  .el-segmented {
    display: inline-flex;
    height: 28px;
    align-items: center;
    font-size: 12px;
  }

  .el-segmented__item-label {
    line-height: 20px;
  }

  .el-button {
    display: inline-flex;
    height: 28px;
    align-items: center;
    justify-content: center;
    margin: 0 !important;
    padding: 0 !important;
    line-height: 20px;
    white-space: nowrap;
  }
}

.row-data-detail__navigation {
  display: flex;
  flex: none;
  min-height: 44px;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 6px 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-lighter);
  font-size: 12px;

  strong {
    color: var(--el-text-color-primary);
    font-weight: 600;
  }

  :deep(.el-button) {
    margin: 0;
  }
}

.row-data-detail__fields {
  flex: 1;
  min-height: 0;
  padding: 16px;
  overflow: auto;
  background: var(--el-bg-color);
}

.row-data-detail__empty {
  flex: 1;
  min-height: 0;
}

.row-data-detail__json-tree {
  flex: 1;
  min-height: 0;
}

.row-data-detail__json-source {
  flex: 1;
  min-height: 0;
  padding: 12px 16px;
  overflow: hidden;
  background: var(--el-bg-color);

  :deep(.fs-code-editor) {
    height: 100%;
  }
}

.row-data-detail__field-name {
  color: var(--el-text-color-primary);
  font-weight: 500;
  word-break: break-all;
}

.row-data-detail__field-type {
  color: var(--el-text-color-secondary);
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 11px;
  word-break: break-all;
}

.row-data-detail__field-value {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
}

.row-data-detail__field-copy {
  flex: none;
  margin-left: auto;
  opacity: 0.35;
  transition: opacity 0.16s ease;

  &:hover,
  &:focus-visible {
    opacity: 1;
  }
}

.row-data-detail__value {
  display: inline-block;
  max-height: 240px;
  overflow: auto;
  color: var(--el-text-color-regular);
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 12px;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-all;

  &.is-object {
    color: var(--el-color-primary-dark-2);
  }

  &.is-null,
  &.is-empty-string {
    height: 20px;
    padding: 0 6px;
    border-radius: 4px;
    font-size: 10px;
    line-height: 20px;
    white-space: nowrap;
  }

  &.is-null {
    color: var(--el-text-color-secondary);
    background: var(--el-fill-color-light);
  }

  &.is-empty-string {
    color: var(--el-color-warning-dark-2);
    background: var(--el-color-warning-light-9);
    box-shadow: 0 0 0 1px var(--el-color-warning-light-7) inset;
  }
}

.row-data-detail__code {
  flex: 1;
  min-height: 0;
  margin: 0;
  padding: 16px;
  overflow: auto;
  color: var(--el-text-color-regular);
  background: var(--el-bg-color);
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 12px;
  line-height: 1.75;
  white-space: pre-wrap;
  word-break: break-all;
}

@media (max-width: 700px) {
  .row-data-detail {
    width: 100% !important;
  }

  .row-data-detail__toolbar {
    align-items: stretch;
  }

  .row-data-detail__view-switch {
    width: 100%;
    overflow-x: auto;
  }

  .row-data-detail .row-data-detail__toolbar-actions {
    width: 100%;
    margin-left: 0;

    .button-copy:last-child {
      margin-left: auto;
    }
  }

  .row-data-detail__navigation {
    align-items: flex-start;
    flex-direction: column;
  }

}
</style>
