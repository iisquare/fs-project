<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { type FormInstance, type TableInstance } from 'element-plus';
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router';
import DateUtil from '@/utils/DateUtil';
import TableUtil from '@/utils/TableUtil';
import DataLogApi from '@/api/member/DataLogApi';

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'permits', label: '权限标识' },
  { prop: 'serverId', label: '服务标识' },
  { prop: 'clientId', label: '客户标识' },
  { prop: 'requestUrl', label: '请求地址', hide: true },
  { prop: 'requestIp', label: '请求IP', hide: true },
  { prop: 'requestTime', label: '请求时间', formatter: DateUtil.render },
])
const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, { advanced: false, roleIds: [] }))
const pagination = ref(RouteUtil.pagination(filters.value))
const selection = ref([])
const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  DataLogApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
onMounted(() => {
  handleRefresh(false, true)
})
const infoVisible = ref(false)
const form: any = ref({})
const handleShow = (scope: any) => {
  form.value = Object.assign({}, scope.row)
  infoVisible.value = true
}
const handleDelete = () => {
  TableUtil.selection(selection.value).then((ids: any) => {
    loading.value = true
    DataLogApi.delete(ids, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {})
  }).catch(() => {})
}
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="权限标识" prop="permits">
        <el-input v-model="filters.permits" clearable />
      </form-search-item>
      <form-search-item label="服务标识" prop="serverId">
        <el-input v-model="filters.serverId" clearable />
      </form-search-item>
      <form-search-item label="客户标识" prop="clientId">
        <el-input v-model="filters.serverId" clearable />
      </form-search-item>
      <form-search-item>
        <el-button type="primary" @click="handleRefresh(true, false)">查询</el-button>
        <el-button @click="filterRef?.resetFields()">重置</el-button>
        <button-advanced v-model="filters.advanced" />
      </form-search-item>
      <template v-if="filters.advanced">
        <form-search-item label="请求地址" prop="requestUrl">
          <el-input v-model="filters.requestUrl" clearable />
        </form-search-item>
        <form-search-item label="请求IP" prop="requestIp">
          <el-input v-model="filters.requestIp" clearable />
        </form-search-item>
        <form-search-item label="请求头部" prop="requestHeaders">
          <el-input v-model="filters.requestHeaders" clearable />
        </form-search-item>
        <form-search-item label="请求参数" prop="requestParams">
          <el-input v-model="filters.requestParams" clearable />
        </form-search-item>
        <form-search-item label="日志标识" prop="id">
          <el-input v-model="filters.id" clearable />
        </form-search-item>
        <form-search-item label="请求开始时间" prop="requestTimeBegin">
          <form-date-picker v-model="filters.requestTimeBegin" placeholder="开始时间" />
        </form-search-item>
        <form-search-item label="请求结束时间" prop="requestTimeEnd">
          <form-date-picker v-model="filters.requestTimeTimeEnd" placeholder="结束时间" />
        </form-search-item>
      </template>
    </form-search>
  </el-card>
  <el-card :bordered="false" shadow="never" class="fs-table-card">
    <div class="fs-table-toolbar flex-between">
      <el-space>
        <button-delete v-permit="'member:dataLog:delete'" :disabled="selection.length === 0" @click="handleDelete" />
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
      <TableColumn :columns="columns">
        <template #role="scope">
          <el-space><el-tag v-for="item in scope.row.roles" :key="item.id">{{ item.name }}</el-tag></el-space>
        </template>
      </TableColumn>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button link @click="handleShow(scope)" v-permit="'member:user:'">查看</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
  </el-card>
  <el-drawer v-model="infoVisible" :title="'信息查看 - ' + form.id" :close-on-click-modal="false" :destroy-on-close="true" size="80%">
    <el-descriptions border>
      <el-descriptions-item label="请求权限" :span="2">{{ form.permits }}</el-descriptions-item>
      <el-descriptions-item label="请求时间">{{ DateUtil.format(form.requestTime) }}</el-descriptions-item>
      <el-descriptions-item label="服务标识">{{ form.serverId }}</el-descriptions-item>
      <el-descriptions-item label="客户标识">{{ form.clientId }}</el-descriptions-item>
      <el-descriptions-item label="请求IP">{{ form.requestIp }}</el-descriptions-item>
      <el-descriptions-item label="请求地址" :span="3"><el-input type="textarea" v-model="form.requestUrl" :rows="3" /></el-descriptions-item>
      <el-descriptions-item label="请求参数" :span="3"><el-input type="textarea" v-model="form.requestParams" :rows="3" /></el-descriptions-item>
      <el-descriptions-item label="请求头部" :span="3"><el-input type="textarea" v-model="form.requestHeaders" :rows="5" /></el-descriptions-item>
      <template v-for="item in form.permitted" :key="item.id">
        <el-descriptions-item label="权限标识">{{ item.dataSerial }}</el-descriptions-item>
        <el-descriptions-item label="权限主键">{{ item.dataId }}</el-descriptions-item>
        <el-descriptions-item label="记录主键">{{ item.id }}</el-descriptions-item>
        <el-descriptions-item label="授权字段" :span="3"><el-input type="textarea" v-model="item.fields" :rows="3" /></el-descriptions-item>
        <el-descriptions-item label="授权范围" :span="3"><el-input type="textarea" v-model="item.filters" :rows="5" /></el-descriptions-item>
      </template>
    </el-descriptions>
  </el-drawer>
</template>

<style lang="scss" scoped>
</style>
