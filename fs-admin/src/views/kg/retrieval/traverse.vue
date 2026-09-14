<script setup lang="ts">
/**
 * 图探索 - 以某条实体数据为起点，按关系类型与深度展开关系网络
 *
 * 交互约定：
 * 1. 顶部选择本体、起点实体与起点数据，支持按标题字段远程搜索；
 * 2. 画布复用设计器的实体节点与关系连线样式，可直接查看数据属性；
 * 3. 点击节点或连线，右侧展示属性，可继续展开、跳转编辑或删除；
 * 4. 画布支持拖动与缩放，提供"适合画布"一键回到全览。
 */
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as ElementPlusIcons from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import ApiUtil from '@/utils/ApiUtil'
import TableUtil from '@/utils/TableUtil'
import GraphApi from '@/api/kg/GraphApi'
import OntologyApi from '@/api/kg/OntologyApi'
import X6Container from '@/designer/X6/X6Container.vue'
import LayoutHeading from '@/components/Layout/LayoutHeading.vue'

const route = useRoute()
const router = useRouter()

const flowRef = ref<any>()
const tips = ref<any>({ text: '' })
const diagram = ref<any>({})
const activeItem = ref<any>({})

const ontologies = ref<any[]>([])
const ontologyId = ref<any>(null)
const model = ref<any>({ entities: [], relationships: [] })
const entityLabel = ref<any>(null)
const startId = ref<any>(null)
const startOptions = ref<any[]>([])
const endEntity = ref<any>(null)
const endId = ref<any>(null)
const endOptions = ref<any[]>([])
const activePathId = ref('')
const direction = ref('out')
const depth = ref(2)
const limit = ref(200)
const relationshipTypes = ref<string[]>([])
const loading = ref(false)
const data = ref<any>({ nodes: [], relationships: [], paths: [] })
const cells = ref<any[]>([])
const history = ref<any[]>([])
const displayPrefs = ref<any>({}) // 按实体保存"画布展示字段"的用户偏好
const PREF_KEY = 'kg-graph-display-fields'
const layoutMode = ref('layer')
const compactMode = ref(false)
const searchText = ref('')
const focusMode = ref(false)
const bound = ref(false)

/** 选择了终点数据即进入两点路径推理模式 */
const pathMode = computed(() => endId.value !== null && endId.value !== undefined && endId.value !== '')

const palette = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#9b59b6', '#16a085', '#d35400']
const relationshipColors = computed(() => {
  const colors: any = {}
  let index = 0
  for (const rel of (data.value.relationships ?? [])) {
    if (colors[rel.type]) continue
    colors[rel.type] = palette[index++ % palette.length]
  }
  return colors
})

/**
 * 节点图例：按本体定义给出实体的配色与名称，与关系图例一起展示在画布左下角
 */
const nodeLegend = computed(() => {
  const result: any[] = []
  for (const node of (data.value.nodes ?? [])) {
    const label = node.labels?.[0]
    if (!label || result.some((item: any) => item.label === label)) continue
    const def = entityDef(label)
    result.push({ label, name: def?.name || label, color: def?.color || '#4299e1' })
  }
  return result
})

/* 最短路径 */
const pathVisible = ref(false)
const pathLoading = ref(false)
const pathResult = ref<any>(null)
const pathEntity = ref<any>(null)
const pathId = ref<any>(null)
const pathOptions = ref<any[]>([])

const options: any = {
  resizing: false,
  rotating: false,
  allowMulti: true,
  allowLoop: true,
  panning: true,
  mousewheel: true,
  ports: false, // 只读画布，不展示连接锚点
}

const entities = computed(() => model.value.entities ?? [])
const relationships = computed(() => model.value.relationships ?? [])
const startEntity = computed(() => entities.value.find((item: any) => item.label === entityLabel.value))
const endEntityDef = computed(() => entities.value.find((item: any) => item.label === endEntity.value))
const entityDef = (label: string) => entities.value.find((item: any) => item.label === label)
/**
 * 关系类型在本体中的定义：画布与面板优先展示本体里的关系名称（中文），类型作为补充
 */
const relationshipDef = (type: any) => relationships.value.find((item: any) => item.label === type)
const relationshipName = (type: any) => relationshipDef(type)?.name || type
const prefKey = (label: string) => `${ontologyId.value}:${label}`
const loadPrefs = () => {
  try { displayPrefs.value = JSON.parse(localStorage.getItem(PREF_KEY) || '{}') } catch (e) { displayPrefs.value = {} }
}
const persistPrefs = () => {
  try { localStorage.setItem(PREF_KEY, JSON.stringify(displayPrefs.value)) } catch (e) { /* 忽略存储失败 */ }
}

/**
 * 节点卡片展示的字段：优先用户偏好，其次本体设计时勾选的"画布展示"字段；都没有则不展示字段
 */
const displayFieldNames = (def: any, properties: any) => {
  const preferred = def ? displayPrefs.value[prefKey(def.label)] : undefined
  const configured = (def?.fields ?? []).filter((field: any) => field.display).map((field: any) => field.name)
  const names: string[] = (undefined !== preferred ? preferred : configured)
    .filter((name: string) => properties && properties[name] !== undefined && properties[name] !== null && properties[name] !== '')
  return names.slice(0, 8)
}

const valueText = (value: any) => Array.isArray(value) ? value.join(', ') : String(value)

const nodeCaption = (node: any) => {
  const def = entityDef(node?.labels?.[0])
  const properties = node?.properties ?? {}
  for (const key of [def?.captionField, def?.primaryField].filter(Boolean)) {
    if (properties[key] != null && properties[key] !== '') return String(properties[key])
  }
  const first = Object.values(properties).find(value => value != null && value !== '')
  return first == null ? node?.elementId : String(first)
}

const handleOntologies = () => {
  OntologyApi.list({ page: 1, pageSize: 200 }).then((result: any) => {
    ontologies.value = ApiUtil.data(result)?.rows ?? []
  }).catch(() => {})
}

const loadModel = () => {
  if (!ontologyId.value) return
  OntologyApi.model({ id: ontologyId.value }, { warning: false }).then((result: any) => {
    model.value = ApiUtil.data(result) ?? { entities: [], relationships: [] }
    if (!entityLabel.value && entities.value.length > 0) entityLabel.value = entities.value[0].label
  }).catch(() => {})
}

/**
 * 起点数据的取值可能与下拉选项类型不一致（路由参数都是字符串，主键可能是数字），
 * 这里按字符串比对后回写成选项里的原始值，保证下拉能正常显示标题
 */
const normalizeStartId = () => {
  const options = startOptions.value ?? []
  if (startId.value == null || startId.value === '') return
  const matched = options.find((item: any) => String(item.value) === String(startId.value))
  if (matched) startId.value = matched.value
}

const loadStartOptions = (query: string) => {
  const def = startEntity.value
  if (!def || !ontologyId.value) return
  GraphApi.search({ ontologyId: ontologyId.value, entity: def.label, keyword: query || undefined, page: 1, pageSize: 20 }, { warning: false })
    .then((result: any) => {
      startOptions.value = (ApiUtil.data(result)?.rows ?? []).map((node: any) => ({
        value: node.properties?.[def.primaryField],
        label: nodeCaption(node),
      }))
      normalizeStartId()
    }).catch(() => {})
}

/**
 * 终点数据选项：与起点同一套取值逻辑，路由或上次选择带入的值按字符串比对回写
 */
const loadEndOptions = (query: string) => {
  const def = endEntityDef.value
  if (!def || !ontologyId.value) return
  GraphApi.search({ ontologyId: ontologyId.value, entity: def.label, keyword: query || undefined, page: 1, pageSize: 20 }, { warning: false })
    .then((result: any) => {
      endOptions.value = (ApiUtil.data(result)?.rows ?? []).map((node: any) => ({
        value: node.properties?.[def.primaryField],
        label: nodeCaption(node),
      }))
      if (endId.value === null || endId.value === undefined || endId.value === '') return
      const matched = endOptions.value.find((item: any) => String(item.value) === String(endId.value))
      if (matched) endId.value = matched.value
    }).catch(() => {})
}

/**
 * 画布元素标识
 *
 * elementId 仅用于数据排查与会话内的元素定位，取值后转义为画布元素 id，
 * 避免图数据库标识中的特殊字符影响图形引擎；业务关联一律使用本体主键字段。
 */
const cellIdOf = (prefix: 'n' | 'r', elementId: any) => `${prefix}${String(elementId ?? '').replace(/[^A-Za-z0-9]/g, '_')}`
const nodeCellId = (node: any) => cellIdOf('n', node?.elementId)
const relationshipCellId = (rel: any) => cellIdOf('r', rel?.elementId)

const layout = () => {
  const nodes = data.value.nodes ?? []
  const rels = data.value.relationships ?? []
  // 精简模式：节点只展示名称，采用本体配置的颜色作为背景，节点更小、间距更紧凑
  const compact = compactMode.value
  const nodeWidth = compact ? 150 : 260
  const nodeHeight = compact ? 44 : 88
  const columnGap = compact ? 200 : 380
  const rowGap = compact ? 76 : 190
  const start = pathNodeIds(pathList.value[0])[0]
  const adjacency: any = {}
  rels.forEach((rel: any) => {
    const source = String(rel.startElementId)
    const target = String(rel.endElementId)
    adjacency[source] = adjacency[source] ?? []
    adjacency[target] = adjacency[target] ?? []
    adjacency[source].push(target)
    adjacency[target].push(source)
  })
  const depthMap: any = {}
  if (start) {
    const queue: any[] = [String(start)]
    depthMap[String(start)] = 0
    while (queue.length > 0) {
      const currentId = queue.shift()
      for (const next of (adjacency[currentId] ?? [])) {
        if (depthMap[next] != null) continue
        depthMap[next] = depthMap[currentId] + 1
        queue.push(next)
      }
    }
  }
  const levelCounter: any = {}
  const result: any[] = []
  const total = Math.max(nodes.length, 1)
  nodes.forEach((node: any, order: number) => {
    const level = depthMap[String(node.elementId)] ?? 0
    const index = levelCounter[level] = (levelCounter[level] ?? 0) + 1
    const def = entityDef(node.labels?.[0])
    const properties = node.properties ?? {}
    const shown = displayFieldNames(def, properties)
    // 展示模式：字段顺序与本体定义保持一致，本体未声明的扩展属性排在最后
    const defined = def?.fields ?? []
    const keys = [
      ...defined.map((field: any) => field.name).filter((name: string) => properties[name] !== undefined),
      ...Object.keys(properties).filter((key: string) => !defined.some((field: any) => field.name === key)),
    ]
    const fields = compact ? [] : keys.map((key: string) => {
      const field = (def?.fields ?? []).find((item: any) => item.name === key)
      return {
        name: key,
        title: field?.title || key,
        type: field?.type || '',
        value: valueText(properties[key]),
        display: shown.includes(key),
      }
    })
    let x = 40 + level * columnGap
    let y = 36 + (index - 1) * rowGap
    if (layoutMode.value === 'circle') {
      const radius = compact ? 300 : 420
      const angle = (2 * Math.PI * order) / total
      x = 520 + Math.cos(angle) * radius
      y = 420 + Math.sin(angle) * radius
    } else if (layoutMode.value === 'grid') {
      const columns = Math.ceil(Math.sqrt(total))
      x = 40 + (order % columns) * (nodeWidth + 40)
      y = 36 + Math.floor(order / columns) * (nodeHeight + 30)
    }
    result.push({
      id: nodeCellId(node),
      shape: 'kg-node',
      zIndex: 1,
      x,
      y,
      width: nodeWidth,
      height: nodeHeight + fields.length * 30,
      data: {
        name: nodeCaption(node),
        label: (node.labels ?? []).join(':'),
        labels: node.labels ?? [],
        color: def?.color || '#4299e1',
        fields,
        mode: 'view',
        compact,
        elementId: node.elementId,
      },
    })
  })
  rels.forEach((rel: any) => {
    result.push({
      id: relationshipCellId(rel),
      shape: 'flow-edge',
      zIndex: 0,
      source: { cell: cellIdOf('n', rel.startElementId), port: 'right' },
      target: { cell: cellIdOf('n', rel.endElementId), port: 'left' },
      attrs: { line: { stroke: relationshipColors.value[rel.type] ?? '#A2B1C3' } },
      data: { name: relationshipName(rel.type), label: rel.type, fields: [], elementId: rel.elementId },
    })
  })
  return result
}

const render = () => {
  cells.value = layout()
  flowRef.value?.flow?.fromJSON(cells.value)
  setTimeout(() => flowRef.value?.flow?.fitting(), 60)
  bindEvents()
}

const graphInstance = () => flowRef.value?.flow?.graph

const bindEvents = () => {
  const graph = graphInstance()
  if (!graph || bound.value) return
  bound.value = true
  graph.on('node:dblclick', ({ node }: any) => {
    const elementId = node.getData()?.elementId
    const item = (data.value.nodes ?? []).find((row: any) => row.elementId === elementId)
    if (!item) return
    const def = entityDef(item.labels?.[0])
    history.value.push({ entity: entityLabel.value, id: startId.value })
    entityLabel.value = item.labels?.[0]
    startId.value = item.properties?.[def?.primaryField]
    handleTraverse()
  })
}

const handleLocate = () => {
  const text = DPText(searchText.value)
  if (!text) return
  const item = (data.value.nodes ?? []).find((row: any) => nodeCaption(row).toLowerCase().includes(text))
  if (!item) return ElMessage.info('当前画布中未找到匹配节点')
  const cell = graphInstance()?.getCellById(nodeCellId(item))
  if (!cell) return
  graphInstance()?.centerCell(cell)
  flowRef.value?.flow?.select(cell)
}

const DPText = (value: any) => String(value ?? '').trim().toLowerCase()

/**
 * 高亮选中节点及其邻居，聚焦模式下隐藏其余节点
 */
const applyHighlight = () => {
  const graph = graphInstance()
  const item = selected.value
  if (!graph) return
  const colors: any = {}
  const neighbors = new Set<string>()
  if (item && item.kind === 'ENTITY') {
    for (const rel of (data.value.relationships ?? [])) {
      if (String(rel.startElementId) === String(item.node.elementId)) neighbors.add(cellIdOf('n', rel.endElementId))
      if (String(rel.endElementId) === String(item.node.elementId)) neighbors.add(cellIdOf('n', rel.startElementId))
    }
  }
  graph.getNodes().forEach((cell: any) => {
    const id = String(cell.id)
    if (!item) {
      colors[id] = ''
      cell.show()
      return
    }
    if (id === String(activeItem.value?.id)) {
      colors[id] = '#409eff'
      cell.show()
    } else if (neighbors.has(id)) {
      colors[id] = '#67c23a'
      cell.show()
    } else {
      colors[id] = '#c0c4cc'
      if (focusMode.value) cell.hide()
      else cell.show()
    }
  })
  graph.getEdges().forEach((cell: any) => {
    const id = String(cell.id)
    if (!item) {
      cell.show()
      return
    }
    const source = String(cell.getSourceCellId?.() ?? '')
    const target = String(cell.getTargetCellId?.() ?? '')
    const related = source === String(activeItem.value?.id) || target === String(activeItem.value?.id)
    if (focusMode.value) related ? cell.show() : cell.hide()
    else cell.show()
    colors[id] = related ? '#409eff' : ''
  })
  flowRef.value?.flow?.highlight(colors)
}

/**
 * 导出范围
 *
 * 以默认内容范围（节点与连线）为基准，四周再补固定边距：
 * 一是让图片留白更舒适，二是避免连线上的关系名称因为超出内容范围被裁掉。
 *
 * 注意：X6 导出（toPNG/toSVG）期望的 viewBox 是图坐标（与 getContentBBox 一致），
 * 不能再用 graphToLocal 转换，否则会与当前缩放/平移叠加，导致导出内容偏移或缺失。
 */
const EXPORT_PADDING = 80
const exportViewBox = (graph: any) => {
  let bbox: any = null
  try {
    bbox = graph.getContentBBox()
  } catch (e) {
    return null
  }
  if (!bbox || !bbox.width || !bbox.height) return null
  return {
    x: bbox.x - EXPORT_PADDING,
    y: bbox.y - EXPORT_PADDING,
    width: bbox.width + EXPORT_PADDING * 2,
    height: bbox.height + EXPORT_PADDING * 2,
  }
}

const handleExportPng = () => {
  const graph = graphInstance()
  if (!graph) return
  const viewBox = exportViewBox(graph)
  graph.toPNG((dataUri: string) => {
    const link = document.createElement('a')
    link.href = dataUri
    link.download = `graph_${Date.now()}.png`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  }, {
    backgroundColor: '#F2F7FA',
    quality: 1,
    // 使用自定义范围（已包含边距），避免默认范围裁掉边上的关系名称
    ...(viewBox ? { viewBox } : {}),
  })
}

/**
 * 切换本体后清空画布与选择条件
 */
const handleOntologyReset = () => {
  entityLabel.value = null
  startId.value = null
  startOptions.value = []
  data.value = { nodes: [], relationships: [], paths: [] }
  render()
  loadModel()
}

/**
 * 画布缩放
 *
 * X6 的 zoom 为增量缩放：正数放大、负数缩小，这里限制在 0.3 ~ 3 倍之间
 */
const handleZoom = (delta: number) => {
  const graph = graphInstance()
  if (!graph) return
  const scale = Number(graph.zoom() ?? 1) + delta
  if (scale < 0.3 || scale > 3) return
  graph.zoom(delta)
}

/* 页面内画布全屏 */
const canvasFullscreen = ref(false)

const handleFullscreenKeydown = (event: KeyboardEvent) => {
  if ('Escape' === event.key && canvasFullscreen.value) canvasFullscreen.value = false
}

/**
 * 画布在页面内铺满窗口（不触发浏览器全屏），全屏时隐藏右侧属性面板
 */
const toggleFullscreen = () => {
  canvasFullscreen.value = !canvasFullscreen.value
  setTimeout(() => flowRef.value?.flow?.fitting(), 80)
}

watch(activeItem, () => { setTimeout(applyHighlight, 20) }, { deep: true })
watch(layoutMode, () => render())
watch(compactMode, () => render())

const handleTraverse = () => {
  if (!ontologyId.value) return ElMessage.warning('请先选择本体')
  if (!entityLabel.value) return ElMessage.warning('请先选择起点实体')
  if (startId.value == null || startId.value === '') return ElMessage.warning('请先选择起点数据')
  if (pathMode.value) return handleReasonPath()
  loading.value = true
  GraphApi.traverse({
    ontologyId: ontologyId.value,
    entity: entityLabel.value,
    id: startId.value,
    direction: direction.value,
    relationships: relationshipTypes.value,
    depth: depth.value,
    limit: limit.value,
  }, { warning: false }).then((result: any) => {
    data.value = ApiUtil.data(result) ?? { nodes: [], relationships: [], paths: [] }
    if ((data.value.nodes ?? []).length === 0) {
      ElMessage.info('该数据在当前条件下没有关联关系')
    }
    render()
  }).catch((result: any) => {
    ElMessage.warning(ApiUtil.message(result))
  }).finally(() => {
    loading.value = false
  })
}
/**
 * 两点路径推理：指定起点、终点与最大深度，展示两点之间的全部节点路径
 */
const handleReasonPath = () => {
  if (!endEntity.value) return ElMessage.warning('请选择终点实体')
  if (String(endId.value) === String(startId.value) && entityLabel.value === endEntity.value) {
    return ElMessage.warning('起点与终点不能是同一个节点')
  }
  loading.value = true
  GraphApi.paths({
    ontologyId: ontologyId.value,
    fromEntity: entityLabel.value,
    fromId: startId.value,
    toEntity: endEntity.value,
    toId: endId.value,
    relationships: relationshipTypes.value,
    direction: direction.value,
    maxDepth: depth.value,
    limit: limit.value,
  }, { warning: false }).then((result: any) => {
    data.value = ApiUtil.data(result) ?? { nodes: [], relationships: [], paths: [] }
    const paths = pathList.value
    activePathId.value = paths.length ? pathIdOf(paths[0]) : ''
    render()
    if (!paths.length) {
      ElMessage.info('在指定深度与关系范围内没有找到路径')
    } else if (data.value.truncated) {
      ElMessage.warning(`路径数已达上限 ${limit.value} 条，可缩小深度或提高条数`)
    }
    highlightPath()
  }).catch((result: any) => {
    ElMessage.warning(ApiUtil.message(result))
  }).finally(() => {
    loading.value = false
  })
}

/* ---------------- 路径高亮 ---------------- */
/**
 * 路径中的元素标识列表
 *
 * 兼容三种返回形态：标识字符串数组、节点/关系对象数组、逗号分隔的字符串，
 * 避免返回结构差异导致前端渲染报错。
 */
const toIdList = (value: any): string[] => {
  if (Array.isArray(value)) {
    return value
      .map((item: any) => (item && 'object' === typeof item ? (item.elementId ?? '') : item))
      .filter((id: any) => id !== null && id !== undefined && id !== '')
      .map((id: any) => String(id))
  }
  if ('string' === typeof value) {
    return value.split(',').map((id: string) => id.trim()).filter((id: string) => !!id)
  }
  return []
}
const pathList = computed<any[]>(() => Array.isArray(data.value.paths) ? data.value.paths : [])
const pathNodeIds = (path: any) => toIdList(path?.nodes)
const pathRelationshipIds = (path: any) => toIdList(path?.relationships)
const pathIdOf = (path: any) => pathNodeIds(path).join('>')
const activePath = computed(() => pathList.value.find((item: any) => pathIdOf(item) === activePathId.value))
const pathNodeList = (path: any) => pathNodeIds(path).map((id: string) => (data.value.nodes ?? []).find((node: any) => node.elementId === id)).filter(Boolean)
const pathRelationshipList = (path: any) => pathRelationshipIds(path).map((id: string) => (data.value.relationships ?? []).find((rel: any) => rel.elementId === id)).filter(Boolean)

/**
 * 选中路径高亮：命中路径的节点与连线保持有色，其余置灰
 */
const highlightPath = () => {
  const graph = graphInstance()
  if (!graph) return
  const path = activePath.value
  const colors: any = {}
  const nodeIds = new Set<string>()
  const relationshipIds = new Set<string>()
  if (path) {
    pathNodeIds(path).forEach((id: string) => nodeIds.add(nodeCellId({ elementId: id })))
    pathRelationshipIds(path).forEach((id: string) => relationshipIds.add(relationshipCellId({ elementId: id })))
  }
  graph.getNodes().forEach((cell: any) => {
    colors[cell.id] = !path || nodeIds.has(cell.id) ? '' : '#c0c4cc'
  })
  graph.getEdges().forEach((cell: any) => {
    colors[cell.id] = !path || relationshipIds.has(cell.id) ? '' : '#c0c4cc'
  })
  flowRef.value?.flow?.highlight(colors)
}

const handleSelectPath = (path: any) => {
  activePathId.value = pathIdOf(path)
  highlightPath()
}
const startRelationTypes = computed(() => relationships.value.map((item: any) => ({ value: item.label, label: item.name || item.label })))

const selected = computed(() => {
  const id = String(activeItem.value?.id ?? '')
  if (id.startsWith('n')) {
    const node = (data.value.nodes ?? []).find((item: any) => nodeCellId(item) === id)
    return node ? { kind: 'ENTITY', node, properties: node.properties ?? {}, label: node.labels?.[0] } : null
  }
  if (id.startsWith('r')) {
    const relationship = (data.value.relationships ?? []).find((item: any) => relationshipCellId(item) === id)
    return relationship ? {
      kind: 'RELATIONSHIP',
      relationship,
      properties: relationship.properties ?? {},
      label: relationshipName(relationship.type),
      type: relationship.type,
    } : null
  }
  return null
})

const selectedEntityLabel = computed(() => selected.value?.kind === 'ENTITY' ? (selected.value.label ?? '') : '')
const selectedEntityFields = computed(() => entityDef(selectedEntityLabel.value)?.fields ?? [])
/**
 * 选中节点在数据库中实际拥有的全部标签
 */
const selectedLabels = computed(() => {
  const item = selected.value
  if (!item || item.kind !== 'ENTITY') return []
  return item.node?.labels ?? []
})

/**
 * 右侧属性面板的展示顺序与本体定义保持一致，本体未声明的扩展属性排在最后，
 * 属性名优先展示本体里的显示名称（中文）
 */
const propertyEntries = computed(() => {
  const item = selected.value
  if (!item) return []
  const properties = item.properties ?? {}
  const defined = item.kind === 'ENTITY'
    ? (entityDef(item.label)?.fields ?? [])
    : (relationshipDef(item.type)?.fields ?? [])
  const keys = [
    ...defined.map((field: any) => field.name).filter((name: string) => properties[name] !== undefined),
    ...Object.keys(properties).filter((key: string) => !defined.some((field: any) => field.name === key)),
  ]
  return keys.map((key: string) => {
    const field = defined.find((item: any) => item.name === key)
    return { key, label: field?.title || key, value: properties[key] }
  })
})

const currentDisplayFields = computed({
  get: () => {
    const label = selectedEntityLabel.value
    if (!label) return []
    const preferred = displayPrefs.value[prefKey(label)]
    if (undefined !== preferred) return preferred
    return (entityDef(label)?.fields ?? []).filter((field: any) => field.display).map((field: any) => field.name)
  },
  set: (value: string[]) => {
    const label = selectedEntityLabel.value
    if (!label) return
    displayPrefs.value = Object.assign({}, displayPrefs.value, { [prefKey(label)]: value })
    persistPrefs()
    render()
  },
})

const handleExpand = () => {
  const item = selected.value
  if (!item || item.kind !== 'ENTITY') return
  const def = entityDef(item.label)
  history.value.push({ entity: entityLabel.value, id: startId.value })
  entityLabel.value = item.label
  startId.value = item.properties?.[def?.primaryField]
  relationshipTypes.value = []
  // 以新的起点重新展开，退出路径推理模式
  endId.value = null
  endOptions.value = []
  activePathId.value = ''
  handleTraverse()
}

const handleBack = async () => {
  const last = history.value.pop()
  if (!last) return
  entityLabel.value = last.entity
  await nextTick()
  startId.value = last.id
  endId.value = null
  endOptions.value = []
  activePathId.value = ''
  handleTraverse()
}

const loadPathOptions = (query: string) => {
  const def = entities.value.find((item: any) => item.label === pathEntity.value)
  if (!def || !ontologyId.value) return
  GraphApi.search({ ontologyId: ontologyId.value, entity: def.label, keyword: query || undefined, page: 1, pageSize: 20 }, { warning: false })
    .then((result: any) => {
      pathOptions.value = (ApiUtil.data(result)?.rows ?? []).map((node: any) => ({
        value: node.properties?.[def.primaryField],
        label: nodeCaption(node),
      }))
    }).catch(() => {})
}

const handlePath = () => {
  if (!entityLabel.value || startId.value == null || startId.value === '') return ElMessage.warning('请先选择起点数据')
  if (!pathEntity.value || pathId.value == null || pathId.value === '') return ElMessage.warning('请选择终点数据')
  if (pathEntity.value === entityLabel.value && String(pathId.value) === String(startId.value)) {
    return ElMessage.warning('终点与起点是同一个节点，请选择不同的终点数据')
  }
  pathLoading.value = true
  GraphApi.path({
    ontologyId: ontologyId.value,
    fromEntity: entityLabel.value,
    fromId: startId.value,
    toEntity: pathEntity.value,
    toId: pathId.value,
    relationships: relationshipTypes.value,
    maxDepth: 6,
  }, { warning: false }).then((result: any) => {
    pathResult.value = ApiUtil.data(result) ?? {}
    if (!pathResult.value.found) ElMessage.info('在指定条件下未找到路径')
  }).catch((result: any) => {
    ElMessage.warning(ApiUtil.message(result))
  }).finally(() => {
    pathLoading.value = false
  })
}

const applyPath = () => {
  if (!pathResult.value?.found) return
  data.value = {
    nodes: pathResult.value.nodes ?? [],
    relationships: pathResult.value.relationships ?? [],
    paths: [],
  }
  pathVisible.value = false
  render()
}

const handleEdit = () => {
  const item = selected.value
  if (!item || item.kind !== 'ENTITY') return
  const def = entityDef(item.label)
  router.push({
    path: '/kg/extraction/data',
    query: { ontologyId: String(ontologyId.value), entity: item.label, id: String(item.properties?.[def?.primaryField] ?? '') },
  })
}

const handleDelete = () => {
  const item = selected.value
  if (!item) return
  if (item.kind === 'ENTITY') {
    const def = entityDef(item.label)
    TableUtil.confirm('确认删除该数据？将同时删除与其相关的全部关系。', '删除确认').then(() => {
      GraphApi.remove({
        ontologyId: ontologyId.value, entity: item.label,
        ids: [item.properties?.[def?.primaryField]], detach: true,
      }, { success: true }).then(() => handleTraverse()).catch(() => {})
    }).catch(() => {})
  } else {
    TableUtil.confirm('确认删除该关系？', '删除确认').then(() => {
      GraphApi.relationshipRemove({ ids: [item.relationship?.elementId] }, { success: true }).then(() => handleTraverse()).catch(() => {})
    }).catch(() => {})
  }
}

watch(entityLabel, () => {
  startId.value = null
  startOptions.value = []
  loadStartOptions('')
})

onMounted(() => {
  const query = route.query as any
  document.addEventListener('keydown', handleFullscreenKeydown)
  loadPrefs()
  handleOntologies()
  if (query.ontologyId) {
    // 路由参数是字符串，需转成与下拉选项一致的数值类型，否则会直接显示本体 id
    ontologyId.value = /^\d+$/.test(String(query.ontologyId)) ? Number(query.ontologyId) : query.ontologyId
    loadModel()
    setTimeout(() => {
      if (query.entity) entityLabel.value = query.entity
      if (query.id) startId.value = isNaN(Number(query.id)) ? query.id : Number(query.id)
      if (query.entity && query.id) handleTraverse()
    }, 600)
  }
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleFullscreenKeydown)
})
</script>

<template>
  <div class="gx-page">
    <LayoutHeading title="图谱探索" description="以某条实体数据为起点，按关系类型与深度展开关系网络" />

    <el-card :bordered="false" shadow="never" class="gx-filter">
      <div class="gx-filter__row">
        <div class="gx-filter__item">
          <span class="gx-label">本体</span>
          <el-select v-model="ontologyId" clearable filterable placeholder="请选择本体" style="width: 180px" @change="handleOntologyReset">
            <el-option v-for="item in ontologies" :key="item.id" :value="item.id" :label="item.name" />
          </el-select>
        </div>
        <div class="gx-filter__item">
          <span class="gx-label">起点实体</span>
          <el-select v-model="entityLabel" clearable placeholder="请选择" style="width: 140px">
            <el-option v-for="item in entities" :key="item.label" :value="item.label" :label="item.name || item.label" />
          </el-select>
        </div>
        <div class="gx-filter__item">
          <span class="gx-label">起点数据</span>
          <el-select v-model="startId" filterable remote :remote-method="loadStartOptions" placeholder="搜索标题或主键" style="width: 200px">
            <el-option v-for="item in startOptions" :key="item.value" :value="item.value" :label="item.label" />
          </el-select>
        </div>
        <div class="gx-filter__item">
          <span class="gx-label">终点</span>
          <el-select v-model="endEntity" clearable filterable placeholder="可选" style="width: 120px"
            @change="() => { endId = null; loadEndOptions('') }">
            <el-option v-for="item in entities" :key="item.label" :value="item.label" :label="item.name || item.label" />
          </el-select>
          <el-select v-model="endId" clearable filterable remote :remote-method="loadEndOptions"
            placeholder="选择终点即路径推理" style="width: 190px">
            <el-option v-for="item in endOptions" :key="item.value" :value="item.value" :label="item.label" />
          </el-select>
        </div>
        <div class="gx-filter__item gx-filter__item--auto">
          <el-button type="primary" :icon="ElementPlusIcons.Search" :loading="loading" @click="handleTraverse">
            {{ pathMode ? '路径推理' : '探索' }}
          </el-button>
        </div>
      </div>
      <div class="gx-filter__row">
        <div class="gx-filter__item">
          <span class="gx-label">方向</span>
          <el-radio-group v-model="direction">
            <el-radio value="out">出边</el-radio>
            <el-radio value="in">入边</el-radio>
            <el-radio value="both">双向</el-radio>
          </el-radio-group>
        </div>
        <div class="gx-filter__item">
          <span class="gx-label">深度</span>
          <el-slider v-model="depth" :min="1" :max="5" style="width: 120px" />
        </div>
        <div class="gx-filter__item">
          <span class="gx-label">条数</span>
          <el-input-number v-model="limit" :min="10" :max="1000" :controls="false" style="width: 86px" />
        </div>
        <div class="gx-filter__item">
          <span class="gx-label">关系类型</span>
          <el-select v-model="relationshipTypes" multiple clearable collapse-tags placeholder="全部关系" style="width: 260px">
            <el-option v-for="item in startRelationTypes" :key="item.value" :value="item.value" :label="item.label" />
          </el-select>
        </div>
      </div>
      <div class="gx-filter__row">
        <div class="gx-filter__item">
          <span class="gx-label">布局</span>
          <el-radio-group v-model="layoutMode">
            <el-radio value="layer">分层</el-radio>
            <el-radio value="circle">环形</el-radio>
            <el-radio value="grid">网格</el-radio>
          </el-radio-group>
        </div>
        <div class="gx-filter__item">
          <span class="gx-label">定位节点</span>
          <el-input v-model="searchText" placeholder="按标题搜索" clearable style="width: 180px" @keyup.enter="handleLocate" />
          <el-button :icon="ElementPlusIcons.Aim" @click="handleLocate">定位</el-button>
        </div>
        <div class="gx-filter__item gx-filter__item--auto">
          <el-checkbox v-model="focusMode" @change="applyHighlight">聚焦选中</el-checkbox>
          <el-tooltip content="节点只展示配置的名称，并以本体配置的颜色作为背景，便于一屏容纳更多节点" placement="top">
            <el-checkbox v-model="compactMode">精简模式</el-checkbox>
          </el-tooltip>
        </div>
      </div>
    </el-card>

    <el-card :bordered="false" shadow="never" class="gx-card" :class="{ 'gx-card--fullscreen': canvasFullscreen }">
      <el-splitter class="gx-body">
        <el-splitter-panel>
          <div class="gx-canvas">
            <X6Container
              ref="flowRef"
              v-model="diagram"
              v-model:tips="tips"
              :options="options"
              :active-item="activeItem"
              @update:active-item="(value: any) => activeItem = value"
            />
            <div class="gx-tools">
              <el-button-group size="small">
                <el-button size="small" :icon="ElementPlusIcons.ZoomOut" @click="handleZoom(-0.1)" />
                <el-button size="small" :icon="ElementPlusIcons.ZoomIn" @click="handleZoom(0.1)" />
              </el-button-group>
              <el-tooltip content="缩放画布以完整展示当前关系网络" placement="top">
                <el-button size="small" :icon="ElementPlusIcons.FullScreen" @click="render">适应画布</el-button>
              </el-tooltip>
              <el-tooltip content="导出当前画布为 PNG 图片" placement="top">
                <el-button size="small" :icon="ElementPlusIcons.Picture" @click="handleExportPng">导出图片</el-button>
              </el-tooltip>
              <el-tooltip :content="canvasFullscreen ? '退出全屏' : '画布全屏'" placement="top">
                <el-button
                  size="small"
                  :icon="canvasFullscreen ? ElementPlusIcons.ScaleToOriginal : ElementPlusIcons.FullScreen"
                  @click="toggleFullscreen"
                />
              </el-tooltip>
            </div>
            <div class="gx-count">
              <el-tag size="small" effect="plain">节点 {{ (data.nodes ?? []).length }}</el-tag>
              <el-tag size="small" effect="plain" type="success">关系 {{ (data.relationships ?? []).length }}</el-tag>
              <el-tag size="small" effect="plain" type="info">路径 {{ pathList.length }}</el-tag>
            </div>
            <div v-if="(data.nodes ?? []).length === 0" class="gx-empty">
              <el-empty description="选择起点数据后点击探索，即可查看关系网络">
                <template #image><el-icon :size="48"><ElementPlusIcons.Share /></el-icon></template>
              </el-empty>
            </div>
            <div v-if="nodeLegend.length || Object.keys(relationshipColors).length" class="gx-legend">
              <template v-if="nodeLegend.length">
                <div class="gx-legend__group">实体</div>
                <div v-for="item in nodeLegend" :key="item.label" class="gx-legend__item">
                  <span class="gx-legend__dot" :style="{ background: item.color }"></span>
                  <span class="gx-legend__name">{{ item.name }}</span>
                  <em v-if="item.name !== item.label" class="gx-legend__type">{{ item.label }}</em>
                </div>
              </template>
              <template v-if="Object.keys(relationshipColors).length">
                <div class="gx-legend__group">关系</div>
                <div v-for="(color, type) in relationshipColors" :key="type" class="gx-legend__item">
                  <span class="gx-legend__dot" :style="{ background: color }"></span>
                  <span class="gx-legend__name">{{ relationshipName(type) }}</span>
                  <em v-if="relationshipName(type) !== type" class="gx-legend__type">{{ type }}</em>
                </div>
              </template>
            </div>
          </div>
        </el-splitter-panel>
        <el-splitter-panel v-if="!canvasFullscreen" size="320px" min="260px" max="520px">
          <el-scrollbar class="gx-panel">
            <template v-if="pathMode && pathList.length">
              <div class="gx-panel__title">
                路径列表
                <span class="gx-panel__tip">共 {{ pathList.length }} 条，点击高亮</span>
              </div>
              <div
                v-for="(path, index) in pathList"
                :key="pathIdOf(path)"
                class="gx-path"
                :class="{ 'gx-path--active': pathIdOf(path) === activePathId }"
                @click="handleSelectPath(path)"
              >
                <div class="gx-path__head">
                  <span>路径 {{ index + 1 }}</span>
                  <el-tag size="small" effect="plain">{{ path.length ?? (pathNodeIds(path).length - 1) }} 跳</el-tag>
                </div>
                <div class="gx-path__chain">
                  <template v-for="(node, position) in pathNodeList(path)" :key="node.elementId">
                    <span v-if="position > 0" class="gx-path__rel">
                      —{{ relationshipName(pathRelationshipList(path)[position - 1]?.type) }}→
                    </span>
                    <span>{{ nodeCaption(node) }}</span>
                  </template>
                </div>
              </div>
            </template>
            <template v-if="selected">
              <div class="gx-panel__title">
                <el-tag :type="selected.kind === 'ENTITY' ? 'primary' : 'success'" effect="plain">
                  {{ selected.kind === 'ENTITY' ? '实体' : '关系' }}
                </el-tag>
                <span>{{ selected.label }}</span>
                <em v-if="selected.kind === 'RELATIONSHIP' && selected.type !== selected.label" class="gx-panel__type">
                  {{ selected.type }}
                </em>
                <el-tooltip content="删除该数据" placement="top">
                  <el-button
                    v-permit="'kg:graph:delete'"
                    link
                    type="danger"
                    class="gx-panel__delete"
                    :icon="ElementPlusIcons.Delete"
                    @click="handleDelete"
                  />
                </el-tooltip>
              </div>
              <div v-if="selectedLabels.length" class="gx-panel__labels">
                <span class="gx-panel__labels-title">实际标签</span>
                <div class="gx-panel__labels-list">
                  <el-tag v-for="label in selectedLabels" :key="label" size="small" effect="plain">{{ label }}</el-tag>
                </div>
              </div>
              <div v-if="selected.kind === 'ENTITY'" class="gx-fields">
                <div class="gx-fields__label">
                  画布展示字段
                  <el-tooltip content="默认取本体设计时勾选的“画布展示”字段；此处调整只影响你自己的画布展示，不会修改本体定义" placement="top">
                    <el-icon class="gx-help"><ElementPlusIcons.QuestionFilled /></el-icon>
                  </el-tooltip>
                </div>
                <el-select v-model="currentDisplayFields" multiple collapse-tags filterable clearable placeholder="选择要在节点卡片上展示的字段">
                  <el-option v-for="field in selectedEntityFields" :key="field.name" :value="field.name" :label="field.title || field.name" />
                </el-select>
              </div>
              <el-descriptions :column="1" border size="small" label-width="90px">
                <el-descriptions-item v-for="entry in propertyEntries" :key="entry.key" :label="entry.label">
                  {{ Array.isArray(entry.value) ? entry.value.join(', ') : (entry.value == null ? '-' : String(entry.value)) }}
                </el-descriptions-item>
              </el-descriptions>
              <el-space style="margin-top: 12px" wrap>
                <el-button v-if="history.length" size="small" @click="handleBack">返回上一步</el-button>
                <el-button v-if="selected.kind === 'ENTITY'" type="primary" size="small" @click="handleExpand">以此为中心</el-button>
                <el-button v-if="selected.kind === 'ENTITY'" size="small" @click="pathVisible = true; pathResult = null; pathEntity = null; pathId = null; pathOptions = []">最短路径</el-button>
                <el-button v-if="selected.kind === 'ENTITY'" v-permit="'kg:graph:modify'" size="small" @click="handleEdit">跳转编辑</el-button>
              </el-space>
            </template>
            <el-empty v-else description="点击画布中的节点或连线查看属性" :image-size="80" />
          </el-scrollbar>
        </el-splitter-panel>
      </el-splitter>
    </el-card>
  </div>

  <el-dialog v-model="pathVisible" title="最短路径" width="620px">
    <el-form label-width="90px">
      <el-form-item label="起点">
        <span>{{ nodeCaption(selected?.node) }}（{{ entityLabel }}）</span>
      </el-form-item>
      <el-form-item label="终点实体">
        <el-select v-model="pathEntity" filterable placeholder="请选择实体" @change="loadPathOptions('')">
          <el-option v-for="item in entities" :key="item.label" :value="item.label" :label="item.name || item.label" />
        </el-select>
      </el-form-item>
      <el-form-item label="终点数据">
        <el-select v-model="pathId" filterable remote :remote-method="loadPathOptions" placeholder="搜索标题或主键" style="width: 260px">
          <el-option v-for="item in pathOptions" :key="item.value" :value="item.value" :label="item.label" />
        </el-select>
        <el-button type="primary" style="margin-left: 8px" :loading="pathLoading" @click="handlePath">查找</el-button>
      </el-form-item>
    </el-form>
    <el-alert v-if="pathResult && !pathResult.found" type="info" :closable="false" show-icon title="未找到路径，可尝试调整关系类型或加深探索条件" />
    <template v-if="pathResult?.found">
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="路径长度">{{ pathResult.length }} 跳</el-descriptions-item>
        <el-descriptions-item label="经过节点">
          <el-tag v-for="item in pathResult.nodes" :key="item.elementId" size="small" effect="plain" style="margin-right: 4px">
            {{ nodeCaption(item) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="经过关系">
          <el-tag v-for="item in pathResult.relationships" :key="item.elementId" size="small" type="success" effect="plain" style="margin-right: 4px">
            {{ relationshipName(item.type) }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <div style="margin-top: 10px">
        <el-button type="primary" @click="applyPath">在画布中查看</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style lang="scss" scoped>
.gx-label {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.gx-filter .gx-label {
  // 标签宽度固定，保证同一列控件的起始位置对齐
  flex: none;
  min-width: 56px;
  text-align: right;
}
.gx-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
  // 撑满内容区：视口高度减去顶部导航与 el-main 上下内边距
  height: calc(100vh - var(--fs-layout-header-height) - 40px);
  min-height: 480px;
  // 标题自带下边距，这里统一由 flex gap 控制间距
  :deep(.fs-heading) { margin-bottom: 0; }
}
.gx-filter {
  flex: none;
  :deep(.el-card__body) {
    padding: 12px 16px;
  }
  &__row {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 10px 14px;
    & + .gx-filter__row { margin-top: 10px; }
  }
  &__item {
    display: flex;
    align-items: center;
    gap: 8px;
    // 每个条目按比例占满整行，避免控件全部堆在左侧
    flex: 1 1 240px;
    min-width: 0;
    // 条目内的控件自适应剩余宽度，忽略模板上的固定宽度
    > .el-select,
    > .el-input,
    > .el-input-number,
    > .el-slider {
      flex: 1;
      min-width: 0;
      width: auto !important;
    }
  }
  &__item--auto {
    flex: 0 0 auto;
    min-width: 0;
    > .el-select,
    > .el-input,
    > .el-input-number { flex: none; width: auto !important; }
  }
  &__label {
    flex: none;
    min-width: 56px;
    text-align: right;
    color: var(--el-text-color-secondary);
    font-size: 13px;
  }
  :deep(.el-divider--vertical) { margin: 0 4px; }
}
.gx-card {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  :deep(.el-card__body) {
    height: 100%;
    padding: 0;
  }
  // 页面内全屏：固定在窗口上铺满，不触发浏览器全屏
  &--fullscreen {
    position: fixed;
    inset: 0;
    z-index: 2000;
    margin: 0;
    border-radius: 0;
    background: var(--el-bg-color);
  }
}
.gx-body {
  height: 100%;
}
.gx-canvas {
  position: relative;
  height: 100%;
  overflow: hidden;
}
.gx-tools {
  position: absolute;
  top: 12px;
  right: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  // 工具条整体更紧凑，避免遮挡画布
  :deep(.el-button) {
    padding: 0 8px;
    font-size: 12px;
    > .el-icon + span { margin-left: 4px; }
  }
  :deep(.el-button-group > .el-button) { padding: 0 6px; }
}
.gx-count {
  position: absolute;
  top: 12px;
  left: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.gx-empty {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  pointer-events: none;
}
.gx-panel {
  height: 100%;
  padding: 14px;
  &__title {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 10px;
    font-weight: 600;
    overflow: hidden;
    white-space: nowrap;
    > span {
      min-width: 0;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  }
  &__type {
    flex: none;
    font-size: 12px;
    font-style: normal;
    font-weight: 400;
    color: var(--el-text-color-placeholder);
  }
  // 删除按钮固定在标题行右上角
  &__delete {
    flex: none;
    margin-left: auto;
    padding: 0 2px;
  }
  &__labels {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 8px;
    margin-bottom: 10px;
  }
  &__labels-title {
    flex: none;
    padding-top: 2px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
  &__labels-list {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-wrap: wrap;
    justify-content: flex-end;
    gap: 6px;
  }
  &__tip { font-size: 12px; font-weight: 400; color: var(--el-text-color-placeholder); }
}
.gx-path {
  margin-bottom: 10px;
  padding: 8px 10px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  cursor: pointer;
  &:hover { border-color: var(--el-color-primary); }
  &--active { border-color: var(--el-color-primary); background: var(--el-color-primary-light-9); }
  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 4px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
  &__chain { font-size: 12px; line-height: 1.9; word-break: break-all; }
  &__rel { margin: 0 2px; color: var(--el-color-primary); }
}
.gx-fields {
  margin-bottom: 10px;
  &__label {
    display: flex;
    align-items: center;
    gap: 4px;
    margin-bottom: 4px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
}
.gx-help {
  color: var(--el-text-color-placeholder);
  cursor: help;
}
@media (max-width: 1280px) {
  .gx-page {
    height: auto;
    min-height: 0;
  }
  .gx-card {
    flex: none;
    :deep(.el-card__body) { height: auto; }
  }
  .gx-body { height: 520px; }
}
.gx-legend {
  position: absolute;
  left: 12px;
  bottom: 12px;
  max-width: 240px;
  max-height: 45%;
  overflow-y: auto;
  padding: 8px 10px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  font-size: 12px;
  &__group {
    margin: 4px 0 2px;
    font-size: 11px;
    color: var(--el-text-color-placeholder);
    &:first-child { margin-top: 0; }
  }
  &__item { display: flex; align-items: center; gap: 6px; margin: 2px 0; }
  &__dot { width: 10px; height: 10px; border-radius: 50%; display: inline-block; }
  &__name {
    max-width: 150px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  &__type {
    font-style: normal;
    color: var(--el-text-color-placeholder);
  }
}
</style>
