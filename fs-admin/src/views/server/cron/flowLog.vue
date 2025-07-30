<script setup lang="ts">
import { onMounted, ref } from 'vue';
import type { FormInstance, TableInstance } from 'element-plus';
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router';
import CronApi from '@/api/server/CronApi';
import DateUtil from '@/utils/DateUtil';
import ApiUtil from '@/utils/ApiUtil';
import * as echarts from 'echarts'

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'id', label: '日志标识' },
  { prop: 'flowId', label: '流程标识' },
  { prop: 'flowInfo.name', label: '流程名称' },
  { prop: 'createdTime', label: '开始时间', formatter: DateUtil.render },
  { prop: 'updatedTime', label: '结束时间', formatter: DateUtil.render },
  { prop: 'durationPretty', label: '持续时长' },
  { prop: 'state', label: '运行状态' },
])
const config = ref({
  ready: false,
  status: {},
})
const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, { advanced: false, agentIds: [] }))
const pagination = ref(RouteUtil.pagination(filters.value))
const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  CronApi.flowLogList(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
    handleStatistics()
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
onMounted(() => {
  handleRefresh(false, true)
  CronApi.flowConfig().then((result: any) => {
    Object.assign(config.value, { ready: true }, ApiUtil.data(result))
  })
})
const handleStage = (scope: any, env: Event) => {
  RouteUtil.forward(route, router, env, {
    path: '/server/cron/flowStage',
    query: {
      logId: scope.row.id
    }
  })
}
const statisticsRef = ref()
const statisticsVisible = ref(true)

const handleStatistics = () => {
  const data = rows.value.map((item: any) => {
    return [new Date(item.createdTime), item.duration, item.durationPretty]
  })
  echarts.init(statisticsRef.value).setOption({
    tooltip: {
      trigger: 'item',
      formatter (params: any) {
        return [
          '开始时间: ',
          DateUtil.format(params.data[0]),
          '<br/>',
          params.marker,
          ' ',
          params.data[2]
        ].join('')
      },
      axisPointer: {
        type: 'shadow'
      }
    },
    xAxis: { type: 'time' },
    yAxis: {},
    series: [{ ymbolSize: 20, data, type: 'scatter' }]
  })
}
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="所属流程" prop="flowId">
        <form-select v-model="filters.flowId" :callback="(param: any) => CronApi.flowList(param)" clearable />
      </form-search-item>
      <form-search-item label="运行状态" prop="state">
        <el-input v-model="filters.state" clearable />
      </form-search-item>
      <form-search-item>
        <el-button type="primary" @click="handleRefresh(true, false)">查询</el-button>
        <el-button @click="filterRef?.resetFields()">重置</el-button>
      </form-search-item>
    </form-search>
  </el-card>
  <el-card :bordered="false" shadow="never" class="fs-table-card">
    <div ref="statisticsRef" class="chart" v-show="statisticsVisible"></div>
    <div class="fs-table-toolbar flex-between">
      <el-space>
        <el-button link @click="statisticsVisible = !statisticsVisible">展示/隐藏统计</el-button>
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
      :row-key="(record: any) => record.id"
      :border="true"
      v-loading="loading"
      table-layout="auto"
    >
      <el-table-column type="expand">
        <template #default="scope">
          <el-descriptions border label-width="130px">
            <el-descriptions-item label="并发度">{{ scope.row.concurrent }}</el-descriptions-item>
            <el-descriptions-item label="并发策略">{{ scope.row.concurrency }}</el-descriptions-item>
            <el-descriptions-item label="失败策略">{{ scope.row.failure }}</el-descriptions-item>
          </el-descriptions>
        </template>
        </el-table-column>
      <TableColumn :columns="columns">
        <template #role="scope">
          <el-space><el-tag v-for="item in scope.row.roles" :key="item.id">{{ item.name }}</el-tag></el-space>
        </template>
      </TableColumn>
      <el-table-column label="操作" width="80px">
        <template #default="scope">
          <el-space>
            <el-button link @click="(e: any) => handleStage(scope, e)" type="primary" v-permit="'cron:flow:'">明细</el-button>
          </el-space>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
  </el-card>
</template>

<style lang="scss" scoped>
.chart {
  width: 100%;
  height: 350px;
}
</style>
