<script setup lang="ts">
import { onMounted, ref } from 'vue';
import type { FormInstance, TableInstance } from 'element-plus';
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router';
import ApplicationApi from '@/api/member/ApplicationApi';
import ApiUtil from '@/utils/ApiUtil';
import DateUtil from '@/utils/DateUtil';
import LayoutIcon from '@/components/Layout/LayoutIcon.vue';
import MenuApi from '@/api/member/MenuApi';
import TableUtil from '@/utils/TableUtil';
import UIUtil from '@/utils/UIUtil';

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'serial', label: '标识' },
  { prop: 'name', label: '名称' },
  { prop: 'url', label: '链接', slot: 'url' },
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
const pagination = ref(RouteUtil.pagination(filters.value, { pageSize: 100 }))
const selection = ref([])
const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  ApplicationApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
onMounted(() => {
  handleRefresh(false, true)
  ApplicationApi.config().then(result => {
    Object.assign(config.value, { ready: true }, ApiUtil.data(result))
  })
})
const infoVisible = ref(false)
const formVisible = ref(false)
const formLoading = ref(false)
const form: any = ref({})
const formRef: any = ref<FormInstance>()
const rules = ref({
  serial: [{ required: true, message: '请输入标识', trigger: 'blur' }],
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
    lockedTime: DateUtil.format(scope.row.lockedTime),
    roleIds: scope.row.roles ? scope.row.roles.map((item: any) => item.id) : []
  })
  formVisible.value = true
}
const handleSubmit = () => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid || formLoading.value) return
    formLoading.value = true
    ApplicationApi.save(form.value, { success: true }).then(result => {
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
    ApplicationApi.delete(ids, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {})
  }).catch(() => {})
}
const handleTree = (type: string, scope: any) => {
  router.push({ path: '/member/application/' + type, query: { id: scope.row.id } })
}
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="标识" prop="serial">
        <el-input v-model="filters.serial" clearable />
      </form-search-item>
      <form-search-item label="名称" prop="name">
        <el-input v-model="filters.name" clearable />
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
        <button-add v-permit="'member:application:add'" @click="handleAdd" />
        <button-delete v-permit="'member:application:delete'" :disabled="selection.length === 0" @click="handleDelete" />
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
        <template #url="scope">
          <LayoutIcon :name="scope.row.icon" />
          <RouterLink :to="scope.row.url" :title="scope.row.url" target="_blank">{{ scope.row.target ? scope.row.target : (scope.row.url ? '_self' : '无链接') }}</RouterLink>
        </template>
      </TableColumn>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button link @click="handleShow(scope)" v-permit="'member:application:'">查看</el-button>
          <el-button link @click="handleEdit(scope)" v-permit="'member:application:modify'">编辑</el-button>
          <el-button link @click="handleTree('resource', scope)" v-permit="'member:application:resource'">资源</el-button>
          <el-button link @click="handleTree('menu', scope)" v-permit="'member:application:menu'">菜单</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
  </el-card>
  <el-drawer v-model="infoVisible" :title="'信息查看 - ' + form.id">
    <el-form :model="form" label-width="auto">
      <el-form-item label="标识">{{ form.serial }}</el-form-item>
      <el-form-item label="名称">{{ form.name }}</el-form-item>
      <el-form-item label="图标"><LayoutIcon :name="form.icon" />{{ form.icon }}</el-form-item>
      <el-form-item label="链接">{{ form.url }}</el-form-item>
      <el-form-item label="打开方式">{{ form.target }}</el-form-item>
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
      <el-form-item label="标识" prop="serial">
        <el-input v-model="form.serial" />
      </el-form-item>
      <el-form-item label="名称" prop="name">
        <el-input v-model="form.name" />
      </el-form-item>
      <el-form-item label="图标" prop="icon">
        <el-input v-model="form.icon" />
      </el-form-item>
      <el-form-item label="链接" prop="url">
        <el-input v-model="form.url" placeholder="为空时不在面板展示该应用" />
      </el-form-item>
      <el-form-item label="打开方式" prop="target">
        <el-autocomplete v-model="form.target" :fetch-suggestions="query => UIUtil.filterSuggestions(MenuApi.target(), query)">
          <template #default="{ item }">{{ item.value }} - {{ item.description }}</template>
        </el-autocomplete>
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
