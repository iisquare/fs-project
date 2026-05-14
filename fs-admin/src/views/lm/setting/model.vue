<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import type { FormInstance, TableInstance } from 'element-plus';
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router';
import ModelApi from '@/api/lm/ModelApi';
import ApiUtil from '@/utils/ApiUtil';
import DateUtil from '@/utils/DateUtil';
import TableUtil from '@/utils/TableUtil';
import ProviderApi from '@/api/lm/ProviderApi';
import RoleApi from '@/api/member/RoleApi';
import { el } from 'element-plus/es/locales.mjs';

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'providerInfo.name', label: '供应商' },
  { prop: 'name', label: '模型名称' },
  { prop: 'alias', label: '模型别名', slot: 'alias' },
  { prop: 'typeText', label: '模型类型' },
  { prop: 'explorable', label: '模型广场' },
  { prop: 'allVisible', label: '全部可见' },
  { prop: 'securityDetectable', label: '安全围栏' },
  { prop: 'planText', label: '计费方案' },
  { prop: 'role', label: '授权角色', slot: 'role' },
  { prop: 'sort', label: '排序' },
  { prop: 'statusText', label: '状态' },
])
const config: any = ref({
  ready: false,
  status: {},
  types: {},
  plans: {},
})
const parameters = computed(() => {
  const plans = config.value.plans[form.value.plan]
  if (!plans) return []
  return Object.entries(plans.parameters || {}).map(([key, item]) => {
    return {
      key,
      ...item as any,
    }
  })
})
const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, { advanced: false, serverEndpointIds: [] }))
const pagination = ref(RouteUtil.pagination(filters.value))
const selection = ref([])
const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  ModelApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
onMounted(() => {
  handleRefresh(false, true)
  ModelApi.config().then((result: any) => {
    Object.assign(config.value, { ready: true }, ApiUtil.data(result))
  }).catch(() => {})
})
const infoVisible = ref(false)
const formVisible = ref(false)
const formLoading = ref(false)
const form: any = ref({})
const formRef: any = ref<FormInstance>()
const rules = ref({
  providerId: [{ required: true, message: '请选择所属供应商', trigger: 'change' }],
  name: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  plan: [{ required: true, message: '请选择计费方案', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
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
    ModelApi.save(form.value, { success: true }).then(result => {
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
    ModelApi.delete(ids, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {})
  }).catch(() => {})
}
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="供应商" prop="providerId">
        <form-select v-model="filters.providerId" :callback="ProviderApi.list" clearable />
      </form-search-item>
      <form-search-item label="模型名称" prop="model">
        <el-input v-model="filters.model" clearable />
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
        <button-add v-permit="'lm:serverEndpoint:add'" @click="handleAdd" />
        <button-delete v-permit="'lm:serverEndpoint:delete'" :disabled="selection.length === 0" @click="handleDelete" />
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
        <template #alias="scope">{{ scope.row.alias ? scope.row.alias : (scope.row.providerInfo?.serial + '/' + scope.row.name) }}</template>
        <template #role="scope">
          <el-space><el-tag v-for="item in scope.row.roles" :key="item.id">{{ item.name }}</el-tag></el-space>
        </template>
      </TableColumn>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button link @click="handleShow(scope)" v-permit="'lm:model:'">查看</el-button>
          <el-button link @click="handleEdit(scope)" v-permit="'lm:model:modify'">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
  </el-card>
  <el-drawer v-model="infoVisible" :title="'信息查看 - ' + form.id">
    <el-form :model="form" label-width="auto">
      <el-form-item label="所属供应商">{{ form.providerInfo?.name }}</el-form-item>
      <el-form-item label="模型名称">{{ form.name }}</el-form-item>
      <el-form-item label="模型别名">{{ form.alias }}</el-form-item>
      <el-form-item label="模型类型">{{ form.typeText }}</el-form-item>
      <el-form-item label="模型广场">{{ form.explorable }}</el-form-item>
      <el-form-item label="全部可见">{{ form.allVisible }}</el-form-item>
      <el-form-item label="安全围栏">{{ form.securityDetectable }}</el-form-item>
      <el-form-item label="计费方案">{{ form.planText }}</el-form-item>
      <el-form-item label="授权角色">
        <el-space><el-tag v-for="item in form.roles" :key="item.id">{{ item.name }}</el-tag></el-space>
      </el-form-item>
      <el-form-item label="配置参数">
         <el-input type="textarea" :value="JSON.stringify(form.content, null, 4)" :rows="5" />
      </el-form-item>
      <el-form-item label="排序">{{ form.sort }}</el-form-item>
      <el-form-item label="状态">{{ form.statusText }}</el-form-item>
      <el-form-item label="描述">{{ form.description ? form.description : '暂无' }}</el-form-item>
      <el-form-item label="创建者">{{ form.createdUserInfo?.name }}</el-form-item>
      <el-form-item label="创建时间">{{ DateUtil.format(form.createdTime) }}</el-form-item>
      <el-form-item label="修改者">{{ form.updatedUserInfo?.name }}</el-form-item>
      <el-form-item label="修改时间">{{ DateUtil.format(form.updatedTime) }}</el-form-item>
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
        <el-descriptions-item label="所属供应商">
          <form-select v-model="form.providerId" :callback="ProviderApi.list" clearable />
        </el-descriptions-item>
        <el-descriptions-item label="模型名称" prop="name">
          <el-input v-model="form.name" />
        </el-descriptions-item>
        <el-descriptions-item label="模型别名" prop="alias">
          <el-input v-model="form.alias" placeholder="默认为：供应商标识/模型名称" />
        </el-descriptions-item>
        <el-descriptions-item label="模型类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择">
            <el-option v-for="(value, key) in config.types" :key="key" :value="key" :label="value" />
          </el-select>
        </el-descriptions-item>
        <el-descriptions-item label="计费方案" prop="plan">
          <el-select v-model="form.plan" placeholder="请选择">
            <el-option v-for="(item, key) in config.plans" :key="key" :value="key" :label="item.name" />
          </el-select>
        </el-descriptions-item>
        <el-descriptions-item label="状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择">
            <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
          </el-select>
        </el-descriptions-item>
        <el-descriptions-item label="模型广场">
           <el-checkbox v-model="form.explorable">在模型广场展示</el-checkbox>
        </el-descriptions-item>
        <el-descriptions-item label="可见范围">
          <el-checkbox v-model="form.allVisible">无授权用户也可见</el-checkbox>
        </el-descriptions-item>
        <el-descriptions-item label="安全围栏">
          <el-checkbox v-model="form.securityDetectable">启用安全围栏</el-checkbox>
        </el-descriptions-item>
        <el-descriptions-item label="排序">
          <el-input-number v-model="form.sort" />
        </el-descriptions-item>
        <el-descriptions-item label="授权角色" :span="3">
          <form-select v-model="form.roleIds" :callback="RoleApi.list" multiple clearable />
        </el-descriptions-item>
        <el-descriptions-item label="描述信息" :span="3">
          <el-input type="textarea" v-model="form.description" />
        </el-descriptions-item>
      </el-descriptions>
      <el-divider content-position="left">计费方案详细配置</el-divider>
      <el-table :data="parameters">
        <el-table-column prop="name" label="参数名称" />
        <el-table-column label="参数值">
          <template #default="scope">
            <el-input-number v-model="form.content[scope.row.key]" disabled-scientific style="width: 300px;" v-if="scope.row.type === 'number'" />
            <el-input v-model="form.content[scope.row.key]" v-else-if="scope.row.type === 'string'" />
            <el-checkbox v-model="form.content[scope.row.key]"  v-else-if="scope.row.type === 'boolean'" />
            <el-alert :title="`未知参数类型：${scope.row.type}`" type="warning" show-icon v-else />
          </template>
        </el-table-column>
        <el-table-column prop="unit" label="单位" />
      </el-table>
    </el-form>
  </el-drawer>
</template>

<style lang="scss" scoped>
</style>
