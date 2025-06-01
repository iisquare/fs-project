<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import type { FormInstance, TableInstance } from 'element-plus';
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router';
import ApplicationApi from '@/api/member/ApplicationApi';
import ApiUtil from '@/utils/ApiUtil';
import DateUtil from '@/utils/DateUtil';
import LayoutIcon from '@/components/Layout/LayoutIcon.vue';
import MenuApi from '@/api/member/MenuApi';
import RoleApi from '@/api/member/RoleApi';
import * as ElementPlusIcons from '@element-plus/icons-vue';
import DataUtil from '@/utils/DataUtil';
import TableUtil from '@/utils/TableUtil';

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'serial', label: '标识' },
  { prop: 'name', label: '名称' },
  { prop: 'permit', label: '授权状态' },
  { prop: 'statusText', label: '应用状态' },
])

const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, { advanced: false, roleIds: [] }))
const pagination = ref(RouteUtil.pagination(filters.value, { pageSize: 100 }))
const selection = ref([])
const permitted: any = ref([])
const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  const params = { id: route.query.id, type: 'application' }
  Promise.all([RoleApi.permit(params), ApplicationApi.list(filters.value)]).then((results: any) => {
    RouteUtil.result2pagination(pagination.value, results[1])
    permitted.value = results[0].data.checked
    rows.value = results[1].data.rows.map((item: any) => {
      item.permit = permitted.value.indexOf(item.id) === -1 ? 'N' : 'Y'
      return item
    })
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

const info: any = ref({})
onMounted(() => {
  handleRefresh(false, true)
  RoleApi.info(route.query.id).then(result => {
    info.value = ApiUtil.data(result)
  }).catch(() => {})
})

const handleGrant = () => {
  const bids = new Set(permitted.value)
  selection.value.forEach((item: any) => bids.add(item.id))
  handlePermit(Array.from(bids))
}

const handleRevoke = () => {
  const bids = new Set(permitted.value)
  selection.value.forEach((item: any) => bids.delete(item.id))
  handlePermit(Array.from(bids))
}

const handlePermit = (bids: any) => {
  if (loading.value) return false
  loading.value = true
  const params = { id: route.query.id, type: 'application', bids }
  RoleApi.permit(params, { success: true }).then(result => {
    handleRefresh(false, true)
  }).catch(() => {})
}

const tree = reactive({
  id: '',
  type: '',
  application: {} as any,
  title: '',
  visible: false,
  loading: false,
  selection: [],
  rows: [],
  expandedRowKeys: [],
  columns: [
    { label: '名称', prop: 'name' },
    { label: '全称', prop: 'fullName' }
  ]
})
const treeRef = ref<TableInstance>()
const handleTree = (type: string, scope: any) => {
  Object.assign(tree, {
    id: info.value.id,
    type: type,
    application: scope.row,
    title: { resource: '资源分配', menu: '菜单分配' }[type],
    rows: [],
    visible: true,
    loading: true,
  })
  treeRef.value?.clearSelection()
  RoleApi.permit({ id: tree.id, type, applicationId: scope.row.id }).then((result: any) => {
    Object.assign(tree, {
      loading: false,
      rows: result.data.tree,
      expandedRowKeys: TableUtil.expandedRowKeys(result.data.tree, 2)
    })
    TableUtil.toggleRowSelection(treeRef.value, result.data.checked)
  }).catch(() => {})
}
const handleSubmit = () => {
  if (tree.loading) return false
  tree.loading = true
  const params = {
    id: tree.id,
    type: tree.type,
    applicationId: tree.application.id,
    bids: DataUtil.values(tree.selection, 'id')
  }
  RoleApi.permit(params, { success: true }).then(() => {
    tree.visible = false
    handleRefresh(false, true)
  }).catch(() => {}).finally(() => {
    tree.loading = false
  })
}
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <template #header>
      <div class="flex-between">
        <div>角色：[{{ info.id }}]{{ info.name }}</div>
        <el-button @click="router.go(-1)">返回</el-button>
      </div>
    </template>
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="标识" prop="serial">
        <el-input v-model="filters.serial" clearable />
      </form-search-item>
      <form-search-item label="名称" prop="name">
        <el-input v-model="filters.name" clearable />
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
        <el-button type="danger" :icon="ElementPlusIcons.CirclePlus" :disabled="selection.length === 0" @click="handleGrant">授权</el-button>
        <el-button type="warning" :icon="ElementPlusIcons.Remove" :disabled="selection.length === 0" @click="handleRevoke">解除</el-button>
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
      :row-key="record => record.id"
      :border="true"
      v-loading="loading"
      table-layout="auto"
      @selection-change="newSelection => selection = newSelection"
    >
      <el-table-column type="selection" />
      <TableColumn :columns="columns"></TableColumn>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button link @click="handleTree('resource', scope)" v-permit="'member:role:resource'">资源</el-button>
          <el-button link @click="handleTree('menu', scope)" v-permit="'member:role:menu'">菜单</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
  </el-card>
  <el-drawer v-model="tree.visible" :close-on-click-modal="false" :show-close="false" :destroy-on-close="true">
    <template #header="{ close, titleId, titleClass }">
      <h4 :id="titleId" :class="titleClass">{{ tree.title }}</h4>
      <el-space>
        <el-button type="primary" @click="handleSubmit" :loading="tree.loading">确定</el-button>
        <el-button @click="close">取消</el-button>
      </el-space>
    </template>
    <el-descriptions title="角色信息" border>
      <el-descriptions-item label="ID">{{ info.id }}</el-descriptions-item>
      <el-descriptions-item label="名称">{{ info.name }}</el-descriptions-item>
      <el-descriptions-item label="类型">{{ tree.type }}</el-descriptions-item>
    </el-descriptions>
    <el-descriptions title="应用信息" border>
      <el-descriptions-item label="ID">{{ tree.application.id }}</el-descriptions-item>
      <el-descriptions-item label="标识">{{ tree.application.serial }}</el-descriptions-item>
      <el-descriptions-item label="名称">{{ tree.application.name }}</el-descriptions-item>
    </el-descriptions>
    <el-table
      ref="treeRef"
      :data="tree.rows"
      :row-key="record => record.id"
      :expand-row-keys="tree.expandedRowKeys"
      :border="true"
      v-loading="tree.loading"
      table-layout="auto"
      :tree-props="{ checkStrictly: true }"
      @selection-change="newSelection => tree.selection = newSelection"
    >
      <el-table-column type="selection" />
      <TableColumn :columns="tree.columns"></TableColumn>
    </el-table>
  </el-drawer>
</template>

<style lang="scss" scoped>
.el-descriptions {
  margin-bottom: 20px;
}
</style>
