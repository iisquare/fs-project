<script setup lang="ts">
import { onMounted, ref } from 'vue'
import type { FormInstance, TableInstance } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import DashboardTemplateApi from '@/api/bi/DashboardTemplateApi'
import DateUtil from '@/utils/DateUtil'
import RouteUtil from '@/utils/RouteUtil'
import TableUtil from '@/utils/TableUtil'

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'name', label: '模板名称' },
  { prop: 'layout', label: '默认布局' },
  { prop: 'styleSummary', label: '样式配置' },
  { prop: 'sort', label: '排序' },
  { prop: 'statusText', label: '状态' },
  { prop: 'description', label: '描述', hide: true },
  { prop: 'createdUidName', label: '创建者', hide: true },
  { prop: 'createdTime', label: '创建时间', hide: true, formatter: DateUtil.render },
  { prop: 'updatedTime', label: '修改时间', hide: true, formatter: DateUtil.render },
])
const rows = ref<any[]>([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, { advanced: false }))
const pagination = ref(RouteUtil.pagination(filters.value))
const selection = ref<any[]>([])
const layoutLabels: Record<string, string> = {
  absolute: '自由定位',
  relative: '相对定位',
  fixed: '固定定位',
  flex: '弹性布局',
  grid: '栅格布局',
}

const parseContent = (content: string) => {
  try {
    return JSON.parse(content || '{}')
  } catch {
    return {}
  }
}

const decorateRows = (items: any[]) => items.map((item) => {
  const content = parseContent(item.content)
  const canvas = content.canvas || {}
  const specialCount = Object.keys(content.componentDefaults || {}).length
  return {
    ...item,
    layout: canvas.width && canvas.height
      ? `${canvas.width} × ${canvas.height} · ${layoutLabels[canvas.layout] || '自由定位'}`
      : '默认画布',
    styleSummary: specialCount
      ? `通用样式 + ${specialCount} 类个性化`
      : '仅通用样式',
  }
})

const handleRefresh = (toQuery: boolean, keepPage: boolean) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  toQuery && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  DashboardTemplateApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = decorateRows(result.data?.rows || [])
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

onMounted(() => {
  handleRefresh(false, true)
})

const handleAdd = () => {
  router.push('/bi/design/dashboardTemplate').catch(() => {})
}
const handleDelete = () => {
  TableUtil.selection(selection.value).then((ids: any) => {
    loading.value = true
    DashboardTemplateApi.delete(ids, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {
      loading.value = false
    })
  }).catch(() => {})
}
const handleDesign = (row: any) => {
  router.push({ path: '/bi/design/dashboardTemplate', query: { id: row.id } }).catch(() => {})
}
const handleCreateDashboard = (row: any) => {
  router.push({ path: '/bi/design/dashboard', query: { templateId: row.id } }).catch(() => {})
}
</script>

<template>
  <el-card v-show="searchable" :bordered="false" shadow="never" class="fs-table-search">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="名称" prop="name">
        <el-input v-model="filters.name" clearable placeholder="请输入模板名称" />
      </form-search-item>
      <form-search-item>
        <el-button type="primary" :loading="loading" @click="handleRefresh(true, false)">查询</el-button>
        <el-button @click="filterRef?.resetFields()">重置</el-button>
      </form-search-item>
    </form-search>
  </el-card>

  <el-card :bordered="false" shadow="never" class="fs-table-card">
    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="template-tip"
      title="模板统一配置基础信息、页面布局、通用组件样式和特殊组件个性化样式。新建大屏默认创建空白画布并继承这些配置。"
    />
    <div class="fs-table-toolbar flex-between">
      <el-space>
        <el-button v-permit="'bi:dashboardTemplate:add'" type="primary" :icon="Plus" @click="handleAdd">新建模板</el-button>
        <button-delete v-permit="'bi:dashboardTemplate:delete'" :disabled="selection.length === 0" @click="handleDelete" />
      </el-space>
      <el-space>
        <button-search @click="searchable = !searchable" />
        <button-refresh :loading="loading" @click="handleRefresh(true, true)" />
        <TableColumnSetting v-model="columns" :table="tableRef" :loading="loading" />
      </el-space>
    </div>
    <el-table
      ref="tableRef"
      v-loading="loading"
      :data="rows"
      :row-key="(row: any) => row.id"
      table-layout="auto"
      @selection-change="(value: any[]) => selection = value">
      <el-table-column type="selection" />
      <TableColumn :columns="columns" />
      <el-table-column label="操作" width="190" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" v-permit="'bi:dashboardTemplate:modify'" @click="handleDesign(row)">编辑配置</el-button>
          <el-button
            link
            v-permit="'bi:dashboard:add'"
            :disabled="String(row.status) !== '1'"
            @click="handleCreateDashboard(row)">用模板新建</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" :loading="loading" @change="handleRefresh(true, true)" />
  </el-card>

</template>

<style lang="scss" scoped>
.template-tip {
  margin-bottom: 12px;
}
</style>
