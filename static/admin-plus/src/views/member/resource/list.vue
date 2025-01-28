<script setup lang="ts">
import { onMounted, ref } from 'vue';
import type { FormInstance, TableInstance } from 'element-plus';
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router';
import ResourceApi from '@/api/member/ResourceApi';
import ApiUtil from '@/utils/ApiUtil';
import DateUtil from '@/utils/DateUtil';
import ApplicationApi from '@/api/member/ApplicationApi';
import TableUtil from '@/utils/TableUtil';

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'name', label: '名称' },
  { prop: 'fullName', label: '全称' },
  { prop: 'applicationId', label: '应用', slot: 'applicationId' },
  { prop: 'parentId', label: '父级', slot: 'parentId' },
  { prop: 'module', label: '模块' },
  { prop: 'controller', label: '控制器' },
  { prop: 'action', label: '动作' },
  { prop: 'sort', label: '排序' },
  { prop: 'statusText', label: '状态' },
])
const config = ref({
  ready: false,
  status: {},
})
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
  ResourceApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
onMounted(() => {
  handleRefresh(false, true)
  ResourceApi.config().then(result => {
    Object.assign(config.value, { ready: true }, ApiUtil.data(result))
  })
})
const infoVisible = ref(false)
const formVisible = ref(false)
const formLoading = ref(false)
const form: any = ref({})
const formRef: any = ref<FormInstance>()
const rules = ref({
  applicationId: [{ required: true, message: '请选择所属应用', trigger: 'blur' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
})
const handleAdd = () => {
  form.value = {
    status: '1',
  }
  formVisible.value = true
}
const handleShow = (scope: any) => {
  form.value = Object.assign({}, scope.row)
  infoVisible.value = true
}
const handleEdit = (scope: any) => {
  form.value = Object.assign({}, scope.row, {
    status: scope.row.status + '',
  })
  formVisible.value = true
}
const handleSublevel = (scope: any) => {
  const record = scope.row
  form.value = {
    parentId: record.id,
    applicationId: record.applicationId,
    module: record.module,
    controller: record.controller,
    action: record.action,
    status: scope.row.status + '',
  }
  formVisible.value = true
}
const handleSubmit = () => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid || formLoading.value) return
    formLoading.value = true
    ResourceApi.save(form.value, { success: true }).then(result => {
      handleRefresh(false, true)
      formVisible.value = false
    })
  }).catch(() => {}).finally(() => {
    formLoading.value = false
  })
}
const handleDelete = () => {
  TableUtil.selection(selection.value).then((ids: any) => {
    loading.value = true
    ResourceApi.delete(ids, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {})
  }).catch(() => {})
}
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="全称" prop="fullName">
        <el-input v-model="filters.fullName" clearable />
      </form-search-item>
      <form-search-item label="应用" prop="applicationId">
        <form-select v-model="filters.applicationId" :callback="ApplicationApi.list" clearable />
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
        <form-search-item label="父级" prop="parentId">
          <el-input v-model="filters.parentId" clearable />
        </form-search-item>
        <form-search-item label="模块" prop="module">
          <el-input v-model="filters.module" clearable />
        </form-search-item>
        <form-search-item label="控制器" prop="controller">
          <el-input v-model="filters.controller" clearable />
        </form-search-item>
        <form-search-item label="动作" prop="action">
          <el-input v-model="filters.parentId" clearable />
        </form-search-item>
      </template>
    </form-search>
  </el-card>
  <el-card :bordered="false" shadow="never" class="fs-table-card">
    <div class="fs-table-toolbar flex-between">
      <el-space>
        <button-add v-permit="'member:resource:add'" @click="handleAdd" />
        <button-delete v-permit="'member:resource:delete'" :disabled="selection.length === 0" @click="handleDelete" />
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
        <template #applicationId="{ row }">[{{ row.applicationId }}]{{ row.applicationInfo?.name }}</template>
        <template #parentId="{ row }">[{{ row.parentId }}]{{ row.parentId > 0 ? row.parentInfo?.name : '根节点' }}</template>
      </TableColumn>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button link @click="handleShow(scope)" v-permit="'member:resource:'">查看</el-button>
          <el-button link @click="handleEdit(scope)" v-permit="'member:resource:modify'">编辑</el-button>
          <el-button link @click="handleSublevel(scope)" v-permit="'member:resource:add'">子级</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
  </el-card>
  <el-drawer v-model="infoVisible" :title="'信息查看 - ' + form.id">
    <el-form :model="form" label-width="auto">
      <el-form-item label="父级">{{ form.parentId }}</el-form-item>
      <el-form-item label="名称">{{ form.name }}</el-form-item>
      <el-form-item label="全称">{{ form.fullName }}</el-form-item>
      <el-form-item label="模块">{{ form.module }}</el-form-item>
      <el-form-item label="控制器">{{ form.controller }}</el-form-item>
      <el-form-item label="动作">{{ form.action }}</el-form-item>
      <el-form-item label="资源">{{ form.module }}:{{ form.controller }}:{{ form.action }}</el-form-item>
      <el-form-item label="排序">{{ form.sort }}</el-form-item>
      <el-form-item label="状态">{{ form.statusText }}</el-form-item>
      <el-form-item label="描述">{{ form.description }}</el-form-item>
      <el-form-item label="创建者">{{ form.createdUserInfo?.name }}</el-form-item>
      <el-form-item label="创建时间">{{ DateUtil.format(form.createdTime) }}</el-form-item>
      <el-form-item label="修改者">{{ form.updatedUserInfo?.name }}</el-form-item>
      <el-form-item label="修改时间">{{ DateUtil.format(form.updatedTime) }}</el-form-item>
    </el-form>
  </el-drawer>
  <el-drawer v-model="formVisible" :close-on-click-modal="false" :show-close="false" :destroy-on-close="true">
    <template #header="{ close, titleId, titleClass }">
      <h4 :id="titleId" :class="titleClass">{{ '信息' + (form.id ? ('修改 - ' + form.id) : '添加') }}</h4>
      <el-space>
        <el-button type="primary" @click="handleSubmit" :loading="formLoading">确定</el-button>
        <el-button @click="close">取消</el-button>
      </el-space>
    </template>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="auto">
      <el-form-item label="ID" prop="id">
        <el-input v-model="form.id" />
      </el-form-item>
      <el-form-item label="应用" prop="applicationId">
        <form-select v-model="form.applicationId" :callback="ApplicationApi.list" />
      </el-form-item>
      <el-form-item label="父级" prop="parentId">
        <el-input v-model="form.parentId" />
      </el-form-item>
      <el-form-item label="名称" prop="name">
        <el-input v-model="form.name" />
      </el-form-item>
      <el-form-item label="模块" prop="module">
        <el-input v-model="form.module" />
      </el-form-item>
      <el-form-item label="控制器" prop="controller">
        <el-input v-model="form.controller" />
      </el-form-item>
      <el-form-item label="动作" prop="action">
        <el-input v-model="form.action" />
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="form.sort" />
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
  </el-drawer>
</template>

<style lang="scss" scoped>
</style>
