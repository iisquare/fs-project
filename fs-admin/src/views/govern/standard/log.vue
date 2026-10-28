<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, TableInstance } from 'element-plus'
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
const filters = ref<any>(RouteUtil.query2filter(route, {
  advanced: false,
  assess: route.query.assess ? Number(route.query.assess) : '',
  page: 1,
  pageSize: 20,
}))
const pagination = ref<any>(RouteUtil.pagination(filters.value))
const filterRef = ref<FormInstance>()
const detailVisible = ref(false)
const current = ref<any>({})
const columns = ref([
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'assess', label: '方案', width: 80 },
  { prop: 'standard', label: '标准路径', minWidth: 200 },
  { prop: 'source', label: '数据源', minWidth: 120 },
  { prop: 'model', label: '数据表', minWidth: 130 },
  { prop: 'code', label: '字段', minWidth: 120 },
  { prop: 'level', label: '等级', width: 90 },
  { prop: 'name', label: '编码差异', width: 90, slot: 'diff' },
  { prop: 'type', label: '类型差异', width: 90, slot: 'diff' },
  { prop: 'size', label: '长度差异', width: 90, slot: 'diff' },
  { prop: 'digit', label: '精度差异', width: 90, slot: 'diff', hide: true },
  { prop: 'nullable', label: '可空差异', width: 90, slot: 'diff', hide: true },
])

const refresh = (filter2query: boolean, keepPage = true) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  if (filter2query) RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  AssessApi.log(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = ApiUtil.data(result)?.rows ?? []
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

const show = (row: any) => {
  let detail = {}
  try { detail = JSON.parse(row.detail || '{}') } catch {}
  current.value = Object.assign({}, row, { detail })
  detailVisible.value = true
}

const remove = () => {
  if (selection.value.length === 0) return
  TableUtil.selection(selection.value).then((ids: any) => {
    loading.value = true
    AssessApi.clear(ids, { success: true }).then(() => refresh(false)).catch(() => {
      loading.value = false
    })
  }).catch(() => {})
}

onMounted(() => refresh(false))
</script>

<template>
  <GovernPageShell title="评估结果" description="查看标准落地检查发现的字段差异，并追溯到具体方案、数据源和数据表">
  <el-card shadow="never" class="search-card">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="落地方案"><el-input v-model="filters.assess" clearable /></form-search-item>
      <form-search-item label="评估标准"><el-input v-model="filters.standard" clearable /></form-search-item>
      <form-search-item label="数据源"><el-input v-model="filters.source" clearable /></form-search-item>
      <form-search-item label="数据表"><el-input v-model="filters.model" clearable /></form-search-item>
      <form-search-item label="字段"><el-input v-model="filters.code" clearable /></form-search-item>
      <form-search-item label="等级"><el-input v-model="filters.level" clearable /></form-search-item>
      <form-search-item>
        <el-button type="primary" :loading="loading" @click="refresh(true, false)">查询</el-button>
        <el-button @click="filterRef?.resetFields()">重置</el-button>
      </form-search-item>
    </form-search>
  </el-card>
  <el-card shadow="never" class="fs-table-card">
    <div class="fs-table-toolbar flex-between">
      <button-delete v-permit="'govern:assess:clear'" :disabled="selection.length === 0" @click="remove" />
      <el-space>
        <button-refresh :loading="loading" @click="refresh(true)" />
        <TableColumnSetting v-model="columns" :table="tableRef" :loading="loading" />
      </el-space>
    </div>
    <el-table ref="tableRef" v-loading="loading" :data="rows" row-key="id" border @selection-change="(value: any[]) => selection = value">
      <el-table-column type="selection" width="45" />
      <TableColumn :columns="columns">
        <template #diff="scope">
          <el-tag :type="scope.row[scope.column.property] ? 'danger' : 'success'" effect="plain">
            {{ scope.row[scope.column.property] ? '异常' : '一致' }}
          </el-tag>
        </template>
      </TableColumn>
      <el-table-column label="操作" width="80"><template #default="{ row }"><el-button link @click="show(row)">详情</el-button></template></el-table-column>
    </el-table>
    <TablePagination v-model="pagination" :loading="loading" @change="refresh(true)" />
  </el-card>

  <el-drawer v-model="detailVisible" title="标准落地差异" size="58%">
    <el-descriptions :column="2" border>
      <el-descriptions-item label="评估方案">{{ current.assess }}</el-descriptions-item>
      <el-descriptions-item label="检测时间">{{ current.checkTime ? new Date(Number(current.checkTime)).toLocaleString('zh-CN', { hour12: false }) : '暂无' }}</el-descriptions-item>
      <el-descriptions-item label="标准路径" :span="2">{{ current.standard }}</el-descriptions-item>
      <el-descriptions-item label="数据源">{{ current.source }}</el-descriptions-item>
      <el-descriptions-item label="数据表">{{ current.model }}</el-descriptions-item>
      <el-descriptions-item label="字段">{{ current.code }}</el-descriptions-item>
      <el-descriptions-item label="预警等级">{{ current.level }}</el-descriptions-item>
    </el-descriptions>
    <LayoutHeading title="字段属性对比" class="mt-20" />
    <el-table :data="[
      { name: '字段名称', standard: current.detail?.standardName, source: current.detail?.sourceName, result: current.name },
      { name: '字段类型', standard: current.detail?.standardType, source: current.detail?.sourceType, result: current.type },
      { name: '字段长度', standard: current.detail?.standardSize, source: current.detail?.sourceSize, result: current.size },
      { name: '小数位数', standard: current.detail?.standardDigit, source: current.detail?.sourceDigit, result: current.digit },
      { name: '允许为空', standard: current.detail?.standardNullable, source: current.detail?.sourceNullable, result: current.nullable },
    ]" border>
      <el-table-column prop="name" label="属性" />
      <el-table-column prop="standard" label="数据标准" />
      <el-table-column prop="source" label="数据源字段" />
      <el-table-column label="对比" width="90">
        <template #default="{ row }"><el-tag :type="row.result ? 'danger' : 'success'">{{ row.result ? '异常' : '一致' }}</el-tag></template>
      </el-table-column>
    </el-table>
  </el-drawer>
  </GovernPageShell>
</template>
