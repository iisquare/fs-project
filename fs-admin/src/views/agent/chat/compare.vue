<script setup lang="ts">
/**
 * 模型对比 - 多模型并排调试。
 *
 * 左侧是配置：先选模型，每个模型一套独立参数（温度 / 最大输出 / 思考模式 / 思考强度），
 * 再设全局的系统提示词、用户输入与流式、思考开关；右侧按行并排展示各模型的输出。
 *
 * 关于流式与结果：
 * - 一个模型一条流式连接：参数逐模型独立，合并成一条流就分不开了；
 * - 结果按字段分开存：思考与正文各归各的，不然没法单独折叠、也没法比思考长度；
 * - 每次提交自动留档：中断与失败同样入档，否则调了半天什么都没留下。
 *   留档由服务端写：发送前先建档拿标识，各模型的输出随各自的流写库，
 *   所以切页面、刷新、断网都只中断「看」，不会让这一轮从历史里消失。
 *
 * 三条数据线，彼此不硬关联：
 * - 工作区：这份配置的存档，一个用户多份、各自命名，显式「保存 / 另存为 / 打开」；
 *   默认只有本人可见，共享后所有人可见，但可见不等于可改——他人的共享只能打开查看，
 *   要留成自己的走「另存为」。
 * - 对比记录：一次提交一条，自带 models / global / results 快照，独立于工作区；
 *   恢复时把当时的模型配置与结果一并载入编辑器。记录与明细都在服务端，
 *   页面做的是「先建档拿标识 → 带着标识发流 → 收尾时回读明细把 id 与反馈补到卡片上」。
 * - 编辑器草稿：内存里这份配置，工作区与记录只是往里读、往外写。
 *
 * 这些都只服务本页，state 就留在组件里，不进 store。
 */
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowDown,
  ArrowRight,
  DocumentCopy,
  EditPen,
  FolderOpened,
  Plus,
  Promotion,
  Refresh,
  VideoPause,
} from '@element-plus/icons-vue'
import CompareApi from '@/api/agent/CompareApi'
import ApiUtil from '@/utils/ApiUtil'
import UIUtil from '@/utils/UIUtil'
import useModelCompare from '@/composables/useModelCompare'
import CompareModelPanel from '@/views/agent/components/CompareModelPanel.vue'
import CompareResultCard from '@/views/agent/components/CompareResultCard.vue'
import CompareHistory from '@/views/agent/components/CompareHistory.vue'
import CompareWorkspace from '@/views/agent/components/CompareWorkspace.vue'
import { COMPARE_THINK_EFFORTS, COMPARE_THINK_MODES } from '@/types/compare'
import type {
  CompareGlobalConfig,
  CompareModelConfig,
  CompareRun,
  CompareWorkspace as CompareWorkspaceData,
  CompareWorkspaceSummary,
} from '@/types/compare'

const { results, running, start, stop, stopOne, reset } = useModelCompare()

/* ------------------------------- 工作区 ------------------------------- */

/** 历史记录每页条数 */
const RUN_SIZE = 20
/** 另存为时的默认名称后缀 */
const COPY_SUFFIX = '（副本）'

const defaultGlobal = (): CompareGlobalConfig => ({
  systemPrompt: '',
  input: '',
  stream: true,
  think: true,
  column: 2,
  raw: false,
})

/**
 * 兜底补齐：历史数据可能是上一版结构，缺的字段用默认值填上，
 * 避免页面上读出一堆 undefined。
 */
const normalizeModel = (item: any): CompareModelConfig => ({
  key: item?.key || UIUtil.uuid('cmp-'),
  model: String(item?.model ?? ''),
  temperatureEnabled: !!item?.temperatureEnabled,
  temperature: Number(item?.temperature ?? 0),
  maxTokensEnabled: !!item?.maxTokensEnabled,
  maxTokens: Number(item?.maxTokens ?? 0),
  thinkModeEnabled: !!item?.thinkModeEnabled,
  thinkMode: String(item?.thinkMode ?? 'auto'),
  thinkEffortEnabled: !!item?.thinkEffortEnabled,
  thinkEffort: String(item?.thinkEffort ?? 'medium'),
})

const normalizeGlobal = (item: any): CompareGlobalConfig => ({
  ...defaultGlobal(),
  ...(item ?? {}),
  column: Number(item?.column) > 0 ? Number(item.column) : defaultGlobal().column,
  raw: !!item?.raw,
})

/** 编辑器草稿：页面内存里这份配置，工作区与记录都只是往里读、往外写 */
const workspace = ref<CompareWorkspaceData>({ models: [], global: defaultGlobal() })
/** 可见的工作区（我的 + 他人共享的） */
const list = ref<CompareWorkspaceSummary[]>([])
const runs = ref<CompareRun[]>([])
/** 当前绑定哪份工作区：null 表示这份配置还没保存过 */
const workspaceId = ref<any>(null)
const workspaceName = ref('')
/** 当前工作区是否已共享（只有自己的才有意义） */
const sharedFlag = ref(0)
/** 配置的来源说明：打开他人共享、或从历史恢复时填上，用于提示「保存会另存为」 */
const fromLabel = ref('')
/** 来源类型：''-自己的；'shared'-他人的共享；'run'-从历史恢复 */
const fromKind = ref('')
const dirty = ref(false)
/** 程序化改写草稿时置位，避免把载入 / 保存本身算成「用户改的」 */
let pristine = false

watch(workspace, () => {
  if (pristine) return
  dirty.value = true
}, { deep: true })

/** 手风琴：一次只展开一个模型的参数，收起时只看摘要 */
const activeModel = ref('')
const onModelExpand = (key: string, value: boolean) => {
  activeModel.value = value ? key : ''
}
/** 全局设置里的次要项默认收起：用户输入才是每次都要改的 */
const advancedVisible = ref(false)
/** 结果区每行列数：属于「怎么看」，所以放在结果栏 */
const columnOptions = [1, 2, 3, 4]


/** 程序化载入草稿：等这次改动 flush 完再清掉脏标记 */
const loadDraft = (models: any, global: any) => {
  pristine = true
  workspace.value = {
    models: Array.isArray(models) ? models.map(normalizeModel) : [],
    global: normalizeGlobal(global),
  }
  // 手风琴默认展开第一个，其余收起——一屏里只有一节参数是铺开的
  activeModel.value = workspace.value.models[0]?.key ?? ''
  nextTick(() => {
    pristine = false
    dirty.value = false
  })
}

/** 载入 / 新建 / 恢复前的确认：避免把没保存的改动默默丢掉 */
const guardDirty = (action: () => void) => {
  if (!dirty.value) {
    action()
    return
  }
  ElMessageBox.confirm('当前配置有未保存的改动，继续将丢弃这些改动。是否继续？', '未保存的改动', {
    type: 'warning',
  }).then(action).catch(() => {})
}

/* ------------------------------- 工作区 ------------------------------- */

/** 「打开」弹窗：工作区不在工作台上常驻，点按钮才出来选 */
const workspaceVisible = ref(false)

/** 名称默认按文本展示，点铅笔才变成输入框——免得一屏全是输入控件 */
const nameEditing = ref(false)
const nameRef = ref<any>(null)
const displayName = computed(() => String(workspaceName.value ?? '').trim() || '未命名工作区')
const startEditName = () => {
  nameEditing.value = true
  nextTick(() => nameRef.value?.focus())
}

/** 保存状态用一个圆点表示（在标题前），完整说明放 title 里，鼠标悬停能看到 */
const stateTitle = computed(() => {
  if ('shared' === fromKind.value) return `${fromLabel.value}，保存会另存为你自己的`
  if ('run' === fromKind.value) return `${fromLabel.value}，保存后即为新的工作区`
  if (null == workspaceId.value) return '这份配置还没保存过，点「保存」存下来'
  return dirty.value ? '当前配置有未保存的改动' : '当前配置已保存'
})
const stateSaved = computed(() => null != workspaceId.value && !dirty.value && !fromKind.value)

const loadList = () => CompareApi.workspaceList({ warning: false, error: false })
  .then((result: any) => {
    const data: any = ApiUtil.data(result)
    list.value = data?.rows ?? []
  }).catch(() => {})

/** 历史列表的检索与分页：状态只放这一处，子组件把关键词/页码抛上来即可 */
const runQuery = ref({ title: '', page: 1, pageSize: RUN_SIZE })
const runTotal = ref(0)
const runLoading = ref(false)

const loadRuns = () => {
  runLoading.value = true
  return CompareApi.runList({ ...runQuery.value }, { warning: false, error: false })
    .then((result: any) => {
      const data: any = ApiUtil.data(result)
      runs.value = data?.rows ?? []
      runTotal.value = Number(data?.total ?? 0)
    }).catch(() => {}).finally(() => {
      runLoading.value = false
    })
}

const handleRunSearch = (keyword: string) => {
  runQuery.value.title = keyword
  runQuery.value.page = 1
  loadRuns()
}

const handleRunPage = (page: number) => {
  runQuery.value.page = page
  loadRuns()
}

/** 打开工作区：自己的载进来可直接「保存」；他人的只读，保存会另存为 */
const openWorkspace = (row: any) => {
  guardDirty(() => {
    clearActiveRun() // 打开工作区即离开那条记录
    CompareApi.workspaceInfo(row.id, { warning: false, error: false }).then((result: any) => {
      const detail: any = ApiUtil.data(result)
      if (!detail) {
        ElMessage.warning('工作区已不存在')
        return
      }
      loadDraft(detail.models, detail.global)
      workspaceId.value = detail.mine ? detail.id : null
      workspaceName.value = detail.mine ? String(detail.name ?? '') : ''
      sharedFlag.value = detail.mine ? Number(detail.shared ?? 0) : 0
      fromLabel.value = detail.mine ? '' : `${detail.createdUserInfo?.name || '其他用户'} 的共享工作区`
      fromKind.value = detail.mine ? '' : 'shared'
      nameEditing.value = false
      ElMessage.success(detail.mine ? '已打开工作区' : '已载入共享工作区，保存将另存为你自己的')
    }).catch(() => {})
  })
}

/** 保存 / 另存为共用的落库动作 */
const submitSave = (id: any, name: string) => {
  CompareApi.workspaceSave({
    id,
    name,
    models: JSON.parse(JSON.stringify(workspace.value.models)),
    global: JSON.parse(JSON.stringify(workspace.value.global)),
  }, { error: false }).then((result: any) => {
    const saved: any = ApiUtil.data(result)
    if (!saved) return
    workspaceId.value = saved.id
    workspaceName.value = String(saved.name ?? name)
    sharedFlag.value = Number(saved.shared ?? 0)
    fromLabel.value = ''
    fromKind.value = ''
    pristine = true
    nextTick(() => {
      pristine = false
      dirty.value = false
    })
    loadList()
    ElMessage.success('已保存')
  }).catch(() => {})
}

/** 保存到当前工作区：名称必填——多个工作区靠它区分场景 */
const saveWorkspace = () => {
  const name = String(workspaceName.value ?? '').trim()
  if (!name) {
    ElMessage.warning('请先填写工作区名称')
    return
  }
  // 按钮不再置灰（分裂按钮没法只禁半边），没有改动就直说，别白跑一趟
  if (null != workspaceId.value && !dirty.value) {
    ElMessage.info('当前配置已保存')
    return
  }
  submitSave(workspaceId.value, name)
}

/** 分裂按钮展开后的菜单：保存之外的动作都收在这里 */
const handleHeadCommand = (command: string) => {
  if ('open' === command) workspaceVisible.value = true
  else if ('saveAs' === command) saveAsWorkspace()
  else if ('new' === command) newWorkspace()
}

/** 另存为一份新的：默认在现有名称后加「（副本）」，避免覆盖当前工作区 */
const saveAsWorkspace = () => {
  ElMessageBox.prompt('保存为一份新的工作区，不影响现有的。', '另存为', {
    inputValue: String(workspaceName.value || '未命名工作区') + COPY_SUFFIX,
    inputPlaceholder: '请输入工作区名称',
    inputValidator: (value: any) => !!String(value ?? '').trim() || '名称不能为空',
  }).then(({ value }: any) => submitSave(null, String(value).trim())).catch(() => {})
}

/** 新建：清空编辑器草稿并解绑工作区（已有的工作区不会被删） */
const newWorkspace = () => {
  guardDirty(() => {
    clearActiveRun()
    reset()
    loadDraft([], defaultGlobal())
    workspaceId.value = null
    workspaceName.value = ''
    sharedFlag.value = 0
    fromLabel.value = ''
    fromKind.value = ''
  })
}

/** 共享 / 收回共享：共享后所有人可见，但只有属主能改 */
const handleShare = (row: any) => {
  const shared = 1 !== Number(row.shared)
  CompareApi.workspaceShare([row.id], shared, { error: false }).then(() => {
    if (workspaceId.value === row.id) sharedFlag.value = shared ? 1 : 0
    loadList()
    ElMessage.success(shared ? '已共享，所有人都能看到' : '已收回共享')
  }).catch(() => {})
}

const handleRemoveWorkspace = (row: any) => {
  ElMessageBox.confirm(`将删除工作区「${row.name || '未命名工作区'}」，对比记录不受影响。是否继续？`, '删除工作区', {
    type: 'warning',
  }).then(() => {
    CompareApi.workspaceDelete([row.id], { error: false }).then(() => {
      // 被删的就是当前打开的那份：编辑器退回成「未保存的新工作区」
      if (workspaceId.value === row.id) unbind()
      loadList()
    }).catch(() => {})
  }).catch(() => {})
}

/** 解绑当前工作区：编辑器里这份配置退回成「未保存的新工作区」 */
const unbind = () => {
  workspaceId.value = null
  fromLabel.value = ''
  fromKind.value = ''
  workspaceName.value = ''
  sharedFlag.value = 0
}

/**
 * 建立本轮记录：点「发送」时先建档拿到标识，再把标识交给各模型的流。
 *
 * 明细由服务端随流写库（中断与失败同样入档），页面只留这个标识：
 * 建档失败就直说这一轮不留档——结果只在当前页面可见，免得之后去历史里白找。
 * 建档用的就是真正要发的这批模型：模型个数决定「几个到齐才算结束」，
 * 快照也与实际下发的参数一一对应（没填模型名的卡片本来就不会发起调用）。
 */
const createRun = (models: CompareModelConfig[]) => CompareApi.runCreate({
  global: JSON.parse(JSON.stringify(workspace.value.global)),
  models: JSON.parse(JSON.stringify(models)),
}, { error: false }).then((result: any) => {
  const saved: any = ApiUtil.data(result)
  if (!saved?.id) return null
  // 建档即入列表：这一轮还在跑，也能在历史里看到「进行中」，跑完再刷新一次看摘要
  runQuery.value.page = 1
  loadRuns()
  return saved.id
}).catch(() => null)

/**
 * 回读本轮记录：服务端已经写好的明细按本地 key 回填到页面的卡片上。
 *
 * 反馈按 callId 定位，卡片拿到 id 才能点；顺带把服务端那份反馈状态带回来，
 * 页面上看到的就是落库的那一份（历史恢复走的是同一条路）。
 */
const syncRun = (id: any) => {
  if (!id) return
  CompareApi.runInfo(id, { warning: false, error: false }).then((result: any) => {
    const detail: any = ApiUtil.data(result)
    const rows: any[] = Array.isArray(detail?.results) ? detail.results : []
    if (!rows.length) return
    const map: Record<string, any> = {}
    rows.forEach((item: any) => { if (item?.key) map[String(item.key)] = item })
    results.value.forEach((item: any) => {
      const saved = map[String(item.key)]
      if (!saved?.id) return
      item.id = saved.id
      item.feedbackEmotion = saved.feedbackEmotion ?? ''
      item.feedbackTag = saved.feedbackTag ?? ''
      item.feedbackContent = saved.feedbackContent ?? ''
    })
  }).catch(() => {})
}

/** 提交反馈：接口按「再次提交同一情绪即取消」处理，回来后同步本地回显 */
const handleFeedback = (item: any, payload: { emotion: string, tag: string, content: string }) => {
  if (!item?.id) {
    ElMessage.warning('本轮还没入库，稍后再试')
    return
  }
  CompareApi.feedback({
    callId: item.id,
    emotion: payload.emotion,
    tag: payload.tag,
    content: payload.content,
  }, { error: false }).then((result: any) => {
    const data: any = ApiUtil.data(result)
    const emotion = data?.emotion ?? payload.emotion
    item.feedbackEmotion = emotion
    item.feedbackTag = emotion ? payload.tag : ''
    item.feedbackContent = emotion ? payload.content : ''
    ElMessage.success(emotion ? '已记录反馈' : '已取消反馈')
  }).catch(() => {})
}

const removeRun = (run: CompareRun) => {
  CompareApi.runDelete([run.id], { warning: false, error: false }).then(() => {
    // 删掉当前页最后一条时回退一页，免得停在一张空列表上
    if (1 === runs.value.length && runQuery.value.page > 1) runQuery.value.page -= 1
  }).catch(() => {}).finally(() => {
    loadRuns()
  })
}

const clearRuns = () => {
  // 走「清空全部」接口：列表有分页，页面上拿到的只是当前页的 id
  CompareApi.runClear({ warning: false, error: false }).catch(() => {}).finally(() => {
    runQuery.value.page = 1
    loadRuns()
  })
}

/* --------------------------- 记录的打开与刷新 --------------------------- */

/**
 * 结果区此刻展示的那条记录：打开历史后才有，进行中的一轮靠它给出「等待 / 手动刷新 / 终止」的入口。
 *
 * 这里不做自动刷新：记录是服务端的存档，看进度是显式动作——
 * 页面只提示还在进行中，要不要看最新的由用户点「刷新」决定，随时可以终止。
 */
const activeRun = ref<any>(null)

/** 清掉「正在看某条记录」这层关系：发新一轮、打开工作区、清空结果区都要清 */
const clearActiveRun = () => {
  activeRun.value = null
}

/** 记录摘要：只留身份与状态，明细本身不进内存（结果区那份才是要展示的） */
const brief = (detail: any) => ({
  id: detail?.id,
  title: String(detail?.title ?? ''),
  status: Number(detail?.status ?? 0),
  active: Number(detail?.active ?? 0),
})

/** 回读一条记录：拿到服务端这一刻的明细、状态与还在跑的流数（失败返回 null） */
const pullRun = (id: any) => CompareApi.runInfo(id, { warning: false, error: false })
  .then((result: any) => ApiUtil.data(result) ?? null)
  .catch(() => null)

/** 把记录里的明细铺到卡片上：服务端那一份是唯一事实来源，按 key 与页面对齐 */
const applyRun = (detail: any) => {
  results.value = JSON.parse(JSON.stringify(detail?.results ?? []))
}

/** 进行中的提示语：还有流在跑就让人等，一条都没有了就只有「终止」能收口 */
const runningText = (detail: any) => (1 > Number(detail?.active ?? 0)
  ? '这一轮还在进行中，但服务端已没有在跑的流，点「终止」即可收口'
  : `这一轮还在进行中（服务端还有 ${Number(detail?.active ?? 0)} 条流在跑），稍后点「刷新」查看进度`)

/**
 * 打开一条历史记录：把当时的模型配置与结果一并载入编辑器。
 * 记录与工作区不硬关联，所以这里只落到草稿上、并解绑当前工作区——
 * 要留成一份工作区，得显式「保存」。这样也不会把正打开着的工作区覆盖掉。
 */
const openRun = (run: CompareRun) => {
  pullRun(run.id).then((detail: any) => {
    if (!detail) {
      ElMessage.warning('读取不到这条记录，可能已被删除')
      loadRuns()
      return
    }
    loadDraft(detail.models, detail.global)
    applyRun(detail)
    unbind()
    activeRun.value = brief(detail)
    fromLabel.value = `${detail.title || '历史记录'} 的配置`
    fromKind.value = 'run'
    if (0 !== Number(detail.status)) {
      ElMessage.success('已载入编辑器，保存后即为新的工作区')
      return
    }
    ElMessage.warning(`已载入编辑器：${runningText(detail)}`)
  })
}

/** 手动刷新正在看的这条记录：进行中的一轮以服务端那一刻的明细为准 */
const refreshRun = () => {
  const id = activeRun.value?.id
  if (!id) return
  pullRun(id).then((detail: any) => {
    if (!detail) {
      clearActiveRun()
      ElMessage.warning('读取不到这条记录，可能已被删除')
      return
    }
    applyRun(detail)
    activeRun.value = brief(detail)
    loadRuns() // 列表里的摘要也跟着更新
    if (0 !== Number(detail.status)) {
      ElMessage.success('这一轮已结束，记录已更新')
      return
    }
    ElMessage.info(runningText(detail))
  })
}

/** 点击历史记录：本地这一轮还在跑时先说清楚——打开记录会先把它停掉 */
const restoreRun = (run: CompareRun) => {
  if (!running.value) {
    openRun(run)
    return
  }
  ElMessageBox.confirm('本地这一轮还在运行，打开历史记录会先终止它（已收到的内容同样入档）。是否继续？', '本轮正在运行', {
    type: 'warning',
  }).then(() => {
    stop()
    openRun(run)
  }).catch(() => {})
}

/** 终止一条记录：服务端断掉这一轮还在跑的流，一直没回来的模型记为中断 */
const stopRun = (id: any) => {
  ElMessageBox.confirm('将终止这一轮还在进行的模型调用，未收到输出的模型记为中断。是否继续？', '终止本轮', {
    type: 'warning',
  }).then(() => {
    CompareApi.runStop(id, { error: false }).then(() => {
      // 正在看的正好是这一条：顺手回读一次，卡片与提示都切到已收口的状态（提示由它给）
      if (activeRun.value?.id === id) {
        refreshRun()
        return
      }
      loadRuns()
      ElMessage.success('已终止本轮')
    }).catch(() => {})
  }).catch(() => {})
}

/** 可调试的模型清单：取自模型网关的可达模型；接口异常时下拉为空，仍可手输模型名 */
const models = ref<any[]>([])
const thinkModes = ref<any[]>(COMPARE_THINK_MODES)
const thinkEfforts = ref<any[]>(COMPARE_THINK_EFFORTS)
/** 模型清单既可能是 `['gpt-4o']`，也可能是 `[{ name: 'gpt-4o' }]`，两种都认 */
const options = computed(() => models.value
  .map((item: any) => ('string' === typeof item ? item : item?.name))
  .filter((name: any) => !!name))

const loadModels = () => {
  CompareApi.models({ warning: false, error: false }).then((result: any) => {
    const data: any = ApiUtil.data(result)
    models.value = Array.isArray(data) ? data : (data?.rows ?? [])
  }).catch(() => {})
}

/* ------------------------------- 模型 ------------------------------- */

const pending = ref('')

/** 新加入的模型：四项参数都不下发，由服务端取默认值 */
const createModel = (name: string): CompareModelConfig => ({
  key: UIUtil.uuid('cmp-'),
  model: name,
  temperatureEnabled: false,
  temperature: 0.7,
  maxTokensEnabled: false,
  maxTokens: 2048,
  thinkModeEnabled: false,
  thinkMode: 'auto',
  thinkEffortEnabled: false,
  thinkEffort: 'medium',
})

/** 添加模型：同一个模型可以重复添加，用来对照不同参数 */
const handleAdd = (value?: any) => {
  const name = String(value ?? pending.value ?? '').trim()
  pending.value = ''
  if (!name) return
  const item = createModel(name)
  workspace.value.models.push(item)
  activeModel.value = item.key // 新加的这一个直接展开，省得还要再点一下
}

const handleRemove = (index: number) => {
  workspace.value.models.splice(index, 1)
}

/* ------------------------------- 提交 ------------------------------- */

/** 本轮运行记录标识：建档后才有；明细由服务端写，页面只拿它回读 id 与反馈 */
const runId = ref<any>(null)
/** 建档进行中：连点两次「发送」不能建出两条记录 */
const creating = ref(false)

const handleSubmit = () => {
  if (running.value) {
    stop()
    return
  }
  const targets = workspace.value.models.filter((item) => String(item.model ?? '').trim())
  if (!targets.length) {
    ElMessage.warning('请先添加至少一个模型')
    return
  }
  if (!String(workspace.value.global.input ?? '').trim()) {
    ElMessage.warning('请先填写用户输入')
    return
  }
  if (creating.value) return
  // 新一轮接管结果区：不再对应哪条历史记录
  clearActiveRun()
  // 先建档再发流：记录由服务端写，中途切走页面这一轮也不会丢
  creating.value = true
  createRun(targets).then((id: any) => {
    runId.value = id
    if (!id) ElMessage.warning('这一轮没能建档，结果只在当前页面可见')
    start(targets, workspace.value.global, id)
  }).finally(() => {
    creating.value = false
  })
}

// 本轮收尾（成功、失败、中断都算）：明细服务端已经写完，这里刷新列表并回读一次 id
watch(running, (current, previous) => {
  if (!previous || current) return
  loadRuns()
  syncRun(runId.value)
})

/** 清空结果区：清空之后展示的就不再是那条记录了 */
const handleReset = () => {
  clearActiveRun()
  reset()
}

/* ------------------------------- 历史 ------------------------------- */

const handleClearRuns = () => {
  ElMessageBox.confirm('将删除全部调试记录，是否继续？', '清空记录', { type: 'warning' }).then(() => {
    clearRuns()
  }).catch(() => {})
}

/* ------------------------------- 快捷键 ------------------------------- */

/** Ctrl / Cmd + Enter 直接发送（或停止）：调参时手不用离开键盘 */
const handleShortcut = (event: KeyboardEvent) => {
  if (!(event.ctrlKey || event.metaKey) || 'Enter' !== event.key) return
  event.preventDefault()
  handleSubmit()
}

onMounted(() => {
  // 快速恢复：有工作区就自动打开最近改动的那个，没有则留空草稿
  loadList().then(() => {
    const recent = list.value.find((item) => item.mine)
    if (recent) openWorkspace(recent)
    loadRuns()
  })
  loadModels()
  window.addEventListener('keydown', handleShortcut)
})

onBeforeUnmount(() => window.removeEventListener('keydown', handleShortcut))
</script>

<template>
  <el-splitter class="compare-page">
    <!-- 左列：配置 -->
    <!-- collapsible 用 splitter 内置的：分隔条上会出现箭头，点一下折叠 / 展开 -->
    <el-splitter-panel class="compare-config" size="380px" min="300px" max="560px" collapsible>
      <!-- 工作区身份与保存状态：名称按文本展示，点铅笔才变输入框 -->
      <div class="config-head">
        <!-- 左端：状态圆点 + 名称 + 重命名 + 共享标记，挨在一起 -->
        <div class="head-main">
          <!-- 保存状态：标题前一个小圆点，颜色区分存没存过，说明在 title 里 -->
          <span class="head-dot" :class="{ 'is-saved': stateSaved }" :title="stateTitle"></span>
          <el-input
            v-if="nameEditing"
            ref="nameRef"
            class="head-input"
            v-model="workspaceName"
            size="small"
            maxlength="128"
            placeholder="未命名工作区"
            @blur="nameEditing = false"
            @keydown.enter="nameEditing = false" />
          <template v-else>
            <span class="head-name" :title="displayName" @click="startEditName">{{ displayName }}</span>
            <el-button class="head-edit" link :icon="EditPen" title="重命名" @click="startEditName" />
          </template>
          <el-tag class="head-tag" v-if="sharedFlag" size="small" effect="plain" type="success">已共享</el-tag>
        </div>
        <!-- 分裂按钮：点主体就是「保存」（最常用），点箭头才展开打开 / 另存为 / 新建 -->
        <el-dropdown
          class="head-more"
          split-button
          type="primary"
          size="small"
          trigger="click"
          @click="saveWorkspace"
          @command="handleHeadCommand">
          保存
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="open" :icon="FolderOpened">打开工作区</el-dropdown-item>
              <el-dropdown-item command="saveAs" :icon="DocumentCopy">另存为</el-dropdown-item>
              <el-dropdown-item command="new" :icon="Plus">新建</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
      <!-- 滚动统一交给 el-scrollbar：常显滚动条，能一眼看出还有内容 -->
      <el-scrollbar class="config-body">
        <div class="config-inner">
        <div class="group">
          <div class="group-title">模型（{{ workspace.models.length }}）</div>
          <el-select
            class="group-add"
            v-model="pending"
            filterable
            clearable
            allow-create
            default-first-option
            placeholder="选择或输入模型名称"
            @change="handleAdd">
            <el-option v-for="name in options" :key="name" :value="name" :label="name" />
          </el-select>
          <div class="model-list">
            <CompareModelPanel
              v-for="(item, index) in workspace.models"
              :key="item.key"
              v-model="workspace.models[index]"
              :index="index + 1"
              :expanded="activeModel === item.key"
              :think-modes="thinkModes"
              :think-efforts="thinkEfforts"
              @update:expanded="(value: boolean) => onModelExpand(item.key, value)"
              @remove="handleRemove(index)" />
            <div class="list-hint" v-if="!workspace.models.length">从上面选一个模型开始</div>
          </div>
        </div>
        <div class="group">
          <div class="group-title">用户输入</div>
          <el-input
            type="textarea"
            v-model="workspace.global.input"
            :rows="6"
            resize="vertical"
            placeholder="要发给各模型的内容" />
          <!-- 系统提示词与输出设置不是每次都要动，收起后配置列短一大截 -->
          <div
            class="disclosure"
            role="button"
            tabindex="0"
            @click="advancedVisible = !advancedVisible"
            @keydown.enter.prevent="advancedVisible = !advancedVisible"
            @keydown.space.prevent="advancedVisible = !advancedVisible">
            <el-icon class="disclosure-caret"><component :is="advancedVisible ? ArrowDown : ArrowRight" /></el-icon>
            <span>系统提示词</span>
            <span class="disclosure-flag" v-if="workspace.global.systemPrompt">已设置提示词</span>
          </div>
          <div class="disclosure-body" v-show="advancedVisible">
            <el-input
              type="textarea"
              v-model="workspace.global.systemPrompt"
              :rows="4"
              placeholder="留空则不增加系统提示词" />
          </div>
        </div>
      </div>
      </el-scrollbar>
      <div class="config-foot">
        <el-button
          type="primary"
          :icon="running ? VideoPause : Promotion"
          :title="running ? '停止生成' : '发送（Ctrl / Cmd + Enter）'"
          :disabled="creating"
          @click="handleSubmit">
          {{ running ? '停止' : '发送' }}
        </el-button>
        <!-- 两个开关每次调参都要动，跟发送按钮同排，不用展开「系统提示词」才够得着 -->
        <div class="foot-switches">
          <el-checkbox v-model="workspace.global.stream" size="small">流式输出</el-checkbox>
          <el-checkbox v-model="workspace.global.think" size="small">启用思考</el-checkbox>
        </div>
      </div>
      <!-- 打开工作区的弹窗：挂在配置列下，实际 teleport 到 body -->
      <CompareWorkspace
        v-model="workspaceVisible"
        :list="list"
        @open="openWorkspace"
        @share="handleShare"
        @remove="handleRemoveWorkspace" />
    </el-splitter-panel>
    <!-- 中列：各模型输出并排 -->
    <el-splitter-panel class="compare-main">
      <div class="main-head">
        <span class="head-title">输出对比</span>
        <span class="head-meta" v-if="results.length">{{ results.length }} 个模型</span>
        <!-- 结果区是一条进行中的记录：不自动刷新，看进度点「刷新」，不想等就直接终止 -->
        <template v-if="activeRun && 0 === activeRun.status">
          <el-tag class="head-watch" size="small" effect="plain" type="warning">
            记录 #{{ activeRun.id }} 进行中（{{ activeRun.active }} 条流在跑）
          </el-tag>
          <el-button class="head-watch-btn" link :icon="Refresh" title="刷新这条记录" @click="refreshRun()" />
          <el-button class="head-watch-btn" link :icon="VideoPause" title="终止这条记录" @click="stopRun(activeRun.id)" />
        </template>
        <div class="head-tools">
          <!-- 原文：关掉 Markdown 渲染直接看原始文本，对照模型输出里的标记时用得上 -->
          <el-checkbox class="tool-raw" v-model="workspace.global.raw" size="small">原文</el-checkbox>
          <!-- 列数属于「怎么看」，放在结果栏比放在配置里顺手 -->
          <span class="tool-label">每行</span>
          <el-segmented v-model="workspace.global.column" :options="columnOptions" size="small" />
          <el-button link :icon="Refresh" :disabled="!results.length" @click="handleReset()">清空</el-button>
        </div>
      </div>
      <el-scrollbar class="main-body">
        <div class="main-inner">
          <div class="result-grid" :style="{ '--compare-column': workspace.global.column }">
            <CompareResultCard
              v-for="item in results"
              :key="item.key"
              :result="item"
              :raw="workspace.global.raw"
              :closable="running"
              @stop="stopOne(item.key)"
              @feedback="(payload: any) => handleFeedback(item, payload)" />
          </div>
          <el-empty
            class="main-empty"
            :description="activeRun && 0 === activeRun.status
              ? '这一轮还在进行中，暂时没有可展示的输出，可稍后点「刷新」查看'
              : '添加模型、填好输入后发送，各模型的输出会并排出现在这里'"
            :image-size="90"
            v-if="!results.length" />
        </div>
      </el-scrollbar>
    </el-splitter-panel>
    <!-- 右列：历史记录 -->
    <el-splitter-panel class="compare-history" size="280px" min="220px" max="420px" collapsible>
      <CompareHistory
        :runs="runs"
        :total="runTotal"
        :page="runQuery.page"
        :page-size="runQuery.pageSize"
        :loading="runLoading"
        @search="handleRunSearch"
        @page="handleRunPage"
        @restore="restoreRun"
        @stop="(row: any) => stopRun(row.id)"
        @remove="removeRun"
        @refresh="loadRuns()"
        @clear="handleClearRuns" />
    </el-splitter-panel>
  </el-splitter>
</template>

<style lang="scss" scoped>
.compare-page {
  height: 100%;
  min-height: 0;
  background: var(--el-bg-color);
  :deep(.el-splitter-panel) {
    display: flex;
    flex-direction: column;
    min-height: 0;
    overflow: hidden;
  }
  /* 配置与历史用同一层浅灰底，结果区留白——一眼能分出「编辑 / 查看记录」与「看输出」 */
  :deep(.compare-config) {
    min-width: 0;
    background: var(--fs-layout-background-color);
  }
  :deep(.compare-main) {
    min-width: 0;
  }
  :deep(.compare-history) {
    min-width: 0;
    background: var(--fs-layout-background-color);
    .side-panel {
      height: 100%;
    }
  }
}
/* 中列：工作区身份 + 配置 + 底部操作 */
.compare-config {
  /* 身份区：名称、保存状态、按钮挤在一行；名称可伸缩，其余固定 */
  .config-head {
    /* 两端对齐：左端是名称组，右端是保存按钮 */
    @include flex-between();
    gap: 8px;
    flex: none;
    padding: 10px 12px;
    border-bottom: solid 1px var(--el-border-color-lighter);
    .head-main {
      @include flex-start();
      gap: 6px;
      flex: 1;
      min-width: 0;
      .head-name {
        flex: 0 1 auto;
        min-width: 0;
        font-size: 13px;
        font-weight: 500;
        color: var(--el-text-color-primary);
        cursor: text;
        @include text-wrap();
      }
      .head-input {
        flex: 0 1 180px;
        min-width: 0;
      }
    }
    .head-more,
    .head-edit,
    .head-tag,
    .head-dot {
      flex: none;
    }
    /* 保存状态的小圆点：绿=已保存，橙=有未保存改动 / 新工作区 / 来自共享或历史 */
    .head-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: var(--el-color-warning);
      cursor: default;
      &.is-saved {
        background: var(--el-color-success);
      }
    }
  }
  /* 滚动交给 el-scrollbar；留白放内层，滚动条才能贴着面板边缘 */
  .config-body {
    flex: 1;
    min-height: 0;
  }
  .config-inner {
    padding: 12px;
  }
  .group {
    & + .group {
      margin-top: 14px;
    }
    .group-title {
      @include flex-start();
      gap: 6px;
      margin-bottom: 8px;
      font-size: 13px;
      font-weight: 500;
      color: var(--el-text-color-primary);
    }
  }
  .group-add {
    width: 100%;
  }
  .model-list {
    margin-top: 8px;
  }
  .list-hint {
    padding: 10px 4px;
    font-size: 12px;
    color: var(--el-text-color-placeholder);
  }
  /* 次要设置的展开条：一行小字 + 箭头，收起时不占地方 */
  .disclosure {
    @include flex-start();
    gap: 6px;
    margin-top: 10px;
    padding: 6px 4px;
    border-radius: 4px;
    font-size: 13px;
    color: var(--el-text-color-regular);
    cursor: pointer;
    user-select: none;
    &:hover {
      background: var(--el-fill-color-light);
    }
    .disclosure-caret {
      flex: none;
      color: var(--el-text-color-placeholder);
    }
    .disclosure-flag {
      margin-left: auto;
      font-size: 12px;
      color: var(--el-text-color-placeholder);
    }
  }
  .disclosure-body {
    padding: 6px 4px 0;
  }
  /* 底部操作行：发送在左，输出开关在右，两端对齐 */
  .config-foot {
    @include flex-between();
    flex: none;
    gap: 8px;
    padding: 10px 12px;
    border-top: solid 1px var(--el-border-color-lighter);
    .foot-switches {
      @include flex-start();
      flex: none;
      gap: 8px;
      /* 去掉 el-checkbox 默认的 30px 右间距，两个开关才挨得紧 */
      .el-checkbox {
        margin-right: 0;
      }
    }
  }
}
/* 右侧结果区 */
.compare-main {
  .main-head {
    @include flex-start();
    flex: none;
    gap: 8px;
    padding: 10px 16px;
    border-bottom: solid 1px var(--el-border-color-lighter);
    .head-title {
      font-size: 13px;
      font-weight: 500;
      color: var(--el-text-color-primary);
    }
    .head-meta {
      font-size: 12px;
      color: var(--el-text-color-placeholder);
    }
    .head-watch {
      flex: none;
    }
    /* 刷新与终止挨着放，都是这条记录上的动作，不与右侧「怎么看」的工具混在一起 */
    .head-watch-btn {
      flex: none;
    }
    .head-tools {
      margin-left: auto;
      @include flex-start();
      gap: 8px;
      .tool-label {
        font-size: 12px;
        color: var(--el-text-color-placeholder);
      }
      /* 去掉 el-checkbox 默认的 30px 右间距，跟旁边的控件贴齐 */
      .tool-raw {
        margin-right: 0;
      }
    }
  }
  .main-body {
    flex: 1;
    min-height: 0;
  }
  .main-inner {
    padding: 12px 16px 16px;
  }
  /* 列数由全局设置决定，窄屏时靠 minmax(0, 1fr) 保证不撑破容器 */
  .result-grid {
    display: grid;
    grid-template-columns: repeat(var(--compare-column, 1), minmax(0, 1fr));
    gap: 12px;
  }
  .main-empty {
    margin-top: 15vh;
  }
}
</style>
