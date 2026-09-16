<script setup lang="ts">
/**
 * 智能体编排 - 应用管理，维护编排应用并进入编排画布。
 */
import { onMounted, ref } from 'vue'
import type { FormInstance, TableInstance } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import AgenticApi from '@/api/agent/AgenticApi'
import ApiUtil from '@/utils/ApiUtil'
import DateUtil from '@/utils/DateUtil'
import ElementUtil from '@/utils/ElementUtil'
import RouteUtil from '@/utils/RouteUtil'
import TableUtil from '@/utils/TableUtil'
import config from '@/designer/Agentic/config'

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'name', label: '应用名称' },
  { prop: 'modeText', label: '应用类型' },
  { prop: 'tags', label: '标签', slot: 'tags' },
  { prop: 'publishText', label: '发布状态', slot: 'publishText' },
  { prop: 'publishedTime', label: '发布时间', slot: 'publishedTime' },
  { prop: 'description', label: '描述', hide: true },
  { prop: 'sort', label: '排序' },
  { prop: 'statusText', label: '状态' },
  { prop: 'updatedTime', label: '修改时间', slot: 'updatedTime' },
])
const configData: any = ref({
  ready: false,
  sorts: {},
  status: {},
})
const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, { advanced: false }))
const pagination = ref(RouteUtil.pagination(filters.value))
const selection: any = ref([])
const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  AgenticApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
onMounted(() => {
  handleRefresh(false, true)
  AgenticApi.config().then((result: any) => {
    const data: any = ApiUtil.data(result) ?? {}
    Object.assign(configData.value, { ready: true }, data)
    config.status = data.status ?? config.status // 编排画布复用状态字典
  }).catch(() => {})
})
const infoVisible = ref(false)
const info: any = ref({})
// 新增与修改均在编排画布中完成，列表页只负责跳转、查看、发布与删除
const handleAdd = () => {
  router.push({ path: '/agent/agentic/model' })
}
const handleShow = (scope: any) => {
  info.value = Object.assign({}, scope.row)
  infoVisible.value = true
  // 列表不返回画布内容，查看时补取详情（节点数量、发布者等）
  AgenticApi.info(scope.row.id).then((result: any) => {
    info.value = Object.assign({}, info.value, ApiUtil.data(result) ?? {})
  }).catch(() => {})
}
const handlePublish = (scope: any) => {
  const tips = `发布后对外提供的是「${scope.row.name}」最近一次保存的内容，画布中未保存的改动不会包含在内。确认发布？`
  ElementUtil.confirm(tips).then(() => {
    loading.value = true
    AgenticApi.publish({ id: scope.row.id }, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {
      loading.value = false
    })
  }).catch(() => {})
}
const handleDelete = () => {
  TableUtil.selection(selection.value).then((ids: any) => {
    loading.value = true
    AgenticApi.delete(ids, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {
      loading.value = false
    })
  }).catch(() => {})
}
const handleDesign = (scope: any) => {
  router.push({ path: '/agent/agentic/model', query: { id: scope.row.id } })
}
const modeText = (mode: any) => {
  const item = config.modes.find((item: any) => item.value === mode)
  return item ? item.label : mode
}
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="名称" prop="name">
        <el-input v-model="filters.name" clearable />
      </form-search-item>
      <form-search-item label="类型" prop="mode">
        <el-select v-model="filters.mode" placeholder="请选择" clearable>
          <el-option :key="item.value" :value="item.value" :label="item.label" v-for="item in config.modes" />
        </el-select>
      </form-search-item>
      <form-search-item label="状态" prop="status">
        <el-select v-model="filters.status" placeholder="请选择" clearable>
          <el-option :key="key" :value="key" :label="value" v-for="(value, key) in configData.status" />
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
        <button-add v-permit="'agent:agentic:add'" @click="handleAdd" />
        <button-delete v-permit="'agent:agentic:delete'" :disabled="selection.length === 0" @click="handleDelete" />
      </el-space>
      <el-space>
        <button-search @click="searchable = !searchable" />
        <button-refresh @click="handleRefresh(true, true)" :loading="loading" />
        <TableColumnSetting v-model="columns" :table="tableRef" />
        <TableSort v-model="filters.sort" :columns="columns" :sortable="configData.sorts" @change="handleRefresh(true, true)" />
      </el-space>
    </div>
    <el-table
      ref="tableRef"
      :data="rows"
      :row-key="(record: any) => record.id"
      :border="true"
      v-loading="loading"
      table-layout="auto"
      @selection-change="(s: any) => selection = s"
    >
      <el-table-column type="selection" />
      <TableColumn :columns="columns">
        <template #tags="scope">
          <el-space>
            <el-tag type="info" v-for="item in scope.row.tags" :key="item">{{ item }}</el-tag>
          </el-space>
        </template>
        <template #publishText="scope">
          <el-tag :type="scope.row.publishedVersion > 0 ? 'success' : 'info'" effect="plain">{{ scope.row.publishText }}</el-tag>
        </template>
        <template #publishedTime="scope">{{ scope.row.publishedTime ? DateUtil.format(scope.row.publishedTime) : '—' }}</template>
        <template #updatedTime="scope">{{ DateUtil.format(scope.row.updatedTime) }}</template>
      </TableColumn>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button link @click="handleShow(scope)" v-permit="'agent:agentic:'">查看</el-button>
          <el-button link @click="handleDesign(scope)" v-permit="'agent:agentic:modify'">编排</el-button>
          <el-button link @click="handlePublish(scope)" v-permit="'agent:agentic:modify'">发布</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
  </el-card>
  <el-drawer v-model="infoVisible" :title="'信息查看 - ' + info.id">
    <el-form :model="info" label-width="auto">
      <el-form-item label="应用名称">{{ info.name }}</el-form-item>
      <el-form-item label="应用类型">{{ info.modeText || modeText(info.mode) }}</el-form-item>
      <el-form-item label="应用标签">
        <el-space>
          <el-tag type="info" v-for="item in info.tags" :key="item">{{ item }}</el-tag>
        </el-space>
      </el-form-item>
      <el-form-item label="排序">{{ info.sort }}</el-form-item>
      <el-form-item label="状态">{{ info.statusText }}</el-form-item>
      <el-form-item label="描述">{{ info.description ? info.description : '暂无' }}</el-form-item>
      <el-form-item label="节点数量">{{ info.content?.cells?.length ?? 0 }}</el-form-item>
      <el-form-item label="发布状态">{{ info.publishText }}</el-form-item>
      <el-form-item label="发布版本">{{ info.publishedVersion > 0 ? 'v' + info.publishedVersion : '—' }}</el-form-item>
      <el-form-item label="发布时间">{{ info.publishedTime ? DateUtil.format(info.publishedTime) : '—' }}</el-form-item>
      <el-form-item label="发布者">{{ info.publishedUserInfo?.name || '—' }}</el-form-item>
      <el-form-item label="创建者">{{ info.createdUserInfo?.name }}</el-form-item>
      <el-form-item label="创建时间">{{ DateUtil.format(info.createdTime) }}</el-form-item>
      <el-form-item label="修改者">{{ info.updatedUserInfo?.name }}</el-form-item>
      <el-form-item label="修改时间">{{ DateUtil.format(info.updatedTime) }}</el-form-item>
    </el-form>
  </el-drawer>
</template>

<style lang="scss" scoped>
</style>
