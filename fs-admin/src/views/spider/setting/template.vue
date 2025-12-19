<script setup lang="ts">
import { onMounted, ref } from 'vue';
import type { FormInstance, TableInstance } from 'element-plus';
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router';
import TemplateApi from '@/api/spider/TemplateApi';
import ApiUtil from '@/utils/ApiUtil';
import DateUtil from '@/utils/DateUtil';
import TableUtil from '@/utils/TableUtil';
import SpiderPanParams from '@/designer/Spider/SpiderPanParams.vue';
import SpiderSite from '@/designer/Spider/SpiderSite.vue';
import SpiderWhitelist from '@/designer/Spider/SpiderWhitelist.vue';
import SpiderBlacklist from '@/designer/Spider/SpiderBlacklist.vue';
import RateApi from '@/api/spider/RateApi';
import ElementUtil from '@/utils/ElementUtil';

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'name', label: '模板名称' },
  { prop: 'typeText', label: '采集方式' },
  { prop: 'maxThreads', label: '最大线程' },
  { prop: 'rateInfo.name', label: '请求频率' },
  { prop: 'rateInfo.parallelByKey', label: '并行度' },
  { prop: 'rateInfo.concurrent', label: '并发数' },
  { prop: 'rateInfo.intervalMillisecond', label: '并发间隔' },
  { prop: 'priority', label: '优先级', hide: true },
  { prop: 'minHalt', label: '最小停顿间隔', hide: true },
  { prop: 'maxHalt', label: '最大停顿间隔', hide: true },
  { prop: 'description', label: '描述', hide: true },
  { prop: 'sort', label: '排序' },
  { prop: 'statusText', label: '状态', hide: true  },
  { prop: 'publishedTime', label: '发布时间', formatter: DateUtil.render },
  { prop: 'createdTime', label: '创建时间', formatter: DateUtil.render, hide: true },
  { prop: 'updatedTime', label: '修改时间', formatter: DateUtil.render, hide: true },
])
const config = ref({
  ready: false,
  status: {},
  types: {},
})
const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, { advanced: false }))
const pagination = ref(RouteUtil.pagination(filters.value))
const selection = ref([])
const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  TemplateApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
onMounted(() => {
  handleRefresh(false, true)
  TemplateApi.config().then((result: any) => {
    Object.assign(config.value, { ready: true }, ApiUtil.data(result))
  }).catch(() => {})
})
const infoVisible = ref(false)
const infoLoading = ref(false)
const active = ref('params')
const formVisible = ref(false)
const formLoading = ref(false)
const form: any = ref({})
const formRef: any = ref<FormInstance>()
const rules = ref({
  name: [{ required: true, message: '请输入服务名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择采集方式', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
})
const handleAdd = () => {
  form.value = {
    status: '1',
    params: {},
  }
  active.value = 'params'
  formVisible.value = true
}
const handleShow = (scope: any) => {
  form.value = Object.assign({}, scope.row)
  active.value = 'params'
  infoVisible.value = true
}
const handleReload = () => {
  infoLoading.value = true
  TemplateApi.info({ id: form.value.id }).then(result => {
    Object.assign(form.value, ApiUtil.data(result))
  }).catch(() => {}).finally(() => {
    infoLoading.value = false
  })
}
const handleEdit = (scope: any) => {
  form.value = Object.assign({}, scope.row, {
    status: scope.row.status + '',
  })
  active.value = 'params'
  formVisible.value = true
}
const handlePublish = (scope: any) => {
  TemplateApi.publish(scope.row, { success: true }).then(result => {
    handleRefresh(false, true)
  }).catch(() => {})
}
const handleSubmit = () => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid || formLoading.value) return
    formLoading.value = true
    TemplateApi.save(form.value, { success: true }).then(result => {
      handleRefresh(false, true)
    }).catch(() => {}).finally(() => {
      formLoading.value = false
    })
  })
}
const handleDelete = () => {
  TableUtil.selection(selection.value).then((ids: any) => {
    loading.value = true
    TemplateApi.delete(ids, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {})
  }).catch(() => {})
}
const handleClear = (scope: any) => {
  ElementUtil.confirm(`确定清理'${scope.row.name}'配置？`).then(() => {
    TemplateApi.clear({ id: scope.row.id }, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {})
  }).catch(() => {})
}
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="模板名称" prop="name">
        <el-input v-model="filters.name" clearable />
      </form-search-item>
      <form-search-item label="采集方式" prop="type">
        <el-select v-model="filters.type" placeholder="请选择" clearable>
          <el-option v-for="(value, key) in config.types" :key="key" :value="key" :label="value" />
        </el-select>
      </form-search-item>
      <form-search-item label="请求频率" prop="rateId">
        <form-select v-model="filters.rateId" :callback="RateApi.list" clearable />
      </form-search-item>
      <form-search-item>
        <el-button type="primary" @click="handleRefresh(true, false)">查询</el-button>
        <el-button @click="filterRef?.resetFields()">重置</el-button>
      </form-search-item>
    </form-search>
  </el-card>
  <el-card :bordered="false" shadow="never" class="fs-table-card">
    <div class="fs-table-toolbar flex-between">
      <el-space>
        <button-add v-permit="'spider:template:add'" @click="handleAdd" />
        <button-delete v-permit="'spider:template:delete'" :disabled="selection.length === 0" @click="handleDelete" />
      </el-space>
      <el-space>
        <button-search @click="searchable = !searchable" />
        <button-refresh @click="handleRefresh(true, true)" :loading="loading" />
        <TableColumnSetting v-model="columns" :table="tableRef" />
      </el-space>
    </div>
    <el-table
      ref="tableRef"
      :data="rows"
      :row-key="record => record.id"
      :border="true"
      v-loading="loading"
      table-layout="auto"
      @selection-change="newSelection => selection = newSelection"
    >
      <el-table-column type="selection" />
      <TableColumn :columns="columns">
      </TableColumn>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button link @click="handlePublish(scope)" :disabled="scope.row.status !== 1">发布</el-button>
          <el-button link @click="handleClear(scope)" :disabled="scope.row.status !== 1">清理</el-button>
          <el-button link @click="handleEdit(scope)" v-permit="'spider:template:modify'">配置</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
  </el-card>
  <el-drawer v-model="formVisible" :close-on-click-modal="false" :show-close="false" :destroy-on-close="true" size="80%">
    <template #header="{ close, titleId, titleClass }">
      <h4 :id="titleId" :class="titleClass">模板配置</h4>
      <el-space>
        <el-button type="primary" @click="handleSubmit" :loading="formLoading">保存</el-button>
        <el-button @click="close">关闭</el-button>
      </el-space>
    </template>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="auto">
      <el-descriptions border>
        <el-descriptions-item label="模板名称"><el-input v-model="form.name" /></el-descriptions-item>
        <el-descriptions-item label="采集方式" v-if="form.id">{{ form.typeText }}</el-descriptions-item>
        <el-descriptions-item label="采集方式" v-else>
          <el-select v-model="form.type" placeholder="请选择">
            <el-option v-for="(value, key) in config.types" :key="key" :value="key" :label="value" />
          </el-select>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-select v-model="form.status" placeholder="请选择">
            <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
          </el-select>
        </el-descriptions-item>
        <el-descriptions-item label="最大线程"><form-input-number v-model="form.maxThreads" /></el-descriptions-item>
        <el-descriptions-item label="请求频率"><form-select v-model="form.rateId" :callback="RateApi.list" clearable /></el-descriptions-item>
        <el-descriptions-item label="排序"><form-input-number v-model="form.sort" /></el-descriptions-item>
        <el-descriptions-item label="优先级"><form-input-number v-model="form.priority" /></el-descriptions-item>
        <el-descriptions-item label="最小停顿间隔">
          <el-space>
            <form-input-number v-model="form.minHalt" />
            <span>毫秒</span>
          </el-space>
        </el-descriptions-item>
        <el-descriptions-item label="最大停顿间隔">
          <el-space>
            <form-input-number v-model="form.maxHalt" />
            <span>毫秒</span>
          </el-space>
        </el-descriptions-item>
        <el-descriptions-item label="描述" :span="3"><el-input type="textarea" v-model="form.description" /></el-descriptions-item>
      </el-descriptions>
    </el-form>  
    <el-tabs v-model="active">
      <el-tab-pane label="运行参数" name="params">
        <SpiderPanParams v-model="form.params" />
      </el-tab-pane>
      <el-tab-pane label="渠道站点" name="site">
        <SpiderSite v-model="form" />
      </el-tab-pane>
      <el-tab-pane label="白名单" name="whitelist">
        <SpiderWhitelist v-model="form" />
      </el-tab-pane>
      <el-tab-pane label="黑名单" name="blacklist">
        <SpiderBlacklist v-model="form" />
      </el-tab-pane>
    </el-tabs>
  </el-drawer>
</template>

<style lang="scss" scoped>
</style>
