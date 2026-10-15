<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import type { FormInstance, TableInstance } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import RouteUtil from '@/utils/RouteUtil'
import JobApi from '@/api/bi/JobApi';
import DiagramApi from '@/api/bi/DiagramApi';
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
  { prop: 'name', label: '作业名称' },
  { prop: 'engineText', label: '计算引擎' },
  { prop: 'modelText', label: '处理模式' },
  { prop: 'triggerTypeText', label: '触发方式' },
  { prop: 'stateText', label: '运行状态', slot: 'stateText' },
  { prop: 'jobId', label: '远程标识', hide: true },
  { prop: 'retry', label: '恢复次数' },
  { prop: 'startTime', label: '开始时间', formatter: DateUtil.render },
  { prop: 'endTime', label: '结束时间', formatter: (row: any) => row.endTime ? DateUtil.format(row.endTime) : '运行中' },
  { prop: 'alarm', label: '预警信息', hide: true },
])
const config: any = ref({
  ready: false,
  states: {},
  triggers: {},
  engines: {},
  models: {},
})
const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, { advanced: false, diagramId: undefined, state: undefined }))
const pagination = ref(RouteUtil.pagination(filters.value))
const selection: any = ref([])
const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  JobApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
onMounted(() => {
  handleRefresh(false, true)
  JobApi.config().then((result: any) => {
    Object.assign(config.value, { ready: true }, ApiUtil.data(result))
  }).catch(() => {})
})

const stateType = (state: string) => {
  switch (state) {
    case 'RUNNING': return 'primary'
    case 'FINISHED': return 'success'
    case 'FAILED': return 'danger'
    case 'CANCELLED': return 'info'
    default: return 'warning'
  }
}

const infoVisible = ref(false)
const detail: any = ref({})
const handleShow = (scope: any) => {
  JobApi.info(scope.row.id).then((result: any) => {
    detail.value = ApiUtil.data(result)
    infoVisible.value = true
  }).catch(() => {})
}
const handleCancel = (scope: any) => {
  ElementUtil.confirm('确认取消该作业？').then(() => {
    loading.value = true
    JobApi.cancel({ id: scope.row.id }, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {}).finally(() => {
      loading.value = false
    })
  }).catch(() => {})
}
const handleRestart = (scope: any) => {
  ElementUtil.confirm('确认重新提交该作业？').then(() => {
    loading.value = true
    JobApi.restart({ id: scope.row.id, clusterId: scope.row.clusterId }, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {}).finally(() => {
      loading.value = false
    })
  }).catch(() => {})
}
const handleDelete = () => {
  TableUtil.selection(selection.value).then((ids: any) => {
    loading.value = true
    JobApi.delete(ids, { success: true }).then(() => {
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
      <form-search-item label="作业名称" prop="name">
        <el-input v-model="filters.name" clearable />
      </form-search-item>
      <form-search-item label="编排流程" prop="diagramId">
        <form-select v-model="filters.diagramId" :callback="DiagramApi.list" placeholder="检索选择流程" clearable />
      </form-search-item>
      <form-search-item label="计算引擎" prop="engine">
        <el-select v-model="filters.engine" clearable placeholder="请选择">
          <el-option v-for="(value, key) in config.engines" :key="key" :value="key" :label="value" />
        </el-select>
      </form-search-item>
      <form-search-item label="运行状态" prop="state">
        <el-select v-model="filters.state" clearable placeholder="请选择">
          <el-option v-for="(value, key) in config.states" :key="key" :value="key" :label="value" />
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
        <button-delete v-permit="'bi:job:delete'" :disabled="selection.length === 0" @click="handleDelete" />
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
      <TableColumn :columns="columns">
        <template #stateText="scope">
          <el-tag :type="stateType(scope.row.state)">{{ scope.row.stateText || scope.row.state }}</el-tag>
        </template>
      </TableColumn>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button link @click="handleShow(scope)" v-permit="'bi:job:'">详情</el-button>
          <el-button link @click="handleCancel(scope)" v-permit="'bi:job:'" v-if="['RUNNING', 'SUBMITTING'].includes(scope.row.state)">取消</el-button>
          <el-button link @click="handleRestart(scope)" v-permit="'bi:job:'" v-if="['FAILED', 'CANCELLED', 'FINISHED'].includes(scope.row.state)">重跑</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" :loading="loading" @change="handleRefresh(true, true)" />
  </el-card>
  <el-drawer v-model="infoVisible" :title="'作业详情 - ' + detail.id" size="60%">
    <layout-heading title="基础信息" />
    <el-descriptions :column="2" label-width="120px" border>
      <el-descriptions-item label="作业名称">{{ detail.name }}</el-descriptions-item>
      <el-descriptions-item label="运行状态">{{ detail.stateText || detail.state }}</el-descriptions-item>
      <el-descriptions-item label="计算引擎">{{ detail.engineText || detail.engine }}</el-descriptions-item>
      <el-descriptions-item label="处理模式">{{ detail.modelText || detail.model }}</el-descriptions-item>
      <el-descriptions-item label="触发方式">{{ detail.triggerTypeText || detail.triggerType }}</el-descriptions-item>
      <el-descriptions-item label="远程标识">{{ detail.jobId || '暂无' }}</el-descriptions-item>
      <el-descriptions-item label="恢复次数">{{ detail.retry }}</el-descriptions-item>
      <el-descriptions-item label="开始时间">{{ DateUtil.format(detail.startTime) }}</el-descriptions-item>
      <el-descriptions-item label="结束时间">{{ detail.endTime ? DateUtil.format(detail.endTime) : '运行中' }}</el-descriptions-item>
      <el-descriptions-item label="自动恢复">{{ detail.autoRecover ? '已启用' : '未启用' }}</el-descriptions-item>
      <el-descriptions-item label="预警信息" :span="2">
        <pre class="detail-pre">{{ detail.alarm || '暂无' }}</pre>
      </el-descriptions-item>
      <el-descriptions-item label="提交结果" :span="2">
        <pre class="detail-pre">{{ JSON.stringify(detail.result, null, 2) }}</pre>
      </el-descriptions-item>
      <el-descriptions-item label="调试输出" :span="2" v-if="detail.outputs">
        <pre class="detail-pre">{{ JSON.stringify(detail.outputs, null, 2) }}</pre>
      </el-descriptions-item>
    </el-descriptions>
  </el-drawer>
</template>

<style lang="scss" scoped>
.detail-pre {
  max-height: 320px;
  margin: 0;
  overflow: auto;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
