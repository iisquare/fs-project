<script setup lang="ts">
/**
 * 智能体编排 - 编排画布，左侧节点库、中间流程画布、右侧节点属性、底部状态栏。
 * 节点配置以 content.cells 形式整体保存，由后端按应用标识存储。
 *
 * 与 server/cron/diagram.vue 的主要差异：本画布由前端定义节点目录（config.ts），
 * 节点尺寸与锚点随数据重建，迭代/循环容器的变量由容器节点自身提供。
 * 新建编排的自适应时机见 fittingWhenReady 注释。
 */
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'
import LayoutDesigner from '@/components/Layout/LayoutDesigner.vue'
import LayoutBack from '@/components/Layout/LayoutBack.vue'
import LayoutIcon from '@/components/Layout/LayoutIcon.vue'
import LayoutProperty from '@/components/Layout/LayoutProperty.vue'
import LayoutToolbar from '@/components/Layout/LayoutToolbar.vue'
import LayoutWidget from '@/components/Layout/LayoutWidget.vue'
import X6Container from '@/designer/X6/X6Container.vue'
import Flow from '@/designer/X6/flow'
import config from '@/designer/Agentic/config'
import SwitchLayout from '@/designer/X6/switch'
import AgenticApi from '@/api/agent/AgenticApi'
import ApiUtil from '@/utils/ApiUtil'
import DateUtil from '@/utils/DateUtil'
import DesignUtil from '@/utils/DesignUtil'

const route = useRoute()
const flowRef = ref()
const tips: any = ref({})
const diagram: any = ref(Object.assign(config.canvas.options(), { status: '1', content: { cells: [] } }))
const activeItem: any = ref({})
const property = computed(() => {
  return DesignUtil.widgetFlowProperty(activeItem.value, config)
})

/**
 * 条件分支节点按分支数量决定高度，且每个分支对应一个输出锚点，
 * 锚点不随图形数据结构持久化，统一在节点数据变化与载入后重建
 */
const handleCellSync = (cell: any) => {
  const data = cell.getData() ?? {}
  const widget: any = DesignUtil.widgetByType(data.type, config)
  if (!widget) return
  if ('End' === data.type) {
    data.mode = config.mode
  }
  if (widget.size) {
    const size = widget.size(data)
    const current = cell.getSize()
    if (current.width !== size.width || current.height !== size.height) {
      cell.resize(size.width, size.height)
    }
  }
  if (widget.ports) {
    const ports = widget.ports(data)
    const signature = JSON.stringify(ports)
    if (cell.__portsSignature !== signature) {
      cell.__portsSignature = signature
      cell.setProp('ports', ports)
    }
  }
  if ('agent-switch' === cell.shape) {
    const rows = SwitchLayout.rows(data)
    // 无连线时 getOutgoingEdges 返回 null，需兜底为空数组
    const edges: any[] = (cell.model ? cell.model.getOutgoingEdges(cell) : null) ?? []
    edges.forEach((edge: any) => {
      const item: any = rows.find((row: any) => row.id === edge.getSourcePortId())
      if (!item) return
      const edgeData = edge.getData() ?? {}
      if (edgeData.name === item.name) return
      edge.setData(Object.assign({}, edgeData, { name: item.name }))
    })
  }
}

const options: any = {
  // 普通节点尺寸由数据决定（见 handleCellSync），仅迭代/循环容器允许手动调整大小
  resizing: {
    enabled: (node: any) => 'flow-subprocess' === node?.shape,
    minWidth: 160,
    minHeight: 120,
  },
  rotating: false,
  allowMulti: true,
  allowLoop: true,
  onCellSync: handleCellSync,
  // 连线标签按分支名展示，禁止拖动（其余交互保持默认）
  interacting: () => ({}),
  edgeData: (edge: any) => {
    const source: any = edge.getSourceCell()
    const portId = edge.getSourcePortId()
    if (!source || !portId || 'agent-switch' !== source.shape) return {}
    const item: any = SwitchLayout.rows(source.getData() ?? {}).find((row: any) => row.id === portId)
    return item ? { name: item.name } : {}
  },
}
const handleDragStart = (event: any, widget: any) => {
  flowRef.value.flow.startDrag(event, widget)
}

const createNode = (type: string, x: number, y: number) => {
  const flow: any = flowRef.value.flow
  const widget: any = DesignUtil.widgetByType(type, config)
  const shape = Flow.NodeShapes[widget.shape]
  const node = flow.graph.addNode({
    shape: widget.shape,
    x, y,
    width: shape.width,
    height: shape.height,
    zIndex: ++flow.counter,
    data: Object.assign(widget.options(), {
      name: widget.label,
      icon: widget.icon,
      type: widget.type,
      description: widget.title,
    }),
  })
  flow.syncCell(node)
  return node
}

/**
 * 画布自适应（新建编排时使用）。
 *
 * splitter 面板挂载瞬间宽度为 0，容器尚未测量时执行 X6 的 zoomToFit 会按 0 宽视口计算：
 * newSX = 0，写入画布时被 clampScale 夹到 0.01，节点与拖拽 ghost 因此缩到几乎不可见。
 * 又因为 zoomToFit 以「当前缩放」为基数计算（newSX = 视口宽 / 内容宽 * 当前缩放），
 * 退化缩放一旦写入就无法通过再次点「适合」恢复，需等视口拿到真实尺寸后再执行并复位缩放。
 */
const fittingWhenReady = () => {
  const flow: any = flowRef.value?.flow
  const graph: any = flow?.graph
  if (!graph) return
  const fit = () => {
    graph.scale(1, 1) // 复位缩放，避免以退化缩放为基数计算
    flow.fitting()
  }
  // zoomToFit 取 graph.options.width/height 作为视口尺寸，为 0 说明容器还没测量完成
  const sized = () => graph.options.width > 0 && graph.options.height > 0
  if (sized()) {
    fit()
    return
  }
  const handleResize = () => {
    if (!sized()) return
    graph.off('resize', handleResize)
    fit()
  }
  graph.on('resize', handleResize)
}

/**
 * 新建的编排默认放置开始与结束节点，方便直接连线
 */
const handleInit = () => {
  const flow: any = flowRef.value.flow
  if (flow.graph.getNodes().length) return
  config.mode = diagram.value.mode || 'workflow'
  createNode('Start', 200, 160)
  createNode('End', 620, 160)
  fittingWhenReady()
}

/**
 * 兼容历史数据：按节点类型补齐缺失的配置字段，避免属性面板读取到空值
 */
const repairCells = () => {
  const flow: any = flowRef.value?.flow
  if (!flow) return
  flow.graph.getNodes().forEach((node: any) => {
    const data = node.getData() ?? {}
    const widget: any = DesignUtil.widgetByType(data.type, config)
    if (!widget) return
    // 节点自定义的历史数据修复（如开始节点拆分固定输入与自定义参数）
    widget.repair?.(data)
    const defaults = Object.assign({
      type: widget.type,
      name: widget.label,
      icon: widget.icon,
      description: widget.title,
    }, widget.options())
    Object.keys(defaults).forEach((key: string) => {
      if (undefined !== data[key]) return
      data[key] = defaults[key]
    })
  })
  flow.syncCells()
}

const loading = ref(false)
const publishing = ref(false)

/**
 * 提交给后端的只有编排自身的字段，详情接口返回的发布内容等字段不回传
 */
const params = () => {
  return {
    id: diagram.value.id,
    name: diagram.value.name,
    mode: diagram.value.mode,
    icon: diagram.value.icon,
    tags: diagram.value.tags ?? [],
    sort: diagram.value.sort,
    status: diagram.value.status,
    description: diagram.value.description,
    content: flowRef.value.flow.toJSON(),
  }
}

const applyDetail = (data: any) => {
  if (!data) return
  Object.assign(diagram.value, {
    id: data.id ?? diagram.value.id,
    status: (data.status ?? diagram.value.status) + '',
    updatedTime: data.updatedTime ?? diagram.value.updatedTime,
    publishedVersion: data.publishedVersion ?? 0,
    publishedTime: data.publishedTime ?? 0,
  })
}

/**
 * 保存草稿：保存后的内容仅用于调试运行，对外提供的内容以发布版本为准
 */
const save = () => {
  loading.value = true
  return AgenticApi.save(params(), { success: true }).then((result: any) => {
    applyDetail(ApiUtil.data(result))
    return true
  }).catch(() => false).finally(() => {
    loading.value = false
  })
}

const handleSubmit = () => {
  save()
}

/**
 * 发布：先保存草稿，再把当时的草稿固化为对外提供的发布内容
 */
const handlePublish = () => {
  publishing.value = true
  save().then((result: boolean) => {
    if (!result || !diagram.value.id) return
    return AgenticApi.publish({ id: diagram.value.id }, { success: true }).then((response: any) => {
      applyDetail(ApiUtil.data(response))
      ElMessage.success('发布完成')
    })
  }).catch(() => {}).finally(() => {
    publishing.value = false
  })
}

// 发布状态：草稿保存时间晚于发布时间说明存在未发布的改动
const publishState = computed<{ type: 'info' | 'warning' | 'success', text: string }>(() => {
  const version = diagram.value.publishedVersion ?? 0
  if (!version) return { type: 'info', text: '未发布' }
  const changed = (diagram.value.updatedTime ?? 0) > (diagram.value.publishedTime ?? 0)
  return {
    type: changed ? 'warning' : 'success',
    text: `已发布 v${version}${changed ? ' · 有未发布改动' : ''}`,
  }
})

// 早期保存的数据中问题分类器为 agent-node 形状，载入时升级为专用形状
const legacyShapes: any = {
  QuestionClassifier: 'agent-switch',
}

// 迭代/循环的固定入口节点已下线，历史数据中残留的节点需在载入时清理
const obsoleteTypes = ['IterationStart', 'LoopStart']

/**
 * 载入前整理节点数据
 * - 形状升级：问题分类器早期使用 agent-node（卡片样式），现改为 agent-switch，按分类重建锚点
 * - 历史清理：迭代/循环容器的固定入口节点已不再注册，残留节点、其连线与容器 child 引用一并剔除
 */
const normalizeCells = (cells: any[]) => {
  const removed: string[] = []
  const kept: any[] = []
  const obsolete = (cell: any) => {
    return 'agent-container-start' === cell?.shape || obsoleteTypes.indexOf(cell?.data?.type) !== -1
  }
  cells.forEach((cell: any) => {
    if (obsolete(cell)) {
      removed.push(cell.id)
      return
    }
    kept.push(cell)
  })
  return kept.map((cell: any) => {
    let item: any = cell
    const shape = legacyShapes[cell?.data?.type]
    if (shape && 'agent-node' === cell?.shape) item = Object.assign({}, cell, { shape })
    if (!removed.length) return item
    const children: any[] = Array.isArray(cell?.children) ? cell.children : []
    if (!children.length) return item
    const filtered = children.filter((id: any) => removed.indexOf(id) === -1)
    if (filtered.length === children.length) return item
    return Object.assign({}, item, { children: filtered })
  }).filter((cell: any) => {
    if (['flow-edge', 'edge'].indexOf(cell?.shape) === -1) return true
    return removed.indexOf(cell?.source?.cell) === -1 && removed.indexOf(cell?.target?.cell) === -1
  })
}

const handleReload = () => {
  const params = {
    id: route.query.id,
  }
  if (!params.id) return
  loading.value = true
  AgenticApi.info(params.id).then((result: any) => {
    Object.assign(diagram.value, ApiUtil.data(result))
    Object.assign(diagram.value, {
      status: diagram.value.status + '',
    })
    config.mode = diagram.value.mode || 'workflow'
    flowRef.value.flow.fromJSON(normalizeCells(diagram.value.content?.cells ?? []))
    repairCells()
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

// 应用类型（工作流/对话流）决定结束节点的配置形态
watch(() => diagram.value.mode, (mode: string) => {
  config.mode = mode || 'workflow'
})

const runVisible = ref(false)
const runLoading = ref(false)
const runInputs: any = ref({})
const runResult: any = ref(null)
const runVariables = computed<any[]>(() => {
  const nodes: any[] = flowRef.value?.flow?.graph?.getNodes?.() ?? []
  const start = nodes.find((node: any) => 'Start' === node.getData()?.type)
  return start ? config.startInputs(start.getData() ?? {}) : []
})
const runResultText = computed(() => {
  if (null === runResult.value || undefined === runResult.value) return ''
  return 'string' === typeof runResult.value ? runResult.value : JSON.stringify(runResult.value, null, 2)
})

// 调试运行表单默认值：布尔取假值，数值留空，其余按文本处理
const runDefault = (type: string) => {
  if ('Boolean' === type) return false
  if (['Number', 'Integer'].indexOf(type) !== -1) return undefined
  return ''
}

// 文件列表的取值说明：上传数量与允许的文件类型
const runFileTips = (item: any) => {
  const labels = (item.fileTypes ?? []).map((value: string) => {
    const option: any = (config.fileTypes ?? []).find((option: any) => option.value === value)
    return option ? option.label : value
  })
  return [
    item.maxCount ? `最多 ${item.maxCount} 个` : '',
    labels.length ? `支持 ${labels.join('、')}` : '',
  ].filter((text: string) => text).join(' · ')
}

const handleRun = () => {
  if (!diagram.value.id) {
    ElMessage.warning('请先保存编排后再运行')
    return
  }
  runResult.value = null
  runVariables.value.forEach((item: any) => {
    if (item.name && undefined === runInputs.value[item.name]) runInputs.value[item.name] = runDefault(item.type)
  })
  runVisible.value = true
}

const handleRunSubmit = () => {
  const missing = runVariables.value.filter((item: any) => true === item.required && !String(runInputs.value[item.name] ?? '').trim())
  if (missing.length) {
    ElMessage.warning(`请填写必填参数：${missing.map((item: any) => item.label || item.name).join('、')}`)
    return
  }
  const inputs: any = {}
  runVariables.value.forEach((item: any) => {
    const value = runInputs.value[item.name]
    // 文件列表按英文逗号拆分为文件ID数组，便于传入已上传文件的标识
    inputs[item.name] = 'Array<File>' === item.type
      ? String(value ?? '').split(',').map((text: string) => text.trim()).filter((text: string) => text)
      : value
  })
  runLoading.value = true
  AgenticApi.run({ id: diagram.value.id, inputs }).then((result: any) => {
    runResult.value = ApiUtil.data(result)
  }).catch(() => {}).finally(() => {
    runLoading.value = false
  })
}

// 节点目录与应用类型字典以前端定义为准，后端配置只补充运行期字典（状态等）
const catalog: any = {
  widgets: config.widgets,
  outputs: config.outputs,
  canvas: config.canvas,
  edge: config.edge,
  toolbars: config.toolbars,
  startInputs: config.startInputs,
  startRepair: config.startRepair,
  inputTypes: config.inputTypes,
  fileTypes: config.fileTypes,
  modes: config.modes, // 下拉使用 {label, value} 数组，避免被后端字典覆盖
}

onMounted(() => {
  AgenticApi.config().then((result: any) => {
    Object.assign(config, ApiUtil.data(result))
    Object.assign(config, catalog)
    config.mode = diagram.value.mode || 'workflow'
  }).catch(() => {})
  if (route.query.id) {
    handleReload()
  } else {
    handleInit() // 仅新建编排自动放置开始与结束节点
  }
})
</script>

<template>
  <LayoutDesigner splitter>
    <template #left>
      <LayoutWidget :widgets="config.widgets" @drag-start="handleDragStart" />
    </template>
    <template #top>
      <el-space>
        <LayoutBack to="/agent/agentic/list" variant="text" label="返回" />
        <el-divider direction="vertical" />
        <LayoutToolbar :toolbars="config.toolbars" :instance="flowRef" />
        <el-divider direction="vertical" />
        <span class="diagram-name">{{ diagram.name || '未命名编排' }}</span>
        <el-tag :type="publishState.type" size="small" effect="plain">{{ publishState.text }}</el-tag>
      </el-space>
      <el-space>
        <el-button text @click="handleRun"><LayoutIcon name="VideoPlay" /><span>调试运行</span></el-button>
        <el-button text @click="handleSubmit" :loading="loading"><LayoutIcon name="Check" /><span>保存</span></el-button>
        <el-button text type="primary" @click="handlePublish" :loading="publishing"><LayoutIcon name="Promotion" /><span>发布</span></el-button>
      </el-space>
    </template>
    <template #default>
      <X6Container ref="flowRef" v-model="diagram" :active-item="activeItem" :tips="tips" :options="options" @update:active-item="(v: any) => activeItem = v" />
    </template>
    <template #right>
      <LayoutProperty
        v-model="diagram"
        :key="'property-' + diagram.mode"
        :active-item="activeItem"
        :instance="flowRef"
        :config="config"
        :tips="tips"
        :property="property" />
    </template>
    <template #footer>
      <el-space>
        <LayoutIcon name="Opportunity" />
        <div>{{ tips.text }}</div>
      </el-space>
    </template>
  </LayoutDesigner>
  <el-drawer v-model="runVisible" title="调试运行（使用保存后的草稿内容）" size="420px">
    <el-form label-position="top" v-if="runVariables.length">
      <el-form-item :key="item.name" v-for="item in runVariables">
        <template #label>
          <span>{{ item.label || item.name }}</span>
          <span class="run-alias" v-if="item.label && item.label !== item.name">{{ item.name }}</span>
          <el-tag v-if="item.required" type="danger" size="small" effect="plain" class="run-required">必填</el-tag>
        </template>
        <el-input
          v-if="'Array<File>' === item.type"
          v-model="runInputs[item.name]"
          type="textarea"
          :rows="2"
          placeholder="文件ID列表，多个以英文逗号分隔" />
        <el-input
          v-else-if="'Paragraph' === item.type"
          v-model="runInputs[item.name]"
          type="textarea"
          :rows="3"
          :placeholder="item.description" />
        <el-input-number
          v-else-if="['Number', 'Integer'].indexOf(item.type) !== -1"
          v-model="runInputs[item.name]"
          :controls="false"
          :placeholder="item.description" />
        <el-switch v-else-if="'Boolean' === item.type" v-model="runInputs[item.name]" />
        <el-input v-else v-model="runInputs[item.name]" :placeholder="item.description" />
        <div class="run-tips" v-if="'Array<File>' === item.type">{{ runFileTips(item) }}</div>
      </el-form-item>
    </el-form>
    <el-empty v-else description="开始节点未定义输入变量" :image-size="80" />
    <el-space>
      <el-button type="primary" @click="handleRunSubmit" :loading="runLoading">运行</el-button>
      <el-button @click="runVisible = false">关闭</el-button>
    </el-space>
    <template v-if="runResultText">
      <el-divider>运行结果</el-divider>
      <pre class="run-result">{{ runResultText }}</pre>
    </template>
  </el-drawer>
</template>

<style lang="scss" scoped>
.diagram-name {
  font-size: 13px;
  color: var(--el-text-color-regular);
}
.run-required {
  margin-left: 6px;
}
.run-alias {
  margin-left: 6px;
  font-size: 12px;
  font-weight: normal;
  color: var(--el-text-color-placeholder);
}
.run-tips {
  margin-top: 2px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--el-text-color-placeholder);
}
.run-result {
  margin: 0;
  padding: 10px;
  border-radius: 4px;
  background: var(--el-fill-color-lighter);
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
