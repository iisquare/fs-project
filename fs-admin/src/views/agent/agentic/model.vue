<script setup lang="ts">
/**
 * 智能体编排 - 编排画布，左侧节点库、中间流程画布、右侧节点属性、底部状态栏。
 * 节点配置以 content.cells 形式整体保存，由后端按应用标识存储。
 *
 * 与 server/cron/diagram.vue 的主要差异：本画布由前端定义节点目录（config.ts），
 * 节点尺寸与锚点随数据重建，迭代/循环容器的变量由容器节点自身提供。
 * 新建编排的适配时机由 Flow.fitting 统一处理（见 designer/X6/flow.ts）。
 */
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { CircleCloseFilled, Paperclip, Plus, Promotion } from '@element-plus/icons-vue'
import ChatMessage from '@/components/Chat/ChatMessage.vue'
import ChatTextBlock from '@/components/Chat/ChatTextBlock.vue'
import AgenticSteps from '@/components/Agentic/AgenticSteps.vue'
import AgenticTimeline from '@/components/Agentic/AgenticTimeline.vue'
import LayoutDesigner from '@/components/Layout/LayoutDesigner.vue'
import LayoutBack from '@/components/Layout/LayoutBack.vue'
import LayoutIcon from '@/components/Layout/LayoutIcon.vue'
import LayoutProperty from '@/components/Layout/LayoutProperty.vue'
import LayoutToolbar from '@/components/Layout/LayoutToolbar.vue'
import LayoutWidget from '@/components/Layout/LayoutWidget.vue'
import X6Container from '@/designer/X6/X6Container.vue'
import useAgenticStream from '@/composables/useAgenticStream'
import useChatBranch from '@/composables/useChatBranch'
import useChatFeedback from '@/composables/useChatFeedback'
import useChatScroll from '@/composables/useChatScroll'
import { useUserStore } from '@/stores/user'
import Flow from '@/designer/X6/flow'
import config from '@/designer/Agentic/config'
import { upgradeVariables, variableReferences } from '@/designer/Agentic/variable'
import SwitchLayout from '@/designer/X6/switch'
import AgenticApi from '@/api/agent/AgenticApi'
import streams from '@/api/agent/streams'
import ApiUtil from '@/utils/ApiUtil'
import AgenticUtil from '@/utils/AgenticUtil'
import DateUtil from '@/utils/DateUtil'
import DesignUtil from '@/utils/DesignUtil'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const flowRef = ref()
const tips: any = ref({})
const diagram: any = ref(Object.assign(config.canvas.options(), { status: '1', content: { cells: [] } }))
const activeItem: any = ref({})
const property = computed(() => {
  const cell: any = activeItem.value
  // 兜底：某些单元格（拖拽过程、历史脏数据）没有 data，节点属性面板会读到空数据而报错
  const isEdge = ['flow-edge', 'edge'].indexOf(cell?.shape) !== -1
  if (cell?.shape && !isEdge && !cell.data) return config.canvas.property
  return DesignUtil.widgetFlowProperty(cell, config)
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
 * 新建的编排默认放置开始与结束节点，方便直接连线
 */
const handleInit = () => {
  const flow: any = flowRef.value.flow
  if (flow.graph.getNodes().length) return
  config.mode = diagram.value.mode || 'workflow'
  createNode('Start', 200, 160)
  createNode('End', 620, 160)
  flow.fitting() // 视口尚未测量时由 Flow.fitting 等 resize，避免按 0 宽视口算出退化缩放
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
    roleIds: diagram.value.roleIds ?? [],
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
 * 保存后把编排标识写回路由 - 新建流程首次保存时后端才生成 id，
 * 不同步到地址栏的话刷新（或分享链接）会被当成新建，重新又得到一个空编排
 */
const syncRouteId = () => {
  const id = diagram.value.id
  if (!id || String(route.query.id ?? '') === String(id)) return
  router.replace({ query: Object.assign({}, route.query, { id }) })
}

/**
 * 保存草稿：保存后的内容仅用于调试运行，对外提供的内容以发布版本为准
 */
const save = () => {
  loading.value = true
  return AgenticApi.save(params(), { success: true }).then((result: any) => {
    applyDetail(ApiUtil.data(result))
    syncRouteId()
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
 * - 变量升级：下拉选择器早期保存的是裸引用（`节点标识.变量名`），统一换成占位符，
 *   否则运行时解析不到取值（问题分类器、知识检索会把标识原样发给模型）
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
  const result = kept.map((cell: any) => {
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
  const references = variableReferences(result)
  return result.map((cell: any) => {
    if (!cell?.data) return cell
    return Object.assign({}, cell, { data: upgradeVariables(cell.data, references) })
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
// 多轮调试：会话标识与消息列表（每条助手消息带上该轮的执行明细）
const runChatId = ref(0)
/** 调试对话的分支视图：消息按 parentId 组成消息树，只渲染当前分支（见 composables/useChatBranch） */
const branch = useChatBranch()
const runMessages: any = branch.messages
/** 模板渲染用：当前分支路径上的消息（尚未落库的流式消息始终可见） */
const runVisibleMessages = computed<any[]>(() => branch.visible.value)
const runMessage: any = ref('')
// 回放令牌：自增即取消上一轮回放（关闭抽屉、新会话、重新回放）
let playToken = 0
// 流式输出中：模型增量仍在推送（用于输入区提示与按钮状态）
const streaming = ref(false)
// 对话抽屉停靠方向：可在右侧（默认）与左侧之间切换
const runDirection = ref<'rtl' | 'ltr'>('rtl')
const handleRunDirection = () => {
  runDirection.value = 'rtl' === runDirection.value ? 'ltr' : 'rtl'
}

// 新会话：清空消息与会话标识，重新开始一轮对话
const handleRunNew = () => {
  runChatId.value = 0
  branch.reset()
  runResult.value = null
  // 新会话：终止回放并清掉画布上的运行态高亮
  resetRunState()
}
/**
 * 画布选中变化：用户点选节点/画布时终止正在进行的回放
 * （回放自身直接改 activeItem，不经过这里，不会把自己取消掉）
 */
const handleActiveItem = (value: any) => {
  playToken++
  activeItem.value = value
}

// 消息反馈：与流程对话、对话历史共用同一套提交逻辑（见 composables/useChatFeedback）
const { feeding, submit: handleRunFeedback } = useChatFeedback()

// 消息列表滚动：贴底才跟随新内容（向上翻阅历史时不被拉回，见 composables/useChatScroll）
const { bodyRef: runChatRef, handleScroll, scrollBottom: scrollRunChat } = useChatScroll()
watch(() => runMessages.value.length, () => {
  nextTick(() => scrollRunChat())
})

// 关闭抽屉时清掉画布上的运行态高亮：节点背景与连线描边还原
watch(runVisible, (visible: boolean) => {
  if (visible) return
  // 终止尚未结束的回放循环并清掉运行态高亮，避免它继续抢占选中状态
  resetRunState()
})

/**
 * 请求级异常（缺少必填参数、未发布等）挂到触发它的那条用户消息上：
 * 这类请求没有产生节点回复，异常只能跟着用户输入走。
 * 极端情况下（没有用户消息）补一条独立提示，保证异常不会静默。
 */
const attachNotice = (summary: string, detail = '') => {
  const list = runMessages.value
  for (let index = list.length - 1; index >= 0; index--) {
    if ('user' !== list[index].role) continue
    // 用独立字段承载异常详情：助手消息的 error 是字符串（原始失败原因），不能混用
    list[index].notice = { summary, detail }
    return
  }
  list.push({ role: 'error', content: summary, notice: { summary, detail }, id: 0 })
}

// 发送：用户输入与文件属于底部输入区，其余自定义参数在「参数设置」里维护
const handleRunChat = () => {
  if (runLoading.value) return
  const message = String(runMessage.value ?? '').trim()
  // 开始节点启用「用户输入」时必须填写内容，未启用时允许只带参数运行
  if (!message && queryParam.value) {
    ElMessage.warning('请输入内容')
    return
  }
  runMessage.value = ''
  // 续写位置：会话当前分支尾（没有分支时就是最后一条消息）
  const parentId = Number(branch.activeLeaf.value ?? 0) || 0
  const question: any = { role: 'user', content: message || '（按参数运行）', parentId }
  runMessages.value.push(question)
  handleRunSubmit(message, { parentId, question })
}

/** 切换分支：按当前消息的兄弟节点前后移动（只影响展示与续写位置） */
const handleRunSwitch = (item: any, step: number) => {
  branch.switchBranch(item, step)
  nextTick(() => scrollRunChat(true))
}

/** 编辑提问后重新发送：新消息接在被编辑消息的父节点下，形成新分支 */
const handleRunEdit = (item: any, content: string) => {
  if (runLoading.value) {
    ElMessage.warning('上一轮运行还在进行中，请稍候')
    return
  }
  const parentId = Number(item?.parentId ?? 0) || 0
  const question: any = { role: 'user', content, parentId, createdTime: Date.now() }
  runMessages.value.push(question)
  handleRunSubmit(content, { parentId, question })
}

/** 重新生成回复：不重复落用户消息，只在同一条提问下新增一条助手回复 */
const handleRunRegenerate = (item: any) => {
  if (runLoading.value) {
    ElMessage.warning('上一轮运行还在进行中，请稍候')
    return
  }
  const question: any = runMessages.value.find((row: any) => String(row?.id) === String(item?.parentId))
  if (!question) {
    ElMessage.warning('找不到该回复对应的提问，无法重新生成')
    return
  }
  handleRunSubmit(String(question.content ?? ''), { parentId: item.parentId, reuseQuestion: true })
}

/**
 * 输入框回车：Enter 直接发送，Shift / Ctrl / Cmd + Enter 换行（与流程对话页一致）。
 * 中文输入法组词时回车用于选词（isComposing / keyCode 229），此时不发送。
 */
const handleComposerSend = (event: Event | KeyboardEvent) => {
  const key = event as KeyboardEvent
  if (key.isComposing || 229 === key.keyCode) return
  // Shift + Enter 浏览器自带换行，交给默认行为
  if (key.shiftKey) return
  // Ctrl / Cmd + Enter 浏览器不会插入换行，手动在光标处换行
  if (key.ctrlKey || key.metaKey) {
    event.preventDefault()
    insertComposerNewline(key)
    return
  }
  event.preventDefault()
  handleRunChat()
}

/** 在光标处插入换行：Ctrl / Cmd + Enter 没有浏览器默认换行，需要手动写回输入内容 */
const insertComposerNewline = (event: KeyboardEvent) => {
  const element = event.target as HTMLTextAreaElement | null
  if (!element || 'TEXTAREA' !== String(element.tagName ?? '').toUpperCase()) return
  const start = null == element.selectionStart ? element.value.length : element.selectionStart
  const end = null == element.selectionEnd ? start : element.selectionEnd
  runMessage.value = `${element.value.slice(0, start)}\n${element.value.slice(end)}`
  nextTick(() => {
    element.selectionStart = start + 1
    element.selectionEnd = start + 1
    // 触发 input 事件，让 el-input 的 autosize 按新内容重算高度
    element.dispatchEvent(new Event('input', { bubbles: true }))
  })
}

/**
 * 画布运行态：节点背景色 + 连线描边，执行中蓝、成功绿、失败红。
 * Vue 节点（卡片）用类名改背景，SVG 容器节点改 body 填充，连线用 X6 的 attr 改描边。
 */
const RUN_NODE_CLASS: any = { running: 'x6-run-running', success: 'x6-run-success', failed: 'x6-run-failed' }
const RUN_NODE_CLASSES: string[] = Object.values(RUN_NODE_CLASS)
// Vue 渲染的卡片节点：背景在组件样式里，加类名即可
const HTML_SHAPES = ['agent-node', 'agent-switch']
// SVG 容器节点（迭代/循环）的背景色
const RUN_NODE_FILL: any = { running: '#d9ecff', success: '#e1f3d8', failed: '#fde2e2' }
const RUN_EDGE_COLOR: any = { running: '#409EFF', success: '#67C23A', failed: '#F56C6C' }
// 容器节点原始填充：运行态是临时着色，重新运行或清空时还原
const nodeFills = new Map<string, any>()
// 连线原始描边：运行态是临时着色，重新运行或清空时还原
const edgeStrokes = new Map<string, any>()

/** 节点 DOM 容器：Vue/HTML 节点取视图容器，取不到时按 cell-id 兜底查找 */
const cellElement = (flow: any, cell: any): HTMLElement | null => {
  const container: HTMLElement | undefined = flow?.graph?.findViewByCell?.(cell)?.container
  if (container) return container
  return document.querySelector(`[data-cell-id="${cell.id}"]`) as HTMLElement | null
}

/** 节点着色：先清掉旧状态再加新状态 */
const markNode = (flow: any, cell: any, state: string) => {
  if (HTML_SHAPES.indexOf(cell.shape) >= 0) {
    const element = cellElement(flow, cell)
    if (!element) return
    RUN_NODE_CLASSES.forEach((name: string) => element.classList.remove(name))
    const name = RUN_NODE_CLASS[state]
    if (name) element.classList.add(name)
    return
  }
  if (!nodeFills.has(cell.id)) nodeFills.set(cell.id, cell.getAttrByPath?.('body/fill'))
  const fill = RUN_NODE_FILL[state]
  if (fill) cell.attr('body/fill', fill)
}

/** 连线着色：首次修改时记录原描边，便于还原 */
const markEdge = (flow: any, cell: any, state: string) => {
  if (!edgeStrokes.has(cell.id)) edgeStrokes.set(cell.id, cell.getAttrByPath?.('line/stroke'))
  const color = RUN_EDGE_COLOR[state]
  if (!color) return
  cell.attr('line/stroke', color)
  cell.attr('line/targetMarker/fill', color)
}

/** 连线还原：恢复首次着色前记录的描边（与 clearRunState 同一口径，供单条连线使用） */
const restoreEdge = (cell: any) => {
  if (!edgeStrokes.has(cell.id)) return
  const stroke = edgeStrokes.get(cell.id)
  if (undefined === stroke || null === stroke) cell.removeAttrByPath?.('line/stroke')
  else cell.attr('line/stroke', stroke)
  cell.removeAttrByPath?.('line/targetMarker/fill')
  edgeStrokes.delete(cell.id)
}

/** 已按命中分支着色的分支节点：其出边不再按「上一步 → 当前步」推断，避免未命中的分支被连带着色 */
const branchPainted = new Set<string>()

/**
 * 分支节点着色：命中分支由后端随步骤下发（条件分支 case-{标识}、默认分支 default、问题分类器 class-{标识}），
 * 与连线起点锚点同一口径，据此只给命中的那条连线着色，其余分支恢复原描边。
 * 老数据的锚点与连线对不上时退回按分支名称匹配；仍定位不到（锚点标识已丢失）就给该节点的
 * 分支连线一起着色，至少能看出这一层分支的走向，不会出现命中了却一条都不亮的情况
 */
const markBranch = (flow: any, cell: any, branch: string, state: string) => {
  if (!branch) return
  const edges: any[] = (flow?.graph?.getEdges?.() ?? []).filter((edge: any) => cell.id === edge.getSourceCellId?.())
  if (!edges.length) return
  // 锚点标识：终端里存的是标识本身，个别数据可能存的是锚点对象，两种都归一成标识
  const portId = (edge: any) => {
    const port: any = edge.getSourcePortId?.()
    return port && 'object' === typeof port ? String(port.id ?? '') : String(port ?? '')
  }
  let hit = edges.filter((edge: any) => branch === portId(edge))
  // 兜底一：按分支名称匹配（分支名会同步成连线名称，如 否则（默认））
  if (!hit.length) {
    const rows: any[] = SwitchLayout.rows(cell.getData?.() ?? {})
    const name = String(rows.find((row: any) => row.id === branch)?.name ?? '')
    if (name) hit = edges.filter((edge: any) => name === String(edge.getData?.()?.name ?? ''))
  }
  // 兜底二：无法确定命中分支时按用户口径把该节点的分支连线一起着色
  if (!hit.length) {
    // 留一条排查线索：命中分支与画布锚点都对不上时，把两侧的标识打出来（正常数据不会走到这里）
    console.warn('[agentic-branch] 未匹配到命中分支的连线，按该节点的全部分支着色', {
      branch, ports: edges.map(portId),
    })
    hit = edges
  }
  hit.forEach((edge: any) => markEdge(flow, edge, state))
  edges.filter((edge: any) => hit.indexOf(edge) < 0).forEach((edge: any) => restoreEdge(edge))
  branchPainted.add(cell.id)
  // 命中分支行：卡片里点亮命中的那一行、其余行淡化，默认分支命中时同样点亮
  const card: any = cellElement(flow, cell)?.querySelector?.('.agent-switch')
  if (card) {
    card.classList.add('is-branch-marked')
    const hits = hit.map(portId)
    card.querySelectorAll('.row[data-branch]').forEach((row: any) => {
      row.classList.toggle('is-hit', hits.indexOf(String(row.dataset.branch ?? '')) >= 0)
    })
  }
}

/** 两个节点之间的连线（含分支、容器内的连线） */
const edgesBetween = (flow: any, sourceId: string, targetId: string) => {
  return (flow?.graph?.getEdges?.() ?? []).filter((edge: any) => {
    return sourceId === edge.getSourceCellId?.() && targetId === edge.getTargetCellId?.()
  })
}

/** 相邻节点之间的连线着色：出边已按命中分支着色的分支节点不再重复推断 */
const markEdgesBetween = (flow: any, sourceId: string, targetId: string, state: string) => {
  edgesBetween(flow, sourceId, targetId).forEach((edge: any) => {
    if (branchPainted.has(edge.getSourceCellId?.())) return
    markEdge(flow, edge, state)
  })
}

/** 清空画布运行态：节点去掉状态类，连线恢复原描边 */
const clearRunState = () => {
  const flow: any = flowRef.value?.flow
  if (!flow?.graph) return
  branchPainted.clear()
  flow.graph.getCells?.().forEach((cell: any) => {
    const element: HTMLElement | null = cellElement(flow, cell)
    element?.classList.remove(...RUN_NODE_CLASSES)
    // 分支卡片的命中分支标记一并还原（命中行加粗、未命中行淡化）
    element?.querySelectorAll?.('.agent-switch.is-branch-marked').forEach((card: any) => {
      card.classList.remove('is-branch-marked')
      card.querySelectorAll('.row.is-hit').forEach((row: any) => row.classList.remove('is-hit'))
    })
    if (nodeFills.has(cell.id)) {
      const fill = nodeFills.get(cell.id)
      if (undefined === fill || null === fill) cell.removeAttrByPath?.('body/fill')
      else cell.attr('body/fill', fill)
      nodeFills.delete(cell.id)
    }
    restoreEdge(cell)
  })
}

/**
 * 实时进度：运行过程中后端每执行一个节点就推一次（开始 running、结束 success/failed），
 * 这里把节点与上一步到它的连线一起着色；收到的次数用于判断是否需要跑完后再回放一遍。
 */
let liveStepCount = 0
let livePreviousId = ''
const markRunStep = (step: any) => {
  // 抽屉已关闭时不再往画布上画状态（关闭即清空，避免运行中的事件又把颜色画回来）
  if (!runVisible.value) return
  const flow: any = flowRef.value?.flow
  const cell: any = flow?.graph?.getCellById?.(step?.id)
  if (!cell) return
  liveStepCount++
  if ('running' === step.state) {
    if (livePreviousId) markEdgesBetween(flow, livePreviousId, step.id, 'running')
    markNode(flow, cell, 'running')
    return
  }
  const state = 2 === step.status ? 'failed' : 'success'
  markNode(flow, cell, state)
  if (livePreviousId) markEdgesBetween(flow, livePreviousId, step.id, state)
  // 分支节点：命中分支的输出连线（默认分支同样着色）在这一步结尾定型
  markBranch(flow, cell, String(step.branch ?? ''), state)
  livePreviousId = step.id
}
/** 开始新一轮运行：清掉上一轮着色与实时进度计数 */
const resetRunState = () => {
  playToken++
  liveStepCount = 0
  livePreviousId = ''
  clearRunState()
}

/**
 * 调试回放：按执行顺序依次高亮节点与连线（执行中蓝、成功绿、失败红），
 * 并选中居中当前节点；运行失败时停在失败节点上，便于对照该节点的配置与入参。
 */
const playSteps = async (steps: any[], failed = '') => {
  const flow: any = flowRef.value?.flow
  if (!flow?.graph || !(steps ?? []).length) return
  // 回放令牌：关闭抽屉、新会话或再次回放时自增，旧的回放循环立即退出，
  // 否则它会继续按节奏选中节点，把用户点选的节点覆盖掉（属性面板看起来没反应）
  const token = ++playToken
  // 重新回放前先清掉上一轮的状态
  clearRunState()
  const before: any = activeItem.value
  let previous: any = null
  for (const step of steps) {
    if (token !== playToken) return
    const cell: any = flow.graph.getCellById?.(step.id)
    if (!cell) continue
    // 连线先按执行中着色，随后与该节点的结果一起定型
    if (previous) markEdgesBetween(flow, previous.id, cell.id, 'running')
    markNode(flow, cell, 'running')
    cell.select?.()
    // 必须用 cell2meta 的普通对象：activeItem 变化会触发 updateCell(cell)，
    // 传入活单元格时会 removeData 后读到已清空的 data，把节点数据抹掉
    activeItem.value = flow.cell2meta(cell)
    flow.graph.centerCell?.(cell, { padding: 80 })
    await new Promise((resolve) => setTimeout(resolve, 360))
    if (token !== playToken) return
    const state = 2 === step.status ? 'failed' : 'success'
    markNode(flow, cell, state)
    if (previous) markEdgesBetween(flow, previous.id, cell.id, state)
    // 分支节点：命中分支的输出连线（默认分支同样着色）与节点结果一起定型
    markBranch(flow, cell, String(step.branch ?? ''), state)
    previous = cell
  }
  const target: any = failed ? flow.graph.getCellById?.(failed) : null
  if (token !== playToken) return
  if (target) {
    target.select?.()
    activeItem.value = flow.cell2meta(target)
    return
  }
  // 成功的一轮回放结束后恢复原来的选中，避免打断正在编辑的节点
  if (before?.id) {
    const cell: any = flow.graph.getCellById?.(before.id)
    cell?.select?.()
    activeItem.value = cell ? flow.cell2meta(cell) : before
  }
}

/** 失败节点：运行结果里状态为失败的步骤对应的节点 */
const failedStep = (steps: any[]) => {
  const item: any = (steps ?? []).find((step: any) => 2 === step.status)
  return item?.id ?? ''
}

/**
 * 调试运行的输入项：画布数据不是响应式的，属性面板改完节点后需要重新读取，
 * 因此用可刷新的列表而不是 computed —— 打开抽屉与每次提交前各同步一次。
 */
const runVariables: any = ref<any[]>([])
const refreshRunVariables = () => {
  const nodes: any[] = flowRef.value?.flow?.graph?.getNodes?.() ?? []
  const start = nodes.find((node: any) => 'Start' === node.getData()?.type)
  runVariables.value = start ? config.startInputs(start.getData() ?? {}) : []
  runVariables.value.forEach((item: any) => {
    if (item.name && undefined === runInputs.value[item.name]) runInputs.value[item.name] = runDefault(item.type)
  })
}
// 固定输入：用户输入（query）与文件列表（files）并入底部输入区，其余为自定义参数
const queryParam = computed<any>(() => runVariables.value.find((item: any) => 'query' === item.name) ?? null)
const filesParam = computed<any>(() => runVariables.value.find((item: any) => 'Array<File>' === item.type) ?? null)
const runParams = computed<any[]>(() => runVariables.value.filter((item: any) => {
  return 'query' !== item.name && 'Array<File>' !== item.type
}))
const runFileList = computed<any[]>(() => {
  const name = filesParam.value?.name
  const value = name ? runInputs.value[name] : null
  return Array.isArray(value) ? value : []
})
// 输入区提示：占位用开始节点的用户输入说明，底部补充文件数量与类型限制
const composerPlaceholder = computed(() => {
  // 输入法回车用于选词，提示里写清换行与发送的按键
  if (!queryParam.value) return '开始节点未启用用户输入，可直接发送'
  return `${queryParam.value.description || '请输入内容'}（Enter 发送，Shift+Enter 换行）`
})
const composerTips = computed(() => filesParam.value ? runFileTips(filesParam.value) : '')
// 必填未填写的自定义参数：用于参数配置入口的红点提示与提交前校验
const paramBlank = (item: any) => {
  const value = runInputs.value[item.name]
  if (null === value || undefined === value) return true
  if ('boolean' === typeof value) return false
  return '' === String(value).trim()
}
const missingParams = computed<any[]>(() => runParams.value.filter((item: any) => true === item.required && paramBlank(item)))
const canSend = computed(() => {
  if (runLoading.value) return false
  if (queryParam.value) return '' !== String(runMessage.value ?? '').trim()
  return true
})
// 调试运行表单默认值：布尔取假值，数值留空，其余按文本处理
const runDefault = (type: string) => {
  if ('Boolean' === type) return false
  // 文件列表由上传得到，元素是文件存储服务返回的文件信息
  if ('Array<File>' === type) return []
  if (['Number', 'Integer'].indexOf(type) !== -1) return undefined
  return ''
}

/* ------------------------------- 文件上传（走文件服务） ------------------------------- */

const runFileLoading = ref('')

// 上传文件 → 文件服务返回 { id, name, type, ... }，作为开始节点 files 数组的元素
const handleRunFileUpload = (item: any, file: File) => {
  runFileLoading.value = item.name
  AgenticApi.upload(file, { success: false }).then((result: any) => {
    const data: any = ApiUtil.data(result)
    if (!data?.id) return
    const files: any[] = Array.isArray(runInputs.value[item.name]) ? runInputs.value[item.name] : []
    files.push({ id: data.id, name: data.name, type: data.type, suffix: data.suffix, size: data.size })
    runInputs.value[item.name] = files
  }).catch(() => {}).finally(() => {
    runFileLoading.value = ''
  })
  // 由前端调用上传接口，不使用 el-upload 的内置请求
  return false
}

const handleRunFileRemove = (item: any, file: any) => {
  const files: any[] = Array.isArray(runInputs.value[item.name]) ? runInputs.value[item.name] : []
  runInputs.value[item.name] = files.filter((row: any) => row.id !== file.id)
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

// 文件列表取值：优先按文件信息 JSON 数组解析，只填文件ID时按 { id } 提交
const runFiles = (value: any) => {
  const text = String(value ?? '').trim()
  if (!text) return []
  if ('[' === text.charAt(0)) {
    try {
      const parsed = JSON.parse(text)
      if (Array.isArray(parsed)) return parsed
    } catch (error) {
      // 非法 JSON 时回落到文件ID列表
    }
  }
  return text.split(',')
    .map((item: string) => item.trim())
    .filter((item: string) => item)
    .map((id: string) => ({ id }))
}

const handleRun = () => {
  if (!diagram.value.id) {
    ElMessage.warning('请先保存编排后再运行')
    return
  }
  runResult.value = null
  // 每次打开都按当前画布同步输入项，避免属性面板改动后表单仍是旧清单
  refreshRunVariables()
  runVisible.value = true
  nextTick(() => scrollRunChat())
}

const handleRunSubmit = (message = '', options: any = {}) => {
  // 提交前再同步一次：新增/删除参数后必填校验与表单保持一致，不会误报「未填写」
  refreshRunVariables()
  const missing = runVariables.value.filter((item: any) => true === item.required && paramBlank(item))
  if (missing.length && !message) {
    // 必填缺失不弹窗，挂到这条用户消息上（气泡左侧出现异常图标，点开看详情）
    const names = missing.map((item: any) => item.label || item.name).join('、')
    attachNotice(`请填写必填参数：${names}`, `在右上角「参数配置」中填写后重新发送：${names}`)
    return
  }
  const inputs: any = {}
  runVariables.value.forEach((item: any) => {
    const value = runInputs.value[item.name]
    // 文件列表为文件信息数组，支持 JSON 数组或逗号分隔的文件ID
    // 文件列表为文件信息数组（由上传得到），直接按数组传入
    inputs[item.name] = 'Array<File>' === item.type ? (Array.isArray(value) ? value : []) : value
  })
  // 对话输入：作为开始节点的 query 传入（多轮对话时同一会话内沿用其它入参）
  if (message) inputs.query = message
  runLoading.value = true
  // 新一轮运行：清掉上一轮的画布运行态与实时进度计数（本轮节点会边执行边着色）
  resetRunState()
  // 分支位置：新消息接在 parentId 之后（未指定时接在当前分支尾）
  const parentId = Number(options?.parentId ?? 0) || Number(branch.activeLeaf.value ?? 0) || 0
  // 流式运行：先建好这一轮的助手消息，模型增量到达时实时追加
  // 用 reactive 包一层：push 进数组后仍在改这个对象，直接改原始对象不会触发渲染
  const reply: any = reactive({
    role: 'assistant',
    id: 0,
    content: '',
    reasoning: '',
    // 流式中：内容为空时先展示“输出中”，避免出现“（无回复内容）”闪烁
    streaming: true,
    status: 1,
    error: '',
    duration: 0,
    steps: [],
    // 实时执行进度：后端每执行一个节点推一次 step 事件（只含节点与状态）
    progress: [],
    // 实时 ReAct 轮次：后端每轮模型推理与每次工具方法调用推一次 round 事件
    rounds: [],
    result: null,
    notice: null,
    feedbackEmotion: '',
    feedbackTag: '',
    feedbackContent: '',
    parentId,
  })
  runMessages.value.push(reply)
  // 分支视图先切到这条尚未落库的回复：重新生成时上一次的输出随之隐藏，可再用「◀ n/m ▶」切回；
  // 新提问一并登记，接在会话起点（编辑第一条提问）时也能算出当前分支，不必退回整条会话
  branch.setPending(reply, options?.question ?? null)
  nextTick(() => scrollRunChat())
  // 本轮上下文：流式回调按「当前回复」写屏，发送前先登记（重新生成时没有新的用户消息）
  turn.reply = reply
  turn.question = options?.question ?? null
  streaming.value = true
  stream.send({
    id: diagram.value.id,
    chatId: runChatId.value,
    // 分支位置：新消息接在 parentId 之后；reuseQuestion 表示这是重新生成
    parentId,
    reuseQuestion: true === options?.reuseQuestion,
    inputs,
  })
}

/** 本轮上下文：流式回调在 send 前登记，回调里只认这一份 */
const turn: { reply: any, question: any } = { reply: null, question: null }

/**
 * 流式运行：事件分发与运行状态复位统一收在组合式函数里；
 * 调试面板失败就是失败（fallback: 'none'），不在本页做一次性重跑。
 */
const stream = useAgenticStream({
  ...streams.agenticRun,
  headers: { 'X-Auth-Token': user.info.token },
  fallback: 'none',
  onDelta: (chunk: any) => {
    const reply: any = turn.reply
    if (!reply) return
    // 新一轮模型调用：思考内容重置（中间轮次的推理不留在最终「思考过程」里）
    if (chunk.reasoningReset) reply.reasoning = ''
    if (chunk.replace) {
      // 最终回复：整段替换（模板拼装、非模型输出的场景）
      reply.content = String(chunk.content ?? '')
    } else {
      reply.content = String(reply.content ?? '') + String(chunk.content ?? '')
      if (chunk.reasoning) reply.reasoning = String(reply.reasoning ?? '') + String(chunk.reasoning)
    }
    nextTick(() => scrollRunChat())
  },
  // 节点执行进度：执行过程时间线实时刷新，流程图上的节点与连线同步着色
  onStep: (step: any) => {
    const reply: any = turn.reply
    if (reply) AgenticUtil.markStep(reply.progress, step)
    markRunStep(step)
    nextTick(() => scrollRunChat())
  },
  // ReAct 轮次：模型推理开始、工具方法开始调用与返回都实时补进执行过程
  onRound: (round: any) => {
    const reply: any = turn.reply
    if (reply) AgenticUtil.markRound(reply.rounds, round)
    nextTick(() => scrollRunChat())
  },
  // 业务失败（授权被撤销、缺少必填参数）与连接异常都走气泡外的异常提示，不重跑
  onError: ({ message }: any) => {
    const reply: any = turn.reply
    if (reply) reply.streaming = false
    attachNotice(message || '运行失败')
  },
  onDone: (data: any) => applyRunResult(data, turn.reply),
  onClose: () => {
    if (turn.reply) turn.reply.streaming = false
    streaming.value = false
    runLoading.value = false
  },
})

/** 运行结果落到这一轮的助手消息与运行结果面板上（由组合式函数的 onDone 调用，入参已解包） */
const applyRunResult = (data: any, reply: any) => {
  if (null == data) {
    attachNotice('运行失败')
    return
  }
  runResult.value = data
  runChatId.value = data?.chatId ?? 0
  reply.streaming = false
  // 思考过程：流式没有捕获到时从节点输出里补（与对话历史的取值口径一致）
  if (!reply.reasoning) {
    const outputs: any = data?.outputs ?? {}
    const source: string = Object.keys(outputs).find((id: string) => outputs[id]?.reasoning) ?? ''
    if (source) reply.reasoning = outputs[source].reasoning
  }
  // 节点/工具调用异常：挂到这一轮的节点回复上，气泡左侧展示异常图标
  const failures = AgenticUtil.runFailures(data)
  Object.assign(reply, {
    id: data?.answerId ?? 0,
    // 最终回复为空时保留流式预览（失败或空回复场景别把已经流出来的内容抹掉）
    content: data?.answer || reply.content || '',
    status: data?.status ?? 1,
    error: data?.error ?? '',
    duration: data?.duration ?? 0,
    steps: data?.steps ?? [],
    result: data,
    // 摘要取第一条异常，详情只放其余异常，避免同一条信息展示两遍
    notice: failures.length ? { summary: failures[0], detail: failures.slice(1).join('\n') } : null,
  })
  // 落库标识与分支位置：提问补 id、回复补父消息，分支尾指向本轮回复
  if (turn.question) turn.question.id = data?.questionId ?? turn.question.id ?? 0
  reply.parentId = data?.questionId ? data.questionId : reply.parentId
  branch.commit(reply, data?.leafId)
  // 抽屉已关闭：只更新消息，不把运行态画回画布（关闭抽屉即清空状态）
  if (!runVisible.value) return
  const failedId = failedStep(data?.steps ?? [])
  // 运行过程中已经按步骤实时着色，这里不再整体回放，只把失败节点定位出来看配置与入参
  if (liveStepCount > 0) {
    const flow: any = flowRef.value?.flow
    const cell: any = failedId ? flow?.graph?.getCellById?.(failedId) : null
    if (cell) {
      cell.select?.()
      activeItem.value = flow.cell2meta(cell)
      flow.graph.centerCell?.(cell, { padding: 80 })
    }
    return
  }
  // 没收到实时进度（旧后端或单次返回）时按步骤回放执行过程
  playSteps(data?.steps ?? [], failedId)
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
      <X6Container ref="flowRef" v-model="diagram" :active-item="activeItem" :tips="tips" :options="options" @update:active-item="handleActiveItem" />
    </template>
    <template #right>
      <!-- 只按应用类型重建：同一类型的节点之间切换时复用面板实例，
           这样分组折叠状态、编辑器实例不会被重置（各面板已改为可响应数据切换） -->
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
  <!--
    调试抽屉：resizable 可拖拽调宽；:modal="false" + modal-penetrable 使遮罩可穿透，
    底层画布仍可拖动、点选节点；关闭 esc / 点遮罩关闭，调试时可以在画布与对话之间来回操作
  -->
  <el-drawer
    v-model="runVisible"
    class="run-drawer"
    :direction="runDirection"
    size="560px"
    resizable
    :modal="false"
    modal-penetrable
    :close-on-click-modal="false"
    :close-on-press-escape="false">
    <!-- 头部：停靠方向切换用图标放在标题左侧 -->
    <template #header="{ titleId, titleClass }">
      <div class="run-header">
        <el-button
          link
          size="small"
          :title="'rtl' === runDirection ? '停靠到左侧' : '停靠到右侧'"
          @click="handleRunDirection">
          <LayoutIcon name="Switch" />
        </el-button>
        <span class="run-title" :id="titleId" :class="titleClass">调试运行（使用保存后的草稿内容）</span>
      </div>
    </template>
    <div class="run-panel">
      <!-- 顶部工具条：会话维度的操作 -->
      <div class="run-head">
        <span class="run-head-title">
          <LayoutIcon name="ChatDotRound" />
          <span>{{ runChatId ? '调试会话进行中' : '新会话' }}</span>
        </span>
        <el-space>
          <el-button link type="primary" :disabled="!(runResult?.steps ?? []).length" @click="playSteps(runResult?.steps ?? [], failedStep(runResult?.steps ?? []))">回放流程</el-button>
          <el-button link type="primary" @click="handleRunNew">新会话</el-button>
          <!-- 参数配置：右上角下拉菜单里填写自定义参数，不占对话区高度 -->
          <el-dropdown
            v-if="runParams.length"
            trigger="click"
            popper-class="run-params-popper"
            :hide-on-click="false"
            placement="bottom-end"
            :show-timeout="0">
            <el-button link type="primary" class="params-trigger" title="填写开始节点的自定义参数">
              <LayoutIcon name="Setting" />
              <span>参数配置（{{ runParams.length }}）</span>
              <!-- 必填未填写时给一个红点，避免发送后才被发现 -->
              <span class="params-dot" v-if="missingParams.length"></span>
            </el-button>
            <template #dropdown>
              <div class="run-params-panel">
                <div class="params-head">参数配置</div>
                <div class="params-tips" v-if="missingParams.length">必填未填写：{{ missingParams.map((item: any) => item.label || item.name).join('、') }}</div>
                <el-form label-position="top">
                  <el-form-item :key="item.name" v-for="item in runParams">
                    <template #label>
                      <span>{{ item.label || item.name }}</span>
                      <span class="run-alias" v-if="item.label && item.label !== item.name">{{ item.name }}</span>
                      <el-tag v-if="item.required" type="danger" size="small" effect="plain" class="run-required">必填</el-tag>
                    </template>
                    <el-input
                      v-if="'Paragraph' === item.type"
                      v-model="runInputs[item.name]"
                      type="textarea"
                      :rows="2"
                      :placeholder="item.description" />
                    <el-input-number
                      v-else-if="['Number', 'Integer'].indexOf(item.type) !== -1"
                      v-model="runInputs[item.name]"
                      :controls="false"
                      :placeholder="item.description" />
                    <el-switch v-else-if="'Boolean' === item.type" v-model="runInputs[item.name]" />
                    <el-input v-else v-model="runInputs[item.name]" :placeholder="item.description" />
                  </el-form-item>
                </el-form>
              </div>
            </template>
          </el-dropdown>
        </el-space>
      </div>
      <!-- 消息列表：标准对话窗口，占满剩余高度并自动滚到底部 -->
      <div class="run-chat" ref="runChatRef" @scroll="handleScroll">
        <ChatMessage
          class="run-message"
          :key="index"
          v-for="(item, index) in runVisibleMessages"
          :item="item"
          :streaming="item.streaming"
          :disabled="feeding === item.id"
          :branch="branch.branchOf(item)"
          :editable="'user' === item.role && !!item.id && !runLoading"
          :regenerable="'assistant' === item.role && !!item.id && !runLoading"
          @submit="(payload: any) => handleRunFeedback(item, payload)"
          @switch="(step: number) => handleRunSwitch(item, step)"
          @edit="(content: string) => handleRunEdit(item, content)"
          @regenerate="handleRunRegenerate(item)">
          <!-- 执行过程（顶部）：与流程对话同一形态的轻量时间线 -->
          <template #steps>
            <AgenticTimeline
              v-if="'assistant' === item.role && (item.streaming || (item.steps ?? []).length)"
              :steps="item.steps"
              :progress="item.progress"
              :rounds="item.rounds"
              :streaming="item.streaming" />
          </template>
          <!-- 执行明细（底部）：逐节点完整内容 + 本轮原始输出，排查用，与对话历史详情共用组件 -->
          <template #extra>
            <AgenticSteps
              class="run-detail"
              v-if="'assistant' === item.role && (item.steps ?? []).length"
              :steps="item.steps"
              :title="`执行明细（${item.steps.length} 个节点 · ${item.duration} 毫秒）`"
              output>
              <!-- 本轮原始输出：需要排查细节时展开，不占据对话主界面 -->
              <el-collapse class="run-raw">
                <el-collapse-item title="原始输出">
                  <ChatTextBlock :value="item.result ?? {}" />
                </el-collapse-item>
              </el-collapse>
            </AgenticSteps>
          </template>
        </ChatMessage>
        <!-- 运行中：与对话页一致显示思考中 -->
        <ChatMessage
          class="run-message"
          v-if="runLoading && !streaming"
          :item="{ role: 'assistant', streaming: true }" />
      </div>
      <!-- 底部输入区：一个输入框内三行 —— 文件列表、文本区域、按钮区域 -->
      <div class="run-composer">
        <div class="composer-box">
          <!-- 第 1 行：已上传文件的回显 -->
          <div class="composer-files" v-if="runFileList.length">
            <el-tag
              class="run-file"
              :key="file.id"
              closable
              size="small"
              type="info"
              effect="plain"
              v-for="file in runFileList"
              @close="handleRunFileRemove(filesParam, file)">{{ file.name || file.id }}</el-tag>
          </div>
          <!-- 第 2 行：文本区域占满整宽，高度只随文本增减（不含按钮行） -->
          <el-input
            class="composer-input"
            v-model="runMessage"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 8 }"
            resize="none"
            :placeholder="composerPlaceholder"
            @keydown.enter="handleComposerSend" />
          <!-- 第 3 行：附件与发送 -->
          <div class="composer-tools">
            <div class="composer-tools-main">
              <el-upload v-if="filesParam" :show-file-list="false" :before-upload="(file: File) => handleRunFileUpload(filesParam, file)">
                <el-tooltip :content="composerTips ? '上传文件：' + composerTips : '上传文件'" placement="top">
                  <el-button class="composer-attach" link :icon="Paperclip" :loading="runFileLoading === filesParam.name" />
                </el-tooltip>
              </el-upload>
              <span class="composer-tips" v-if="composerTips">{{ composerTips }}</span>
            </div>
            <div class="composer-tools-end">
              <el-button
                class="composer-send"
                circle
                type="primary"
                :icon="Promotion"
                title="发送"
                :loading="runLoading"
                :disabled="!canSend"
                @click="handleRunChat" />
            </div>
          </div>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<style lang="scss" scoped>
/**
 * 对话区域公共列宽：限宽并居中，抽屉拉宽后消息、工具条与输入框保持同一列
 */
@mixin run-column {
  width: 100%;
  min-width: min(320px, 100%);
  max-width: 900px;
  margin-left: auto;
  margin-right: auto;
}
.diagram-name {
  font-size: 13px;
  color: var(--el-text-color-regular);
}
/* 调试抽屉：工具条 + 消息列表 + 参数 + 底部输入区，整体按对话窗口自上而下排列 */
.run-panel {
  display: flex;
  flex-direction: column;
  flex: 1;
  gap: 8px;
  height: 100%;
  min-height: 0;
}
/* 抽屉头部：方向切换图标 + 标题（标题占满剩余宽度，关闭按钮仍在最右） */
.run-header {
  flex: 1;
  min-width: 0;
  @include flex-start();
  gap: 6px;
  .run-title {
    flex: 1;
    min-width: 0;
    @include text-wrap();
  }
}
.run-head {
  @include flex-between();
  @include run-column();
  flex: none;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  .run-head-title {
    @include flex-start();
    gap: 4px;
  }
  /* 参数配置入口：必填未填写时在按钮右上角标一个红点 */
  .params-trigger {
    position: relative;
    .params-dot {
      position: absolute;
      top: -2px;
      right: -6px;
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: var(--el-color-danger);
    }
  }
}
.run-chat {
  @include run-column();
  /* 撑满输入区以上的剩余高度：输入区因此始终贴在容器最底部 */
  flex: 1;
  min-height: 0;
  padding: 8px;
  overflow: auto;
  border-radius: 4px;
  background: var(--el-fill-color-lighter);
  /* 消息行：结构与通用样式在 components/Chat/ChatMessage.vue，这里只保留模型对话自己的口径 */
  .run-message {
    /* 头像尺寸与间距：气泡的左右边界以两侧头像的竖线为准 */
    --run-avatar-size: 26px;
    --run-gap: 8px;
    gap: var(--run-gap);
    &.is-user {
      flex-direction: row-reverse;
      /* 用户气泡外层多了操作条容器（已排过头像），宽度上限按容器算，不再重复扣头像宽度 */
      :deep(.chat-bubble) {
        max-width: 100%;
      }
    }
  }
  :deep(.chat-avatar) {
    width: var(--run-avatar-size);
    height: var(--run-avatar-size);
    font-size: 14px;
  }
  /* 气泡按内容收起、超长时最多到对侧头像的竖线，左右都不越过头像 */
  :deep(.chat-bubble) {
    flex: 0 1 auto;
    max-width: calc(100% - var(--run-avatar-size) - var(--run-gap));
  }
  /* 折叠面板头：默认 48px 对气泡内的过程信息太高，压到 28px（与调试面板一致） */
  :deep(.chat-reasoning .el-collapse-item__header) {
    height: 28px;
  }
}
.run-detail {
  /* 明细区：上边留白，底部收敛，避免气泡下方空出一段 */
  margin: 8px 0 0;
  /**
   * 折叠面板的类就加在这个根元素上（不是后代），自带上下两条边框：
   * 都去掉，避免明细底部与气泡边框叠成多条线
   */
  &.el-collapse {
    border-top: none;
    border-bottom: none;
  }
  /* 头部下边框在收起时就是气泡底部的那条线，一并去掉；展开后由内容留白区分 */
  :deep(.el-collapse-item__header) {
    height: 32px;
    font-size: 12px;
    border-bottom: none;
  }
  /* 内容外层同样带一条下边框（收起时也会画出来），去掉避免叠成多条线 */
  :deep(.el-collapse-item__wrap) {
    border-bottom: none;
  }
  /* 展开后的内容底部留白收敛 */
  :deep(.el-collapse-item__content) {
    padding-bottom: 6px;
  }
}


/* 执行明细里的原始输出：折在明细内部，默认收起 */
.run-raw {
  margin: 8px 0 0;
  /* 同上：根元素自身带边框，全部去掉，避免嵌套折叠面板叠出多余线条 */
  &.el-collapse {
    border-top: none;
    border-bottom: none;
  }
  :deep(.el-collapse-item__header) {
    height: 28px;
    font-size: 12px;
    border-bottom: none;
  }
  :deep(.el-collapse-item__wrap) {
    border-bottom: none;
  }
  :deep(.el-collapse-item__content) {
    padding-bottom: 4px;
  }
}

/* 底部输入区：一个输入框内三行 —— 文件列表、文本区域、按钮区域 */
.run-composer {
  @include run-column();
  flex: none;
  .composer-box {
    padding: 8px 10px;
    border-radius: 8px;
    border: solid 1px var(--el-border-color);
    background: var(--el-bg-color);
    &:focus-within {
      border-color: var(--el-color-primary);
    }
  }
  /* 第 1 行：已上传文件，没有文件时整行不占位 */
  .composer-files {
    margin-bottom: 6px;
    @include flex-wrap();
    gap: 6px;
    .run-file {
      max-width: 100%;
    }
  }
  /* 第 2 行：文本区域占满整宽，高度只随文本增减（不含按钮行） */
  .composer-input {
    width: 100%;
    :deep(.el-textarea__inner) {
      padding: 0;
      border: none;
      box-shadow: none;
      background: transparent;
      font-size: 13px;
      line-height: 1.7;
    }
  }
  /* 第 3 行：左侧附件与文件限制说明，右侧发送 */
  .composer-tools {
    @include flex-between();
    margin-top: 6px;
    .composer-tools-main {
      @include flex-start();
      min-width: 0;
      gap: 8px;
    }
    .composer-attach {
      color: var(--el-text-color-placeholder);
      &:hover {
        color: var(--el-color-primary);
      }
    }
    .composer-tips {
      font-size: 12px;
      color: var(--el-text-color-placeholder);
      @include text-wrap();
    }
    .composer-tools-end {
      flex: 0 1 auto;
      min-width: 0;
      @include flex-start();
      gap: 8px;
    }
  }
}
</style>

<style lang="scss">
/* 调试回放的节点运行态：用背景色标识执行进度（蓝=执行中，绿=成功，红=失败） */
.x6-run-running .agent-node,
.x6-run-running .agent-switch {
  background-color: var(--el-color-primary-light-8);
}
.x6-run-success .agent-node,
.x6-run-success .agent-switch {
  background-color: var(--el-color-success-light-8);
}
.x6-run-failed .agent-node,
.x6-run-failed .agent-switch {
  background-color: var(--el-color-danger-light-8);
}
/* 抽屉内容区铺满高度，消息列表才能撑开并让底部输入区固定在下沿（类名已加前缀避免污染） */
.run-drawer {
  /* 拖拽调宽时的下限：内容列最小 320px 加左右内边距，避免被拖到不可用 */
  min-width: min(480px, 100%);
}
.run-drawer .el-drawer__body {
  display: flex;
  flex-direction: column;
  padding: 12px 16px 16px;
  overflow: hidden;
}
/* 参数配置下拉：内容挂载在 body 上，样式需全局作用域；参数多时面板内部滚动 */
.run-params-popper {
  padding: 0 !important;
}
.run-params-panel {
  width: 320px;
  max-height: 60vh;
  padding: 12px;
  overflow: auto;
  .params-head {
    font-size: 13px;
    font-weight: 500;
    color: var(--el-text-color-primary);
  }
  .params-tips {
    margin-top: 6px;
    font-size: 12px;
    line-height: 1.6;
    color: var(--el-color-danger);
  }
  .el-form {
    margin-top: 8px;
  }
  .el-form-item {
    margin-bottom: 10px;
    &:last-child {
      margin-bottom: 0;
    }
  }
  /* 标签行：变量名称跟在标题名称后面，必填标签靠右 */
  .run-alias {
    margin-left: 6px;
    font-size: 12px;
    font-weight: normal;
    color: var(--el-text-color-placeholder);
  }
  .run-required {
    margin-left: 6px;
  }
  .el-input, .el-input-number, .el-select {
    width: 100%;
  }
}
</style>
