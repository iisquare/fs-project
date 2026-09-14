<script setup lang="ts">
import { onMounted, ref } from 'vue'
import type { FormInstance, TableInstance } from 'element-plus'
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router'
import ApiUtil from '@/utils/ApiUtil'
import TableUtil from '@/utils/TableUtil'
import DateUtil from '@/utils/DateUtil'
import WorkflowApi from '@/api/oa/WorkflowApi'
import FormFrameApi from '@/api/oa/FormFrameApi'

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'name', label: '名称' },
  { prop: 'formId', label: '表单' },
  { prop: 'sort', label: '排序' },
  { prop: 'statusText', label: '状态' },
  { prop: 'deploymentInfo.deploymentTime', label: '发布时间', formatter: DateUtil.render },
])
const config = ref<any>({
  ready: false,
  status: {},
})
const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, {}))
const pagination = ref(RouteUtil.pagination(filters.value))
const selection: any = ref([])
const infoVisible = ref(false)
const infoRow = ref<any>({})

const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  WorkflowApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

onMounted(() => {
  handleRefresh(false, true)
  WorkflowApi.config().then((result: any) => {
    Object.assign(config.value, { ready: true }, ApiUtil.data(result))
  }).catch(() => {})
})

// 新增流程直接进入设计器，名称、关联表单等在流程属性面板中编辑
const handleAdd = (env: Event) => {
  RouteUtil.forward(route, router, env, {
    path: '/oa/workflow/model'
  })
}

const handleEdit = (scope: any, env: Event) => {
  RouteUtil.forward(route, router, env, {
    path: '/oa/workflow/model',
    query: { id: scope.row.id }
  })
}

const handleShow = (scope: any) => {
  infoRow.value = Object.assign({}, scope.row, {
    description: scope.row.description ? scope.row.description : '暂无'
  })
  infoVisible.value = true
}

const handleDelete = () => {
  TableUtil.selection(selection.value).then((ids: any) => {
    loading.value = true
    WorkflowApi.delete(ids, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {
      loading.value = false
    })
  }).catch(() => {})
}

const handlePublish = (scope: any) => {
  loading.value = true
  WorkflowApi.publish({ id: scope.row.id }, { success: true }).then(() => {
    handleRefresh(false, true)
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="名称" prop="name">
        <el-input v-model="filters.name" clearable />
      </form-search-item>
      <form-search-item label="表单" prop="formId">
        <form-select v-model="filters.formId" :callback="FormFrameApi.list" clearable placeholder="输入表单名称检索" />
      </form-search-item>
      <form-search-item label="状态" prop="status">
        <el-select v-model="filters.status" placeholder="请选择" clearable>
          <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
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
        <button-add v-permit="'oa:workflow:add'" @click="(e: Event) => handleAdd(e)" />
        <button-delete v-permit="'oa:workflow:delete'" :disabled="selection.length === 0" @click="handleDelete" />
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
      @selection-change="(newSelection: any) => selection = newSelection"
    >
      <el-table-column type="selection" />
      <TableColumn :columns="columns" />
      <el-table-column label="操作" width="160">
        <template #default="scope">
          <el-button link v-permit="'oa:workflow:'" @click="() => handleShow(scope)">查看</el-button>
          <el-button link @click="(e: any) => handleEdit(scope, e)" v-permit="'oa:workflow:modify'">编辑</el-button>
          <el-button link @click="() => handlePublish(scope)" v-permit="'oa:workflow:publish'">发布</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
  </el-card>

  <el-dialog v-model="infoVisible" :title="`信息查看 - ${infoRow.id}`" width="500">
    <el-descriptions :column="1" border>
      <el-descriptions-item label="名称">{{ infoRow.name }}</el-descriptions-item>
      <el-descriptions-item label="表单">{{ infoRow.formId }}</el-descriptions-item>
      <el-descriptions-item label="排序">{{ infoRow.sort }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ infoRow.statusText }}</el-descriptions-item>
      <el-descriptions-item label="描述">{{ infoRow.description }}</el-descriptions-item>
      <el-descriptions-item label="创建者">{{ infoRow.createdUidName }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ DateUtil.format(infoRow.createdTime) }}</el-descriptions-item>
      <el-descriptions-item label="修改者">{{ infoRow.updatedUidName }}</el-descriptions-item>
      <el-descriptions-item label="修改时间">{{ DateUtil.format(infoRow.updatedTime) }}</el-descriptions-item>
      <el-descriptions-item label="发布者">{{ infoRow.deploymentUidName }}</el-descriptions-item>
      <el-descriptions-item label="发布时间">{{ DateUtil.format(infoRow.deploymentInfo?.deploymentTime) }}</el-descriptions-item>
    </el-descriptions>
  </el-dialog>
</template>

<style lang="scss" scoped>
</style>
