<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import type { FormInstance, TableInstance } from 'element-plus';
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router';
import DataApi from '@/api/bi/DataApi';
import ApiUtil from '@/utils/ApiUtil';
import DateUtil from '@/utils/DateUtil';
import TableUtil from '@/utils/TableUtil';
import UIUtil from '@/utils/UIUtil';
import DataFieldMapping from '@/components/Data/DataFieldMapping.vue';
import DataPreview from '@/components/Data/DataPreview.vue';
import DataFieldSelect from '@/components/Data/DataFieldSelect.vue';
import LayoutIcon from '@/components/Layout/LayoutIcon.vue';

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'name', label: '名称' },
  { prop: 'url', label: '接口地址' },
  { prop: 'methodText', label: '请求方式' },
  { prop: 'contentType', label: '请求体类型', hide: true },
  { prop: 'timeout', label: '超时时间', hide: true },
  { prop: 'pks', label: '主键', slot: 'pks', hide: true },
  { prop: 'sort', label: '排序' },
  { prop: 'statusText', label: '状态' },
  { prop: 'description', label: '描述', hide: true },
  { prop: 'createdTime', label: '创建时间', formatter: DateUtil.render, hide: true },
])
const config: any = ref({
  ready: false,
  sorts: {},
  status: {},
  methods: {},
  contentTypes: {},
  fieldTypes: [],
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
  DataApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
onMounted(() => {
  handleRefresh(false, true)
  DataApi.config().then((result: any) => {
    Object.assign(config.value, { ready: true }, ApiUtil.data(result))
  }).catch(() => {})
})
const infoVisible = ref(false)
const formVisible = ref(false)
const formLoading = ref(false)
const form: any = ref({})
const formRef: any = ref<FormInstance>()
const rules = ref({
  name: [{ required: true, message: '请输入接口名称', trigger: 'blur' }],
  url: [{ required: true, message: '请输入接口地址', trigger: 'blur' }],
  method: [{ required: true, message: '请选择请求方式', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
})
const payloadFormable = computed(() => ['form-data', 'x-www-form-urlencoded'].includes(form.value.contentType))
const payloadBodyable = computed(() => ['json', 'xml', 'raw'].includes(form.value.contentType))
const handleAdd = () => {
  form.value = {
    status: '1',
    method: 'post',
    timeout: 3000,
    headers: {},
    contentType: 'json',
    payloadForm: {},
    payloadBody: '',
    pks: [],
    fields: [],
    pageRequestField: '',
    pageSizeRequestField: '',
    pageResponseField: '',
    pageSizeResponseField: '',
    totalResponseField: '',
  }
  lastTestResult.value = null
  formVisible.value = true
}
const handleShow = (scope: any) => {
  form.value = Object.assign({}, scope.row)
  infoVisible.value = true
}
const handleEdit = (scope: any) => {
  form.value = Object.assign({}, scope.row, {
    status: scope.row.status + '',
    headers: scope.row.headers || {},
    contentType: scope.row.contentType || 'none',
    payloadForm: scope.row.payloadForm || {},
    pks: scope.row.pks || [],
    fields: scope.row.fields || [],
    pageRequestField: scope.row.pageRequestField || '',
    pageSizeRequestField: scope.row.pageSizeRequestField || '',
    pageResponseField: scope.row.pageResponseField || '',
    pageSizeResponseField: scope.row.pageSizeResponseField || '',
    totalResponseField: scope.row.totalResponseField || '',
  })
  lastTestResult.value = null
  formVisible.value = true
}
const handleSubmit = () => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid || formLoading.value) return
    formLoading.value = true
    DataApi.save(form.value, { success: true }).then(() => {
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
    DataApi.delete(ids, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {
      loading.value = false
    })
  }).catch(() => {})
}
const active = ref('payload')
const pageFieldOptions = computed(() => {
  const options: any[] = []
  const pushed: Record<string, boolean> = {}
  ;(function walk(items: any) {
    items && items.forEach((item: any) => {
      const path = item.path
      if (path && !pushed[path]) {
        pushed[path] = true
        options.push({
          value: path,
          label: `${item.title || item.name || item.field || path} (${path})`,
        })
      }
      walk(item.children)
    })
  })(form.value.fields)
  return options
})
const queryPageFields = (query: string, callback: any) => {
  const keyword = (query || '').trim().toLowerCase()
  callback(keyword
    ? pageFieldOptions.value.filter((item: any) => item.label.toLowerCase().includes(keyword) || item.value.toLowerCase().includes(keyword))
    : pageFieldOptions.value
  )
}
const lastTestResult = ref<any>(null)
const handleFormatBody = () => {
  try {
    form.value.payloadBody = JSON.stringify(JSON.parse(form.value.payloadBody), null, 2)
  } catch (error: any) {
    ElMessage.error('JSON 格式错误：' + (error?.message || ''))
  }
}
const handleTest = () => {
  if (formLoading.value) return
  formLoading.value = true
  DataApi.test(form.value, { success: true }).then((result: any) => {
    lastTestResult.value = result
    form.value.fields = UIUtil.mergeDataFieldMapping(form.value.fields, ApiUtil.data(result)?.schema)
    active.value = 'schema'
  }).catch((result: any) => {
    lastTestResult.value = result
  }).finally(() => {
    formLoading.value = false
  })
}
const responseResult = computed(() => lastTestResult.value ? ApiUtil.data(lastTestResult.value) : null)
const responseHeaderVisible = ref(false)
const responseBody = ref('')
const responseStatusType = computed(() => {
  const status = responseResult.value?.status
  if (status >= 200 && status < 300) return 'success'
  if (status >= 300 && status < 400) return 'warning'
  if (status >= 400) return 'danger'
  return 'info'
})
const responseRaw = (data: any) => {
  if (data == null) return ''
  if (typeof data.json === 'string') return data.json
  try {
    return JSON.stringify(data.json ?? '', null, 2)
  } catch {
    return String(data.json ?? '')
  }
}
watch(responseResult, (data: any) => {
  responseBody.value = responseRaw(data)
}, { immediate: true })
const handleFormatResponse = () => {
  try {
    responseBody.value = JSON.stringify(JSON.parse(responseBody.value), null, 2)
  } catch (error: any) {
    ElMessage.error('JSON 格式错误：' + (error?.message || ''))
  }
}
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="名称" prop="name">
        <el-input v-model="filters.name" clearable />
      </form-search-item>
      <form-search-item label="接口地址" prop="url">
        <el-input v-model="filters.url" clearable />
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
        <button-add v-permit="'bi:dataApi:add'" @click="handleAdd" />
        <button-delete v-permit="'bi:dataApi:delete'" :disabled="selection.length === 0" @click="handleDelete" />
      </el-space>
      <el-space>
        <button-search @click="searchable = !searchable" />
        <button-refresh @click="handleRefresh(true, true)" :loading="loading" />
        <TableColumnSetting v-model="columns" :table="tableRef" />
        <TableSort v-model="filters.sort" :columns="columns" :sortable="config.sorts" @change="handleRefresh(true, true)" />
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
        <template #pks="scope">
          <DataFieldSelect :model-value="scope.row.pks" multiple />
        </template>
      </TableColumn>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button link @click="handleShow(scope)" v-permit="'bi:dataApi:'">查看</el-button>
          <el-button link @click="handleEdit(scope)" v-permit="'bi:dataApi:modify'">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
  </el-card>
  <el-drawer v-model="infoVisible" :title="'信息查看 - ' + form.id" size="80%">
    <layout-heading title="基础信息" />
    <el-descriptions :column="2" label-width="120px" border>
      <el-descriptions-item label="名称">{{ form.name }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ form.statusText }}</el-descriptions-item>
      <el-descriptions-item label="请求方式">{{ form.methodText }}</el-descriptions-item>
      <el-descriptions-item label="请求体类型">{{ form.contentType || '暂无' }}</el-descriptions-item>
      <el-descriptions-item label="超时时间">{{ form.timeout }}</el-descriptions-item>
      <el-descriptions-item label="排序">{{ form.sort }}</el-descriptions-item>
      <el-descriptions-item label="主键" :span="2">
        <DataFieldSelect v-if="form.pks?.length" :model-value="form.pks" multiple />
        <span v-else>暂无</span>
      </el-descriptions-item>
      <el-descriptions-item label="接口地址" :span="2">{{ form.url }}</el-descriptions-item>
      <el-descriptions-item label="请求头" :span="2"><metadata-table v-model="form.headers" /></el-descriptions-item>
      <el-descriptions-item v-if="payloadFormable" label="表单参数" :span="2"><metadata-table v-model="form.payloadForm" /></el-descriptions-item>
      <el-descriptions-item v-if="payloadBodyable" label="请求体" :span="2">
        <code-editor v-model="form.payloadBody" mode="javascript" :height="120" resizable />
      </el-descriptions-item>
      <el-descriptions-item label="描述" :span="2">{{ form.description || '暂无' }}</el-descriptions-item>
      <el-descriptions-item label="创建者">{{ form.createdUserInfo?.name }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ DateUtil.format(form.createdTime) }}</el-descriptions-item>
      <el-descriptions-item label="修改者">{{ form.updatedUserInfo?.name }}</el-descriptions-item>
      <el-descriptions-item label="修改时间">{{ DateUtil.format(form.updatedTime) }}</el-descriptions-item>
    </el-descriptions>
    <layout-heading title="字段配置" style="margin-top:15px" />
    <DataFieldMapping v-model="form.fields" :types="config.fieldTypes" />
    <layout-heading title="分页配置" style="margin-top:15px" />
    <el-descriptions :column="2" label-width="150px" border>
      <el-descriptions-item label="页码传参字段">{{ form.pageRequestField || '暂无' }}</el-descriptions-item>
      <el-descriptions-item label="分页大小传参字段">{{ form.pageSizeRequestField || '暂无' }}</el-descriptions-item>
      <el-descriptions-item label="页码返回字段">{{ form.pageResponseField || '暂无' }}</el-descriptions-item>
      <el-descriptions-item label="分页大小返回字段">{{ form.pageSizeResponseField || '暂无' }}</el-descriptions-item>
      <el-descriptions-item label="记录总数返回字段" :span="2">{{ form.totalResponseField || '暂无' }}</el-descriptions-item>
    </el-descriptions>
  </el-drawer>
  <el-drawer v-model="formVisible" :close-on-click-modal="false" :show-close="false" :destroy-on-close="true" size="80%">
    <template #header="{ close, titleId, titleClass }">
      <h4 :id="titleId" :class="titleClass">{{ '信息' + (form.id ? ('修改 - ' + form.id) : '添加') }}</h4>
      <el-space>
        <el-button type="primary" @click="handleSubmit" :loading="formLoading">确定</el-button>
        <el-button @click="close">取消</el-button>
      </el-space>
    </template>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="auto">
      <layout-heading title="基础信息" />
      <el-descriptions :column="2" border>
        <el-descriptions-item label="名称" prop="name">
          <el-input v-model="form.name" />
        </el-descriptions-item>
        <el-descriptions-item label="状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择">
            <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
          </el-select>
        </el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">
          <el-input type="textarea" v-model="form.description" />
        </el-descriptions-item>
        <el-descriptions-item label="超时时间">
          <el-space>
            <form-input-number v-model="form.timeout" />
            <span>毫秒</span>
          </el-space>
        </el-descriptions-item>
        <el-descriptions-item label="排序">
          <form-input-number v-model="form.sort" />
        </el-descriptions-item>
        <el-descriptions-item label="主键" :span="2">
          <DataFieldSelect v-model="form.pks" v-model:fields="form.fields" editable multiple />
        </el-descriptions-item>
      </el-descriptions>
    </el-form>
    <layout-heading title="请求配置" />
    <el-input v-model="form.url" placeholder="请求接口地址" clearable>
      <template #prepend>
        <el-select v-model="form.method" placeholder="请求方式" style="width: 80px">
          <el-option v-for="(value, key) in config.methods" :key="key" :value="key" :label="value" />
        </el-select>
      </template>
      <template #append>
        <el-button type="primary" @click="handleTest" :loading="formLoading">发送</el-button>
      </template>
    </el-input>
    <el-alert type="warning" show-icon :title="lastTestResult?.message" :description="lastTestResult?.data" :closable="false" v-if="ApiUtil.failed(lastTestResult)" />
    <el-tabs v-model="active">
      <el-tab-pane label="请求参数" name="payload">
        <div class="flex-between" style="margin-bottom: 15px">
          <el-radio-group v-model="form.contentType">
            <el-radio v-for="(value, key) in config.contentTypes" :key="key" :value="key" :label="value" />
          </el-radio-group>
          <el-space>
            <el-button size="small" @click="handleFormatBody" text v-if="payloadBodyable">
              <LayoutIcon name="action.beautify" /><span>格式化</span>
            </el-button>
          </el-space>
        </div>
        <metadata-table v-model="form.payloadForm" :editable="true" v-if="payloadFormable" />
        <code-editor v-model="form.payloadBody" mode="javascript" :height="300" resizable v-else-if="payloadBodyable" />
        <el-empty v-else description="暂无请求参数配置" />
      </el-tab-pane>
      <el-tab-pane label="请求头" name="header">
        <metadata-table v-model="form.headers" :editable="true" />
      </el-tab-pane>
      <el-tab-pane label="字段映射" name="schema">
        <DataFieldMapping v-model="form.fields" :types="config.fieldTypes" editable />
      </el-tab-pane>
      <el-tab-pane label="分页配置" name="pagination">
        <el-alert type="info" show-icon :closable="false" title="字段名称为文本录入，聚焦输入框后可从响应结构字段映射的 path 中辅助选择" style="margin-bottom: 15px" />
        <el-descriptions :column="1" label-width="150px" border>
          <el-descriptions-item label="页码传参字段">
            <el-autocomplete v-model="form.pageRequestField" :fetch-suggestions="queryPageFields" placeholder="请输入字段路径，可聚焦后选择辅助录入" clearable />
          </el-descriptions-item>
          <el-descriptions-item label="分页大小传参字段">
            <el-autocomplete v-model="form.pageSizeRequestField" :fetch-suggestions="queryPageFields" placeholder="请输入字段路径，可聚焦后选择辅助录入" clearable />
          </el-descriptions-item>
          <el-descriptions-item label="页码返回字段">
            <el-autocomplete v-model="form.pageResponseField" :fetch-suggestions="queryPageFields" placeholder="请输入字段路径，可聚焦后选择辅助录入" clearable />
          </el-descriptions-item>
          <el-descriptions-item label="分页大小返回字段">
            <el-autocomplete v-model="form.pageSizeResponseField" :fetch-suggestions="queryPageFields" placeholder="请输入字段路径，可聚焦后选择辅助录入" clearable />
          </el-descriptions-item>
          <el-descriptions-item label="记录总数返回字段">
            <el-autocomplete v-model="form.totalResponseField" :fetch-suggestions="queryPageFields" placeholder="请输入字段路径，可聚焦后选择辅助录入" clearable />
          </el-descriptions-item>
        </el-descriptions>
      </el-tab-pane>
      <el-tab-pane label="数据预览" name="data">
        <DataPreview :json="responseResult?.json" :schema="form.fields" :loading="formLoading" />
      </el-tab-pane>
      <el-tab-pane label="响应结果" name="response">
        <template v-if="responseResult">
          <div class="flex-between" style="margin-bottom: 15px">
            <el-space>
              <el-tag :type="responseStatusType">{{ responseResult.status }}</el-tag>
              <el-tag effect="plain">{{ responseResult.headers?.['Content-Type'] || responseResult.headers?.['content-type'] || '未知' }}</el-tag>
            </el-space>
            <el-space>
              <el-switch v-model="responseHeaderVisible" active-text="显示响应头" inactive-text="隐藏响应头" inline-prompt />
              <el-button size="small" @click="handleFormatResponse" text><LayoutIcon name="action.beautify" /><span>格式化</span></el-button>
            </el-space>
          </div>
          <el-descriptions :column="1" border v-if="responseHeaderVisible && responseResult.headers && Object.keys(responseResult.headers).length">
            <el-descriptions-item v-for="(value, key) in responseResult.headers" :key="key" :label="String(key)">{{ value }}</el-descriptions-item>
          </el-descriptions>
          <code-editor v-model="responseBody" mode="javascript" :height="300" resizable fold-gutter />
        </template>
        <el-empty v-else description="暂无响应结果，请先发送请求" />
      </el-tab-pane>
    </el-tabs>
  </el-drawer>
</template>

<style lang="scss" scoped>
.el-descriptions {
  margin-bottom: 15px;
}
</style>
