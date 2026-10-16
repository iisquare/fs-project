<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { useProjectStore } from '@renderer/stores/project'
import { useCatalogStore } from '@renderer/stores/catalog'
import ApiUtil from '@renderer/utils/ApiUtil'
import { formatTokens } from '@renderer/core/Tokens'
import {
  fixedCount,
  paramFields,
  paramHint,
  paramTypeText,
  pruneArgs
} from '@renderer/utils/ToolUtil'
import { configOf, filenameOf, projectFile } from '@renderer/utils/ProjectUtil'
import LayoutIcon from '@renderer/components/Layout/LayoutIcon.vue'
import type {
  Project,
  ProjectEnv,
  ProjectTool,
  ProjectToolArg,
  ProjectToolArgs,
  ProjectToolKind,
  ProjectWorkspace
} from '@renderer/db/ProjectDb'

/**
 * 项目编辑面板：新建 / 编辑的待填项全部平铺在页面右侧区域，
 * 不用弹窗、不折叠成标签页，滚动即可看到全部字段。
 *
 * 项目有两条并行的执行路径：绑定「服务端编排应用」（独立服务），或直接选「模型」发起模型对话；
 * 工具按类型添加（与服务端编排的工具种类一致），凭据与调用都在服务端。
 */
const props = defineProps<{
  project: Project | null
  creating: boolean
}>()

const emit = defineEmits<{
  saved: [id: string]
  cancel: []
  /** 分享：表单已保存，页面据此跳到项目分享页继续发布 */
  share: [id: string]
}>()

const projectStore = useProjectStore()
const catalogStore = useCatalogStore()

interface ProjectForm {
  id: string
  model: string
  contextLength: number
  systemPrompt: string
  memory: string
  thinkEffort: string
  name: string
  description: string
  icon: string
  sort: number
  envVars: ProjectEnv[]
  tools: ProjectTool[]
  workspace: ProjectWorkspace[]
  inputs: Record<string, any>
}

/** 工具类型：与服务端编排里大语言模型节点的工具种类一致 */
const toolKinds: { value: ProjectToolKind; label: string }[] = [
  { value: 'method', label: '工具方法' },
  { value: 'knowledge', label: '知识库' },
  { value: 'theme', label: '数据主题' },
  { value: 'ontology', label: '知识图谱' },
  { value: 'agentic', label: '编排应用' }
]

/** 思考强度：与服务端编排、模型对比同一套取值；空值表示不下发、也不显式关闭思考 */
const thinkEfforts: { value: string; label: string }[] = [
  { value: '', label: '默认（不下发）' },
  { value: 'low', label: '低' },
  { value: 'medium', label: '中' },
  { value: 'high', label: '高' },
  { value: 'max', label: '最高' }
]

const formRef = ref<FormInstance>()
const saving = ref(false)
const form = ref<ProjectForm>(emptyForm())

const toolKind = ref<ProjectToolKind>('method')
const toolKeyword = ref('')
const searchResults = ref<any[]>([])
const searching = ref(false)

const isEdit = computed(() => Boolean(form.value.id))

/** 当前模型在服务端声明的上下文窗口：手工值留 0 时就是按它算比例 */
const declaredContextText = computed(() => {
  const declared = catalogStore.modelContext(String(form.value.model ?? '').trim())
  return declared > 0 ? `${formatTokens(declared)} 词元` : '未声明（需手工指定）'
})

/**
 * 模型候选：清单取自服务端（/bot/models），输入即在前端过滤；清单为空时先取一次再给候选。
 * 取值就是输入框里的文本，所以清单里没有的模型也能直接手填，不会被「只能选」挡住。
 */
async function modelSuggestions(query: string): Promise<any[]> {
  if (!catalogStore.models.length && !catalogStore.loadingModels) await catalogStore.fetchModels()
  const keyword = String(query ?? '')
    .trim()
    .toLowerCase()
  return catalogStore.models
    .map((item) => ({
      value: String(item?.name ?? ''),
      contextLength: Number(item?.contextLength ?? 0)
    }))
    .filter((item) => item.value && (!keyword || item.value.toLowerCase().includes(keyword)))
}

/** 某类型清单的加载失败原因：权限类失败已翻译成「找谁开通什么权限」 */
function toolLoadError(kind: ProjectToolKind): string {
  if ('knowledge' === kind) return catalogStore.knowledgeError
  if ('theme' === kind) return catalogStore.themeError
  if ('ontology' === kind) return catalogStore.ontologyError
  if ('agentic' === kind) return catalogStore.appError
  return ''
}

const toolError = computed(() => toolLoadError(toolKind.value))

function emptyForm(): ProjectForm {
  return {
    id: '',
    model: '',
    contextLength: 0,
    systemPrompt: '',
    memory: '',
    thinkEffort: '',
    name: '',
    description: '',
    icon: '',
    sort: 0,
    envVars: [],
    tools: [],
    workspace: [],
    inputs: {}
  }
}

function load(project: Project | null) {
  form.value = project
    ? {
        id: project.id,
        model: project.model,
        contextLength: project.contextLength,
        systemPrompt: project.systemPrompt,
        memory: project.memory,
        thinkEffort: project.thinkEffort,
        name: project.name,
        description: project.description,
        icon: project.icon,
        sort: project.sort,
        envVars: [...project.envVars],
        tools: [...project.tools],
        workspace: [...project.workspace],
        inputs: { ...project.inputs }
      }
    : emptyForm()
  toolKind.value = 'method'
  toolKeyword.value = ''
  searchResults.value = []
  openedArgs.value = {}
  paramsError.value = {}
  formRef.value?.clearValidate()
}

watch(
  () => [props.creating, props.project] as const,
  async ([creating, project]) => {
    if (!creating && !project) return
    load(creating ? null : (project as Project))
    // 目录按需加载：模型清单表单上立刻要用，工具清单等切到对应类型再取（见 loadToolOptions）
    await catalogStore.fetchModels()
  },
  { immediate: true }
)

/** 换类型时清掉上一次的检索结果与关键词，避免把知识库的清单带到数据主题上 */
watch(toolKind, () => {
  toolKeyword.value = ''
  searchResults.value = []
})

function cancel() {
  emit('cancel')
}

// ── 图标 ───────────────────────────────────────────────

/**
 * 选择本地图片：主进程把图片复制到图标目录后回传地址，这里只负责回填。
 * 网络地址照旧手填——两种来源在库里都是同一个字符串字段（见 icons.ts）。
 */
async function pickIcon(): Promise<void> {
  try {
    const icon = await window.ipc.icon.pick()
    if (icon) form.value.icon = icon
  } catch (error) {
    ElMessage.error(`选择本地图片失败：${error instanceof Error ? error.message : String(error)}`)
  }
}

// ── 环境变量 ───────────────────────────────────────────

function addEnv() {
  form.value.envVars.push({ key: '', value: '', secret: false })
}

function removeEnv(index: number) {
  form.value.envVars.splice(index, 1)
}

// ── 工具 ───────────────────────────────────────────────

/** 类型文案：列表行与提示里都用它 */
function toolKindText(kind: ProjectToolKind): string {
  return toolKinds.find((item) => item.value === kind)?.label ?? kind
}

/** 类型标签配色：只为扫读服务，含义仍由标签文字表达 */
function toolTagType(kind: ProjectToolKind): 'primary' | 'success' | 'warning' | 'info' | 'danger' {
  if ('knowledge' === kind) return 'success'
  if ('theme' === kind) return 'warning'
  if ('ontology' === kind) return 'info'
  if ('agentic' === kind) return 'danger'
  return 'primary'
}

/** 搜索框提示：各类型的检索口径一致，只是提示词不同 */
function toolSearchHint(kind: ProjectToolKind): string {
  if ('method' === kind) return '按工具或方法名搜索'
  return `按${toolKindText(kind)}名称搜索`
}

/**
 * 搜索当前类型的候选工具，结果落在 searchResults 里供列表添加。
 * 工具方法走服务端的方法检索（工具名与方法名都能命中），
 * 其余类型按名称过滤各自的服务端清单（编排应用清单没有关键词参数，在本地过滤）；
 * 关键词留空即列出全部（分页内的）。
 */
async function searchTools(): Promise<void> {
  const kind = toolKind.value
  const keyword = toolKeyword.value.trim()
  searching.value = true
  try {
    if ('method' === kind) {
      const result = await catalogStore.searchMethods(keyword, 1, 50)
      searchResults.value = result.rows
    } else if ('knowledge' === kind) {
      searchResults.value = await catalogStore.fetchKnowledges(keyword)
    } else if ('theme' === kind) {
      searchResults.value = await catalogStore.fetchThemes(keyword)
    } else if ('ontology' === kind) {
      searchResults.value = await catalogStore.fetchOntologies(keyword)
    } else if ('agentic' === kind) {
      const rows = await catalogStore.fetchApps()
      searchResults.value = keyword
        ? rows.filter((item) => String(item.name ?? '').includes(keyword))
        : rows
    }
  } finally {
    searching.value = false
  }
}

/** 候选行的展示名：工具方法带上所属工具，其余只展示名称 */
function toolRowName(row: any): string {
  if ('method' === toolKind.value) return `${row.toolName} / ${row.title || row.name}`
  return String(row?.name ?? `#${row?.id ?? ''}`)
}

/** 候选行是否已在工具清单里：按「类型 + 标识」判重，已在清单里的不再重复添加 */
function toolRowAdded(row: any): boolean {
  const kind = toolKind.value
  const id = Number(row?.id ?? 0)
  if (!id) return true
  return form.value.tools.some((item) => item.kind === kind && item.id === id)
}

/**
 * 添加候选行：工具方法额外记下所属工具与方法名（展示与排查用），其余只记标识与名称；
 * 新加的工具默认启用，是否参与运行由清单上的开关控制。
 */
function addToolRow(row: any): void {
  const kind = toolKind.value
  const id = Number(row?.id ?? 0)
  if (!id || toolRowAdded(row)) return
  if ('method' === kind) {
    form.value.tools.push({
      kind,
      id,
      name: `${row.toolName} / ${row.title || row.name}`,
      enabled: true,
      toolId: Number(row.toolId),
      toolName: String(row.toolName ?? ''),
      methodId: id,
      methodName: String(row.name ?? ''),
      title: String(row.title ?? row.name ?? ''),
      description: String(row.description ?? '')
    })
    // 选中行自带参数明细：直接入缓存，展开配置固定参数时不必再请求一次
    // （服务端没带上明细时不入缓存，展开时按工具标识与方法名再取一次）
    if (Array.isArray(row.params)) {
      cacheParams(form.value.tools[form.value.tools.length - 1], row.params)
    }
    return
  }
  form.value.tools.push({
    kind,
    id,
    name: String(row?.name ?? `${toolKindText(kind)} #${id}`),
    enabled: true,
    description: String(row?.description ?? '')
  })
}

function removeTool(index: number) {
  form.value.tools.splice(index, 1)
}

// ── 工具方法的固定参数 ──────────────────────────────────

/**
 * 固定参数：把某些参数从「模型决定」改成「按配置固定」，配置随项目落库，
 * 由服务端在调用时补齐（与编排里的执行变量同一套实现）。
 * 这里只管配置与展示——明细按需取，配置只存固定项。
 */
const methodParams = ref<Record<string, any[]>>({})
const paramsLoading = ref<Record<string, boolean>>({})
const paramsError = ref<Record<string, string>>({})
/** 展开参数配置的工具行：按行记，收起后配置仍在（只是不占地方） */
const openedArgs = ref<Record<string, boolean>>({})

/** 工具行的键：工具清单里的行按「类型 + 标识」定位 */
function rowKey(item: ProjectTool): string {
  return `${item.kind}-${item.id}`
}

/** 方法明细的键：工具标识 + 方法名（与服务端定位方法的口径一致） */
function methodKey(item: ProjectTool): string {
  return `${item.toolId ?? 0}.${item.methodName ?? ''}`
}

/** 方法的参数明细：已取到的直接读，没取到的返回空数组（要不要去取由调用方决定） */
function paramsOf(item: ProjectTool): any[] {
  return methodParams.value[methodKey(item)] ?? []
}

function cacheParams(item: ProjectTool, params: any[]): void {
  const key = methodKey(item)
  if (!key) return
  methodParams.value = Object.assign({}, methodParams.value, { [key]: params })
}

/** 读取方法明细：选中行自带的先入缓存，其余按工具标识与方法名取一次（force 为重新加载） */
async function loadParams(item: ProjectTool, force = false): Promise<void> {
  const key = methodKey(item)
  if (!key || (!force && methodParams.value[key])) return
  paramsLoading.value = Object.assign({}, paramsLoading.value, { [key]: true })
  paramsError.value = Object.assign({}, paramsError.value, { [key]: '' })
  const params = await catalogStore.fetchMethodParams(
    Number(item.toolId ?? 0),
    String(item.methodName ?? '')
  )
  if (null === params) {
    paramsError.value = Object.assign({}, paramsError.value, {
      [key]: '参数明细取不到：该方法可能已被删除或重新解析过，请到工具页确认后重新选择'
    })
  } else {
    cacheParams(item, params)
    syncArgs(item, params)
  }
  paramsLoading.value = Object.assign({}, paramsLoading.value, { [key]: false })
}

/** 展开 / 收起某个工具行的参数配置：展开时补一次明细 */
function toggleArgs(item: ProjectTool): void {
  const key = rowKey(item)
  const next = !openedArgs.value[key]
  openedArgs.value = Object.assign({}, openedArgs.value, { [key]: next })
  if (next) loadParams(item)
}

/** 对象参数的字段：逐个字段配「自动 / 固定」，未配置的字段仍由模型决定 */
function fieldsOf(
  parameter: any
): Array<{ name: string; type: string; required: boolean; description: string }> {
  return paramFields(parameter)
}

/**
 * 标量参数的绑定：默认「自动」（由模型决定），关掉后按填写内容固定下发。
 * 渲染期按需补齐默认值（与编排里的执行变量同一套做法），保存前再统一整理成「只留固定项」
 */
function argOf(item: ProjectTool, parameter: any): ProjectToolArg {
  if (!item.args || 'object' !== typeof item.args) item.args = {}
  const binding: any = item.args[parameter.name]
  if (!binding || 'object' !== typeof binding) {
    item.args[parameter.name] = { auto: true, value: '' }
    return item.args[parameter.name]
  }
  if (undefined === binding.auto && !binding.fields) binding.auto = true
  return binding
}

/** 对象参数的字段绑定：字段各自一个「自动 / 固定」 */
function fieldArgOf(item: ProjectTool, parameter: any, field: any): ProjectToolArg {
  const binding = argOf(item, parameter)
  if (!binding.fields || 'object' !== typeof binding.fields) binding.fields = {}
  if (!binding.fields[field.name]) binding.fields[field.name] = { auto: true, value: '' }
  return binding.fields[field.name]
}

/** 「参数」按钮上的提示：已配置固定项时给出数量 */
function fixedText(item: ProjectTool): string {
  const count = fixedCount(item.args)
  return count ? `（固定 ${count}）` : ''
}

/**
 * 按当前方法的参数整理绑定：方法换过或服务端重新解析后，不留上一个方法的残留
 * （明细还没取到时不动，避免把已配置的固定参数清空）
 */
function syncArgs(item: ProjectTool, params: any[]): void {
  const next: ProjectToolArgs = {}
  params.forEach((parameter: any) => {
    const binding: any = item.args?.[parameter.name]
    if (!binding || 'object' !== typeof binding) return
    const fields = fieldsOf(parameter)
    if (!fields.length) {
      next[parameter.name] = binding
      return
    }
    const values: Record<string, ProjectToolArg> = {}
    fields.forEach((field: any) => {
      const exists = binding.fields?.[field.name]
      if (exists) values[field.name] = exists
    })
    if (Object.keys(values).length) next[parameter.name] = { fields: values }
  })
  item.args = next
}

/** 保存前整理工具清单：固定参数只留「关掉自动且填了取值」的项（明细没取到的不动） */
function prunedTools(): ProjectTool[] {
  return form.value.tools.map((item) => {
    if ('method' !== item.kind || !item.args) return item
    const params = paramsOf(item)
    if (!params.length) return item
    return Object.assign({}, item, { args: pruneArgs(item.args, params) })
  })
}

/** 候选行的描述：工具方法取方法描述，其余取对象描述；服务端没写描述时给一句明确的话 */
function rowHelp(row: any): string {
  const description = String(row?.description ?? '').trim()
  if (description) return description
  if ('method' === toolKind.value)
    return `工具：${row?.toolName ?? ''}\n方法：${row?.title || row?.name || ''}\n服务端未给出方法描述`
  return `服务端未给出${toolKindText(toolKind.value)}描述`
}

// ── 工作区 ─────────────────────────────────────────────

async function addWorkspace(): Promise<void> {
  const path = await window.ipc.workspace.pick()
  if (!path) return
  if (form.value.workspace.some((item) => item.path === path)) return
  // 默认给读写：目录是用户自己挑的，进来先按最常用的方式用，要收紧再点左边的只读 / 只写
  form.value.workspace.push({ path, permission: 'readwrite' })
}

function removeWorkspace(index: number): void {
  form.value.workspace.splice(index, 1)
}

/**
 * 改这一行的目录：与「添加目录」同一个原生选择框，选中后就地替换路径。
 * 旧配置、分享来的配置里的目录在本机常常不存在，改一下比「移除再加一条」顺手。
 * 路径是这一行的身份（清单按路径去重、主进程按路径合并权限），所以撞上别的行就挡下来，
 * 只换到已经声明过的目录上不算改（选择框里又点回原目录时什么都不做）。
 */
async function changeWorkspace(index: number): Promise<void> {
  const path = await window.ipc.workspace.pick()
  if (!path) return
  const item = form.value.workspace[index]
  if (!item || path === item.path) return
  const exists = form.value.workspace.some(
    (other, current) => current !== index && other.path === path
  )
  if (exists) {
    ElMessage.warning('该目录已在工作区列表里')
    return
  }
  item.path = path
}

/** 主目录在清单里的位置：标了 primary 的那一行；没标就是第一行（与主进程的取值口径一致） */
function primaryIndex(): number {
  const index = form.value.workspace.findIndex((item) => item.primary === true)
  if (index >= 0) return index
  return form.value.workspace.length > 0 ? 0 : -1
}

/** 设为主目录：只留被点的那一行带标记，其余行清掉字段，配置里不堆 false */
function setPrimary(index: number): void {
  form.value.workspace.forEach((item, current) => {
    if (current === index) item.primary = true
    else delete item.primary
  })
}

// ── 保存 ───────────────────────────────────────────────

async function submit(): Promise<string> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return ''
  if (!form.value.model) {
    ElMessage.warning('请选择模型')
    return ''
  }
  saving.value = true
  // 保存前先记住是新建还是编辑，保存后 form 会被重新载入
  const editingId = form.value.id
  try {
    const payload = {
      model: String(form.value.model ?? '').trim(),
      contextLength: Math.max(0, Math.floor(Number(form.value.contextLength) || 0)),
      systemPrompt: form.value.systemPrompt,
      memory: form.value.memory,
      thinkEffort: form.value.thinkEffort,
      name: form.value.name,
      description: form.value.description,
      icon: form.value.icon,
      sort: form.value.sort,
      envVars: form.value.envVars.filter((item) => item.key),
      // 固定参数只落「固定项」：面板渲染期补的默认值（自动 / 空值）不写进项目
      tools: prunedTools(),
      workspace: form.value.workspace.filter((item) => item.path),
      inputs: form.value.inputs
    }
    let id = editingId
    if (editingId) {
      await projectStore.update(editingId, payload)
      ElMessage.success('项目已保存')
    } else {
      const result: any = await projectStore.create(payload)
      id = String(ApiUtil.data(result)?.id ?? '')
      ElMessage.success('项目已创建')
    }
    emit('saved', id)
    return id
  } finally {
    saving.value = false
  }
}

/**
 * 导出配置：把当前表单里的项目配置写成一份 JSON 文件（未保存的改动也一并导出），
 * 格式与项目分享导出的文件一致——两者都能通过「导入项目」回到本地。
 */
async function exportConfig(): Promise<void> {
  try {
    const config = configOf(form.value)
    const file = projectFile(config, null)
    const saved = await window.ipc.project.exportFile(
      filenameOf(config.name),
      JSON.stringify(file, null, 2)
    )
    if (saved.path) ElMessage.success(`已导出到 ${saved.path}`)
  } catch (error) {
    ElMessage.error(`导出失败：${error instanceof Error ? error.message : String(error)}`)
  }
}

/**
 * 分享到服务端：先把这一份配置保存下来（保证分享的就是页面上看到的），
 * 再由页面跳到项目分享页填写版本号与版本说明后发布。
 */
async function share(): Promise<void> {
  const id = await submit()
  if (id) emit('share', id)
}
</script>

<template>
  <el-form ref="formRef" :model="form" label-width="96px" class="project-editor">
    <section class="section">
      <div class="section-head">
        <div class="section-label">
          <span class="section-title">基本信息</span>
        </div>
      </div>
      <el-form-item
        label="名称"
        prop="name"
        :rules="[{ required: true, message: '请输入项目名称' }]"
      >
        <el-input v-model="form.name" placeholder="项目名称" />
      </el-form-item>
      <el-form-item label="图标">
        <div class="icon-field">
          <el-avatar :size="36" :src="form.icon">{{ form.name.charAt(0) }}</el-avatar>
          <el-input v-model="form.icon" placeholder="图标地址：网络地址，或点右侧选择本地图片">
            <template #suffix>
              <span class="icon-pick" title="选择本地图片" @click="pickIcon">
                <LayoutIcon name="plus.folder" />
              </span>
            </template>
          </el-input>
        </div>
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="form.description" type="textarea" :rows="3" placeholder="项目描述" />
      </el-form-item>
      <el-form-item label="模型">
        <!-- 可手填 + 可检索：autocomplete 的取值就是输入框文本，清单里没有的模型也填得进去 -->
        <el-autocomplete
          v-model="form.model"
          clearable
          :fetch-suggestions="modelSuggestions"
          placeholder="输入或检索模型名称"
          style="width: 100%"
        >
          <!-- 候选行顺带给出服务端声明的上下文窗口：选之前就知道这个模型多大 -->
          <template #default="{ item }">
            <span class="model-option-name">{{ item.value }}</span>
            <span v-if="item.contextLength" class="model-option-meta">
              {{ formatTokens(item.contextLength) }} 词元
            </span>
          </template>
        </el-autocomplete>
        <el-alert
          v-if="catalogStore.modelError"
          class="field-alert"
          type="warning"
          :closable="false"
          show-icon
          :title="catalogStore.modelError"
        >
          <el-button text type="primary" size="small" @click="catalogStore.fetchModels()">
            重试
          </el-button>
        </el-alert>
        <div v-else class="field-tip">
          模型清单取自服务端，可输入关键词检索，也可直接填写清单外的模型名称；
          新会话直接用该模型对话，历史存在本地。
        </div>
      </el-form-item>
      <el-form-item label="上下文大小">
        <el-input-number
          v-model="form.contextLength"
          class="context-field"
          :min="0"
          :step="1024"
          :controls="false"
          placeholder="留空按服务端声明的窗口"
        />
        <div class="field-tip">
          本项目手工指定的上下文窗口（词元数），圆环的用量比例按它算；填 0
          表示以服务端声明的窗口为准。
        </div>
        <div class="field-tip">当前模型在服务端声明的是 {{ declaredContextText }}。</div>
      </el-form-item>
      <el-form-item label="系统提示词">
        <el-input
          v-model="form.systemPrompt"
          type="textarea"
          :rows="4"
          resize="vertical"
          placeholder="每轮对话最前下发的 system 提示词（可为空）"
        />
        <div class="field-tip">
          作为首条 system 消息随每轮请求下发，项目里的所有会话都按它跑；留空则不下发。
        </div>
      </el-form-item>
      <el-form-item label="记忆">
        <el-input
          v-model="form.memory"
          type="textarea"
          :rows="4"
          resize="vertical"
          placeholder="长期记忆：希望模型一直记得的事实、偏好与约定（可为空）"
        />
        <div class="field-tip">
          与系统提示词分开存放：这里写「长期有用」的东西（用户偏好、项目约定、已定的决策），
          每轮作为提示词的一段（# 记忆）追加在系统提示词之后一起下发，模型只读、没有写入通道；
          改完保存，下一次提问就生效。
        </div>
        <div class="field-tip">
          它和系统提示词一样占每轮上下文（计入输入框旁的用量圆环）：只写长期有用的少量事实，
          别把一次性的任务、临时结论或整段对话抄进来。
        </div>
      </el-form-item>
      <el-form-item label="思考强度">
        <el-select v-model="form.thinkEffort" style="width: 100%">
          <el-option
            v-for="item in thinkEfforts"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <div class="field-tip">
          仅对支持思考的模型生效；默认不下发，由模型与网关取自己的默认强度。
        </div>
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="form.sort" :min="0" :max="999" />
      </el-form-item>
    </section>

    <section class="section">
      <div class="section-head">
        <div class="section-label">
          <span class="section-title">环境变量</span>
          <span class="section-count">{{ form.envVars.length }}</span>
        </div>
        <div class="section-actions">
          <el-button @click="addEnv">添加变量</el-button>
        </div>
      </div>
      <div v-if="form.envVars.length === 0" class="section-empty">暂无环境变量</div>
      <div v-for="(item, index) in form.envVars" :key="index" class="env-row">
        <el-input v-model="item.key" placeholder="变量名" />
        <el-input
          v-model="item.value"
          placeholder="变量值"
          :type="item.secret ? 'password' : 'text'"
          show-password
        />
        <el-checkbox v-model="item.secret">密钥</el-checkbox>
        <el-button text type="danger" @click="removeEnv(index)">删除</el-button>
      </div>
    </section>

    <section class="section">
      <div class="section-head">
        <div class="section-label">
          <span class="section-title">工具</span>
          <span class="section-count">{{ form.tools.length }}</span>
        </div>
        <div class="section-actions">
          <el-select v-model="toolKind" style="width: 130px">
            <el-option
              v-for="item in toolKinds"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          <el-input
            v-model="toolKeyword"
            :placeholder="toolSearchHint(toolKind)"
            style="width: 240px"
            @keyup.enter="searchTools"
          />
          <el-button :loading="searching" @click="searchTools">搜索</el-button>
        </div>
      </div>
      <div class="section-note">
        工具类型与服务端编排一致：工具方法、知识库、数据主题、知识图谱、编排应用；先搜索再从结果里添加，凭据与调用都在服务端。
      </div>
      <div v-if="toolError" class="section-warning">{{ toolError }}</div>
      <el-scrollbar v-if="searchResults.length" class="method-options" max-height="180px">
        <div v-for="row in searchResults" :key="`${toolKind}-${row.id}`" class="method-option">
          <span class="list-name">{{ toolRowName(row) }}</span>
          <!-- 帮助：候选行能看到服务端给出的描述，选之前先确认是不是这一只 -->
          <el-popover placement="left" :width="300" trigger="click">
            <template #reference>
              <el-button text class="help-button" title="查看描述">
                <LayoutIcon name="plus.help" />
              </el-button>
            </template>
            <div class="help-title">{{ toolRowName(row) }}</div>
            <el-scrollbar max-height="200px">
              <div class="help-body">{{ rowHelp(row) }}</div>
            </el-scrollbar>
          </el-popover>
          <span v-if="toolRowAdded(row)" class="list-tip">已添加</span>
          <el-button text type="primary" :disabled="toolRowAdded(row)" @click="addToolRow(row)">
            添加
          </el-button>
        </div>
      </el-scrollbar>
      <div v-if="form.tools.length === 0" class="section-empty">暂无工具</div>
      <div v-for="(item, index) in form.tools" :key="rowKey(item)" class="tool-item">
        <div class="list-row">
          <el-tag size="small" :type="toolTagType(item.kind)">{{ toolKindText(item.kind) }}</el-tag>
          <span class="list-name">{{ item.name || `#${item.id}` }}</span>
          <!-- 帮助：描述来自添加时保存的那一份（工具方法为方法描述） -->
          <el-popover placement="left" :width="300" trigger="click">
            <template #reference>
              <el-button text class="help-button" title="查看描述">
                <LayoutIcon name="plus.help" />
              </el-button>
            </template>
            <div class="help-title">{{ item.name || `#${item.id}` }}</div>
            <el-scrollbar max-height="200px">
              <div class="help-body">
                {{ item.description || `服务端未给出${toolKindText(item.kind)}描述` }}
              </div>
            </el-scrollbar>
          </el-popover>
          <!-- 工具方法可以把参数固定下来：模型不必再决定这些参数 -->
          <el-button
            v-if="'method' === item.kind"
            text
            type="primary"
            size="small"
            @click="toggleArgs(item)"
          >
            参数{{ fixedText(item) }}
          </el-button>
          <el-switch v-model="item.enabled" size="small" />
          <el-button text type="danger" @click="removeTool(index)">移除</el-button>
        </div>
        <!-- 固定参数：展开时才占地方，收起后配置照旧保留 -->
        <div v-if="'method' === item.kind && openedArgs[rowKey(item)]" class="arg-editor">
          <div class="arg-note">
            固定参数：切成「固定」并填写取值后，这一项不再交给模型，调用时由服务端按这里的取值补齐；
            留空视为不固定，未配置的参数仍由模型决定（对象与数组按 JSON 文本填写）。
          </div>
          <template v-if="paramsOf(item).length">
            <div v-for="parameter in paramsOf(item)" :key="parameter.name" class="arg-block">
              <div class="arg-head">
                <span class="arg-name">{{ parameter.name }}</span>
                <span class="arg-type">{{ paramTypeText(parameter) }}</span>
              </div>
              <!-- 对象参数按字段配：未列出的字段仍由模型决定 -->
              <template v-if="fieldsOf(parameter).length">
                <div v-for="field in fieldsOf(parameter)" :key="field.name" class="arg-row">
                  <span class="arg-field" :title="field.description">
                    {{ field.name }}{{ field.required ? '*' : '' }}
                  </span>
                  <el-switch
                    v-model="fieldArgOf(item, parameter, field).auto"
                    size="small"
                    inline-prompt
                    active-text="自动"
                    inactive-text="固定"
                    title="自动：由模型决定；固定：按填写内容下发"
                  />
                  <el-input
                    v-if="false === fieldArgOf(item, parameter, field).auto"
                    v-model="fieldArgOf(item, parameter, field).value"
                    size="small"
                    placeholder="固定取值"
                  />
                </div>
              </template>
              <div v-else class="arg-row">
                <span class="arg-field" :title="paramHint(parameter)">取值</span>
                <el-switch
                  v-model="argOf(item, parameter).auto"
                  size="small"
                  inline-prompt
                  active-text="自动"
                  inactive-text="固定"
                  title="自动：由模型决定；固定：按填写内容下发"
                />
                <el-input
                  v-if="false === argOf(item, parameter).auto"
                  v-model="argOf(item, parameter).value"
                  size="small"
                  placeholder="固定取值"
                />
              </div>
            </div>
          </template>
          <div v-else-if="paramsError[methodKey(item)]" class="arg-error">
            {{ paramsError[methodKey(item)] }}
            <el-button text type="primary" size="small" @click="loadParams(item, true)"
              >重新加载</el-button
            >
          </div>
          <div v-else class="arg-empty">
            <span v-if="paramsLoading[methodKey(item)]">正在读取参数明细…</span>
            <template v-else>
              该方法没有参数
              <el-button text type="primary" size="small" @click="loadParams(item, true)"
                >重新加载</el-button
              >
            </template>
          </div>
        </div>
      </div>
    </section>

    <section class="section">
      <div class="section-head">
        <div class="section-label">
          <span class="section-title">工作区</span>
          <span class="section-count">{{ form.workspace.length }}</span>
        </div>
        <div class="section-actions">
          <el-button @click="addWorkspace">添加目录</el-button>
        </div>
      </div>
      <div class="section-note">
        权限决定这个项目的会话能否读写该目录下的文件、能否在该目录下执行命令，
        保存后立刻生效，只影响本项目。
        主目录决定命令默认在哪个目录里执行、相对路径优先按哪个目录解析；没标主目录时用列表里的第一个。
      </div>
      <div v-if="form.workspace.length === 0" class="section-empty">暂无工作区目录</div>
      <!--
        key 用索引而不是路径：路径本身就是这一行里随手可改的输入框，
        拿它当 key 会让每次按键都换掉整行的 DOM（输入框当场丢焦点）。
      -->
      <div v-for="(item, index) in form.workspace" :key="index" class="workspace-row">
        <el-input v-model="item.path" placeholder="目录路径">
          <template #suffix>
            <span class="path-pick" title="重新选择这个目录" @click="changeWorkspace(index)">
              <LayoutIcon name="plus.folder" />
            </span>
          </template>
        </el-input>
        <el-radio-group v-model="item.permission">
          <el-radio-button label="只读" value="read" />
          <el-radio-button label="只写" value="write" />
          <el-radio-button label="读写" value="readwrite" />
        </el-radio-group>
        <div class="primary-slot">
          <el-tooltip
            v-if="primaryIndex() === index"
            content="命令默认在它里面执行，相对路径优先按它解析；要在它里面跑命令，权限得给写"
            placement="top"
          >
            <el-tag type="warning" effect="plain" size="small">主目录</el-tag>
          </el-tooltip>
          <el-button v-else text @click="setPrimary(index)">设为主目录</el-button>
        </div>
        <el-button text type="danger" @click="removeWorkspace(index)">移除</el-button>
      </div>
    </section>

    <div class="editor-footer">
      <el-button @click="exportConfig" title="导出当前表单里的配置，未保存的改动也一并导出">
        导出配置
      </el-button>
      <el-button v-if="isEdit" @click="share" title="先保存，再发布到服务端供自己与其他人使用">
        分享
      </el-button>
      <el-button @click="cancel">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">
        {{ isEdit ? '保存' : '创建' }}
      </el-button>
    </div>
  </el-form>
</template>

<style lang="scss" scoped>
.project-editor {
  padding: 4px 20px 0;
}

.section {
  padding: 16px 0;

  & + .section {
    border-top: 1px solid var(--el-border-color-lighter);
  }
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}

.section-label {
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--ev-c-text-1);
}

.section-count {
  min-width: 18px;
  padding: 0 6px;
  line-height: 18px;
  text-align: center;
  font-size: 12px;
  color: var(--ev-c-text-3);
  background: var(--el-fill-color);
  border-radius: 9px;
}

.section-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.section-note {
  margin: -6px 0 10px;
  font-size: 12px;
  color: var(--ev-c-text-3);
}

.section-empty {
  padding: 2px 0 6px;
  font-size: 13px;
  color: var(--ev-c-text-3);
}

.icon-field {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

/* 图标选择按钮：suffix 内层默认不吃点击（pointer-events: none），这里单独放开 */
.icon-pick {
  display: inline-flex;
  align-items: center;
  pointer-events: auto;
  cursor: pointer;
  transition: color var(--el-transition-duration);

  &:hover {
    color: var(--el-color-primary);
  }
}

/* 上下文大小：整行铺开，与旁边几个输入框同宽（数字靠左，单位写在下方的说明里） */
.context-field {
  width: 100%;

  :deep(.el-input__inner) {
    text-align: left;
  }
}

.field-tip {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--ev-c-text-3);
}

.field-warning,
.section-warning {
  font-size: 12px;
  line-height: 1.6;
  color: var(--el-color-warning);
}

.field-warning {
  margin-top: 4px;
}

.field-alert {
  margin-top: 6px;
}

.section-warning {
  margin: -6px 0 10px;
}

.env-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.6fr) auto auto;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.workspace-row {
  display: grid;
  /* 主目录这一列给固定宽度：「主目录」标记与「设为主目录」按钮长短不一，各行才对得齐 */
  grid-template-columns: minmax(0, 1fr) auto 104px auto;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.primary-slot {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  width: 100%;
}

/* 路径输入框里的「改目录」按钮：与图标选择按钮同一套（suffix 内层默认不吃点击，这里放开） */
.path-pick {
  display: inline-flex;
  align-items: center;
  pointer-events: auto;
  cursor: pointer;
  transition: color var(--el-transition-duration);

  &:hover {
    color: var(--el-color-primary);
  }
}

.list-row,
.method-option {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border-radius: 6px;

  &:hover {
    background: var(--el-fill-color-light);
  }
}

.method-options {
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  margin-bottom: 12px;
}

.list-name {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  color: var(--ev-c-text-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.list-tip {
  font-size: 12px;
  color: var(--ev-c-text-3);
}

/* 模型候选：名称 + 服务端声明的上下文窗口，选之前就知道这个模型多大 */
.model-option-name {
  color: var(--ev-c-text-1);
}

.model-option-meta {
  margin-left: 8px;
  font-size: 12px;
  color: var(--ev-c-text-3);
}

/* 帮助按钮：行内的小问号，点开看服务端给出的描述 */
.help-button {
  flex: none;
  height: auto;
  padding: 0 4px;
  color: var(--ev-c-text-3);

  &:hover {
    color: var(--el-color-primary);
  }
}

.help-title {
  margin-bottom: 6px;
  font-weight: 600;
  color: var(--ev-c-text-1);
}

.help-body {
  font-size: 12px;
  line-height: 1.7;
  color: var(--el-text-color-regular);
  white-space: pre-line;
  word-break: break-word;
}

/* 固定参数：挂在工具行下面，展开时才占地方 */
.tool-item {
  .arg-editor {
    margin: 0 8px 8px;
    padding: 8px 10px;
    border-radius: 6px;
    background: var(--el-fill-color-light);

    .arg-note {
      margin-bottom: 8px;
      font-size: 12px;
      line-height: 1.6;
      color: var(--ev-c-text-3);
    }

    .arg-block {
      & + .arg-block {
        margin-top: 8px;
        padding-top: 8px;
        border-top: 1px dashed var(--el-border-color-lighter);
      }

      .arg-head {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 6px;
      }

      .arg-name {
        font-size: 13px;
        font-weight: 500;
        color: var(--ev-c-text-1);
      }

      .arg-type {
        font-size: 12px;
        color: var(--ev-c-text-3);
      }

      .arg-row {
        display: flex;
        align-items: center;
        gap: 8px;

        & + .arg-row {
          margin-top: 6px;
        }
      }

      /* 参数名固定宽度：开关与输入框每行都在同一条竖线上 */
      .arg-field {
        flex: none;
        width: 112px;
        overflow: hidden;
        font-size: 12px;
        color: var(--ev-c-text-1);
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }

    .arg-error {
      font-size: 12px;
      line-height: 1.6;
      color: var(--el-color-danger);
    }

    .arg-empty {
      font-size: 12px;
      color: var(--ev-c-text-3);
    }
  }
}

.editor-footer {
  position: sticky;
  bottom: 0;
  z-index: 2;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 0;
  margin-top: 12px;
  background: var(--el-bg-color);
  border-top: 1px solid var(--el-border-color-lighter);
}
</style>
