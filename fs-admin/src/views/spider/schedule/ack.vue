<script setup lang="ts">
import { onMounted, ref } from 'vue';
import type { TableInstance } from 'element-plus';
import NodeApi from '@/api/spider/NodeApi';
import DateUtil from '@/utils/DateUtil';
import ElementUtil from '@/utils/ElementUtil';

const tableRef = ref<TableInstance>()
const loading = ref(false)
const columns = ref([
  { prop: 'nodeId', label: '节点标识' },
  { prop: 'token.id', label: '令牌标识' },
  { prop: 'token.jobId', label: '作业标识' },
  { prop: 'task.id', label: '任务标识' },
  { prop: 'task.url', label: '请求链接' },
  { prop: 'time', label: '调度时间', formatter: DateUtil.render },
])

const rows = ref([])
const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  loading.value = true
  NodeApi.ack({}).then((result: any) => {
    rows.value = Object.values(result.data)
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
onMounted(() => {
  handleRefresh(false, true)
})

const handleDiscard = (scope: any) => {
  ElementUtil.confirm(`确定丢弃ACK-${scope.row.token.id}？`).then(() => {
    NodeApi.discardAck(scope.row, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {})
  }).catch(() => {})
}
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-card pb-20">
    <div class="fs-table-toolbar flex-between">
      <el-space>
        <h5>确认队列 - 数量：{{ rows.length }}</h5>
      </el-space>
      <el-space>
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
    >
      <TableColumn :columns="columns">
      </TableColumn>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button link @click="handleDiscard(scope)">丢弃</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<style lang="scss" scoped>
</style>
