<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import type { FormInstance, TableInstance } from 'element-plus';
import { ElMessage } from 'element-plus';
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router';
import ToolApi from '@/api/agent/ToolApi';
import ApiUtil from '@/utils/ApiUtil';
import DateUtil from '@/utils/DateUtil';
import TableUtil from '@/utils/TableUtil';
import RoleApi from '@/api/member/RoleApi';
import MetadataTable from '@/components/Data/MetadataTable.vue'
import { paramDetailText, paramHint, schemaSample } from '@/designer/Agentic/tool'

// 「MCP样例」按钮的兜底地址：正常取后端配置（fs.agent.mcp.sampleUrl，即本服务自带的 MCP 服务）
const MCP_SAMPLE_URL = 'http://localhost:7827/sse'

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'name', label: '工具名称' },
  { prop: 'typeText', label: '工具类型' },
  { prop: 'passSession', label: '传递会话', width: 90, slot: 'session' },
  { prop: 'methodCount', label: '方法数' },
  { prop: 'url', label: '调用地址', hide: true },
  { prop: 'labels', label: '标签', slot: 'labels' },
  { prop: 'roles', label: '授权角色', slot: 'role' },
  { prop: 'description', label: '描述', hide: true },
  { prop: 'sort', label: '排序' },
  { prop: 'statusText', label: '状态' },
])
const config: any = ref({
  ready: false,
  sorts: {},
  status: {},
  types: {},
  mcpSampleUrl: '',
})
const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, { advanced: false }))
const pagination = ref(RouteUtil.pagination(filters.value))
const selection: any = ref([])
const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  ToolApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
onMounted(() => {
  handleRefresh(false, true)
  ToolApi.config().then((result: any) => {
    Object.assign(config.value, { ready: true }, ApiUtil.data(result))
  }).catch(() => {})
})
const infoVisible = ref(false)
const formVisible = ref(false)
const formLoading = ref(false)
const form: any = ref({})
const formRef: any = ref<FormInstance>()
const rules = ref({
  name: [{ required: true, message: '请输入工具名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择工具类型', trigger: 'change' }],
  url: [{ required: true, message: '请输入调用地址', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
})
// 打开编辑抽屉时的配置快照：用于判断编辑中的内容与已保存的是否一致
const formSource = ref('')
const formSnapshot = () => JSON.stringify({ type: form.value.type, content: form.value.content })
const snapshotForm = () => { formSource.value = formSnapshot() }
const formDirty = computed(() => formSnapshot() !== formSource.value)

const handleAdd = () => {
  form.value = {
    status: '1',
    header: {},
    query: {},
    passSession: 0,
    labels: [],
    roleIds: [],
  }
  formVisible.value = true
  snapshotForm()
}
const handleShow = (scope: any) => {
  form.value = Object.assign({}, scope.row)
  infoVisible.value = true
  loadMethods(scope.row.id)
}
const handleEdit = (scope: any) => {
  form.value = Object.assign({}, scope.row, {
    status: scope.row.status + '',
    roleIds: scope.row.roleIds || [],
    labels: scope.row.labels || [],
    header: scope.row.header || {},
    query: scope.row.query || {},
    passSession: scope.row.passSession ?? 0,
  })
  formVisible.value = true
  snapshotForm()
  loadFormMethods(true)
}
const handleSubmit = () => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid || formLoading.value) return
    formLoading.value = true
    ToolApi.save(form.value, { success: true }).then(result => {
      handleRefresh(false, true)
      formVisible.value = false
    }).catch((error: any) => {
      // 保存失败多半是配置解析失败（如 OpenAPI 文档错误、MCP 内容里没有方法清单）：原因留在方法清单上方
      formParseError.value = ApiUtil.message(error) ?? ''
    }).finally(() => {
      formLoading.value = false
    })
  })
}
const handleDelete = () => {
  TableUtil.selection(selection.value).then((ids: any) => {
    loading.value = true
    ToolApi.delete(ids, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {
      loading.value = false
    })
  }).catch(() => {})
}
const handleJsonDemo = () => {
  form.value.content = `{
      "openapi": "3.1.0",
      "info": {
        "title": "Get weather data",
        "description": "Retrieves current weather data for a location.",
        "version": "v1.0.0"
      },
      "servers": [
        {
          "url": "https://weather.example.com"
        }
      ],
      "paths": {
        "/location": {
          "get": {
            "description": "Get temperature for a specific location",
            "operationId": "GetCurrentWeather",
            "parameters": [
              {
                "name": "location",
                "in": "query",
                "description": "The city and state to retrieve the weather for",
                "required": true,
                "schema": {
                  "type": "string"
                }
              }
            ],
            "deprecated": false
          }
        }
      },
      "components": {
        "schemas": {}
      }
    }`
}
const handleYamlDemo = () => {
  form.value.content = `# Taken from https://github.com/OAI/OpenAPI-Specification/blob/main/examples/v3.0/petstore.yaml

    openapi: "3.0.0"
    info:
      version: 1.0.0
      title: Swagger Petstore
      license:
        name: MIT
    servers:
      - url: https://petstore.swagger.io/v1
    paths:
      /pets:
        get:
          summary: List all pets
          operationId: listPets
          tags:
            - pets
          parameters:
            - name: limit
              in: query
              description: How many items to return at one time (max 100)
              required: false
              schema:
                type: integer
                maximum: 100
                format: int32
          responses:
            '200':
              description: A paged array of pets
              headers:
                x-next:
                  description: A link to the next page of responses
                  schema:
                    type: string
              content:
                application/json:
                  schema:
                    $ref: "#/components/schemas/Pets"
            default:
              description: unexpected error
              content:
                application/json:
                  schema:
                    $ref: "#/components/schemas/Error"
        post:
          summary: Create a pet
          operationId: createPets
          tags:
            - pets
          responses:
            '201':
              description: Null response
            default:
              description: unexpected error
              content:
                application/json:
                  schema:
                    $ref: "#/components/schemas/Error"
      /pets/{petId}:
        get:
          summary: Info for a specific pet
          operationId: showPetById
          tags:
            - pets
          parameters:
            - name: petId
              in: path
              required: true
              description: The id of the pet to retrieve
              schema:
                type: string
          responses:
            '200':
              description: Expected response to a valid request
              content:
                application/json:
                  schema:
                    $ref: "#/components/schemas/Pet"
            default:
              description: unexpected error
              content:
                application/json:
                  schema:
                    $ref: "#/components/schemas/Error"
    components:
      schemas:
        Pet:
          type: object
          required:
            - id
            - name
          properties:
            id:
              type: integer
              format: int64
            name:
              type: string
            tag:
              type: string
        Pets:
          type: array
          maxItems: 100
          items:
            $ref: "#/components/schemas/Pet"
        Error:
          type: object
          required:
            - code
            - message
          properties:
            code:
              type: integer
              format: int32
            message:
              type: string`
}
const handleMcpSync = () => {
  // 点「同步MCP配置」就意味着这个工具是 MCP 服务：顺手把类型切过来，
  // 否则内容按 OpenAPI 解析，页面只会显示「未解析到方法」，看不出是类型没选对
  form.value.type = 'mcp'
  const params = {
    url: form.value.url,
    header: form.value.header,
    query: form.value.query,
    passSession: form.value.passSession ?? 0,
  }
  formLoading.value = true
  formParseError.value = ''
  ToolApi.mcpSync(params, { success: true }).then((result: any) => {
    form.value.content = JSON.stringify(ApiUtil.data(result), null, 2)
    formParseError.value = ''
  }).catch((error: any) => {
    // 服务地址不通、握手失败、服务端没有 tools 能力：把原因留在页面上，别只剩一句「未解析到方法」
    formParseError.value = ApiUtil.message(error) ?? '同步 MCP 配置失败'
  }).finally(() => {
    formLoading.value = false
  })
}

// MCP样例：填入本服务自带的 MCP 服务地址（/sse），并立刻同步一次解析，方法清单直接可见
const handleMcpDemo = () => {
  form.value.type = 'mcp'
  form.value.url = config.value.mcpSampleUrl || MCP_SAMPLE_URL
  // 样例地址就是本服务自己的 MCP 服务：工具方法要求登录用户，默认把会话开关打开
  form.value.passSession = 1
  handleMcpSync()
}

/* ------------------------------- 方法清单 ------------------------------- */

// 已保存工具的方法：后端解析落库后读库返回（不对配置重复解析）
const methods = ref<any[]>([])
const methodsLoading = ref(false)
const loadMethods = (id: any) => {
  methods.value = []
  if (!id) return
  methodsLoading.value = true
  ToolApi.methods({ id }).then((result: any) => {
    methods.value = ApiUtil.data(result) ?? []
  }).catch(() => {}).finally(() => {
    methodsLoading.value = false
  })
}

// 编辑中的方法预览：解析当前内容但不落库（粘贴 OpenAPI 或同步 MCP 后立即可见）
const formMethods = ref<any[]>([])
const formParseError = ref('')
let parseTimer: any = null
const loadFormMethods = (immediate = false) => {
  clearTimeout(parseTimer)
  // 编辑器里的输入逐字触发，延迟一点再请求，避免每敲一个字符解析一次
  if (!immediate) {
    parseTimer = setTimeout(() => loadFormMethods(true), 600)
    return
  }
  formMethods.value = []
  // 没有内容时不重置错误：同步失败的原因要留在页面上
  if (!form.value.type || !form.value.content) return
  formParseError.value = ''
  const param = { type: form.value.type, content: form.value.content }
  ToolApi.parse(param).then((result: any) => {
    formMethods.value = ApiUtil.data(result) ?? []
  }).catch((error: any) => {
    formParseError.value = error?.message ?? '解析失败'
  })
}
watch(() => [form.value.type, form.value.content], () => {
  if (formVisible.value) loadFormMethods()
})

// 重新解析：按已保存工具的配置重解析并落库（存量工具或内容变更后重新解析）
const handleParseSource = () => {
  if (!form.value.id) {
    ElMessage.warning('请先保存工具后再重新解析')
    return
  }
  formLoading.value = true
  ToolApi.parseSource({ id: form.value.id }, { success: true }).then(() => {
    loadMethods(form.value.id) // 详情抽屉的方法清单
    loadFormMethods(true) // 编辑中的预览同步刷新
    handleRefresh(false, true) // 列表里的方法数
  }).catch(() => {}).finally(() => {
    formLoading.value = false
  })
}

// 方法启用 / 停用：描述与参数由解析结果决定，这里只维护可用状态
const handleMethodStatus = (method: any) => {
  const status = 1 === method.status ? 2 : 1
  ToolApi.methodSave({ id: method.id, status }, { success: true }).then(() => {
    method.status = status
  }).catch(() => {})
}

/* ------------------------------- 方法测试 ------------------------------- */

const testVisible = ref(false)
const testLoading = ref(false)
const testMethod = ref<any>(null)
const testValues = ref<Record<string, string>>({})
const testTab = ref('params')
const testResponse = ref('')
const testSuccess = ref(true)
const testStatus = ref<number | null>(null)
// 测试时可临时调整请求头（只作用于本次测试，不改动工具配置）
const testHeaders = ref<Record<string, string>>({})
// 响应头与响应体：与数据接口配置一致，响应头默认折叠
const testResponseHeaders = ref<Record<string, string>>({})
const testHeaderVisible = ref(false)

const testParams = computed<any[]>(() => testMethod.value?.params ?? [])
// 对象与数组参数按 JSON 录入，其余按文本
const complexParam = (item: any) => ['object', 'array'].indexOf(String(item?.type ?? '').toLowerCase()) >= 0

const handleTestOpen = (method: any) => {
  testMethod.value = method
  testValues.value = {}
  ;(method?.params ?? []).forEach((item: any) => {
    // 对象/数组参数按结构生成模板，直接改字段值即可，避免面对空白的 JSON 输入框
    testValues.value[item.name] = complexParam(item) && item.schema
      ? JSON.stringify(schemaSample(item.schema), null, 2)
      : ''
  })
  testHeaders.value = Object.assign({}, form.value.header ?? {})
  testTab.value = 'params'
  testResponse.value = ''
  testSuccess.value = true
  testStatus.value = null
  testResponseHeaders.value = {}
  testHeaderVisible.value = false
  testVisible.value = true
}

/** 请求方法与地址：OpenAPI 取 invoke 的 server + path，MCP 取服务地址 */
const testVerb = computed(() => {
  const invoke: any = testMethod.value?.invoke ?? {}
  return invoke.method ? String(invoke.method) : 'tools/call'
})
const testAddress = computed(() => {
  const invoke: any = testMethod.value?.invoke ?? {}
  const server = invoke.server || form.value.url || ''
  return invoke.path ? `${server}${invoke.path}` : server
})
const testContentType = computed(() => {
  const headers: any = testResponseHeaders.value ?? {}
  const key = Object.keys(headers).find((key: string) => 'content-type' === key.toLowerCase())
  return key ? headers[key] : ''
})
const testResponseHeaderText = computed(() => JSON.stringify(testResponseHeaders.value ?? {}, null, 2))

// 执行变量取值：对象/数组按 JSON 解析，布尔按真假，其余保持字符串
const testArgs = () => {
  const args: any = {}
  testParams.value.forEach((item: any) => {
    const value = String(testValues.value[item.name] ?? '').trim()
    if ('' === value) return
    const type = String(item.type ?? '').toLowerCase()
    if ('object' === type || 'array' === type) {
      try {
        args[item.name] = JSON.parse(value)
      } catch (error) {
        args[item.name] = value
      }
    } else if ('boolean' === type) {
      args[item.name] = 'false' !== value && '0' !== value
    } else {
      args[item.name] = value
    }
  })
  return args
}

const handleTestSubmit = () => {
  const missing = testParams.value.filter((item: any) => item.required && !String(testValues.value[item.name] ?? '').trim())
  if (missing.length) {
    ElMessage.warning(`请填写必填参数：${missing.map((item: any) => item.name).join('、')}`)
    return
  }
  // 带上当前配置（含编辑中未保存的内容），后端据此解析方法并真正发起一次调用
  const param = Object.assign({}, form.value, {
    header: testHeaders.value, // 测试可临时调整请求头
    methodName: testMethod.value?.name,
    args: testArgs(),
  })
  delete param.id
  testLoading.value = true
  ToolApi.test(param).then((result: any) => {
    const data: any = ApiUtil.data(result)
    testSuccess.value = false !== data?.success
    testStatus.value = undefined === data?.status ? null : Number(data.status)
    testResponseHeaders.value = data?.header ?? {}
    const response = data?.response ?? data?.message ?? ''
    const text = 'string' === typeof response ? response : JSON.stringify(response, null, 2)
    // 响应是 JSON 时自动格式化，便于直接查看
    let pretty = text
    try {
      pretty = JSON.stringify(JSON.parse(text), null, 2)
    } catch (error) {
      pretty = text
    }
    testResponse.value = pretty
    testTab.value = 'response'
  }).catch(() => {}).finally(() => {
    testLoading.value = false
  })
}

</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="名称" prop="name">
        <el-input v-model="filters.name" clearable />
      </form-search-item>
      <form-search-item label="类型" prop="type">
        <el-select v-model="filters.type" placeholder="请选择" clearable>
          <el-option v-for="(value, key) in config.types" :key="key" :value="key" :label="value" />
        </el-select>
      </form-search-item>
      <form-search-item label="状态" prop="status">
        <el-select v-model="filters.status" placeholder="请选择" clearable>
          <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
        </el-select>
      </form-search-item>
      <form-search-item>
        <el-button type="primary" @click="handleRefresh(true, false)" :loading="loading">查询</el-button>
        <el-button @click="filterRef?.resetFields()">重置</el-button>
      </form-search-item>
    </form-search>
  </el-card>
  <el-card :bordered="false" shadow="never" class="fs-table-card">
    <div class="fs-table-toolbar flex-between">
      <el-space>
        <button-add v-permit="'agent:tool:add'" @click="handleAdd" />
        <button-delete v-permit="'agent:tool:delete'" :disabled="selection.length === 0" @click="handleDelete" />
      </el-space>
      <el-space>
        <button-search @click="searchable = !searchable" />
        <button-refresh @click="handleRefresh(true, true)" :loading="loading" />
        <TableColumnSetting v-model="columns" :table="tableRef" :loading="loading" />
        <TableSort v-model="filters.sort" :columns="columns" :sortable="config.sorts" :loading="loading" @change="handleRefresh(true, true)" />
      </el-space>
    </div>
    <el-table
      ref="tableRef"
      :data="rows"
      :row-key="(record: any) => record.id"
      :border="true"
      v-loading="loading"
      table-layout="auto"
      @selection-change="(s: any) => selection = s"
    >
      <el-table-column type="selection" />
      <TableColumn :columns="columns">
        <template #session="scope">
          <el-tag :type="1 === scope.row.passSession ? 'success' : 'info'" size="small" effect="plain">
            {{ 1 === scope.row.passSession ? '是' : '否' }}
          </el-tag>
        </template>
        <template #role="scope">
          <el-space><el-tag v-for="item in scope.row.roles" :key="item.id">{{ item.name }}</el-tag></el-space>
        </template>
        <template #labels="scope">
          <el-space><el-tag v-for="item in scope.row.labels" :key="item">{{ item }}</el-tag></el-space>
        </template>
      </TableColumn>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button link @click="handleShow(scope)" v-permit="'agent:tool:'">查看</el-button>
          <el-button link @click="handleEdit(scope)" v-permit="'agent:tool:modify'">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" :loading="loading" @change="handleRefresh(true, true)" />
  </el-card>
  <el-drawer v-model="infoVisible" :title="'信息查看 - ' + form.id" size="60%">
    <el-descriptions :column="2" label-width="120px" border>
      <el-descriptions-item label="工具名称">{{ form.name }}</el-descriptions-item>
      <el-descriptions-item label="工具类型">{{ form.typeText }}</el-descriptions-item>
      <el-descriptions-item label="标签">
        <el-space><el-tag v-for="item in form.labels" :key="item">{{ item }}</el-tag></el-space>
      </el-descriptions-item>
      <el-descriptions-item label="授权角色">
        <el-space><el-tag v-for="item in form.roles" :key="item.id">{{ item.name }}</el-tag></el-space>
      </el-descriptions-item>
      <el-descriptions-item label="排序">{{ form.sort }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ form.statusText }}</el-descriptions-item>
      <el-descriptions-item label="传递用户会话">
        {{ 1 === form.passSession ? '是（x-auth-token）' : '否' }}
      </el-descriptions-item>
      <el-descriptions-item label="描述" :span="2">{{ form.description ? form.description : '暂无' }}</el-descriptions-item>
      <el-descriptions-item label="调用地址" :span="2">{{ form.url }}</el-descriptions-item>
      <el-descriptions-item label="配置信息" :span="2">
        <CodeEditor v-model="form.content" :height="300" mode="javascript" resizable />
      </el-descriptions-item>
      <el-descriptions-item label="方法清单" :span="2">
        <el-table :data="methods" size="small" border v-loading="methodsLoading" v-if="methods.length">
          <el-table-column prop="name" label="方法" min-width="140" show-overflow-tooltip />
          <el-table-column prop="description" label="描述" show-overflow-tooltip />
          <el-table-column label="参数" min-width="180" show-overflow-tooltip>
            <template #default="scope">{{ paramDetailText(scope.row) || '无' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="scope">
              <el-tag :type="1 === scope.row.present ? (1 === scope.row.status ? 'success' : 'info') : 'danger'" size="small" effect="plain">
                {{ 1 === scope.row.present ? (1 === scope.row.status ? '启用' : '停用') : '已失效' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="130" fixed="right">
            <template #default="scope">
              <el-button link @click="handleTestOpen(scope.row)">测试</el-button>
              <!-- 失效的方法不需要启停：重新解析后才可恢复 -->
              <el-button link @click="handleMethodStatus(scope.row)" v-if="1 === scope.row.present">
                {{ 1 === scope.row.status ? '停用' : '启用' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-text type="danger" size="small" v-else-if="form.parseError">{{ form.parseError }}</el-text>
        <el-text type="info" size="small" v-else>
          未解析到方法：可直接点「编辑」里的「重新解析」按已保存配置重解析；schema 工具需为 OpenAPI 文档（JSON / YAML），MCP 工具请先同步 MCP 配置
        </el-text>
      </el-descriptions-item>
      <el-descriptions-item label="请求头" :span="2"><metadata-table v-model="form.header" /></el-descriptions-item>
      <el-descriptions-item label="查询参数" :span="2"><metadata-table v-model="form.query" /></el-descriptions-item>
      <el-descriptions-item label="创建者">{{ form.createdUserInfo?.name }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ DateUtil.format(form.createdTime) }}</el-descriptions-item>
      <el-descriptions-item label="修改者">{{ form.updatedUserInfo?.name }}</el-descriptions-item>
      <el-descriptions-item label="修改时间">{{ DateUtil.format(form.updatedTime) }}</el-descriptions-item>
    </el-descriptions>
  </el-drawer>
  <el-drawer v-model="formVisible" :close-on-click-modal="false" :show-close="false" :destroy-on-close="true" size="60%">
    <template #header="{ close, titleId, titleClass }">
      <h4 :id="titleId" :class="titleClass">{{ '信息' + (form.id ? ('修改 - ' + form.id) : '添加') }}</h4>
      <el-space>
        <el-button type="primary" @click="handleSubmit" :loading="formLoading">确定</el-button>
        <el-button @click="close">取消</el-button>
      </el-space>
    </template>
    <el-form ref="formRef" :model="form" :rules="rules">
      <el-descriptions :column="2" label-width="120px" border>
        <el-descriptions-item label="工具名称"><el-input v-model="form.name" /></el-descriptions-item>
        <el-descriptions-item label="工具类型">
          <el-select v-model="form.type" placeholder="请选择">
            <el-option v-for="(value, key) in config.types" :key="key" :value="key" :label="value" />
          </el-select>
        </el-descriptions-item>
        <el-descriptions-item label="标签">
          <el-select v-model="form.labels" multiple filterable allow-create :reserve-keyword="false" default-first-option placeholder="输入后回车创建标签" />
        </el-descriptions-item>
        <el-descriptions-item label="授权角色">
          <form-select v-model="form.roleIds" :callback="RoleApi.list" multiple clearable />
        </el-descriptions-item>
        <el-descriptions-item label="排序"><el-input-number v-model="form.sort" /></el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-select v-model="form.status" placeholder="请选择">
            <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
          </el-select>
        </el-descriptions-item>
        <el-descriptions-item label="描述" :span="2"><el-input type="textarea" v-model="form.description" /></el-descriptions-item>
        <el-descriptions-item label="调用地址" :span="2"><el-input v-model="form.url" /></el-descriptions-item>
        <el-descriptions-item label="传递用户会话" :span="2">
          <el-space>
            <el-switch v-model="form.passSession" :active-value="1" :inactive-value="0" />
            <el-text type="info" size="small">开启后把当前登录会话作为 x-auth-token 发给工具服务</el-text>
          </el-space>
        </el-descriptions-item>
        <el-descriptions-item label="配置操作" :span="2">
          <el-space>
            <el-button @click="handleJsonDemo" :loading="formLoading">JSON样例</el-button>
            <el-button @click="handleYamlDemo" :loading="formLoading">YAML样例</el-button>
            <el-button @click="handleMcpDemo" :loading="formLoading">MCP样例</el-button>
            <el-button @click="handleMcpSync" :loading="formLoading" :disabled="!form.url">同步MCP配置</el-button>
            <!-- 编辑中的内容与已保存的不一致时，重新解析没有意义：保存时会自动解析 -->
            <el-button
              @click="handleParseSource"
              :loading="formLoading"
              :disabled="!form.id || formDirty"
              :title="formDirty ? '内容已修改，保存时会自动解析' : '按已保存的配置重新解析方法'">重新解析</el-button>
          </el-space>
        </el-descriptions-item>
        <el-descriptions-item label="配置信息" :span="2">
          <CodeEditor v-model="form.content" :height="300" mode="javascript" resizable />
        </el-descriptions-item>
        <el-descriptions-item label="方法清单" :span="2">
          <el-table :data="formMethods" size="small" border v-if="formMethods.length">
            <el-table-column prop="name" label="方法" min-width="140" show-overflow-tooltip />
            <el-table-column prop="description" label="描述" show-overflow-tooltip />
            <el-table-column label="参数" min-width="180" show-overflow-tooltip>
              <template #default="scope">{{ paramDetailText(scope.row) || '无' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="80" fixed="right">
              <template #default="scope">
                <el-button link @click="handleTestOpen(scope.row)">测试</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-text type="danger" size="small" v-else-if="formParseError">{{ formParseError }}</el-text>
          <el-text type="info" size="small" v-else>
            未解析到方法：schema 工具需为 OpenAPI 文档（JSON / YAML）；MCP 工具先选类型「MCP服务」再点「同步MCP配置」，服务地址填服务给出的 MCP 地址（SSE 的 /sse 与流式 HTTP 的 /mcp 都支持，需后端能直接访问）
          </el-text>
        </el-descriptions-item>
        <el-descriptions-item label="请求头" :span="2">
          <metadata-table v-model="form.header" :editable="true" />
        </el-descriptions-item>
        <el-descriptions-item label="查询参数" :span="2">
          <metadata-table v-model="form.query" :editable="true" />
        </el-descriptions-item>
      </el-descriptions>
    </el-form>
  </el-drawer>
  <!-- 方法测试：布局与交互参考数据接口配置（请求地址一行 + 发送，请求参数 / 请求头 / 响应结果分页签） -->
  <!-- 关闭方式与其它抽屉统一：只读/测试类抽屉用默认关闭按钮，编辑抽屉用确定+取消 -->
  <el-drawer v-model="testVisible" :title="'测试方法 - ' + (testMethod?.name ?? '')" :destroy-on-close="true" size="60%">
    <el-alert
      class="test-alert"
      type="info"
      :closable="false"
      show-icon
      :title="testMethod?.description"
      v-if="testMethod?.description" />
    <!-- 请求地址：方法 + 地址一行，右侧「发送」，与数据接口配置一致 -->
    <el-input v-model="testAddress" readonly placeholder="请求地址">
      <template #prepend>
        <span class="test-verb">{{ testVerb }}</span>
      </template>
      <template #append>
        <el-button type="primary" @click="handleTestSubmit" :loading="testLoading">发送</el-button>
      </template>
    </el-input>
    <el-tabs v-model="testTab" class="test-tabs">
      <el-tab-pane label="请求参数" name="params">
        <!-- 标签在输入框上方：参数名（+ 类型与是否必填）与取值各占一行，窄抽屉里也读得清 -->
        <el-form label-position="top" v-if="testParams.length">
          <el-form-item :key="item.name" v-for="item in testParams">
            <template #label>
              <span :title="paramHint(item)">{{ item.name }}</span>
              <span class="param-type">{{ item.type }}{{ item.required ? ' · 必填' : '' }}</span>
            </template>
            <el-input
              v-model="testValues[item.name]"
              type="textarea"
              :rows="4"
              :placeholder="item.description || '请输入 JSON 内容'"
              v-if="complexParam(item)" />
            <el-input
              v-model="testValues[item.name]"
              :placeholder="item.description || '请输入参数值'"
              v-else />
          </el-form-item>
        </el-form>
        <el-empty description="该方法没有参数" :image-size="80" v-else />
      </el-tab-pane>
      <el-tab-pane label="请求头" name="header">
        <metadata-table v-model="testHeaders" :editable="true" />
        <!-- 提示放表格下方并留出间距：不挤占页签与表格之间的空间 -->
        <div class="table-tip">
          <tip-text text="只作用于本次测试：请求头会随测试请求一起发送，不会改动工具配置" />
        </div>
      </el-tab-pane>
      <el-tab-pane label="响应结果" name="response">
        <template v-if="testStatus || testResponse">
          <div class="test-response-head">
            <el-space>
              <el-tag :type="testSuccess ? 'success' : 'danger'">{{ null === testStatus ? (testSuccess ? '成功' : '失败') : testStatus }}</el-tag>
              <el-tag effect="plain" v-if="testContentType">{{ testContentType }}</el-tag>
            </el-space>
            <el-switch v-model="testHeaderVisible" active-text="显示响应头" inactive-text="隐藏响应头" inline-prompt />
          </div>
          <el-descriptions class="test-headers" :column="1" border v-if="testHeaderVisible && Object.keys(testResponseHeaders).length">
            <el-descriptions-item v-for="(value, key) in testResponseHeaders" :key="key" :label="String(key)">{{ value }}</el-descriptions-item>
          </el-descriptions>
          <code-editor v-model="testResponse" mode="javascript" :height="320" resizable />
        </template>
        <el-empty description="暂无响应结果，请先发送请求" :image-size="80" v-else />
      </el-tab-pane>
    </el-tabs>
  </el-drawer>
</template>

<style lang="scss" scoped>
.test-alert {
  margin-bottom: 12px;
}
.param-type {
  margin-left: 6px;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}
/* 请求方法：与数据接口配置一样放在地址输入框的前置槽里 */
.test-verb {
  font-size: 13px;
  font-weight: 500;
  color: var(--el-text-color-primary);
}
.test-tabs {
  margin-top: 12px;
}
/* 表格类页签里的提示文案：放在表格下方并留出间距 */
.table-tip {
  margin-top: 10px;
}
/* 响应结果头部：状态码、内容类型与「显示响应头」开关 */
.test-response-head {
  margin-bottom: 12px;
  @include flex-between();
}
/* 响应头明细与下方响应体之间留出间距，展开响应头时不贴在一起 */
.test-headers {
  margin-bottom: 12px;
}
</style>
