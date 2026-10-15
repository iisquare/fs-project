<script setup lang="ts">
import { onMounted, ref } from 'vue';
import type { FormInstance, TableInstance } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import RouteUtil from '@/utils/RouteUtil'
import DiagramApi from '@/api/bi/DiagramApi';
import ClusterApi from '@/api/bi/ClusterApi';
import ApiUtil from '@/utils/ApiUtil';
import DateUtil from '@/utils/DateUtil';
import TableUtil from '@/utils/TableUtil';
import ElementUtil from '@/utils/ElementUtil';

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'name', label: '流程名称' },
  { prop: 'engineText', label: '计算引擎' },
  { prop: 'modelText', label: '处理模式' },
  { prop: 'clusterIdName', label: '计算集群', formatter: (row: any) => row.clusterIdName || '本地执行' },
  { prop: 'expression', label: 'Cron表达式', formatter: (row: any) => row.schedule ? (row.expression || '暂无') : '未启用' },
  { prop: 'sort', label: '排序' },
  { prop: 'statusText', label: '状态' },
  { prop: 'description', label: '描述', hide: true },
  { prop: 'updatedTime', label: '修改时间', hide: true, formatter: DateUtil.render },
])
const config: any = ref({
  ready: false,
  status: {},
  engines: {},
  models: {},
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
  DiagramApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
onMounted(() => {
  handleRefresh(false, true)
  DiagramApi.config().then((result: any) => {
    Object.assign(config.value, { ready: true }, ApiUtil.data(result))
  }).catch(() => {})
})
const handleAdd = () => {
  router.push({ path: '/bi/diagram/model' }).catch(() => {})
}
const handleModel = (scope: any) => {
  router.push({ path: '/bi/diagram/model', query: { id: scope.row.id } }).catch(() => {})
}
const handleRun = (scope: any) => {
  ElementUtil.confirm('确认提交该编排作业？').then(() => {
    loading.value = true
    DiagramApi.run({ id: scope.row.id, clusterId: scope.row.clusterId }, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {}).finally(() => {
      loading.value = false
    })
  }).catch(() => {})
}
const handleMonitor = (scope: any) => {
  router.push({ path: '/bi/diagram/job', query: { diagramId: scope.row.id } }).catch(() => {})
}
const handleDelete = () => {
  TableUtil.selection(selection.value).then((ids: any) => {
    loading.value = true
    DiagramApi.delete(ids, { success: true }).then(() => {
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
      <form-search-item label="名称" prop="name">
        <el-input v-model="filters.name" clearable />
      </form-search-item>
      <form-search-item label="计算引擎" prop="engine">
        <el-select v-model="filters.engine" clearable placeholder="请选择">
          <el-option v-for="(value, key) in config.engines" :key="key" :value="key" :label="value" />
        </el-select>
      </form-search-item>
      <form-search-item label="处理模式" prop="model">
        <el-select v-model="filters.model" clearable placeholder="请选择">
          <el-option v-for="(value, key) in config.models" :key="key" :value="key" :label="value" />
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
        <button-add v-permit="'bi:diagram:add'" @click="handleAdd" />
        <button-delete v-permit="'bi:diagram:delete'" :disabled="selection.length === 0" @click="handleDelete" />
      </el-space>
      <el-space>
        <button-search @click="searchable = !searchable" />
        <button-refresh @click="handleRefresh(true, true)" :loading="loading" />
        <TableColumnSetting v-model="columns" :table="tableRef" :loading="loading" />
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
      <TableColumn :columns="columns" />
      <el-table-column label="操作">
        <template #default="scope">
          <el-button link @click="handleModel(scope)" v-permit="'bi:diagram:'">设计</el-button>
          <el-button link @click="handleRun(scope)" v-permit="'bi:diagram:add'">运行</el-button>
          <el-button link @click="handleMonitor(scope)" v-permit="'bi:diagram:'">作业</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" :loading="loading" @change="handleRefresh(true, true)" />
  </el-card>
</template>

<style lang="scss" scoped>
</style>
