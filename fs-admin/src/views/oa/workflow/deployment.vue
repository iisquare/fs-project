<script setup lang="ts">
import { onMounted, ref } from 'vue'
import type { FormInstance, TableInstance } from 'element-plus'
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router'
import TableUtil from '@/utils/TableUtil'
import DateUtil from '@/utils/DateUtil'
import WorkflowApi from '@/api/oa/WorkflowApi'

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'key', label: '标识' },
  { prop: 'name', label: '名称' },
  { prop: 'deploymentTime', label: '部署时间', formatter: DateUtil.render },
])
const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, {}))
const pagination = ref(RouteUtil.pagination(filters.value))
const infoVisible = ref(false)
const infoRow = ref<any>({})

const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  WorkflowApi.searchDeployment(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

onMounted(() => {
  handleRefresh(false, true)
})

const handleShow = (scope: any) => {
  infoRow.value = Object.assign({}, scope.row, {
    description: scope.row.description ? scope.row.description : '暂无'
  })
  infoVisible.value = true
}

const handleRemove = (scope: any, cascade: boolean) => {
  const content = cascade ? '确认级联删除该部署及其流程实例吗？' : '确认删除所选记录吗？'
  TableUtil.confirm(content).then(() => {
    loading.value = true
    WorkflowApi.deleteDeployment({ id: scope.row.id, cascade }, { success: true }).then(() => {
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
      <form-search-item label="标识" prop="key">
        <el-input v-model="filters.key" clearable />
      </form-search-item>
      <form-search-item label="名称" prop="name">
        <el-input v-model="filters.name" clearable />
      </form-search-item>
      <form-search-item>
        <el-button type="primary" @click="handleRefresh(true, false)" :loading="loading">查询</el-button>
        <el-button @click="filterRef?.resetFields()">重置</el-button>
      </form-search-item>
    </form-search>
  </el-card>
  <el-card :bordered="false" shadow="never" class="fs-table-card">
    <div class="fs-table-toolbar flex-end">
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
      <TableColumn :columns="columns" />
      <el-table-column label="操作" width="190">
        <template #default="scope">
          <el-button link @click="() => handleShow(scope)">查看</el-button>
          <el-button link type="danger" v-permit="'oa:workflow:deleteDeployment'" @click="() => handleRemove(scope, false)">删除</el-button>
          <el-button link type="danger" v-permit="'oa:workflow:deleteDeployment'" @click="() => handleRemove(scope, true)">级联删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
  </el-card>

  <el-dialog v-model="infoVisible" :title="`信息查看 - ${infoRow.id}`" width="500">
    <el-descriptions :column="1" border>
      <el-descriptions-item label="部署ID">{{ infoRow.id }}</el-descriptions-item>
      <el-descriptions-item label="父级部署">{{ infoRow.parentDeploymentId }}</el-descriptions-item>
      <el-descriptions-item label="标识">{{ infoRow.key }}</el-descriptions-item>
      <el-descriptions-item label="名称">{{ infoRow.name }}</el-descriptions-item>
      <el-descriptions-item label="分类">{{ infoRow.category }}</el-descriptions-item>
      <el-descriptions-item label="部署时间">{{ DateUtil.format(infoRow.deploymentTime) }}</el-descriptions-item>
      <el-descriptions-item label="描述">{{ infoRow.description }}</el-descriptions-item>
    </el-descriptions>
  </el-dialog>
</template>

<style lang="scss" scoped>
</style>
