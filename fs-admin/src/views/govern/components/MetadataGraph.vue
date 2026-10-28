<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { Graph } from '@antv/x6'
import { FullScreen, ZoomIn, ZoomOut } from '@element-plus/icons-vue'

export interface GovernGraphNode {
  id: string
  name: string
  subtitle?: string
  color?: string
  data?: any
}

export interface GovernGraphEdge {
  id: string
  source: string
  target: string
  label?: string
  data?: any
}

const props = defineProps<{
  nodes?: GovernGraphNode[]
  edges?: GovernGraphEdge[]
}>()

const emit = defineEmits<{
  nodeClick: [node: GovernGraphNode]
  edgeClick: [edge: GovernGraphEdge]
  blankClick: []
}>()

const container = ref<HTMLElement>()
let graph: Graph | null = null
let observer: ResizeObserver | null = null

const normalizeId = (value: any) => String(value ?? '').replace(/[^A-Za-z0-9_-]/g, '_')

const layout = () => {
  const nodes = props.nodes ?? []
  const edges = props.edges ?? []
  if (!graph) return
  graph.clearCells()
  if (nodes.length === 0) return

  const nodeIds = new Set(nodes.map(item => String(item.id)))
  const adjacency: Record<string, string[]> = {}
  for (const edge of edges) {
    const source = String(edge.source)
    const target = String(edge.target)
    if (!nodeIds.has(source) || !nodeIds.has(target)) continue
    ;(adjacency[source] ??= []).push(target)
    ;(adjacency[target] ??= []).push(source)
  }

  const levels = new Map<string, number>()
  const queue: string[] = []
  const first = String(nodes[0].id)
  levels.set(first, 0)
  queue.push(first)
  while (queue.length > 0) {
    const current = queue.shift() as string
    for (const next of adjacency[current] ?? []) {
      if (levels.has(next)) continue
      levels.set(next, (levels.get(current) ?? 0) + 1)
      queue.push(next)
    }
  }
  for (const node of nodes) {
    if (!levels.has(String(node.id))) levels.set(String(node.id), 0)
  }

  const levelsMap = new Map<number, string[]>()
  for (const node of nodes) {
    const level = levels.get(String(node.id)) ?? 0
    if (!levelsMap.has(level)) levelsMap.set(level, [])
    levelsMap.get(level)?.push(String(node.id))
  }
  const nodeMap = new Map(nodes.map(item => [String(item.id), item]))
  const maxRows = Math.max(1, ...Array.from(levelsMap.values()).map(items => items.length))
  const columnGap = 300
  const rowGap = 94

  levelsMap.forEach((ids, level) => {
    ids.forEach((id, index) => {
      const node = nodeMap.get(id) as GovernGraphNode
      const x = 50 + level * columnGap
      const y = 40 + (maxRows - ids.length) * rowGap / 2 + index * rowGap
      const color = node.color || '#409eff'
      graph?.addNode({
        id: normalizeId(id),
        x,
        y,
        width: 220,
        height: 58,
        data: node,
        attrs: {
          body: {
            fill: '#ffffff',
            stroke: color,
            strokeWidth: 1.5,
            rx: 8,
            ry: 8,
          },
          label: {
            text: node.subtitle ? `${node.name}\n${node.subtitle}` : node.name,
            fill: '#303133',
            fontSize: 13,
            fontWeight: 600,
            textWrap: {
              width: 196,
              height: 42,
              ellipsis: true,
            },
          },
        },
      })
    })
  })

  const validIds = new Set(nodes.map(item => normalizeId(item.id)))
  for (const edge of edges) {
    const source = normalizeId(edge.source)
    const target = normalizeId(edge.target)
    if (!validIds.has(source) || !validIds.has(target)) continue
    graph?.addEdge({
      id: normalizeId(edge.id),
      source: { cell: source },
      target: { cell: target },
      data: edge,
      attrs: {
        line: {
          stroke: '#94a3b8',
          strokeWidth: 1.2,
          targetMarker: { name: 'classic', size: 7 },
        },
      },
      labels: edge.label ? [{
        attrs: {
          label: {
            text: edge.label,
            fill: '#606266',
            fontSize: 11,
            backgroundColor: '#ffffff',
          },
        },
      }] : [],
      router: { name: 'manhattan', args: { padding: 18 } },
      connector: { name: 'rounded', args: { radius: 8 } },
    })
  }
  requestAnimationFrame(() => graph?.zoomToFit({ padding: 36, maxScale: 1.2 }))
}

const fit = () => {
  requestAnimationFrame(() => graph?.zoomToFit({ padding: 36, maxScale: 1.2 }))
}

const zoomIn = () => graph?.zoom(0.12)
const zoomOut = () => graph?.zoom(-0.12)

defineExpose({ fit, zoomIn, zoomOut })

onMounted(() => {
  if (!container.value) return
  graph = new Graph({
    container: container.value,
    background: { color: 'transparent' },
    grid: { visible: true, type: 'dot', args: { color: '#e5e7eb', thickness: 1 } },
    panning: true,
    mousewheel: { enabled: true, modifiers: ['ctrl'] },
    interacting: false,
  })
  graph.on('node:click', ({ node }: any) => emit('nodeClick', node.getData()))
  graph.on('edge:click', ({ edge }: any) => emit('edgeClick', edge.getData()))
  graph.on('blank:click', () => emit('blankClick'))
  observer = new ResizeObserver(() => graph?.resize())
  observer.observe(container.value)
  layout()
})

watch(() => [props.nodes, props.edges], layout, { deep: true })

onBeforeUnmount(() => {
  observer?.disconnect()
  graph?.dispose()
  graph = null
})
</script>

<template>
  <div class="govern-graph-shell">
    <div ref="container" class="govern-graph" />
    <div v-if="(nodes?.length ?? 0) === 0" class="govern-graph-empty">
      <el-empty :image-size="64" description="暂无关系图数据" />
    </div>
    <div class="govern-graph-actions">
      <el-tag effect="plain" type="info">{{ nodes?.length ?? 0 }} 节点 / {{ edges?.length ?? 0 }} 关系</el-tag>
      <el-button-group>
        <el-tooltip content="放大">
          <el-button :icon="ZoomIn" circle @click="zoomIn" />
        </el-tooltip>
        <el-tooltip content="缩小">
          <el-button :icon="ZoomOut" circle @click="zoomOut" />
        </el-tooltip>
        <el-tooltip content="适配全图">
          <el-button :icon="FullScreen" circle @click="fit" />
        </el-tooltip>
      </el-button-group>
    </div>
  </div>
</template>

<style scoped>
.govern-graph-shell {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 420px;
}
.govern-graph {
  width: 100%;
  height: 100%;
  min-height: 420px;
}
.govern-graph-empty {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}
.govern-graph-actions {
  position: absolute;
  right: 14px;
  bottom: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: color-mix(in srgb, #fff 92%, transparent);
  box-shadow: 0 4px 16px rgb(15 23 42 / 8%);
  backdrop-filter: blur(6px);
}
.govern-graph-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}
</style>
