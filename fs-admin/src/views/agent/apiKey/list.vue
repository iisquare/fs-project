<script setup lang="ts">
import { onMounted, ref } from 'vue'
import type { FormInstance, TableInstance } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import AgenticApi from '@/api/agent/AgenticApi'
import ApiKeyApi from '@/api/agent/ApiKeyApi'
import UserApi from '@/api/member/UserApi'
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
  { prop: 'name', label: '名称' },
  { prop: 'description', label: '说明', hide: true },
  { prop: 'secret', label: '密钥', slot: 'secret' },
  { prop: 'uidUserInfo.name', label: '所属用户' },
  { prop: 'agenticIds', label: '授权流程', slot: 'agentics' },
  { prop: 'statusText', label: '状态' },
  { prop: 'lastUsedTime', label: '最后使用', formatter: DateUtil.render },
  { prop: 'expiredTime', label: '过期时间', formatter: DateUtil.render },
  { prop: 'createdTime', label: '创建时间', formatter: DateUtil.render, hide: true },
  { prop: 'updatedTime', label: '修改时间', formatter: DateUtil.render, hide: true },
])
const config = ref({ sorts: {}, status: {} })
const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, { advanced: false, uid: '', status: '', secret: '' }))
const pagination = ref(RouteUtil.pagination(filters.value))
const selection: any = ref([])

const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  ApiKeyApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = ApiUtil.data(result)?.rows ?? []
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

onMounted(() => {
  handleRefresh(false, true)
  ApiKeyApi.config().then((result: any) => {
    Object.assign(config.value, ApiUtil.data(result) ?? {})
  }).catch(() => {})
})

const formVisible = ref(false)
const formLoading = ref(false)
const form: any = ref({})
const formRef = ref<FormInstance>()
const rules = ref({
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  uid: [{ required: true, message: '请选择所属用户', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
})

const handleAdd = () => {
  form.value = {
    name: '',
    description: '',
    secret: '',
    uid: '',
    agenticIds: [],
    status: 1,
  }
  formVisible.value = true
}

const handleEdit = (scope: any) => {
  form.value = Object.assign({}, scope.row, {
    expiredTime: DateUtil.format(scope.row.expiredTime),
  })
  formVisible.value = true
}

const handleSubmit = () => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid || formLoading.value) return
    formLoading.value = true
    ApiKeyApi.save(form.value, { success: true }).then(() => {
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
    ApiKeyApi.delete(ids, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {
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
      <form-search-item label="所属用户" prop="uid">
        <form-select v-model="filters.uid" :callback="UserApi.list" clearable />
      </form-search-item>
      <form-search-item label="状态" prop="status">
        <el-select v-model="filters.status" placeholder="请选择" clearable>
          <el-option v-for="(value, key) in config.status" :key="key" :value="Number(key)" :label="value" />
        </el-select>
      </form-search-item>
      <form-search-item>
        <el-button type="primary" @click="handleRefresh(true, false)" :loading="loading">查询</el-button>
        <el-button @click="filterRef?.resetFields()">重置</el-button>
      </form-search-item>
      <form-search-item label="密钥" prop="secret">
        <el-input v-model="filters.secret" clearable />
      </form-search-item>
    </form-search>
  </el-card>

  <el-card :bordered="false" shadow="never" class="fs-table-card">
    <div class="fs-table-toolbar flex-between">
      <el-space>
        <button-add v-permit="'agent:apiKey:add'" @click="handleAdd" />
        <button-delete v-permit="'agent:apiKey:delete'" :disabled="selection.length === 0" @click="handleDelete" />
      </el-space>
      <el-space>
        <button-search @click="searchable = !searchable" />
        <button-refresh @click="handleRefresh(true, true)" :loading="loading" />
        <TableColumnSetting v-model="columns" :table="tableRef" :loading="loading" />
        <TableSort v-model="filters.sort" :columns="columns" :sortable="config.sorts" :loading="loading" @change="handleRefresh(true, true)" />
      </el-space>
    </div>
    <el-table
      ref="tableRef"
      :data="rows"
      :row-key="(record: any) => record.id"
      border
      v-loading="loading"
      table-layout="auto"
      @selection-change="(s: any) => selection = s"
    >
      <el-table-column type="selection" />
      <TableColumn :columns="columns">
        <template #secret="scope">
          <form-password v-model="scope.row.secret" level="medium" />
        </template>
        <template #agentics="scope">
          <el-tag v-if="!scope.row.agentics?.length" type="info">全部流程</el-tag>
          <el-space v-else wrap :size="4">
            <el-tag v-for="item in scope.row.agentics" :key="item.id" size="small">{{ item.name }}</el-tag>
          </el-space>
        </template>
      </TableColumn>
      <el-table-column label="操作" width="80">
        <template #default="scope">
          <el-button link v-permit="'agent:apiKey:modify'" @click="handleEdit(scope)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" :loading="loading" @change="handleRefresh(true, true)" />
  </el-card>

  <el-drawer v-model="formVisible" :close-on-click-modal="false" :show-close="false" :destroy-on-close="true">
    <template #header="{ close, titleId, titleClass }">
      <h4 :id="titleId" :class="titleClass">{{ form.id ? 'API Key 修改 - ' + form.id : 'API Key 添加' }}</h4>
      <el-space>
        <el-button type="primary" @click="handleSubmit" :loading="formLoading">确定</el-button>
        <el-button @click="close">取消</el-button>
      </el-space>
    </template>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="auto">
      <el-form-item prop="name" label="名称">
        <el-input v-model="form.name" maxlength="128" placeholder="请输入名称" />
      </el-form-item>
      <el-form-item prop="status" label="状态">
        <el-select v-model="form.status" placeholder="请选择状态">
          <el-option v-for="(value, key) in config.status" :key="key" :value="Number(key)" :label="value" />
        </el-select>
      </el-form-item>
      <el-form-item prop="uid" label="所属用户">
        <form-select v-model="form.uid" :callback="UserApi.list" clearable placeholder="请选择所属用户" />
      </el-form-item>
      <el-form-item prop="secret" label="密钥">
        <el-input v-model="form.secret" type="password" show-password maxlength="64" placeholder="留空时自动生成" />
      </el-form-item>
      <el-form-item prop="agenticIds" label="允许调用的流程">
        <form-select v-model="form.agenticIds" :callback="AgenticApi.list" multiple clearable placeholder="留空表示全部流程" />
      </el-form-item>
      <el-form-item prop="expiredTime" label="过期时间">
        <form-date-picker v-model="form.expiredTime" placeholder="留空为永久有效" />
      </el-form-item>
      <el-form-item prop="description" label="说明">
        <el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" placeholder="请输入说明" />
      </el-form-item>
    </el-form>
  </el-drawer>
</template>

<style lang="scss" scoped>
</style>
