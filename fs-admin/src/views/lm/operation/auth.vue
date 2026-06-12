<script setup lang="ts">
import { onMounted, ref } from 'vue'
import type { FormInstance, TableInstance } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import UserApi from '@/api/member/UserApi'
import AuthApi from '@/api/lm/AuthApi'
import ModelApi from '@/api/lm/ModelApi'
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
  { prop: 'secret', label: '密钥', slot: 'secret' },
  { prop: 'uid', label: '所属用户', hide: true },
  { prop: 'uidUserInfo.serial', label: '用户账号' },
  { prop: 'uidUserInfo.name', label: '用户昵称' },
  { prop: 'modelIds', label: '模型限制', slot: 'modelIds' },
  { prop: 'statusText', label: '状态' },
  { prop: 'expiredTime', label: '过期时间', formatter: DateUtil.render },
  { prop: 'createdTime', label: '创建时间', formatter: DateUtil.render, hide: true },
  { prop: 'updatedTime', label: '修改时间', formatter: DateUtil.render, hide: true },
  { prop: 'deletedTime', label: '删除时间', formatter: DateUtil.render, hide: true },
])
const config = ref({
  ready: false,
  status: {},
})
const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, { advanced: false, modelIds: [], deleted: 'without' }))
const pagination = ref(RouteUtil.pagination(filters.value))
const selection = ref([])

const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  AuthApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

onMounted(() => {
  handleRefresh(false, true)
  AuthApi.config().then((result: any) => {
    Object.assign(config.value, { ready: true }, ApiUtil.data(result))
  }).catch(() => {})
})

const infoVisible = ref(false)
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
    modelIds: [],
    status: 'valid',
  }
  formVisible.value = true
}

const handleShow = (scope: any) => {
  form.value = Object.assign({}, scope.row)
  infoVisible.value = true
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
    AuthApi.save(form.value, { success: true }).then(() => {
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
    AuthApi.delete(ids, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {})
  }).catch(() => {})
}
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="" prop="deleted">
        <form-deleted v-model="filters.deleted" @change="handleRefresh(true, false)" />
      </form-search-item>
      <form-search-item label="所属用户" prop="uid">
        <form-select v-model="filters.uid" :callback="UserApi.list" clearable />
      </form-search-item>
      <form-search-item label="名称" prop="name">
        <el-input v-model="filters.name" clearable />
      </form-search-item>
      <form-search-item>
        <el-button type="primary" @click="handleRefresh(true, false)">查询</el-button>
        <el-button @click="filterRef?.resetFields()">重置</el-button>
        <button-advanced v-model="filters.advanced" />
      </form-search-item>
      <template v-if="filters.advanced">
        <form-search-item label="密钥" prop="secret">
          <el-input v-model="filters.secret" clearable />
        </form-search-item>
        <form-search-item label="状态" prop="status">
          <el-select v-model="filters.status" placeholder="请选择" clearable>
            <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
          </el-select>
        </form-search-item>
      </template>
    </form-search>
  </el-card>

  <el-card :bordered="false" shadow="never" class="fs-table-card">
    <div class="fs-table-toolbar flex-between">
      <el-space>
        <button-add v-permit="'lm:auth:add'" @click="handleAdd" />
        <button-delete v-permit="'lm:auth:delete'" :disabled="selection.length === 0" @click="handleDelete" />
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
        <template #secret="scope">
          <form-password v-model="scope.row.secret" level="medium" />
        </template>
        <template #modelIds="scope">
          <el-space wrap v-if="scope.row.models?.length">
            <el-tag v-for="item in scope.row.models" :key="item.id">{{ item.name }}</el-tag>
          </el-space>
          <el-tag type="info" v-else>不限</el-tag>
        </template>
      </TableColumn>
      <el-table-column label="操作" width="120">
        <template #default="scope">
          <el-button link @click="handleShow(scope)" v-permit="'lm:auth:'">查看</el-button>
          <el-button link @click="handleEdit(scope)" v-if="scope.row.deletedTime == 0" v-permit="'lm:auth:modify'">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
  </el-card>

  <el-drawer v-model="infoVisible" :title="'信息查看 - ' + form.id" size="70%">
    <el-descriptions border :column="2">
      <el-descriptions-item label="名称">{{ form.name }}</el-descriptions-item>
      <el-descriptions-item label="密钥"><form-password v-model="form.secret" level="medium" /></el-descriptions-item>
      <el-descriptions-item label="所属用户">{{ form.uid }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ form.statusText }}</el-descriptions-item>
      <el-descriptions-item label="用户账号">{{ form.uidUserInfo?.serial }}</el-descriptions-item>
      <el-descriptions-item label="用户昵称">{{ form.uidUserInfo?.name }}</el-descriptions-item>
      <el-descriptions-item label="模型限制" :span="2">
        <el-space wrap v-if="form.models?.length">
          <el-tag v-for="item in form.models" :key="item.id">{{ item.name }}</el-tag>
        </el-space>
        <span v-else>不限</span>
      </el-descriptions-item>
      <el-descriptions-item label="过期时间" :span="2">{{ DateUtil.format(form.expiredTime) }}</el-descriptions-item>
      <el-descriptions-item label="创建者">{{ form.createdUserInfo?.name }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ DateUtil.format(form.createdTime) }}</el-descriptions-item>
      <el-descriptions-item label="修改者">{{ form.updatedUserInfo?.name }}</el-descriptions-item>
      <el-descriptions-item label="修改时间">{{ DateUtil.format(form.updatedTime) }}</el-descriptions-item>
      <el-descriptions-item label="删除者">{{ form.deletedUserInfo?.name }}</el-descriptions-item>
      <el-descriptions-item label="删除时间">{{ DateUtil.format(form.deletedTime) }}</el-descriptions-item>
    </el-descriptions>
  </el-drawer>

  <el-drawer v-model="formVisible" :close-on-click-modal="false" :show-close="false" :destroy-on-close="true">
    <template #header="{ close, titleId, titleClass }">
      <h4 :id="titleId" :class="titleClass">{{ '授权密钥' + (form.id ? ('修改 - ' + form.id) : '添加') }}</h4>
      <el-space>
        <el-button type="primary" @click="handleSubmit" :loading="formLoading">确定</el-button>
        <el-button @click="close">取消</el-button>
      </el-space>
    </template>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="auto">
      <el-form-item prop="name" label="名称">
        <el-input v-model="form.name" />
      </el-form-item>
      <el-form-item prop="status" label="状态">
        <el-select v-model="form.status" placeholder="请选择">
          <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
        </el-select>
      </el-form-item>
      <el-form-item prop="uid" label="所属用户">
        <form-select v-model="form.uid" :callback="UserApi.list" clearable />
      </el-form-item>
      <el-form-item prop="secret" label="密钥">
        <el-input v-model="form.secret" type="password" show-password placeholder="留空时自动生成" />
      </el-form-item>
      <el-form-item prop="modelIds" label="模型限制">
        <form-select v-model="form.modelIds" :callback="ModelApi.list" multiple clearable />
      </el-form-item>
      <el-form-item prop="expiredTime" label="过期时间">
         <form-date-picker v-model="form.expiredTime" placeholder="留空为永久有效" />
      </el-form-item>
    </el-form>
  </el-drawer>
</template>

<style lang="scss" scoped>
</style>
