<script setup lang="ts">
import { onMounted, ref } from 'vue';
import type { FormInstance, TableInstance } from 'element-plus';
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router';
import SkillApi from '@/api/agent/SkillApi';
import ApiUtil from '@/utils/ApiUtil';
import DateUtil from '@/utils/DateUtil';
import TableUtil from '@/utils/TableUtil';
import RoleApi from '@/api/member/RoleApi';

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'name', label: '技能名称', slot: 'name' },
  { prop: 'typeText', label: '技能类型' },
  { prop: 'labels', label: '标签', slot: 'labels' },
  { prop: 'roles', label: '授权角色', slot: 'role' },
  { prop: 'sort', label: '排序' },
  { prop: 'statusText', label: '状态' },
  { prop: 'description', label: '描述', hide: true },
])
const config: any = ref({
  ready: false,
  sorts: {},
  status: {},
  types: {},
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
  SkillApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
onMounted(() => {
  handleRefresh(false, true)
  SkillApi.config().then((result: any) => {
    Object.assign(config.value, { ready: true }, ApiUtil.data(result))
  }).catch(() => {})
})
const infoVisible = ref(false)
const formVisible = ref(false)
const formLoading = ref(false)
const form: any = ref({})
const formRef: any = ref<FormInstance>()
const rules = ref({
  name: [{ required: true, message: '请输入技能名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择技能类型', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
})
const handleAdd = () => {
  form.value = {
    status: '1',
    labels: [],
    roleIds: [],
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
    roleIds: scope.row.roleIds || [],
    labels: scope.row.labels || [],
  })
  formVisible.value = true
}
const handleSubmit = () => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid || formLoading.value) return
    formLoading.value = true
    SkillApi.save(form.value, { success: true }).then(result => {
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
    SkillApi.delete(ids, { success: true }).then(() => {
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
      <form-search-item label="类型" prop="type">
        <el-select v-model="filters.type" placeholder="请选择" clearable>
          <el-option v-for="(value, key) in config.types" :key="key" :value="key" :label="value" />
        </el-select>
      </form-search-item>
      <form-search-item label="状态" prop="status">
        <el-select v-model="filters.status" placeholder="请选择" clearable>
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
        <button-add v-permit="'agent:skill:add'" @click="handleAdd" />
        <button-delete v-permit="'agent:skill:delete'" :disabled="selection.length === 0" @click="handleDelete" />
      </el-space>
      <el-space>
        <button-search @click="searchable = !searchable" />
        <button-refresh @click="handleRefresh(true, true)" :loading="loading" />
        <TableColumnSetting v-model="columns" :table="tableRef" />
        <TableSort v-model="filters.sort" :columns="columns" :sortable="config.sorts" @change="handleRefresh(true, true)" />
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
        <template #name="scope">
          <el-link type="primary" underline="never" @click="router.push({ path: '/agent/plugin/skillVersion', query: { skillId: scope.row.id } })">{{ scope.row.name }}</el-link>
        </template>
        <template #role="scope">
          <el-space><el-tag v-for="item in scope.row.roles" :key="item.id">{{ item.name }}</el-tag></el-space>
        </template>
        <template #labels="scope">
          <el-space><el-tag v-for="item in scope.row.labels" :key="item">{{ item }}</el-tag></el-space>
        </template>
      </TableColumn>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button link @click="handleShow(scope)" v-permit="'agent:skill:'">查看</el-button>
          <el-button link @click="handleEdit(scope)" v-permit="'agent:skill:modify'">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
  </el-card>
  <el-drawer v-model="infoVisible" :title="'信息查看 - ' + form.id" size="60%">
    <el-descriptions :column="2" label-width="100px" border>
      <el-descriptions-item label="技能名称">{{ form.name }}</el-descriptions-item>
      <el-descriptions-item label="技能类型">{{ form.typeText }}</el-descriptions-item>
      <el-descriptions-item label="标签">
        <el-space><el-tag v-for="item in form.labels" :key="item">{{ item }}</el-tag></el-space>
      </el-descriptions-item>
      <el-descriptions-item label="授权角色">
        <el-space><el-tag v-for="item in form.roles" :key="item.id">{{ item.name }}</el-tag></el-space>
      </el-descriptions-item>
      <el-descriptions-item label="排序">{{ form.sort }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ form.statusText }}</el-descriptions-item>
      <el-descriptions-item label="描述" :span="2">{{ form.description ? form.description : '暂无' }}</el-descriptions-item>
      <el-descriptions-item label="配置信息" :span="2">
        <CodeEditor v-model="form.content" :height="300" mode="javascript" resizable />
      </el-descriptions-item>
      <el-descriptions-item label="创建者">{{ form.createdUserInfo?.name }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ DateUtil.format(form.createdTime) }}</el-descriptions-item>
      <el-descriptions-item label="修改者">{{ form.updatedUserInfo?.name }}</el-descriptions-item>
      <el-descriptions-item label="修改时间">{{ DateUtil.format(form.updatedTime) }}</el-descriptions-item>
    </el-descriptions>
  </el-drawer>
  <el-drawer v-model="formVisible" :close-on-click-modal="false" :show-close="false" :destroy-on-close="true" size="60%">
    <template #header="{ close, titleId, titleClass }">
      <h4 :id="titleId" :class="titleClass">{{ '信息' + (form.id ? ('修改 - ' + form.id) : '添加') }}</h4>
      <el-space>
        <el-button type="primary" @click="handleSubmit" :loading="formLoading">确定</el-button>
        <el-button @click="close">取消</el-button>
      </el-space>
    </template>
    <el-form ref="formRef" :model="form" :rules="rules">
      <el-descriptions :column="2" label-width="100px" border>
        <el-descriptions-item label="技能名称"><el-input v-model="form.name" /></el-descriptions-item>
        <el-descriptions-item label="技能类型">
          <el-select v-model="form.type" placeholder="请选择">
            <el-option v-for="(value, key) in config.types" :key="key" :value="key" :label="value" />
          </el-select>
        </el-descriptions-item>
        <el-descriptions-item label="标签">
          <el-select v-model="form.labels" multiple filterable allow-create :reserve-keyword="false" default-first-option placeholder="输入后回车创建标签" />
        </el-descriptions-item>
        <el-descriptions-item label="授权角色">
          <form-select v-model="form.roleIds" :callback="RoleApi.list" multiple clearable />
        </el-descriptions-item>
        <el-descriptions-item label="排序"><el-input-number v-model="form.sort" /></el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-select v-model="form.status" placeholder="请选择">
            <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
          </el-select>
        </el-descriptions-item>
        <el-descriptions-item label="描述" :span="2"><el-input type="textarea" v-model="form.description" /></el-descriptions-item>
        <el-descriptions-item label="配置信息" :span="2">
          <CodeEditor v-model="form.content" :height="300" mode="javascript" resizable />
        </el-descriptions-item>
      </el-descriptions>
    </el-form>
  </el-drawer>
</template>

<style lang="scss" scoped>
</style>
