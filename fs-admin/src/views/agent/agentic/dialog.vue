<script setup lang="ts">
/**
 * 流程对话 - 面向最终用户：选择已发布且授权给自己的流程进行多轮对话。
 * 左侧是本人的对话历史（支持标题筛选，可新建会话），右侧是对话框；
 * 每轮回复下面可展开「执行过程」，只展示节点与工具方法名及状态，用于定位追踪；
 * 详细的入参、返回结果与完整节点输出在「运行日志」页查看。
 */
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { MagicStick, Delete, Paperclip, Plus, Promotion, Search, Switch, Clock } from '@element-plus/icons-vue'
import AgenticApi from '@/api/agent/AgenticApi'
import streams from '@/api/agent/streams'
import ChatElevator from '@/components/Chat/ChatElevator.vue'
import ChatMessage from '@/components/Chat/ChatMessage.vue'
import useAgenticStream from '@/composables/useAgenticStream'
import useChatFeedback from '@/composables/useChatFeedback'
import useChatBranch from '@/composables/useChatBranch'
import useChatScroll from '@/composables/useChatScroll'
import AgenticUtil from '@/utils/AgenticUtil'
import ApiUtil from '@/utils/ApiUtil'
import DateUtil from '@/utils/DateUtil'
import TableUtil from '@/utils/TableUtil'
import { useUserStore } from '@/stores/user'
import { useRoute, useRouter } from 'vue-router'

const user = useUserStore()
const route = useRoute()
const router = useRouter()
/** 可对话的流程：已发布、状态启用，且授权角色命中当前用户 */
const agents = ref<any[]>([])
const agenticId = ref<any>('')
const agentic = computed<any>(() => agents.value.find((item: any) => String(item.id) === String(agenticId.value)) ?? {})

/** 左侧会话列表：本人（createdUid）的发布应用会话，支持标题筛选 */
const keyword = ref('')
const chats = ref<any[]>([])
const chatsLoading = ref(false)
const chatId = ref(0)
/** 消息区滚动：贴底才跟随新内容，向上翻阅历史时不被拉回（见 composables/useChatScroll） */
const { bodyRef: chatRef, handleScroll, scrollBottom } = useChatScroll()
const info: any = ref({})
const infoLoading = ref(false)
/**
 * 对话分支：消息按 parentId 组成消息树，页面只渲染当前分支那条路径，
 * 同一处有多条分支时（编辑提问 / 重新生成）显示「◀ 2/3 ▶」切换（见 composables/useChatBranch）
 */
const branch = useChatBranch()
const messages = computed<any[]>(() => branch.visible.value)

/** 路由里的对话标识：刷新或重新打开页面时据此恢复历史会话 */
const routeChatId = () => Number(route.query.chatId ?? 0) || 0
/** 对话标识写回路由：选中会话、会话落库、新建会话时同步，地址栏始终对应当前会话 */
const syncRouteChat = (id: any) => {
  const value = Number(id ?? 0) || 0
  if (value === routeChatId()) return
  const query: any = { ...route.query }
  if (value) query.chatId = String(value)
  else delete query.chatId
  router.replace({ query })
}

const loadAgents = () => {
  return AgenticApi.authorized({ warning: false }).then((result: any) => {
    agents.value = ApiUtil.data(result)?.rows ?? []
    if (!agenticId.value && agents.value.length) agenticId.value = agents.value[0].id
  }).catch(() => {})
}

const loadChats = () => {
  chatsLoading.value = true
  return AgenticApi.chatList({
    page: 1,
    limit: 100,
    type: 'published',
    title: keyword.value,
    createdUid: user.info.id,
  }, { warning: false }).then((result: any) => {
    chats.value = ApiUtil.data(result)?.rows ?? []
  }).catch(() => {}).finally(() => {
    chatsLoading.value = false
  })
}

/**
 * 会话标题筛选：输入、回车、清空都走这一处防抖，
 * 避免同一次筛选被 change 与 keyup.enter 各触发一次（连续输入也只发一次请求）
 */
let keywordTimer: any = null
watch(keyword, () => {
  window.clearTimeout(keywordTimer)
  keywordTimer = window.setTimeout(() => loadChats(), 300)
})

/** 打开历史会话：编排标识取自该会话的运行记录，右侧继续沿用同一会话 */
const handleOpen = (item: any) => {
  chatId.value = item.id
  // 选中的会话写进路由，刷新后仍停在这一条历史记录上
  syncRouteChat(item.id)
  infoLoading.value = true
  AgenticApi.chatInfo(item.id, { warning: false }).then((result: any) => {
    info.value = ApiUtil.data(result) ?? {}
    // 装载消息树：leafId 是会话当前分支尾，缺失时取最后一条消息
    branch.setMessages(info.value?.messages, info.value?.leafId)
    const runs = info.value?.runs ?? []
    if (runs.length) agenticId.value = runs[0].agenticId
    nextTick(() => scrollBottom(true))
  }).catch(() => {}).finally(() => {
    infoLoading.value = false
  })
}

/** 新建会话：保留当前选择的流程，发送第一条消息时创建会话 */
const handleNew = () => {
  chatId.value = 0
  syncRouteChat(0)
  info.value = {}
  branch.reset()
  message.value = ''
}

/** 删除会话：确认后删除，删的是当前打开的会话时回到新会话状态 */
/** 消息反馈：回复落库后（带 id）可点赞点踩，再次提交同一情绪表示取消（见 composables/useChatFeedback） */
const { feeding, submit: handleFeedback } = useChatFeedback()

const handleDeleteChat = (item: any) => {
  TableUtil.selection([item]).then((ids: any) => {
    AgenticApi.chatDelete(ids, { success: true }).then(() => {
      if (chatId.value === item.id) handleNew()
      loadChats()
    }).catch(() => {})
  }).catch(() => {})
}

/**
 * 发送消息：发布应用走流式外部调用（/agentic/invokeStream），模型与思考内容实时上屏，
 * 结束后取回运行结果（会话标识、日志标识）并刷新会话列表
 */
const handleSend = () => {
  const text = String(message.value ?? '').trim()
  // 允许只发附件：有文件没有文字时同样提交（query 传空）；
  // 开始节点关闭「用户输入」时没有可填的内容，直接发送（只带参数运行）
  if (queryInput.value && !text && !runFiles.value.length) return
  if (!agenticId.value) {
    ElMessage.warning('请先选择要对话的流程')
    return
  }
  // 新建会话：先补齐必填参数，避免发出去才被后端拦下「缺少必填参数」
  if (!chatId.value && missingParams.value.length) {
    ElMessage.warning(`请填写必填参数：${missingText.value}`)
    return
  }
  // 关闭文件列表后不再提交附件（切换开关前已选的附件也不带上）
  const files = filesInput.value ? runFiles.value.slice() : []
  message.value = ''
  runFiles.value = []
  // 续写位置：会话当前分支尾（没有分支时就是最后一条消息）
  sendRound({ text, files, parentId: branch.activeLeaf.value })
}

/** 发送按钮可用性：关闭用户输入时可直接发送，否则至少要有文字或附件 */
const canSend = computed(() => !queryInput.value || !!String(message.value ?? '').trim() || runFiles.value.length > 0)

/**
 * 起一轮对话：普通提问落「用户气泡 + 回复气泡」，重新生成（reuseQuestion）只落回复气泡。
 * parentId 指定这条消息接在哪条消息之后，新起的分支与原分支同时保留（见 composables/useChatBranch）。
 */
const sendRound = (options: any) => {
  if (sending.value) {
    ElMessage.warning('上一轮对话还在运行中，请稍候')
    return
  }
  const text = String(options?.text ?? '')
  const files: any[] = Array.isArray(options?.files) ? options.files.slice() : []
  const parentId = Number(options?.parentId ?? 0) || 0
  const reuseQuestion = true === options?.reuseQuestion
  sending.value = true
  // 消息统一放在分支视图的消息数组里（历史会话装载后与 info.messages 是同一个数组）
  const list: any[] = branch.messages.value
  let question: any = null
  if (!reuseQuestion) {
    // 先补一条提问，避免等待期间看不到自己发的内容
    // 没有用户输入也没有附件时（开始节点关闭了固定输入）只说明「按参数运行」
    const content = text || (files.length ? `（上传了 ${files.length} 个文件）` : '（按参数运行）')
    question = { role: 'user', content, createdTime: Date.now(), notice: null, parentId }
    if (files.length) question.files = files
    list.push(question)
  }
  // 本轮的回复消息：流式增量直接写在这里，结束后补上日志标识与异常提示
  const reply: any = reactive({
    role: 'assistant',
    content: '',
    reasoning: '',
    streaming: true,
    notice: null,
    createdTime: 0,
    logId: 0,
    charts: [],
    parentId,
    // 实时执行进度：后端每执行一个节点推一次 step 事件（只含节点与状态）
    progress: [],
    // 实时 ReAct 轮次：后端每轮模型推理与每次工具方法调用推一次 round 事件
    rounds: [],
    // 完整步骤：运行结束后后端随运行结果返回，历史会话再按 logId 拉取
    steps: [],
  })
  list.push(reply)
  // 分支视图先切到这条尚未落库的回复：重新生成时上一次的输出随之隐藏，可再用「◀ n/m ▶」切回；
  // 新提问一并登记，接在会话起点（编辑第一条提问）时也能算出当前分支，不必退回整条会话
  branch.setPending(reply, question)
  nextTick(() => scrollBottom(true))
  // 本轮上下文：流式回调按「当前提问 / 当前回复」写屏，发送前先登记
  turn.reply = reply
  turn.question = question
  stream.send({
    id: agenticId.value,
    chatId: chatId.value,
    // 分支位置：新消息接在 parentId 之后；reuseQuestion 表示这是重新生成
    parentId,
    reuseQuestion,
    // 固定输入按开始节点的启用情况提交：关闭的输入不入参（与调试运行同一口径），
    // 自定义参数随每轮入参一起提交
    inputs: {
      ...paramInputs(),
      ...(queryInput.value ? { query: text } : {}),
      ...(filesInput.value ? { files } : {}),
    },
  })
}

/** 切换分支：按当前消息的兄弟节点前后移动（只影响展示与续写位置，不动数据） */
const handleSwitch = (item: any, step: number) => {
  branch.switchBranch(item, step)
  nextTick(() => scrollBottom(true))
}

/** 编辑提问后重新发送：新消息接在被编辑消息的父节点下，形成新分支 */
const handleEdit = (item: any, content: string) => {
  sendRound({ text: content, files: item?.files ?? [], parentId: item?.parentId ?? 0 })
}

/** 重新生成回复：不重复落用户消息，只在同一条提问下新增一条助手回复 */
const handleRegenerate = (item: any) => {
  const question: any = branch.messages.value.find((row: any) => String(row?.id) === String(item?.parentId))
  if (!question) {
    ElMessage.warning('找不到该回复对应的提问，无法重新生成')
    return
  }
  sendRound({ text: String(question.content ?? ''), files: question.files ?? [], parentId: item.parentId, reuseQuestion: true })
}

const sending = ref(false)
const message = ref('')
/** 附件：走文件服务存储，随本轮 inputs.files 传给开始节点（与调试面板一致） */
const runFiles = ref<any[]>([])
const uploading = ref(false)
const handleUpload = (file: File) => {
  uploading.value = true
  AgenticApi.upload(file, { success: false }).then((result: any) => {
    const data: any = ApiUtil.data(result)
    if (data?.id) runFiles.value.push(data)
  }).catch(() => {
    ElMessage.error('文件上传失败')
  }).finally(() => {
    uploading.value = false
  })
  return false
}

/**
 * 流式一个事件都没收到时的一次性兜底（由组合式函数触发）：走 /agentic/invoke 把本轮跑完，
 * 结果同样回填正文、思考过程、图表、日志标识与异常提示，页面不会因为流式不可用而报错
 */
const fallbackInvoke = (payload: any, reply: any, question: any) => {
  if (!reply || !question) return Promise.resolve()
  // 本轮已转成一次性调用：先收起「输出中」状态，等待结果回填即可
  reply.streaming = false
  return AgenticApi.invoke(payload, { success: false, warning: false }).then((result: any) => {
    const data: any = ApiUtil.data(result)
    if (null == data) {
      question.notice = {
        summary: ApiUtil.message(result) || '发送失败',
        detail: ApiUtil.code(result) ? `状态码：${ApiUtil.code(result)}` : '',
      }
      return
    }
    reply.content = data.answer || reply.content || ''
    if (!reply.reasoning) {
      const outputs: any = data?.outputs ?? {}
      const source: string = Object.keys(outputs).find((id: string) => outputs[id]?.reasoning) ?? ''
      if (source) reply.reasoning = outputs[source].reasoning
    }
    reply.charts = Array.isArray(data.charts) ? data.charts : []
    reply.logId = data.logId ?? 0
    reply.steps = Array.isArray(data.steps) ? data.steps : reply.steps
    reply.id = data.answerId ?? 0
    reply.createdTime = Date.now()
    const failures = AgenticUtil.runFailures(data)
    if (failures.length) reply.notice = { summary: failures[0], detail: failures.slice(1).join('\n') }
    chatId.value = data.chatId ?? chatId.value
    syncRouteChat(chatId.value)
    loadChats()
    // 落库标识：提问补 id、回复补父消息，分支尾指向本轮回复
    if (question) question.id = data.questionId ?? question.id ?? 0
    reply.parentId = data.questionId ? data.questionId : reply.parentId
    branch.commit(reply, data.leafId)
  }).catch((error: any) => {
    question.notice = {
      summary: ApiUtil.message(error) || '发送失败',
      detail: '',
    }
  }).finally(() => {
    sending.value = false
    nextTick(() => scrollBottom())
  })
}

/** 本轮上下文：流式回调在 send 前登记，回调里只认这一份（避免回调绑死在某一轮消息上） */
const turn: { reply: any; question: any } = { reply: null, question: null }

/**
 * 流式对话：事件分发、运行状态复位与「一个事件都没收到」时的一次性兜底统一收在组合式函数里，
 * 页面只保留「收到增量 / 进度怎么画」与「结束后怎么收尾」这些业务语义。
 */
const stream = useAgenticStream({
  ...streams.agenticInvoke,
  headers: { 'X-Auth-Token': user.info.token },
  fallback: 'invoke',
  invoke: (payload: any) => fallbackInvoke(payload, turn.reply, turn.question),
  onDelta: (chunk: any) => {
    const reply: any = turn.reply
    if (!reply) return
    // 新一轮模型调用：思考内容重置（中间轮次的推理不留在最终「思考过程」里）
    if (chunk.reasoningReset) reply.reasoning = ''
    if (chunk.replace) {
      // 最终回复：整段替换（模板拼装等非模型输出的场景）
      reply.content = String(chunk.content ?? '')
    } else {
      reply.content = String(reply.content ?? '') + String(chunk.content ?? '')
      if (chunk.reasoning) reply.reasoning = String(reply.reasoning ?? '') + String(chunk.reasoning)
    }
    nextTick(() => scrollBottom())
  },
  // 节点进度：running 表示开始执行，success / failed 表示结束（循环内节点会重复执行）
  onStep: (step: any) => {
    const reply: any = turn.reply
    if (!reply) return
    AgenticUtil.markStep(reply.progress, step)
    nextTick(() => scrollBottom())
  },
  // ReAct 轮次：模型推理开始、工具方法开始调用与返回都实时补进执行过程
  onRound: (round: any) => {
    const reply: any = turn.reply
    if (!reply) return
    AgenticUtil.markRound(reply.rounds, round)
    nextTick(() => scrollBottom())
  },
  /**
   * 异常：业务失败（授权被撤销、缺少必填参数）直接写在气泡里；
   * 运行中途断开则提示「服务端仍在跑」，两种情况都不在本页重跑（重跑会重复消耗工具与模型额度）
   */
  onError: ({ message, code, streamed }: any) => {
    const reply: any = turn.reply
    if (!reply) return
    reply.streaming = false
    if (!streamed) {
      reply.notice = { summary: message || '运行失败', detail: code ? `状态码：${code}` : '' }
      return
    }
    reply.notice = {
      summary: message || '连接已中断',
      detail: '本轮在服务端会继续执行并记录运行日志，稍后刷新会话即可看到结果',
    }
    reply.createdTime = Date.now()
    loadChats()
  },
  onDone: (data: any) => {
    const reply: any = turn.reply
    if (!reply) return
    reply.streaming = false
    reply.content = data.answer || reply.content || ''
    // 思考过程：流式没有捕获到时从节点输出里补（与调试面板同一取值口径）
    if (!reply.reasoning) {
      const outputs: any = data?.outputs ?? {}
      const source: string = Object.keys(outputs).find((id: string) => outputs[id]?.reasoning) ?? ''
      if (source) reply.reasoning = outputs[source].reasoning
    }
    // 图表随回复一起展示（与调试面板一致）
    reply.charts = Array.isArray(data.charts) ? data.charts : []
    reply.logId = data.logId ?? 0
    // 本轮完整步骤随运行结果返回：完成后直接展示执行过程，不必再请求一次运行日志
    reply.steps = Array.isArray(data.steps) ? data.steps : reply.steps
    // 落库后的消息标识：反馈按钮需要它
    reply.id = data.answerId ?? 0
    reply.createdTime = Date.now()
    // 节点/工具调用异常挂在回复上（与调试面板一致），请求级异常才挂提问
    const failures = AgenticUtil.runFailures(data)
    if (failures.length) reply.notice = { summary: failures[0], detail: failures.slice(1).join('\n') }
    chatId.value = data.chatId ?? chatId.value
    // 首轮回复落库后会话才真正创建：把会话标识补进路由，刷新后仍在同一会话里
    syncRouteChat(chatId.value)
    loadChats()
    // 落库标识与分支位置：提问补 id、回复补父消息，分支尾指向本轮回复
    if (turn.question) turn.question.id = data.questionId ?? turn.question.id ?? 0
    reply.parentId = data.questionId ? data.questionId : reply.parentId
    branch.commit(reply, data.leafId)
  },
  onClose: () => {
    if (turn.reply) turn.reply.streaming = false
    sending.value = false
  },
})

/**
 * 输入框回车：Enter 直接发送，Shift / Ctrl / Cmd + Enter 换行（与调试页一致）。
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
  handleSend()
}

/** 在光标处插入换行：Ctrl / Cmd + Enter 没有浏览器默认换行，需要手动写回输入内容 */
const insertComposerNewline = (event: KeyboardEvent) => {
  const element = event.target as HTMLTextAreaElement | null
  if (!element || 'TEXTAREA' !== String(element.tagName ?? '').toUpperCase()) return
  const start = null == element.selectionStart ? element.value.length : element.selectionStart
  const end = null == element.selectionEnd ? start : element.selectionEnd
  message.value = `${element.value.slice(0, start)}\n${element.value.slice(end)}`
  nextTick(() => {
    element.selectionStart = start + 1
    element.selectionEnd = start + 1
    // 触发 input 事件，让 el-input 的 autosize 按新内容重算高度
    element.dispatchEvent(new Event('input', { bubbles: true }))
  })
}

/**
 * 执行过程：本轮刚跑完时步骤随运行结果一起返回，历史会话再按消息上的 logId 懒加载运行日志
 */
const stepCache = ref<Record<string, any[]>>({})
const stepLoading = ref<Record<string, boolean>>({})
const stepsOf = (item: any) => stepCache.value[String(item?.logId ?? '')] ?? item?.steps ?? []
const loadSteps = (item: any) => {
  const logId = String(item?.logId ?? '')
  // 本轮的步骤已随运行结果返回，只有历史会话需要按 logId 拉取
  if (!logId || (item?.steps ?? []).length || stepCache.value[logId] || stepLoading.value[logId]) return
  stepLoading.value[logId] = true
  AgenticApi.logInfo(Number(logId), { warning: false }).then((result: any) => {
    stepCache.value[logId] = ApiUtil.data(result)?.steps ?? []
  }).catch(() => {}).finally(() => {
    stepLoading.value[logId] = false
  })
}

/** 可用流程检索：按名称、描述、标签匹配 */
const agenticKeyword = ref('')
const filteredAgents = computed<any[]>(() => {
  const word = String(agenticKeyword.value ?? '').trim().toLowerCase()
  if (!word) return agents.value
  return agents.value.filter((item: any) => [item.name, item.description, (item.tags ?? []).join(' ')]
    .some((text: any) => String(text ?? '').toLowerCase().indexOf(word) >= 0))
})
/** 选中流程：开始一轮新会话（左侧历史列表保留） */
const handleAgenticSelect = (item: any) => {
  agenticId.value = item.id
  handleNew()
}
/** 切换流程：回到流程选择区 */
const handleChangeAgentic = () => {
  agenticId.value = ''
  handleNew()
}

/**
 * 开始节点的输入清单：清单取自开始节点配置（与调试运行的「参数配置」同一份口径）。
 * 固定输入 query / files 由底部输入区承载，只在开始节点启用时展示；
 * 自定义参数进新建会话的参数表单（startParams），固定输入关闭后输入区同样收起。
 */
const startParams = ref<any[]>([])
const paramValues = ref<Record<string, any>>({})
const paramCache = ref<Record<string, any>>({})
/** 固定输入：为空表示开始节点关闭了该项，底部输入区不展示对应入口 */
const queryInput = ref<any>(null)
const filesInput = ref<any>(null)

/** 参数默认值：布尔取假值，数值留空，其余按文本（与调试运行一致） */
const paramDefault = (type: string) => {
  if ('Boolean' === type) return false
  if (['Number', 'Integer'].indexOf(type) !== -1) return undefined
  return ''
}

/** 必填参数是否为空：布尔取假值算已填，其余按文本判空（与后端 invoke 的校验口径一致） */
const paramBlank = (item: any) => {
  const value = paramValues.value[item.name]
  if (null === value || undefined === value) return true
  if ('boolean' === typeof value) return false
  return '' === String(value).trim()
}

const missingParams = computed<any[]>(() => startParams.value.filter((item: any) => true === item.required && paramBlank(item)))
const missingText = computed(() => missingParams.value.map((item: any) => item.label || item.name).join('、'))

/** 自定义参数取值：随每轮入参一起提交（同一会话的后续轮次沿用新建会话时填写的值） */
const paramInputs = () => {
  const inputs: any = {}
  startParams.value.forEach((item: any) => {
    inputs[item.name] = paramValues.value[item.name]
  })
  return inputs
}

/** 开始节点的输入清单：拆出固定输入（query / files）与自定义参数 */
const startConfigOf = (start: any) => {
  const inputs = AgenticUtil.startInputs(start)
  return {
    query: inputs.find((item: any) => 'query' === item.name) ?? null,
    files: inputs.find((item: any) => 'Array<File>' === item.type) ?? null,
    params: inputs.filter((item: any) => 'query' !== item.name && 'Array<File>' !== item.type),
  }
}

/**
 * 把开始节点配置搬到当前会话：固定输入决定底部输入区展示，其余作为新建会话的参数表单。
 * 配置缺失（未选中流程、流程详情接口异常等）时按空开始节点的默认口径（固定输入都启用）处理，
 * 不把输入区整块收起。
 */
const applyStartConfig = (config: any) => {
  const item = config ?? startConfigOf({})
  queryInput.value = item.query ?? null
  filesInput.value = item.files ?? null
  startParams.value = item.params ?? []
  // 关闭文件列表后：已选附件不再回显，也不会随入参提交
  if (!filesInput.value) runFiles.value = []
  // 表单取值：切换流程后按新参数重建，同名参数沿用已填写的值
  const values: Record<string, any> = {}
  startParams.value.forEach((param: any) => {
    values[param.name] = undefined === paramValues.value[param.name] ? paramDefault(param.type) : paramValues.value[param.name]
  })
  paramValues.value = values
}

/** 应用开始节点配置：解析该流程的输入清单并缓存 */
const applyStartParams = (start: any) => {
  const id = String(agenticId.value ?? '')
  paramCache.value[id] = startConfigOf(start)
  applyStartConfig(paramCache.value[id])
}

/** 读取所选流程的参数清单：已发布配置优先，按流程缓存，切换流程时才重新拉取 */
const loadStartParams = () => {
  const id = String(agenticId.value ?? '')
  if (!id) {
    applyStartConfig(null)
    return
  }
  const cached = paramCache.value[id]
  if (cached) {
    applyStartConfig(cached)
    return
  }
  /**
   * 参数清单优先取「已发布」的开始节点配置（authorized 接口随流程返回的 start），
   * 保证与外部调用实际执行的口径一致；老接口没返回时再退回流程详情兜底。
   */
  const published: any = agents.value.find((item: any) => String(item.id) === id)?.start
  if (published) {
    applyStartParams(published)
    return
  }
  AgenticApi.info(id, { warning: false }).then((result: any) => {
    const content: any = ApiUtil.data(result)?.content ?? {}
    const cells: any[] = Array.isArray(content?.cells) ? content.cells : []
    const start: any = cells.find((cell: any) => 'Start' === cell?.data?.type)?.data ?? {}
    applyStartParams(start)
  }).catch(() => {
    applyStartConfig(null)
  })
}

// 选中流程后同步开始节点的参数清单
watch(agenticId, () => loadStartParams())

onMounted(() => {
  loadAgents()
  loadChats()
  // 刷新或重新打开页面：按路由里的对话标识恢复历史会话
  const id = routeChatId()
  if (id) handleOpen({ id })
})

// 浏览器前进/后退时跟随路由切换会话（选中会话时我们自己写路由，值相同不会重复加载）
watch(() => route.query.chatId, (value) => {
  const id = Number(value ?? 0) || 0
  if (id === (Number(chatId.value ?? 0) || 0)) return
  if (id) handleOpen({ id })
  else handleNew()
})

// 离开页面时清掉筛选防抖（运行秒表由执行过程组件自己维护）
onBeforeUnmount(() => {
  window.clearTimeout(keywordTimer)
})
</script>

<template>
  <el-splitter class="dialog-page">
    <!-- 左侧：对话历史（本人会话，按标题筛选） -->
    <el-splitter-panel class="dialog-aside" size="260px" min="200px" max="460px">
      <!-- 新建会话：整行按钮，主操作置顶 -->
      <el-button class="aside-new" type="primary" plain :icon="Plus" @click="handleNew">新建对话</el-button>
      <div class="aside-filter">
        <el-input
          v-model="keyword"
          clearable
          :prefix-icon="Search"
          placeholder="搜索会话标题" />
      </div>
      <div class="aside-list" v-loading="chatsLoading">
        <div
          class="chat-item"
          :class="{ 'is-active': chatId === item.id }"
          :key="item.id"
          v-for="item in chats"
          @click="handleOpen(item)">
          <div class="chat-item-main">
            <span class="chat-item-title">{{ item.title || '新会话' }}</span>
            <el-button
              class="chat-item-delete"
              link
              :icon="Delete"
              title="删除会话"
              @click.stop="handleDeleteChat(item)" />
          </div>
          <!-- 最近对话时间：用时间图标代替文字前缀 -->
          <div class="chat-item-time">
            <el-icon><Clock /></el-icon>
            <span>{{ DateUtil.format(item.updatedTime) }}</span>
          </div>
        </div>
        <el-empty
          class="aside-empty"
          :description="keyword ? '没有匹配的会话' : '还没有对话，选择右侧流程开始'"
          :image-size="60"
          v-if="!chats.length" />
      </div>
    </el-splitter-panel>
    <!-- 右侧：未选流程时居中选流程（支持检索），选中后进入对话 -->
    <el-splitter-panel class="dialog-main">
      <!-- v-loading 挂在真实元素上：el-splitter-panel 的根是 fragment，指令挂组件上不生效 -->
      <div class="dialog-main-body" v-loading="infoLoading">
        <header class="dialog-head">
          <template v-if="agenticId">
            <el-avatar class="head-avatar" :icon="MagicStick" />
            <span class="dialog-title">{{ agentic.name }}</span>
            <el-tag class="head-tag" size="small" effect="plain">{{ agentic.modeText || agentic.mode }}</el-tag>
            <el-button class="dialog-switch" link type="primary" :icon="Switch" @click="handleChangeAgentic">切换流程</el-button>
          </template>
          <template v-else>
            <span class="dialog-title">流程对话</span>
            <span class="dialog-sub">选择一个已发布的流程开始对话</span>
          </template>
        </header>
        <!-- 未选流程：居中展示可用流程，带检索 -->
        <div class="dialog-picker" v-if="!agenticId">
          <div class="picker-inner">
            <div class="picker-head">
              <div class="picker-head-main">
                <span class="picker-title">选择要对话的流程</span>
                <span class="picker-sub">按名称、描述或标签检索已授权给你的流程</span>
              </div>
              <el-input
                class="picker-search"
                v-model="agenticKeyword"
                clearable
                :prefix-icon="Search"
                placeholder="搜索流程" />
            </div>
            <div class="picker-list">
              <div class="picker-item" :key="item.id" v-for="item in filteredAgents" @click="handleAgenticSelect(item)">
                <div class="picker-item-avatar">{{ String(item.name || '流').slice(0, 1) }}</div>
                <div class="picker-item-main">
                  <div class="picker-item-head">
                    <span class="picker-item-name">{{ item.name }}</span>
                    <el-tag size="small" effect="plain">{{ item.modeText || item.mode }}</el-tag>
                  </div>
                  <div class="picker-item-desc">{{ item.description || '暂无描述' }}</div>
                </div>
              </div>
              <el-empty class="picker-empty" description="没有可用的流程" :image-size="80" v-if="!filteredAgents.length" />
            </div>
          </div>
        </div>
      <div class="dialog-chat" v-else>
        <div class="dialog-body" ref="chatRef" @scroll="handleScroll">
        <ChatMessage
          class="message"
          :key="index"
          v-for="(item, index) in messages"
          :item="item"
          :streaming="item.streaming"
          :disabled="feeding === item.id"
          :branch="branch.branchOf(item)"
          :editable="'user' === item.role && !!item.id && !sending"
          :regenerable="'assistant' === item.role && !!item.id && !sending"
          @submit="(payload: any) => handleFeedback(item, payload)"
          @switch="(step: number) => handleSwitch(item, step)"
          @edit="(content: string) => handleEdit(item, content)"
          @regenerate="handleRegenerate(item)">
          <!-- 执行过程（置顶）：节点与节点内部的工具调用全部用时间线展示，运行中实时刷新 -->
          <template #steps>
            <AgenticTimeline
              v-if="'assistant' === item.role && (item.logId || item.streaming)"
              :steps="stepsOf(item)"
              :progress="item.progress"
              :rounds="item.rounds"
              :streaming="item.streaming"
              :loading="stepLoading[String(item.logId)]"
              :log-id="item.logId"
              @open="loadSteps(item)" />
          </template>
        </ChatMessage>
            <!-- 空状态：关闭用户输入时没有可填内容，文案改为直接发送 -->
            <el-empty
              class="dialog-empty"
              :description="queryInput ? '还没有对话，输入内容开始吧' : '还没有对话，点击发送开始运行'"
              :image-size="80"
              v-if="!messages.length && !sending" />
          </div>
        <!-- 电梯导航：按用户消息生成右侧横条，点击定位到对应气泡（组件见 components/Chat） -->
        <ChatElevator
          :target="chatRef"
          :revision="messages.length"
          selector="[data-chat-role='user']"
          text-selector="[data-chat-text]" />
        </div>
        <footer class="dialog-composer" v-if="agenticId">
          <!-- 新建会话的自定义参数：开始节点配置了参数时直接展示表单（固定输入 query / files 在下方输入区） -->
          <div class="params-box" v-if="!chatId && startParams.length">
            <div class="params-head">
              <span class="params-title">参数配置</span>
              <span class="params-count">{{ startParams.length }} 个自定义参数</span>
              <span class="params-missing" v-if="missingParams.length">必填未填写：{{ missingText }}</span>
            </div>
            <el-form class="params-form" label-position="top">
              <el-form-item :key="item.name" v-for="item in startParams">
                <template #label>
                  <span class="params-label">{{ item.label || item.name }}</span>
                  <span class="params-alias" v-if="item.label && item.label !== item.name">{{ item.name }}</span>
                  <el-tag class="params-required" type="danger" size="small" effect="plain" v-if="item.required">必填</el-tag>
                </template>
                <el-input
                  v-if="'Paragraph' === item.type"
                  v-model="paramValues[item.name]"
                  type="textarea"
                  :rows="2"
                  :placeholder="item.description" />
                <el-input-number
                  v-else-if="['Number', 'Integer'].indexOf(item.type) !== -1"
                  v-model="paramValues[item.name]"
                  :controls="false"
                  :placeholder="item.description" />
                <el-switch v-else-if="'Boolean' === item.type" v-model="paramValues[item.name]" />
                <el-input v-else v-model="paramValues[item.name]" :placeholder="item.description" />
              </el-form-item>
            </el-form>
          </div>
          <!-- 与调试面板一致：一个圆角输入框，内部上为文本区、下为按钮行；
               开始节点关闭「用户输入」时不展示文本区，关闭「文件列表」时不展示上传入口 -->
          <div class="composer-box">
            <!-- 第 1 行：附件回显（没有附件时整行不占位） -->
            <div class="composer-files" v-if="runFiles.length">
              <el-tag
                class="composer-file"
                size="small"
                type="info"
                effect="plain"
                v-for="(file, fileIndex) in runFiles"
                :key="file.id ?? fileIndex"
                @close="runFiles.splice(fileIndex, 1)"
                closable>{{ file.name || file.id }}</el-tag>
            </div>
            <el-input
              v-if="queryInput"
              class="composer-input"
              v-model="message"
              type="textarea"
              :autosize="{ minRows: 1, maxRows: 8 }"
              resize="none"
              placeholder="请输入内容（Enter 发送，Shift+Enter 换行）"
              @keydown.enter="handleComposerSend" />
            <div class="composer-tools">
              <div class="composer-tools-main">
                <el-upload v-if="filesInput" :show-file-list="false" :before-upload="handleUpload">
                  <el-tooltip content="上传文件" placement="top">
                    <el-button class="composer-attach" link :icon="Paperclip" :loading="uploading" />
                  </el-tooltip>
                </el-upload>
                <span class="composer-tips">{{ queryInput ? 'Enter 发送，Shift + Enter 换行' : '开始节点未启用用户输入，可直接发送' }}</span>
              </div>
              <div class="composer-tools-end">
                <el-button
                  class="composer-send"
                  circle
                  type="primary"
                  :icon="Promotion"
                  title="发送"
                  :loading="sending"
                  :disabled="!canSend"
                  @click="handleSend" />
              </div>
            </div>
          </div>
        </footer>
      </div>
    </el-splitter-panel>
  </el-splitter>
</template>

<style lang="scss" scoped>
/**
 * 对话列几何：消息行 = 头像区 + 内容列 + 头像区，气泡与输入框同宽同线。
 * 尺寸一律用固定像素，不随容器百分比收缩；窗口不够宽时由内容区横向滚动。
 */
$dialog-avatar: 26px;
$dialog-gap: 10px;
$dialog-inset: $dialog-avatar + $dialog-gap;       /* 36px：一侧头像区宽度 */
$dialog-min: 600px;                                /* 内容列最小宽度 */
$dialog-max: 788px;                                /* 内容列最大宽度：气泡与输入框共用 */
$dialog-row-max: $dialog-max + $dialog-inset * 2;  /* 860px：消息行总宽 */

/* 内容列：固定像素上下限 + 居中；消息行与输入框共用同一套宽度保证竖线一致 */
@mixin dialog-column($min, $max) {
  width: 100%;
  min-width: $min;
  max-width: $max;
  margin-left: auto;
  margin-right: auto;
}
.dialog-page {
  height: 100%;
  min-height: 0;
  background: var(--el-bg-color);
  /**
   * el-splitter-panel 的根是 fragment（面板 + 分隔条），scoped 的类选择器命中不到面板本身，
   * 面板级样式统一用 :deep 定位；面板内部元素仍按普通 scoped 选择器生效。
   */
  /* 面板内容撑满并允许内部滚动：消息区自己滚，输入框固定在底部 */
  :deep(.el-splitter-panel) {
    display: flex;
    flex-direction: column;
    min-height: 0;
    overflow: hidden;
  }
  /* 左侧：浅灰底与右侧白色对话区形成层次，四周留出内边距（分隔线由 splitter 自带） */
  :deep(.dialog-aside) {
    min-width: 0;
    padding: 16px;
    background: var(--fs-layout-background-color);
  }
  :deep(.dialog-main) {
    min-width: 0;
    background: var(--el-bg-color);
  }
}
.dialog-aside {
  /* 新建会话：整行按钮，主操作置顶 */
  .aside-new {
    flex: none;
    width: 100%;
    border-radius: 6px;
  }
  .aside-filter {
    flex: none;
    width: 100%;
    margin-top: 8px;
    .el-input {
      width: 100%;
    }
  }
  .aside-list {
    flex: 1;
    min-height: 0;
    width: 100%;
    margin-top: 8px;
    overflow-x: hidden;
    overflow-y: auto;
    .chat-item {
      padding: 8px 10px;
      border-radius: 8px;
      border: solid 1px transparent;
      cursor: pointer;
      transition: background-color 0.2s, border-color 0.2s, box-shadow 0.2s;
      & + .chat-item {
        margin-top: 6px;
      }
      &:hover {
        background: var(--el-bg-color);
      }
      /* 选中项在浅灰底上浮起为白卡片 + 主色描边 */
      &.is-active {
        background: var(--el-bg-color);
        border-color: var(--el-color-primary-light-7);
        box-shadow: 0 1px 2px rgb(0 0 0 / 4%);
      }
      /* 标题与删除按钮两端对齐：按钮悬停时才显现，避免列表显得杂乱 */
      .chat-item-main {
        @include flex-between();
        gap: 6px;
        .chat-item-delete {
          flex: none;
          padding: 0;
          opacity: 0;
          color: var(--el-text-color-placeholder);
          transition: opacity 0.2s;
          &:hover {
            color: var(--el-color-danger);
          }
        }
      }
      &:hover .chat-item-delete,
      &.is-active .chat-item-delete {
        opacity: 1;
      }
      .chat-item-title {
        flex: 1;
        min-width: 0;
        font-size: 13px;
        color: var(--el-text-color-regular);
        /* 标题单行展示，超出省略：列表更整齐，长标题不撑高条目 */
        @include text-wrap();
      }
      /* 当前会话的标题用主色点出，与列表项状态呼应 */
      &.is-active .chat-item-title {
        color: var(--el-color-primary);
        font-weight: 500;
      }
      .chat-item-time {
        @include flex-start();
        gap: 4px;
        margin-top: 4px;
        font-size: 12px;
        color: var(--el-text-color-placeholder);
      }
    }
    /* 空状态与面板留白一致 */
    .aside-empty {
      padding: 24px 0;
      :deep(.el-empty__description p) {
        font-size: 12px;
        line-height: 1.7;
        color: var(--el-text-color-placeholder);
      }
    }
  }
}
.dialog-main {
  /* 面板内容包一层真实元素：v-loading 需要挂在元素上，内部仍是「顶栏 + 内容 + 输入区」的纵向排列 */
  .dialog-main-body {
    flex: 1;
    min-height: 0;
    width: 100%;
    display: flex;
    flex-direction: column;
    align-items: stretch;
  }
  /* 顶栏：流程标识 + 名称 + 模式 + 切换入口 */
  .dialog-head {
    flex: none;
    @include flex-start();
    gap: 8px;
    padding: 10px 16px;
    border-bottom: solid 1px var(--el-border-color-lighter);
    .head-avatar {
      flex: none;
      width: 26px;
      height: 26px;
      font-size: 14px;
      color: var(--el-color-primary);
      background: var(--el-color-primary-light-9);
    }
    .dialog-title {
      min-width: 0;
      font-size: 13px;
      font-weight: 500;
      color: var(--el-text-color-primary);
      @include text-wrap();
    }
    .dialog-sub {
      min-width: 0;
      font-size: 12px;
      color: var(--el-text-color-placeholder);
      @include text-wrap();
    }
    .head-tag {
      flex: none;
    }
    /* 切换流程：靠右，不抢标题宽度 */
    .dialog-switch {
      flex: none;
      margin-left: auto;
    }
  }
  /* 未选流程：居中展示可用流程并支持检索 */
  .dialog-picker {
    flex: 1;
    min-height: 0;
    overflow-x: auto;
    overflow-y: auto;
    padding: 20px 16px;
    .picker-inner {
      @include dialog-column($dialog-min, $dialog-row-max);
      .picker-head {
        @include flex-between();
        flex-wrap: wrap;
        gap: 8px 12px;
        margin-bottom: 12px;
        .picker-head-main {
          min-width: 0;
          @include flex-column();
          .picker-title {
            font-size: 13px;
            font-weight: 500;
            color: var(--el-text-color-primary);
          }
          .picker-sub {
            margin-top: 2px;
            font-size: 12px;
            line-height: 1.7;
            color: var(--el-text-color-placeholder);
          }
        }
        .picker-search {
          width: 240px;
          max-width: 100%;
        }
      }
      /* 流程卡片：宽屏一行两张，窄屏自然回落为单列 */
      .picker-list {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
        gap: 10px;
        .picker-item {
          display: flex;
          align-items: flex-start;
          gap: 10px;
          padding: 12px;
          border: solid 1px var(--el-border-color-lighter);
          border-radius: 8px;
          background: var(--el-bg-color);
          cursor: pointer;
          transition: border-color 0.2s, background-color 0.2s, box-shadow 0.2s;
          &:hover {
            border-color: var(--el-color-primary-light-5);
            background: var(--el-color-primary-light-9);
            box-shadow: 0 2px 8px rgb(0 0 0 / 6%);
          }
          /* 流程首字小色块：便于在列表中快速区分 */
          .picker-item-avatar {
            flex: none;
            @include flex-center();
            width: 28px;
            height: 28px;
            border-radius: 6px;
            font-size: 13px;
            color: var(--el-color-primary);
            background: var(--el-color-primary-light-9);
          }
          &:hover .picker-item-avatar {
            background: var(--el-color-primary-light-8);
          }
          .picker-item-main {
            flex: 1;
            min-width: 0;
          }
          .picker-item-head {
            @include flex-start();
            gap: 8px;
            .picker-item-name {
              min-width: 0;
              font-size: 13px;
              font-weight: 500;
              color: var(--el-text-color-primary);
              @include text-wrap();
            }
            .el-tag {
              flex: none;
            }
          }
          .picker-item-desc {
            margin-top: 4px;
            font-size: 12px;
            line-height: 1.7;
            color: var(--el-text-color-secondary);
            word-break: break-word;
            @include line-wrap(2);
          }
        }
        /* 空状态占满整行，不占单个卡片位 */
        .picker-empty {
          grid-column: 1 / -1;
          padding: 24px 0;
        }
      }
    }
  }
  /* 消息区外层：给右侧电梯导航提供定位基准（导航不随消息一起滚动） */
  .dialog-chat {
    position: relative;
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;
  }
  .dialog-body {
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;
    padding: 16px 40px 8px;
    overflow-x: auto;
    overflow-y: auto;
    /* 两侧都预留滚动条宽度：有/无滚动条时消息列中心都不偏移 */
    scrollbar-gutter: stable both-edges;
    /* 消息列限宽居中，与其它对话页观感一致 */
    /* 消息行：结构在 components/Chat/ChatMessage.vue，这里只保留对话页自己的列宽与观感 */
    .message {
      @include dialog-column($dialog-min + $dialog-inset * 2, $dialog-row-max);
      flex: none;
      gap: $dialog-gap;
      & + .message {
        margin-top: 16px;
      }
      /* 头像随内容列排列，不再绝对定位溢出到列外 */
      :deep(.chat-avatar) {
        margin-top: 2px;
        width: $dialog-avatar;
        height: $dialog-avatar;
        font-size: 13px;
      }
      /**
       * 气泡宽度＝输入框宽度（max-width 兜住，不越过两侧头像/异常图标那条竖线）；
       * 不再给气泡留整条头像区的外边距，异常图标才会紧贴气泡（留 10px 的行间距）
       */
      :deep(.chat-bubble) {
        flex: 1 1 auto;
        max-width: $dialog-max;
        padding: 8px 12px;
        border-radius: 10px;
        border: none;
        background: var(--el-fill-color-light);
      }
      &.is-user :deep(.chat-bubble) {
        flex: 0 1 auto;
        background: var(--el-color-primary-light-9);
      }
      /* 折叠面板头：默认 48px 对气泡内的过程信息太高，压到 22px（折叠头高度与行高都跟随该变量） */
      :deep(.chat-reasoning.el-collapse) {
        --el-collapse-header-height: 22px;
      }
      /* 回复工具条：仅靠间距与正文分隔，时间靠右 */
      :deep(.chat-toolbar) {
        margin-top: 8px;
      }
    }
    /* 空状态：在消息区中部居中 */
    .dialog-empty {
      margin: auto 0;
      :deep(.el-empty__description p) {
        font-size: 12px;
        color: var(--el-text-color-placeholder);
      }
    }
  }
  .dialog-composer {
    flex: none;
    width: 100%;
    /* 左右内边距＝消息区内边距＋头像区：输入框与气泡同一条竖线 */
    padding: 12px ($dialog-inset + 40px) 16px;
    background: var(--el-bg-color);
    /* 新建会话参数表单：与输入框同一列，字段顺序与开始节点配置一致 */
    .params-box {
      @include dialog-column($dialog-min, $dialog-max);
      margin-bottom: 8px;
      padding: 8px 12px;
      border-radius: 10px;
      border: solid 1px var(--el-border-color-lighter);
      background: var(--el-bg-color);
      .params-head {
        @include flex-start();
        flex-wrap: wrap;
        gap: 8px;
        font-size: 12px;
        .params-title {
          color: var(--el-text-color-primary);
        }
        .params-count {
          color: var(--el-text-color-placeholder);
        }
        /* 必填未填写：提交前就能看到，不用发出去才发现 */
        .params-missing {
          color: var(--el-color-danger);
        }
      }
      .params-form {
        margin-top: 6px;
        :deep(.el-form-item) {
          margin-bottom: 8px;
        }
        :deep(.el-form-item:last-child) {
          margin-bottom: 0;
        }
        :deep(.el-form-item__label) {
          @include flex-start();
          gap: 6px;
          height: auto;
          padding-bottom: 2px;
          font-size: 12px;
          line-height: 1.6;
        }
        .params-label {
          color: var(--el-text-color-primary);
        }
        .params-alias {
          color: var(--el-text-color-placeholder);
        }
        .params-required {
          flex: none;
        }
        :deep(.el-input-number) {
          width: 100%;
        }
      }
    }
    /* 与调试面板一致：圆角输入框，聚焦时描边高亮并带一圈浅色光晕 */
    .composer-box {
      @include dialog-column($dialog-min, $dialog-max);
      padding: 8px 12px;
      border-radius: 10px;
      border: solid 1px var(--el-border-color);
      background: var(--el-bg-color);
      transition: border-color 0.2s, box-shadow 0.2s;
      &:hover {
        border-color: var(--el-border-color-darker);
      }
      &:focus-within {
        border-color: var(--el-color-primary);
        box-shadow: 0 0 0 3px var(--el-color-primary-light-9);
      }
    }
    /* 文本区域占满整宽、无自身边框，高度只随内容增减 */
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
    /* 底部一行：左侧附件与按键提示，右侧发送按钮 */
    .composer-tools {
      @include flex-between();
      gap: 8px;
      margin-top: 4px;
      /* 文本区不展示时（开始节点关闭用户输入）工具行成为首行：与盒子内边距对齐，不再多留一段间距 */
      &:first-child {
        margin-top: 0;
      }
      .composer-tools-main {
        @include flex-start();
        min-width: 0;
        gap: 8px;
        .composer-attach {
          padding: 0;
          color: var(--el-text-color-placeholder);
          &:hover {
            color: var(--el-color-primary);
          }
        }
      }
      .composer-tips {
        min-width: 0;
        font-size: 12px;
        color: var(--el-text-color-placeholder);
        @include text-wrap();
      }
      /* 右侧操作区：与调试面板同结构，发送按钮保持圆形 */
      .composer-tools-end {
        flex: none;
        @include flex-start();
        gap: 8px;
        .composer-send {
          flex: none;
        }
      }
    }
    /* 附件回显：小标签一行，超出折行 */
    .composer-files {
      margin-bottom: 6px;
      @include flex-wrap();
      gap: 6px;
      .composer-file {
        max-width: 100%;
      }
    }
  }
}
</style>
