<script setup lang="ts">
import { onMounted, ref } from 'vue';
import type { FormInstance, TableInstance } from 'element-plus';
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router';
import CronApi from '@/api/server/CronApi';
import ApiUtil from '@/utils/ApiUtil';
import DateUtil from '@/utils/DateUtil';
import TableUtil from '@/utils/TableUtil';

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'schedule', label: '调度实例', hide: true },
  { prop: 'app', label: '目标应用' },
  { prop: 'uri', label: '目标路径', hide: true },
  { prop: 'jobGroup', label: '作业分组', hide: true },
  { prop: 'jobName', label: '作业名称' },
  { prop: 'triggerGroup', label: '触发器分组', hide: true },
  { prop: 'triggerName', label: '触发器名称', hide: true },
  { prop: 'requestHeaders', label: '请求头', hide: true },
  { prop: 'requestBody', label: '请求体', hide: true },
  { prop: 'responseHeaders', label: '响应头', hide: true },
  { prop: 'status', label: '状态码' },
  { prop: 'responseBody', label: '响应体', hide: true },
  { prop: 'state', label: '调用状态' },
  { prop: 'message', label: '状态描述', hide: true },
  { prop: 'requestTime', label: '请求时间', formatter: DateUtil.render },
  { prop: 'responseTime', label: '响应时间', formatter: DateUtil.render, hide: true },
  { prop: 'duration', label: '耗时(ms)' },
])

const config = ref({
  ready: false,
  states: {},
  sorts: {},
})
const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, { advanced: false }))
const pagination = ref(RouteUtil.pagination(filters.value))
const selection: any = ref([])

const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage), {
    columns: TableUtil.columns2query(columns.value)
  })
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  CronApi.rpcLogList(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

onMounted(() => {
  handleRefresh(false, true)
  CronApi.rpcLogConfig().then((result: any) => {
    Object.assign(config.value, { ready: true }, ApiUtil.data(result))
  })
})

const formVisible = ref(false)
const formLoading = ref(false)
const form: any = ref({})

const loadInfo = () => {
  formLoading.value = true
  CronApi.rpcLogInfo({ id: form.value.id }).then((result: any) => {
    Object.assign(form.value, ApiUtil.data(result) || {})
  }).catch(() => {}).finally(() => {
    formLoading.value = false
  })
}

const handleShow = (scope: any) => {
  form.value = Object.assign({}, scope.row)
  formVisible.value = true
  loadInfo()
}

const handleDelete = () => {
  TableUtil.selection(selection.value).then((ids: any) => {
    loading.value = true
    CronApi.rpcLogDelete(ids, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {
      loading.value = false
    })
  }).catch(() => {})
}
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="目标应用" prop="app">
        <el-input v-model="filters.app" clearable />
      </form-search-item>
      <form-search-item label="作业名称" prop="jobName">
        <el-input v-model="filters.jobName" clearable />
      </form-search-item>
      <form-search-item label="调用状态" prop="state">
        <el-select v-model="filters.state" clearable placeholder="请选择">
          <el-option v-for="(value, key) in config.states" :key="key" :value="key" :label="value" />
        </el-select>
      </form-search-item>
      <form-search-item>
        <el-button type="primary" @click="handleRefresh(true, false)" :loading="loading">查询</el-button>
        <el-button @click="filterRef?.resetFields()">重置</el-button>
        <button-advanced v-model="filters.advanced" />
      </form-search-item>
      <template v-if="filters.advanced">
        <form-search-item label="目标路径" prop="uri">
          <el-input v-model="filters.uri" clearable />
        </form-search-item>
        <form-search-item label="作业分组" prop="jobGroup">
          <el-input v-model="filters.jobGroup" clearable />
        </form-search-item>
        <form-search-item label="调度实例" prop="schedule">
          <el-input v-model="filters.schedule" clearable />
        </form-search-item>
        <form-search-item label="日志ID" prop="id">
          <el-input v-model="filters.id" clearable />
        </form-search-item>
        <form-search-item label="请求开始时间" prop="requestTimeBegin">
          <form-date-picker v-model="filters.requestTimeBegin" placeholder="开始时间" />
        </form-search-item>
        <form-search-item label="请求结束时间" prop="requestTimeEnd">
          <form-date-picker v-model="filters.requestTimeEnd" placeholder="结束时间" />
        </form-search-item>
      </template>
    </form-search>
  </el-card>

  <el-card :bordered="false" shadow="never" class="fs-table-card">
    <div class="fs-table-toolbar flex-between">
      <el-space>
        <button-delete v-permit="'cron:rpcLog:delete'" :disabled="selection.length === 0" @click="handleDelete" />
      </el-space>
      <el-space>
        <button-search @click="searchable = !searchable" />
        <button-refresh @click="handleRefresh(true, true)" :loading="loading" />
        <TableColumnSetting v-model="columns" :table="tableRef" @change="handleRefresh(true, true)" />
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
      <TableColumn :columns="columns"></TableColumn>
      <el-table-column label="操作" width="80px">
        <template #default="scope">
          <el-button link @click="handleShow(scope)" v-permit="'cron::'">查看</el-button>
        </template>
      </el-table-column>
    </el-table>

    <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
  </el-card>

  <el-drawer v-model="formVisible" :close-on-click-modal="false" :show-close="false" :destroy-on-close="true" size="80%">
    <template #header="{ close, titleId, titleClass }">
      <h4 :id="titleId" :class="titleClass">调用日志 - {{ form.id }}</h4>
      <el-space>
        <el-button @click="close">取消</el-button>
      </el-space>
    </template>

    <el-form ref="formRef" :model="form" label-width="auto" v-loading="formLoading">
      <el-descriptions border>
        <el-descriptions-item label="ID">{{ form.id }}</el-descriptions-item>
        <el-descriptions-item label="调度实例">{{ form.schedule }}</el-descriptions-item>
        <el-descriptions-item label="目标应用">{{ form.app }}</el-descriptions-item>
        <el-descriptions-item label="作业名称">{{ form.jobName }}</el-descriptions-item>
        <el-descriptions-item label="作业分组">{{ form.jobGroup }}</el-descriptions-item>
        <el-descriptions-item label="状态码">{{ form.status }}</el-descriptions-item>
        <el-descriptions-item label="触发器名称">{{ form.triggerName }}</el-descriptions-item>
        <el-descriptions-item label="触发器分组">{{ form.triggerGroup }}</el-descriptions-item>
        <el-descriptions-item label="调用状态">{{ form.state }}</el-descriptions-item>
        <el-descriptions-item label="请求时间">{{ DateUtil.format(form.requestTime) }}</el-descriptions-item>
        <el-descriptions-item label="响应时间">{{ DateUtil.format(form.responseTime) }}</el-descriptions-item>
        <el-descriptions-item label="耗时(ms)">{{ form.duration }}</el-descriptions-item>
        <el-descriptions-item label="目标路径" :span="3">{{ form.uri }}</el-descriptions-item>
        <el-descriptions-item label="请求头" :span="3">
          <el-input type="textarea" v-model="form.requestHeaders" :rows="4" />
        </el-descriptions-item>
        <el-descriptions-item label="请求体" :span="3">
          <el-input type="textarea" v-model="form.requestBody" :rows="8" />
        </el-descriptions-item>
        <el-descriptions-item label="响应头" :span="3">
          <el-input type="textarea" v-model="form.responseHeaders" :rows="4" />
        </el-descriptions-item>
        <el-descriptions-item label="响应体" :span="3">
          <el-input type="textarea" v-model="form.responseBody" :rows="12" />
        </el-descriptions-item>
        <el-descriptions-item label="状态描述" :span="3">
          <el-input type="textarea" v-model="form.message" :rows="4" />
        </el-descriptions-item>
      </el-descriptions>
    </el-form>
  </el-drawer>
</template>

<style lang="scss" scoped>
</style>
