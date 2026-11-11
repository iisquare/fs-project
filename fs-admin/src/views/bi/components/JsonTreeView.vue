<script setup lang="ts">
/**
 * JSON 树形查看器：保留真实花括号/方括号和缩进层级，
 * 支持逐层折叠、全部展开/收起，以及复制单个节点值。
 */
import { computed, ref, watch } from 'vue'
import { ArrowRight } from '@element-plus/icons-vue'
import ButtonCopy from '@/components/Button/ButtonCopy.vue'

interface JsonNode {
  path: string
  key: string
  displayKey: string
  arrayItem: boolean
  value: any
  depth: number
  kind: 'object' | 'array' | 'string' | 'number' | 'boolean' | 'null' | 'other'
  children: JsonNode[]
}

interface JsonRow {
  type: 'open' | 'property' | 'close'
  path: string
  depth: number
  node: JsonNode
  comma: boolean
  expandable?: boolean
  expanded?: boolean
}

const props = defineProps<{
  data: Record<string, any>
}>()

const collapsedPaths = ref<Set<string>>(new Set())

const nodeKind = (value: any): JsonNode['kind'] => {
  if (Array.isArray(value)) return 'array'
  if (value === null || value === undefined) return 'null'
  if (typeof value === 'object' && !(value instanceof Date)) return 'object'
  if (typeof value === 'string') return 'string'
  if (typeof value === 'number' || typeof value === 'bigint') return 'number'
  if (typeof value === 'boolean') return 'boolean'
  return 'other'
}

const childPath = (parent: string, key: string | number, array: boolean) => {
  return array ? `${parent}[${key}]` : `${parent}[${JSON.stringify(key)}]`
}

const buildNode = (
  key: string,
  value: any,
  path: string,
  depth: number,
  displayKey: string,
  arrayItem: boolean,
): JsonNode => {
  const kind = nodeKind(value)
  const children: JsonNode[] = []
  if (kind === 'array') {
    value.forEach((item: any, index: number) => {
      children.push(buildNode(
        String(index),
        item,
        childPath(path, index, true),
        depth + 1,
        `[${index}]`,
        true,
      ))
    })
  } else if (kind === 'object') {
    Object.entries(value).forEach(([childKey, childValue]) => {
      children.push(buildNode(
        childKey,
        childValue,
        childPath(path, childKey, false),
        depth + 1,
        JSON.stringify(childKey),
        false,
      ))
    })
  }
  return { path, key, displayKey, arrayItem, value, depth, kind, children }
}

const rootNode = computed(() => buildNode('$', props.data, '$', 0, '', false))

const containerPaths = computed(() => {
  const paths: string[] = []
  const walk = (node: JsonNode) => {
    if (node.kind !== 'object' && node.kind !== 'array') return
    paths.push(node.path)
    node.children.forEach(walk)
  }
  walk(rootNode.value)
  return paths
})

const resetCollapsed = () => {
  collapsedPaths.value = new Set(containerPaths.value.filter(path => path !== '$'))
}

const expandedRows = computed<JsonRow[]>(() => {
  const rows: JsonRow[] = []
  const root = rootNode.value
  const rootExpanded = root.children.length > 0 && !collapsedPaths.value.has(root.path)
  rows.push({
    type: 'open',
    path: root.path,
    depth: root.depth,
    node: root,
    comma: false,
    expandable: root.children.length > 0,
    expanded: rootExpanded,
  })
  if (!rootExpanded) return rows

  const walk = (node: JsonNode, depth: number, isLast: boolean) => {
    const expandable = node.children.length > 0
    const expanded = expandable && !collapsedPaths.value.has(node.path)
    rows.push({
      type: 'property',
      path: node.path,
      depth,
      node,
      comma: expanded ? false : !isLast,
      expandable,
      expanded,
    })
    if (!expanded) return
    node.children.forEach((child, index) => {
      walk(child, depth + 1, index === node.children.length - 1)
    })
    rows.push({
      type: 'close',
      path: `close:${node.path}`,
      depth,
      node,
      comma: !isLast,
    })
  }

  root.children.forEach((child, index) => {
    walk(child, 1, index === root.children.length - 1)
  })
  rows.push({
    type: 'close',
    path: 'close:$',
    depth: 0,
    node: root,
    comma: false,
  })
  return rows
})

const expandAll = () => {
  collapsedPaths.value = new Set()
}

const collapseAll = () => {
  collapsedPaths.value = new Set(containerPaths.value)
}

const togglePath = (path: string) => {
  const next = new Set(collapsedPaths.value)
  if (next.has(path)) next.delete(path)
  else next.add(path)
  collapsedPaths.value = next
}

const toggleNode = (row: JsonRow) => togglePath(row.path)

const rowIndent = (row: JsonRow) => {
  const depthIndent = Math.max(row.depth - 1, 0) * 20
  const closeIndent = row.type === 'close' && row.depth > 0 ? 20 : 0
  return 16 + depthIndent + closeIndent
}

const opening = (node: JsonNode) => (node.kind === 'array' ? '[' : '{')
const closing = (node: JsonNode) => (node.kind === 'array' ? ']' : '}')

const collapsedSummary = (node: JsonNode) => {
  if (node.kind === 'array') return `[ … ${node.children.length} 项 ]`
  return `{ … ${node.children.length} 个字段 }`
}

const emptyContainer = (node: JsonNode) => (node.kind === 'array' ? '[]' : '{}')

const primitiveText = (node: JsonNode) => {
  if (node.kind === 'null') return 'null'
  if (node.kind === 'string') return JSON.stringify(node.value)
  if (node.kind === 'other' && node.value instanceof Date) return JSON.stringify(node.value.toISOString())
  return String(node.value)
}

const copyValue = (node: JsonNode) => {
  if (node.kind === 'null') return 'null'
  if (typeof node.value === 'object' && node.value !== null) {
    try {
      return JSON.stringify(node.value, null, 2)
    } catch {
      return String(node.value)
    }
  }
  return String(node.value)
}

watch(() => props.data, resetCollapsed, { immediate: true })

defineExpose({ expandAll, collapseAll })
</script>

<template>
  <div class="json-tree-view">
    <div class="json-tree-view__content">
      <div
        v-for="row in expandedRows"
        :key="row.path"
        class="json-tree-view__row"
        :class="`is-${row.type}`"
        :style="{ paddingLeft: `${rowIndent(row)}px` }"
      >
        <template v-if="row.type === 'open'">
          <button
            class="json-tree-view__brace-button"
            type="button"
            :aria-label="row.expanded ? '收起全部字段' : '展开全部字段'"
            :aria-expanded="row.expanded"
            @click="toggleNode(row)"
          >{{ row.expanded ? opening(row.node) : collapsedSummary(row.node) }}</button>
        </template>

        <template v-else-if="row.type === 'close'">
          <button
            class="json-tree-view__brace-button"
            type="button"
            title="点击收起当前层级"
            @click="togglePath(row.node.path)"
          >{{ closing(row.node) }}</button>
          <span v-if="row.comma" class="json-tree-view__punctuation">,</span>
        </template>

        <template v-else>
          <button
            v-if="row.expandable"
            class="json-tree-view__toggle"
            type="button"
            :aria-label="row.expanded ? `收起 ${row.node.key}` : `展开 ${row.node.key}`"
            :aria-expanded="row.expanded"
            @click="toggleNode(row)"
          >
            <el-icon :class="{ 'is-expanded': row.expanded }"><ArrowRight /></el-icon>
          </button>
          <span v-else class="json-tree-view__toggle-placeholder" />

          <span
            class="json-tree-view__key"
            :class="{ 'is-array-item': row.node.arrayItem }"
          >{{ row.node.displayKey }}</span>
          <span class="json-tree-view__colon">:</span>

          <button
            v-if="row.expandable"
            class="json-tree-view__container"
            :class="`is-${row.node.kind}`"
            type="button"
            @click="toggleNode(row)"
          >{{ row.expanded ? opening(row.node) : collapsedSummary(row.node) }}</button>
          <span
            v-else-if="row.node.kind === 'object' || row.node.kind === 'array'"
            class="json-tree-view__container"
            :class="`is-${row.node.kind}`"
          >{{ emptyContainer(row.node) }}</span>
          <span
            v-else
            class="json-tree-view__value"
            :class="`is-${row.node.kind}`"
          >{{ primitiveText(row.node) }}</span>

          <span v-if="row.comma" class="json-tree-view__punctuation">,</span>
          <ButtonCopy
            class="json-tree-view__copy"
            :content="copyValue(row.node)"
            :title="`复制字段「${row.node.key}」的值`"
          />
        </template>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.json-tree-view {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  background: var(--el-bg-color);
}

.json-tree-view__content {
  flex: 1;
  min-height: 0;
  padding: 12px 0 18px;
  overflow: auto;
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 12px;
  line-height: 1.7;
}

.json-tree-view__row {
  display: flex;
  min-height: 28px;
  align-items: center;
  padding-right: 14px;
  border-radius: 4px;

  &.is-property:hover {
    background: var(--el-color-primary-light-9);
  }
}

.json-tree-view__toggle,
.json-tree-view__toggle-placeholder {
  display: inline-flex;
  width: 20px;
  height: 20px;
  flex: none;
  align-items: center;
  justify-content: center;
}

.json-tree-view__toggle {
  padding: 0;
  border: 0;
  color: var(--el-text-color-secondary);
  background: transparent;
  cursor: pointer;

  .el-icon {
    font-size: 12px;
    transition: transform 0.16s ease;

    &.is-expanded {
      transform: rotate(90deg);
    }
  }

  &:hover {
    color: var(--el-color-primary);
  }
}

.json-tree-view__key {
  flex: none;
  max-width: 38%;
  overflow: hidden;
  color: var(--el-color-primary-dark-2);
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;

  &.is-array-item {
    color: var(--el-text-color-secondary);
    font-weight: 400;
  }
}

.json-tree-view__colon {
  flex: none;
  margin-right: 6px;
  color: var(--el-text-color-placeholder);
}

.json-tree-view__punctuation {
  color: var(--el-text-color-secondary);
  font-weight: 600;
}

.json-tree-view__brace-button {
  padding: 0;
  border: 0;
  color: var(--el-text-color-secondary);
  background: transparent;
  font: inherit;
  font-weight: 600;
  line-height: inherit;
  cursor: pointer;

  &:hover,
  &:focus-visible {
    color: var(--el-color-primary);
  }

  &:focus-visible {
    border-radius: 2px;
    outline: 1px solid var(--el-color-primary);
    outline-offset: 2px;
  }
}

.json-tree-view__container,
.json-tree-view__value {
  min-width: 0;
  color: var(--el-text-color-regular);
  word-break: break-all;
}

.json-tree-view__container {
  padding: 0;
  border: 0;
  background: transparent;
  font: inherit;
  text-align: left;

  &.is-object {
    color: var(--el-color-primary);
  }

  &.is-array {
    color: var(--el-color-warning-dark-2);
  }

  &[type='button'] {
    cursor: pointer;

    &:hover {
      text-decoration: underline;
    }
  }
}

.json-tree-view__value {
  white-space: pre-wrap;

  &.is-string {
    color: var(--el-color-success-dark-2);
  }

  &.is-number {
    color: var(--el-color-warning-dark-2);
  }

  &.is-boolean {
    color: var(--el-color-primary-dark-2);
  }

  &.is-null {
    color: var(--el-text-color-placeholder);
    font-style: italic;
  }
}

.json-tree-view__copy {
  flex: none;
  margin-left: 5px;
  opacity: 0;
  transition: opacity 0.16s ease;
}

.json-tree-view__row:hover .json-tree-view__copy,
.json-tree-view__copy:focus-visible {
  opacity: 1;
}

@media (max-width: 700px) {
  .json-tree-view__key {
    max-width: 30%;
  }

  .json-tree-view__copy {
    opacity: 1;
  }
}
</style>
