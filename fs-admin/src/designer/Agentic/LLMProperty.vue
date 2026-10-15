<script setup lang="ts">
/**
 * 大语言模型节点属性 - 模型配置（模型名称、温度、思考模式与强度、AGENT 策略、系统提示词、多模态输入参数）、
 * 用户输入（变量编辑器）、工具列表（可添加与启用/停用）、记忆配置。
 */
import { ref, watch } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import DesignUtil from '@/utils/DesignUtil'
import AgenticApi from '@/api/agent/AgenticApi'
import KnowledgeApi from '@/api/agent/KnowledgeApi'
import DataThemeApi from '@/api/bi/DataThemeApi'
import OntologyApi from '@/api/kg/OntologyApi'
import ToolApi from '@/api/agent/ToolApi'
import CollapseItem from './CollapseItem.vue'
import MemoryField from './MemoryField.vue'
import ModelParamsField from './ModelParamsField.vue'
import NodeSlice from './NodeSlice.vue'
import OutputSlice from './OutputSlice.vue'
import ParamRow from './ParamRow.vue'
import SectionSlice from './SectionSlice.vue'
import VariableField from './VariableField.vue'
import VariableSelect from './VariableSelect.vue'
import { useCollapse } from './collapse'
import { methodAvailable, paramFields, paramHint, toolMethodCache, toolMethodLoaded, toolMethodsLoaded } from './tool'
import { variableToken } from './variable'

const active = ref('property')
const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
defineProps<{
  config?: any,
  instance?: any,
}>()

// 工具列表：可添加、可启用/停用，默认收起，仅一条时默认展开
const {
  isOpen: isToolOpen,
  open: openTool,
  toggle: toggleTool,
  remove: removeTool,
} = useCollapse(() => 1 === (model.value?.data?.tools ?? []).length)

/**
 * 内置工具（知识库、数据主题、本体、编排）同样以 function calling 暴露给模型，参数只有 query。
 * 候选列表交给 form-select 的远程检索：面板挂载不再预拉全量列表，
 * 只在用户输入关键词检索、或回显已选值时按需请求
 */

/** 内置工具的函数名：模型 function calling 只接受字母数字与下划线 */
const builtinName = (kind: string, id: any) => {
  if ('knowledge' === kind) return `knowledge_search_${id}`
  if ('theme' === kind) return `theme_query_${id}`
  if ('ontology' === kind) return `ontology_search_${id}`
  return `agentic_invoke_${id}`
}

/** 选中内置工具目标后：按选中行自动补函数名与描述（描述会展示给模型，用于判断何时调用） */
const handleBuiltinSelect = (item: any, kind: string, id: any, row: any) => {
  // 清空选择：函数名与描述一并清掉，避免留下 xxx_search_undefined 这类脏值
  if (undefined === id || null === id || '' === id) {
    item.name = ''
    item.description = ''
    if ('knowledge' === kind) item.knowledgeName = ''
    else if ('theme' === kind) item.themeName = ''
    else if ('ontology' === kind) item.ontologyName = ''
    else item.agenticName = ''
    return
  }
  item.name = builtinName(kind, id)
  item.description = row?.description || row?.name || ''
  if ('knowledge' === kind) item.knowledgeName = row?.name ?? ''
  else if ('theme' === kind) item.themeName = row?.name ?? ''
  else if ('ontology' === kind) {
    item.ontologyName = row?.name ?? ''
    // 本体描述为空时给一个能说明两种动作的默认描述
    if (!item.description) item.description = `检索「${item.ontologyName}」本体中的实体数据，或推理两个实体之间的关联路径`
  }
  else item.agenticName = row?.name ?? ''
}

const handleAddTool = (kind = 'method') => {
  if (!Array.isArray(model.value.data.tools)) model.value.data.tools = []
  const item: any = { id: DesignUtil.uuid(), kind, enabled: true }
  if ('method' === kind) item.toolId = ''
  model.value.data.tools.push(item)
  openTool(model.value.data.tools.length - 1)
}

const handleRemoveTool = (index: number) => {
  model.value.data.tools.splice(index, 1)
  removeTool(index)
}

// 方法明细缓存：按工具存放已取到的方法行，供执行变量与失效提示读取（面板内响应式）
const methodCache = ref<Record<string, any[]>>({})
const refreshMethods = () => { methodCache.value = Object.assign({}, toolMethodCache.data) }

/**
 * 写入方法明细：选择器返回的行自带参数明细，选中即缓存。
 * 工具与方法都可能有大量数据，不再按工具全量拉取，缓存只为已选中的方法服务
 */
const cacheMethod = (row: any) => {
  if (!row?.toolId || !row?.name) return
  const key = String(row.toolId)
  const rows: any[] = toolMethodCache.data[key] ?? []
  const index = rows.findIndex((item: any) => item.name === row.name)
  if (index >= 0) rows[index] = row
  else rows.push(row)
  toolMethodCache.data[key] = rows
  toolMethodLoaded[key] = true
  refreshMethods()
}

/**
 * 方法检索：候选走服务端分页检索（/tool/methodList），工具名与方法名都能命中。
 * 取值形如 `工具ID.方法名`：检索时按关键词查，回显时拆成工具与方法精确命中，选中行顺带写入明细缓存
 */
const handleMethodSelect = (params: any) => {
  const value = String(params?.value ?? '')
  const at = value.indexOf('.')
  const request: any = Object.assign({}, params, { withDetail: true })
  delete request.value
  if (at > 0) {
    request.toolId = value.slice(0, at)
    request.name = value.slice(at + 1)
  }
  return ToolApi.methodList(request).then((result: any) => {
    const rows: any[] = result?.data?.rows ?? []
    rows.forEach((row: any) => cacheMethod(row))
    return {
      data: {
        rows: rows.map((row: any) => Object.assign({}, row, {
          value: `${row.toolId}.${row.name}`,
          group: row.toolName,
          disabled: !methodAvailable(row),
        })),
      },
    }
  })
}

// 收起时的标题：工具名（+ 方法名），未选择时给出提示
const toolTitle = (item: any) => {
  if ('knowledge' === item?.kind) return `知识库 · ${item.knowledgeName || item.knowledgeId || '未选择'}`
  if ('theme' === item?.kind) return `数据主题 · ${item.themeName || item.themeId || '未选择'}`
  if ('ontology' === item?.kind) return `本体 · ${item.ontologyName || item.ontologyId || '未选择'}`
  if ('agentic' === item?.kind) return `编排 · ${item.agenticName || item.agenticId || '未选择'}`
  const name = item.toolName || item.toolId || '未选择工具'
  return item.method ? `${name} · ${item.method}` : name
}

/** 工具暴露的方法清单（后端解析工具配置后落库，含失效与停用标记） */
const methodsOf = (item: any) => {
  return item?.toolId ? methodCache.value[String(item.toolId)] ?? [] : []
}

/** 工具方法下拉的取值：`工具ID.方法名`（方法名已规范化为 [0-9a-zA-Z_-]，不含点） */
const toolMethodValue = (item: any) => {
  return item?.toolId && item?.method ? `${item.toolId}.${item.method}` : ''
}

/** 方法选项文案：方法名（展示名）+ 失效提示 */
const methodLabel = (method: any) => {
  const title = method?.title && method.title !== method.name ? `（${method.title}）` : ''
  return `${method.name}${title}${methodAvailable(method) ? '' : ' · 已失效'}`
}

/** 已选方法是否仍可调用 */
const methodUsable = (item: any) => {
  const method: any = methodsOf(item).find((method: any) => method.name === item?.method)
  return !method || methodAvailable(method)
}

/** 对象参数的字段：逐个字段配置执行变量，未配置的字段仍由模型决定 */
const fieldsOf = (parameter: any) => paramFields(parameter)

// 字段展开状态：默认只展示必填与已配置的字段，字段多时按需展开，避免面板被长表单撑爆
const expandedFields = ref<Record<string, boolean>>({})
const fieldKey = (item: any, parameter: any) => `${item?.id ?? ''}.${parameter?.name ?? ''}`
const visibleFields = (item: any, parameter: any) => {
  const fields = fieldsOf(parameter)
  if (fields.length <= 4 || expandedFields.value[fieldKey(item, parameter)]) return fields
  // 默认展示必填字段与已手工配置的字段，其余按需展开
  return fields.filter((field: any) => field.required || false === fieldArgOf(item, parameter, field).auto)
}
const hiddenFieldCount = (item: any, parameter: any) => {
  return fieldsOf(parameter).length - visibleFields(item, parameter).length
}
const toggleFields = (item: any, parameter: any) => {
  const key = fieldKey(item, parameter)
  expandedFields.value[key] = !expandedFields.value[key]
}

/** 已选方法的执行变量（方法参数）*/
const paramsOf = (item: any) => {
  const method: any = methodsOf(item).find((method: any) => method.name === item?.method)
  return method?.params ?? []
}

/**
 * 执行变量的绑定状态：`{ auto: true }` 由模型决定；`{ auto: false, value }` 手工指定内容，
 * 内容里可以混排固定字符串与变量占位符（`{{#节点标识.变量名#}}`），由后端按内容解析。
 * 对象参数按字段存 `{ source: 'fields', fields: { 字段名: 绑定 } }`，未配置的字段仍由模型决定
 */
const defaultArg = (parameter: any) => {
  return paramFields(parameter).length ? { source: 'fields', fields: {} } : { auto: true }
}

/** 历史数据归一：模型决定 / 引用变量 / 固定值 → 自动开关 + 内容（变量转成占位符） */
const normalizeArg = (exists: any) => {
  if (!exists || 'object' !== typeof exists) return { auto: true }
  if ('fields' === exists.source) return exists
  if (undefined !== exists.auto) return { auto: true === exists.auto, value: exists.value ?? '' }
  if ('model' === exists.source || undefined === exists.source) return { auto: true }
  if ('variable' === exists.source) {
    return { auto: false, value: exists.variable ? variableToken(exists.variable) : '' }
  }
  return { auto: false, value: exists.value ?? '' }
}

const argOf = (item: any, parameter: any) => {
  if (!item.args || 'object' !== typeof item.args) item.args = {}
  if (!item.args[parameter.name]) item.args[parameter.name] = defaultArg(parameter)
  const binding: any = item.args[parameter.name]
  // 对象参数按字段配置：历史数据里的整体取值不再使用，补齐字段结构
  if (paramFields(parameter).length && (!binding.fields || 'object' !== typeof binding.fields)) {
    binding.source = 'fields'
    binding.fields = {}
    return binding
  }
  // 普通参数：只在缺少 auto 时归一一次历史数据（渲染期不能反复改写，否则会触发递归更新）
  if (!paramFields(parameter).length && undefined === binding.auto) {
    item.args[parameter.name] = normalizeArg(binding)
  }
  return item.args[parameter.name]
}

/** 字段级绑定状态：对象参数的每个字段各自维护自动/手工 */
const fieldArgOf = (item: any, parameter: any, field: any) => {
  const binding = argOf(item, parameter)
  if (!binding.fields[field.name]) {
    binding.fields[field.name] = { auto: true }
    return binding.fields[field.name]
  }
  const exists: any = binding.fields[field.name]
  // 同上：仅在历史数据缺 auto 时归一一次
  if (undefined === exists.auto) binding.fields[field.name] = normalizeArg(exists)
  return binding.fields[field.name]
}

/** 按当前方法补齐/清理执行变量绑定，避免切换方法后残留上个方法的参数 */
const syncArgs = (item: any) => {
  // 方法明细未取到时不重建绑定：避免把已配置的执行变量清空（明细随选中行或回显补齐）
  if (item?.method && !methodsOf(item).length) return item
  const current: any = item?.args && 'object' === typeof item.args ? item.args : {}
  const next: any = {}
  paramsOf(item).forEach((parameter: any) => {
    const exists: any = current[parameter.name]
    const fields = paramFields(parameter)
    if (fields.length) {
      const values: any = {}
      fields.forEach((field: any) => {
        values[field.name] = normalizeArg(exists?.fields?.[field.name])
      })
      next[parameter.name] = { source: 'fields', fields: values }
      return
    }
    next[parameter.name] = normalizeArg(exists)
  })
  // 取值与结构都没变化时不替换对象：避免触发画布与面板的无谓重渲染（甚至递归更新）
  if (JSON.stringify(item?.args ?? {}) === JSON.stringify(next)) return item
  item.args = next
  return item
}

const syncToolArgs = () => {
  ;(model.value?.data?.tools ?? []).forEach((item: any) => syncArgs(item))
}

// 切换节点时同样要兜底（属性面板实例会在同类节点之间复用）
watch(model, (value: any) => {
  if (!value?.data) return
  syncToolArgs()
}, { immediate: true })

/** 选择工具方法：拆出工具与方法，换方法后按新方法的参数重建执行变量；row 为选中行（自带参数明细） */
const handleToolMethodChange = (item: any, value: any, row: any = null) => {
  // 选中行先入缓存，后面按方法参数重建执行变量时才有明细可读
  if (row) cacheMethod(row)
  const text = String(value ?? '')
  const at = text.indexOf('.')
  if (at < 0) {
    item.toolId = ''
    item.toolName = ''
    item.method = ''
  } else {
    item.toolId = text.slice(0, at)
    // 工具名由选中行带出，不必为了展示再拉一次工具清单
    item.toolName = row?.toolName ?? item.toolName ?? ''
    item.method = text.slice(at + 1)
  }
  item.args = {}
  syncArgs(item)
}

</script>

<template>
  <el-tabs v-model="active" class="tab-property">
    <el-tab-pane label="节点属性" name="property">
      <el-form :model="model" label-position="top">
        <NodeSlice v-model="model" :instance="$props.instance" :config="$props.config" :tips="tips" />
        <ModelParamsField
          v-model="model.data"
          :config="$props.config"
          :instance="$props.instance"
          :active-item="model">
          <template #extra>
            <ParamRow label="调度策略" wide>
              <!-- 用 el-select-v2：选项来自 options 数组，选中项能直接从数据解析出标签并即时回显
                   （el-select + el-option 在面板首次渲染时拿不到 option，选完不回显）；
                   下拉不跟随输入框宽度，避免 FunctionCalling 之类的长选项被截断 -->
              <el-select-v2
                v-model="model.data.agentStrategy"
                :options="$props.config?.agentStrategies ?? []"
                :fit-input-width="false"
                placeholder="请选择" />
            </ParamRow>
            <!-- 只有 ReAct 才循环迭代：每轮返回工具调用就执行并回填上下文继续推理 -->
            <ParamRow label="迭代次数" wide v-if="'react' === model.data.agentStrategy">
              <el-input-number v-model="model.data.maxIterations" :min="1" :max="50" :controls="false" placeholder="每批工具调用轮次" />
            </ParamRow>
            <ParamRow label="接续次数" wide v-if="'react' === model.data.agentStrategy">
              <el-input-number v-model="model.data.continueCount" :min="0" :max="5" :controls="false" placeholder="跑满一批后自动接续的次数" />
            </ParamRow>
            <el-form-item label="" v-if="'react' === model.data.agentStrategy">
              <tip-text>
                迭代次数是「一批」的工具往返轮次：跑满仍在请求工具就自动接续下一批，接续点会提醒模型提防无效轮转；
                接续次数不含首批（配 0 即跑满一批就收尾），用完后再禁用工具做一次收尾调用——收尾是额外一轮、不占批次，
                节点按已有信息作答并在输出上标记 wrapped，不再按节点异常中断。
              </tip-text>
            </el-form-item>
          </template>
        </ModelParamsField>
        <VariableField
          v-model="model.data.prompt"
          title="用户输入"
          :instance="$props.instance"
          :active-item="model"
          :height="180"
          placeholder="请输入用户输入，可插入上游变量" />
        <SectionSlice title="工具列表">
          <el-form-item label="">
            <div class="tool-slice">
              <CollapseItem
                :key="item.id"
                v-for="(item, index) in (model.data.tools ?? []) as any[]"
                :title="toolTitle(item)"
                :tags="[false === item.enabled ? '已停用' : '已启用']"
                :expanded="isToolOpen(index)"
                @toggle="toggleTool(index)"
                @delete="handleRemoveTool(index)">
                <!-- 知识库工具：模型给出 query，节点按知识库召回后回填 -->
                <template v-if="'knowledge' === item.kind">
                  <form-select
                    v-model="item.knowledgeId"
                    clearable
                    :callback="KnowledgeApi.list"
                    placeholder="输入名称检索知识库"
                    @change="(value: any, row: any) => handleBuiltinSelect(item, 'knowledge', value, row)" />
                  <el-input v-model="item.name" placeholder="函数名称，如 knowledge_search_1" />
                  <el-input
                    v-model="item.description"
                    type="textarea"
                    :autosize="{ minRows: 1, maxRows: 4 }"
                    resize="none"
                    placeholder="函数描述（展示给模型，说明何时调用）" />
                </template>
                <!-- 编排工具：模型给出 query，作为开始节点入参调用另一个编排应用 -->
                <template v-else-if="'agentic' === item.kind">
                  <form-select
                    v-model="item.agenticId"
                    clearable
                    :callback="AgenticApi.list"
                    placeholder="输入名称检索编排应用"
                    @change="(value: any, row: any) => handleBuiltinSelect(item, 'agentic', value, row)" />
                  <el-input v-model="item.name" placeholder="函数名称，如 agentic_invoke_1" />
                  <el-input
                    v-model="item.description"
                    type="textarea"
                    :autosize="{ minRows: 1, maxRows: 4 }"
                    resize="none"
                    placeholder="函数描述（展示给模型，说明何时调用）" />
                </template>
                <!-- 数据主题工具：不带 sql 返回主题数据字典（数据集/字段/关联），带 sql 执行查询 -->
                <template v-else-if="'theme' === item.kind">
                  <form-select
                    v-model="item.themeId"
                    clearable
                    :callback="DataThemeApi.list"
                    placeholder="输入名称检索数据主题"
                    @change="(value: any, row: any) => handleBuiltinSelect(item, 'theme', value, row)" />
                  <el-input v-model="item.name" placeholder="函数名称，如 theme_query_1" />
                  <el-input
                    v-model="item.description"
                    type="textarea"
                    :autosize="{ minRows: 1, maxRows: 4 }"
                    resize="none"
                    placeholder="函数描述（展示给模型，说明主题里有哪些数据）" />
                </template>
                <!-- 本体工具：模型给出实体类型与关键词，走 KG 图检索 -->
                <template v-else-if="'ontology' === item.kind">
                  <form-select
                    v-model="item.ontologyId"
                    clearable
                    :callback="OntologyApi.list"
                    placeholder="输入名称检索本体"
                    @change="(value: any, row: any) => handleBuiltinSelect(item, 'ontology', value, row)" />
                  <el-input v-model="item.name" placeholder="函数名称，如 ontology_search_1" />
                  <el-input
                    v-model="item.description"
                    type="textarea"
                    :autosize="{ minRows: 1, maxRows: 4 }"
                    resize="none"
                    placeholder="函数描述（展示给模型，说明本体里有哪些实体、支持检索与路径推理）" />
                </template>
                <!-- 工具与方法用一个分组下拉选择：按工具分组列出其方法，避免两级选择 -->
                <template v-else>
                  <form-select
                    :model-value="toolMethodValue(item)"
                    clearable
                    field-value="value"
                    :page-size="50"
                    group-field="group"
                    :callback="handleMethodSelect"
                    :label-formatter="(row: any) => methodLabel(row)"
                    placeholder="输入工具名或方法名检索"
                    @change="(value: any, row: any) => handleToolMethodChange(item, value, row)" />
                  <div class="method-tip" v-if="item.method && toolMethodsLoaded(item.toolId) && !methodsOf(item).length">
                    该工具未解析到方法，请到工具页重新解析
                  </div>
                  <div class="method-tip" v-else-if="item.method && methodsOf(item).length && !methodUsable(item)">
                    该方法已失效或已停用，请重新选择方法
                  </div>
                <!-- 执行变量：对象参数按字段分别配置，其余参数可整体替换 -->
                <div class="arg-list" v-if="paramsOf(item).length">
                  <div class="arg-block" :key="parameter.name" v-for="parameter in paramsOf(item)">
                    <div class="arg-head">
                      <span class="arg-name" :title="paramHint(parameter)">{{ parameter.name }}</span>
                      <span class="arg-type">{{ parameter.type }}{{ parameter.required ? ' · 必填' : '' }}</span>
                    </div>
                    <template v-if="fieldsOf(parameter).length">
                      <!-- 字段一行一个：字段名在左、自动开关在右两端对齐；关闭自动后补出内容编辑框 -->
                      <div class="field" :key="field.name" v-for="field in visibleFields(item, parameter)">
                        <div class="field-row">
                          <span class="field-name" :title="field.description">{{ field.name }}{{ field.required ? '*' : '' }}</span>
                          <span class="field-auto">
                            <el-switch
                              v-model="fieldArgOf(item, parameter, field).auto"
                              inline-prompt
                              active-text="自动"
                              inactive-text="手工"
                              title="自动：由模型决定；手工：按填写内容执行，内容可插入变量" />
                          </span>
                        </div>
                        <!-- 用外层容器控制间距/宽度：VariableField 是多分支根节点，class 无法自动继承 -->
                        <div class="field-value" v-if="false === fieldArgOf(item, parameter, field).auto">
                          <VariableField
                            compact
                            single
                            v-model="fieldArgOf(item, parameter, field).value"
                            :instance="$props.instance"
                            :active-item="model"
                            placeholder="请输入内容，可插入变量" />
                        </div>
                      </div>
                      <el-button
                        class="field-toggle"
                        link
                        type="primary"
                        size="small"
                        v-if="hiddenFieldCount(item, parameter) || expandedFields[fieldKey(item, parameter)]"
                        @click="toggleFields(item, parameter)">
                        {{ expandedFields[fieldKey(item, parameter)] ? '收起字段' : `展开其余 ${hiddenFieldCount(item, parameter)} 个字段` }}
                      </el-button>
                    </template>
                    <!-- 普通参数：参数名左侧、自动开关右侧两端对齐，关闭自动后补出内容编辑框 -->
                    <div class="field" v-else>
                      <div class="field-row">
                        <span class="field-name">取值</span>
                        <span class="field-auto">
                          <el-switch
                            v-model="argOf(item, parameter).auto"
                            inline-prompt
                            active-text="自动"
                            inactive-text="手工"
                            title="自动：由模型决定；手工：按填写内容执行，内容可插入变量" />
                        </span>
                      </div>
                      <div class="field-value" v-if="false === argOf(item, parameter).auto">
                        <VariableField
                          compact
                          single
                          v-model="argOf(item, parameter).value"
                          :instance="$props.instance"
                          :active-item="model"
                          placeholder="请输入内容，可插入变量" />
                      </div>
                    </div>
                  </div>
                </div>
                </template>
                <div class="field-inline">
                  <span>启用</span>
                  <el-switch v-model="item.enabled" />
                </div>
              </CollapseItem>
              <!-- 工具清单可添加三类能力：工具方法、知识库、编排应用 -->
              <el-dropdown class="tool-add" trigger="click" @command="handleAddTool">
                <el-button link type="primary" :icon="Plus">添加工具</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="method">工具方法</el-dropdown-item>
                    <el-dropdown-item command="knowledge">知识库</el-dropdown-item>
                    <el-dropdown-item command="theme">数据主题</el-dropdown-item>
                    <el-dropdown-item command="ontology">知识图谱本体</el-dropdown-item>
                    <el-dropdown-item command="agentic">编排应用</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </el-form-item>
        </SectionSlice>
        <MemoryField v-model="model.data" />
        <OutputSlice :data="model.data" />
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
.tool-slice {
  width: 100%;
  /* 添加工具按钮：与上方工具卡片保持间距 */
  .tool-add {
    display: inline-block;
    margin-top: 8px;
  }
  .collapse-body {
    .el-select {
      width: 100%;
    }
    /* 内置工具：下拉 + 函数名 + 函数描述（文本域）依次排列，行之间留出间距 */
    .el-select + .el-input,
    .el-input + .el-select,
    .el-input + .el-input,
    .el-input + .el-textarea,
    .el-select + .el-textarea {
      margin-top: 6px;
    }
    /**
     * 执行变量：一行一个方法参数 —— 参数名 + 取值来源，来源选了变量/固定值时
     * 再在下方补出对应的取值控件，只覆盖需要固定的参数时每行只占一行
     */
    .arg-list {
      margin-top: 6px;
      /**
       * 每个方法参数一块：标题行是参数名与类型，下面按字段或整体配置取值来源。
       * 对象参数（如 body）把字段拆成独立行，不配置的字段仍由模型决定
       */
      .arg-block {
        & + .arg-block {
          margin-top: 8px;
        }
        .arg-head {
          @include flex-start();
          gap: 6px;
          .arg-name {
            font-size: 12px;
            font-weight: 500;
            color: var(--el-text-color-primary);
          }
          .arg-type {
            font-size: 12px;
            color: var(--el-text-color-placeholder);
          }
        }
        .field-row {
          /* 字段名在左、自动开关在右：两端对齐 */
          @include flex-between();
          gap: 6px;
          margin-top: 6px;
          .field-name {
            flex: 1;
            min-width: 0;
            font-size: 12px;
            color: var(--el-text-color-regular);
            @include text-wrap();
          }
        }
        .field-auto {
          flex: none;
          @include flex-start();
          gap: 4px;
        }
        /* 内容编辑框独占一行：窄面板里编辑固定字符串与变量混排的内容也够用 */
        .field-value {
          width: 100%;
          margin-top: 6px;
        }
        .field-toggle {
          margin-top: 6px;
        }
      }
    }
    /* 方法失效/停用时的提示：已在编排里的引用不会静默换方法 */
    .method-tip {
      margin-top: 6px;
      font-size: 12px;
      color: var(--el-color-danger);
    }
    .field-inline {
      margin-top: 6px;
      font-size: 12px;
      color: var(--el-text-color-regular);
      @include flex-between();
    }
  }
}
</style>
