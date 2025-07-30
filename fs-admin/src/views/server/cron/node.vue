<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import type { TableInstance } from 'element-plus';
import CronApi from '@/api/server/CronApi';
import LayoutIcon from '@/components/Layout/LayoutIcon.vue';
import TableRadio from '@/components/Table/TableRadio.vue';

const tableRef = ref<TableInstance>()
const loading = ref(false)

const columns = ref([
  { prop: 'id', label: '节点', slot: 'id' },
  { prop: 'state', label: '状态' },
  { prop: 'command', label: '指令' },
  { prop: 'scheduler.id', label: '实例' },
  { prop: 'scheduler.name', label: '集群' },
  { prop: 'scheduler', label: '调度器', slot: 'scheduler' },
])

const nodes = ref({})
const rows = computed(() => Object.values(nodes.value))
const selection = ref()
const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  selection.value = ''
  loading.value = true
  CronApi.nodeStats().then((result: any) => {
    for (const id in result.data.nodes) {
      result.data.nodes[id].command = result.data.commands[id]
    }
    nodes.value = result.data.nodes
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

const modeForce = ref(false)
const handleStandby = (bAll: Boolean) => {
  loading.value = true
  const nodeId = bAll ? '' : selection.value
  CronApi.nodeStandby({ nodeId }).then(() => {
    handleRefresh(false, true)
  }).catch(() => {})
}
const handleRestart = (bAll: Boolean) => {
  loading.value = true
  const nodeId = bAll ? '' : selection.value
  CronApi.nodeRestart({ nodeId, modeForce: modeForce.value }).then(() => {
    handleRefresh(false, true)
  }).catch(() => {})
}
const handleShutdown = (bAll: Boolean) => {
  loading.value = true
  const nodeId = bAll ? '' : selection.value
  CronApi.nodeShutdown({ nodeId, modeForce: modeForce.value }).then(() => {
    handleRefresh(false, true)
  }).catch(() => {})
}

onMounted(() => {
  handleRefresh(false, true)
})
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-card pb-20">
    <div class="fs-table-toolbar flex-between">
      <el-space>
        <el-button @click="handleRestart(false)" :loading="loading" :disabled="!selection">重新启动</el-button>
        <el-button @click="handleStandby(false)" :loading="loading" :disabled="!selection">待机等候</el-button>
        <el-button @click="handleShutdown(false)" :loading="loading" :disabled="!selection">终止运行</el-button>
        <el-button @click="handleRestart(true)" :loading="loading">全部重启</el-button>
        <el-button @click="handleStandby(true)" :loading="loading">全部待机</el-button>
        <el-button @click="handleShutdown(true)" :loading="loading">全部终止</el-button>
        <el-checkbox v-model="modeForce">强制模式，忽略在执行作业</el-checkbox>
      </el-space>
      <el-space>
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
      @selection-change="(newSelection: any) => selection = newSelection"
    >
      <TableRadio v-model="selection" :value="(scope: any) => scope.row.id" />
      <TableColumn :columns="columns">
        <template #id="scope">
          <div class="flex-start">
            <LayoutIcon :name="scope.row.leadership ? 'StarFilled' : 'Star'" />
            <span> - {{ scope.row.id }}</span>
          </div>
        </template>
        <template #scheduler="scope">
          <el-space>
            <el-tag type="warning" v-if="scope.row.scheduler.standby">standby</el-tag>
            <el-tag type="success" v-if="scope.row.scheduler.started">started</el-tag>
            <el-tag type="danger" v-if="scope.row.scheduler.shutdown">shutdown</el-tag>
          </el-space>
        </template>
      </TableColumn>
    </el-table>
  </el-card>
</template>

<style lang="scss" scoped>
</style>
