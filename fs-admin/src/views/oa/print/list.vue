<script setup lang="ts">
import { onMounted, ref } from 'vue'
import type { FormInstance, TableInstance } from 'element-plus'
import { ElMessage } from 'element-plus'
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router'
import ApiUtil from '@/utils/ApiUtil'
import TableUtil from '@/utils/TableUtil'
import PrintApi from '@/api/oa/PrintApi'
import FormFrameApi from '@/api/oa/FormFrameApi'

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'name', label: '名称' },
  { prop: 'frameIdName', label: '数据表单' },
  { prop: 'templateName', label: 'Word 模板' },
  { prop: 'hasTemplate', label: '文件状态', slot: 'template' },
  { prop: 'sort', label: '排序' },
  { prop: 'statusText', label: '状态', slot: 'status' },
])
const config = ref({
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
  PrintApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

onMounted(() => {
  handleRefresh(false, true)
  PrintApi.config().then((result: any) => {
    Object.assign(config.value, { ready: true }, ApiUtil.data(result))
  }).catch(() => {})
})

const handleAdd = (env?: Event) => {
  if (env) {
    RouteUtil.forward(route, router, env, { path: '/oa/print/model' })
    return
  }
  router.push({ path: '/oa/print/model' })
}

const handleEdit = (scope: any, env: Event) => {
  RouteUtil.forward(route, router, env, {
    path: '/oa/print/model',
    query: { id: scope.row.id },
  })
}

const handleShow = (scope: any) => {
  infoRow.value = Object.assign({}, scope.row, {
    description: scope.row.description ? scope.row.description : '暂无',
  })
  infoVisible.value = true
}

const handlePreview = (scope: any, env: Event) => {
  if (Number(scope.row.status) !== 1) {
    ElMessage.warning('请先启用该打印模板')
    return
  }
  RouteUtil.forward(route, router, env, {
    path: '/oa/print/pdf',
    query: { id: scope.row.id, sample: '1', returnTo: '/oa/print/list' },
  })
}

const handleDelete = () => {
  TableUtil.selection(selection.value).then((ids: any) => {
    loading.value = true
    PrintApi.delete(ids, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {}).finally(() => {
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
      <form-search-item label="数据表单" prop="frameId">
        <form-select v-model="filters.frameId" :callback="FormFrameApi.list" clearable placeholder="选择表单" />
      </form-search-item>
      <form-search-item label="状态" prop="status">
        <el-select v-model="filters.status" clearable placeholder="全部状态">
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
        <button-add v-permit="'oa:print:add'" @click="handleAdd" />
        <button-delete v-permit="'oa:print:delete'" :disabled="selection.length === 0" @click="handleDelete" />
      </el-space>
      <el-space>
        <button-search @click="searchable = !searchable" />
        <button-refresh @click="handleRefresh(true, true)" :loading="loading" />
        <TableColumnSetting v-model="columns" :table="tableRef" :loading="loading" />
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
      <TableColumn :columns="columns">
        <template #template="scope">
          <el-tag :type="scope.row.hasTemplate ? 'success' : 'warning'" effect="plain">
            {{ scope.row.hasTemplate ? '已上传' : '待上传' }}
          </el-tag>
        </template>
        <template #status="scope">
          <el-tag :type="Number(scope.row.status) === 1 ? 'success' : 'info'" effect="plain">
            {{ scope.row.statusText }}
          </el-tag>
        </template>
      </TableColumn>
      <el-table-column label="操作" width="176" align="center">
        <template #default="scope">
          <el-button link v-permit="'oa:print:'" @click="() => handleShow(scope)">查看</el-button>
          <el-button link v-permit="'oa:print:modify'" @click="(e: any) => handleEdit(scope, e)">
            {{ scope.row.hasTemplate ? '编辑' : '配置' }}
          </el-button>
          <el-tooltip
            :disabled="scope.row.hasTemplate && Number(scope.row.status) === 1"
            :content="!scope.row.hasTemplate ? '请先配置 Word 模板' : '请先启用该打印模板'"
            placement="top"
          >
            <el-button
              link
              type="primary"
              :disabled="!scope.row.hasTemplate || Number(scope.row.status) !== 1"
              @click="(e: any) => handlePreview(scope, e)"
            >
              预览
            </el-button>
          </el-tooltip>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" :loading="loading" @change="handleRefresh(true, true)" />
  </el-card>

  <el-dialog v-model="infoVisible" :title="`信息查看 - ${infoRow.id}`" width="500">
    <el-descriptions :column="1" border>
      <el-descriptions-item label="名称">{{ infoRow.name }}</el-descriptions-item>
      <el-descriptions-item label="数据表单">{{ infoRow.frameIdName || infoRow.frameId || '未关联' }}</el-descriptions-item>
      <el-descriptions-item label="Word 模板">{{ infoRow.templateName || '未上传' }}</el-descriptions-item>
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
