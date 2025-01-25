<script setup lang="ts">
import { onMounted, ref } from 'vue';
import * as ElementPlusIcons from '@element-plus/icons-vue';
import type { FormInstance, TableInstance } from 'element-plus';
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router';
import UserApi from '@/api/member/UserApi';
import RoleApi from '@/api/member/RoleApi';
import ApiUtil from '@/utils/ApiUtil';
import DateUtil from '@/utils/DateUtil';

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'serial', label: '账号' },
  { prop: 'name', label: '名称' },
  { prop: 'sort', label: '排序' },
  { prop: 'statusText', label: '状态' },
  { prop: 'role', label: '角色', slot: 'role' },
  { prop: 'lockedTime', label: '锁定时间', formatter: DateUtil.dateRender },
  { prop: 'createdTime', label: '创建时间', formatter: DateUtil.dateRender, hide: true },
  { prop: 'updatedTime', label: '修改时间', formatter: DateUtil.dateRender, hide: true },
  { prop: 'deletedTime', label: '删除时间', formatter: DateUtil.dateRender },
  { prop: 'loginIp', label: '登录IP', hide: true },
  { prop: 'loginTime', label: '登录时间', formatter: DateUtil.dateRender, hide: true },
  
])
const config = ref({
  ready: false,
  defaultPassword: '',
  status: [],
})
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, { advanced: false, roleIds: [] }))
const pagination = ref(RouteUtil.pagination(filters.value))
const selection = ref([])
const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  UserApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
onMounted(() => {
  handleRefresh(false, true)
  UserApi.config().then(result => {
    Object.assign(config.value, { ready: true }, ApiUtil.data(result))
  })
})

const rows = ref([])
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="帐号" prop="serial">
        <el-input v-model="filters.serial" clearable />
      </form-search-item>
      <form-search-item label="名称" prop="name">
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
        <button-advanced v-model="filters.advanced" />
      </form-search-item>
      <template v-if="filters.advanced">
        <form-search-item label="ID" prop="id">
          <el-input v-model="filters.id" clearable />
        </form-search-item>
        <form-search-item label="角色" prop="roleIds">
          <form-select v-model="filters.roleIds" :callback="RoleApi.list" multiple clearable />
        </form-search-item>
        <form-search-item label="注册IP" prop="createdIp">
          <el-input v-model="filters.createdIp" clearable />
        </form-search-item>
        <form-search-item label="登录IP" prop="loginIp">
          <el-input v-model="filters.loginIp" clearable />
        </form-search-item>
        <form-search-item label="创建开始时间" prop="createdTimeBegin">
          <form-date-picker v-model="filters.createdTimeBegin" placeholder="开始时间" />
        </form-search-item>
        <form-search-item label="创建结束时间" prop="createdTimeEnd">
          <form-date-picker v-model="filters.createdTimeEnd" placeholder="结束时间" />
        </form-search-item>
        <form-search-item label="修改开始时间" prop="updatedTimeBegin">
          <form-date-picker v-model="filters.updatedTimeBegin" placeholder="开始时间" />
        </form-search-item>
        <form-search-item label="修改结束时间" prop="updatedTimeEnd">
          <form-date-picker v-model="filters.updatedTimeEnd" placeholder="结束时间" />
        </form-search-item>
        <form-search-item label="登录开始时间" prop="loginTimeBegin">
          <form-date-picker v-model="filters.loginTimeBegin" placeholder="开始时间" />
        </form-search-item>
        <form-search-item label="登录结束时间" prop="loginTimeEnd">
          <form-date-picker v-model="filters.loginTimeEnd" placeholder="结束时间" />
        </form-search-item>
        <form-search-item label="锁定开始时间" prop="lockedTimeBegin">
          <form-date-picker v-model="filters.lockedTimeBegin" placeholder="开始时间" />
        </form-search-item>
        <form-search-item label="锁定结束时间" prop="lockedTimeEnd">
          <form-date-picker v-model="filters.lockedTimeEnd" placeholder="结束时间" />
        </form-search-item>
        <form-search-item label="删除开始时间" prop="deletedTimeBegin">
          <form-date-picker v-model="filters.deletedTimeBegin" placeholder="开始时间" />
        </form-search-item>
        <form-search-item label="删除结束时间" prop="deletedTimeEnd">
          <form-date-picker v-model="filters.deletedTimeEnd" placeholder="结束时间" />
        </form-search-item>
      </template>
    </form-search>
  </el-card>
  <el-card :bordered="false" shadow="never" class="fs-table-card">
    <div class="fs-table-toolbar flex-between">
      <el-space>
        <el-button type="success" :icon="ElementPlusIcons.Plus" v-permit="'member:user:add'">新增</el-button>
        <el-button type="warning" :icon="ElementPlusIcons.Edit" v-permit="'member:user:modify'" :disabled="selection.length !== 1">编辑</el-button>
        <el-button type="danger" :icon="ElementPlusIcons.Delete" v-permit="'member:user:delete'" :disabled="selection.length === 0">删除</el-button>
      </el-space>
      <el-space>
        <el-button :icon="ElementPlusIcons.Search" circle title="展示/隐藏搜索栏" @click="searchable = !searchable" />
        <el-button :icon="ElementPlusIcons.Refresh" circle title="重新加载分页数据" @click="handleRefresh(true, true)" :loading="loading" />
        <TableColumnSetting v-model="columns" :table="tableRef" />
      </el-space>
    </div>
    <el-table
      ref="tableRef"
      :data="rows"
      :row-key="record => record.id"
      :border="true"
      v-loading="loading"
      @selection-change="newSelection => selection = newSelection"
    >
      <el-table-column type="selection" />
      <TableColumn :columns="columns">
        <template #role="scope">
          <el-tag v-for="item in scope.row.roles" :key="item.id">{{ item.name }}</el-tag>
        </template>
      </TableColumn>
    </el-table>
    <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
  </el-card>
</template>

<style lang="scss" scoped>
</style>
