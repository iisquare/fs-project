<script setup lang="ts">
import { onMounted, ref } from 'vue';
import type { FormInstance, TableInstance } from 'element-plus';
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router';
import ApiUtil from '@/utils/ApiUtil';
import TableUtil from '@/utils/TableUtil';
import FormFrameApi from '@/api/oa/FormFrameApi';

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'name', label: '名称' },
  { prop: 'sort', label: '排序' },
  { prop: 'statusText', label: '状态' },
  { prop: 'description', label: '描述' },
])
const config = ref({
  ready: false,
  status: {},
})
const infoVisible = ref(false)
const infoRow = ref<any>({})
const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, {}))
const pagination = ref(RouteUtil.pagination(filters.value))
const selection: any = ref([])
const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  FormFrameApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
onMounted(() => {
  handleRefresh(false, true)
  FormFrameApi.config().then((result: any) => {
    Object.assign(config.value, { ready: true }, ApiUtil.data(result))
  }).catch(() => {})
})
const handleAdd = (env: Event) => {
  RouteUtil.forward(route, router, env, {
    path: '/oa/form/model'
  })
}
const handleEdit = (scope: any, env: Event) => {
  RouteUtil.forward(route, router, env, {
    path: '/oa/form/model',
    query: {
      id: scope.row.id
    }
  })
}
const handleShow = (scope: any) => {
  infoRow.value = Object.assign({}, scope.row, {
    description: scope.row.description ? scope.row.description : '暂无'
  })
  infoVisible.value = true
}
const handleData = (scope: any) => {
  RouteUtil.forward(route, router, undefined, {
    path: '/oa/form/list',
    query: { id: scope.row.id }
  })
}
const handleDelete = () => {
  TableUtil.selection(selection.value).then((ids: any) => {
    loading.value = true
    FormFrameApi.delete(ids, { success: true }).then(() => {
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
      <form-search-item label="名称" prop="name">
        <el-input v-model="filters.name" clearable />
      </form-search-item>
      <form-search-item label="内容" prop="content">
        <el-input v-model="filters.content" clearable />
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
        <button-add v-permit="'oa:formFrame:add'" @click="(e: Event) => handleAdd(e)" />
        <button-delete v-permit="'oa:formFrame:delete'" :disabled="selection.length === 0" @click="handleDelete" />
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
      <el-table-column label="操作" width="180">
        <template #default="scope">
          <el-button link v-permit="'oa:formFrame:'" @click="() => handleShow(scope)">查看</el-button>
          <el-button link @click="(e: any) => handleEdit(scope, e)" v-permit="'oa:formFrame:modify'">编辑</el-button>
          <el-button link @click="() => handleData(scope)">数据</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
  </el-card>

  <el-dialog v-model="infoVisible" :title="`信息查看 - ${infoRow.id}`" width="500">
    <el-descriptions :column="1" border>
      <el-descriptions-item label="名称">{{ infoRow.name }}</el-descriptions-item>
      <el-descriptions-item label="排序">{{ infoRow.sort }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ infoRow.statusText }}</el-descriptions-item>
      <el-descriptions-item label="描述">{{ infoRow.description }}</el-descriptions-item>
      <el-descriptions-item label="创建者">{{ infoRow.createdUidName }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ infoRow.createdTime }}</el-descriptions-item>
      <el-descriptions-item label="修改者">{{ infoRow.updatedUidName }}</el-descriptions-item>
      <el-descriptions-item label="修改时间">{{ infoRow.updatedTime }}</el-descriptions-item>
    </el-descriptions>
  </el-dialog>
</template>

<style lang="scss" scoped>
</style>
