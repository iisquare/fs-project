<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, TableInstance } from 'element-plus'
import QualityPlanApi from '@/api/govern/QualityPlanApi'
import QualityLogicApi from '@/api/govern/QualityLogicApi'
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
const filters = ref<any>(RouteUtil.query2filter(route, {
  advanced: false,
  planId: route.query.planId ? Number(route.query.planId) : '',
  page: 1,
  pageSize: 20,
}))
const pagination = ref<any>(RouteUtil.pagination(filters.value))
const filterRef = ref<FormInstance>()
const logicTree = ref<any[]>([])
const detailVisible = ref(false)
const current = ref<any>({})
const columns = ref([
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'source', label: '数据源', minWidth: 120 },
  { prop: 'logicIdName', label: '分类', minWidth: 120 },
  { prop: 'ruleIdName', label: '规则', minWidth: 150 },
  { prop: 'planIdName', label: '方案', minWidth: 150 },
  { prop: 'checkTable', label: '检查表', minWidth: 130 },
  { prop: 'checkColumn', label: '检查字段', minWidth: 120 },
  { prop: 'checkCount', label: '检查记录', width: 100 },
  { prop: 'hitCount', label: '命中记录', width: 100 },
  { prop: 'state', label: '状态', width: 90, slot: 'state' },
  { prop: 'createdTime', label: '检查时间', minWidth: 165, formatter: (row: any) => new Date(Number(row.createdTime || 0)).toLocaleString('zh-CN', { hour12: false }) },
])

const refresh = (filter2query: boolean, keepPage = true) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  if (filter2query) RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  QualityPlanApi.log(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = ApiUtil.data(result)?.rows ?? []
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

const show = (row: any) => {
  current.value = row
  detailVisible.value = true
}

const remove = () => {
  if (selection.value.length === 0) return
  TableUtil.selection(selection.value).then((ids: any) => {
    loading.value = true
    QualityPlanApi.clear(ids, { success: true }).then(() => refresh(false)).catch(() => {
      loading.value = false
    })
  }).catch(() => {})
}

onMounted(() => {
  refresh(false)
  QualityLogicApi.tree({}, { warning: false }).then((result: any) => {
    logicTree.value = ApiUtil.data(result) ?? []
  }).catch(() => {})
})
</script>

<template>
  <GovernPageShell title="质检报告" description="检索历史质检记录，查看检查数量、命中数量及实际执行的检查逻辑">
  <el-card shadow="never" class="search-card">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="所属分类">
        <el-tree-select v-model="filters.logicId" :data="logicTree" node-key="id" :props="{ label: 'name', children: 'children' }" clearable check-strictly default-expand-all />
      </form-search-item>
      <form-search-item label="所属规则"><el-input v-model="filters.ruleId" clearable placeholder="规则 ID" /></form-search-item>
      <form-search-item label="所属方案"><el-input v-model="filters.planId" clearable placeholder="方案 ID" /></form-search-item>
      <form-search-item label="数据源"><el-input v-model="filters.source" clearable /></form-search-item>
      <form-search-item label="检查表名"><el-input v-model="filters.checkTable" clearable /></form-search-item>
      <form-search-item label="检查字段"><el-input v-model="filters.checkColumn" clearable /></form-search-item>
      <form-search-item label="检查状态">
        <el-select v-model="filters.state" clearable>
          <el-option label="成功" :value="0" />
          <el-option label="失败" :value="5002" />
        </el-select>
      </form-search-item>
      <form-search-item>
        <el-button type="primary" :loading="loading" @click="refresh(true, false)">查询</el-button>
        <el-button @click="filterRef?.resetFields()">重置</el-button>
      </form-search-item>
    </form-search>
  </el-card>
  <el-card shadow="never" class="fs-table-card">
    <div class="fs-table-toolbar flex-between">
      <button-delete v-permit="'govern:qualityPlan:clear'" :disabled="selection.length === 0" @click="remove" />
      <el-space>
        <button-refresh :loading="loading" @click="refresh(true)" />
        <TableColumnSetting v-model="columns" :table="tableRef" :loading="loading" />
      </el-space>
    </div>
    <el-table ref="tableRef" v-loading="loading" :data="rows" row-key="id" border @selection-change="(value: any[]) => selection = value">
      <el-table-column type="selection" width="45" />
      <TableColumn :columns="columns">
        <template #state="scope">
          <el-tag :type="scope.row.state === 0 ? 'success' : 'danger'" effect="plain">
            {{ scope.row.state === 0 ? '成功' : '失败' }}
          </el-tag>
        </template>
      </TableColumn>
      <el-table-column label="操作" width="80" fixed="right"><template #default="{ row }"><el-button link @click="show(row)">详情</el-button></template></el-table-column>
    </el-table>
    <TablePagination v-model="pagination" :loading="loading" @change="refresh(true)" />
  </el-card>

  <el-drawer v-model="detailVisible" title="质检报告详情" size="68%">
    <el-descriptions :column="3" border>
      <el-descriptions-item label="日志标识">{{ current.id }}</el-descriptions-item>
      <el-descriptions-item label="检查状态">
        <el-tag :type="current.state === 0 ? 'success' : 'danger'">{{ current.state === 0 ? '成功' : '失败' }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="检查时间">{{ current.createdTime ? new Date(Number(current.createdTime)).toLocaleString('zh-CN', { hour12: false }) : '暂无' }}</el-descriptions-item>
      <el-descriptions-item label="所属方案">{{ current.planIdName }}（{{ current.planId }}）</el-descriptions-item>
      <el-descriptions-item label="所属规则">{{ current.ruleIdName }}（{{ current.ruleId }}）</el-descriptions-item>
      <el-descriptions-item label="所属分类">{{ current.logicIdName }}（{{ current.logicId }}）</el-descriptions-item>
      <el-descriptions-item label="数据源">{{ current.source }}</el-descriptions-item>
      <el-descriptions-item label="检查表">{{ current.checkTable }}</el-descriptions-item>
      <el-descriptions-item label="检查字段">{{ current.checkColumn }}</el-descriptions-item>
      <el-descriptions-item label="检查记录">{{ current.checkCount }}</el-descriptions-item>
      <el-descriptions-item label="命中记录">{{ current.hitCount }}</el-descriptions-item>
      <el-descriptions-item label="整改原因">{{ current.reason || '暂无' }}</el-descriptions-item>
    </el-descriptions>
    <LayoutHeading title="检查逻辑" class="mt-20" />
    <el-input :model-value="current.expression || '暂无检查逻辑记录'" type="textarea" :rows="10" readonly />
  </el-drawer>
  </GovernPageShell>
</template>
