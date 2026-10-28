<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, TableInstance } from 'element-plus'
import { VideoPlay } from '@element-plus/icons-vue'
import AssessApi from '@/api/govern/AssessApi'
import ApiUtil from '@/utils/ApiUtil'
import RouteUtil from '@/utils/RouteUtil'
import TableUtil from '@/utils/TableUtil'
import GovernPageShell from '@/views/govern/components/GovernPageShell.vue'

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const rows = ref<any[]>([])
const selection = ref<any[]>([])
const filters = ref<any>(RouteUtil.query2filter(route, { advanced: false, page: 1, pageSize: 15 }))
const pagination = ref<any>(RouteUtil.pagination(filters.value))
const filterRef = ref<FormInstance>()
const config = ref<any>({ status: {} })
const columns = ref([
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'name', label: '方案名称', minWidth: 180 },
  { prop: 'source', label: '数据源编码', minWidth: 150 },
  { prop: 'sort', label: '排序', width: 80 },
  { prop: 'statusText', label: '状态', width: 90, slot: 'status' },
  { prop: 'updatedUidName', label: '修改者', width: 110 },
])

const refresh = (filter2query: boolean, keepPage = true) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  if (filter2query) RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  AssessApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = ApiUtil.data(result)?.rows ?? []
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

const edit = (row?: any) => {
  router.push({ path: '/govern/standard/model', query: row?.id ? { id: row.id } : {} }).catch(() => {})
}

const run = (row: any) => {
  TableUtil.confirm(`确认执行评估方案“${row.name}”？`).then(() => {
    loading.value = true
    AssessApi.check({ id: row.id }, { success: true }).then(() => {
      refresh(false)
    }).catch(() => {}).finally(() => {
      loading.value = false
    })
  }).catch(() => {})
}

const remove = () => {
  if (selection.value.length === 0) return
  TableUtil.selection(selection.value).then((ids: any) => {
    loading.value = true
    AssessApi.delete(ids, { success: true }).then(() => refresh(false)).catch(() => {
      loading.value = false
    })
  }).catch(() => {})
}

onMounted(() => {
  refresh(false)
  AssessApi.config({ warning: false }).then((result: any) => {
    config.value = ApiUtil.data(result) ?? config.value
  }).catch(() => {})
})
</script>

<template>
  <GovernPageShell title="落地评估" description="将已启用数据标准映射到数据源，自动比对字段类型、长度、精度和可空属性">
  <el-card shadow="never" class="search-card">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="方案名称" prop="name"><el-input v-model="filters.name" clearable /></form-search-item>
      <form-search-item label="数据源" prop="source"><el-input v-model="filters.source" clearable /></form-search-item>
      <form-search-item>
        <el-button type="primary" :loading="loading" @click="refresh(true, false)">查询</el-button>
        <el-button @click="filterRef?.resetFields()">重置</el-button>
      </form-search-item>
    </form-search>
  </el-card>
  <el-card shadow="never" class="fs-table-card">
    <div class="fs-table-toolbar flex-between">
      <el-space>
        <button-add v-permit="'govern:assess:add'" @click="edit()" />
        <button-delete v-permit="'govern:assess:delete'" :disabled="selection.length === 0" @click="remove" />
      </el-space>
      <el-space>
        <button-refresh :loading="loading" @click="refresh(true)" />
        <TableColumnSetting v-model="columns" :table="tableRef" :loading="loading" />
      </el-space>
    </div>
    <el-table ref="tableRef" v-loading="loading" :data="rows" row-key="id" border @selection-change="(value: any[]) => selection = value">
      <el-table-column type="selection" width="45" />
      <TableColumn :columns="columns">
        <template #status="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" effect="plain">
            {{ scope.row.statusText || config.status?.[scope.row.status] }}
          </el-tag>
        </template>
      </TableColumn>
      <el-table-column label="操作" width="190" fixed="right">
        <template #default="{ row }">
          <el-button link :icon="VideoPlay" :disabled="row.status !== 1" @click="run(row)">执行</el-button>
          <el-button link @click="router.push({ path: '/govern/standard/log', query: { assess: row.id } })">结果</el-button>
          <el-button link v-permit="'govern:assess:modify'" @click="edit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" :loading="loading" @change="refresh(true)" />
  </el-card>
  </GovernPageShell>
</template>
