<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import type { FormInstance, TableInstance } from 'element-plus';
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router';
import PageApi from '@/api/spider/PageApi';
import ApiUtil from '@/utils/ApiUtil';
import DateUtil from '@/utils/DateUtil';
import TableUtil from '@/utils/TableUtil';
import SpiderBrowserPolicy from '@/designer/Spider/SpiderBrowserPolicy.vue';
import SpiderRequestModeSwitch from '@/designer/Spider/SpiderRequestModeSwitch.vue';

const model: any = defineModel()

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'name', label: '页面名称' },
  { prop: 'code', label: '页面编码' },
  { prop: 'url', label: '链接地址' },
  { prop: 'requestType', label: '请求类型' },
  { prop: 'fetchMode', label: '请求模式', formatter: (_row: any, _column: any, value: any) => config.value.fetchModes[value] || value },
  { prop: 'contentType', label: '参数类型', hide: true },
  { prop: 'priority', label: '优先级' },
  { prop: 'forceMode', label: '强制采集' },
  { prop: 'charset', label: '字符编码', hide: true },
  { prop: 'collection', label: '数据存储' },
  { prop: 'bucket', label: '文件存储' },
  { prop: 'connectTimeout', label: '连接超时', hide: true },
  { prop: 'socketTimeout', label: '执行超时', hide: true },
  { prop: 'iterateCount', label: '翻页深度', hide: true },
  { prop: 'retryCount', label: '重试次数', hide: true },
  { prop: 'description', label: '描述', hide: true },
  { prop: 'sort', label: '排序' },
  { prop: 'statusText', label: '状态' },
  { prop: 'createdTime', label: '创建时间', formatter: DateUtil.render, hide: true },
  { prop: 'updatedTime', label: '修改时间', formatter: DateUtil.render, hide: true },
])
const config: any = ref({
  ready: false,
  status: {},
  requestTypes: {},
  contentTypes: {},
  fetchModes: {},
  browserWaitModes: {},
  sorts: {},
})
const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, { advanced: false }))
const pagination = ref(RouteUtil.pagination(filters.value))
const selection: any = ref([])
const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage), { templateId: model.value.id })
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  PageApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
onMounted(() => {
  handleRefresh(false, true)
  PageApi.config().then((result: any) => {
    Object.assign(config.value, { ready: true }, ApiUtil.data(result))
  }).catch(() => {})
})

const infoVisible = ref(false)
const infoHeaders = ref<Record<string, string>>({})
const infoPayloadForm = ref<Record<string, string>>({})
const active = ref('request')
const formVisible = ref(false)
const formLoading = ref(false)
const form: any = ref({})
const formRef: any = ref<FormInstance>()
const rules = ref({
  name: [{ required: true, message: '请输入页面名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入页面编码', trigger: 'blur' }],
  url: [{ required: true, message: '请输入链接地址', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
})

const emptyForm = () => ({
  templateId: model.value.id,
  status: '1',
  requestType: 'GET',
  requestBody: '',
  contentType: 'none',
  payloadForm: {},
  fetchMode: 'http',
  browserWaitUntil: 'networkidle',
  browserWaitSelector: '',
  browserTimeout: 30000,
  forceMode: false,
  priority: 0,
  sort: 0,
  charset: '',
  collection: '',
  bucket: '',
  connectTimeout: 0,
  socketTimeout: 0,
  iterateCount: 0,
  retryCount: 0,
  headers: {},
  parser: '',
  mapper: '',
  description: '',
})

const normalizeObject = (value: any, label: string, notify = false): Record<string, string> => {
  if (value == null || value === '') return {}
  let object = value
  if (typeof object === 'string') {
    try {
      object = JSON.parse(object)
    } catch {
      if (notify) ElMessage.warning(label + '不是有效的 JSON 对象，已按空配置展示')
      return {}
    }
  }
  if (typeof object !== 'object' || Array.isArray(object)) {
    if (notify) ElMessage.warning(label + '必须是 JSON 对象，已按空配置展示')
    return {}
  }
  return Object.entries(object).reduce((result: Record<string, string>, [key, item]) => {
    result[key] = item == null ? '' : String(item)
    return result
  }, {})
}
const normalizeHeaders = (value: any, notify = false) => normalizeObject(value, '请求头', notify)
const normalizePayloadForm = (value: any, notify = false) => normalizeObject(value, '表单参数', notify)
const payloadFormable = computed(() => ['form-data', 'x-www-form-urlencoded'].includes(form.value.contentType))
const payloadBodyable = computed(() => ['json', 'xml', 'raw'].includes(form.value.contentType))
const handleFormatBody = () => {
  const body = String(form.value.requestBody ?? '').trim()
  if (!body) return
  try {
    if (form.value.contentType === 'xml') {
      form.value.requestBody = formatXml(body)
      return
    }
    if (form.value.contentType === 'json' || body.startsWith('{') || body.startsWith('[')) {
      form.value.requestBody = JSON.stringify(JSON.parse(body), null, 2)
      return
    }
    if (form.value.contentType === 'raw' && body.startsWith('<')) {
      form.value.requestBody = formatXml(body)
      return
    }
    ElMessage.warning('raw 请求体没有统一的格式化规则')
  } catch (error: any) {
    ElMessage.error('请求体格式错误：' + (error?.message || ''))
  }
}
const formatXml = (body: string) => {
  const document = new DOMParser().parseFromString(body, 'application/xml')
  if (document.getElementsByTagName('parsererror').length > 0) {
    throw new Error('XML 格式不正确')
  }
  if (!document.documentElement) throw new Error('XML 内容为空')
  const escapeText = (value: string) => value.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  const escapeAttribute = (value: string) => escapeText(value).replace(/"/g, '&quot;')
  const render = (node: Node, level: number): string[] => {
    const indent = '  '.repeat(level)
    if (node.nodeType === Node.ELEMENT_NODE) {
      const element = node as Element
      const attributes = Array.from(element.attributes)
        .map(attribute => ` ${attribute.name}="${escapeAttribute(attribute.value)}"`)
        .join('')
      const children = Array.from(element.childNodes)
        .filter(child => child.nodeType !== Node.TEXT_NODE || String(child.nodeValue ?? '').trim())
      if (!children.length) return [`${indent}<${element.tagName}${attributes} />`]
      if (children.length === 1 && children[0].nodeType === Node.TEXT_NODE) {
        return [`${indent}<${element.tagName}${attributes}>${escapeText(String(children[0].nodeValue ?? '').trim())}</${element.tagName}>`]
      }
      return [
        `${indent}<${element.tagName}${attributes}>`,
        ...children.flatMap(child => render(child, level + 1)),
        `${indent}</${element.tagName}>`,
      ]
    }
    if (node.nodeType === Node.TEXT_NODE) {
      const text = String(node.nodeValue ?? '').trim()
      return text ? [`${indent}${escapeText(text)}`] : []
    }
    if (node.nodeType === Node.CDATA_SECTION_NODE) {
      return [`${indent}<![CDATA[${String(node.nodeValue ?? '')}]]>`]
    }
    if (node.nodeType === Node.COMMENT_NODE) {
      return [`${indent}<!--${String(node.nodeValue ?? '')}-->`]
    }
    return []
  }
  const declaration = body.match(/^\s*(<\?xml[^>]*\?>)/i)?.[1]
  return [declaration, ...render(document.documentElement, 0)].filter(Boolean).join('\n')
}

const handleAdd = () => {
  form.value = emptyForm()
  active.value = 'request'
  formVisible.value = true
}
const handleShow = (scope: any) => {
  form.value = Object.assign({}, scope.row)
  infoHeaders.value = normalizeHeaders(scope.row.headers)
  infoPayloadForm.value = normalizePayloadForm(scope.row.payloadForm)
  infoVisible.value = true
}
const handleEdit = (scope: any) => {
  form.value = Object.assign({}, emptyForm(), scope.row, {
    templateId: model.value.id,
    status: String(scope.row.status),
    requestType: scope.row.requestType || 'GET',
    requestBody: scope.row.requestBody || '',
    contentType: scope.row.contentType || 'none',
    fetchMode: scope.row.fetchMode || 'http',
    browserWaitUntil: scope.row.browserWaitUntil || 'networkidle',
    browserWaitSelector: scope.row.browserWaitSelector || '',
    browserTimeout: scope.row.browserTimeout || 30000,
    payloadForm: normalizePayloadForm(scope.row.payloadForm, true),
    headers: normalizeHeaders(scope.row.headers, true),
  })
  active.value = 'request'
  formVisible.value = true
}
const handleSubmit = () => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid || formLoading.value) return
    formLoading.value = true
    const payload = Object.assign({}, form.value, {
      forceMode: !!form.value.forceMode,
      headers: JSON.stringify(normalizeHeaders(form.value.headers)),
      contentType: form.value.contentType || 'none',
      fetchMode: form.value.fetchMode || 'http',
      browserTimeout: form.value.browserTimeout || 30000,
      payloadForm: JSON.stringify(normalizePayloadForm(form.value.payloadForm)),
    })
    PageApi.save(payload, { success: true }).then(() => {
      handleRefresh(false, true)
      formVisible.value = false
    }).catch(() => {}).finally(() => {
      formLoading.value = false
    })
  })
}
const handleDelete = () => {
  TableUtil.selection(selection.value).then((ids: any) => {
    loading.value = true
    PageApi.delete(ids, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {
      loading.value = false
    })
  }).catch(() => {})
}
const placeholder = {
  parser: `Jsoup解析器使用方式：
jsoup {
    title: title(),
    -: body() {
        -1: select(".class-object") {
            detail: html(),
            property: select(".class-name") [
                key: select(".class-name").text(),
                value: select(".class-name").text()
            ]
        },
        -2: select(".class-array") [
            href: attr("abs:href"),
            description: select(".class-name:eq(0) > .class-name").html()
        ]
    }
}
`,
  mapper: `JS脚本：
context: { // 上下文
    url: '', // 实际请求的链接地址
    job: {}, // 作业信息
    task: {}, // 任务信息
    body: '', // 请求结果
    parsed: {}, // 解析结果
    status: 200, // 请求状态
    exception: null, // 异常信息
}
return: { // 返回结果
    collect: [{}], // 输出结果
    fetch: { // 发起新请求
        pageId: [{}], // 页面标识 -> 页面参数
    },
    iterate: 'url', // 分页迭代链接
}
`
}
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="名称" prop="name">
        <el-input v-model="filters.name" clearable />
      </form-search-item>
      <form-search-item label="编码" prop="code">
        <el-input v-model="filters.code" clearable />
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
        <button-add v-permit="'spider:page:add'" @click="handleAdd" />
        <button-delete v-permit="'spider:page:delete'" :disabled="selection.length === 0" @click="handleDelete" />
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
      </TableColumn>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button link @click="handleShow(scope)" v-permit="'spider:page:'">查看</el-button>
          <el-button link @click="handleEdit(scope)" v-permit="'spider:page:modify'">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" :loading="loading" @change="handleRefresh(true, true)" />
  </el-card>

  <el-drawer v-model="infoVisible" :title="'页面信息 - ' + form.id" size="80%">
    <layout-heading title="基础信息" />
    <el-descriptions :column="2" label-width="120px" border>
      <el-descriptions-item label="页面名称">{{ form.name }}</el-descriptions-item>
      <el-descriptions-item label="页面编码">{{ form.code }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ form.statusText }}</el-descriptions-item>
      <el-descriptions-item label="排序">{{ form.sort }}</el-descriptions-item>
      <el-descriptions-item label="优先级">{{ form.priority }}</el-descriptions-item>
      <el-descriptions-item label="强制采集">
        <el-tag :type="form.forceMode ? 'warning' : 'info'" effect="plain">{{ form.forceMode ? '已启用' : '未启用' }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="描述" :span="2">{{ form.description || '暂无' }}</el-descriptions-item>
      <el-descriptions-item label="创建者">{{ form.createdUserInfo?.name }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ DateUtil.format(form.createdTime) }}</el-descriptions-item>
      <el-descriptions-item label="修改者">{{ form.updatedUserInfo?.name }}</el-descriptions-item>
      <el-descriptions-item label="修改时间">{{ DateUtil.format(form.updatedTime) }}</el-descriptions-item>
    </el-descriptions>
    <layout-heading title="请求与采集配置" />
    <el-descriptions :column="2" label-width="120px" border>
      <el-descriptions-item label="链接地址" :span="2">{{ form.url }}</el-descriptions-item>
      <el-descriptions-item label="请求类型">{{ form.requestType || 'GET' }}</el-descriptions-item>
      <el-descriptions-item label="请求模式">{{ config.fetchModes[form.fetchMode] || form.fetchMode || '标准 HTTP' }}</el-descriptions-item>
      <el-descriptions-item label="参数类型">{{ form.contentType || 'none' }}</el-descriptions-item>
      <el-descriptions-item label="字符编码">{{ form.charset || '默认 UTF-8' }}</el-descriptions-item>
      <el-descriptions-item label="数据存储">{{ form.collection || '暂无' }}</el-descriptions-item>
      <el-descriptions-item label="文件存储">{{ form.bucket || '暂无' }}</el-descriptions-item>
      <el-descriptions-item label="连接超时">{{ form.connectTimeout }} 毫秒</el-descriptions-item>
      <el-descriptions-item label="执行超时">{{ form.socketTimeout }} 毫秒</el-descriptions-item>
      <el-descriptions-item label="翻页深度">{{ form.iterateCount }}</el-descriptions-item>
      <el-descriptions-item label="重试次数">{{ form.retryCount }}</el-descriptions-item>
      <template v-if="form.fetchMode === 'browser'">
        <el-descriptions-item label="页面就绪条件">{{ config.browserWaitModes[form.browserWaitUntil] || form.browserWaitUntil }}</el-descriptions-item>
        <el-descriptions-item label="等待元素">{{ form.browserWaitSelector || '未设置' }}</el-descriptions-item>
        <el-descriptions-item label="浏览器超时">{{ form.browserTimeout }} 毫秒</el-descriptions-item>
      </template>
    </el-descriptions>
    <template v-if="payloadFormable">
      <layout-heading title="表单参数" />
      <metadata-table v-model="infoPayloadForm" />
    </template>
    <template v-else-if="payloadBodyable">
      <layout-heading title="请求体" />
      <pre class="code-preview">{{ form.requestBody || '暂无请求体' }}</pre>
    </template>
    <layout-heading title="请求头" />
    <metadata-table v-model="infoHeaders" />
    <layout-heading title="页面解析器" />
    <pre class="code-preview">{{ form.parser || '暂无解析器配置' }}</pre>
    <layout-heading title="结果映射器" />
    <pre class="code-preview">{{ form.mapper || '暂无映射器配置' }}</pre>
  </el-drawer>

  <el-drawer v-model="formVisible" :close-on-click-modal="false" :show-close="false" :destroy-on-close="true" size="80%">
    <template #header="{ close, titleId, titleClass }">
      <h4 :id="titleId" :class="titleClass">{{ '页面配置' + (form.id ? (' - ' + form.id) : '') }}</h4>
      <el-space>
        <el-button type="primary" @click="handleSubmit" :loading="formLoading">保存</el-button>
        <el-button @click="close">关闭</el-button>
      </el-space>
    </template>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="auto">
      <layout-heading title="基础信息">
        <template #extra>
          <SpiderRequestModeSwitch v-model="form" :modes="config.fetchModes" />
        </template>
      </layout-heading>
      <el-descriptions border :column="2" class="basic-info-description">
        <el-descriptions-item label="页面名称">
          <el-form-item prop="name" class="descriptions-form-item">
            <el-input v-model="form.name" placeholder="请输入页面名称" />
          </el-form-item>
        </el-descriptions-item>
        <el-descriptions-item label="页面编码">
          <el-form-item prop="code" class="descriptions-form-item">
            <el-input v-model="form.code" placeholder="用于页面之间的调度引用" />
          </el-form-item>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-form-item prop="status" class="descriptions-form-item">
            <el-select v-model="form.status" placeholder="请选择">
              <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
            </el-select>
          </el-form-item>
        </el-descriptions-item>
        <el-descriptions-item label="排序">
          <form-input-number v-model="form.sort" />
        </el-descriptions-item>
        <el-descriptions-item label="优先级">
          <form-input-number v-model="form.priority" />
        </el-descriptions-item>
        <el-descriptions-item label="强制采集">
          <el-switch v-model="form.forceMode" inline-prompt active-text="是" inactive-text="否" />
          <span class="field-help">忽略已采集记录并重新采集</span>
        </el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">
          <el-input type="textarea" v-model="form.description" :rows="2" placeholder="页面用途或采集要求" />
        </el-descriptions-item>
      </el-descriptions>

      <div v-if="form.fetchMode === 'browser'" class="browser-policy-block">
        <layout-heading title="浏览器页面就绪策略" description="浏览器完成渲染后再进入页面解析" />
        <SpiderBrowserPolicy
          v-model="form"
          :wait-modes="config.browserWaitModes" />
      </div>

      <el-tabs v-model="active" class="config-tabs">
        <el-tab-pane label="请求参数" name="request">
          <el-alert
            type="info"
            show-icon
            :closable="false"
            title="链接地址支持 {变量} 占位符，变量值由页面映射器传递到后续页面"
            class="tab-alert" />
          <el-form-item label="链接地址" prop="url">
            <el-input v-model="form.url" placeholder="请求链接地址，支持{变量}占位符" clearable>
              <template #prepend>
                <el-select v-model="form.requestType" class="request-type-select">
                  <el-option v-for="(value, key) in config.requestTypes" :key="key" :value="key" :label="value" />
                </el-select>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="参数类型">
            <div class="parameter-type-row">
              <el-radio-group v-model="form.contentType">
                <el-radio v-for="(value, key) in config.contentTypes" :key="key" :value="key" :label="value" />
              </el-radio-group>
              <el-button v-if="payloadBodyable" text size="small" @click="handleFormatBody">格式化</el-button>
            </div>
          </el-form-item>
          <el-form-item label="表单参数" v-if="payloadFormable">
            <metadata-table v-model="form.payloadForm" :editable="true" />
            <div class="field-help request-body-help">
              {{ form.requestType === 'GET' ? 'GET 请求会将表单参数编码后拼接到链接地址' : '参数值支持 {变量} 占位符' }}
            </div>
          </el-form-item>
          <el-form-item label="请求体" v-else-if="payloadBodyable">
            <code-editor
              v-model="form.requestBody"
              mode="javascript"
              :height="180"
              placeholder="{&quot;keyword&quot;: &quot;{keyword}&quot;}"
              resizable />
            <div class="field-help request-body-help">
              {{ form.requestType === 'GET' ? 'GET 请求不会发送请求体，请切换请求方法' : '内容支持 {变量} 占位符，系统会按参数类型设置默认 Content-Type' }}
            </div>
          </el-form-item>
          <el-empty v-else description="不携带请求参数" :image-size="60" />
        </el-tab-pane>
        <el-tab-pane label="存储与超时" name="storage">
          <layout-heading title="存储与执行参数" />
          <el-descriptions border :column="2">
            <el-descriptions-item label="字符编码">
              <el-input v-model="form.charset" placeholder="默认 UTF-8" />
            </el-descriptions-item>
            <el-descriptions-item label="数据存储">
              <el-input v-model="form.collection" placeholder="Mongo 集合名称" />
            </el-descriptions-item>
            <el-descriptions-item label="文件存储">
              <el-input v-model="form.bucket" placeholder="S3 对象存储桶名称" />
            </el-descriptions-item>
            <el-descriptions-item label="翻页深度">
              <el-input-number v-model="form.iterateCount" :min="0" />
            </el-descriptions-item>
            <el-descriptions-item label="连接超时">
              <el-space>
                <el-input-number v-model="form.connectTimeout" :min="0" />
                <span>毫秒</span>
              </el-space>
            </el-descriptions-item>
            <el-descriptions-item label="执行超时">
              <el-space>
                <el-input-number v-model="form.socketTimeout" :min="0" />
                <span>毫秒</span>
              </el-space>
            </el-descriptions-item>
            <el-descriptions-item label="重试次数">
              <el-input-number v-model="form.retryCount" :min="0" />
            </el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
        <el-tab-pane label="请求头" name="header">
          <el-alert
            type="info"
            show-icon
            :closable="false"
            title="按字段名称和字段值维护请求头，认证类字段会以密文展示"
            class="tab-alert" />
          <metadata-table v-model="form.headers" :editable="true" />
        </el-tab-pane>
        <el-tab-pane label="页面解析" name="parser">
          <el-alert
            type="info"
            show-icon
            :closable="false"
            title="解析器把页面响应转换为结构化 JSON，支持 jsoup 与 json 表达式"
            class="tab-alert" />
          <code-editor v-model="form.parser" mode="javascript" :height="480" :placeholder="placeholder.parser" resizable fold-gutter />
        </el-tab-pane>
        <el-tab-pane label="结果映射" name="mapper">
          <el-alert
            type="info"
            show-icon
            :closable="false"
            title="映射器可输出采集结果，并通过 fetch 指定下一页面及参数，通过 iterate 指定分页链接"
            class="tab-alert" />
          <code-editor v-model="form.mapper" mode="javascript" :height="480" :placeholder="placeholder.mapper" resizable fold-gutter />
        </el-tab-pane>
      </el-tabs>
    </el-form>
  </el-drawer>
</template>

<style lang="scss" scoped>
.el-descriptions {
  margin-bottom: 15px;
}

.descriptions-form-item {
  width: 100%;
  margin-bottom: 0;
}

.field-help {
  margin-left: 12px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.request-type-select {
  width: 104px;

  :deep(.el-select__wrapper) {
    box-shadow: none;
    background: transparent;
  }
}

.request-body-help {
  margin-left: 0;
}

.parameter-type-row {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  gap: 12px;

  :deep(.el-radio-group) {
    flex: 1;
    min-width: 0;
    flex-wrap: wrap;
  }

  > .el-button {
    flex: none;
  }
}

.config-tabs {
  margin-top: 0;
}

.tab-alert {
  margin-bottom: 15px;
}

.code-preview {
  min-height: 80px;
  max-height: 360px;
  margin: 0 0 15px;
  padding: 12px;
  overflow: auto;
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
  background: var(--el-fill-color-lighter);
  color: var(--el-text-color-primary);
  font-family: monospace;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.basic-info-description,
.browser-policy-block {
  margin-bottom: 16px;
}

.basic-info-description :deep(.el-descriptions__body) {
  border-radius: 6px;
}
</style>
