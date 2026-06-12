<script setup lang="ts">
import { onMounted, ref } from 'vue';
import type { FormInstance, TableInstance } from 'element-plus';
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router';
import MessageApi from '@/api/member/MessageApi';
import DateUtil from '@/utils/DateUtil';
import TableUtil from '@/utils/TableUtil';

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'type', label: '消息类型' },
  { prop: 'recipient', label: '收件人' },
  { prop: 'subject', label: '主题' },
  { prop: 'status', label: '状态' },
  { prop: 'content', label: '内容', hide: true },
  { prop: 'exception', label: '异常', hide: true },
  { prop: 'createdTime', label: '创建时间', formatter: DateUtil.render },
])
const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, { advanced: false, deleted: '' }))
const pagination = ref(RouteUtil.pagination(filters.value))
const selection = ref([])
const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  MessageApi.list(filters.value).then((result: any) => {
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
    MessageApi.delete(ids, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {})
  }).catch(() => {})
}
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="消息类型" prop="type">
        <el-input v-model="filters.type" clearable />
      </form-search-item>
      <form-search-item label="收件人" prop="recipient">
        <el-input v-model="filters.recipient" clearable />
      </form-search-item>
      <form-search-item label="主题" prop="subject">
        <el-input v-model="filters.subject" clearable />
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
        <form-search-item label="状态" prop="status">
          <el-input v-model="filters.status" clearable />
        </form-search-item>
        <form-search-item label="内容" prop="content" :span="12">
          <el-input v-model="filters.content" clearable />
        </form-search-item>
        <form-search-item label="创建开始时间" prop="createdTimeBegin">
          <form-date-picker v-model="filters.createdTimeBegin" placeholder="开始时间" />
        </form-search-item>
        <form-search-item label="创建结束时间" prop="createdTimeEnd">
          <form-date-picker v-model="filters.createdTimeEnd" placeholder="结束时间" />
        </form-search-item>
        <form-search-item label="异常" prop="exception" :span="12">
          <el-input v-model="filters.exception" clearable />
        </form-search-item>
      </template>
    </form-search>
  </el-card>
  <el-card :bordered="false" shadow="never" class="fs-table-card">
    <div class="fs-table-toolbar flex-between">
      <el-space>
        <button-delete v-permit="'member:message:delete'" :disabled="selection.length === 0" @click="handleDelete" />
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
      <TableColumn :columns="columns" />
      <el-table-column label="操作">
        <template #default="scope">
          <el-button link @click="handleShow(scope)" v-permit="'member:message:'">查看</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
  </el-card>
  <el-drawer v-model="infoVisible" :title="'消息详情 - ' + form.id" size="60%">
    <el-descriptions border :column="2">
      <el-descriptions-item label="消息类型">{{ form.type }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ form.status }}</el-descriptions-item>
      <el-descriptions-item label="收件人">{{ form.recipient }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ DateUtil.format(form.createdTime) }}</el-descriptions-item>
      <el-descriptions-item label="主题" :span="2">{{ form.subject }}</el-descriptions-item>
      <el-descriptions-item label="内容" :span="2"><layout-html-sandbox :html="form.content" style="height: 600px;" /></el-descriptions-item>
      <el-descriptions-item label="异常信息" :span="2">{{ form.exception }}</el-descriptions-item>
    </el-descriptions>
  </el-drawer>
</template>

<style lang="scss" scoped>
</style>
