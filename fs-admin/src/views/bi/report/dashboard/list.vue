<script setup lang="ts">
import { onMounted, ref } from 'vue'
import type { FormInstance, TableInstance } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import DashboardApi from '@/api/bi/DashboardApi'
import ApiUtil from '@/utils/ApiUtil'
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
  { prop: 'name', label: '大屏名称' },
  { prop: 'sort', label: '排序' },
  { prop: 'statusText', label: '状态' },
  { prop: 'description', label: '描述', hide: true },
  { prop: 'createdUidName', label: '创建者', hide: true },
  { prop: 'createdTime', label: '创建时间', hide: true, formatter: DateUtil.render },
  { prop: 'updatedTime', label: '修改时间', hide: true, formatter: DateUtil.render },
])
const config = ref<any>({ ready: false, status: {} })
const rows = ref<any[]>([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, { advanced: false }))
const pagination = ref(RouteUtil.pagination(filters.value))
const selection = ref<any[]>([])
const infoVisible = ref(false)
const formVisible = ref(false)
const formLoading = ref(false)
const form = ref<any>({})
const formRef = ref<FormInstance>()
const rules = {
  name: [{ required: true, message: '请输入大屏名称', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

const handleRefresh = (toQuery: boolean, keepPage: boolean) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  toQuery && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  DashboardApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data?.rows || []
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

onMounted(() => {
  handleRefresh(false, true)
  DashboardApi.config().then((result: any) => {
    Object.assign(config.value, { ready: true }, ApiUtil.data(result))
  }).catch(() => {})
})

const handleAdd = () => {
  form.value = { status: '1', sort: 0, description: '' }
  formVisible.value = true
}
const handleShow = (row: any) => {
  form.value = { ...row }
  infoVisible.value = true
}
const handleEdit = (row: any) => {
  form.value = { ...row, status: String(row.status) }
  formVisible.value = true
}
const handleSubmit = () => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid || formLoading.value) return
    formLoading.value = true
    DashboardApi.save(form.value, { success: true }).then((result: any) => {
      if (ApiUtil.succeed(result)) {
        formVisible.value = false
        handleRefresh(false, true)
        if (!form.value.id && result.data?.id) {
          router.push({ path: '/bi/design/dashboard', query: { id: result.data.id } }).catch(() => {})
        }
      }
    }).catch(() => {}).finally(() => {
      formLoading.value = false
    })
  })
}
const handleDelete = () => {
  TableUtil.selection(selection.value).then((ids: any) => {
    loading.value = true
    DashboardApi.delete(ids, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {
      loading.value = false
    })
  }).catch(() => {})
}
const handleDesign = (row: any) => {
  router.push({ path: '/bi/design/dashboard', query: { id: row.id } }).catch(() => {})
}
const handlePreview = (row: any) => {
  const target = router.resolve({ path: '/bi/dashboard/view', query: { id: row.id } })
  window.open(target.href, '_blank')
}
const handleCreate = () => {
  handleAdd()
}
</script>

<template>
  <el-card v-show="searchable" :bordered="false" shadow="never" class="fs-table-search">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="名称" prop="name">
        <el-input v-model="filters.name" clearable placeholder="请输入大屏名称" />
      </form-search-item>
      <form-search-item>
        <el-button type="primary" :loading="loading" @click="handleRefresh(true, false)">查询</el-button>
        <el-button @click="filterRef?.resetFields()">重置</el-button>
      </form-search-item>
    </form-search>
  </el-card>

  <el-card :bordered="false" shadow="never" class="fs-table-card">
    <div class="fs-table-toolbar flex-between">
      <el-space>
        <el-button v-permit="'bi:dashboard:add'" type="primary" :icon="Plus" @click="handleCreate">新建大屏</el-button>
        <button-delete v-permit="'bi:dashboard:delete'" :disabled="selection.length === 0" @click="handleDelete" />
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
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link v-permit="'bi:dashboard:'" @click="handleShow(row)">查看</el-button>
          <el-button link type="primary" v-permit="'bi:dashboard:modify'" @click="handleDesign(row)">设计</el-button>
          <el-button link v-permit="'bi:dashboard:'" @click="handlePreview(row)">预览</el-button>
          <el-button link v-permit="'bi:dashboard:modify'" @click="handleEdit(row)">设置</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" :loading="loading" @change="handleRefresh(true, true)" />
  </el-card>

  <el-drawer v-model="infoVisible" :title="'大屏信息 - ' + form.name" size="50%">
    <layout-heading title="基础信息" />
    <el-descriptions :column="2" label-width="100px" border>
      <el-descriptions-item label="名称">{{ form.name }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ form.statusText }}</el-descriptions-item>
      <el-descriptions-item label="排序">{{ form.sort }}</el-descriptions-item>
      <el-descriptions-item label="创建者">{{ form.createdUidName || '暂无' }}</el-descriptions-item>
      <el-descriptions-item label="描述" :span="2">{{ form.description || '暂无' }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ DateUtil.format(form.createdTime) }}</el-descriptions-item>
      <el-descriptions-item label="修改时间">{{ DateUtil.format(form.updatedTime) }}</el-descriptions-item>
    </el-descriptions>
  </el-drawer>

  <el-drawer v-model="formVisible" :close-on-click-modal="false" :show-close="false" destroy-on-close size="50%">
    <template #header="{ close, titleId, titleClass }">
      <h4 :id="titleId" :class="titleClass">{{ form.id ? '修改大屏设置' : '新建数据大屏' }}</h4>
      <el-space>
        <el-button type="primary" :loading="formLoading" @click="handleSubmit">确定</el-button>
        <el-button @click="close">取消</el-button>
      </el-space>
    </template>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="auto">
      <el-form-item label="名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入大屏名称" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="form.status" placeholder="请选择状态">
          <el-option v-for="(label, value) in config.status" :key="value" :label="label" :value="value" />
        </el-select>
      </el-form-item>
      <el-form-item label="排序">
        <form-input-number v-model="form.sort" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入大屏说明" />
      </el-form-item>
    </el-form>
  </el-drawer>
</template>
