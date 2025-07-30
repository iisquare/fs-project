<script setup lang="ts">
import { onMounted, ref } from 'vue';
import type { FormInstance, TableInstance } from 'element-plus';
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router';
import TableUtil from '@/utils/TableUtil';
import CronApi from '@/api/server/CronApi';
import TableRadio from '@/components/Table/TableRadio.vue';
import DateUtil from '@/utils/DateUtil';
import ApiUtil from '@/utils/ApiUtil';

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'id', label: '流程标识' },
  { prop: 'name', label: '流程名称' },
  { prop: 'expression', label: 'Cron表达式' },
  { prop: 'state', label: '调度状态' },
  { prop: 'sort', label: '排序' },
  { prop: 'statusText', label: '流程状态' },
])
const config = ref({
  ready: false,
  status: {},
})
const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, { advanced: false, agentIds: [] }))
const pagination = ref(RouteUtil.pagination(filters.value))
const selection: any = ref(null)
const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  selection.value = null
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  CronApi.flowList(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
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

const handleAdd = (env: Event) => {
  RouteUtil.forward(route, router, env, {
    path: '/server/cron/diagram'
  })
}
const handleEdit = (scope: any, env: Event) => {
  RouteUtil.forward(route, router, env, {
    path: '/server/cron/diagram',
    query: {
      id: scope.row.id
    }
  })
}
const handleLog = (scope: any, env: Event) => {
  RouteUtil.forward(route, router, env, {
    path: '/server/cron/flowLog',
    query: RouteUtil.filter({ flowId: scope.row.id })
  })
}
const handleDelete = () => {
  TableUtil.confirm().then(() => {
    const record: any = rows.value[selection.value]
    loading.value = true
    CronApi.flowDelete({ id: record.id }, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {})
  }).catch(() => {})
}
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="流程标识" prop="id">
        <el-input v-model="filters.id" clearable />
      </form-search-item>
      <form-search-item label="流程名称" prop="name">
        <el-input v-model="filters.name" clearable />
      </form-search-item>
      <form-search-item label="状态" prop="status">
        <el-select v-model="filters.status" placeholder="请选择" clearable>
          <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
        </el-select>
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
        <button-add v-permit="'cron:flow:add'" @click="(e: Event) => handleAdd(e)" />
        <button-delete v-permit="'cron:flow:delete'" :disabled="selection === null" @click="handleDelete" />
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
      :row-key="(record: any) => `${record.group}_${record.name}`"
      :border="true"
      v-loading="loading"
      table-layout="auto"
      @selection-change="(newSelection: any) => selection = newSelection"
    >
      <TableRadio v-model="selection" :value="(scope: any) => scope.$index" width="60px" />
      <el-table-column type="expand">
        <template #default="scope">
          <el-descriptions border label-width="130px">
            <el-descriptions-item label="创建者">{{ scope.row.createdUserInfo?.name }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ DateUtil.format(scope.row.createdTime) }}</el-descriptions-item>
            <el-descriptions-item label="开始时间">{{ DateUtil.format(scope.row.startTime) }}</el-descriptions-item>
            <el-descriptions-item label="修改者">{{ scope.row.updatedUserInfo?.name }}</el-descriptions-item>
            <el-descriptions-item label="修改时间">{{ DateUtil.format(scope.row.updatedTime) }}</el-descriptions-item>
            <el-descriptions-item label="结束时间">{{ DateUtil.format(scope.row.endTime) }}</el-descriptions-item>
            <el-descriptions-item label="上次触发">{{ DateUtil.format(scope.row.previousFireTime) }}</el-descriptions-item>
            <el-descriptions-item label="下次触发">{{ DateUtil.format(scope.row.nextFireTime) }}</el-descriptions-item>
            <el-descriptions-item label="最终触发">{{ DateUtil.format(scope.row.finalFireTime) }}</el-descriptions-item>
            <el-descriptions-item label="描述信息" :span="3">{{ scope.row.description }}</el-descriptions-item>
          </el-descriptions>
        </template>
        </el-table-column>
      <TableColumn :columns="columns">
        <template #role="scope">
          <el-space><el-tag v-for="item in scope.row.roles" :key="item.id">{{ item.name }}</el-tag></el-space>
        </template>
      </TableColumn>
      <el-table-column label="操作" width="150px">
        <template #default="scope">
          <el-space>
            <el-button link @click="(e: any) => handleEdit(scope, e)" v-permit="'cron:flow:modify'">编辑</el-button>
            <el-button link @click="(e: any) => handleLog(scope, e)" v-permit="'cron:flow:'">日志</el-button>
          </el-space>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
  </el-card>
</template>

<style lang="scss" scoped>
</style>
