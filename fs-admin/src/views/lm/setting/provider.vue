<script setup lang="ts">
import { onMounted, ref } from 'vue';
import type { FormInstance, TableInstance } from 'element-plus';
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router';
import ProviderApi from '@/api/lm/ProviderApi';
import ApiUtil from '@/utils/ApiUtil';
import DateUtil from '@/utils/DateUtil';
import TableUtil from '@/utils/TableUtil';
import MarkdownEditor from '@/components/Editor/MarkdownEditor.vue';

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'typeText', label: '供应商类型' },
  { prop: 'name', label: '供应商名称' },
  { prop: 'serial', label: '唯一标识' },
  { prop: 'endpoint', label: '调用地址' },
  { prop: 'sort', label: '排序' },
  { prop: 'statusText', label: '状态' },
])
const config = ref({
  ready: false,
  types: {},
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
  ProviderApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
onMounted(() => {
  handleRefresh(false, true)
  ProviderApi.config().then((result: any) => {
    Object.assign(config.value, { ready: true }, ApiUtil.data(result))
  }).catch(() => {})
})
const infoVisible = ref(false)
const formVisible = ref(false)
const formLoading = ref(false)
const form: any = ref({})
const formRef: any = ref<FormInstance>()
const rules = ref({
  type: [{ required: true, message: '请输入供应商类型', trigger: 'change' }],
  serial: [{ required: true, message: '请输入唯一标识', trigger: 'blur' }],
  name: [{ required: true, message: '请输入供应商名称', trigger: 'blur' }],
  endpoint: [{ required: true, message: '请输入调用地址', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
})
const handleAdd = () => {
  form.value = {
    status: '1',
    content: {},
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
    ProviderApi.save(form.value, { success: true }).then(result => {
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
    ProviderApi.delete(ids, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {
      loading.value = false
    })
  }).catch(() => {})
}
const handleModel = (scope: any, env: any) => {
  RouteUtil.forward(route, router, env, {
    path: '/lm/setting/model',
    query: RouteUtil.filter({ providerId: scope.row.id })
  })
}
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="名称" prop="name">
        <el-input v-model="filters.name" clearable />
      </form-search-item>
      <form-search-item label="标识" prop="serial">
        <el-input v-model="filters.serial" clearable />
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
        <button-add v-permit="'lm:provider:add'" @click="handleAdd" />
        <button-delete v-permit="'lm:provider:delete'" :disabled="selection.length === 0" @click="handleDelete" />
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
      @selection-change="(s: any) => selection = s"
    >
      <el-table-column type="selection" />
      <TableColumn :columns="columns">
      </TableColumn>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button link @click="handleShow(scope)" v-permit="'lm:provider:'">查看</el-button>
          <el-button link @click="handleEdit(scope)" v-permit="'lm:provider:modify'">编辑</el-button>
          <el-button link @click="env => handleModel(scope, env)" v-permit="'lm:model:'">模型</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
  </el-card>
  <el-drawer v-model="infoVisible" :title="'信息查看 - ' + form.id" size="80%">
    <el-form :model="form" label-width="auto">
      <el-descriptions border>
        <el-descriptions-item label="供应商类型">{{ form.typeText }}</el-descriptions-item>
        <el-descriptions-item label="供应商名称">{{ form.name }}</el-descriptions-item>
        <el-descriptions-item label="供应商标识">{{ form.serial }}</el-descriptions-item>
        <el-descriptions-item label="调用地址" :span="3">{{ form.endpoint }}</el-descriptions-item>
        <el-descriptions-item label="认证标识"><form-password v-model="form.token" level="medium" /></el-descriptions-item>
        <el-descriptions-item label="排序">{{ form.sort }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ form.statusText }}</el-descriptions-item>
        <el-descriptions-item label="官网地址" :span="3">{{ form.website }}</el-descriptions-item>
        <el-descriptions-item label="创建者">{{ form.createdUserInfo?.name }}</el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ DateUtil.format(form.createdTime) }}</el-descriptions-item>
        <el-descriptions-item label="修改者">{{ form.updatedUserInfo?.name }}</el-descriptions-item>
        <el-descriptions-item label="修改时间" :span="2">{{ DateUtil.format(form.updatedTime) }}</el-descriptions-item>
        <el-descriptions-item label="描述信息" :span="3"><MarkdownEditor v-model="form.description" readonly /></el-descriptions-item>
      </el-descriptions>
    </el-form>
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
      <el-descriptions border>
        <el-descriptions-item label="供应商类型">
          <el-select v-model="form.type" placeholder="请选择">
            <el-option v-for="(value, key) in config.types" :key="key" :value="key" :label="value" />
          </el-select>
        </el-descriptions-item>
        <el-descriptions-item label="唯一标识"><el-input v-model="form.serial" /></el-descriptions-item>
        <el-descriptions-item label="供应商名称"><el-input v-model="form.name" /></el-descriptions-item>
        <el-descriptions-item label="调用地址" :span="3"><el-input type="textarea" v-model="form.endpoint" /></el-descriptions-item>
        <el-descriptions-item label="认证标识"><el-input  v-model="form.token"  type="password" placeholder="请输入认证标识" show-password /></el-descriptions-item>
        <el-descriptions-item label="排序"><el-input-number v-model="form.sort" /></el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-select v-model="form.status" placeholder="请选择">
            <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
          </el-select>
        </el-descriptions-item>
        <el-descriptions-item label="官网链接" :span="3"><el-input type="textarea" v-model="form.website" /></el-descriptions-item>
        <el-descriptions-item label="描述信息" :span="3"><MarkdownEditor v-model="form.description" /></el-descriptions-item>
      </el-descriptions>
    </el-form>
  </el-drawer>
</template>

<style lang="scss" scoped>
</style>
