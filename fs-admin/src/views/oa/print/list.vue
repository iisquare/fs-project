<script setup lang="ts">
import { onMounted, ref } from 'vue'
import type { FormInstance, TableInstance } from 'element-plus'
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router'
import ApiUtil from '@/utils/ApiUtil'
import TableUtil from '@/utils/TableUtil'
import PrintApi from '@/api/oa/PrintApi'

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

const formRef = ref<FormInstance>()
const formVisible = ref(false)
const formLoading = ref(false)
const form = ref<any>({})
const formRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

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

const handleAdd = () => {
  form.value = { sort: 0 }
  formVisible.value = true
}

const handleEdit = (scope: any) => {
  form.value = Object.assign({}, scope.row, { status: scope.row.status + '' })
  formVisible.value = true
}

const handleShow = (scope: any) => {
  infoRow.value = Object.assign({}, scope.row, {
    description: scope.row.description ? scope.row.description : '暂无',
  })
  infoVisible.value = true
}

const handleModel = (scope: any, env: Event) => {
  RouteUtil.forward(route, router, env, {
    path: '/oa/print/model',
    query: { id: scope.row.id },
  })
}

const handleSubmit = () => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid || formLoading.value) return
    formLoading.value = true
    PrintApi.save({
      id: form.value.id,
      name: form.value.name,
      sort: form.value.sort,
      status: form.value.status,
      description: form.value.description,
    }, { success: true }).then(() => {
      formVisible.value = false
      handleRefresh(false, true)
    }).catch(() => {}).finally(() => {
      formLoading.value = false
    })
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
      <form-search-item>
        <el-button type="primary" @click="handleRefresh(true, false)">查询</el-button>
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
      <el-table-column label="操作">
        <template #default="scope">
          <el-button link v-permit="'oa:print:'" @click="() => handleShow(scope)">查看</el-button>
          <el-button link v-permit="'oa:print:modify'" @click="() => handleEdit(scope)">编辑</el-button>
          <el-button link @click="(e: any) => handleModel(scope, e)">模型</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
  </el-card>

  <el-dialog v-model="formVisible" :title="form.id ? `信息修改 - ${form.id}` : '信息添加'" :close-on-click-modal="false" width="500">
    <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
      <el-form-item label="ID" prop="id">
        <el-input v-model="form.id" :disabled="!!form.id" />
      </el-form-item>
      <el-form-item label="名称" prop="name">
        <el-input v-model="form.name" />
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="form.sort" :min="0" :max="200" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="form.status" placeholder="请选择">
          <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
        </el-select>
      </el-form-item>
      <el-form-item label="描述">
        <el-input type="textarea" v-model="form.description" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="formVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="formLoading">确定</el-button>
    </template>
  </el-dialog>

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
