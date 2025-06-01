<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import type { FormInstance, TableInstance } from 'element-plus';
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router';
import DataPermitApi from '@/api/member/DataPermitApi';
import ApiUtil from '@/utils/ApiUtil';
import TableUtil from '@/utils/TableUtil';
import DataTable from '@/components/Data/DataTable.vue';
import DataApi from '@/api/member/DataApi';
import RoleApi from '@/api/member/RoleApi';
import DataFilter from '@/components/Data/DataFilter.vue';
import DataSelect from '@/components/Data/DataSelect.vue';

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'dataInfo.serial', label: '模型' },
  { prop: 'roleInfo.name', label: '角色' },
  { prop: 'fields', label: '列权限' },
  { prop: 'statusText', label: '状态' },
  { prop: 'description', label: '描述' },
])
const config = ref({
  ready: false,
  status: {},
  types: []
})
const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, { advanced: false, roleIds: [] }))
const pagination = ref(RouteUtil.pagination(filters.value, { pageSize: 100 }))
const selection = ref([])
const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  DataPermitApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
onMounted(() => {
  handleRefresh(false, true)
  DataPermitApi.config().then(result => {
    Object.assign(config.value, { ready: true }, ApiUtil.data(result))
  })
})
const infoVisible = ref(false)
const formVisible = ref(false)
const formLoading = ref(false)
const form: any = ref({})
const formRef: any = ref<FormInstance>()
const rules = ref({
  dataId: [{ required: true, message: '请选择模型', trigger: 'change' }],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
})
const handleAdd = () => {
  form.value = {
    dataId: filters.value.dataId,
    roleId: filters.value.roleId,
    status: '1',
    filters: [],
    fields: [],
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
const handleSubmit = () => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid || formLoading.value) return
    formLoading.value = true
    DataPermitApi.save(form.value, { success: true }).then(result => {
      handleRefresh(false, true)
      formVisible.value = false
    }).catch(() => {}).finally(() => {
      formLoading.value = false
    })
  })
}
const handleDelete = () => {
  TableUtil.selection(selection.value).then((ids: any) => {
    loading.value = true
    DataPermitApi.delete(ids, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {})
  }).catch(() => {})
}

const dataSelected: any = ref()
const fields = computed(() => {
  return dataSelected.value?.fields || []
})
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="模型" prop="dataId">
        <form-select v-model="filters.dataId" :callback="DataApi.list" clearable />
      </form-search-item>
      <form-search-item label="角色" prop="name">
        <form-select v-model="filters.roleId" :callback="RoleApi.list" clearable />
      </form-search-item>
      <form-search-item label="状态" prop="status">
        <el-select v-model="filters.status" placeholder="请选择" clearable>
          <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
        </el-select>
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
        <button-add v-permit="'member:dataPermit:add'" @click="handleAdd" />
        <button-delete v-permit="'member:dataPermit:delete'" :disabled="selection.length === 0" @click="handleDelete" />
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
          <el-button link @click="handleShow(scope)" v-permit="'member:dataPermit:'">查看</el-button>
          <el-button link @click="handleEdit(scope)" v-permit="'member:dataPermit:modify'">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
  </el-card>
  <el-drawer v-model="infoVisible" :title="'信息查看 - ' + form.id" size="80%">
    <el-descriptions border>
      <el-descriptions-item label="模型名称">{{ form.dataInfo?.name }}</el-descriptions-item>
      <el-descriptions-item label="模型编码">{{ form.dataInfo?.serial }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ form.statusText }}</el-descriptions-item>
      <el-descriptions-item label="角色名称" :span="2">{{ form.roleInfo?.name }}</el-descriptions-item>
      <el-descriptions-item label="排序">{{ form.sort }}</el-descriptions-item>
      <el-descriptions-item label="描述" :span="3">{{ form.description }}</el-descriptions-item>
    </el-descriptions>
    <el-divider content-position="left">行权限</el-divider>
    <DataFilter v-model:="form.filters" :fields="fields" />
    <el-divider content-position="left">列权限</el-divider>
    <DataSelect v-model="form.fields" :fields="fields" />
  </el-drawer>
  <el-drawer v-model="formVisible" :close-on-click-modal="false" :show-close="false" :destroy-on-close="true" size="80%">
    <template #header="{ close, titleId, titleClass }">
      <h4 :id="titleId" :class="titleClass">{{ '信息' + (form.id ? ('修改 - ' + form.id) : '添加') }}</h4>
      <el-space>
        <el-button type="primary" @click="handleSubmit" :loading="formLoading">确定</el-button>
        <el-button @click="close">取消</el-button>
      </el-space>
    </template>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="auto">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="模型" prop="dataId">
            <form-select v-model="form.dataId" v-model:selected="dataSelected" :callback="DataApi.list" clearable />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="角色" prop="roleId">
            <form-select v-model="form.roleId" :callback="RoleApi.list" clearable />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="状态" prop="status">
            <el-select v-model="form.status" placeholder="请选择">
              <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="排序">
            <el-input-number v-model="form.sort" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="描述">
            <el-input type="textarea" v-model="form.description" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <el-divider content-position="left">行权限</el-divider>
    <DataFilter v-model:="form.filters" :fields="fields" editable />
    <el-divider content-position="left">列权限</el-divider>
    <DataSelect v-model="form.fields" :fields="fields" editable />
  </el-drawer>
</template>

<style lang="scss" scoped>
.el-descriptions {
  margin-bottom: 20px;
}
</style>
